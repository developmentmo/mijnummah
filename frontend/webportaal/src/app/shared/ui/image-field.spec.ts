import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { FormControl } from '@angular/forms';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';

import {
  API,
  MOSQUE,
  V1,
  buttonByText,
  configureTesting,
  http,
  respond,
  settle,
  text,
} from '../../testing/harness';
import { ImageField } from './image-field';

@Component({
  imports: [ImageField],
  template: `<mu-image-field
    mosqueId="m-1"
    [idControl]="id"
    [urlControl]="url"
    label="Afbeelding"
  />`,
})
class Host {
  readonly id = new FormControl<string | null>(null);
  readonly url = new FormControl('https://example.org/oud.jpg', { nonNullable: true });
}

function choose(root: HTMLElement, file: File): void {
  const input = root.querySelector<HTMLInputElement>('input[type=file]')!;
  Object.defineProperty(input, 'files', { value: [file], configurable: true });
  input.dispatchEvent(new Event('change'));
}

describe('ImageField', () => {
  beforeEach(() => configureTesting());
  afterEach(() => http().verify());

  async function render() {
    const fixture = TestBed.createComponent(Host);
    await fixture.whenStable();
    return fixture;
  }

  it('toont een oudere externe link en de regels over beeldrechten', async () => {
    const fixture = await render();
    const root: HTMLElement = fixture.nativeElement;
    expect(root.querySelector('img')?.getAttribute('src')).toBe('https://example.org/oud.jpg');
    expect(text(root.querySelector('[data-testid=beeldrechten]'))).toContain('toestemming');
  });

  it('uploadt een afbeelding en bewaart alleen het ID', async () => {
    const fixture = await render();
    const root: HTMLElement = fixture.nativeElement;
    choose(root, new File(['x'], 'foto.png', { type: 'image/png' }));
    await respond(`${V1}/admin/mosques/${MOSQUE.id}/images`, {
      id: 'img-1',
      path: '/api/v1/public/images/img-1',
    });
    await fixture.whenStable();
    expect(fixture.componentInstance.id.value).toBe('img-1');
    expect(fixture.componentInstance.id.dirty).toBe(true);
    expect(root.querySelector('img')?.getAttribute('src')).toBe(
      `${API}/api/v1/public/images/img-1`,
    );
  });

  it('weigert andere bestandstypen zonder te uploaden', async () => {
    const fixture = await render();
    const root: HTMLElement = fixture.nativeElement;
    choose(root, new File(['x'], 'foto.gif', { type: 'image/gif' }));
    await settle();
    await fixture.whenStable();
    expect(text(root)).toContain('Kies een JPEG- of PNG-bestand.');
    http().expectNone(() => true);
  });

  it('verwijdert zowel de upload als de oude link', async () => {
    const fixture = await render();
    fixture.componentInstance.id.setValue('img-1');
    await fixture.whenStable();
    buttonByText(fixture.nativeElement, 'Verwijderen').click();
    await fixture.whenStable();
    expect(fixture.componentInstance.id.value).toBeNull();
    expect(fixture.componentInstance.url.value).toBe('');
    expect(text(fixture.nativeElement)).toContain('Geen afbeelding');
  });
});
