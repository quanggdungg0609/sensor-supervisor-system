import { NextRequest, NextResponse } from 'next/server';
import { getServerSession } from 'next-auth';
import { getEnvVarWithFallback } from '@/lib/env-validation';
import axios from 'axios';

/**
 * Interface for add email request body
 * @interface AddEmailRequest
 * @property {string} email - Email address to add
 */
interface AddEmailRequest {
  email: string;
}

/**
 * Interface for add email response
 * @interface AddEmailResponse
 * @property {string} status - Response status
 * @property {string} message - Operation result message
 */
interface AddEmailResponse {
  status: string;
  message: string;
}

/**
 * POST handler for adding an alert email
 * @param {NextRequest} request - The incoming request
 * @returns {Promise<NextResponse>} Response with addition result or error
 */
export async function POST(request: NextRequest) {
  try {
    // Check authentication
    console.log('Checking authentication for add-alert-email route...');
    const session = await getServerSession();
    console.log('Session:', session);
    
    if (!session) {
      console.log('No session found, returning 401');
      return NextResponse.json(
        { error: 'Unauthorized' },
        { status: 401 }
      );
    }

    console.log('Authentication successful, proceeding to add email...');

    // Parse request body
    const body: AddEmailRequest = await request.json();
    const { email } = body;

    // Validate required fields
    if (!email) {
      return NextResponse.json(
        { error: 'Missing required field: email' },
        { status: 400 }
      );
    }

    // Validate field format
    if (typeof email !== 'string') {
      return NextResponse.json(
        { error: 'Invalid field type: email must be a string' },
        { status: 400 }
      );
    }

    // Trim and validate non-empty value
    const trimmedEmail = email.trim();
    if (!trimmedEmail) {
      return NextResponse.json(
        { error: 'email cannot be empty' },
        { status: 400 }
      );
    }

    // Basic email format validation
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(trimmedEmail)) {
      return NextResponse.json(
        { error: 'Invalid email format' },
        { status: 400 }
      );
    }

    // Get base URL from environment variable with fallback
    const baseUrl = getEnvVarWithFallback('ALERT_SERVICE_API_URL', 'http://localhost:2006');
    const externalApiUrl = `${baseUrl}/api/v1/threshold/add_email_alert`;

    // Prepare data for external API
    const externalApiData = {
      email: trimmedEmail
    };

    // Internal service communication
    console.log('Calling alert service API:', externalApiUrl);
    console.log('With data:', externalApiData);

    const response = await axios.post<AddEmailResponse>(
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
    console.error('Error adding alert email:', error);
    
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
      
      // Handle specific CONFLICT error for duplicate email
      if (status === 409) {
        return NextResponse.json(
          { error: 'Email already exists', message: 'Cette adresse email existe déjà dans la liste des alertes' },
          { status: 409 }
        );
      }
      
      return NextResponse.json(
        { error: `Failed to add email: ${message}` },
        { status }
      );
    }
    
    return NextResponse.json(
      { error: 'Failed to add alert email', details: error instanceof Error ? error.message : 'Unknown error' },
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