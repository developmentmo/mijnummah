import { ChangeDetectionStrategy, Component, OnInit, inject, input, signal } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { Router, RouterLink } from '@angular/router';

import { describeError } from '../../core/api/api-error';
import { AdminMosque } from '../../core/api/models';
import { AuthService } from '../../core/auth/auth.service';
import { MosqueContext } from '../../core/mosque/mosque-context';
import { MOSQUE_ROLE_LABELS } from '../../core/format/labels';
import { PageHeader } from '../../shared/ui/page-header';
import { Skeleton } from '../../shared/ui/skeleton';
import { StatePlaceholder } from '../../shared/ui/state-placeholder';
import { StatusMessage } from '../../shared/ui/status-message';

/**
 * Kies de moskee om te beheren. Wie er precies één beheert, gaat direct door (behalve als hij expliciet wil
 * kiezen of geen toegang had tot de gevraagde moskee).
 */
@Component({
  selector: 'mu-mosque-picker-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButton, PageHeader, RouterLink, Skeleton, StatePlaceholder, StatusMessage],
  template: `
    <mu-page-header heading="Moskee kiezen" subtitle="Kies de moskee die je wilt beheren." />

    @if (melding() === 'geen-toegang') {
      <mu-status-message
        kind="warning"
        message="Je hebt geen toegang tot die moskee. Kies een van de moskeeën hieronder."
      />
    }

    @if (error(); as message) {
      <mu-status-message
        kind="error"
        [message]="message"
        actionLabel="Opnieuw proberen"
        (action)="load(true)"
      />
    } @else if (mosques(); as list) {
      @if (list.length === 0) {
        <mu-state-placeholder
          heading="Nog geen moskee"
          message="Je account beheert nog geen moskee. Vraag de beheerder van je moskee om je toe te voegen."
          actionLabel="Uitloggen"
          (action)="logout()"
        />
      } @else {
        <ul class="list">
          @for (mosque of list; track mosque.id) {
            <li>
              <a class="mosque" [routerLink]="['/m', mosque.id]">
                <span class="mu-title-medium">{{ mosque.name }}</span>
                <span class="mu-body-medium mu-muted">
                  {{ mosque.address.city || mosque.slug }} · {{ roleOf(mosque) }}
                  @if (!mosque.active) {
                    · inactief
                  }
                </span>
              </a>
            </li>
          }
        </ul>
      }
    } @else {
      <div class="list" aria-busy="true" aria-label="Moskeeën laden">
        <mu-skeleton height="var(--mu-space-16)" />
        <mu-skeleton height="var(--mu-space-16)" />
      </div>
    }

    @if (auth.isPlatformAdmin()) {
      <p class="platform mu-body-medium">
        Als platformbeheerder zie je alle moskeeën.
        <a matButton routerLink="/accounts">Accounts beheren</a>
      </p>
    }
  `,
  styles: `
    .list {
      display: grid;
      gap: var(--mu-space-3);
      margin: var(--mu-space-4) 0 0;
      padding: 0;
      list-style: none;
    }
    .mosque {
      display: grid;
      gap: var(--mu-space-1);
      padding: var(--mu-space-4) var(--mu-space-5);
      border: 1px solid var(--mu-color-border);
      border-radius: var(--mu-card-radius);
      background: var(--mu-color-surface);
      color: var(--mu-color-on-surface);
      text-decoration: none;
    }
    .mosque:hover {
      border-color: var(--mu-color-primary);
    }
    .platform {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: var(--mu-space-2);
      margin-top: var(--mu-space-8);
    }
  `,
})
export class MosquePickerPage implements OnInit {
  protected readonly auth = inject(AuthService);
  private readonly context = inject(MosqueContext);
  private readonly router = inject(Router);

  /** `?kies=1`: altijd de lijst tonen, ook bij één moskee. */
  readonly kies = input<string>();
  /** `?melding=geen-toegang` na een link naar een moskee zonder toegang. */
  readonly melding = input<string>();

  protected readonly mosques = signal<AdminMosque[] | null>(null);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    void this.load();
  }

  protected async load(force = false): Promise<void> {
    this.error.set(null);
    try {
      const list = await this.context.load(force);
      if (list.length === 1 && !this.kies() && !this.melding()) {
        await this.router.navigate(['/m', list[0].id], { replaceUrl: true });
        return;
      }
      this.mosques.set(list);
    } catch (error) {
      this.error.set(describeError(error));
    }
  }

  protected roleOf(mosque: AdminMosque): string {
    return mosque.myRole ? MOSQUE_ROLE_LABELS[mosque.myRole] : 'Platformbeheerder';
  }

  protected async logout(): Promise<void> {
    await this.auth.logout();
    await this.router.navigate(['/inloggen']);
  }
}
