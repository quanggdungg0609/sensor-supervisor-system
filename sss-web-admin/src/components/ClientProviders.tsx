'use client';

import { SessionProvider } from 'next-auth/react';
import { ChakraProvider } from '@chakra-ui/react';

/**
 * Client-side providers wrapper component
 * Wraps SessionProvider and ChakraProvider for client-side functionality
 * @param children - Child components to render
 * @returns JSX element with providers
 */
export default function ClientProviders({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <SessionProvider>
      <ChakraProvider>
        {children}
      </ChakraProvider>
    </SessionProvider>
  );
}