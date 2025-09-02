import { NextRequest, NextResponse } from 'next/server';
import { getServerSession } from 'next-auth';
import { getEnvVarWithFallback } from '@/lib/env-validation';
import { ThresholdConfigRequest, ThresholdConfigResponse } from '@/app/types/threshold.type';
import axios from 'axios';

/**
 * POST handler for configuring device threshold
 * @param {NextRequest} request - The incoming request
 * @param {Object} context - Route context
 * @param {Promise<{ deviceUuid: string }>} context.params - Route parameters
 * @returns {Promise<NextResponse>} Response with configuration result or error
 */
export async function POST(
  request: NextRequest,
  context: { params: Promise<{ deviceUuid: string }> }
) {
  try {
    // Check authentication
    console.log('Checking authentication for configure-threshold route...');
    const session = await getServerSession();
    console.log('Session:', session);
    
    if (!session) {
      console.log('No session found, returning 401');
      return NextResponse.json(
        { error: 'Unauthorized' },
        { status: 401 }
      );
    }

    console.log('Authentication successful, proceeding to configure threshold...');

    // Get device UUID from params
    const { deviceUuid } = await context.params;
    
    if (!deviceUuid) {
      return NextResponse.json(
        { error: 'Missing device UUID' },
        { status: 400 }
      );
    }

    // Parse request body
    const body: ThresholdConfigRequest = await request.json();
    const { thresholdLevel, thresholdType, message, threshold } = body;

    // Validate required fields
    if (!thresholdLevel || !thresholdType || !threshold) {
      return NextResponse.json(
        { error: 'Missing required fields: thresholdLevel, thresholdType, threshold' },
        { status: 400 }
      );
    }

    // Validate threshold level
    if (!['WARNING', 'CRITICAL'].includes(thresholdLevel)) {
      return NextResponse.json(
        { error: 'Invalid thresholdLevel. Must be WARNING or CRITICAL' },
        { status: 400 }
      );
    }

    // Validate threshold type
    if (!['UPPER', 'LOWER'].includes(thresholdType)) {
      return NextResponse.json(
        { error: 'Invalid thresholdType. Must be UPPER or LOWER' },
        { status: 400 }
      );
    }

    // Validate threshold values - check that threshold is an object
    if (!threshold || typeof threshold !== 'object') {
      return NextResponse.json(
        { error: 'Invalid threshold values. Threshold must be an object' },
        { status: 400 }
      );
    }

    // Validate that all threshold values are numbers
    for (const [key, value] of Object.entries(threshold)) {
      if (typeof value !== 'number') {
        return NextResponse.json(
          { error: `Invalid threshold value for ${key}. All threshold values must be numbers` },
          { status: 400 }
        );
      }
      
      // Apply specific validation rules based on telemetry type
      if (key === 'temperature') {
        if (value < -50 || value > 100) {
          return NextResponse.json(
            { error: 'Temperature must be between -50 and 100 degrees Celsius' },
            { status: 400 }
          );
        }
      } else if (key === 'humidity') {
        if (value < 0 || value > 100) {
          return NextResponse.json(
            { error: 'Humidity must be between 0 and 100 percent' },
            { status: 400 }
          );
        }
      } else {
        // Generic validation for other telemetry types
        if (value < 0 || value > 1000) {
          return NextResponse.json(
            { error: `Value for ${key} must be between 0 and 1000` },
            { status: 400 }
          );
        }
      }
    }

    // Get base URL from environment variable with fallback
    const baseUrl = getEnvVarWithFallback('ALERT_SERVICE_API_URL', 'http://localhost:2006');
    const externalApiUrl = `${baseUrl}/api/v1/threshold/configure/${deviceUuid}`;

    // Prepare data for external API
    const externalApiData = {
      thresholdLevel,
      thresholdType,
      message: message || null,
      threshold
    };

    // Internal service communication
    console.log('Calling alert service API:', externalApiUrl);
    console.log('With data:', externalApiData);

    const response = await axios.post<ThresholdConfigResponse>(
      externalApiUrl,
      externalApiData,
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
    console.error('Error configuring threshold:', error);
    
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
        { error: `Failed to configure threshold: ${message}` },
        { status }
      );
    }
    
    return NextResponse.json(
      { error: 'Failed to configure threshold', details: error instanceof Error ? error.message : 'Unknown error' },
      { status: 500 }
    );
  }
}

/**
 * Handles unsupported HTTP methods
 * @returns {NextResponse} Method not allowed response
 */
export async function GET(): Promise<NextResponse> {
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