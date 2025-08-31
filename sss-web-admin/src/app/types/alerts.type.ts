/**
 * Alert Email related type definitions
 */

/**
 * Interface for alert emails response from API
 * @interface AlertEmailsResponse
 * @property {string} status - Response status (success/error)
 * @property {string[]} emails - Array of email addresses
 */
export interface AlertEmailsResponse {
  status: string;
  emails: string[];
}

/**
 * Interface for adding new email request
 * @interface AddEmailRequest
 * @property {string} email - Email address to add
 */
export interface AddEmailRequest {
  email: string;
}

/**
 * Interface for deleting email request
 * @interface DeleteEmailRequest
 * @property {string} email - Email address to delete
 */
export interface DeleteEmailRequest {
  email: string;
}

/**
 * Interface for email operation response
 * @interface EmailOperationResponse
 * @property {string} status - Response status
 * @property {string} message - Operation result message
 */
export interface EmailOperationResponse {
  status: string;
  message: string;
}