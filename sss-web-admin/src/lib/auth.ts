import { NextAuthOptions } from "next-auth";
import CredentialsProvider from "next-auth/providers/credentials";

declare module "next-auth" {
  interface Session {
    user: {
      id: string;
      name?: string | null;
      email?: string | null;
      image?: string | null;
    }
  }
}

declare module "next-auth/jwt" {
  interface JWT {
    id: string;
  }
}

export const authOptions: NextAuthOptions = {
  providers: [
    CredentialsProvider({
      name: "Credentials",
      credentials: {
        username: { label: "Username", type: "text" },
        password: { label: "Password", type: "password" }
      },
      async authorize(credentials) {
        const adminUser = process.env.ADMIN_USER;
        const adminPassword = process.env.ADMIN_PASSWORD;

        if (credentials?.username === adminUser && credentials?.password === adminPassword) {
          return { id: "1", name: "Admin" };
        }
        return null;
      }
    })
  ],
  secret: process.env.NEXTAUTH_SECRET,
  session: {
    strategy: "jwt",
    maxAge: 24 * 60 * 60, // 24 hours
  },
  // Configuration for reverse proxy setup
  useSecureCookies: process.env.NODE_ENV === "production" && process.env.NEXTAUTH_URL?.startsWith("https://"),
  cookies: {
    sessionToken: {
      name: process.env.NODE_ENV === "production" ? "__Secure-next-auth.session-token" : "next-auth.session-token",
      options: {
        httpOnly: true,
        sameSite: "lax",
        path: "/",
        domain: process.env.NODE_ENV === "production" ? ".media115.lanestel.fr" : undefined,
        secure: process.env.NODE_ENV === "production" && process.env.NEXTAUTH_URL?.startsWith("https://"),
      },
    },
  },
  callbacks: {
    async jwt({ token, user }) {
      if (user) {
        token.id = user.id;
      }
      return token;
    },
    async session({ session, token }) {
      if (token) {
        session.user.id = token.id;
      }
      return session;
    },
    async redirect({ url, baseUrl }) {
      // Handle redirects for multi-layer reverse proxy (Nginx → Traefik)
      console.log('NextAuth redirect:', { url, baseUrl, nodeEnv: process.env.NODE_ENV, nextAuthUrl: process.env.NEXTAUTH_URL });
      
      // Always redirect to dashboard for successful login
      console.log('Redirecting to dashboard');
      return '/dashboard';
    }
  },
  pages: {
    signIn: '/',
  },
};