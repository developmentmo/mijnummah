import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { AdminApi } from '../../core/api/admin-api';
import { describeError } from '../../core/api/api-error';
import { AdminMosque, MosqueStatus } from '../../core/api/models';
import { MosqueContext } from '../../core/mosque/mosque-context';
import { Confirm } from '../../shared/ui/confirm-dialog';
import { Notifier } from '../../shared/ui/notifier';
import { PageHeader } from '../../shared/ui/page-header';
import { Skeleton } from '../../shared/ui/skeleton';
import { StatusBadge } from '../../shared/ui/status-badge';
import { StatusMessage } from '../../shared/ui/status-message';

const STATUS_LABELS: Record<MosqueStatus, string> = {
  ACTIVE: 'Actief',
  INACTIVE: 'Inactief',
  BLOCKED: 'Geblokkeerd',
};

interface StatusAction {
  to: MosqueStatus;
  label: string;
  destructive: boolean;
}

/** Wat je per status kunt doen. */
const ACTIONS: Record<MosqueStatus, StatusAction[]> = {
  ACTIVE: [
    { to: 'INACTIVE', label: 'Deactiveren', destructive: true },
    { to: 'BLOCKED', label: 'Blokkeren', destructive: true },
  ],
  INACTIVE: [
    { to: 'ACTIVE', label: 'Activeren', destructive: false },
    { to: 'BLOCKED', label: 'Blokkeren', destructive: true },
  ],
  BLOCKED: [{ to: 'ACTIVE', label: 'Blokkade opheffen', destructive: false }],
};

/**
 * Platformbeheer: alle moskeeën op het platform, ook inactieve en geblokkeerde. Toevoegen, openen om te beheren, en
 * de status wijzigen. Inactief en geblokkeerd verbergen de moskee en al haar content in de app; geblokkeerd sluit
 * ook haar eigen beheerders buiten. Er wordt niets gewist.
 */
@Component({
  selector: 'mu-mosques-admin-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButton, PageHeader, RouterLink, Skeleton, StatusBadge, StatusMessage],
  template: `
    <mu-page-header
      heading="Alle moskeeën"
      subtitle="Platformbeheer: als platformbeheerder beheer je elke moskee met alle rechten van een beheerder."
    >
      <a matButton="filled" routerLink="/moskeeen/nieuw">Moskee toevoegen</a>
    </mu-page-header>

    @if (rowError(); as message) {
      <mu-status-message kind="error" [message]="message" />
    }
    @if (error(); as message) {
      <mu-status-message
        kind="error"
        [message]="message"
        actionLabel="Opnieuw proberen"
        (action)="load()"
      />
    } @else if (mosques(); as list) {
      <div class="mu-table-wrap">
        <table class="mu-table">
          <caption class="mu-visually-hidden">
            Alle moskeeën op naam
          </caption>
          <thead>
            <tr>
              <th scope="col">Naam</th>
              <th scope="col">Slug</th>
              <th scope="col">Plaats</th>
              <th scope="col">Status</th>
              <th scope="col"><span class="mu-visually-hidden">Acties</span></th>
            </tr>
          </thead>
          <tbody>
            @for (m of list; track m.id) {
              <tr>
                <td>{{ m.name }}</td>
                <td>
                  <code>{{ m.slug }}</code>
                </td>
                <td>{{ m.address.city || '–' }}</td>
                <td>
                  <mu-status-badge [status]="m.status" [label]="statusLabels[m.status]" />
                </td>
                <td class="actions">
                  <a matButton [routerLink]="['/m', m.id]">Beheren</a>
                  @for (action of actions[m.status]; track action.to) {
                    <button
                      matButton
                      type="button"
                      [class.mu-danger-button]="action.destructive"
                      [disabled]="busy()"
                      (click)="changeStatus(m, action.to)"
                    >
                      {{ action.label }}
                    </button>
                  }
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    } @else {
      <div class="mu-loading-rows" aria-busy="true" aria-label="Moskeeën laden">
        <mu-skeleton height="var(--mu-space-12)" />
        <mu-skeleton height="var(--mu-space-12)" />
      </div>
    }
  `,
  styles: `
    :host {
      display: grid;
      gap: var(--mu-space-4);
    }
    .actions {
      white-space: nowrap;
      text-align: end;
    }
  `,
})
export class MosquesAdminPage implements OnInit {
  private readonly api = inject(AdminApi);
  private readonly context = inject(MosqueContext);
  private readonly confirm = inject(Confirm);
  private readonly notifier = inject(Notifier);

  protected readonly mosques = signal<AdminMosque[] | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly rowError = signal<string | null>(null);
  protected readonly busy = signal(false);
  protected readonly statusLabels = STATUS_LABELS;
  protected readonly actions = ACTIONS;

  ngOnInit(): void {
    void this.load();
  }

  protected async load(): Promise<void> {
    this.error.set(null);
    try {
      this.mosques.set(await this.context.load(true));
    } catch (error) {
      this.error.set(describeError(error));
    }
  }

  protected async changeStatus(mosque: AdminMosque, to: MosqueStatus): Promise<void> {
    if (this.busy()) return;
    const ok = await this.confirm.ask(this.question(mosque, to));
    if (!ok) return;
    this.busy.set(true);
    this.rowError.set(null);
    try {
      const updated = await firstValueFrom(this.api.setMosqueStatus(mosque.id, to));
      this.mosques.set(await this.context.load(true));
      this.notifier.success(`${updated.name} is ${DONE[updated.status]}.`);
    } catch (error) {
      this.rowError.set(describeError(error));
    } finally {
      this.busy.set(false);
    }
  }

  private question(mosque: AdminMosque, to: MosqueStatus) {
    switch (to) {
      case 'INACTIVE':
        return {
          title: `${mosque.name} deactiveren?`,
          message:
            'De moskee en al haar activiteiten, lezingen en aankondigingen verdwijnen uit de app. Haar beheerders kunnen verder voorbereiden. Er wordt niets gewist.',
          confirmLabel: 'Deactiveren',
          destructive: true,
        };
      case 'BLOCKED':
        return {
          title: `${mosque.name} blokkeren?`,
          message:
            'De moskee verdwijnt uit de app en haar eigen beheerders en redacteuren kunnen niet meer bij het beheer. Alleen platformbeheerders kunnen haar nog openen. Er wordt niets gewist.',
          confirmLabel: 'Blokkeren',
          destructive: true,
        };
      case 'ACTIVE':
        return {
          title:
            mosque.status === 'BLOCKED'
              ? `Blokkade van ${mosque.name} opheffen?`
              : `${mosque.name} activeren?`,
          message:
            'De moskee en haar gepubliceerde content zijn weer zichtbaar in de app, en haar beheerders kunnen weer beheren.',
          confirmLabel: mosque.status === 'BLOCKED' ? 'Blokkade opheffen' : 'Activeren',
        };
    }
  }
}

const DONE: Record<MosqueStatus, string> = {
  ACTIVE: 'actief',
  INACTIVE: 'gedeactiveerd',
  BLOCKED: 'geblokkeerd',
};
