'use client';

import {
  Box,
  Button,
  FormControl,
  FormLabel,
  Input,
  VStack,
  Heading,
  Alert,
  AlertIcon,
  AlertTitle,
  AlertDescription,
  Text,
  useToast,
  Card,
  CardBody,
  CardHeader,
  Divider,
  Code,
  HStack,
  IconButton,
  Grid,
  GridItem,
} from '@chakra-ui/react';
import { useState } from 'react';
import { AiOutlineEye, AiOutlineEyeInvisible } from 'react-icons/ai';
import apiClient, { CreateDeviceResponse } from '@/lib/apiClient';

/**
 * Component for creating new device with MQTT credentials
 * @returns {JSX.Element} CreateDeviceForm component
 */
export default function CreateDeviceForm() {
  const [deviceName, setDeviceName] = useState('');
  const [mqttUsername, setMqttUsername] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [deviceResponse, setDeviceResponse] = useState<CreateDeviceResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [showPassword, setShowPassword] = useState(false);
  const toast = useToast();

  /**
   * Handles form submission to create a new device
   * @param {React.FormEvent} e - Form event
   */
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!deviceName.trim() || !mqttUsername.trim()) {
      setError('Veuillez remplir tous les champs');
      return;
    }

    setIsLoading(true);
    setError(null);
    setDeviceResponse(null);
    setShowPassword(false); // Reset password visibility

    try {
      const response = await apiClient.createDevice({
        device_name: deviceName.trim(),
        mqtt_username: mqttUsername.trim(),
      });

      setDeviceResponse(response);
      setDeviceName('');
      setMqttUsername('');
      
      toast({
        title: 'Succès',
        description: 'Le nouveau compte a été créé avec succès',
        status: 'success',
        duration: 3000,
        isClosable: true,
      });
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Erreur inconnue';
      setError(errorMessage);
      
      toast({
        title: 'Erreur',
        description: errorMessage,
        status: 'error',
        duration: 5000,
        isClosable: true,
      });
    } finally {
      setIsLoading(false);
    }
  };

  /**
   * Resets the form and clears results
   */
  const handleReset = () => {
    setDeviceResponse(null);
    setError(null);
    setDeviceName('');
    setMqttUsername('');
    setShowPassword(false);
  };

  /**
   * Toggles password visibility
   */
  const togglePasswordVisibility = () => {
    setShowPassword(!showPassword);
  };

  /**
   * Masks password with asterisks
   * @param {string} password - Original password
   * @returns {string} Masked password
   */
  const maskPassword = (password: string): string => {
    return '*'.repeat(password.length);
  };

  return (
    <Box maxW="600px" mx="auto" p={6}>
      <Card>
        <CardHeader>
          <Heading size="lg" textAlign="center">
            Créer un compte MQTT pour un device
          </Heading>
        </CardHeader>
        
        <CardBody>
          <form onSubmit={handleSubmit}>
            <VStack spacing={4}>
              <FormControl isRequired>
                <FormLabel>Nom du device</FormLabel>
                <Input
                  type="text"
                  value={deviceName}
                  onChange={(e) => setDeviceName(e.target.value)}
                  placeholder="Nom du device"
                  disabled={isLoading}
                />
              </FormControl>

              <FormControl isRequired>
                <FormLabel>Nom d&apos;utilisateur MQTT</FormLabel>
                <Input
                  type="text"
                  value={mqttUsername}
                  onChange={(e) => setMqttUsername(e.target.value)}
                  placeholder="Nom d'utilisateur MQTT"
                  disabled={isLoading}
                />
              </FormControl>

              <Button
                type="submit"
                colorScheme="blue"
                size="lg"
                width="full"
                isLoading={isLoading}
                loadingText="Création en cours..."
              >
                Créer le device
              </Button>
            </VStack>
          </form>

          {error && (
            <Alert status="error" mt={4}>
              <AlertIcon />
              <AlertTitle>Erreur!</AlertTitle>
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          )}

          {deviceResponse && (
            <Box mt={6}>
              <Divider mb={4} />
              <Alert status="success" mb={4}>
                <AlertIcon />
                <AlertTitle>Succès!</AlertTitle>
                <AlertDescription>
                  Le compte a été créé avec succès. Voici les informations de connexion MQTT:
                </AlertDescription>
              </Alert>
              
              <Card variant="outline">
                <CardBody>
                  <Grid templateColumns="1fr 2fr" gap={4} alignItems="center">
                    <GridItem>
                      <Text fontWeight="bold">Nom du device:</Text>
                    </GridItem>
                    <GridItem>
                      <Code width="100%" p={2}>{deviceResponse.device_name}</Code>
                    </GridItem>
                    
                    <GridItem>
                      <Text fontWeight="bold">MQTT Username:</Text>
                    </GridItem>
                    <GridItem>
                      <Code width="100%" p={2}>{deviceResponse.mqtt_username}</Code>
                    </GridItem>
                    
                    <GridItem>
                      <Text fontWeight="bold">MQTT Password:</Text>
                    </GridItem>
                    <GridItem>
                      <HStack spacing={2} width="100%">
                        <Code colorScheme="red" flex={1} p={2}>
                          {showPassword 
                            ? deviceResponse.mqtt_password 
                            : maskPassword(deviceResponse.mqtt_password)
                          }
                        </Code>
                        <IconButton
                          aria-label={showPassword ? 'Masquer le mot de passe' : 'Afficher le mot de passe'}
                          icon={showPassword ? <AiOutlineEyeInvisible /> : <AiOutlineEye />}
                          size="sm"
                          variant="ghost"
                          onClick={togglePasswordVisibility}
                          flexShrink={0}
                        />
                      </HStack>
                    </GridItem>
                    
                    <GridItem>
                      <Text fontWeight="bold">Client ID:</Text>
                    </GridItem>
                    <GridItem>
                      <Code width="100%" p={2}>{deviceResponse.client_id}</Code>
                    </GridItem>
                  </Grid>
                </CardBody>
              </Card>
              
              <Button
                mt={4}
                variant="outline"
                onClick={handleReset}
                width="full"
              >
                Créer un nouveau device
              </Button>
            </Box>
          )}
        </CardBody>
      </Card>
    </Box>
  );
}