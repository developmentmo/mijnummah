import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  input,
  signal,
  untracked,
} from '@angular/core';
import { MatButton } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { MatPaginator, PageEvent } from '@angular/material/paginator';
import { Router, RouterLink } from '@angular/router';

import { AdminApi } from '../../core/api/admin-api';
import { AdminMedia, MediaStatus, Page } from '../../core/api/models';
import { formatCalendarDate, formatDuration } from '../../core/format/dates';
import { CONTENT_STATUS_LABELS, MEDIA_CATEGORY_LABELS } from '../../core/format/labels';
import { MosqueContext } from '../../core/mosque/mosque-context';
import { Loadable } from '../../shared/data/loadable';
import { PageHeader } from '../../shared/ui/page-header';
import { DUTCH_PAGINATOR } from '../../shared/ui/paginator-intl';
import { Skeleton } from '../../shared/ui/skeleton';
import { StatePlaceholder } from '../../shared/ui/state-placeholder';
import { StatusBadge } from '../../shared/ui/status-badge';
import { StatusMessage } from '../../shared/ui/status-message';

const PAGE_SIZE = 20;
const STATUSES: MediaStatus[] = ['DRAFT', 'PUBLISHED'];

/** De mediabibliotheek: lezingen, khutbahs en andere video's. Filter, zoekterm en pagina staan in de URL. */
@Component({
  selector: 'mu-media-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [DUTCH_PAGINATOR],
  imports: [
    MatButton,
    MatButtonToggleModule,
    MatFormFieldModule,
    MatInput,
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
      heading="Lezingen en video’s"
      subtitle="YouTube-video’s in de mediabibliotheek van de app."
    >
      <a matButton="filled" [routerLink]="['nieuw']">Nieuwe video</a>
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
      <form role="search" (submit)="$event.preventDefault(); search()">
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>Zoeken op titel of spreker</mat-label>
          <input
            matInput
            type="search"
            [value]="q() ?? ''"
            (input)="draft.set($any($event.target).value)"
          />
        </mat-form-field>
        <button matButton="tonal" type="submit">Zoeken</button>
      </form>
    </div>

    @if (media.error(); as message) {
      <mu-status-message
        kind="error"
        [message]="message"
        actionLabel="Opnieuw proberen"
        (action)="media.load()"
      />
    } @else if (media.value(); as page) {
      @if (page.items.length === 0) {
        <mu-state-placeholder
          [heading]="filtered() ? 'Niets gevonden' : 'Nog geen video’s'"
          [message]="
            filtered()
              ? 'Pas het filter of de zoekterm aan.'
              : 'Voeg een YouTube-video toe. Hij blijft een concept tot je hem publiceert.'
          "
          actionLabel="Nieuwe video"
          (action)="create()"
        />
      } @else {
        <div class="mu-table-wrap" [attr.aria-busy]="media.loading()">
          <table class="mu-table">
            <caption class="mu-visually-hidden">
              Media-items, laatst gewijzigd eerst
            </caption>
            <thead>
              <tr>
                <th scope="col">Titel</th>
                <th scope="col">Categorie</th>
                <th scope="col">Opgenomen</th>
                <th scope="col">Duur</th>
                <th scope="col">Status</th>
              </tr>
            </thead>
            <tbody>
              @for (item of page.items; track item.id) {
                <tr>
                  <td>
                    <a class="title-link" [routerLink]="[item.id]">{{ item.title }}</a>
                    @if (item.speaker) {
                      <div class="mu-body-small mu-muted">{{ item.speaker }}</div>
                    }
                  </td>
                  <td>{{ categoryLabels[item.category] }}</td>
                  <td class="nowrap">
                    {{ item.recordedOn ? calendarDate(item.recordedOn) : '—' }}
                  </td>
                  <td class="nowrap">{{ duration(item.durationSeconds) || '—' }}</td>
                  <td>
                    <mu-status-badge [status]="item.status" [label]="statusLabels[item.status]" />
                  </td>
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
          aria-label="Pagina's met media"
        />
      }
    } @else {
      <div class="mu-loading-rows" aria-busy="true" aria-label="Media laden">
        @for (row of [1, 2, 3, 4]; track row) {
          <mu-skeleton height="var(--mu-space-12)" />
        }
      </div>
    }
  `,
  styles: `
    form {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: var(--mu-space-2);
    }
  `,
})
export class MediaPage {
  private readonly api = inject(AdminApi);
  private readonly router = inject(Router);
  private readonly mosqueId = inject(MosqueContext).current()!.id;

  readonly status = input<string>();
  readonly q = input<string>();
  readonly pagina = input<string>();

  protected readonly statuses = STATUSES;
  protected readonly statusLabels = CONTENT_STATUS_LABELS;
  protected readonly categoryLabels = MEDIA_CATEGORY_LABELS;
  protected readonly pageSize = PAGE_SIZE;
  protected readonly calendarDate = formatCalendarDate;
  protected readonly duration = formatDuration;
  protected readonly draft = signal('');

  protected readonly currentStatus = computed(() =>
    STATUSES.includes(this.status() as MediaStatus) ? (this.status() as MediaStatus) : null,
  );
  protected readonly filtered = computed(() => !!this.currentStatus() || !!this.q()?.trim());
  private readonly pageIndex = computed(() => Math.max(0, Number(this.pagina() ?? 1) - 1 || 0));

  protected readonly media = new Loadable<Page<AdminMedia>>(() =>
    this.api.mediaList(this.mosqueId, {
      status: this.currentStatus(),
      q: this.q()?.trim() || null,
      page: this.pageIndex(),
      size: PAGE_SIZE,
    }),
  );

  constructor() {
    effect(() => {
      this.currentStatus();
      this.q();
      this.pageIndex();
      untracked(() => {
        this.draft.set(this.q() ?? '');
        void this.media.load();
      });
    });
  }

  protected search(): void {
    this.navigate({ q: this.draft().trim() || null, pagina: null });
  }

  protected goTo(event: PageEvent): void {
    this.navigate({ pagina: event.pageIndex > 0 ? event.pageIndex + 1 : null });
  }

  protected navigate(queryParams: Record<string, string | number | null>): void {
    void this.router.navigate([], { queryParams, queryParamsHandling: 'merge' });
  }

  protected create(): void {
    void this.router.navigate(['/m', this.mosqueId, 'media', 'nieuw']);
  }
}
