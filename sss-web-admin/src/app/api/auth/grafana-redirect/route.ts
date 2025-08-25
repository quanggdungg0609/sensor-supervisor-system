import { NextRequest, NextResponse } from 'next/server';
import { getServerSession } from 'next-auth';
import { authOptions } from '@/lib/auth';

/**
 * Handles GET request to redirect to Grafana with authentication headers
 * This API acts as a proxy to pass user authentication info to Grafana
 * via auth proxy headers that Grafana trusts
 * 
 * @param {NextRequest} request - The incoming request
 * @returns {Promise<NextResponse>} Response that redirects to Grafana or returns error
 */
export async function GET(request: NextRequest): Promise<NextResponse> {
  try {
    // Check authentication
    const session = await getServerSession(authOptions);
    
    if (!session || !session.user) {
      return NextResponse.json(
        { error: 'Unauthorized. Please login first.' },
        { status: 401 }
      );
    }

    // Get Grafana URL from environment or use default
    const grafanaUrl = process.env.GRAFANA_PUBLIC_URL || '/grafana';
    
    // Create response that redirects to Grafana
    const response = NextResponse.redirect(new URL(grafanaUrl, request.url));
    
    // Set auth proxy headers that Grafana will trust
    response.headers.set('X-WEBAUTH-USER', session.user.name || 'admin');
    response.headers.set('X-WEBAUTH-NAME', session.user.name || 'Admin User');
    response.headers.set('X-WEBAUTH-EMAIL', session.user.email || 'admin@sss.local');
    response.headers.set('X-WEBAUTH-ROLE', 'Admin');
    
    return response;
    
  } catch (error) {
    console.error('Error in Grafana redirect:', error);
    return NextResponse.json(
      { error: 'Internal server error during Grafana redirect' },
      { status: 500 }
    );
  }
}

/**
 * Handles POST request for programmatic Grafana access
 * Returns Grafana URL with authentication headers for client-side redirect
 * 
 * @param {NextRequest} request - The incoming request  
 * @returns {Promise<NextResponse>} Response with Grafana access info
 */
export async function POST(_request: NextRequest): Promise<NextResponse> {
  try {
    // Check authentication
    const session = await getServerSession(authOptions);
    
    if (!session || !session.user) {
      return NextResponse.json(
        { error: 'Unauthorized. Please login first.' },
        { status: 401 }
      );
    }

    // Get Grafana URL from environment or use default
    const grafanaUrl = process.env.GRAFANA_PUBLIC_URL || '/grafana';
    
    // Return the authentication headers and URL for client-side use
    return NextResponse.json({
      success: true,
      grafanaUrl,
      authHeaders: {
        'X-WEBAUTH-USER': session.user.name || 'admin',
        'X-WEBAUTH-NAME': session.user.name || 'Admin User', 
        'X-WEBAUTH-EMAIL': session.user.email || 'admin@sss.local',
        'X-WEBAUTH-ROLE': 'Admin'
      }
    });
    
  } catch (error) {
    console.error('Error getting Grafana auth info:', error);
    return NextResponse.json(
      { error: 'Internal server error getting Grafana auth info' },
      { status: 500 }
    );
  }
}