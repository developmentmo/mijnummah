import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

export type BadgeTone = 'draft' | 'published' | 'cancelled' | 'neutral';

const TONES: Record<string, BadgeTone> = {
  DRAFT: 'draft',
  PUBLISHED: 'published',
  CANCELLED: 'cancelled',
  BLOCKED: 'cancelled',
  ACTIVE: 'published',
};

/** Status als tekstlabel met een kleur. De tekst draagt de betekenis; de kleur ondersteunt alleen. */
@Component({
  selector: 'mu-status-badge',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '[class]': '"tone-" + tone()' },
  template: `{{ label() }}`,
  styles: `
    :host {
      display: inline-block;
      padding: var(--mu-space-1) var(--mu-space-2);
      border-radius: var(--mu-radius-full);
      font: var(--mu-type-label-medium-weight) var(--mu-type-label-medium-size) /
        var(--mu-type-label-medium-line-height) var(--mu-type-label-medium-family);
      white-space: nowrap;
    }
    :host(.tone-draft) {
      background: var(--mu-color-warning-container);
      color: var(--mu-color-on-warning-container);
    }
    :host(.tone-published) {
      background: var(--mu-color-success-container);
      color: var(--mu-color-on-success-container);
    }
    :host(.tone-cancelled) {
      background: var(--mu-color-error-container);
      color: var(--mu-color-on-error-container);
    }
    :host(.tone-neutral) {
      background: var(--mu-color-surface-muted);
      color: var(--mu-color-on-surface);
    }
  `,
})
export class StatusBadge {
  readonly status = input.required<string>();
  readonly label = input.required<string>();
  protected readonly tone = computed<BadgeTone>(() => TONES[this.status()] ?? 'neutral');
}
