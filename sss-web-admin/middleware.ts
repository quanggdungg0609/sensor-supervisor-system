import { withAuth } from "next-auth/middleware";
import { NextResponse } from "next/server";

/**
 * Middleware function to protect routes with authentication
 * Redirects unauthenticated users to login page
 * @param req - The incoming request object
 * @returns NextResponse for the request
 */
export default withAuth(
  function middleware(req) {
    // Log for debugging (remove in production)
    console.log('Middleware triggered for:', req.nextUrl.pathname);
    console.log('Token exists:', !!req.nextauth.token);
    
    return NextResponse.next();
  },
  {
    callbacks: {
      authorized: ({ token, req }) => {
        const { pathname } = req.nextUrl;
        
        // Always allow access to login page
        if (pathname === '/') {
          return true;
        }
        
        // For all other pages, require authentication
        return !!token;
      },
    },
    pages: {
      signIn: '/',
    },
  }
);

export const config = {
  matcher: [
    // Protect all routes except:
    // - API auth routes (/api/auth/*)
    // - Static files (_next/static/*)
    // - Image optimization (_next/image/*)
    // - Favicon and other public assets
    // - Root path (/) - handled by authorized callback
    '/((?!api/auth|_next/static|_next/image|favicon.ico|.*\\..*).*)'
  ],
};