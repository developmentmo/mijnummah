import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { MosqueContext } from '../../core/mosque/mosque-context';
import { Confirm } from '../../shared/ui/confirm-dialog';
import {
  MOSQUE,
  V1,
  anEvent,
  apiError,
  buttonByText,
  configureTesting,
  http,
  respond,
  selectMosque,
  spyOnRouter,
  text,
  typeInto,
} from '../../testing/harness';
import { EventEditPage } from './event-edit-page';

const BASE = `${V1}/admin/mosques/${MOSQUE.id}/events`;

describe('EventEditPage', () => {
  const ask = vi.fn();
  let router: ReturnType<typeof spyOnRouter>;

  beforeEach(async () => {
    ask.mockReset();
    sessionStorage.clear();
    configureTesting([{ provide: Confirm, useValue: { ask } }]);
    router = spyOnRouter();
    await selectMosque();
  });

  afterEach(() => http().verify());

  async function render(eventId?: string) {
    const fixture = TestBed.createComponent(EventEditPage);
    if (eventId) fixture.componentRef.setInput('eventId', eventId);
    await fixture.whenStable();
    return fixture;
  }

  async function open(event = anEvent()) {
    const fixture = await render(event.id);
    await respond(`${BASE}/${event.id}`, event);
    await fixture.whenStable();
    return fixture;
  }

  it('maakt een concept aan met tijden in UTC en opent het daarna', async () => {
    const fixture = await render();
    const page: EventEditPage = fixture.componentInstance;
    const form = (page as unknown as { form: EventEditPage['form'] }).form;
    form.patchValue({
      title: 'Nieuwe lezing',
      category: 'LECTURE',
      startDate: '2026-07-01',
      startTime: '20:00',
    });
    form.markAsDirty();
    expect(page.hasUnsavedChanges()).toBe(true);

    buttonByText(fixture.nativeElement, 'Opslaan als concept').click();
    const created = await respond(`${BASE}`, anEvent({ id: 'e-9' }));
    expect(created.request.method).toBe('POST');
    expect(created.request.body).toMatchObject({
      title: 'Nieuwe lezing',
      startsAt: '2026-07-01T18:00:00Z',
    });
    expect(page.hasUnsavedChanges()).toBe(false);
    expect(router.navigate).toHaveBeenCalledWith(['/m', MOSQUE.id, 'agenda', 'e-9'], {
      replaceUrl: true,
    });
  });

  it('verstuurt niets zolang het formulier ongeldig is', async () => {
    const fixture = await render();
    buttonByText(fixture.nativeElement, 'Opslaan als concept').click();
    await fixture.whenStable();
    expect(text(fixture.nativeElement)).toContain('Controleer de gemarkeerde velden.');
    http().expectNone(BASE);
  });

  it('zet veldfouten van de server bij het juiste veld', async () => {
    const fixture = await open();
    const titleInput = fixture.nativeElement.querySelector('input[formcontrolname=title]');
    typeInto(titleInput, 'Gewijzigd');
    buttonByText(fixture.nativeElement, 'Wijzigingen opslaan').click();
    const { body, options } = apiError('VALIDATION_FAILED', 400, [
      { field: 'imageUrl', message: 'must be an https URL' },
    ]);
    await respond(`${BASE}/e-1`, body, options);
    await fixture.whenStable();
    expect(text(fixture.nativeElement)).toContain('Gebruik een link die met https:// begint.');
  });

  it('publiceert pas na bevestiging en zegt dat er geen pushmelding gaat', async () => {
    ask.mockResolvedValue(true);
    const fixture = await open();
    buttonByText(fixture.nativeElement, 'Publiceren').click();
    await fixture.whenStable();
    expect(ask).toHaveBeenCalledWith(
      expect.objectContaining({ note: 'Er wordt geen pushmelding verstuurd.' }),
    );
    const publish = await respond(
      `${BASE}/e-1/publish`,
      anEvent({ status: 'PUBLISHED', publishedAt: '2026-10-11T10:00:00Z' }),
    );
    expect(publish.request.method).toBe('POST');
    await fixture.whenStable();
    expect(text(fixture.nativeElement.querySelector('aside'))).toContain('Gepubliceerd');
  });

  it('annuleert niets als de beheerder het venster wegklikt', async () => {
    ask.mockResolvedValue(false);
    const fixture = await open(anEvent({ status: 'PUBLISHED' }));
    buttonByText(fixture.nativeElement, 'Activiteit annuleren').click();
    await fixture.whenStable();
    expect(ask).toHaveBeenCalledWith(expect.objectContaining({ destructive: true }));
    http().expectNone(`${BASE}/e-1/cancel`);
  });

  it('legt uit waarom terugzetten niet kan als er aanmeldingen zijn', async () => {
    ask.mockResolvedValue(true);
    const fixture = await open(anEvent({ status: 'PUBLISHED' }));
    buttonByText(fixture.nativeElement, 'Terug naar concept').click();
    const { body, options } = apiError('EVENT_HAS_REGISTRATIONS', 409);
    await respond(`${BASE}/e-1/unpublish`, body, options);
    await respond(`${BASE}/e-1`, anEvent({ status: 'PUBLISHED' }));
    await fixture.whenStable();
    expect(text(fixture.nativeElement)).toContain('Er zijn al aanmeldingen voor deze activiteit.');
  });

  it('kan een geannuleerde activiteit niet meer wijzigen', async () => {
    const fixture = await open(
      anEvent({ status: 'CANCELLED', cancelledAt: '2026-10-10T10:00:00Z' }),
    );
    expect(text(fixture.nativeElement)).toContain('kan niet meer worden gewijzigd');
    expect(fixture.nativeElement.querySelector('input[formcontrolname=title]').disabled).toBe(true);
    expect(() => buttonByText(fixture.nativeElement, 'Wijzigingen opslaan')).toThrow();
  });

  it('blokkeert publiceren zolang er niet-opgeslagen wijzigingen zijn', async () => {
    const fixture = await open();
    typeInto(fixture.nativeElement.querySelector('input[formcontrolname=title]'), 'Anders');
    await fixture.whenStable();
    expect(buttonByText(fixture.nativeElement, 'Publiceren').disabled).toBe(true);
    expect(text(fixture.nativeElement)).toContain('Sla eerst je wijzigingen op.');
  });

  it('toont de link naar aanmeldingen alleen voor de rol Beheerder', async () => {
    let fixture = await open();
    expect(text(fixture.nativeElement)).toContain('Aanmeldingen bekijken');
    fixture.destroy();

    await selectMosqueAgain({ ...MOSQUE, myRole: 'EDITOR' });
    fixture = await open();
    expect(text(fixture.nativeElement)).not.toContain('Aanmeldingen bekijken');
  });
});

async function selectMosqueAgain(mosque: typeof MOSQUE) {
  const context = TestBed.inject(MosqueContext);
  const loading = context.load(true);
  await respond(`${V1}/admin/mosques`, [mosque]);
  await loading;
  await context.select(mosque.id);
}
