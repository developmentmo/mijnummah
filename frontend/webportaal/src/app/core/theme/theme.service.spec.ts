import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it } from 'vitest';

import { ThemeService } from './theme.service';

describe('ThemeService', () => {
  beforeEach(() => {
    localStorage.clear();
    document.documentElement.removeAttribute('data-theme');
  });

  function create(): ThemeService {
    const service = TestBed.inject(ThemeService);
    TestBed.tick();
    return service;
  }

  it('volgt standaard het systeem zonder data-theme', () => {
    const service = create();
    expect(service.mode()).toBe('system');
    expect(document.documentElement.hasAttribute('data-theme')).toBe(false);
  });

  it('zet data-theme bij een expliciete keuze en onthoudt die', () => {
    const service = create();
    service.setMode('dark');
    TestBed.tick();
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
    expect(service.isDark()).toBe(true);
    expect(localStorage.getItem('mu-theme-mode')).toBe('dark');

    service.setMode('light');
    TestBed.tick();
    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
    expect(service.isDark()).toBe(false);
  });

  it('herstelt een eerder gekozen thema', () => {
    localStorage.setItem('mu-theme-mode', 'dark');
    expect(create().mode()).toBe('dark');
  });

  it('negeert een ongeldige opgeslagen waarde', () => {
    localStorage.setItem('mu-theme-mode', 'paars');
    expect(create().mode()).toBe('system');
  });
});
