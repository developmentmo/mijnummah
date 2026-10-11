import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { MatButton } from '@angular/material/button';

/** Lege toestand of fouttoestand: uitleg plus een bruikbare vervolgstap. */
@Component({
  selector: 'mu-state-placeholder',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButton],
  template: `
    <h3 class="mu-title-large">{{ heading() }}</h3>
    <p class="mu-body-medium mu-muted">{{ message() }}</p>
    @if (actionLabel()) {
      <button matButton="filled" type="button" (click)="action.emit()">{{ actionLabel() }}</button>
    }
  `,
  styles: `
    :host {
      display: grid;
      justify-items: center;
      gap: var(--mu-space-2);
      padding: var(--mu-space-8) var(--mu-space-6);
      text-align: center;
      border: 1px dashed var(--mu-color-border-strong);
      border-radius: var(--mu-radius-lg);
    }
    button {
      margin-top: var(--mu-space-3);
    }
  `,
})
export class StatePlaceholder {
  readonly heading = input.required<string>();
  readonly message = input.required<string>();
  readonly actionLabel = input<string>();
  readonly action = output<void>();
}
