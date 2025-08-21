'use client';

import { useState, useEffect } from 'react';
import { Box, Button, FormControl, Input, InputGroup, InputLeftElement, VStack, useToast } from '@chakra-ui/react';
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
  const { data: session, status } = useSession();

  // Redirect if already authenticated
  useEffect(() => {
    if (status === 'authenticated') {
      router.push('/dashboard');
    }
  }, [status, router]);

  /**
   * Handles user login using next-auth
   * Shows toast notifications for success/error states
   */
  const handleLogin = async () => {
    setIsLoading(true);
    
    try {
      const res = await signIn('credentials', {
        username,
        password,
        redirect: false,
      });

      if (res?.error) {
        toast({
          title: 'Erreur',
          description: 'Nom d\'utilisateur ou mot de passe incorrect.',
          status: 'error',
          duration: 3000,
          isClosable: true,
        });
      } else if (res?.ok) {
        toast({
          title: 'Succès',
          description: 'Connexion réussie.',
          status: 'success',
          duration: 2000,
          isClosable: true,
        });
        // Redirect sẽ được xử lý bởi useEffect
      }
    } catch (error) {
      console.error('Login error:', error);
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
    <Box maxW="md" mx="auto" mt="20" p="6" borderWidth="1px" borderRadius="lg">
      <VStack spacing="4">
        <h1 className="text-2xl font-bold text-center mb-4">Connexion Admin</h1>
        <FormControl>
          <InputGroup>
            <InputLeftElement pointerEvents="none">
              {/* MailIcon SVG */}
            </InputLeftElement>
            <Input
              type="text"
              placeholder="Nom d'utilisateur"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              onKeyPress={handleKeyPress}
            />
          </InputGroup>
        </FormControl>
        <FormControl>
          <InputGroup>
            <InputLeftElement pointerEvents="none">
              {/* LockIcon SVG */}
            </InputLeftElement>
            <Input
              type="password"
              placeholder="Mot de passe"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              onKeyPress={handleKeyPress}
            />
          </InputGroup>
        </FormControl>
        <Button 
          colorScheme="blue" 
          onClick={handleLogin}
          isLoading={isLoading}
          loadingText="Connexion..."
          width="full"
        >
          Se connecter
        </Button>
      </VStack>
    </Box>
  );
}

export default LoginPage;
