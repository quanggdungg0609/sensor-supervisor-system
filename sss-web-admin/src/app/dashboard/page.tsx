'use client';

import React, { useEffect } from 'react';
import { signOut, useSession } from 'next-auth/react';
import {
  Box,
  Flex,
  Heading,
  Text,
  Button,
  Container,
  Spinner,
  Center,
  useToast,
  useColorModeValue,
  Spacer,
  VStack,
  HStack,
  Grid,
  GridItem
} from '@chakra-ui/react';
import { useRouter } from 'next/navigation';
import CreateDeviceForm from '@/components/ui/CreateDeviceForm';

/**
 * Dashboard page component - protected route with next-auth
 * @returns JSX element for dashboard
 */
export default function Dashboard() {
  const { data: session, status } = useSession();
  const toast = useToast();
  const router = useRouter();
  
  // Color mode values
  const bgColor = useColorModeValue('gray.50', 'gray.900');
  const navBgColor = useColorModeValue('white', 'gray.800');
  const textColor = useColorModeValue('gray.900', 'white');
  const subtextColor = useColorModeValue('gray.600', 'gray.400');
  const borderColor = useColorModeValue('gray.200', 'gray.700');

  // Redirect to login if not authenticated
  useEffect(() => {
    if (status === 'unauthenticated') {
      router.push('/');
    }
  }, [status, router]);

  /**
   * Handles user logout using next-auth signOut
   * Shows success notification and redirects to login
   */
  const handleLogout = async () => {
    try {
      toast({
        title: 'Déconnexion en cours...',
        status: 'info',
        duration: 1000,
        isClosable: true,
        position: 'top'
      });
      
      await signOut({ 
        redirect: true,
        callbackUrl: '/'
      });
    } catch (error) {
      console.error('Logout error:', error);
      toast({
        title: 'Erreur de déconnexion',
        description: 'Une erreur s\'est produite lors de la déconnexion',
        status: 'error',
        duration: 3000,
        isClosable: true,
        position: 'top'
      });
    }
  };

  // Show loading while checking session
  if (status === 'loading') {
    return (
      <Box minH="100vh" bg={bgColor}>
        <Center h="100vh">
          <VStack spacing={4}>
            <Spinner size="xl" color="blue.500" thickness="4px" />
            <Text fontSize="lg" color={textColor}>Chargement...</Text>
          </VStack>
        </Center>
      </Box>
    );
  }

  // Redirect if not authenticated (additional check)
  if (status === 'unauthenticated') {
    return (
      <Box minH="100vh" bg={bgColor}>
        <Center h="100vh">
          <VStack spacing={4}>
            <Spinner size="xl" color="blue.500" thickness="4px" />
            <Text fontSize="lg" color={textColor}>Redirection...</Text>
          </VStack>
        </Center>
      </Box>
    );
  }

  // Only render dashboard if authenticated
  if (status !== 'authenticated') {
    return null;
  }

  return (
    <Box minH="100vh" bg={bgColor}>
      {/* Navigation Bar */}
      <Box bg={navBgColor} shadow="sm" borderBottomWidth={1} borderColor={borderColor}>
        <Container maxW="7xl" px={{ base: 4, md: 6 }}>
          <Flex h={{ base: 14, md: 16 }} alignItems="center">
            <Heading size={{ base: 'md', md: 'lg' }} color={textColor}>
              Tableau de bord Admin
            </Heading>
            <Spacer />
            <HStack spacing={{ base: 2, md: 4 }}>
              <Text 
                fontSize={{ base: 'xs', md: 'sm' }} 
                color={subtextColor}
                display={{ base: 'none', sm: 'block' }}
              >
                Connecté en tant que: {session?.user?.name || 'Admin'}
              </Text>
              <Button
                onClick={handleLogout}
                colorScheme="red"
                size={{ base: 'xs', md: 'sm' }}
                variant="solid"
              >
                Se déconnecter
              </Button>
            </HStack>
          </Flex>
        </Container>
      </Box>

      {/* Main Content */}
      <Container 
        maxW="7xl" 
        py={{ base: 1, md: 2 }}
        px={{ base: 1, md: 2 }}
      >
        <Grid 
          templateColumns={{ base: '1fr', lg: '1fr 1fr' }} 
          gap={{ base: 2, md: 4 }} 
        >          
          {/* Device Creation Form */}
          <GridItem>
            <CreateDeviceForm />
          </GridItem>
        </Grid>
      </Container>
    </Box>
  );
}