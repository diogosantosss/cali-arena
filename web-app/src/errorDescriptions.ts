
export const errorDescriptions: Record<string, string> = {
  "already-used-username": 
    "The username is already in use. Please choose a different one.",
  "insecure-password": 
    "The password provided is not secure enough. Please choose a stronger password.",
  "error-updating-user-role": 
    "There was an error updating the user's role. Please try again later.",
  "user-not-found": 
    "The specified user could not be found. Please check the username and try again.",
  "invalid-role": 
    "The role specified is invalid. Please choose a valid role.",
  "user-or-password-are-invalid": 
    "The username or password provided is incorrect. Please check your credentials and try again.",
  "not-authorized": 
    "You are not authorized to perform this action. Please check your permissions and try again.",
};

export function getErrorDescription(errorType: string): string {
  return errorDescriptions[errorType] || errorType;
}