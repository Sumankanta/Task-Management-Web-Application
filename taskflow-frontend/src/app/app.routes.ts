import { Routes } from '@angular/router';
import { AuthGuard } from './guards/auth-guard';
import { RoleGuard } from './guards/role-guard';

export const routes: Routes = [

  {
    path: 'login',
    loadComponent: () => import('./auth/login/login').then(m => m.LoginComponent)
  },

  {
    path: 'register',
    loadComponent: () => import('./auth/register/register').then(m => m.RegisterComponent)
  },

  {
    path: 'dashboard',
    canActivate: [AuthGuard],
    loadComponent: () => import('./dashboard/dashboard').then(m => m.DashboardComponent)
  },

  {
    path: 'teams',
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ADMIN', 'MANAGER'] },
    loadComponent: () => import('./teams/teams').then(m => m.TeamsComponent)
  },

  {
    path: 'admin',
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ADMIN'] },
    loadComponent: () => import('./admin/admin').then(m => m.AdminComponent)
  },

  {
    path: 'settings',
    canActivate: [AuthGuard],
    loadComponent: () => import('./settings/settings').then(m => m.SettingsComponent)
  },

  {
    path: 'settings/team',
    redirectTo: '/settings',
    pathMatch: 'full'
  },

  {
    path: '',
    redirectTo: 'login',
    pathMatch: 'full'
  },

  {
    path: '**',
    redirectTo: 'login'
  }

];
