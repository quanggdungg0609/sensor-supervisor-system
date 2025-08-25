'use client';

import { useState, useEffect } from 'react';
import { Box, Button, FormControl, FormLabel, Input, InputGroup, InputLeftElement, VStack, useToast, Icon, Text, Heading } from '@chakra-ui/react';
import { signIn, useSession } from 'next-auth/react';
import { useRouter } from 'next/navigation';

/**
 * Login page component with next-auth integration
 * Redirects authenticated users to dashboard
 * @returns JSX element for login page
 */
function LoginPage() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const toast = useToast();
  const router = useRouter();
  const { status } = useSession();

  // Redirect if already authenticated
  useEffect(() => {
    if (status === 'authenticated') {
      console.log('User already authenticated, redirecting to dashboard...');
      // Use relative path that works with reverse proxy
      window.location.href = '/dashboard';
    }
  }, [status]);

  /**
   * Handles user login using next-auth
   * Shows toast notifications for success/error states
   */
  const handleLogin = async () => {
    setIsLoading(true);
    
    try {
      console.log('Attempting login...');
      const res = await signIn('credentials', {
        username,
        password,
        callbackUrl: '/dashboard',
        redirect: true, // Let NextAuth handle the redirect
      });

      // This code will only run if redirect is false or if there's an error
      console.log('SignIn response:', res);

      if (res?.error) {
        console.error('Login error:', res.error);
        toast({
          title: 'Erreur',
          description: 'Nom d\'utilisateur ou mot de passe incorrect.',
          status: 'error',
          duration: 3000,
          isClosable: true,
        });
      }
    } catch (error) {
      console.error('Login exception:', error);
      toast({
        title: 'Erreur',
        description: 'Une erreur s\'est produite lors de la connexion.',
        status: 'error',
        duration: 3000,
        isClosable: true,
      });
    } finally {
      setIsLoading(false);
    }
  };

  /**
   * Handles form submission on Enter key press
   * @param e - Keyboard event
   */
  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      handleLogin();
    }
  };

  // Show loading while checking session
  if (status === 'loading') {
    return (
      <Box maxW="md" mx="auto" mt="20" p="6" borderWidth="1px" borderRadius="lg">
        <div className="text-center">Chargement...</div>
      </Box>
    );
  }

  // Don't show login form if already authenticated
  if (status === 'authenticated') {
    return null;
  }

  return (
    <Box 
      maxW="450px" 
      mx="auto" 
      mt="16" 
      p="8" 
      borderWidth="1px" 
      borderRadius="xl" 
      boxShadow="2xl"
      bg="white"
    >
      <VStack spacing="6">
        {/* Header */}
        <Box textAlign="center" mb="4">
          <Heading 
            size="lg" 
            color="gray.700"
            mb="2"
          >
            Connexion Admin
          </Heading>
          <Text color="gray.500" fontSize="sm">
            Connectez-vous pour accéder au tableau de bord administrateur
          </Text>
        </Box>

        {/* Username Field */}
        <FormControl>
          <FormLabel 
            htmlFor="username" 
            fontSize="sm" 
            fontWeight="medium" 
            color="gray.700"
            mb="2"
          >
            Nom d&apos;utilisateur
          </FormLabel>
          <InputGroup size="lg">
            <InputLeftElement pointerEvents="none" height="12">
              <Icon viewBox="0 0 24 24" color="gray.400" w="5" h="5">
                <path 
                  fill="currentColor" 
                  d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"
                />
              </Icon>
            </InputLeftElement>
            <Input
              id="username"
              type="text"
              placeholder="Entrez votre nom d'utilisateur"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              onKeyPress={handleKeyPress}
              height="12"
              borderColor="gray.300"
              _focus={{ 
                borderColor: "blue.500", 
                boxShadow: "0 0 0 1px #3182ce" 
              }}
              _hover={{ borderColor: "gray.400" }}
            />
          </InputGroup>
        </FormControl>

        {/* Password Field */}
        <FormControl>
          <FormLabel 
            htmlFor="password" 
            fontSize="sm" 
            fontWeight="medium" 
            color="gray.700"
            mb="2"
          >
            Mot de passe
          </FormLabel>
          <InputGroup size="lg">
            <InputLeftElement pointerEvents="none" height="12">
              <Icon viewBox="0 0 24 24" color="gray.400" w="5" h="5">
                <path 
                  fill="currentColor" 
                  d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zM9 6c0-1.66 1.34-3 3-3s3 1.34 3 3v2H9V6z"
                />
              </Icon>
            </InputLeftElement>
            <Input
              id="password"
              type="password"
              placeholder="Entrez votre mot de passe"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              onKeyPress={handleKeyPress}
              height="12"
              borderColor="gray.300"
              _focus={{ 
                borderColor: "blue.500", 
                boxShadow: "0 0 0 1px #3182ce" 
              }}
              _hover={{ borderColor: "gray.400" }}
            />
          </InputGroup>
        </FormControl>

        {/* Login Button */}
        <Button 
          colorScheme="blue" 
          onClick={handleLogin}
          isLoading={isLoading}
          loadingText="Connexion..."
          width="full"
          height="12"
          fontSize="md"
          fontWeight="semibold"
          _hover={{ transform: "translateY(-1px)", boxShadow: "lg" }}
          _active={{ transform: "translateY(0)" }}
        >
          Se connecter
        </Button>
      </VStack>
    </Box>
  );
}

export default LoginPage;
