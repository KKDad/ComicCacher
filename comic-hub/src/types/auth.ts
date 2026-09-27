export interface User {
  username: string;
  email: string;
  displayName: string;
  roles: string[];
  created: string;
  lastLogin?: string;
}
