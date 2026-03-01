'use client';

import { createContext, useContext, type ReactNode } from 'react';
import type { User } from '@/types/auth';

const UserContext = createContext<User | null>(null);

interface UserProviderProps {
  user: User | null;
  children: ReactNode;
}

export function UserProvider({ user, children }: UserProviderProps) {
  return <UserContext value={user}>{children}</UserContext>;
}

export function useUser(): User | null {
  return useContext(UserContext);
}
