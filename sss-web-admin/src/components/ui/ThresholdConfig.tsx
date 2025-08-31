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
  FormControl,
  FormLabel,
  FormErrorMessage,
  Select,
  NumberInput,
  NumberInputField,
  NumberInputStepper,
  NumberIncrementStepper,
  NumberDecrementStepper,
  Textarea,
  useToast,
  Divider,
  Text,
  Box,
  useColorModeValue,
  Spinner,
  Alert,
  AlertIcon,
  IconButton,
  Tooltip,
  Badge,
  Checkbox,
  Stack
} from '@chakra-ui/react';
import { SettingsIcon, InfoIcon } from '@chakra-ui/icons';
import apiClient from '@/lib/apiClient';
import { 
  ThresholdConfigRequest, 
  ThresholdConfigResponse, 
  ThresholdLevel, 
  ThresholdType,
  TelemetryValuesResponse,
  ThresholdValues
} from '@/app/types/threshold.type';

interface ThresholdConfigProps {
  isOpen: boolean;
  onClose: () => void;
  deviceUuid: string;
  deviceName: string;
  mode?: 'configure' | 'add';
}

interface FormErrors {
  [key: string]: string;
}

const ThresholdConfig: React.FC<ThresholdConfigProps> = ({
  isOpen,
  onClose,
  deviceUuid,
  deviceName,
  mode = 'configure'
}) => {
  // Form state
  const [thresholdLevel, setThresholdLevel] = useState<ThresholdLevel>('WARNING');
  const [thresholdType, setThresholdType] = useState<ThresholdType>('UPPER');
  const [message, setMessage] = useState<string>('');
  const [thresholdValues, setThresholdValues] = useState<ThresholdValues>({});
  const [telemetryNames, setTelemetryNames] = useState<string[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isLoadingTelemetry, setIsLoadingTelemetry] = useState(false);
  const [telemetryError, setTelemetryError] = useState<string | null>(null);
  const [errors, setErrors] = useState<FormErrors>({});
  // New state for tracking which thresholds are selected
  const [selectedThresholds, setSelectedThresholds] = useState<Record<string, boolean>>({});

  const toast = useToast();

  // Color mode values - always call hooks to maintain consistent order
  const bgColor = useColorModeValue('white', 'gray.800');
  const borderColor = useColorModeValue('gray.200', 'gray.600');
  const textColor = useColorModeValue('gray.900', 'white');
  const subtextColor = useColorModeValue('gray.600', 'gray.400');
  const previewBgColor = useColorModeValue('gray.50', 'gray.700');

  // Fetch telemetry values when modal opens
  useEffect(() => {
    if (isOpen && deviceUuid) {
      fetchTelemetryValues();
    }
  }, [isOpen, deviceUuid]);

  const fetchTelemetryValues = async () => {
    setIsLoadingTelemetry(true);
    setTelemetryError(null);
    
    try {
      const response = await apiClient.get<TelemetryValuesResponse>(
        `/auth/telemetry-values/${deviceUuid}`
      );
      
      const telemetryNames = response.telemetry_values;
      setTelemetryNames(telemetryNames);
      
      // Initialize threshold values with default values
      const initialValues: ThresholdValues = {};
      const initialSelected: Record<string, boolean> = {};
      telemetryNames.forEach((name) => {
        // Set default values based on telemetry type
        if (name === 'temperature') {
          initialValues[name] = 40.0;
        } else if (name === 'humidity') {
          initialValues[name] = 70;
        } else {
          initialValues[name] = 50; // Default value for other telemetry types
        }
        // By default, all thresholds are selected
        initialSelected[name] = true;
      });
      setThresholdValues(initialValues);
      setSelectedThresholds(initialSelected);
      
    } catch (error) {
      console.error('Error fetching telemetry values:', error);
      const errorMessage = error instanceof Error ? error.message : 'Failed to fetch telemetry values';
      setTelemetryError(errorMessage);
      
      toast({
        title: 'Erreur',
        description: 'Impossible de récupérer les valeurs de télémétrie',
        status: 'error',
        duration: 5000,
        isClosable: true,
      });
    } finally {
      setIsLoadingTelemetry(false);
    }
  };

  const validateForm = (): boolean => {
    const newErrors: FormErrors = {};

    // Check if at least one threshold is selected
    const hasSelectedThresholds = Object.values(selectedThresholds).some(isSelected => isSelected);
    
    if (!hasSelectedThresholds) {
      toast({
        title: 'Erreur',
        description: 'Vous devez sélectionner au moins un seuil de télémétrie',
        status: 'error',
        duration: 5000,
        isClosable: true,
      });
      return false;
    }

    // Validate only selected telemetry values
    Object.entries(thresholdValues).forEach(([key, value]) => {
      // Only validate if the threshold is selected
      if (selectedThresholds[key]) {
        if (typeof value !== 'number' || isNaN(value)) {
          newErrors[key] = `La valeur pour ${key} doit être un nombre valide`;
        } else {
          // Apply specific validation rules based on telemetry type
          if (key === 'temperature') {
            if (value < -50 || value > 100) {
              newErrors[key] = 'La température doit être entre -50°C et 100°C';
            }
          } else if (key === 'humidity') {
            if (value < 0 || value > 100) {
              newErrors[key] = 'L\'humidité doit être entre 0% et 100%';
            }
          } else {
            // Generic validation for other telemetry types
            if (value < 0 || value > 1000) {
              newErrors[key] = `La valeur pour ${key} doit être entre 0 et 1000`;
            }
          }
        }
      }
    });

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleThresholdValueChange = (key: string, value: number) => {
    setThresholdValues(prev => ({
      ...prev,
      [key]: value
    }));
  };

  // Handle checkbox change for threshold selection
  const handleThresholdSelectionChange = (key: string, isSelected: boolean) => {
    setSelectedThresholds(prev => ({
      ...prev,
      [key]: isSelected
    }));
    
    // If checkbox is being checked, ensure there's a value for this threshold
    if (isSelected && (thresholdValues[key] === undefined || thresholdValues[key] === null)) {
      let defaultValue = 50; // Default value for other telemetry types
      if (key === 'temperature') {
        defaultValue = 40.0;
      } else if (key === 'humidity') {
        defaultValue = 70;
      }
      setThresholdValues(prev => ({
        ...prev,
        [key]: defaultValue
      }));
    }
  };

  const handleSubmit = async () => {
    if (!validateForm()) {
      return;
    }

    setIsSubmitting(true);

    try {
      // Filter threshold values to only include selected ones
      const selectedThresholdValues: ThresholdValues = {};
      Object.entries(thresholdValues).forEach(([key, value]) => {
        if (selectedThresholds[key]) {
          selectedThresholdValues[key] = value;
        }
      });

      const requestData: ThresholdConfigRequest = {
        thresholdLevel,
        thresholdType,
        message: message.trim() || null,
        threshold: selectedThresholdValues
      };

      const endpoint = mode === 'configure' 
        ? `/auth/configure-threshold/${deviceUuid}`
        : `/auth/add-threshold/${deviceUuid}`;

      const response = await apiClient.post<ThresholdConfigResponse>(
        endpoint,
        requestData as unknown as Record<string, unknown>
      );

      toast({
        title: 'Succès',
        description: response.message || (mode === 'configure' 
          ? 'Configuration de seuil mise à jour avec succès' 
          : 'Nouveau seuil ajouté avec succès'),
        status: 'success',
        duration: 3000,
        isClosable: true,
      });

      onClose();
      resetForm();
    } catch (error) {
      console.error('Error configuring threshold:', error);
      
      let errorMessage = 'Impossible de configurer le seuil';
      if (error instanceof Error) {
        errorMessage = error.message;
      }
      
      toast({
        title: 'Erreur',
        description: errorMessage,
        status: 'error',
        duration: 5000,
        isClosable: true,
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  const resetForm = () => {
    setThresholdLevel('WARNING');
    setThresholdType('UPPER');
    setMessage('');
    setThresholdValues({});
    setTelemetryNames([]);
    setErrors({});
    setTelemetryError(null);
    setSelectedThresholds({});
  };

  // New function to select all thresholds
  const selectAllThresholds = () => {
    const newSelected: Record<string, boolean> = {};
    telemetryNames.forEach(name => {
      newSelected[name] = true;
    });
    setSelectedThresholds(newSelected);
  };

  // New function to deselect all thresholds
  const deselectAllThresholds = () => {
    const newSelected: Record<string, boolean> = {};
    telemetryNames.forEach(name => {
      newSelected[name] = false;
    });
    setSelectedThresholds(newSelected);
  };

  const handleClose = () => {
    resetForm();
    onClose();
  };

  const getUnitForTelemetry = (telemetryName: string): string => {
    switch (telemetryName) {
      case 'temperature':
        return '°C';
      case 'humidity':
        return '%';
      default:
        return '';
    }
  };

  const getStepForTelemetry = (telemetryName: string): number => {
    switch (telemetryName) {
      case 'temperature':
        return 0.1;
      case 'humidity':
        return 1;
      default:
        return 1;
    }
  };

  const getPrecisionForTelemetry = (telemetryName: string): number => {
    switch (telemetryName) {
      case 'temperature':
        return 1;
      default:
        return 0;
    }
  };

  return (
    <Modal 
      isOpen={isOpen} 
      onClose={handleClose} 
      size="lg"
      isCentered
      scrollBehavior="inside"
      motionPreset="slideInBottom"
      blockScrollOnMount={true}
      preserveScrollBarGap={true}
    >
      <ModalOverlay />
      <ModalContent 
        mx={4}
        my={4}
        maxH="90vh"
        overflow="hidden"
      >
        <ModalHeader>
          <HStack>
            <SettingsIcon />
            <VStack align="start" spacing={0}>
              <Text>
                {mode === 'configure' ? 'Configuration des seuils' : 'Ajouter un nouveau seuil'}
              </Text>
              <Text fontSize="sm" color={subtextColor} fontWeight="normal">
                {deviceName}
              </Text>
            </VStack>
          </HStack>
        </ModalHeader>
        <ModalCloseButton />
        
        <ModalBody pb={6} overflowY="auto">
          {isLoadingTelemetry ? (
            <VStack spacing={4} py={8}>
              <Spinner size="lg" color="blue.500" thickness="4px" />
              <Text color={subtextColor}>Chargement des valeurs de télémétrie...</Text>
            </VStack>
          ) : telemetryError ? (
            <Alert status="error" borderRadius="md">
              <AlertIcon />
              <Box>
                <Text fontWeight="bold">Erreur de chargement</Text>
                <Text fontSize="sm">{telemetryError}</Text>
              </Box>
            </Alert>
          ) : (
            <VStack spacing={4} align="stretch">
              {/* Threshold Level */}
              <FormControl>
                <FormLabel fontSize="sm">Niveau de seuil</FormLabel>
                <Select
                  value={thresholdLevel}
                  onChange={(e) => setThresholdLevel(e.target.value as ThresholdLevel)}
                  bg={bgColor}
                >
                  <option value="WARNING">WARNING - Avertissement</option>
                  <option value="CRITICAL">CRITICAL - Critique</option>
                </Select>
              </FormControl>

              {/* Threshold Type */}
              <FormControl>
                <FormLabel fontSize="sm">Type de seuil</FormLabel>
                <Select
                  value={thresholdType}
                  onChange={(e) => setThresholdType(e.target.value as ThresholdType)}
                  bg={bgColor}
                >
                  <option value="UPPER">UPPER - Seuil supérieur</option>
                  <option value="LOWER">LOWER - Seuil inférieur</option>
                </Select>
              </FormControl>

              <Divider />

              {/* Select/Deselect All Buttons */}
              {telemetryNames.length > 1 && (
                <HStack justify="flex-end" spacing={3}>
                  <Button 
                    size="xs" 
                    variant="outline" 
                    onClick={selectAllThresholds}
                  >
                    Tout sélectionner
                  </Button>
                  <Button 
                    size="xs" 
                    variant="outline" 
                    onClick={deselectAllThresholds}
                  >
                    Tout déselectionner
                  </Button>
                </HStack>
              )}

              {/* Dynamic Telemetry Threshold Values with Checkboxes */}
              {telemetryNames.map((telemetryName) => (
                <Box key={telemetryName} borderWidth="1px" borderRadius="md" p={3} bg={bgColor}>
                  <Stack direction="row" alignItems="center" spacing={2}>
                    <Checkbox
                      isChecked={selectedThresholds[telemetryName] ?? true}
                      onChange={(e) => handleThresholdSelectionChange(telemetryName, e.target.checked)}
                    >
                      <Text fontSize="sm" fontWeight="medium">
                        {telemetryName}
                      </Text>
                    </Checkbox>
                    {selectedThresholds[telemetryName] ?? true ? (
                      <FormControl isInvalid={!!errors[telemetryName]} flex={1} mb={0}>
                        <NumberInput
                          value={thresholdValues[telemetryName] || 0}
                          onChange={(valueString, valueNumber) => 
                            handleThresholdValueChange(telemetryName, valueNumber || 0)
                          }
                          min={telemetryName === 'temperature' ? -50 : 0}
                          max={telemetryName === 'temperature' ? 100 : telemetryName === 'humidity' ? 100 : 1000}
                          step={getStepForTelemetry(telemetryName)}
                          precision={getPrecisionForTelemetry(telemetryName)}
                          size="sm"
                        >
                          <NumberInputField />
                          <NumberInputStepper>
                            <NumberIncrementStepper />
                            <NumberDecrementStepper />
                          </NumberInputStepper>
                        </NumberInput>
                        <FormErrorMessage>{errors[telemetryName]}</FormErrorMessage>
                      </FormControl>
                    ) : (
                      <Text fontSize="sm" color={subtextColor} flex={1}>
                        Non inclus dans le seuil
                      </Text>
                    )}
                    <Text fontSize="sm" color={subtextColor} minWidth="40px" textAlign="right">
                      {getUnitForTelemetry(telemetryName)}
                    </Text>
                  </Stack>
                </Box>
              ))}

              <Divider />

              {/* Optional Message */}
              <FormControl>
                <FormLabel fontSize="sm">Message personnalisé (optionnel)</FormLabel>
                <Textarea
                  value={message}
                  onChange={(e) => setMessage(e.target.value)}
                  placeholder="Message d'alerte personnalisé..."
                  rows={3}
                  resize="vertical"
                  bg={bgColor}
                />
                <Text fontSize="xs" color={subtextColor} mt={1}>
                  Laissez vide pour utiliser le message par défaut
                </Text>
                <HStack mt={2} spacing={0} alignItems="start">
                  <Box p={2} bg={previewBgColor} borderRadius="md" flex={1} maxH="70px" overflowY="auto">
                    <Text fontSize="xs" fontWeight="medium" mb={1}>
                      Variables:
                    </Text>
                    <Text fontSize="2xs">
                      {"{device_uuid}"}, {"{client_id}"}, {"{threshold_type}"}, {"{threshold_name}"}, {"{threshold_value}"}, {"{current_value}"}
                    </Text>
                  </Box>
                  <Tooltip hasArrow label="UUID, client ID, type de seuil, nom de télémétrie, valeur du seuil, valeur actuelle" placement="top">
                    <Box ml={1}>
                      <IconButton
                        aria-label="Info"
                        icon={<InfoIcon />}
                        size="xs"
                        variant="ghost"
                      />
                    </Box>
                  </Tooltip>
                </HStack>
              </FormControl>

              {/* Preview */}
              {telemetryNames.length > 0 && (
                <Box p={2} bg={previewBgColor} borderRadius="md">
                  <Text fontSize="xs" fontWeight="medium" mb={0}>
                    Aperçu:
                  </Text>
                  <HStack fontSize="xs" flexWrap="wrap" spacing={2} mt={1}>
                    <Badge colorScheme={thresholdLevel === 'WARNING' ? 'yellow' : 'red'}>
                      {thresholdLevel}
                    </Badge>
                    <Badge colorScheme="blue">
                      {thresholdType}
                    </Badge>
                    {Object.entries(thresholdValues).map(([key, value]) => (
                      selectedThresholds[key] ? (
                        <Badge key={key} colorScheme="green">
                          {key}: {value}{getUnitForTelemetry(key)}
                        </Badge>
                      ) : null
                    ))}
                  </HStack>
                  {message && (
                    <Text mt={1} fontSize="xs" noOfLines={1} textOverflow="ellipsis">
                      <strong>Message:</strong> {message}
                    </Text>
                  )}
                  {/* Show message when no thresholds are selected */}
                  {telemetryNames.length > 0 && !Object.values(selectedThresholds).some(isSelected => isSelected) && (
                    <Text mt={1} fontSize="xs" color={subtextColor}>
                      Aucun seuil sélectionné
                    </Text>
                  )}
                </Box>
              )}
            </VStack>
          )}
        </ModalBody>

        <ModalFooter>
          <HStack spacing={3} width="full" justify="end">
            <Button variant="ghost" onClick={handleClose}>
              Annuler
            </Button>
            <Button
              colorScheme="blue"
              onClick={handleSubmit}
              isLoading={isSubmitting}
              loadingText={mode === 'configure' ? 'Configuration...' : 'Ajout...'}
              leftIcon={<SettingsIcon />}
              isDisabled={isLoadingTelemetry || !!telemetryError || telemetryNames.length === 0}
            >
              {mode === 'configure' ? 'Configurer les seuils' : 'Ajouter un seuil'}
            </Button>
          </HStack>
        </ModalFooter>
      </ModalContent>
    </Modal>
  );
};

export default ThresholdConfig;