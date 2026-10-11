import { signal } from '@angular/core';
import { Observable, firstValueFrom } from 'rxjs';

import { describeError } from '../../core/api/api-error';

/**
 * Laadtoestand van één stuk data: bezig, fout (als Nederlandse melding) of waarde. Een nieuwere aanvraag
 * wint altijd van een oudere die later terugkomt.
 */
export class Loadable<T> {
  private requestId = 0;
  readonly value = signal<T | null>(null);
  readonly error = signal<string | null>(null);
  readonly loading = signal(false);

  constructor(
    private readonly fetch: () => Observable<T>,
    private readonly errorMessages: Record<string, string> = {},
  ) {}

  async load(): Promise<void> {
    const id = ++this.requestId;
    this.loading.set(true);
    this.error.set(null);
    try {
      const value = await firstValueFrom(this.fetch());
      if (id === this.requestId) this.value.set(value);
    } catch (error) {
      if (id === this.requestId) this.error.set(describeError(error, this.errorMessages));
    } finally {
      if (id === this.requestId) this.loading.set(false);
    }
  }

  set(value: T): void {
    this.value.set(value);
  }
}
