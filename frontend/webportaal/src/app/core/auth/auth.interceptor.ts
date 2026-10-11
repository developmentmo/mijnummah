import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, from, switchMap, throwError } from 'rxjs';

import { APP_CONFIG } from '../config/app-config';
import { AuthService } from './auth.service';

function withToken(request: HttpRequest<unknown>, token: string | null): HttpRequest<unknown> {
  return token ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : request;
}

/**
 * Zet het access token op verzoeken naar de API. Bij een 401 volgt één refresh en één nieuwe poging; lukt dat
 * niet, dan is de sessie verlopen en gaat de beheerder naar de inlogpagina, terug naar dezelfde pagina na
 * inloggen.
 */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const apiPrefix = `${inject(APP_CONFIG).apiBaseUrl}/api/`;
  if (!request.url.startsWith(apiPrefix)) return next(request);

  return from(auth.validAccessToken().catch(() => null)).pipe(
    switchMap((token) =>
      next(withToken(request, token)).pipe(
        catchError((error: unknown) => {
          if (!(error instanceof HttpErrorResponse) || error.status !== 401 || !token) {
            return throwError(() => error);
          }
          return from(auth.recoverFromRejection(token)).pipe(
            switchMap((fresh) => {
              if (fresh) return next(withToken(request, fresh));
              void router.navigate(['/inloggen'], {
                queryParams: { terug: router.url },
              });
              return throwError(() => error);
            }),
          );
        }),
      ),
    ),
  );
};
