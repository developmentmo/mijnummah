import { HttpBackend, HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { ApiError } from '../api/api-error';
import { CurrentUser, TokenResponse } from '../api/models';
import { APP_CONFIG } from '../config/app-config';
import { RefreshTokenStore } from './refresh-token-store';

/** Ververs het access token als het binnen deze tijd verloopt. */
const REFRESH_MARGIN_MS = 30_000;

export type SessionEndReason = 'logout' | 'expired';

/**
 * Inloggen, uitloggen en tokens. Gebruikt een eigen HttpClient zonder interceptors, zodat de auth-aanroepen
 * nooit zelf een refresh starten. Refresh tokens roteren en hergebruik trekt de sessie in; daarom loopt er
 * altijd hooguit één refresh tegelijk.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = new HttpClient(inject(HttpBackend));
  private readonly baseUrl = inject(APP_CONFIG).apiBaseUrl;
  private readonly store = inject(RefreshTokenStore);

  private accessToken: string | null = null;
  private accessTokenExpiresAt = 0;
  private refreshing?: Promise<string | null>;
  private restoring?: Promise<void>;

  private readonly currentUser = signal<CurrentUser | null>(null);
  readonly user = this.currentUser.asReadonly();
  readonly isSignedIn = computed(() => this.currentUser() !== null);
  readonly isPlatformAdmin = computed(() => this.currentUser()?.platformRole === 'PLATFORM_ADMIN');

  /** Waarom de vorige sessie eindigde; de inlogpagina legt een verlopen sessie uit. */
  readonly lastEndReason = signal<SessionEndReason | null>(null);

  /** Herstelt een sessie na herladen, als er een refresh token is. Wordt hooguit één keer uitgevoerd. */
  restore(): Promise<void> {
    this.restoring ??= this.doRestore();
    return this.restoring;
  }

  async login(email: string, password: string): Promise<CurrentUser> {
    const tokens = await firstValueFrom(
      this.http.post<TokenResponse>(this.url('/auth/login'), { email, password }),
    ).catch((error) => {
      throw ApiError.from(error);
    });
    this.accept(tokens);
    try {
      const user = await this.fetchMe();
      this.currentUser.set(user);
      this.lastEndReason.set(null);
      this.restoring = Promise.resolve();
      return user;
    } catch (error) {
      this.clear();
      throw ApiError.from(error);
    }
  }

  /** Beëindigt de sessie, ook als de server niet bereikbaar is. */
  async logout(): Promise<void> {
    const refreshToken = this.store.read();
    this.endSession('logout');
    if (refreshToken) {
      await firstValueFrom(this.http.post(this.url('/auth/logout'), { refreshToken })).catch(
        () => undefined,
      );
    }
  }

  /** Een geldig access token, zo nodig eerst ververst; null als er geen sessie is. */
  async validAccessToken(): Promise<string | null> {
    if (this.accessToken && this.accessTokenExpiresAt - REFRESH_MARGIN_MS > Date.now()) {
      return this.accessToken;
    }
    if (!this.store.read()) return null;
    return this.refresh();
  }

  /**
   * De server wees `rejectedToken` af (401). Als iemand anders intussen al ververst heeft, is het nieuwe token
   * bruikbaar; anders volgt één refresh. Lukt dat niet, dan is de sessie verlopen.
   */
  async recoverFromRejection(rejectedToken: string): Promise<string | null> {
    if (this.accessToken && this.accessToken !== rejectedToken) return this.accessToken;
    this.accessToken = null;
    const token = await this.refresh().catch(() => null);
    if (!token) this.endSession('expired');
    return token;
  }

  endSession(reason: SessionEndReason): void {
    this.clear();
    this.lastEndReason.set(reason);
  }

  private async doRestore(): Promise<void> {
    if (!this.store.read()) return;
    try {
      await this.refresh();
      this.currentUser.set(await this.fetchMe());
    } catch (error) {
      const apiError = ApiError.from(error);
      if (apiError.status === 401 || apiError.status === 403) {
        this.endSession('expired');
      } else {
        // Server onbereikbaar: token bewaren, zodat een volgende poging na herladen kan slagen.
        this.accessToken = null;
        this.currentUser.set(null);
        this.restoring = undefined;
        throw apiError;
      }
    }
  }

  private refresh(): Promise<string | null> {
    this.refreshing ??= this.doRefresh().finally(() => (this.refreshing = undefined));
    return this.refreshing;
  }

  private async doRefresh(): Promise<string | null> {
    const refreshToken = this.store.read();
    if (!refreshToken) return null;
    try {
      const tokens = await firstValueFrom(
        this.http.post<TokenResponse>(this.url('/auth/refresh'), { refreshToken }),
      );
      this.accept(tokens);
      return tokens.accessToken;
    } catch (error) {
      const apiError = ApiError.from(error);
      if (apiError.status === 401 || apiError.status === 403) {
        this.store.clear();
        this.accessToken = null;
      }
      throw apiError;
    }
  }

  private fetchMe(): Promise<CurrentUser> {
    return firstValueFrom(
      this.http.get<CurrentUser>(this.url('/auth/me'), {
        headers: new HttpHeaders({ Authorization: `Bearer ${this.accessToken}` }),
      }),
    );
  }

  private accept(tokens: TokenResponse): void {
    this.accessToken = tokens.accessToken;
    this.accessTokenExpiresAt = Date.parse(tokens.accessTokenExpiresAt);
    this.store.write(tokens.refreshToken);
  }

  private clear(): void {
    this.accessToken = null;
    this.accessTokenExpiresAt = 0;
    this.store.clear();
    this.currentUser.set(null);
    this.restoring = Promise.resolve();
  }

  private url(path: string): string {
    return `${this.baseUrl}/api/v1${path}`;
  }
}
