import { NextRequest, NextResponse } from 'next/server';
import { getToken } from 'next-auth/jwt';

/**
 * GET handler for Traefik ForwardAuth
 * @param request - The incoming request from Traefik
 * @returns Response with 200 (authenticated) or 302 (not authenticated)
 */
export async function GET(request: NextRequest): Promise<NextResponse> {
  try {
    // Debug logging
    const requestUrl = request.headers.get('x-forwarded-uri') || 'unknown';
    const cookie = request.headers.get('cookie') || 'no-cookie';
    const userAgent = request.headers.get('user-agent') || 'unknown';
    
    // Log key request info for debugging
    console.log(`ForwardAuth: ${requestUrl} | Cookie: ${cookie.substring(0, 100)}... | UA: ${userAgent.substring(0, 50)}`);
    
    // Get the JWT token from NextAuth
    const token = await getToken({ 
      req: request,
      secret: process.env.NEXTAUTH_SECRET 
    });

    // --- CASE 1: User is NOT authenticated ---
    if (!token) {
      console.log(`ForwardAuth: No valid token found for ${requestUrl}`);
      
      // Return 401 instead of redirect to avoid loops
      return new NextResponse('Unauthorized - Please login first', { 
        status: 401,
        headers: {
          'WWW-Authenticate': 'Bearer realm="Access to protected resource"',
          'X-Auth-Required': 'true'
        }
      });
    }

    // --- CASE 2: User IS authenticated ---
    console.log(`ForwardAuth: User '${token.name}' authenticated for ${requestUrl}`);
    
    const response = new NextResponse(null, { status: 200 });
    
    // Set auth proxy headers that Grafana will receive
    response.headers.set('X-WEBAUTH-USER', token.name || 'admin');
    response.headers.set('X-WEBAUTH-ROLE', 'Admin');
    response.headers.set('X-Auth-Success', 'true');
    
    return response;
    
  } catch (error) {
    console.error('ForwardAuth error:', error);
    return new NextResponse('Internal Server Error', { status: 500 });
  }
}


/**
 * HEAD handler for health checks
 */
export async function HEAD(request: NextRequest): Promise<NextResponse> {
  // A simple 200 OK is enough for health checks.
  return new NextResponse(null, { status: 200 });
}