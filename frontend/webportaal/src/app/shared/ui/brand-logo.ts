import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';

import { BRAND } from '../../core/brand/brand.generated';
import { ThemeService } from '../../core/theme/theme.service';

export type BrandLogoVariant = 'horizontal' | 'compact';

/** Het logo van het merk (zie WHITE_LABEL.md) in de variant die bij het actieve thema past. */
@Component({
  selector: 'mu-brand-logo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<img [src]="src()" [style.height.px]="height()" [alt]="name" />`,
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
  protected readonly name = BRAND.name;

  readonly variant = input<BrandLogoVariant>('horizontal');
  readonly height = input(40);

  protected readonly src = computed(
    () => `branding/logo-${this.variant()}-${this.theme.isDark() ? 'dark' : 'light'}.svg`,
  );
}
