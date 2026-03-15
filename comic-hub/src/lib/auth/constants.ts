export const JWT_COOKIE = 'comic-hub-jwt';
export const REFRESH_COOKIE = 'comic-hub-refresh';
export const COOKIE_MAX_AGE = 60 * 60 * 24 * 7; // 7 days
export const PUBLIC_PATHS = ['/login', '/register', '/forgot-password'];
export const GRAPHQL_ENDPOINT = process.env.NEXT_PUBLIC_GRAPHQL_ENDPOINT!;
