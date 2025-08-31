import React, { useEffect, useState } from 'react'
import {
  Box,
  Table,
  Thead,
  Tbody,
  Tr,
  Th,
  Td,
  Button,
  Modal,
  ModalOverlay,
  ModalContent,
  ModalHeader,
  ModalFooter,
  ModalBody,
  ModalCloseButton,
  useDisclosure,
  Text,
  VStack,
  HStack,
  Spinner,
  Alert,
  AlertIcon,
  useToast,
  Badge,
  Divider,
  useColorModeValue,
  TableContainer,
  Card,
  CardHeader,
  CardBody,
  Heading,
  useBreakpointValue,
  Stack,
  Flex,
  IconButton,
  SimpleGrid,
  Code,
  Input,
  FormControl,
  FormLabel,
  FormErrorMessage
} from '@chakra-ui/react';
import { AddIcon, DeleteIcon } from '@chakra-ui/icons';
import apiClient from '@/lib/apiClient';
import { AlertEmailsResponse, EmailOperationResponse } from '@/app/types/alerts.type';

function AlertEmailList() {
  const [isMounted, setIsMounted] = useState(false);
  const [emails, setEmails] = useState<string[]>([]);
  const [loading, setLoading] = useState(true);
  const [newEmail, setNewEmail] = useState('');
  const [emailError, setEmailError] = useState('');
  const { isOpen, onOpen, onClose } = useDisclosure();
  const toast = useToast();

  useEffect(() => {
    setIsMounted(true);
    fetchEmails();
  }, []);

  const fetchEmails = async () => {
    try {
      setLoading(true);
      console.log('Fetching emails from API...');
      const response = await apiClient.get<AlertEmailsResponse>('/auth/get-alert-mails');
      console.log('API response:', response);
      
      if (response.status === 'success') {
        console.log('Setting emails:', response.emails);
        setEmails(response.emails);
      } else {
        console.log('API response status was not success:', response.status);
        throw new Error('Failed to fetch emails');
      }
    } catch (error) {
      console.error('Error fetching emails:', error);
      
      let errorMessage = 'Impossible de charger la liste des emails';
      if (error instanceof Error) {
        errorMessage = error.message;
        console.log('Error message:', errorMessage);
      }
      
      // If it's an authentication error, don't show toast, just log it
      if (errorMessage.includes('Unauthorized')) {
        console.log('Authentication issue detected, not showing toast');
        return;
      }
      
      toast({
        title: 'Erreur',
        description: errorMessage,
        status: 'error',
        duration: 5000,
        isClosable: true,
      });
    } finally {
      setLoading(false);
    }
  };

  const validateEmail = (email: string): boolean => {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
  };

  const handleAddEmail = async () => {
    if (!newEmail.trim()) {
      setEmailError('L\'email est requis');
      return;
    }

    if (!validateEmail(newEmail)) {
      setEmailError('Format d\'email invalide');
      return;
    }

    // Remove client-side duplicate check - let server handle this
    // if (emails.includes(newEmail)) {
    //   setEmailError('Cet email existe déjà dans la liste');
    //   return;
    // }

    try {
      // Call the actual add email API
      const response = await apiClient.post<EmailOperationResponse>('/auth/add-alert-email', { email: newEmail });
      
      // Refresh the emails list from server to ensure consistency
      await fetchEmails();
      
      setNewEmail('');
      setEmailError('');
      onClose();
      
      toast({
        title: 'Succès',
        description: response.message || 'Email ajouté avec succès',
        status: 'success',
        duration: 3000,
        isClosable: true,
      });
    } catch (error) {
      console.error('Error adding email:', error);
      
      let errorMessage = 'Impossible d\'ajouter l\'email';
      
      // Handle specific error cases
      if (error instanceof Error) {
        if (error.message.includes('409') || error.message.includes('already exists') || error.message.includes('existe déjà')) {
          errorMessage = 'Cette adresse email existe déjà dans la liste des alertes';
        } else {
          errorMessage = error.message;
        }
      }
      
      toast({
        title: 'Erreur',
        description: errorMessage,
        status: 'error',
        duration: 5000,
        isClosable: true,
      });
    }
  };

  const handleDeleteEmail = async (emailToDelete: string) => {
    try {
      // Call the actual delete API with proper typing using DELETE method
      const response = await apiClient.delete<EmailOperationResponse>('/auth/delete-alert-email', {
        data: { email: emailToDelete }
      });
      
      // Refresh the emails list from server to ensure consistency
      await fetchEmails();
      
      toast({
        title: 'Succès',
        description: response.message || 'Email supprimé avec succès',
        status: 'success',
        duration: 3000,
        isClosable: true,
      });
    } catch (error) {
      console.error('Error deleting email:', error);
      toast({
        title: 'Erreur',
        description: 'Impossible de supprimer l\'email',
        status: 'error',
        duration: 5000,
        isClosable: true,
      });
    }
  };

  const handleModalClose = () => {
    setNewEmail('');
    setEmailError('');
    onClose();
  };

    // Color mode values - always call hooks to avoid Rules of Hooks violations
    const bgColorValue = useColorModeValue('white', 'gray.800');
    const borderColorValue = useColorModeValue('gray.200', 'gray.600');
    const textColorValue = useColorModeValue('gray.900', 'white');
    const subtextColorValue = useColorModeValue('gray.600', 'gray.400');
    const codeBackgroundColorValue = useColorModeValue('gray.100', 'gray.700');
    const successCodeBgValue = useColorModeValue('green.50', 'green.900');
    const successCodeColorValue = useColorModeValue('green.800', 'green.200');
    const successCodeBorderValue = useColorModeValue('green.200', 'green.600');

    // Use mounted state to prevent hydration issues
    const bgColor = isMounted ? bgColorValue : 'white';
    const borderColor = isMounted ? borderColorValue : 'gray.200';
    const textColor = isMounted ? textColorValue : 'gray.900';
    const subtextColor = isMounted ? subtextColorValue : 'gray.600';
    const codeBackgroundColor = isMounted ? codeBackgroundColorValue : 'gray.100';
    const successCodeBg = isMounted ? successCodeBgValue : 'green.50';
    const successCodeColor = isMounted ? successCodeColorValue : 'green.800';
    const successCodeBorder = isMounted ? successCodeBorderValue : 'green.200';
    return (
        <Box p={{ base: 2, md: 4 }}>
            <Card>
                <CardHeader pb={{ base: 2, md: 4 }}>
                    <Flex 
                        direction={{ base: 'column', sm: 'row' }} 
                        justify="space-between" 
                        align={{ base: 'stretch', sm: 'center' }}
                        gap={{ base: 3, sm: 0 }}
                    >
                        <Heading size={{ base: 'sm', md: 'md' }}>
                            Liste des emails pour les alertes
                        </Heading>
                        <Button
                            leftIcon={<AddIcon />}
                            colorScheme="blue"
                            onClick={onOpen}
                            size={{ base: 'sm', md: 'sm' }}
                            width={{ base: 'full', sm: 'auto' }}
                        >
                            Ajouter un email
                        </Button>
                    </Flex>
                </CardHeader>
                
                <CardBody pt={0}>
                    {loading ? (
                        <Flex justify="center" align="center" py={8}>
                            <Spinner size="lg" />
                            <Text ml={4} fontSize={{ base: 'sm', md: 'md' }}>
                                Chargement des emails...
                            </Text>
                        </Flex>
                    ) : emails.length === 0 ? (
                        <Text 
                            textAlign="center" 
                            color={subtextColor} 
                            py={8}
                            fontSize={{ base: 'sm', md: 'md' }}
                        >
                            Aucun email configuré pour les alertes
                        </Text>
                    ) : (
                        // Mobile-first design: Stack layout for mobile, table for desktop
                        <Box>
                            {/* Mobile Layout */}
                            <Box display={{ base: 'block', md: 'none' }}>
                                <VStack spacing={3} align="stretch">
                                    {emails.map((email, index) => (
                                        <Box
                                            key={index}
                                            p={4}
                                            borderWidth={1}
                                            borderColor={borderColor}
                                            borderRadius="md"
                                            bg={bgColor}
                                        >
                                            <Flex justify="space-between" align="center">
                                                <Box flex={1} mr={3}>
                                                    <Text 
                                                        fontSize="sm" 
                                                        fontWeight="medium"
                                                        color={textColor}
                                                        wordBreak="break-all"
                                                    >
                                                        {email}
                                                    </Text>
                                                </Box>
                                                <IconButton
                                                    aria-label="Supprimer l'email"
                                                    icon={<DeleteIcon />}
                                                    size="sm"
                                                    colorScheme="red"
                                                    variant="ghost"
                                                    onClick={() => handleDeleteEmail(email)}
                                                    flexShrink={0}
                                                />
                                            </Flex>
                                        </Box>
                                    ))}
                                </VStack>
                            </Box>
                            
                            {/* Desktop Layout */}
                            <Box display={{ base: 'none', md: 'block' }}>
                                <TableContainer>
                                    <Table variant="simple">
                                        <Thead>
                                            <Tr>
                                                <Th>Email</Th>
                                                <Th width="100px">Actions</Th>
                                            </Tr>
                                        </Thead>
                                        <Tbody>
                                            {emails.map((email, index) => (
                                                <Tr key={index}>
                                                    <Td>
                                                        <Text>{email}</Text>
                                                    </Td>
                                                    <Td>
                                                        <IconButton
                                                            aria-label="Supprimer l'email"
                                                            icon={<DeleteIcon />}
                                                            size="sm"
                                                            colorScheme="red"
                                                            variant="ghost"
                                                            onClick={() => handleDeleteEmail(email)}
                                                        />
                                                    </Td>
                                                </Tr>
                                            ))}
                                        </Tbody>
                                    </Table>
                                </TableContainer>
                            </Box>
                        </Box>
                    )}
                </CardBody>
            </Card>

            {/* Add Email Modal */}
            <Modal 
              isOpen={isOpen} 
              onClose={handleModalClose} 
              size={{ base: 'full', md: 'md' }}
              blockScrollOnMount={true}
              preserveScrollBarGap={true}
            >
                <ModalOverlay />
                <ModalContent mx={{ base: 0, md: 4 }} my={{ base: 0, md: 4 }}>
                    <ModalHeader fontSize={{ base: 'lg', md: 'xl' }}>
                        Ajouter un email d'alerte
                    </ModalHeader>
                    <ModalCloseButton />
                    <ModalBody pb={6}>
                        <FormControl isInvalid={!!emailError}>
                            <FormLabel fontSize={{ base: 'sm', md: 'md' }}>
                                Adresse email
                            </FormLabel>
                            <Input
                                type="email"
                                placeholder="exemple@domaine.com"
                                value={newEmail}
                                onChange={(e) => {
                                    setNewEmail(e.target.value);
                                    setEmailError('');
                                }}
                                onKeyPress={(e) => {
                                    if (e.key === 'Enter') {
                                        handleAddEmail();
                                    }
                                }}
                                size={{ base: 'md', md: 'md' }}
                            />
                            <FormErrorMessage fontSize="sm">{emailError}</FormErrorMessage>
                        </FormControl>
                    </ModalBody>

                    <ModalFooter>
                        <Button 
                            variant="ghost" 
                            mr={3} 
                            onClick={handleModalClose}
                            size={{ base: 'md', md: 'md' }}
                        >
                            Annuler
                        </Button>
                        <Button 
                            colorScheme="blue" 
                            onClick={handleAddEmail}
                            size={{ base: 'md', md: 'md' }}
                        >
                            Ajouter
                        </Button>
                    </ModalFooter>
                </ModalContent>
            </Modal>
        </Box>
    )
}

export default AlertEmailList