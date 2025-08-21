import axios, { AxiosInstance, AxiosResponse } from 'axios';

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
    this.axiosInstance = axios.create({
      baseURL,
      timeout: 10000,
      headers: {
        'Content-Type': 'application/json',
      },
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
const apiClient = new ApiClient();
export default apiClient;