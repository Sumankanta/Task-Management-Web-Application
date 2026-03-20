import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { AuthService } from '../services/auth';
import { ToastService } from '../services/toast';

export const RoleGuard = (route: ActivatedRouteSnapshot) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const toast = inject(ToastService);

  const allowedRoles: string[] = route.data?.['roles'] ?? [];
  const user = auth.getCurrentUser();

  if (user && allowedRoles.includes(user.role)) {
    return true;
  }

  toast.show('You do not have permission to view this page.', 'error');
  router.navigate(['/dashboard']);
  return false;
};
