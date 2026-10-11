import { TestBed } from '@angular/core/testing';
import { describe, expect, it } from 'vitest';

import { ThemeService } from '../../core/theme/theme.service';
import { BrandLogo } from './brand-logo';

describe('BrandLogo', () => {
  function render(variant: 'horizontal' | 'compact' = 'horizontal') {
    const fixture = TestBed.createComponent(BrandLogo);
    fixture.componentRef.setInput('variant', variant);
    fixture.detectChanges();
    return fixture;
  }

  it('toont het lichte logo met een alternatieve tekst', () => {
    TestBed.inject(ThemeService).setMode('light');
    const img: HTMLImageElement = render().nativeElement.querySelector('img');
    expect(img.getAttribute('src')).toBe('branding/logo-horizontal-light.svg');
    expect(img.alt).toBe('Noeroel Islam');
  });

  it('wisselt naar het donkere logo in het donkere thema', () => {
    TestBed.inject(ThemeService).setMode('dark');
    const img: HTMLImageElement = render('compact').nativeElement.querySelector('img');
    expect(img.getAttribute('src')).toBe('branding/logo-compact-dark.svg');
  });
});
