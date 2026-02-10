export interface User {
  username: string;
  email: string;
  displayName: string;
  roles: string[];
  created: string;
  lastLogin?: string;
}

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
  expiresIn: number; // seconds until expiry
  expiresAt: number; // timestamp
}

export interface LoginCredentials {
  username: string;
  password: string;
  rememberMe?: boolean;
}

export interface RegisterData {
  username: string;
  email: string;
  password: string;
  displayName: string;
}

export interface AuthState {
  user: User | null;
  tokens: AuthTokens | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;
}

// GraphQL AuthPayload response
export interface AuthPayload {
  token: string;
  refreshToken: string;
  username: string;
  displayName: string;
}

// Mapped response for internal use
export interface AuthResponse {
  user: {
    username: string;
    displayName: string;
  };
  accessToken: string;
  refreshToken: string;
}
