'use client';

import React, { useState, useEffect, useCallback, forwardRef, useImperativeHandle } from 'react';
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
  Code
} from '@chakra-ui/react';
import { AiOutlineEye, AiOutlineReload, AiOutlineMore, AiOutlineDelete, AiOutlineUnorderedList } from 'react-icons/ai';
import { FiSettings, FiPlus } from 'react-icons/fi';
import apiClient, { PasswordResetResponse, DeviceDeleteResponse } from '@/lib/apiClient';
import ThresholdConfig from "./ThresholdConfig";
import ThresholdList from "./ThresholdList";

/**
 * Interface for device data structure
 */
interface Device {
  device_uuid: string;
  device_name: string;
  mqtt_username: string;
  client_id: string;
}

/**
 * Device card component for mobile view without truncation
 * @param {Object} props - Component props
 * @param {Device} props.device - Device data
 * @param {Function} props.onMoreInfo - Callback for more info action
 * @returns {JSX.Element} DeviceCard component
 */
function DeviceCard({ device, onMoreInfo }: { device: Device; onMoreInfo: (device: Device) => void }) {
  // Use static defaults for SSR compatibility
  const bgColor = 'white';
  const borderColor = 'gray.200';
  const textColor = 'gray.900';
  const subtextColor = 'gray.600';

  return (
    <Card bg={bgColor} borderColor={borderColor} shadow="sm" size="sm">
      <CardBody>
        <VStack align="stretch" spacing={3}>
          <HStack justify="space-between" align="start">
            <VStack align="start" spacing={1} flex={1}>
              <Text fontSize="md" fontWeight="bold" color={textColor}>
                {device.device_name}
              </Text>
              <Text fontSize="sm" color={subtextColor}>
                {device.mqtt_username}
              </Text>
            </VStack>
            <IconButton
              aria-label="Plus d'informations"
              icon={<AiOutlineMore />}
              size="sm"
              variant="ghost"
              colorScheme="blue"
              onClick={() => onMoreInfo(device)}
            />
          </HStack>
          
          <Divider />
          
          <HStack justify="space-between" align="center">
            <Text fontSize="xs" color={subtextColor}>
              ID: {device.client_id}
            </Text>
            <Button
              size="xs"
              colorScheme="blue"
              variant="outline"
              leftIcon={<AiOutlineEye />}
              onClick={() => onMoreInfo(device)}
            >
              Détails
            </Button>
          </HStack>
        </VStack>
      </CardBody>
    </Card>
  );
}

/**
 * Interface for ref methods
 */
interface DeviceListRef {
  refreshIfLastPage: () => void;
}

/**
 * DeviceList component with forwardRef - displays list of devices with details modal
 * @param props - Component props
 * @param ref - Ref object for exposing methods to parent
 * @returns JSX element for device list
 */
