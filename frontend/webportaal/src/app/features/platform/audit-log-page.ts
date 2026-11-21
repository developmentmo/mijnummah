import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';

import { AdminApi } from '../../core/api/admin-api';
import { auditActionLabel } from '../../core/format/audit';
import { formatDateTime } from '../../core/format/dates';
import { Loadable } from '../../shared/data/loadable';
import { PageHeader } from '../../shared/ui/page-header';
import { DUTCH_PAGINATOR } from '../../shared/ui/paginator-intl';
import { Skeleton } from '../../shared/ui/skeleton';
import { StatePlaceholder } from '../../shared/ui/state-placeholder';
import { StatusMessage } from '../../shared/ui/status-message';

const PAGE_SIZE = 25;

/**
 * Platformbeheer: wie wat heeft gedaan, nieuwste eerst, voor het hele platform of één moskee. Het log bevat alleen
 * ID's en namen, geen inhoud, en wordt 730 dagen bewaard.
 */
@Component({
  selector: 'mu-audit-log-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [DUTCH_PAGINATOR],
  imports: [
    MatFormFieldModule,
    MatPaginator,
    MatSelectModule,
    PageHeader,
    Skeleton,
    StatePlaceholder,
    StatusMessage,
  ],
  template: `
    <mu-page-header
      heading="Auditlog"
      subtitle="Alle beheeracties op het platform, nieuwste eerst. Bewaard voor 730 dagen."
    />

    <mat-form-field appearance="outline" class="filter">
      <mat-label>Moskee</mat-label>
      <mat-select [value]="mosqueId()" (valueChange)="filter($event)">
        <mat-option [value]="null">Hele platform</mat-option>
        @for (m of mosques.value() ?? []; track m.id) {
          <mat-option [value]="m.id">{{ m.name }}</mat-option>
        }
      </mat-select>
    </mat-form-field>

    @if (entries.error(); as message) {
      <mu-status-message
        kind="error"
        [message]="message"
        actionLabel="Opnieuw proberen"
        (action)="entries.load()"
      />
    } @else if (entries.value(); as page) {
      @if (page.items.length === 0) {
        <mu-state-placeholder
          heading="Geen activiteiten"
          message="Voor deze selectie zijn nog geen beheeracties vastgelegd."
        />
      } @else {
        <div class="mu-table-wrap" [attr.aria-busy]="entries.loading()">
          <table class="mu-table">
            <caption class="mu-visually-hidden">
              Beheeracties, nieuwste eerst
            </caption>
            <thead>
              <tr>
                <th scope="col">Wanneer</th>
                <th scope="col">Actie</th>
                <th scope="col">Moskee</th>
                <th scope="col">Door</th>
              </tr>
            </thead>
            <tbody>
              @for (entry of page.items; track entry.id) {
                <tr>
                  <td class="nowrap">{{ when(entry.occurredAt) }}</td>
                  <td>{{ label(entry.action) }}</td>
                  <td>{{ entry.mosqueName ?? 'Platform' }}</td>
                  <td>{{ entry.actor?.displayName ?? 'Verwijderd account' }}</td>
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
          aria-label="Pagina's van het auditlog"
        />
      }
    } @else {
      <div class="mu-loading-rows" aria-busy="true" aria-label="Auditlog laden">
        <mu-skeleton height="var(--mu-space-12)" />
        <mu-skeleton height="var(--mu-space-12)" />
      </div>
    }
  `,
  styles: `
    :host {
      display: grid;
      gap: var(--mu-space-4);
    }
    .filter {
      max-width: calc(var(--mu-space-16) * 5);
    }
  `,
})
export class AuditLogPage implements OnInit {
  private readonly api = inject(AdminApi);

  protected readonly mosqueId = signal<string | null>(null);
  private readonly pageIndex = signal(0);
  protected readonly pageSize = PAGE_SIZE;
  protected readonly mosques = new Loadable(() => this.api.platformMosques());
  protected readonly entries = new Loadable(() =>
    this.api.auditLog({ mosqueId: this.mosqueId(), page: this.pageIndex(), size: PAGE_SIZE }),
  );

  ngOnInit(): void {
    void this.mosques.load();
    void this.entries.load();
  }

  protected filter(mosqueId: string | null): void {
    this.mosqueId.set(mosqueId);
    this.pageIndex.set(0);
    void this.entries.load();
  }

  protected goTo(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    void this.entries.load();
  }

  protected label(action: string): string {
    return auditActionLabel(action);
  }

  protected when(iso: string): string {
    return formatDateTime(iso);
  }
}
