import { ChangeDetectionStrategy, Component, OnInit, computed, inject, input } from '@angular/core';
import { forkJoin } from 'rxjs';

import { AdminApi } from '../../core/api/admin-api';
import { AdminEvent, Registrant } from '../../core/api/models';
import { formatDateTime } from '../../core/format/dates';
import { MosqueContext } from '../../core/mosque/mosque-context';
import { Loadable } from '../../shared/data/loadable';
import { PageHeader } from '../../shared/ui/page-header';
import { Skeleton } from '../../shared/ui/skeleton';
import { StatePlaceholder } from '../../shared/ui/state-placeholder';
import { StatusBadge } from '../../shared/ui/status-badge';
import { StatusMessage } from '../../shared/ui/status-message';

/** Aanmeldingen voor één activiteit. Alleen voor de rol Beheerder: bevat namen en e-mailadressen. */
@Component({
  selector: 'mu-registrations-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [PageHeader, Skeleton, StatePlaceholder, StatusBadge, StatusMessage],
  template: `
    <mu-page-header
      heading="Aanmeldingen"
      [subtitle]="data.value()?.event?.title"
      [backLink]="['/m', mosqueId, 'agenda', eventId()]"
      backLabel="Activiteit"
    />

    @if (data.error(); as message) {
      <mu-status-message
        kind="error"
        [message]="message"
        actionLabel="Opnieuw proberen"
        (action)="data.load()"
      />
    } @else if (data.value(); as value) {
      <p class="mu-body-medium summary">
        {{ confirmed().length }} {{ confirmed().length === 1 ? 'aanmelding' : 'aanmeldingen' }}
        @if (value.event.capacity) {
          van {{ value.event.capacity }} plaatsen
        }
        @if (cancelledCount() > 0) {
          · {{ cancelledCount() }} afgemeld
        }
      </p>
      <mu-status-message
        kind="info"
        message="Deze lijst bevat persoonsgegevens. Gebruik ze alleen voor deze activiteit en deel ze niet verder."
      />
      @if (value.registrants.length === 0) {
        <mu-state-placeholder
          heading="Nog geen aanmeldingen"
          message="Zodra iemand zich via de app aanmeldt, staat hij hier."
        />
      } @else {
        <div class="mu-table-wrap">
          <table class="mu-table">
            <caption class="mu-visually-hidden">
              Aanmeldingen voor
              {{
                value.event.title
              }}
            </caption>
            <thead>
              <tr>
                <th scope="col">Naam</th>
                <th scope="col">E-mailadres</th>
                <th scope="col">Status</th>
                <th scope="col">Aangemeld op</th>
              </tr>
            </thead>
            <tbody>
              @for (r of value.registrants; track r.registrationId) {
                <tr>
                  <td>{{ r.displayName }}</td>
                  <td>
                    <a [href]="'mailto:' + r.email">{{ r.email }}</a>
                  </td>
                  <td>
                    <mu-status-badge
                      [status]="r.status === 'CONFIRMED' ? 'PUBLISHED' : 'CANCELLED'"
                      [label]="r.status === 'CONFIRMED' ? 'Aangemeld' : 'Afgemeld'"
                    />
                  </td>
                  <td class="nowrap">{{ when(r.registeredAt) }}</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    } @else {
      <div class="mu-loading-rows" aria-busy="true" aria-label="Aanmeldingen laden">
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
    .summary {
      margin: 0;
    }
  `,
})
export class RegistrationsPage implements OnInit {
  private readonly api = inject(AdminApi);
  private readonly context = inject(MosqueContext);
  protected readonly mosqueId = this.context.current()!.id;

  readonly eventId = input.required<string>();

  protected readonly data = new Loadable<{ event: AdminEvent; registrants: Registrant[] }>(() =>
    forkJoin({
      event: this.api.event(this.mosqueId, this.eventId()),
      registrants: this.api.registrations(this.mosqueId, this.eventId()),
    }),
  );

  protected readonly confirmed = computed(
    () => this.data.value()?.registrants.filter((r) => r.status === 'CONFIRMED') ?? [],
  );
  protected readonly cancelledCount = computed(
    () => (this.data.value()?.registrants.length ?? 0) - this.confirmed().length,
  );

  ngOnInit(): void {
    void this.data.load();
  }

  protected when(iso: string): string {
    return formatDateTime(iso, this.context.timeZone());
  }
}
