import { NextRequest, NextResponse } from 'next/server';
import { getServerSession } from 'next-auth';
import { getEnvVarWithFallback } from '@/lib/env-validation';
import axios from 'axios';

/**
 * Interface for telemetry values response
 * @interface TelemetryValuesResponse
 * @property {string[]} telemetry_values - Array of telemetry value names
 */
export interface TelemetryValuesResponse {
  telemetry_values: string[];
}

/**
 * Interface for filtered telemetry values response
 * @interface FilteredTelemetryValuesResponse
 * @property {string[]} telemetry_values - Array of filtered telemetry value names (excluding power_status)
 */
export interface FilteredTelemetryValuesResponse {
  telemetry_values: string[];
}

/**
 * GET handler for fetching telemetry values for a specific device
 * @param {NextRequest} request - The incoming request
 * @param {Object} params - Route parameters
 * @param {string} params.deviceUuid - Device UUID from URL
 * @returns {Promise<NextResponse>} Response with filtered telemetry values or error
 */
export async function GET(
  request: NextRequest,
  { params }: { params: { deviceUuid: string } }
) {
  try {
    // Check authentication
    console.log('Checking authentication for telemetry-values route...');
    const session = await getServerSession();
    
    if (!session) {
      console.log('No session found, returning 401');
      return NextResponse.json(
        { error: 'Unauthorized' },
        { status: 401 }
      );
    }

    console.log('Authentication successful, proceeding to fetch telemetry values...');

    // Get device UUID from params
    const { deviceUuid } = params;
    
    if (!deviceUuid) {
      return NextResponse.json(
        { error: 'Missing device UUID' },
        { status: 400 }
      );
    }

    // Get base URL from environment variable with fallback
    const baseUrl = getEnvVarWithFallback('ALERT_SERVICE_API_URL', 'http://localhost:2006');
    const externalApiUrl = `${baseUrl}/api/v1/threshold/get_telemetry_values/${deviceUuid}`;

    // Call external alert service API
    console.log('Calling alert service API:', externalApiUrl);

    const response = await axios.get<TelemetryValuesResponse>(
      externalApiUrl,
      {
        timeout: 10000, // 10 second timeout
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json'
        }
      }
    );

    console.log('Alert service API response status:', response.status);
    console.log('Alert service API response data:', response.data);

    // Filter out power_status from telemetry values
    const filteredTelemetryValues = response.data.telemetry_values.filter(
      (value: string) => value !== 'power_status'
    );

    console.log('Filtered telemetry values:', filteredTelemetryValues);

    const filteredResponse: FilteredTelemetryValuesResponse = {
      telemetry_values: filteredTelemetryValues
    };

    return NextResponse.json(filteredResponse);

  } catch (error) {
    console.error('Error fetching telemetry values:', error);
    
    // Handle axios errors
    if (axios.isAxiosError(error)) {
      const status = error.response?.status || 500;
      const message = error.response?.data?.error || error.response?.data?.message || error.message || 'External API error';
      
      console.error('Axios error details:', {
        status: error.response?.status,
        statusText: error.response?.statusText,
        data: error.response?.data,
        url: error.config?.url
      });
      
      return NextResponse.json(
        { error: `Failed to fetch telemetry values: ${message}` },
        { status }
      );
    }
    
    return NextResponse.json(
      { error: 'Failed to fetch telemetry values', details: error instanceof Error ? error.message : 'Unknown error' },
      { status: 500 }
    );
  }
}

/**
 * Handles unsupported HTTP methods
 * @returns {NextResponse} Method not allowed response
 */
export async function POST(): Promise<NextResponse> {
  return NextResponse.json(
    { error: 'Method not allowed' },
    { status: 405 }
  );
}

export async function PUT(): Promise<NextResponse> {
  return NextResponse.json(
    { error: 'Method not allowed' },
    { status: 405 }
  );
}

export async function DELETE(): Promise<NextResponse> {
  return NextResponse.json(
    { error: 'Method not allowed' },
    { status: 405 }
  );
}