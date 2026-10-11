import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'mu-not-found-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButton, RouterLink],
  template: `
    <main id="inhoud">
      <h1 class="mu-headline-medium">Pagina niet gevonden</h1>
      <p class="mu-body-large mu-muted">Deze pagina bestaat niet (meer). Controleer de link.</p>
      <a matButton="filled" routerLink="/">Naar het beheerportaal</a>
    </main>
  `,
  styles: `
    main {
      display: grid;
      justify-items: start;
      gap: var(--mu-space-4);
      max-width: var(--mu-content-max-width);
      padding: var(--mu-space-16) var(--mu-space-6);
    }
  `,
})
export class NotFoundPage {}
