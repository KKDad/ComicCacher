import { gql } from 'graphql-request';
import { graphqlClient, setAuthToken, clearAuthToken } from '../graphql-client';
import type { AuthResponse, AuthPayload, LoginCredentials, RegisterData } from '@/types/auth';

class AuthAPIError extends Error {
  constructor(
    message: string,
    public code?: string,
    public classification?: string
  ) {
    super(message);
    this.name = 'AuthAPIError';
  }
}

// GraphQL mutations
const LOGIN_MUTATION = gql`
  mutation Login($input: LoginInput!) {
    login(input: $input) {
      token
      refreshToken
      username
      displayName
    }
  }
`;

const REGISTER_MUTATION = gql`
  mutation Register($input: RegisterInput!) {
    register(input: $input) {
      token
      refreshToken
      username
      displayName
    }
  }
`;

const REFRESH_TOKEN_MUTATION = gql`
  mutation RefreshToken($refreshToken: String!) {
    refreshToken(refreshToken: $refreshToken) {
      token
      refreshToken
      username
      displayName
    }
  }
`;

const FORGOT_PASSWORD_MUTATION = gql`
  mutation ForgotPassword($email: String!) {
    forgotPassword(email: $email)
  }
`;

const RESET_PASSWORD_MUTATION = gql`
  mutation ResetPassword($token: String!, $newPassword: String!) {
    resetPassword(token: $token, newPassword: $newPassword) {
      token
      refreshToken
      username
      displayName
    }
  }
`;

const VALIDATE_TOKEN_QUERY = gql`
  query ValidateToken {
    validateToken
  }
`;

// Helper to decode JWT and extract expiration time
function getTokenExpiration(token: string): number {
  try {
    // JWT format: header.payload.signature
    const payload = token.split('.')[1];
    const decoded = JSON.parse(atob(payload));

    if (decoded.exp && decoded.iat) {
      // Return seconds until expiration
      return decoded.exp - decoded.iat;
    }

    // Fallback to 15 minutes if we can't decode
    return 900;
  } catch {
    // Fallback to 15 minutes if decoding fails
    return 900;
  }
}

// Helper to map AuthPayload to AuthResponse
function mapAuthPayload(payload: AuthPayload): AuthResponse {
  const expiresIn = getTokenExpiration(payload.token);

  return {
    user: {
      username: payload.username,
      displayName: payload.displayName,
    },
    accessToken: payload.token,
    refreshToken: payload.refreshToken,
    expiresIn,
  };
}

// Helper to handle GraphQL errors
function handleGraphQLError(error: any): never {
  const graphqlError = error?.response?.errors?.[0];
  if (graphqlError) {
    throw new AuthAPIError(
      graphqlError.message,
      graphqlError.extensions?.code,
      graphqlError.extensions?.classification
    );
  }
  throw new AuthAPIError(error.message || 'An unexpected error occurred');
}

export async function login(credentials: LoginCredentials): Promise<AuthResponse> {
  try {
    const response = await graphqlClient.request<{ login: AuthPayload }>(
      LOGIN_MUTATION,
      {
        input: {
          username: credentials.username,
          password: credentials.password,
        },
      }
    );

    const authResponse = mapAuthPayload(response.login);
    setAuthToken(authResponse.accessToken);
    return authResponse;
  } catch (error) {
    handleGraphQLError(error);
  }
}

export async function register(data: RegisterData): Promise<AuthResponse> {
  try {
    const response = await graphqlClient.request<{ register: AuthPayload }>(
      REGISTER_MUTATION,
      {
        input: {
          username: data.username,
          email: data.email,
          password: data.password,
          displayName: data.displayName,
        },
      }
    );

    const authResponse = mapAuthPayload(response.register);
    setAuthToken(authResponse.accessToken);
    return authResponse;
  } catch (error) {
    handleGraphQLError(error);
  }
}

export async function refreshToken(refreshToken: string): Promise<AuthResponse> {
  try {
    const response = await graphqlClient.request<{ refreshToken: AuthPayload }>(
      REFRESH_TOKEN_MUTATION,
      { refreshToken }
    );

    const authResponse = mapAuthPayload(response.refreshToken);
    setAuthToken(authResponse.accessToken);
    return authResponse;
  } catch (error) {
    handleGraphQLError(error);
  }
}

export async function logout(): Promise<void> {
  // Clear client-side auth token
  // Note: GraphQL API doesn't have a logout mutation - JWT tokens are stateless
  clearAuthToken();
}

export async function validateToken(accessToken: string): Promise<boolean> {
  try {
    // Temporarily set the token for validation
    const originalHeaders = graphqlClient.requestConfig.headers;
    graphqlClient.setHeader('Authorization', `Bearer ${accessToken}`);

    const response = await graphqlClient.request<{ validateToken: boolean }>(
      VALIDATE_TOKEN_QUERY
    );

    // Restore original headers
    graphqlClient.requestConfig.headers = originalHeaders;

    return response.validateToken;
  } catch {
    return false;
  }
}

export async function requestPasswordReset(email: string): Promise<void> {
  try {
    await graphqlClient.request(FORGOT_PASSWORD_MUTATION, { email });
  } catch (error) {
    handleGraphQLError(error);
  }
}

export async function resetPassword(token: string, newPassword: string): Promise<AuthResponse> {
  try {
    const response = await graphqlClient.request<{ resetPassword: AuthPayload }>(
      RESET_PASSWORD_MUTATION,
      { token, newPassword }
    );

    const authResponse = mapAuthPayload(response.resetPassword);
    setAuthToken(authResponse.accessToken);
    return authResponse;
  } catch (error) {
    handleGraphQLError(error);
  }
}
