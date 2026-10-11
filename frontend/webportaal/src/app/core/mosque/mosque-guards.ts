import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { MosqueContext } from './mosque-context';

/** Kiest de moskee uit `:mosqueId`. Zonder toegang (of bij een fout) terug naar het moskeeoverzicht. */
export const mosqueGuard: CanActivateFn = async (route) => {
  const context = inject(MosqueContext);
  const router = inject(Router);
  const mosqueId = route.paramMap.get('mosqueId') ?? '';
  try {
    return (await context.select(mosqueId))
      ? true
      : router.createUrlTree(['/moskeeen'], { queryParams: { melding: 'geen-toegang' } });
  } catch {
    return router.createUrlTree(['/moskeeen']);
  }
};

/** Alleen voor de rol Beheerder van de huidige moskee (moskeegegevens, beheerders, aanmeldingen). */
export const mosqueAdminGuard: CanActivateFn = (route) => {
  const context = inject(MosqueContext);
  if (context.isAdmin()) return true;
  const mosqueId = route.parent?.paramMap.get('mosqueId') ?? context.current()?.id;
  return inject(Router).createUrlTree(mosqueId ? ['/m', mosqueId] : ['/']);
};