const DeviceList = forwardRef<DeviceListRef>((props, ref) => {
  const [devices, setDevices] = useState<Device[]>([]);
  const [selectedDevice, setSelectedDevice] = useState<Device | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [resetLoading, setResetLoading] = useState(false);
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [passwordResetResult, setPasswordResetResult] = useState<PasswordResetResponse | null>(null);
  const [isMounted, setIsMounted] = useState(false);
  
  // Pagination states
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [hasNext, setHasNext] = useState(false);
  const [hasPrevious, setHasPrevious] = useState(false);
  const pageSize = 10;
  
  const { isOpen, onOpen, onClose } = useDisclosure();
  const { 
    isOpen: isResetModalOpen, 
    onOpen: onResetModalOpen, 
    onClose: onResetModalClose 
  } = useDisclosure();
  const { 
    isOpen: isThresholdModalOpen, 
    onOpen: onThresholdModalOpen, 
    onClose: onThresholdModalClose 
  } = useDisclosure();
  const { 
    isOpen: isAddThresholdModalOpen, 
    onOpen: onAddThresholdModalOpen, 
    onClose: onAddThresholdModalClose 
  } = useDisclosure();
  const { 
    isOpen: isThresholdListModalOpen, 
    onOpen: onThresholdListModalOpen, 
    onClose: onThresholdListModalClose 
  } = useDisclosure();
  const toast = useToast();
  
  // Prevent hydration mismatch
  useEffect(() => {
    setIsMounted(true);
  }, []);
  
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
  
  // Responsive values
  const isMobile = useBreakpointValue({ base: true, md: false });
  const tableSize = useBreakpointValue({ base: 'sm', md: 'md' });
  const buttonSize = useBreakpointValue({ base: 'xs', md: 'sm' });
  const modalSize = useBreakpointValue({ base: 'full', md: 'md' });
  const gridColumns = useBreakpointValue({ base: 1, sm: 2, lg: 3 });

  /**
   * Fetches device list from API with pagination
   * @param {number} page - Page number to fetch
   * @param {number} size - Number of items per page
   */
  const fetchDevices = useCallback(async (page: number = 0, size: number = pageSize) => {
    try {
        setLoading(true);
        setError(null);
        const response = await apiClient.getDevices(page, size);
        setDevices(response.data);
        setCurrentPage(response.page);
        setTotalPages(response.total_pages);
        setTotalElements(response.total_elements);
        setHasNext(response.has_next);
        setHasPrevious(response.has_previous);
    } catch (err: unknown) {
        console.error('Error fetching devices:', err);
        setError('Erreur lors du chargement des appareils');
        toast({
            title: 'Erreur',
            description: 'Impossible de charger la liste des appareils',
            status: 'error',
            duration: 3000,
            isClosable: true,
            position: 'top'
        });
    } finally {
        setLoading(false);
    }
  }, [pageSize, toast]);

  /**
   * Handles page change
   * @param {number} newPage - New page number
   */
  const handlePageChange = (newPage: number) => {
    if (newPage >= 0 && newPage < totalPages) {
      fetchDevices(newPage, pageSize);
    }
  };

  /**
   * Handles device deletion
   * @param {string} deviceUuid - UUID of the device to delete
   */
  const handleDeleteDevice = async (deviceUuid: string) => {
    try {
      setDeleteLoading(true);
      
      // Show confirmation dialog
      const confirmed = window.confirm(
        'Êtes-vous sûr de vouloir supprimer définitivement cet appareil ? Cette action est irréversible.'
      );
      
      if (!confirmed) {
        setDeleteLoading(false);
        return;
      }
      
      await apiClient.deleteDevice(deviceUuid);
      
      toast({
        title: 'Succès',
        description: 'Appareil supprimé avec succès',
        status: 'success',
        duration: 3000,
        isClosable: true,
        position: 'top'
      });
      
      // Close modal and refresh device list
      onClose();
      fetchDevices(currentPage, pageSize);
      
    } catch (error: unknown) {
      console.error('Device deletion error:', error);
      const errorMessage = error instanceof Error ? error.message : 'Échec de la suppression de l\'appareil';
      
      toast({
        title: 'Erreur',
        description: errorMessage,
        status: 'error',
        duration: 5000,
        isClosable: true,
        position: 'top'
      });
    } finally {
      setDeleteLoading(false);
    }
  };

  /**
   * Handles device password reset
   * @param {string} deviceUuid - UUID of the device to reset password
   */
  const handleResetPassword = async (deviceUuid: string) => {
    try {
      setResetLoading(true);
      const result = await apiClient.resetDevicePassword(deviceUuid);
      
      setPasswordResetResult(result);
      closeAllModals();
      // Small delay to ensure modals are closed before opening new one
      setTimeout(() => {
        onResetModalOpen();
      }, 100);
      
      toast({
        title: 'Succès',
        description: 'Mot de passe réinitialisé avec succès',
        status: 'success',
        duration: 3000,
        isClosable: true,
        position: 'top'
      });
      
    } catch (error: unknown) {
      console.error('Password reset error:', error);
      const errorMessage = error instanceof Error ? error.message : 'Failed to reset password';
      
      toast({
        title: 'Erreur',
        description: errorMessage,
        status: 'error',
        duration: 5000,
        isClosable: true,
        position: 'top'
      });
    } finally {
      setResetLoading(false);
    }
  };

  /**
   * Handles device info modal opening
   * @param device - Device object to display
   */
  const handleMoreInfo = (device: Device) => {
    closeAllModals();
    setSelectedDevice(device);
    // Small delay to ensure modals are closed before opening new one
    setTimeout(() => {
      onOpen();
    }, 100);
  };

  /**
   * Closes all modals to prevent stacking
   */
  const closeAllModals = () => {
    onClose();
    onThresholdModalClose();
    onAddThresholdModalClose();
    onThresholdListModalClose();
    onResetModalClose();
  };

  /**
   * Handles threshold configuration modal opening
   * @param device - Device object to configure threshold for
   */
  const handleThresholdConfig = (device: Device) => {
    closeAllModals();
    setSelectedDevice(device);
    // Small delay to ensure modals are closed before opening new one
    setTimeout(() => {
      onThresholdModalOpen();
    }, 100);
  };

  /**
   * Handles add new threshold modal opening
   * @param device - Device object to add a threshold for
   */
  const handleAddThreshold = (device: Device) => {
    closeAllModals();
    setSelectedDevice(device);
    // Small delay to ensure modals are closed before opening new one
    setTimeout(() => {
      onAddThresholdModalOpen();
    }, 100);
  };

  /**
   * Handles threshold list modal opening
   * @param device - Device object to view thresholds for
   */
  const handleViewThresholds = (device: Device) => {
    closeAllModals();
    setSelectedDevice(device);
    // Small delay to ensure modals are closed before opening new one
    setTimeout(() => {
      onThresholdListModalOpen();
    }, 100);
  }

  /**
   * Refreshes device list only if currently on the last page
   */
  const refreshIfLastPage = () => {
    // Check if we're on the last page or if there's only one page
    if (!hasNext || totalPages <= 1) {
      fetchDevices(currentPage, pageSize);
    }
  };

  // Expose methods to parent component
  useImperativeHandle(ref, () => ({
    refreshIfLastPage
  }));

  // Load devices on component mount
  useEffect(() => {
    fetchDevices();
  }, [fetchDevices]);

  if (loading) {
    return (
      <Box p={2}>
        <Card bg={bgColor} shadow="sm" borderColor={borderColor}>
          <CardHeader>
            <Heading size="md" color={textColor}>Liste des appareils</Heading>
          </CardHeader>
          <CardBody>
            <VStack spacing={4}>
              <Spinner size="lg" color="blue.500" thickness="4px" />
              <Text color={subtextColor}>Chargement des appareils...</Text>
            </VStack>
          </CardBody>
        </Card>
      </Box>
    );
  }

  if (error) {
    return (
      <Box maxW="600px" mx="auto" p={6}>
        <Card bg={bgColor} shadow="sm" borderColor={borderColor}>
          <CardHeader>
            <Heading size="md" color={textColor}>Liste des appareils</Heading>
          </CardHeader>
          <CardBody>
            <Alert status="error">
              <AlertIcon />
              {error}
            </Alert>
            <Button
              mt={4}
              onClick={() => fetchDevices()}
              colorScheme="blue"
              size={buttonSize}
              leftIcon={<AiOutlineReload />}
              width={isMobile ? "full" : "auto"}
            >
              Réessayer
            </Button>
          </CardBody>
        </Card>
      </Box>
    );
  }

  return (
    <Box p={2}>
      <Card bg={bgColor}>
        <CardHeader>
          <Flex justify="space-between" align="center" wrap="wrap" gap={2}>
            <Heading size={isMobile ? "sm" : "md"} color={textColor}>
              Liste des appareils
            </Heading>
            <Badge colorScheme="blue" variant="subtle" fontSize={isMobile ? "xs" : "sm"}>
              {devices.length} appareil{devices.length !== 1 ? 's' : ''}
            </Badge>
          </Flex>
        </CardHeader>
        <CardBody>
          {devices.length === 0 ? (
            <Text color={subtextColor} textAlign="center" py={8}>
              Aucun appareil trouvé
            </Text>
          ) : (
            <>
              {/* Mobile Card View */}
              {isMobile ? (
                <SimpleGrid columns={gridColumns} spacing={4}>
                  {devices.map((device) => (
                    <DeviceCard
                      key={device.device_uuid}
                      device={device}
                      onMoreInfo={handleMoreInfo}
                    />
                  ))}
                </SimpleGrid>
              ) : (
                /* Desktop Table View */
                <TableContainer>
                  <Table variant="simple" size={tableSize}>
                    <Thead>
                      <Tr>
                        <Th color={subtextColor}>Nom de l&apos;appareil</Th>
                        <Th color={subtextColor}>Nom d&apos;utilisateur MQTT</Th>
                        <Th color={subtextColor} textAlign="center">Actions</Th>
                      </Tr>
                    </Thead>
                    <Tbody>
                      {devices.map((device) => (
                        <Tr key={device.device_uuid}>
                          <Td color={textColor} fontWeight="medium">
                            {device.device_name}
                          </Td>
                          <Td color={textColor}>
                            {device.mqtt_username}
                          </Td>
                          <Td textAlign="center">
                            <Button
                              size={buttonSize}
                              colorScheme="blue"
                              variant="outline"
                              leftIcon={<AiOutlineEye />}
                              onClick={() => handleMoreInfo(device)}
                            >
                              Plus d&apos;infos
                            </Button>
                          </Td>
                        </Tr>
                      ))}
                    </Tbody>
                  </Table>
                </TableContainer>
              )}
              
              {/* Pagination */}
                <Flex 
                  justify="space-between" 
                  align="center" 
                  mt={6} 
                  pt={4} 
                  borderTop="1px" 
                  borderColor={borderColor}
                  flexDirection={isMobile ? "column" : "row"}
                  gap={isMobile ? 4 : 0}
                >
                  <Text fontSize="sm" color={subtextColor}>
                    Page {currentPage + 1} sur {totalPages} ({totalElements} appareils au total)
                  </Text>
                  
                  <HStack spacing={2}>
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => handlePageChange(currentPage - 1)}
                      isDisabled={!hasPrevious || loading}
                    >
                      Précédent
                    </Button>
                    
                    {/* Page numbers */}
                    {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
                      let pageNum: number;
                      if (totalPages <= 5) {
                        pageNum = i;
                      } else if (currentPage < 3) {
                        pageNum = i;
                      } else if (currentPage > totalPages - 4) {
                        pageNum = totalPages - 5 + i;
                      } else {
                        pageNum = currentPage - 2 + i;
                      }
                      
                      return (
                        <Button
                          key={pageNum}
                          size="sm"
                          variant={currentPage === pageNum ? "solid" : "outline"}
                          colorScheme={currentPage === pageNum ? "blue" : "gray"}
                          onClick={() => handlePageChange(pageNum)}
                          isDisabled={loading}
                        >
                          {pageNum + 1}
                        </Button>
                      );
                    })}
                    
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => handlePageChange(currentPage + 1)}
                      isDisabled={!hasNext || loading}
                    >
                      Suivant
                    </Button>
                  </HStack>
                </Flex>
              
            </>
          )}
        </CardBody>
      </Card>

      {/* Device Details Modal */}
      <Modal 
        isOpen={isOpen} 
        onClose={onClose} 
        size={modalSize}
        blockScrollOnMount={true}
        preserveScrollBarGap={true}
      >
        <ModalOverlay />
        <ModalContent mx={isMobile ? 4 : 0}>
          <ModalHeader fontSize={isMobile ? "md" : "lg"}>
            Détails de l&apos;appareil
          </ModalHeader>
          <ModalCloseButton />
          <ModalBody pb={3}>
            {selectedDevice && (
              <VStack spacing={3} align="stretch">
                <Box>
                  <Text fontSize="sm" color={subtextColor} mb={1}>
                    UUID de l&apos;appareil
                  </Text>
                  <Text 
                    fontWeight="medium" 
                    wordBreak="break-all"
                    fontSize={isMobile ? "sm" : "md"}
                  >
                    {selectedDevice.device_uuid}
                  </Text>
                </Box>
                
                <Divider />
                
                <Box>
                  <Text fontSize="sm" color={subtextColor} mb={1}>
                    Nom de l&apos;appareil
                  </Text>
                  <Text fontWeight="medium" fontSize={isMobile ? "sm" : "md"}>
                    {selectedDevice.device_name}
                  </Text>
                </Box>
                
                <Divider />
                
                <Box>
                  <Text fontSize="sm" color={subtextColor} mb={1}>
                    Nom d&apos;utilisateur MQTT
                  </Text>
                  <Text fontWeight="medium" fontSize={isMobile ? "sm" : "md"}>
                    {selectedDevice.mqtt_username}
                  </Text>
                </Box>
                
                <Divider />
                
                <Box>
                  <Text fontSize="sm" color={subtextColor} mb={1}>
                    ID Client
                  </Text>
                  <Text 
                    fontWeight="medium" 
                    wordBreak="break-all"
                    fontSize={isMobile ? "sm" : "md"}
                  >
                    {selectedDevice.client_id}
                  </Text>
                </Box>
              </VStack>
            )}
          </ModalBody>

          <ModalFooter>
            <Stack 
              direction="column"
              spacing={2} 
              width="full"
            >
              <Button
                colorScheme="blue"
                variant="outline"
                onClick={() => selectedDevice && handleAddThreshold(selectedDevice)}
                leftIcon={<FiSettings />}
                size="xs"
                fontSize="xs"
                width="full"
              >
                Configurer les seuils
              </Button>
              
              <Button
                colorScheme="teal"
                variant="outline"
                onClick={() => selectedDevice && handleViewThresholds(selectedDevice)}
                leftIcon={<AiOutlineUnorderedList />}
                size="xs"
                fontSize="xs"
                width="full"
              >
                Voir les seuils configurés
              </Button>
              
              <Button
                colorScheme="red"
                variant="outline"
                onClick={() => selectedDevice && handleResetPassword(selectedDevice.device_uuid)}
                isLoading={resetLoading}
                loadingText="Réinitialisation..."
                leftIcon={<AiOutlineReload />}
                size="xs"
                fontSize="xs"
                width="full"
              >
                Réinitialiser le mot de passe
              </Button>
              
              <Button
                colorScheme="red"
                variant="solid"
                onClick={() => selectedDevice && handleDeleteDevice(selectedDevice.device_uuid)}
                isLoading={deleteLoading}
                loadingText="Suppression..."
                leftIcon={<AiOutlineDelete />}
                size="xs"
                fontSize="xs"
                width="full"
              >
                Supprimer l&apos;appareil
              </Button>
              
              <Button 
                onClick={onClose}
                size="xs"
                fontSize="xs"
                width="full"
              >
                Fermer
              </Button>
            </Stack>
          </ModalFooter>
        </ModalContent>
      </Modal>

      {/* Threshold Configuration Modal */}
      {selectedDevice && (
        <ThresholdConfig
          isOpen={isThresholdModalOpen}
          onClose={onThresholdModalClose}
          deviceUuid={selectedDevice.device_uuid}
          deviceName={selectedDevice.device_name}
          mode="configure"
        />
      )}

      {/* Add New Threshold Modal */}
      {selectedDevice && (
        <ThresholdConfig
          isOpen={isAddThresholdModalOpen}
          onClose={onAddThresholdModalClose}
          deviceUuid={selectedDevice.device_uuid}
          deviceName={selectedDevice.device_name}
          mode="add"
        />
      )}

      {/* Threshold List Modal */}
      {selectedDevice && (
        <ThresholdList
          isOpen={isThresholdListModalOpen}
          onClose={onThresholdListModalClose}
          deviceUuid={selectedDevice.device_uuid}
          deviceName={selectedDevice.device_name}
        />
      )}

      {/* Password Reset Result Modal */}
      <Modal 
        isOpen={isResetModalOpen} 
        onClose={onResetModalClose} 
        size={modalSize}
        blockScrollOnMount={true}
        preserveScrollBarGap={true}
      >
        <ModalOverlay />
        <ModalContent mx={isMobile ? 4 : 0}>
          <ModalHeader fontSize={isMobile ? "lg" : "xl"} color="green.600">
            Réinitialisation du mot de passe réussie
          </ModalHeader>
          <ModalCloseButton />
          <ModalBody>
            {passwordResetResult && (
              <VStack spacing={4} align="stretch">
                <Alert status="success" borderRadius="md">
                  <AlertIcon />
                  <Box>
                    <Text fontWeight="bold">{passwordResetResult.message}</Text>
                  </Box>
                </Alert>
                
                <Box>
                  <Text fontSize="sm" color={subtextColor} mb={2}>
                    ID Client
                  </Text>
                  <Code 
                    p={3} 
                    borderRadius="md" 
                    fontSize={isMobile ? "xs" : "sm"}
                    wordBreak="break-all"
                    width="full"
                    bg={codeBackgroundColor}
                  >
                    {passwordResetResult.client_id}
                  </Code>
                </Box>
                
                <Box>
                  <Text fontSize="sm" color={subtextColor} mb={2}>
                    Nouveau mot de passe
                  </Text>
                  <Code 
                    p={3} 
                    borderRadius="md" 
                    fontSize={isMobile ? "sm" : "md"}
                    fontWeight="bold"
                    width="full"
                    bg={successCodeBg}
                    color={successCodeColor}
                    border="1px solid"
                    borderColor={successCodeBorder}
                  >
                    {passwordResetResult.new_password}
                  </Code>
                </Box>
                
                <Alert status="info" borderRadius="md">
                  <AlertIcon />
                  <Box>
                    <Text fontSize="sm">
                      Veuillez sauvegarder ce mot de passe en sécurité. Vous ne pourrez plus le voir après.
                    </Text>
                  </Box>
                </Alert>
              </VStack>
            )}
          </ModalBody>

          <ModalFooter>
            <Button 
              onClick={() => {
                onResetModalClose();
                setPasswordResetResult(null);
              }}
              colorScheme="green"
              size="xs"
              fontSize="xs"
              width={isMobile ? "full" : "auto"}
            >
              Fermer
            </Button>
          </ModalFooter>
        </ModalContent>
      </Modal>
    </Box>
  );
});

DeviceList.displayName = 'DeviceList';

export default DeviceList;