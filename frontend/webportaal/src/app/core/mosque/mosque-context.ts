import { HttpClient } from '@angular/common/http';
import { Injectable, computed, effect, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { AdminMosque, MosqueRole } from '../api/models';
import { AuthService } from '../auth/auth.service';
import { APP_CONFIG } from '../config/app-config';
import { DEFAULT_TIME_ZONE } from '../format/dates';

/**
 * De moskeeën die de ingelogde beheerder beheert en de moskee waarin hij nu werkt. De rol bepaalt alleen wat
 * het portaal toont; de backend controleert de rechten bij elke aanroep opnieuw.
 */
@Injectable({ providedIn: 'root' })
export class MosqueContext {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly baseUrl = inject(APP_CONFIG).apiBaseUrl;

  private loading?: Promise<AdminMosque[]>;
  private readonly list = signal<AdminMosque[] | null>(null);
  private readonly selected = signal<AdminMosque | null>(null);

  readonly mosques = this.list.asReadonly();
  readonly current = this.selected.asReadonly();

  /** Eigen rol, of ADMIN voor een platformbeheerder (die heeft overal alle rechten). */
  readonly role = computed<MosqueRole | null>(
    () => this.selected()?.myRole ?? (this.auth.isPlatformAdmin() ? 'ADMIN' : null),
  );
  readonly isAdmin = computed(() => this.role() === 'ADMIN');
  readonly timeZone = computed(() => this.selected()?.timeZone || DEFAULT_TIME_ZONE);

  constructor() {
    // Een andere gebruiker in hetzelfde tabblad krijgt nooit de lijst van de vorige.
    effect(() => {
      if (!this.auth.isSignedIn()) this.reset();
    });
  }

  load(force = false): Promise<AdminMosque[]> {
    if (force || (!this.list() && !this.loading)) {
      this.loading = firstValueFrom(
        this.http.get<AdminMosque[]>(`${this.baseUrl}/api/v1/admin/mosques`),
      )
        .then((mosques) => {
          this.list.set(mosques);
          return mosques;
        })
        .finally(() => (this.loading = undefined));
    }
    return this.loading ?? Promise.resolve(this.list() ?? []);
  }

  /** Kiest de moskee uit de route; null als de beheerder er geen toegang toe heeft. */
  async select(mosqueId: string): Promise<AdminMosque | null> {
    const mosques = await this.load();
    const mosque = mosques.find((m) => m.id === mosqueId) ?? null;
    this.selected.set(mosque);
    return mosque;
  }

  /** Na het wijzigen van de moskeegegevens. */
  replace(mosque: AdminMosque): void {
    this.list.update((list) => list?.map((m) => (m.id === mosque.id ? mosque : m)) ?? null);
    if (this.selected()?.id === mosque.id) this.selected.set(mosque);
  }

  private reset(): void {
    this.list.set(null);
    this.selected.set(null);
    this.loading = undefined;
  }
}
