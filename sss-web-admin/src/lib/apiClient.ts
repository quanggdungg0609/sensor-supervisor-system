import { DeviceListResponse } from '@/app/types/devices.type';
import { getEnvVarWithFallback } from './env-validation';
import axios, { AxiosInstance, AxiosResponse } from 'axios';
import https from 'https';

/**
 * Interface for create device request
 * @interface CreateDeviceRequest
 * @property {string} device_name - Name of the device
 * @property {string} mqtt_username - MQTT username for the device
 */
export interface CreateDeviceRequest {
  device_name: string;
  mqtt_username: string;
}

/**
 * Interface for create device response
 * @interface CreateDeviceResponse
 * @property {string} device_name - Name of the device
 * @property {string} mqtt_username - MQTT username
 * @property {string} mqtt_password - Generated MQTT password
 * @property {string} client_id - Generated client ID
 */
export interface CreateDeviceResponse {
  device_name: string;
  mqtt_username: string;
  mqtt_password: string;
  client_id: string;
}

/**
 * Interface for password reset request
 * @interface PasswordResetRequest
 * @property {string} device_uuid - UUID of the device to reset password
 */
export interface PasswordResetRequest {
  device_uuid: string;
}

/**
 * Interface for password reset response
 * @interface PasswordResetResponse
 * @property {string} client_id - Client ID of the device
 * @property {string} new_password - New generated password
 * @property {string} message - Success message
 */
export interface PasswordResetResponse {
  client_id: string;
  new_password: string;
  message: string;
}

/**
 * Interface for API error response
 * @interface ApiError
 * @property {string} error - Error message
 */
export interface ApiError {
  error: string;
}

/**
 * Type for query parameters
 */
export type QueryParams = Record<string, string | number | boolean | undefined>;

/**
 * Type for request body data
 */
export type RequestData = Record<string, unknown> | FormData | string | null;

/**
 * API Client class for handling HTTP requests
 * @class ApiClient
 */
class ApiClient {
  private axiosInstance: AxiosInstance;

  /**
   * Constructor for ApiClient
   * @param {string} baseURL - Base URL for API requests
   */
  constructor(baseURL: string = '/api') {
    // Only create HTTPS agent for external HTTPS URLs (not internal Docker communication)
    const httpsAgent = baseURL.startsWith('https://') ? new https.Agent({
      rejectUnauthorized: false // ONLY for development/testing
    }) : undefined;

    this.axiosInstance = axios.create({
      baseURL,
      timeout: 10000,
      headers: {
        'Content-Type': 'application/json',
      },
      httpsAgent: httpsAgent, // Add HTTPS agent for external API calls
    });

    // Request interceptor
    this.axiosInstance.interceptors.request.use(
      (config) => {
        // Add any auth headers or other request modifications here
        return config;
      },
      (error) => {
        return Promise.reject(error);
      }
    );

    // Response interceptor
    this.axiosInstance.interceptors.response.use(
      (response) => {
        return response;
      },
      (error) => {
        // Handle common errors here
        if (error.response?.status === 401) {
          // Handle unauthorized access
          console.error('Unauthorized access');
        }
        return Promise.reject(error);
      }
    );
  }

  /**
   * Creates a new device with MQTT credentials
   * @param {CreateDeviceRequest} deviceData - Device creation data
   * @returns {Promise<CreateDeviceResponse>} Promise resolving to device response
   * @throws {Error} When API request fails
   */
  async createDevice(deviceData: CreateDeviceRequest): Promise<CreateDeviceResponse> {
    try {
      const response: AxiosResponse<CreateDeviceResponse> = await this.axiosInstance.post(
        '/auth/create-device',
        deviceData
      );
      return response.data;
    } catch (error) {
      if (axios.isAxiosError(error)) {
        const errorMessage = error.response?.data?.error || error.message;
        throw new Error(errorMessage);
      }
      throw new Error('An unexpected error occurred');
    }
  }

