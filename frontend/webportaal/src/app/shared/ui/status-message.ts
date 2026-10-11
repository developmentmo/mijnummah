import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

export type StatusKind = 'info' | 'success' | 'warning' | 'error';

const LABELS: Record<StatusKind, string> = {
  info: 'Informatie',
  success: 'Gelukt',
  warning: 'Let op',
  error: 'Fout',
};

const ICONS: Record<StatusKind, string> = {
  info: 'M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20Zm1 15h-2v-6h2v6Zm0-8h-2V7h2v2Z',
  success: 'M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20Zm-2 15-5-5 1.4-1.4 3.6 3.6 7.6-7.6L19 8l-9 9Z',
  warning: 'M1 21h22L12 2 1 21Zm12-3h-2v-2h2v2Zm0-4h-2v-4h2v4Z',
  error: 'M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20Zm1 15h-2v-2h2v2Zm0-4h-2V7h2v6Z',
};

/**
 * Melding binnen een pagina, met optioneel een herstelactie. De soort is ook zonder kleur herkenbaar
 * (icoon en voorleesbaar label). Foutmeldingen worden direct voorgelezen.
 */
@Component({
  selector: 'mu-status-message',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '[class]': '"mu-status mu-status--" + kind()',
    '[attr.role]': 'kind() === "error" ? "alert" : "status"',
  },
  template: `
    <svg class="mu-status__icon" viewBox="0 0 24 24" aria-hidden="true">
      <path [attr.d]="icon()" />
    </svg>
    <div class="mu-status__body">
      <span class="mu-visually-hidden">{{ label() }}:</span>
      @if (heading()) {
        <p class="mu-title-small">{{ heading() }}</p>
      }
      <p class="mu-body-medium">{{ message() }}</p>
      @if (actionLabel()) {
        <button type="button" class="mu-status__action mu-label-large" (click)="action.emit()">
          {{ actionLabel() }}
        </button>
      }
    </div>
  `,
  styles: `
    :host {
      display: flex;
      gap: var(--mu-space-3);
      padding: var(--mu-notification-padding);
      border-radius: var(--mu-notification-radius);
      background: var(--status-bg);
      color: var(--status-fg);
    }
    :host(.mu-status--info) {
      --status-bg: var(--mu-color-info-container);
      --status-fg: var(--mu-color-on-info-container);
    }
    :host(.mu-status--success) {
      --status-bg: var(--mu-color-success-container);
      --status-fg: var(--mu-color-on-success-container);
    }
    :host(.mu-status--warning) {
      --status-bg: var(--mu-color-warning-container);
      --status-fg: var(--mu-color-on-warning-container);
    }
    :host(.mu-status--error) {
      --status-bg: var(--mu-color-error-container);
      --status-fg: var(--mu-color-on-error-container);
    }
    .mu-status__icon {
      flex: none;
      width: var(--mu-icon-lg);
      height: var(--mu-icon-lg);
      fill: currentColor;
    }
    .mu-status__body {
      display: grid;
      gap: var(--mu-space-1);
    }
    .mu-status__action {
      justify-self: start;
      min-height: var(--mu-touch-target);
      padding: 0;
      border: 0;
      background: none;
      color: inherit;
      text-decoration: underline;
      cursor: pointer;
    }
  `,
})
export class StatusMessage {
  readonly kind = input.required<StatusKind>();
  readonly message = input.required<string>();
  readonly heading = input<string>();
  readonly actionLabel = input<string>();
  readonly action = output<void>();

  protected readonly label = computed(() => LABELS[this.kind()]);
  protected readonly icon = computed(() => ICONS[this.kind()]);
}
