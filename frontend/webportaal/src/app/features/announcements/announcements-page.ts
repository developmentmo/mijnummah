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
import { AdminAnnouncement, AnnouncementStatus, Page } from '../../core/api/models';
import { formatDateTime } from '../../core/format/dates';
import { CONTENT_STATUS_LABELS } from '../../core/format/labels';
import { MosqueContext } from '../../core/mosque/mosque-context';
import { Loadable } from '../../shared/data/loadable';
import { PageHeader } from '../../shared/ui/page-header';
import { DUTCH_PAGINATOR } from '../../shared/ui/paginator-intl';
import { Skeleton } from '../../shared/ui/skeleton';
import { StatePlaceholder } from '../../shared/ui/state-placeholder';
import { StatusBadge } from '../../shared/ui/status-badge';
import { StatusMessage } from '../../shared/ui/status-message';

const PAGE_SIZE = 20;
const STATUSES: AnnouncementStatus[] = ['DRAFT', 'PUBLISHED'];

/** Aankondigingen, laatst gewijzigd eerst. */
@Component({
  selector: 'mu-announcements-page',
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
    <mu-page-header heading="Aankondigingen" subtitle="Berichten op de startpagina van de app.">
      <a matButton="filled" [routerLink]="['nieuw']">Nieuwe aankondiging</a>
    </mu-page-header>

    <div class="mu-toolbar">
      <mat-button-toggle-group
        aria-label="Filter op status"
        hideSingleSelectionIndicator
        [value]="currentStatus() ?? ''"
        (change)="navigate({ status: $event.value || null, pagina: null })"
      >
        <mat-button-toggle value="">Alle</mat-button-toggle>
        @for (s of statuses; track s) {
          <mat-button-toggle [value]="s">{{ statusLabels[s] }}</mat-button-toggle>
        }
      </mat-button-toggle-group>
    </div>

    @if (announcements.error(); as message) {
      <mu-status-message
        kind="error"
        [message]="message"
        actionLabel="Opnieuw proberen"
        (action)="announcements.load()"
      />
    } @else if (announcements.value(); as page) {
      @if (page.items.length === 0) {
        <mu-state-placeholder
          [heading]="
            currentStatus() ? 'Geen aankondigingen met deze status' : 'Nog geen aankondigingen'
          "
          message="Een aankondiging blijft een concept tot je hem publiceert."
          actionLabel="Nieuwe aankondiging"
          (action)="create()"
        />
      } @else {
        <div class="mu-table-wrap" [attr.aria-busy]="announcements.loading()">
          <table class="mu-table">
            <caption class="mu-visually-hidden">
              Aankondigingen, laatst gewijzigd eerst
            </caption>
            <thead>
              <tr>
                <th scope="col">Titel</th>
                <th scope="col">Status</th>
                <th scope="col">Gepubliceerd</th>
                <th scope="col">Verloopt</th>
              </tr>
            </thead>
            <tbody>
              @for (a of page.items; track a.id) {
                <tr>
                  <td>
                    <a class="title-link" [routerLink]="[a.id]">{{ a.title }}</a>
                    @if (a.featured) {
                      <div class="mu-body-small mu-muted">Uitgelicht op de startpagina</div>
                    }
                  </td>
                  <td>
                    @if (a.status === 'PUBLISHED' && a.expired) {
                      <mu-status-badge status="EXPIRED" label="Verlopen" />
                    } @else {
                      <mu-status-badge [status]="a.status" [label]="statusLabels[a.status]" />
                    }
                  </td>
                  <td class="nowrap">{{ a.publishedAt ? when(a.publishedAt) : '—' }}</td>
                  <td class="nowrap">{{ a.expiresAt ? when(a.expiresAt) : 'Niet' }}</td>
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
          aria-label="Pagina's met aankondigingen"
        />
      }
    } @else {
      <div class="mu-loading-rows" aria-busy="true" aria-label="Aankondigingen laden">
        @for (row of [1, 2, 3]; track row) {
          <mu-skeleton height="var(--mu-space-12)" />
        }
      </div>
    }
  `,
})
export class AnnouncementsPage {
  private readonly api = inject(AdminApi);
  private readonly router = inject(Router);
  private readonly context = inject(MosqueContext);
  private readonly mosqueId = this.context.current()!.id;

  readonly status = input<string>();
  readonly pagina = input<string>();

  protected readonly statuses = STATUSES;
  protected readonly statusLabels = CONTENT_STATUS_LABELS;
  protected readonly pageSize = PAGE_SIZE;

  protected readonly currentStatus = computed(() =>
    STATUSES.includes(this.status() as AnnouncementStatus)
      ? (this.status() as AnnouncementStatus)
      : null,
  );
  private readonly pageIndex = computed(() => Math.max(0, Number(this.pagina() ?? 1) - 1 || 0));

  protected readonly announcements = new Loadable<Page<AdminAnnouncement>>(() =>
    this.api.announcements(this.mosqueId, {
      status: this.currentStatus(),
      page: this.pageIndex(),
      size: PAGE_SIZE,
    }),
  );

  constructor() {
    effect(() => {
      this.currentStatus();
      this.pageIndex();
      untracked(() => void this.announcements.load());
    });
  }

  protected when(iso: string): string {
    return formatDateTime(iso, this.context.timeZone());
  }

  protected goTo(event: PageEvent): void {
    this.navigate({ pagina: event.pageIndex > 0 ? event.pageIndex + 1 : null });
  }

  protected navigate(queryParams: Record<string, string | number | null>): void {
    void this.router.navigate([], { queryParams, queryParamsHandling: 'merge' });
  }

  protected create(): void {
    void this.router.navigate(['/m', this.mosqueId, 'aankondigingen', 'nieuw']);
  }
}
