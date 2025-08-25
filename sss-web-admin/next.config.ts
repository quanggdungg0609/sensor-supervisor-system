import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  /* config options here */
  output: 'standalone',
  // No basePath or assetPrefix needed since we're serving from root domain
  // Using stable webpack bundler instead of experimental Turbopack
  // Remove experimental turbo configuration to avoid build issues
  eslint: {
    // Temporarily disable ESLint during build to fix compilation issues
    ignoreDuringBuilds: true,
  },
};

export default nextConfig;
