import { NextRequest, NextResponse } from 'next/server';
import { getServerSession } from 'next-auth';
import { authOptions } from '@/lib/auth';
import { getEnvVarWithFallback } from '@/lib/env-validation';
import { DeviceListResponse } from '@/app/types/devices.type';
import axios from 'axios';
/**
 * Interface for device object
 * @interface Device
 * @property {string} device_uuid - Unique identifier for the device
 * @property {string} device_name - Name of the device
 * @property {string} mqtt_username - MQTT username for the device
 * @property {string} client_id - Client ID for the device
 */


/**
 * Interface for device list response
 * @interface DeviceListResponse
 * @property {Device[]} data - Array of devices
 * @property {number} page - Current page number
 * @property {number} size - Page size
 * @property {number} total_elements - Total number of elements
 * @property {number} total_pages - Total number of pages
 * @property {boolean} first - Whether this is the first page
 * @property {boolean} last - Whether this is the last page
 * @property {boolean} has_next - Whether there is a next page
 * @property {boolean} has_previous - Whether there is a previous page
 */

/**
 * GET handler for fetching devices list
 * @param {NextRequest} request - The incoming request
 * @returns {Promise<NextResponse>} Response with devices list or error
 */
export async function GET(request: NextRequest) {
  try {
    // Check authentication
    const session = await getServerSession(authOptions);
    if (!session) {
      return NextResponse.json(
        { error: 'Unauthorized' },
        { status: 401 }
      );
    }

    // Get base URL from environment variable with fallback
    const baseUrl = getEnvVarWithFallback('DEVICE_SERVICE_API_URL', 'http://sss-device-service:2002');
    const externalApiUrl = `${baseUrl}/api/v1/devices`;

    // Get query parameters
    const { searchParams } = new URL(request.url);
    const page = searchParams.get('page') || '0';
    const size = searchParams.get('size') || '10';

    // Internal Docker Swarm communication uses HTTP, no HTTPS agent needed
    console.log('Calling internal service API:', externalApiUrl);
    console.log('With params:', { page, size });

    const response = await axios.get<DeviceListResponse>(
      externalApiUrl,
      {
        params: { page, size },
        timeout: 10000, // 10 second timeout
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json'
        }
      }
    );

    console.log('External API response status:', response.status);
    return NextResponse.json(response.data);
  } catch (error) {
    console.error('Error fetching devices:', error);
    
    // Log more detailed error information
    if (axios.isAxiosError(error)) {
      console.error('Axios error details:', {
        status: error.response?.status,
        statusText: error.response?.statusText,
        data: error.response?.data,
        url: error.config?.url
      });
    }
    
    return NextResponse.json(
      { error: 'Failed to fetch devices', details: error instanceof Error ? error.message : 'Unknown error' },
      { status: 500 }
    );
  }
}