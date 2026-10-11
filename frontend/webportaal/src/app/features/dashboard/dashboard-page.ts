import { ChangeDetectionStrategy, Component, OnInit, computed, inject } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { RouterLink } from '@angular/router';
import { Observable, map } from 'rxjs';

import { AdminApi } from '../../core/api/admin-api';
import { AdminEvent, Page } from '../../core/api/models';
import { AuthService } from '../../core/auth/auth.service';
import { formatDateTime } from '../../core/format/dates';
import { EVENT_CATEGORY_LABELS } from '../../core/format/labels';
import { MosqueContext } from '../../core/mosque/mosque-context';
import { Loadable } from '../../shared/data/loadable';
import { Skeleton } from '../../shared/ui/skeleton';
import { StatePlaceholder } from '../../shared/ui/state-placeholder';
import { StatusMessage } from '../../shared/ui/status-message';

interface Tile {
  label: string;
  hint: string;
  loadable: Loadable<number>;
  link: unknown[];
  query?: Record<string, string>;
}

/** Overzicht met alleen echte gegevens uit de backend: aantallen, eerstvolgende activiteiten, snelle acties. */
@Component({
  selector: 'mu-dashboard-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButton, RouterLink, Skeleton, StatePlaceholder, StatusMessage],
  templateUrl: './dashboard-page.html',
  styleUrl: './dashboard-page.scss',
})
export class DashboardPage implements OnInit {
  private readonly api = inject(AdminApi);
  private readonly auth = inject(AuthService);
  protected readonly context = inject(MosqueContext);
  private readonly mosqueId = this.context.current()!.id;

  protected readonly base = ['/m', this.mosqueId];
  protected readonly greeting = computed(() => {
    const name = this.auth.user()?.displayName;
    return name ? `Assalamoe alaikoem, ${name}` : 'Assalamoe alaikoem';
  });

  protected readonly upcoming = new Loadable<Page<AdminEvent>>(() =>
    this.api.events(this.mosqueId, {
      status: 'PUBLISHED',
      from: new Date().toISOString(),
      size: 5,
    }),
  );

  private count(fetch: () => Observable<Page<unknown>>): Loadable<number> {
    return new Loadable(() => fetch().pipe(map((page) => page.totalItems)));
  }

  protected readonly tiles: Tile[] = [
    {
      label: 'Concepten in de agenda',
      hint: 'Nog niet zichtbaar in de app',
      loadable: this.count(() => this.api.events(this.mosqueId, { status: 'DRAFT', size: 1 })),
      link: [...this.base, 'agenda'],
      query: { status: 'DRAFT' },
    },
    {
      label: 'Gepubliceerde video’s',
      hint: 'In de mediabibliotheek van de app',
      loadable: this.count(() =>
        this.api.mediaList(this.mosqueId, { status: 'PUBLISHED', size: 1 }),
      ),
      link: [...this.base, 'media'],
      query: { status: 'PUBLISHED' },
    },
    {
      label: 'Concept-video’s',
      hint: 'Klaar om te publiceren',
      loadable: this.count(() => this.api.mediaList(this.mosqueId, { status: 'DRAFT', size: 1 })),
      link: [...this.base, 'media'],
      query: { status: 'DRAFT' },
    },
    {
      label: 'Gepubliceerde aankondigingen',
      hint: 'Inclusief verlopen aankondigingen',
      loadable: this.count(() =>
        this.api.announcements(this.mosqueId, { status: 'PUBLISHED', size: 1 }),
      ),
      link: [...this.base, 'aankondigingen'],
      query: { status: 'PUBLISHED' },
    },
  ];

  protected readonly categoryLabels = EVENT_CATEGORY_LABELS;

  ngOnInit(): void {
    void this.upcoming.load();
    for (const tile of this.tiles) void tile.loadable.load();
  }

  protected when(event: AdminEvent): string {
    return formatDateTime(event.startsAt, this.context.timeZone());
  }

  protected registrations(event: AdminEvent): string {
    if (!event.registrationEnabled) return 'Zonder aanmelding';
    return event.capacity
      ? `${event.confirmedRegistrations} van ${event.capacity} plaatsen bezet`
      : `${event.confirmedRegistrations} aangemeld`;
  }
}
