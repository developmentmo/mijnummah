import { Injectable, inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

/** Korte bevestiging na een geslaagde actie. Wordt beleefd voorgelezen; fouten staan op de pagina zelf. */
@Injectable({ providedIn: 'root' })
export class Notifier {
  private readonly snackBar = inject(MatSnackBar);

  success(message: string): void {
    this.snackBar.open(message, 'Sluiten', { duration: 5000, politeness: 'polite' });
  }
}
