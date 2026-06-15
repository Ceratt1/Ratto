export interface AuthRuntimeConfig {
  url: string;
  realm: string;
  clientId: string;
}

export interface UserProfile {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
}
