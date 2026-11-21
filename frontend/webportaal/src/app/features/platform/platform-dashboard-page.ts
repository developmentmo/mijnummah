import { ChangeDetectionStrategy, Component, OnInit, computed, inject } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { RouterLink } from '@angular/router';

import { AdminApi } from '../../core/api/admin-api';
import { auditActionLabel } from '../../core/format/audit';
import { formatDateTime } from '../../core/format/dates';
import { Loadable } from '../../shared/data/loadable';
import { PageHeader } from '../../shared/ui/page-header';
import { Skeleton } from '../../shared/ui/skeleton';
import { StatePlaceholder } from '../../shared/ui/state-placeholder';
import { StatusMessage } from '../../shared/ui/status-message';

interface Card {
  label: string;
  value: number;
  hint: string;
}

/** Platformbeheer: de totalen van het platform en de laatste beheeracties. */
@Component({
  selector: 'mu-platform-dashboard-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButton, PageHeader, RouterLink, Skeleton, StatePlaceholder, StatusMessage],
  template: `
    <mu-page-header heading="Platform" subtitle="Overzicht van alle moskeeën op MijnUmmah.">
      <a matButton="filled" routerLink="/platform/moskeeen/nieuw">Moskee registreren</a>
    </mu-page-header>

    <section aria-labelledby="cijfers">
      <h2 id="cijfers" class="mu-visually-hidden">Cijfers</h2>
      @if (totals.error(); as message) {
        <mu-status-message
          kind="error"
          [message]="message"
          actionLabel="Opnieuw proberen"
          (action)="totals.load()"
        />
      } @else if (cards(); as list) {
        <ul class="cards">
          @for (card of list; track card.label) {
            <li class="card">
              <span class="mu-display-small count">{{ card.value }}</span>
              <span class="mu-title-small">{{ card.label }}</span>
              <span class="mu-body-small mu-muted">{{ card.hint }}</span>
            </li>
          }
        </ul>
      } @else {
        <div class="cards" aria-busy="true" aria-label="Cijfers laden">
          @for (i of [1, 2, 3, 4]; track i) {
            <mu-skeleton height="calc(var(--mu-space-16) * 1.5)" />
          }
        </div>
      }
    </section>

    <section aria-labelledby="recent" class="panel">
      <div class="panel__head">
        <h2 id="recent" class="mu-title-large">Recente activiteiten</h2>
        <a class="mu-label-large" routerLink="/platform/auditlog">Hele auditlog</a>
      </div>
      @if (recent.error(); as message) {
        <mu-status-message
          kind="error"
          [message]="message"
          actionLabel="Opnieuw proberen"
          (action)="recent.load()"
        />
      } @else if (recent.value(); as page) {
        @if (page.items.length === 0) {
          <mu-state-placeholder
            heading="Nog geen activiteiten"
            message="Beheeracties verschijnen hier zodra iemand iets wijzigt."
          />
        } @else {
          <ul class="activity">
            @for (entry of page.items; track entry.id) {
              <li>
                <span class="mu-body-medium">{{ label(entry.action) }}</span>
                <span class="mu-body-small mu-muted">
                  {{ entry.mosqueName ?? 'Platform' }} ·
                  {{ entry.actor?.displayName ?? 'verwijderd account' }} ·
                  {{ when(entry.occurredAt) }}
                </span>
              </li>
            }
          </ul>
        }
      } @else {
        <div class="activity" aria-busy="true" aria-label="Activiteiten laden">
          <mu-skeleton height="var(--mu-space-10)" />
          <mu-skeleton height="var(--mu-space-10)" />
        </div>
      }
    </section>
  `,
  styles: `
    :host {
      display: grid;
      gap: var(--mu-space-8);
    }
    .cards {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(calc(var(--mu-space-16) * 3.5), 1fr));
      gap: var(--mu-space-3);
      margin: 0;
      padding: 0;
      list-style: none;
    }
    .card {
      display: grid;
      gap: var(--mu-space-1);
      padding: var(--mu-card-padding);
      border: 1px solid var(--mu-color-border);
      border-radius: var(--mu-card-radius);
      background: var(--mu-color-surface);
    }
    .count {
      color: var(--mu-color-primary);
    }
    .panel {
      display: grid;
      gap: var(--mu-space-3);
    }
    .panel__head {
      display: flex;
      flex-wrap: wrap;
      align-items: baseline;
      justify-content: space-between;
      gap: var(--mu-space-2);
    }
    .activity {
      display: grid;
      margin: 0;
      padding: 0;
      list-style: none;
      li {
        display: grid;
        gap: var(--mu-space-1);
        padding: var(--mu-space-3) 0;
        border-bottom: 1px solid var(--mu-color-divider);
      }
    }
  `,
})
export class PlatformDashboardPage implements OnInit {
  private readonly api = inject(AdminApi);

  protected readonly totals = new Loadable(() => this.api.platformTotals());
  protected readonly recent = new Loadable(() => this.api.auditLog({ size: 8 }));

  protected readonly cards = computed<Card[] | null>(() => {
    const t = this.totals.value();
    if (!t) return null;
    return [
      {
        label: 'Moskeeën',
        value: t.mosques.total,
        hint: `${t.mosques.inactive} inactief, ${t.mosques.blocked} geblokkeerd`,
      },
      { label: 'Actieve moskeeën', value: t.mosques.active, hint: 'Zichtbaar in de app' },
      {
        label: 'Geregistreerde gebruikers',
        value: t.users,
        hint: `${t.newUsers} nieuw in 30 dagen`,
      },
      {
        label: 'Moskeevolgers',
        value: t.followers,
        hint: `Mensen die een moskee volgen (${t.follows} keer gevolgd)`,
      },
      {
        label: 'Evenementen',
        value: t.events,
        hint: `${t.upcomingEvents} gepland en gepubliceerd`,
      },
      { label: 'Gepubliceerde video’s', value: t.publishedVideos, hint: 'Lezingen en video’s' },
      { label: 'Aankondigingen', value: t.publishedAnnouncements, hint: 'Gepubliceerd' },
      { label: 'Pushmeldingen', value: t.recentPushNotifications, hint: 'In de laatste 30 dagen' },
    ];
  });

  ngOnInit(): void {
    void this.totals.load();
    void this.recent.load();
  }

  protected label(action: string): string {
    return auditActionLabel(action);
  }

  protected when(iso: string): string {
    return formatDateTime(iso);
  }
}
