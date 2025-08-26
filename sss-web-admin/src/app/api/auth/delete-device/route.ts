import { NextRequest, NextResponse } from 'next/server';
import { getEnvVarWithFallback } from '@/lib/env-validation';
import axios from 'axios';
import https from 'https';

/**
 * Interface for device deletion response from Device service
 */
interface DeviceDeleteResponse {
  message: string;
  device_uuid: string;
}

/**
 * DELETE /api/auth/delete-device
 * Deletes a device completely by calling the Device service
 * 
 * @param request - NextRequest containing the device UUID in the body
 * @returns NextResponse with the deletion result
 */
export async function DELETE(request: NextRequest) {
  try {
    // Parse request body
    const body = await request.json();
    const { device_uuid } = body;

    if (!device_uuid) {
      return NextResponse.json(
        { error: 'UUID de l\'appareil requis' },
        { status: 400 }
      );
    }

    // Validate UUID format
    const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
    if (!uuidRegex.test(device_uuid)) {
      return NextResponse.json(
        { error: 'Format UUID invalide' },
        { status: 400 }
      );
    }

    // Get Device service URL from environment
    const deviceServiceUrl = getEnvVarWithFallback('DEVICE_SERVICE_API_URL', 'http://sss-device-service');
    
    // Create axios instance with HTTPS agent for external API calls
    const httpsAgent = new https.Agent({
      rejectUnauthorized: false // ONLY for development/testing
    });

    console.log('Attempting device deletion for UUID:', device_uuid);
    console.log('Device Service URL:', deviceServiceUrl);
    console.log('Full URL:', `${deviceServiceUrl}/api/v1/devices/${device_uuid}`);

    // Call Device service to delete device
    const response = await axios.delete<DeviceDeleteResponse>(
      `${deviceServiceUrl}/api/v1/devices/${device_uuid}`,
      {
        timeout: 15000,
        httpsAgent,
        headers: {
          'Content-Type': 'application/json',
        },
      }
    );

    console.log('Device Service Response:', response.data);

    return NextResponse.json(response.data);
    
  } catch (error) {
    console.error('Device deletion error details:', {
      message: error instanceof Error ? error.message : 'Unknown error',
      stack: error instanceof Error ? error.stack : undefined,
    });
    
    if (axios.isAxiosError(error)) {
      console.error('Axios error details:', {
        status: error.response?.status,
        statusText: error.response?.statusText,
        data: error.response?.data,
        url: error.config?.url,
        method: error.config?.method,
      });
      
      const status = error.response?.status || 500;
      let message = 'Échec de la suppression de l\'appareil';
      
      // Provide more user-friendly error messages in French
      if (status === 404) {
        message = 'Appareil non trouvé';
      } else if (status === 500) {
        message = 'Erreur du serveur. Veuillez contacter l\'administrateur.';
      } else if (status === 401 || status === 403) {
        message = 'Non autorisé à supprimer cet appareil';
      } else if (error.code === 'ECONNREFUSED' || error.code === 'ETIMEDOUT') {
        message = 'Impossible de contacter le service. Veuillez réessayer plus tard.';
      } else if (error.response?.data?.message) {
        message = error.response.data.message;
      } else if (error.response?.data?.error) {
        message = error.response.data.error;
      }
      
      return NextResponse.json(
        { error: message },
        { status }
      );
    }
    
    return NextResponse.json(
      { error: 'Une erreur inattendue s\'est produite lors de la suppression de l\'appareil' },
      { status: 500 }
    );
  }
}