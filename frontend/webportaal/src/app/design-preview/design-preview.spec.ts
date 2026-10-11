import { TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { describe, expect, it, vi } from 'vitest';

import { DesignPreview } from './design-preview';

describe('DesignPreview', () => {
  function render() {
    const fixture = TestBed.createComponent(DesignPreview);
    fixture.detectChanges();
    return fixture;
  }

  it('toont alle secties van het design system', () => {
    const headings = Array.from(
      render().nativeElement.querySelectorAll('h2') as NodeListOf<HTMLElement>,
    ).map((h) => h.textContent?.trim());
    expect(headings).toEqual([
      "Logo's",
      'Kleuren',
      'Typografie',
      'Knoppen',
      'Invoervelden',
      'Cards',
      'Meldingen',
      'Laden, leeg en fout',
    ]);
  });

  it('toont een validatiefout voor een ongeldig e-mailadres', () => {
    const el: HTMLElement = render().nativeElement;
    expect(el.querySelector('mat-error')?.textContent).toContain('geldig e-mailadres');
  });

  it('voorbeeldknoppen melden eerlijk dat het om een voorbeeld gaat', () => {
    const fixture = render();
    const open = vi.spyOn(TestBed.inject(MatSnackBar), 'open');
    const button = Array.from(
      fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>,
    ).find((b) => b.textContent?.trim() === 'Publiceren');
    button?.click();
    expect(open).toHaveBeenCalledWith('Dit is een voorbeeldknop in de designpreview.', undefined, {
      duration: 4000,
    });
  });

  it('gebruikt alleen kleuren uit de design tokens', () => {
    const swatches = render().nativeElement.querySelectorAll('.swatch') as NodeListOf<HTMLElement>;
    expect(swatches.length).toBeGreaterThan(0);
    for (const swatch of Array.from(swatches)) {
      expect(swatch.style.background).toMatch(/^var\(--mu-color-/);
    }
  });
});
