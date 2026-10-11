import { Injectable } from '@angular/core';

const KEY = 'mu-refresh-token';

/**
 * Bewaart het refresh token in sessionStorage: het overleeft een herlaadactie, maar verdwijnt als het tabblad
 * sluit en is niet zichtbaar in andere tabbladen. Het access token staat alleen in het geheugen.
 */
@Injectable({ providedIn: 'root' })
export class RefreshTokenStore {
  read(): string | null {
    try {
      return sessionStorage.getItem(KEY);
    } catch {
      return null;
    }
  }

  write(token: string): void {
    try {
      sessionStorage.setItem(KEY, token);
    } catch {
      // Opslag geblokkeerd: de sessie werkt tot de pagina herladen wordt.
    }
  }

  clear(): void {
    try {
      sessionStorage.removeItem(KEY);
    } catch {
      // Niets te wissen.
    }
  }
}
