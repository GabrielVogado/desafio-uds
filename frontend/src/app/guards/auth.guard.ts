import {CanActivateFn, Router} from '@angular/router';
import {inject} from '@angular/core';
import {AuthService} from '../services/auth.service';

export const authGuard: CanActivateFn = (_route, _state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isLoggedIn()) {
    return true;
  }

  router.navigate(['/login']);
  return false;
};

// Helper factory in case it's used elsewhere
export function createAuthGuard(authService: AuthService, router: Router): CanActivateFn {
  return () => {
    if (authService.isLoggedIn()) {
      return true;
    }
    router.navigate(['/login']);
    return false;
  };
}
