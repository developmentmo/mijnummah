import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Plaatshouder tijdens het laden. Wordt door screenreaders overgeslagen. */
@Component({
  selector: 'mu-skeleton',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    'aria-hidden': 'true',
    '[style.width]': 'width()',
    '[style.height]': 'height()',
    '[style.border-radius]': 'radius()',
  },
  template: '',
  styles: `
    :host {
      display: block;
      background: var(--mu-color-skeleton);
      animation: mu-pulse calc(var(--mu-duration-slow) * 4) var(--mu-easing-standard) infinite
        alternate;
    }
    @keyframes mu-pulse {
      to {
        background: var(--mu-color-skeleton-highlight);
      }
    }
  `,
})
export class Skeleton {
  readonly width = input('100%');
  readonly height = input('var(--mu-space-4)');
  readonly radius = input('var(--mu-radius-sm)');
}
