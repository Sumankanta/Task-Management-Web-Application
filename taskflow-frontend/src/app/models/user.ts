export type Role = 'ADMIN' | 'MANAGER' | 'MEMBER' | 'VIEWER'

export interface CurrentUser {
  id: number;
  fullName: string;
  email: string;
  role: Role;
  avatarColor?: string;
  bio?: string;
}

export interface UserSummary {
  id: number;
  fullName: string;
  email: string;
  role: Role;
  isActive: boolean;
}
