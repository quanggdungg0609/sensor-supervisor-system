import { NextRequest, NextResponse } from 'next/server';
import { getServerSession } from 'next-auth';
import { getEnvVarWithFallback } from '@/lib/env-validation';
import { AlertEmailsResponse } from '@/app/types/alerts.type';
import axios from 'axios';

/**
 * GET handler for fetching alert emails list
 * @param {NextRequest} request - The incoming request
 * @returns {Promise<NextResponse>} Response with alert emails list or error
 */
export async function GET(request: NextRequest) {
  try {
    // Check authentication
    console.log('Checking authentication for get-alert-mails route...');
    const session = await getServerSession();
    console.log('Session:', session);
    
    if (!session) {
      console.log('No session found, returning 401');
      return NextResponse.json(
        { error: 'Unauthorized' },
        { status: 401 }
      );
    }

    console.log('Authentication successful, proceeding to fetch emails...');

    // Get base URL from environment variable with fallback
    const baseUrl = getEnvVarWithFallback('ALERT_SERVICE_API_URL', 'http://localhost:2006');
    const externalApiUrl = `${baseUrl}/api/v1/threshold/get_all_email_alerts`;

    // Internal service communication
    console.log('Calling alert service API:', externalApiUrl);

    const response = await axios.get<AlertEmailsResponse>(
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
    console.error('Error fetching alert emails:', error);
    
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
      { error: 'Failed to fetch alert emails', details: error instanceof Error ? error.message : 'Unknown error' },
      { status: 500 }
    );
  }
}