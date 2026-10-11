import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { Confirm } from '../../shared/ui/confirm-dialog';
import {
  MOSQUE,
  V1,
  aMedia,
  apiError,
  buttonByText,
  configureTesting,
  http,
  page,
  respond,
  respondPushPanel,
  selectMosque,
  spyOnRouter,
  text,
} from '../../testing/harness';
import { MediaEditPage } from './media-edit-page';

const BASE = `${V1}/admin/mosques/${MOSQUE.id}`;

describe('MediaEditPage', () => {
  beforeEach(async () => {
    sessionStorage.clear();
    configureTesting([{ provide: Confirm, useValue: { ask: vi.fn().mockResolvedValue(true) } }]);
    spyOnRouter();
    await selectMosque();
  });

  afterEach(() => http().verify());

  it('zet een afgewezen YouTube-link bij het linkveld', async () => {
    const fixture = TestBed.createComponent(MediaEditPage);
    await fixture.whenStable();
    await respond((r) => r.url === `${BASE}/events`, page([]));
    const form = (fixture.componentInstance as unknown as { form: MediaEditPage['form'] }).form;
    form.patchValue({
      title: 'Video',
      category: 'LECTURE',
      videoUrl: 'https://youtu.be/dQw4w9WgXcQ',
    });
    buttonByText(fixture.nativeElement, 'Opslaan als concept').click();
    const { body, options } = apiError('VIDEO_ALREADY_ADDED', 409);
    await respond(`${BASE}/media`, body, options);
    await fixture.whenStable();
    expect(form.controls.videoUrl.errors).toEqual({
      server: 'Deze video staat al in de mediabibliotheek.',
    });
    expect(text(fixture.nativeElement)).toContain('Deze video staat al in de mediabibliotheek.');
  });

  it('publiceert een concept en vermeldt dat de video op YouTube blijft bij verwijderen', async () => {
    const ask = TestBed.inject(Confirm).ask as ReturnType<typeof vi.fn>;
    const fixture = TestBed.createComponent(MediaEditPage);
    fixture.componentRef.setInput('mediaId', 'v-1');
    await fixture.whenStable();
    await settleRequests();
    await fixture.whenStable();
    await respondPushPanel();
    await fixture.whenStable();
    buttonByText(fixture.nativeElement, 'Publiceren').click();
    await respond(`${BASE}/media/v-1/publish`, aMedia({ status: 'PUBLISHED' }));
    await fixture.whenStable();
    expect(text(fixture.nativeElement.querySelector('aside'))).toContain('Gepubliceerd');

    ask.mockResolvedValue(false);
    buttonByText(fixture.nativeElement, 'Verwijderen').click();
    await fixture.whenStable();
    expect(ask).toHaveBeenLastCalledWith(
      expect.objectContaining({ message: expect.stringContaining('blijft op YouTube staan') }),
    );
  });
});

async function settleRequests() {
  await respond((r) => r.url === `${BASE}/events`, page([]));
  await respond(`${BASE}/media/v-1`, aMedia());
}