  /**
   * Resets the password for a device
   * @param {string} deviceUuid - UUID of the device to reset password
   * @returns {Promise<PasswordResetResponse>} Promise resolving to password reset response
   * @throws {Error} When API request fails
   */
  async resetDevicePassword(deviceUuid: string): Promise<PasswordResetResponse> {
    try {
      const response: AxiosResponse<PasswordResetResponse> = await this.axiosInstance.post(
        '/auth/reset-password',
        { device_uuid: deviceUuid }
      );
      return response.data;
    } catch (error) {
      if (axios.isAxiosError(error)) {
        const errorMessage = error.response?.data?.error || error.message;
        throw new Error(errorMessage);
      }
      throw new Error('An unexpected error occurred');
    }
  }

  async getDevices(page: number, size: number): Promise<DeviceListResponse> {
    try{
      const response: AxiosResponse<DeviceListResponse> = await this.axiosInstance.get(
        '/auth/devices',
        {
          params: {
            page,
            size,
          },
        }
      );
      return response.data;
    }catch(error){
         if (axios.isAxiosError(error)) {
            const errorMessage = error.response?.data?.error || error.message;
            throw new Error(errorMessage);
        }
        throw new Error('An unexpected error occurred');
    }
  }

  /**
   * Generic GET request method
   * @param {string} endpoint - API endpoint
   * @param {QueryParams} params - Query parameters
   * @returns {Promise<T>} Promise resolving to response data
   */
  async get<T>(endpoint: string, params?: QueryParams): Promise<T> {
    try {
      const response: AxiosResponse<T> = await this.axiosInstance.get(endpoint, { params });
      return response.data;
    } catch (error) {
      if (axios.isAxiosError(error)) {
        const errorMessage = error.response?.data?.error || error.message;
        throw new Error(errorMessage);
      }
      throw new Error('An unexpected error occurred');
    }
  }

  /**
   * Generic POST request method
   * @param {string} endpoint - API endpoint
   * @param {RequestData} data - Request body data
   * @returns {Promise<T>} Promise resolving to response data
   */
  async post<T>(endpoint: string, data?: RequestData): Promise<T> {
    try {
      const response: AxiosResponse<T> = await this.axiosInstance.post(endpoint, data);
      return response.data;
    } catch (error) {
      if (axios.isAxiosError(error)) {
        const errorMessage = error.response?.data?.error || error.message;
        throw new Error(errorMessage);
      }
      throw new Error('An unexpected error occurred');
    }
  }

  /**
   * Generic PUT request method
   * @param {string} endpoint - API endpoint
   * @param {RequestData} data - Request body data
   * @returns {Promise<T>} Promise resolving to response data
   */
  async put<T>(endpoint: string, data?: RequestData): Promise<T> {
    try {
      const response: AxiosResponse<T> = await this.axiosInstance.put(endpoint, data);
      return response.data;
    } catch (error) {
      if (axios.isAxiosError(error)) {
        const errorMessage = error.response?.data?.error || error.message;
        throw new Error(errorMessage);
      }
      throw new Error('An unexpected error occurred');
    }
  }

  /**
   * Generic DELETE request method
   * @param {string} endpoint - API endpoint
   * @returns {Promise<T>} Promise resolving to response data
   */
  async delete<T>(endpoint: string): Promise<T> {
    try {
      const response: AxiosResponse<T> = await this.axiosInstance.delete(endpoint);
      return response.data;
    } catch (error) {
      if (axios.isAxiosError(error)) {
        const errorMessage = error.response?.data?.error || error.message;
        throw new Error(errorMessage);
      }
      throw new Error('An unexpected error occurred');
    }
  }
}

// Create and export a singleton instance
// Use correct environment variable for internal service communication
const serverBaseUrl = getEnvVarWithFallback('DEVICE_SERVICE_API_URL', 'http://sss-device-service:2002');
const serverApiClient = new ApiClient(serverBaseUrl);

// Export both instances
const apiClient = new ApiClient(); // Cho client-side (internal API routes)
export default apiClient;
export { serverApiClient }; // Cho server-side (external API)