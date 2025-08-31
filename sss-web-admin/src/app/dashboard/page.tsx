'use client';

import React, { useEffect, useRef, useState } from 'react';
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
  GridItem,
  Menu,
  MenuButton,
  MenuList,
  MenuItem,
  IconButton,
  Avatar
} from '@chakra-ui/react';
import { ChevronDownIcon } from '@chakra-ui/icons';
import { FiBarChart, FiLogOut } from 'react-icons/fi';
import { useRouter } from 'next/navigation';
import CreateDeviceForm from '@/components/ui/CreateDeviceForm';
import DeviceList from '@/components/ui/DeviceList';
import AlertEmailList from '@/components/ui/AlertEmailList';

/**
 * Dashboard page component - protected route with next-auth
 * @returns JSX element for dashboard
 */
export default function Dashboard() {
  const { data: session, status } = useSession();
  const toast = useToast();
  const router = useRouter();
  const [isMounted, setIsMounted] = useState(false);

  const deviceListRef = useRef<{ refreshIfLastPage: () => void }>(null);

  // Prevent hydration mismatch by only using color values after mount
  useEffect(() => {
    setIsMounted(true);
  }, []);
  
  // Color mode values - always call hooks to avoid Rules of Hooks violations
  const bgColorValue = useColorModeValue('gray.50', 'gray.900');
  const navBgColorValue = useColorModeValue('white', 'gray.800');
  const textColorValue = useColorModeValue('gray.900', 'white');
  const subtextColorValue = useColorModeValue('gray.600', 'gray.400');
  const borderColorValue = useColorModeValue('gray.200', 'gray.700');
  const hoverColorValue = useColorModeValue('gray.100', 'gray.700');

  // Use mounted state to prevent hydration issues
  const bgColor = isMounted ? bgColorValue : 'gray.50';
  const navBgColor = isMounted ? navBgColorValue : 'white';
  const textColor = isMounted ? textColorValue : 'gray.900';
  const subtextColor = isMounted ? subtextColorValue : 'gray.600';
  const borderColor = isMounted ? borderColorValue : 'gray.200';
  const hoverColor = isMounted ? hoverColorValue : 'gray.100';

  // Redirect to login if not authenticated
  useEffect(() => {
    if (status === 'unauthenticated') {
      router.push('/');
    }
  }, [status, router]);


  /**
   * Handles device creation success - refreshes device list if on last page
   */
  const handleDeviceCreated = () => {
    if (deviceListRef.current) {
      deviceListRef.current.refreshIfLastPage();
    }
  };

  /**
   * Handles redirection to Grafana dashboard with proper authentication
   */
  const handleGrafanaRedirect = async () => {
    try {
      // Use server-side redirect that validates session
      window.open(`https://media115.lanestel.fr/grafana`, '_blank');
      
      // Optional: Show feedback to user
      toast({
        title: 'Redirection vers Grafana',
        description: 'Ouverture du tableau de bord Grafana...',
        status: 'info',
        duration: 2000,
        isClosable: true,
        position: 'top'
      });
    } catch (error) {
      console.error('Grafana redirect error:', error);
      toast({
        title: 'Erreur',
        description: 'Impossible d\'accéder à Grafana. Veuillez réessayer.',
        status: 'error',
        duration: 3000,
        isClosable: true,
        position: 'top'
      });
    }
  };

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
              
              {/* User Dropdown Menu */}
              <Menu>
                <MenuButton
                  as={Button}
                  size={{ base: 'sm', md: 'md' }}
                  variant="ghost"
                  rightIcon={<ChevronDownIcon />}
                  leftIcon={
                    <Avatar 
                      size="xs" 
                      name={session?.user?.name || 'Admin'}
                      bg="blue.500"
                    />
                  }
                  color={textColor}
                  _hover={{ bg: hoverColor }}
                >
                  <Text display={{ base: 'none', md: 'block' }}>
                    {session?.user?.name || 'Admin'}
                  </Text>
                </MenuButton>
                <MenuList>
                  <MenuItem 
                    icon={<FiBarChart />}
                    onClick={handleGrafanaRedirect}
                  >
                    Grafana Dashboard
                  </MenuItem>
                  <MenuItem 
                    icon={<FiLogOut />}
                    onClick={handleLogout}
                    color="red.500"
                  >
                    Se déconnecter
                  </MenuItem>
                </MenuList>
              </Menu>
            </HStack>
          </Flex>
        </Container>
      </Box>

      {/* Main Content */}
      <Container 
        maxW="7xl" 
        py={{ base: 2, md: 3 }}
        px={{ base: 2, md: 3 }}
      >
        <Grid 
          templateColumns={{ base: '1fr', xl: '1fr 1fr' }} 
          gap={{ base: 1, md: 2 }}
          templateRows={{ base: 'auto auto', xl: 'auto' }}
        >          
          {/* Device Creation Form */}
          <GridItem>
            <CreateDeviceForm onDeviceCreated={handleDeviceCreated} />
          </GridItem>
          
          {/* Alert Email List */}
          <GridItem>
            <AlertEmailList />
          </GridItem>

          {/* Device List */}
          <GridItem>
            <DeviceList ref={deviceListRef} />
          </GridItem>

          
        </Grid>
      </Container>
    </Box>
  );
}


