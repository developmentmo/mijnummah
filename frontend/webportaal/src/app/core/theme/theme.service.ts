import { DOCUMENT } from '@angular/common';
import { DestroyRef, Injectable, computed, effect, inject, signal } from '@angular/core';

export type ThemeMode = 'system' | 'light' | 'dark';

const STORAGE_KEY = 'mu-theme-mode';

/**
 * Beheert licht/donker. Zet `data-theme` op het html-element; de CSS-variabelen uit
 * tokens.generated.css wisselen daarop mee. In de stand 'system' volgt het portaal het besturingssysteem.
 */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly document = inject(DOCUMENT);
  private readonly media = this.document.defaultView?.matchMedia?.('(prefers-color-scheme: dark)');
  private readonly systemPrefersDark = signal(this.media?.matches ?? false);

  readonly mode = signal<ThemeMode>(this.readStoredMode());
  readonly isDark = computed(
    () => this.mode() === 'dark' || (this.mode() === 'system' && this.systemPrefersDark()),
  );

  constructor() {
    const listener = (event: MediaQueryListEvent) => this.systemPrefersDark.set(event.matches);
    this.media?.addEventListener?.('change', listener);
    inject(DestroyRef).onDestroy(() => this.media?.removeEventListener?.('change', listener));

    effect(() => {
      const mode = this.mode();
      const root = this.document.documentElement;
      if (mode === 'system') {
        root.removeAttribute('data-theme');
      } else {
        root.setAttribute('data-theme', mode);
      }
      this.store(mode);
    });
  }

  setMode(mode: ThemeMode): void {
    this.mode.set(mode);
  }

  private readStoredMode(): ThemeMode {
    try {
      const stored = this.document.defaultView?.localStorage.getItem(STORAGE_KEY);
      return stored === 'light' || stored === 'dark' ? stored : 'system';
    } catch {
      return 'system';
    }
  }

  private store(mode: ThemeMode): void {
    try {
      this.document.defaultView?.localStorage.setItem(STORAGE_KEY, mode);
    } catch {
      // Opslag kan geblokkeerd zijn (privévenster); het thema werkt dan alleen voor deze sessie.
    }
  }
}
