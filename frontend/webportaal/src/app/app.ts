import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { RouterOutlet } from '@angular/router';

import { ThemeMode, ThemeService } from './core/theme/theme.service';
import { BrandLogo } from './shared/ui/brand-logo';

@Component({
  selector: 'mu-root',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BrandLogo, MatButtonToggleModule, RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  protected readonly theme = inject(ThemeService);

  protected setMode(mode: ThemeMode): void {
    this.theme.setMode(mode);
  }
}
