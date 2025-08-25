import { NextRequest, NextResponse } from 'next/server';
import { getEnvVarWithFallback } from '@/lib/env-validation';
import axios from 'axios';

/**
 * Interface for password reset response from ACL service
 */
interface PasswordResetResponse {
  client_id: string;
  new_password: string;
  message: string;
}

/**
 * POST /api/auth/reset-password
 * Resets the password for a device by calling the ACL service
 * 
 * @param request - NextRequest containing the device UUID in the body
 * @returns NextResponse with the password reset result
 */
export async function POST(request: NextRequest) {
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

    // Get ACL service URL from environment
    const aclServiceUrl = getEnvVarWithFallback('AUTH_ACL_SERVICE_API_URL', 'http://sss-auth-acl-service:2001');
    
    console.log('Attempting password reset for device UUID:', device_uuid);
    console.log('ACL Service URL:', aclServiceUrl);
    console.log('Full URL:', `${aclServiceUrl}/api/v1/mqtt/change_password/${device_uuid}`);

    // Call ACL service to reset password using PATCH method (internal HTTP communication)
    const response = await axios.patch<PasswordResetResponse>(
      `${aclServiceUrl}/api/v1/mqtt/change_password/${device_uuid}`,
      {},
      {
        timeout: 15000,
        headers: {
          'Content-Type': 'application/json',
        },
      }
    );

    console.log('ACL Service Response:', response.data);

    return NextResponse.json(response.data);
    
  } catch (error) {
    console.error('Password reset error details:', {
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
      let message = 'Échec de la réinitialisation du mot de passe';
      
      // Provide more user-friendly error messages in French
      if (status === 404) {
        message = 'Appareil non trouvé dans le système ACL';
      } else if (status === 500) {
        message = 'Erreur du serveur ACL. Veuillez contacter l\'administrateur.';
      } else if (status === 401 || status === 403) {
        message = 'Non autorisé à réinitialiser le mot de passe pour cet appareil';
      } else if (error.code === 'ECONNREFUSED' || error.code === 'ETIMEDOUT') {
        message = 'Impossible de contacter le service ACL. Veuillez réessayer plus tard.';
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
      { error: 'Une erreur inattendue s\'est produite lors de la réinitialisation du mot de passe' },
      { status: 500 }
    );
  }
}