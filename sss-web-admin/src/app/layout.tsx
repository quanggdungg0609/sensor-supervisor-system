import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";
import ClientProviders from "@/components/ClientProviders";
import { validateEnvironmentVariables } from "@/lib/env-validation";

const inter = Inter({ subsets: ["latin"] });

export const metadata: Metadata = {
  title: "SSS Web Admin",
  description: "SSS Web Admin Dashboard",
};

// Validate environment variables when the app starts (server-side)
if (typeof window === 'undefined') {
  try {
    validateEnvironmentVariables();
  } catch (error) {
    console.error('Application startup failed:', error);
    // In production, you might want to show an error page instead
  }
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body className={inter.className}>
        <ClientProviders>
          {children}
        </ClientProviders>
      </body>
    </html>
  );
}
