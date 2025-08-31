import React, { useState, useEffect } from 'react';
import {
  Modal,
  ModalOverlay,
  ModalContent,
  ModalHeader,
  ModalFooter,
  ModalBody,
  ModalCloseButton,
  Button,
  VStack,
  HStack,
  Text,
  Box,
  useColorModeValue,
  Spinner,
  Alert,
  AlertIcon,
  useToast,
  Divider,
  Flex,
  useBreakpointValue
} from '@chakra-ui/react';
import { SettingsIcon } from '@chakra-ui/icons';
import apiClient from '@/lib/apiClient';
import { AllThresholdsResponse, ThresholdDetail } from '@/app/types/threshold.type';

interface ThresholdListProps {
  isOpen: boolean;
  onClose: () => void;
  deviceUuid: string;
  deviceName: string;
}

const ThresholdList: React.FC<ThresholdListProps> = ({
  isOpen,
  onClose,
  deviceUuid,
  deviceName,
}) => {
  const [thresholds, setThresholds] = useState<AllThresholdsResponse | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const toast = useToast();

  // Color mode values
  const bgColor = useColorModeValue('white', 'gray.800');
  const borderColor = useColorModeValue('gray.200', 'gray.600');
  const textColor = useColorModeValue('gray.900', 'white');
  const subtextColor = useColorModeValue('gray.600', 'gray.400');
  const previewBgColor = useColorModeValue('gray.50', 'gray.700');
  
  // Responsive values
  const modalSize = useBreakpointValue({ 
    base: 'full', 
    sm: 'xl',
    md: 'lg' 
  });
  
  // Badge background colors
  const criticalBgColor = useColorModeValue('red.100', 'red.900');
  const warningBgColor = useColorModeValue('yellow.100', 'yellow.900');
  const upperBgColor = useColorModeValue('blue.100', 'blue.900');
  const lowerBgColor = useColorModeValue('purple.100', 'purple.900');
  const humidityBgColor = useColorModeValue('green.100', 'green.900');
  const temperatureBgColor = useColorModeValue('teal.100', 'teal.900');

  // Fetch thresholds when modal opens
  useEffect(() => {
    if (isOpen && deviceUuid) {
      fetchThresholds();
    }
  }, [isOpen, deviceUuid]);

  const fetchThresholds = async () => {
    setIsLoading(true);
    setError(null);
    
    try {
      const response = await apiClient.getAllThresholds(deviceUuid);
      setThresholds(response);
    } catch (error) {
      console.error('Error fetching thresholds:', error);
      const errorMessage = error instanceof Error ? error.message : 'Failed to fetch thresholds';
      setError(errorMessage);
      
      toast({
        title: 'Erreur',
        description: 'Impossible de récupérer les seuils configurés',
        status: 'error',
        duration: 5000,
        isClosable: true,
      });
    } finally {
      setIsLoading(false);
    }
  };

  const getBadgeStyleForLevel = (level: string) => {
    return {
      bg: level === 'WARNING' ? warningBgColor : criticalBgColor,
      color: level === 'WARNING' ? 'yellow.800' : 'red.800',
      fontWeight: 'bold',
      px: 3,
      py: 1,
      borderRadius: 'md'
    };
  };

  const getBadgeStyleForType = (type: string) => {
    return {
      bg: type === 'UPPER' ? upperBgColor : lowerBgColor,
      color: type === 'UPPER' ? 'blue.800' : 'purple.800',
      fontWeight: 'bold',
      px: 3,
      py: 1,
      borderRadius: 'md'
    };
  };

  const getBadgeStyleForTelemetry = (telemetry: string) => {
    switch (telemetry.toLowerCase()) {
      case 'temperature':
        return {
          bg: temperatureBgColor,
          color: 'teal.800',
          fontWeight: 'bold',
          px: 3,
          py: 1,
          borderRadius: 'md'
        };
      case 'humidity':
        return {
          bg: humidityBgColor,
          color: 'green.800',
          fontWeight: 'bold',
          px: 3,
          py: 1,
          borderRadius: 'md'
        };
      default:
        return {
          bg: 'gray.100',
          color: 'gray.800',
          fontWeight: 'bold',
          px: 3,
          py: 1,
          borderRadius: 'md'
        };
    }
  };

  const getUnitForTelemetry = (telemetryName: string): string => {
    switch (telemetryName.toLowerCase()) {
      case 'temperature':
        return '°C';
      case 'humidity':
        return '%';
      default:
        return '';
    }
  };

  const handleClose = () => {
    setThresholds(null);
    setError(null);
    onClose();
  };

  return (
    <Modal 
      isOpen={isOpen} 
      onClose={handleClose} 
      size={modalSize}
      isCentered={false}
      scrollBehavior="inside"
      blockScrollOnMount={true}
      preserveScrollBarGap={true}
    >
      <ModalOverlay />
      <ModalContent 
        mx={4} 
        overflow="hidden"
        maxH="90vh"
        display="flex"
        flexDirection="column"
      >
        <ModalHeader>
          <HStack>
            <SettingsIcon />
            <VStack align="start" spacing={0}>
              <Text>
                Seuils configurés
              </Text>
              <Text fontSize="sm" color={subtextColor} fontWeight="normal">
                {deviceName}
              </Text>
            </VStack>
          </HStack>
        </ModalHeader>
        <ModalCloseButton />
        
        <ModalBody 
          pb={8}
          overflowY="auto"
          flex="1"
          minH="0"
          sx={{
            // Remove height: "100%" and maxHeight: calc(...) as flex will handle it
            webkitOverflowScrolling: "touch",
            touchAction: "pan-y",
            msOverflowStyle: "auto",
            scrollbarWidth: "auto",
            "&::-webkit-scrollbar": {
              width: "8px",
              borderRadius: "8px",
              backgroundColor: `rgba(0, 0, 0, 0.05)`,
            },
            "&::-webkit-scrollbar-thumb": {
              backgroundColor: `rgba(0, 0, 0, 0.15)`,
              borderRadius: "8px",
            },
      }}
        >
          {isLoading ? (
            <VStack spacing={4} py={8}>
              <Spinner size="lg" color="blue.500" thickness="4px" />
              <Text color={subtextColor}>Chargement des seuils configurés...</Text>
            </VStack>
          ) : error ? (
            <Alert status="error" borderRadius="md">
              <AlertIcon />
              <Box>
                <Text fontWeight="bold">Erreur de chargement</Text>
                <Text fontSize="sm">{error}</Text>
              </Box>
            </Alert>
          ) : !thresholds || Object.keys(thresholds).length === 0 ? (
            <Alert status="info" borderRadius="md">
              <AlertIcon />
              <Box>
                <Text fontWeight="bold">Aucun seuil configuré</Text>
                <Text fontSize="sm">Ce dispositif n&apos;a pas encore de seuils configurés.</Text>
              </Box>
            </Alert>
          ) : (
            <VStack spacing={6} align="stretch">
              {Object.entries(thresholds).map(([key, threshold]) => (
                <Box key={key} p={4} bg={previewBgColor} borderRadius="md" borderWidth="1px" borderColor={borderColor}>
                  <VStack align="stretch" spacing={3}>
                    <Text fontWeight="bold" fontSize="md">Aperçu:</Text>
                    
                    <Flex gap={2} wrap="wrap">
                      <Box sx={getBadgeStyleForLevel(threshold.thresholdLevel)}>
                        {threshold.thresholdLevel}
                      </Box>
                      <Box sx={getBadgeStyleForType(threshold.thresholdType)}>
                        {threshold.thresholdType}
                      </Box>
                      {Object.entries(threshold.threshold).map(([telemetryName, value]) => (
                        <Box key={telemetryName} sx={getBadgeStyleForTelemetry(telemetryName)}>
                          {telemetryName.toUpperCase()}: {value}{getUnitForTelemetry(telemetryName)}
                        </Box>
                      ))}
                    </Flex>
                    
                    {threshold.message ? (
                      <Box>
                        <Text fontSize="sm" fontWeight="medium">Message:</Text>
                        <Text fontSize="sm">{threshold.message}</Text>
                      </Box>
                    ) : (
                      <Box>
                        <Text fontSize="sm" fontWeight="medium">Message:</Text>
                        <Text fontSize="sm" fontStyle="italic" color={subtextColor}>
                          Message par défaut
                        </Text>
                      </Box>
                    )}
                    
                    <Divider />
                    
                    <HStack justify="space-between">
                      <Text fontSize="sm" color={subtextColor}>Client ID: {threshold.clientId}</Text>
                      <Text fontSize="sm" color={subtextColor}>
                        {threshold.thresholdLevel}-{threshold.thresholdType}
                      </Text>
                    </HStack>
                  </VStack>
                </Box>
              ))}
            </VStack>
          )}
        </ModalBody>

        <ModalFooter>
          <Button onClick={handleClose}>
            Fermer
          </Button>
        </ModalFooter>
      </ModalContent>
    </Modal>
  );
};

export default ThresholdList;