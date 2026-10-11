import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButtonToggleModule } from '@angular/material/button-toggle';

import { ThemeMode, ThemeService } from '../../core/theme/theme.service';

/** Keuze tussen systeem, licht en donker. */
@Component({
  selector: 'mu-theme-toggle',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonToggleModule],
  template: `
    <mat-button-toggle-group
      aria-label="Weergave"
      hideSingleSelectionIndicator
      [value]="theme.mode()"
      (change)="setMode($event.value)"
    >
      <mat-button-toggle value="system">Systeem</mat-button-toggle>
      <mat-button-toggle value="light">Licht</mat-button-toggle>
      <mat-button-toggle value="dark">Donker</mat-button-toggle>
    </mat-button-toggle-group>
  `,
})
export class ThemeToggle {
  protected readonly theme = inject(ThemeService);

  protected setMode(mode: ThemeMode): void {
    this.theme.setMode(mode);
  }
}
