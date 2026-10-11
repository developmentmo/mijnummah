import { TestBed } from '@angular/core/testing';
import { describe, expect, it } from 'vitest';

import { Confirm } from './confirm-dialog';

function dialogText(): string {
  return (document.querySelector('mat-dialog-container')?.textContent ?? '').replace(/\s+/g, ' ');
}

function button(label: string): HTMLButtonElement {
  return Array.from(
    document.querySelectorAll<HTMLButtonElement>('mat-dialog-container button'),
  ).find((b) => b.textContent?.trim() === label)!;
}

describe('Confirm', () => {
  it('toont vraag en toelichting en geeft true bij bevestigen', async () => {
    const answer = TestBed.inject(Confirm).ask({
      title: 'Activiteit annuleren?',
      message: 'De activiteit blijft zichtbaar als geannuleerd.',
      note: 'Er wordt geen pushmelding verstuurd.',
      confirmLabel: 'Activiteit annuleren',
      destructive: true,
    });
    await new Promise((r) => setTimeout(r));
    expect(dialogText()).toContain('Activiteit annuleren?');
    expect(dialogText()).toContain('Er wordt geen pushmelding verstuurd.');
    expect(document.querySelector('mat-dialog-container')?.getAttribute('role')).toBe(
      'alertdialog',
    );
    button('Activiteit annuleren').click();
    expect(await answer).toBe(true);
  });

  it('geeft false bij annuleren', async () => {
    const answer = TestBed.inject(Confirm).ask({
      title: 'Publiceren?',
      message: 'Zichtbaar in de app.',
      confirmLabel: 'Publiceren',
    });
    await new Promise((r) => setTimeout(r));
    button('Annuleren').click();
    expect(await answer).toBe(false);
  });
});
