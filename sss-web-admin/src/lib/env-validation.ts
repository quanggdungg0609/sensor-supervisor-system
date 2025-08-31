/**
 * Environment variables validation utility
 * Validates required environment variables at application startup
 */

/**
 * List of required environment variables
 */
const REQUIRED_ENV_VARS = [
  'DEVICE_SERVICE_API_URL',
  'AUTH_ACL_SERVICE_API_URL',
  'ALERT_SERVICE_API_URL',
  'NEXTAUTH_SECRET',
  'ADMIN_USER',
  'ADMIN_PASSWORD'
] as const;

/**
 * Validates that all required environment variables are present
 * @throws {Error} If any required environment variable is missing
 */
export function validateEnvironmentVariables(): void {
  const missingVars: string[] = [];

  for (const envVar of REQUIRED_ENV_VARS) {
    if (!process.env[envVar] || process.env[envVar]?.trim() === '') {
      missingVars.push(envVar);
    }
  }

  if (missingVars.length > 0) {
    const errorMessage = `Missing required environment variables: ${missingVars.join(', ')}\n` +
      'Please check your .env.local file and ensure all required variables are set.';
    
    console.error('❌ Environment Validation Failed:');
    console.error(errorMessage);
    
    throw new Error(errorMessage);
  }

  console.log('✅ All required environment variables are present');
}

/**
 * Gets a required environment variable
 * @param {string} name - Environment variable name
 * @returns {string} Environment variable value
 * @throws {Error} If environment variable is not set
 */
export function getRequiredEnvVar(name: string): string {
  const value = process.env[name];
  
  if (!value || value.trim() === '') {
    throw new Error(`Required environment variable ${name} is not set`);
  }
  
  return value;
}

/**
 * Gets an environment variable with fallback value for runtime validation
 * @param {string} name - Environment variable name
 * @param {string} fallback - Fallback value if environment variable is not set
 * @returns {string} Environment variable value or fallback
 */
export function getEnvVarWithFallback(name: string, fallback: string): string {
  const value = process.env[name];
  
  if (!value || value.trim() === '') {
    console.warn(`⚠️ Environment variable ${name} is not set, using fallback: ${fallback}`);
    return fallback;
  }
  
  return value;
}
