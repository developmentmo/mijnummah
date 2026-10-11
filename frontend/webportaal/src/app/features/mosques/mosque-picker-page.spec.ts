import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it } from 'vitest';

import { MOSQUE, V1, configureTesting, respond, spyOnRouter, text } from '../../testing/harness';
import { MosquePickerPage } from './mosque-picker-page';

describe('MosquePickerPage', () => {
  let router: ReturnType<typeof spyOnRouter>;

  beforeEach(() => {
    sessionStorage.clear();
    configureTesting();
    router = spyOnRouter();
  });

  async function render(inputs: Record<string, string> = {}) {
    const fixture = TestBed.createComponent(MosquePickerPage);
    for (const [k, v] of Object.entries(inputs)) fixture.componentRef.setInput(k, v);
    await fixture.whenStable();
    return fixture;
  }

  it('gaat direct door bij precies één moskee', async () => {
    await render();
    await respond(`${V1}/admin/mosques`, [MOSQUE]);
    expect(router.navigate).toHaveBeenCalledWith(['/m', MOSQUE.id], { replaceUrl: true });
  });

  it('toont de lijst als de beheerder expliciet wil kiezen', async () => {
    const fixture = await render({ kies: '1' });
    await respond(`${V1}/admin/mosques`, [MOSQUE]);
    await fixture.whenStable();
    expect(router.navigate).not.toHaveBeenCalled();
    expect(text(fixture.nativeElement)).toContain('Noeroel Islam');
    expect(text(fixture.nativeElement)).toContain('Beheerder');
  });

  it('legt uit wat te doen zonder moskee', async () => {
    const fixture = await render();
    await respond(`${V1}/admin/mosques`, []);
    await fixture.whenStable();
    expect(text(fixture.nativeElement)).toContain('Je account beheert nog geen moskee');
  });

  it('meldt het als de gevraagde moskee niet toegankelijk was', async () => {
    const fixture = await render({ melding: 'geen-toegang' });
    await respond(`${V1}/admin/mosques`, [MOSQUE]);
    await fixture.whenStable();
    expect(text(fixture.nativeElement)).toContain('Je hebt geen toegang tot die moskee');
    expect(router.navigate).not.toHaveBeenCalled();
  });
});
