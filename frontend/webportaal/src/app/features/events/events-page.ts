import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  input,
  untracked,
} from '@angular/core';
import { MatButton } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatPaginator, PageEvent } from '@angular/material/paginator';
import { Router, RouterLink } from '@angular/router';

import { AdminApi } from '../../core/api/admin-api';
import { AdminEvent, EventStatus, Page } from '../../core/api/models';
import { formatDateTime } from '../../core/format/dates';
import { EVENT_CATEGORY_LABELS, EVENT_STATUS_LABELS } from '../../core/format/labels';
import { MosqueContext } from '../../core/mosque/mosque-context';
import { Loadable } from '../../shared/data/loadable';
import { PageHeader } from '../../shared/ui/page-header';
import { DUTCH_PAGINATOR } from '../../shared/ui/paginator-intl';
import { Skeleton } from '../../shared/ui/skeleton';
import { StatePlaceholder } from '../../shared/ui/state-placeholder';
import { StatusBadge } from '../../shared/ui/status-badge';
import { StatusMessage } from '../../shared/ui/status-message';

const PAGE_SIZE = 20;
const STATUSES: EventStatus[] = ['DRAFT', 'PUBLISHED', 'CANCELLED'];

/** Alle activiteiten, ook concepten en geannuleerde, nieuwste starttijd eerst. Filter en pagina staan in de URL. */
@Component({
  selector: 'mu-events-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [DUTCH_PAGINATOR],
  imports: [
    MatButton,
    MatButtonToggleModule,
    MatPaginator,
    PageHeader,
    RouterLink,
    Skeleton,
    StatePlaceholder,
    StatusBadge,
    StatusMessage,
  ],
  template: `
    <mu-page-header
      heading="Agenda"
      subtitle="Activiteiten die in de app verschijnen na publiceren."
    >
      <a matButton="filled" [routerLink]="['nieuw']">Nieuwe activiteit</a>
    </mu-page-header>

    <div class="mu-toolbar">
      <mat-button-toggle-group
        aria-label="Filter op status"
        hideSingleSelectionIndicator
        [value]="currentStatus() ?? ''"
        (change)="filter($event.value)"
      >
        <mat-button-toggle value="">Alle</mat-button-toggle>
        @for (s of statuses; track s) {
          <mat-button-toggle [value]="s">{{ statusLabels[s] }}</mat-button-toggle>
        }
      </mat-button-toggle-group>
    </div>

    @if (events.error(); as message) {
      <mu-status-message
        kind="error"
        [message]="message"
        actionLabel="Opnieuw proberen"
        (action)="events.load()"
      />
    } @else if (events.value(); as page) {
      @if (page.items.length === 0) {
        <mu-state-placeholder
          [heading]="
            currentStatus() ? 'Geen activiteiten met deze status' : 'Nog geen activiteiten'
          "
          [message]="
            currentStatus()
              ? 'Kies een ander filter of maak een nieuwe activiteit aan.'
              : 'Maak de eerste activiteit aan. Hij blijft een concept tot je hem publiceert.'
          "
          actionLabel="Nieuwe activiteit"
          (action)="create()"
        />
      } @else {
        <div class="mu-table-wrap" [attr.aria-busy]="events.loading()">
          <table class="mu-table">
            <caption class="mu-visually-hidden">
              Activiteiten, nieuwste starttijd eerst
            </caption>
            <thead>
              <tr>
                <th scope="col">Datum en tijd</th>
                <th scope="col">Titel</th>
                <th scope="col">Categorie</th>
                <th scope="col">Status</th>
                <th scope="col">Aanmeldingen</th>
              </tr>
            </thead>
            <tbody>
              @for (event of page.items; track event.id) {
                <tr>
                  <td class="nowrap">{{ when(event) }}</td>
                  <td>
                    <a class="title-link" [routerLink]="[event.id]">{{ event.title }}</a>
                    @if (event.visibility === 'REGISTERED_USERS') {
                      <div class="mu-body-small mu-muted">Alleen voor ingelogde gebruikers</div>
                    }
                  </td>
                  <td>{{ categoryLabels[event.category] }}</td>
                  <td>
                    <mu-status-badge [status]="event.status" [label]="statusLabels[event.status]" />
                  </td>
                  <td class="nowrap">{{ registrations(event) }}</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
        <mat-paginator
          [length]="page.totalItems"
          [pageIndex]="page.page"
          [pageSize]="pageSize"
          [hidePageSize]="true"
          (page)="goTo($event)"
          aria-label="Pagina's met activiteiten"
        />
      }
    } @else {
      <div class="mu-loading-rows" aria-busy="true" aria-label="Activiteiten laden">
        @for (row of [1, 2, 3, 4]; track row) {
          <mu-skeleton height="var(--mu-space-12)" />
        }
      </div>
    }
  `,
})
export class EventsPage {
  private readonly api = inject(AdminApi);
  private readonly router = inject(Router);
  private readonly context = inject(MosqueContext);
  private readonly mosqueId = this.context.current()!.id;

  readonly status = input<string>();
  readonly pagina = input<string>();

  protected readonly statuses = STATUSES;
  protected readonly statusLabels = EVENT_STATUS_LABELS;
  protected readonly categoryLabels = EVENT_CATEGORY_LABELS;
  protected readonly pageSize = PAGE_SIZE;

  protected readonly currentStatus = computed(() =>
    STATUSES.includes(this.status() as EventStatus) ? (this.status() as EventStatus) : null,
  );
  private readonly pageIndex = computed(() => Math.max(0, Number(this.pagina() ?? 1) - 1 || 0));

  protected readonly events = new Loadable<Page<AdminEvent>>(() =>
    this.api.events(this.mosqueId, {
      status: this.currentStatus(),
      page: this.pageIndex(),
      size: PAGE_SIZE,
    }),
  );

  constructor() {
    effect(() => {
      this.currentStatus();
      this.pageIndex();
      untracked(() => void this.events.load());
    });
  }

  protected when(event: AdminEvent): string {
    return formatDateTime(event.startsAt, this.context.timeZone());
  }

  protected registrations(event: AdminEvent): string {
    if (!event.registrationEnabled) return '—';
    return event.capacity
      ? `${event.confirmedRegistrations} / ${event.capacity}`
      : String(event.confirmedRegistrations);
  }

  protected filter(status: string): void {
    void this.router.navigate([], {
      queryParams: { status: status || null, pagina: null },
      queryParamsHandling: 'merge',
    });
  }

  protected goTo(event: PageEvent): void {
    void this.router.navigate([], {
      queryParams: { pagina: event.pageIndex > 0 ? event.pageIndex + 1 : null },
      queryParamsHandling: 'merge',
    });
  }

  protected create(): void {
    void this.router.navigate(['/m', this.mosqueId, 'agenda', 'nieuw']);
  }
}
