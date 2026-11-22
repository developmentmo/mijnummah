import { ChangeDetectionStrategy, Component, OnInit, computed, inject } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { RouterLink } from '@angular/router';

import { AdminApi } from '../../core/api/admin-api';
import { AdminEvent, AuditEntry, MosqueFigures, Page } from '../../core/api/models';
import { AuthService } from '../../core/auth/auth.service';
import { auditActionLabel } from '../../core/format/audit';
import { formatDateTime } from '../../core/format/dates';
import { EVENT_CATEGORY_LABELS } from '../../core/format/labels';
import { MosqueContext } from '../../core/mosque/mosque-context';
import { Loadable } from '../../shared/data/loadable';
import { Skeleton } from '../../shared/ui/skeleton';
import { StatePlaceholder } from '../../shared/ui/state-placeholder';
import { StatusMessage } from '../../shared/ui/status-message';

interface Tile {
  label: string;
  value: number;
  hint: string;
  link: unknown[];
  query?: Record<string, string>;
}

/**
 * Overzicht met alleen echte gegevens uit de backend: aantallen, eerstvolgende activiteiten, recente activiteit
 * (alleen beheerders, uit het auditlog) en snelle acties.
 */
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

  protected readonly figures = new Loadable<MosqueFigures>(() =>
    this.api.mosqueFigures(this.mosqueId),
  );

  /** Het auditlog van de moskee is alleen voor beheerders (ADMIN). */
  protected readonly showActivity = this.context.isAdmin;
  protected readonly activity = new Loadable<Page<AuditEntry>>(() =>
    this.api.auditLog({ mosqueId: this.mosqueId, size: 6 }),
  );
  protected readonly actionLabel = auditActionLabel;

  protected readonly tiles = computed<Tile[]>(() => {
    const f = this.figures.value();
    if (!f) return [];
    return [
      {
        label: 'Volgers',
        value: f.followers,
        hint: f.newFollowers
          ? `${f.newFollowers} nieuw in de laatste 30 dagen`
          : 'Volgen de moskee in de app',
        link: [...this.base, 'pushmeldingen'],
      },
      {
        label: 'Komende activiteiten',
        value: f.upcomingEvents,
        hint: f.draftEvents ? `En ${f.draftEvents} in concept` : 'Gepubliceerd in de agenda',
        link: [...this.base, 'agenda'],
        query: { status: 'PUBLISHED' },
      },
      {
        label: 'Video’s in de app',
        value: f.publishedVideos,
        hint:
          [
            f.scheduledVideos ? `${f.scheduledVideos} gepland` : '',
            f.draftVideos ? `${f.draftVideos} in concept` : '',
          ]
            .filter(Boolean)
            .join(', ') || 'In de mediabibliotheek',
        link: [...this.base, 'media'],
      },
      {
        label: 'Aankondigingen in de app',
        value: f.publishedAnnouncements,
        hint: f.draftAnnouncements ? `En ${f.draftAnnouncements} in concept` : 'Niet verlopen',
        link: [...this.base, 'aankondigingen'],
      },
      {
        label: 'Pushmeldingen',
        value: f.recentPushNotifications,
        hint: 'Verstuurd in de laatste 30 dagen',
        link: [...this.base, 'pushmeldingen'],
      },
    ];
  });

  protected readonly categoryLabels = EVENT_CATEGORY_LABELS;

  ngOnInit(): void {
    void this.upcoming.load();
    void this.figures.load();
    if (this.showActivity()) void this.activity.load();
  }

  protected at(iso: string): string {
    return formatDateTime(iso, this.context.timeZone());
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
