import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';

import { ThemeService } from '../../core/theme/theme.service';

export type BrandLogoVariant = 'horizontal' | 'compact';

/** Het Noeroel Islam-logo in de variant die bij het actieve thema past. */
@Component({
  selector: 'mu-brand-logo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<img [src]="src()" [style.height.px]="height()" alt="Noeroel Islam" />`,
  styles: `
    :host {
      display: inline-flex;
    }
    img {
      display: block;
      width: auto;
    }
  `,
})
export class BrandLogo {
  private readonly theme = inject(ThemeService);

  readonly variant = input<BrandLogoVariant>('horizontal');
  readonly height = input(40);

  protected readonly src = computed(
    () => `branding/logo-${this.variant()}-${this.theme.isDark() ? 'dark' : 'light'}.svg`,
  );
}
