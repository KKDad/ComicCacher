export type AppRole = 'USER' | 'OPERATOR' | 'ADMIN';

const ROLE_RANK: Record<AppRole, number> = { USER: 0, OPERATOR: 1, ADMIN: 2 };

export function hasRole(userRoles: string[], required: AppRole): boolean {
  return userRoles.some(
    (r) => ROLE_RANK[r as AppRole] !== undefined && ROLE_RANK[r as AppRole] >= ROLE_RANK[required]
  );
}

export function isOperator(roles: string[]): boolean {
  return hasRole(roles, 'OPERATOR');
}

export function isAdmin(roles: string[]): boolean {
  return hasRole(roles, 'ADMIN');
}
