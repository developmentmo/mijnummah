import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';

import { MosqueFigures } from '../../core/api/models';
import {
  MOSQUE,
  V1,
  anEvent,
  apiError,
  configureTesting,
  http,
  page,
  selectMosque,
  settle,
  text,
} from '../../testing/harness';
import { DashboardPage } from './dashboard-page';

const BASE = `${V1}/admin/mosques/${MOSQUE.id}`;

const FIGURES: MosqueFigures = {
  followers: 230,
  newFollowers: 12,
  upcomingEvents: 7,
  draftEvents: 2,
  publishedVideos: 14,
  scheduledVideos: 1,
  draftVideos: 3,
  publishedAnnouncements: 4,
  draftAnnouncements: 0,
  recentPushNotifications: 5,
};

describe('DashboardPage', () => {
  beforeEach(() => {
    sessionStorage.clear();
    configureTesting();
  });

  afterEach(() => http().verify());

  async function render() {
    const fixture = TestBed.createComponent(DashboardPage);
    await fixture.whenStable();
    await settle();
    return fixture;
  }

  function tiles(root: HTMLElement): string[] {
    return Array.from(root.querySelectorAll('.tile') as NodeListOf<HTMLElement>).map(text);
  }

  it('toont alleen echte gegevens uit de API, met recente activiteiten voor een beheerder', async () => {
    await selectMosque();
    const fixture = await render();

    const upcoming = http().expectOne(
      (r) => r.url === `${BASE}/events` && r.params.get('status') === 'PUBLISHED',
    );
    expect(upcoming.request.params.get('from')).toMatch(/^\d{4}-\d{2}-\d{2}T/);
    expect(upcoming.request.params.get('size')).toBe('5');
    upcoming.flush(page([anEvent({ status: 'PUBLISHED' })], 7));
    http().expectOne(`${BASE}/dashboard`).flush(FIGURES);
    const audit = http().expectOne((r) => r.url === `${V1}/admin/audit-log`);
    expect(audit.request.params.get('mosqueId')).toBe(MOSQUE.id);
    audit.flush(
      page([
        {
          id: 'l-1',
          occurredAt: '2026-10-11T10:00:00Z',
          actor: { id: 'u-1', displayName: 'Fatima' },
          mosqueId: MOSQUE.id,
          mosqueName: MOSQUE.name,
          action: 'EVENT_ARCHIVED',
          targetType: 'EVENT',
          targetId: 'e-1',
        },
      ]),
    );
    await settle();
    await fixture.whenStable();

    const content = text(fixture.nativeElement);
    expect(content).toContain('7 gepubliceerde activiteiten gepland');
    expect(content).toContain('12 van 40 plaatsen bezet');
    const t = tiles(fixture.nativeElement);
    expect(t[0]).toMatch(/^230\s*Volgers\s*12 nieuw in de laatste 30 dagen/);
    expect(t[1]).toMatch(/^7\s*Komende activiteiten\s*En 2 in concept/);
    expect(t[2]).toContain('1 gepland, 3 in concept');
    expect(t[4]).toMatch(/^5\s*Pushmeldingen/);
    expect(content).toContain('Recente activiteiten');
    expect(content).toContain('Fatima');
  });

  it('laat een redacteur geen auditlog zien en geeft een herstelactie bij een fout', async () => {
    await selectMosque({ ...MOSQUE, myRole: 'EDITOR' });
    const fixture = await render();

    http()
      .expectOne((r) => r.url === `${BASE}/events`)
      .flush(page([]));
    const { body, options } = apiError('INTERNAL_ERROR', 500);
    http().expectOne(`${BASE}/dashboard`).flush(body, options);
    http().expectNone((r) => r.url === `${V1}/admin/audit-log`);
    await settle();
    await fixture.whenStable();

    const content = text(fixture.nativeElement);
    expect(content).not.toContain('Recente activiteiten');
    expect(content).toContain('Opnieuw proberen');
    expect(tiles(fixture.nativeElement)).toEqual([]);
  });
});
