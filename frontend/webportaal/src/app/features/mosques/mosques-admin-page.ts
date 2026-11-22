import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { MatButton } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { AdminApi } from '../../core/api/admin-api';
import { describeError } from '../../core/api/api-error';
import { MosqueCounts, MosqueStatus } from '../../core/api/models';
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
 * Platformbeheer: alle moskeeën op het platform, ook inactieve en geblokkeerde, met hun beheerders, volgers en
 * content. Zoeken en filteren, registreren, openen om te beheren, en de status wijzigen. Inactief en geblokkeerd verbergen de moskee en al haar content in de app; geblokkeerd sluit
 * ook haar eigen beheerders buiten. Er wordt niets gewist.
 */
@Component({
  selector: 'mu-mosques-admin-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    MatButton,
    MatFormFieldModule,
    MatInput,
    MatSelectModule,
    PageHeader,
    RouterLink,
    Skeleton,
    StatusBadge,
    StatusMessage,
  ],
  template: `
    <mu-page-header
      heading="Moskeeën"
      subtitle="Als platformbeheerder beheer je elke moskee met alle rechten van een beheerder."
    >
      <a matButton="filled" routerLink="/platform/moskeeen/nieuw">Moskee registreren</a>
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
    } @else if (visible(); as list) {
      <div class="filters">
        <mat-form-field appearance="outline">
          <mat-label>Zoeken</mat-label>
          <input
            matInput
            type="search"
            [value]="query()"
            (input)="query.set($any($event.target).value)"
            placeholder="Naam, plaats of slug"
          />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Status</mat-label>
          <mat-select [value]="statusFilter()" (valueChange)="statusFilter.set($event)">
            <mat-option [value]="null">Alle</mat-option>
            <mat-option value="ACTIVE">Actief</mat-option>
            <mat-option value="INACTIVE">Inactief</mat-option>
            <mat-option value="BLOCKED">Geblokkeerd</mat-option>
          </mat-select>
        </mat-form-field>
      </div>
      <p class="mu-body-small mu-muted" aria-live="polite">
        {{ list.length }} van {{ mosques()?.length }} moskeeën
      </p>
      <div class="mu-table-wrap">
        <table class="mu-table">
          <caption class="mu-visually-hidden">
            Alle moskeeën op naam
          </caption>
          <thead>
            <tr>
              <th scope="col">Naam</th>
              <th scope="col">Plaats</th>
              <th scope="col">Status</th>
              <th scope="col" class="num">Beheerders</th>
              <th scope="col" class="num">Volgers</th>
              <th scope="col" class="num">Evenementen</th>
              <th scope="col" class="num">Video’s</th>
              <th scope="col" class="num">Nieuws</th>
              <th scope="col"><span class="mu-visually-hidden">Acties</span></th>
            </tr>
          </thead>
          <tbody>
            @for (m of list; track m.id) {
              <tr>
                <td>
                  {{ m.name }}<br />
                  <code class="mu-body-small">{{ m.slug }}</code>
                </td>
                <td>{{ m.city || '–' }}</td>
                <td>
                  <mu-status-badge [status]="m.status" [label]="statusLabels[m.status]" />
                </td>
                <td class="num">{{ m.administrators }}</td>
                <td class="num">{{ m.followers }}</td>
                <td class="num">{{ m.events }}</td>
                <td class="num">{{ m.videos }}</td>
                <td class="num">{{ m.announcements }}</td>
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
            } @empty {
              <tr>
                <td colspan="9" class="mu-muted">Geen moskeeën gevonden.</td>
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
    .num {
      text-align: end;
    }
    .filters {
      display: flex;
      flex-wrap: wrap;
      gap: var(--mu-space-3);
    }
  `,
})
export class MosquesAdminPage implements OnInit {
  private readonly api = inject(AdminApi);
  private readonly context = inject(MosqueContext);
  private readonly confirm = inject(Confirm);
  private readonly notifier = inject(Notifier);

  protected readonly mosques = signal<MosqueCounts[] | null>(null);
  protected readonly query = signal('');
  protected readonly statusFilter = signal<MosqueStatus | null>(null);
  protected readonly visible = computed(() => {
    const all = this.mosques();
    if (!all) return null;
    const q = this.query().trim().toLowerCase();
    const status = this.statusFilter();
    return all.filter(
      (m) =>
        (!status || m.status === status) &&
        (!q || [m.name, m.city ?? '', m.slug].some((value) => value.toLowerCase().includes(q))),
    );
  });
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
      this.mosques.set(await firstValueFrom(this.api.platformMosques()));
    } catch (error) {
      this.error.set(describeError(error));
    }
  }

  protected async changeStatus(mosque: MosqueCounts, to: MosqueStatus): Promise<void> {
    if (this.busy()) return;
    const ok = await this.confirm.ask(this.question(mosque, to));
    if (!ok) return;
    this.busy.set(true);
    this.rowError.set(null);
    try {
      const updated = await firstValueFrom(this.api.setMosqueStatus(mosque.id, to));
      this.mosques.update(
        (list) =>
          list?.map((m) => (m.id === updated.id ? { ...m, status: updated.status } : m)) ?? null,
      );
      // De kopbalk en de moskeekeuze tonen de status ook.
      void this.context.load(true).catch(() => undefined);
      this.notifier.success(`${updated.name} is ${DONE[updated.status]}.`);
    } catch (error) {
      this.rowError.set(describeError(error));
    } finally {
      this.busy.set(false);
    }
  }

  private question(mosque: MosqueCounts, to: MosqueStatus) {
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
