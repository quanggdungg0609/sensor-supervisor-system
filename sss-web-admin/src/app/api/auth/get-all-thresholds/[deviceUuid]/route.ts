import { NextRequest, NextResponse } from 'next/server';
import { getServerSession } from 'next-auth';
import { getEnvVarWithFallback } from '@/lib/env-validation';
import { AllThresholdsResponse } from '@/app/types/threshold.type';
import axios from 'axios';

/**
 * GET handler for fetching all thresholds for a specific device
 * @param {NextRequest} request - The incoming request
 * @param {Object} context - Route context
 * @param {Promise<{ deviceUuid: string }>} context.params - Route parameters
 * @returns {Promise<NextResponse>} Response with all thresholds or error
 */
export async function GET(
  request: NextRequest,
  context: { params: Promise<{ deviceUuid: string }> }
) {
  try {
    // Check authentication
    console.log('Checking authentication for get-all-thresholds route...');
    const session = await getServerSession();
    
    if (!session) {
      console.log('No session found, returning 401');
      return NextResponse.json(
        { error: 'Unauthorized' },
        { status: 401 }
      );
    }

    console.log('Authentication successful, proceeding to fetch all thresholds...');

    // Get device UUID from params
    const { deviceUuid } = await context.params;
    
    if (!deviceUuid) {
      return NextResponse.json(
        { error: 'Missing device UUID' },
        { status: 400 }
      );
    }

    // Get base URL from environment variable with fallback
    const baseUrl = getEnvVarWithFallback('ALERT_SERVICE_API_URL', 'http://localhost:2006');
    const externalApiUrl = `${baseUrl}/api/v1/threshold/get_all_thresholds/${deviceUuid}`;

    // Call external alert service API
    console.log('Calling alert service API:', externalApiUrl);

    const response = await axios.get<AllThresholdsResponse>(
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

    return NextResponse.json(response.data);

  } catch (error) {
    console.error('Error fetching all thresholds:', error);
    
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
        { error: `Failed to fetch all thresholds: ${message}` },
        { status }
      );
    }
    
    return NextResponse.json(
      { error: 'Failed to fetch all thresholds', details: error instanceof Error ? error.message : 'Unknown error' },
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