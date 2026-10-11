import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

/** Paginatitel met optioneel een terugkoppeling en ruimte voor acties rechts. */
@Component({
  selector: 'mu-page-header',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink],
  template: `
    @if (backLink()) {
      <a class="back mu-label-large" [routerLink]="backLink()">← {{ backLabel() }}</a>
    }
    <div class="row">
      <div class="titles">
        <h1 class="mu-headline-medium">{{ heading() }}</h1>
        @if (subtitle()) {
          <p class="mu-body-medium mu-muted">{{ subtitle() }}</p>
        }
      </div>
      <div class="actions"><ng-content /></div>
    </div>
  `,
  styles: `
    :host {
      display: grid;
      gap: var(--mu-space-2);
      margin-bottom: var(--mu-space-6);
    }
    .back {
      justify-self: start;
      text-decoration: none;
    }
    .back:hover {
      text-decoration: underline;
    }
    .row {
      display: flex;
      flex-wrap: wrap;
      align-items: flex-end;
      justify-content: space-between;
      gap: var(--mu-space-4);
    }
    .titles {
      display: grid;
      gap: var(--mu-space-1);
    }
    .actions {
      display: flex;
      flex-wrap: wrap;
      gap: var(--mu-space-2);
    }
  `,
})
export class PageHeader {
  readonly heading = input.required<string>();
  readonly subtitle = input<string>();
  readonly backLink = input<string | unknown[]>();
  readonly backLabel = input('Terug');
}
