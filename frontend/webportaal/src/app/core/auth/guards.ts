import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from './auth.service';

/** Alleen voor ingelogde accounts; anders naar de inlogpagina, met de gevraagde pagina om naar terug te keren. */
export const authGuard: CanActivateFn = async (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  await auth.restore().catch(() => undefined);
  return auth.isSignedIn()
    ? true
    : router.createUrlTree(['/inloggen'], { queryParams: { terug: state.url } });
};

/** De inlogpagina is overbodig voor wie al is ingelogd. */
export const guestGuard: CanActivateFn = async () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  await auth.restore().catch(() => undefined);
  return auth.isSignedIn() ? router.createUrlTree(['/']) : true;
};

/** Alleen voor platformbeheerders. De backend controleert dit opnieuw bij elke aanroep. */
export const platformAdminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.isPlatformAdmin() ? true : inject(Router).createUrlTree(['/']);
};

/** Alleen een veilige, interne pagina om na het inloggen naar terug te keren. */
export function safeReturnUrl(value: string | null | undefined): string {
  return value && value.startsWith('/') && !value.startsWith('//') && !value.startsWith('/inloggen')
    ? value
    : '/';
}
