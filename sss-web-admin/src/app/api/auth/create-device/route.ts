import { NextRequest, NextResponse } from 'next/server';
import { getServerSession } from 'next-auth';
import axios from 'axios';
import https from 'https';

/**
 * Interface for create device request body
 * @interface CreateDeviceRequest
 * @property {string} device_name - Name of the device
 * @property {string} mqtt_username - MQTT username for the device
 */
interface CreateDeviceRequest {
  device_name: string;
  mqtt_username: string;
}

/**
 * Interface for external API response
 * @interface ExternalApiResponse
 * @property {string} device_name - Name of the device
 * @property {string} mqtt_username - MQTT username
 * @property {string} mqtt_password - Generated MQTT password
 * @property {string} client_id - Generated client ID
 */
interface ExternalApiResponse {
  device_name: string;
  mqtt_username: string;
  mqtt_password: string;
  client_id: string;
}

/**
 * Handles POST request to create a new device
 * @param {NextRequest} request - The incoming request
 * @returns {Promise<NextResponse>} Response with device data or error
 */
export async function POST(request: NextRequest): Promise<NextResponse> {
  try {
    // Check authentication
    const session = await getServerSession();
    if (!session) {
      return NextResponse.json(
        { error: 'Unauthorized - Please login first' },
        { status: 401 }
      );
    }

    // Parse request body
    const body: CreateDeviceRequest = await request.json();
    const { device_name, mqtt_username } = body;

    // Validate required fields
    if (!device_name || !mqtt_username) {
      return NextResponse.json(
        { error: 'Missing required fields: device_name and mqtt_username' },
        { status: 400 }
      );
    }

    // Validate field formats
    if (typeof device_name !== 'string' || typeof mqtt_username !== 'string') {
      return NextResponse.json(
        { error: 'Invalid field types: device_name and mqtt_username must be strings' },
        { status: 400 }
      );
    }

    // Trim and validate non-empty values
    const trimmedDeviceName = device_name.trim();
    const trimmedMqttUsername = mqtt_username.trim();

    if (!trimmedDeviceName || !trimmedMqttUsername) {
      return NextResponse.json(
        { error: 'device_name and mqtt_username cannot be empty' },
        { status: 400 }
      );
    }

    // Prepare data for external API
    const externalApiData = {
      device_name: trimmedDeviceName,
      mqtt_username: trimmedMqttUsername,
    };

    // Call external API
    const externalApiUrl = 'https://media115.lanestel.fr/devices-service/api/v1/devices/create_device';
    
    // Create axios instance with SSL verification disabled
    const httpsAgent = new https.Agent({
      rejectUnauthorized: false // ONLY for development/testing
    });

    const response = await axios.post<ExternalApiResponse>(
      externalApiUrl,
      externalApiData,
      {
        headers: {
          'Content-Type': 'application/json',
        },
        httpsAgent: httpsAgent // Add this line
      }
    );

    return NextResponse.json(response.data, { status: 200 });

  } catch (error) {
    console.error('Create device error:', error);

    // Handle axios errors
    if (axios.isAxiosError(error)) {
      const status = error.response?.status || 500;
      const message = error.response?.data?.error || error.message || 'External API error';
      
      return NextResponse.json(
        { error: `External API error: ${message}` },
        { status }
      );
    }

    // Handle other errors
    return NextResponse.json(
      { error: 'Internal server error' },
      { status: 500 }
    );
  }
}

/**
 * Handles unsupported HTTP methods
 * @returns {NextResponse} Method not allowed response
 */
export async function GET(): Promise<NextResponse> {
  return NextResponse.json(
    { error: 'Method not allowed' },
    { status: 405 }
  );
}