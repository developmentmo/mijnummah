import { InjectionToken } from '@angular/core';

/**
 * Instellingen die per installatie verschillen. Ze staan in `config.json` naast de build, zodat één build
 * voor elke omgeving (en later elke moskee) bruikbaar is zonder opnieuw te bouwen.
 */
export interface AppConfig {
  /** Basisadres van de backend zonder afsluitende slash, bijv. https://api.mijnummah.nl. Leeg = zelfde origin. */
  apiBaseUrl: string;
}

export const APP_CONFIG = new InjectionToken<AppConfig>('APP_CONFIG');

export async function loadAppConfig(fetchFn: typeof fetch = fetch): Promise<AppConfig> {
  const response = await fetchFn('config.json', { cache: 'no-store' });
  if (!response.ok) {
    throw new Error(`config.json kon niet worden geladen (HTTP ${response.status})`);
  }
  const raw = (await response.json()) as Partial<AppConfig>;
  if (typeof raw.apiBaseUrl !== 'string') {
    throw new Error('config.json mist apiBaseUrl');
  }
  return { apiBaseUrl: raw.apiBaseUrl.replace(/\/+$/, '') };
}
