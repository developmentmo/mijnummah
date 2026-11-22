import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';
import {
  EMPTY,
  catchError,
  distinctUntilChanged,
  firstValueFrom,
  startWith,
  switchMap,
} from 'rxjs';

import { AdminApi } from '../../core/api/admin-api';
import { describeError } from '../../core/api/api-error';
import {
  Page,
  PushAudience,
  PushNotification,
  PushStatus,
  PushTarget,
} from '../../core/api/models';
import { formatDateTime } from '../../core/format/dates';
import { MosqueContext } from '../../core/mosque/mosque-context';
import { Loadable } from '../../shared/data/loadable';
import { controlError } from '../../shared/forms/form-errors';
import { Confirm } from '../../shared/ui/confirm-dialog';
import { Notifier } from '../../shared/ui/notifier';
import { PageHeader } from '../../shared/ui/page-header';
import { DUTCH_PAGINATOR } from '../../shared/ui/paginator-intl';
import { Skeleton } from '../../shared/ui/skeleton';
import { StatusBadge } from '../../shared/ui/status-badge';
import { StatusMessage } from '../../shared/ui/status-message';
import { PUSH_BODY_MAX, PUSH_TITLE_MAX } from './push-panel';

const PAGE_SIZE = 20;

export const TARGET_LABELS: Record<PushTarget, string> = {
  ALL_FOLLOWERS: 'Alle volgers die meldingen ontvangen',
  ANNOUNCEMENTS: 'Volgers die meldingen over aankondigingen ontvangen',
  EVENTS: 'Volgers die meldingen over activiteiten ontvangen',
  MEDIA: 'Volgers die meldingen over lezingen en video’s ontvangen',
};

const SUBJECT_LABELS: Record<PushNotification['contentType'], string> = {
  MESSAGE: 'Eigen bericht',
  ANNOUNCEMENT: 'Aankondiging',
  EVENT: 'Activiteit',
  MEDIA: 'Video',
};

const STATUS_LABELS: Record<PushStatus, string> = {
  SENDING: 'Bezig',
  SENT: 'Verstuurd',
  FAILED: 'Mislukt',
};

const PUSH_ERRORS: Record<string, string> = {
  PUSH_NOT_CONFIGURED: 'Pushmeldingen zijn nog niet ingesteld voor deze omgeving.',
};

/**
 * Pushmeldingen van de moskee: een eigen bericht versturen naar een gekozen doelgroep (alleen ADMIN) en alle
 * verstuurde meldingen met hun verzendstatus. Meldingen over een item verstuur je bij dat item.
 */
@Component({
  selector: 'mu-push-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [DUTCH_PAGINATOR],
  imports: [
    MatButton,
    MatFormFieldModule,
    MatInput,
    MatPaginator,
    MatSelectModule,
    PageHeader,
    ReactiveFormsModule,
    Skeleton,
    StatusBadge,
    StatusMessage,
  ],
  template: `
    <mu-page-header
      heading="Pushmeldingen"
      subtitle="Berichten op de telefoons van mensen die de moskee volgen. Een melding over een activiteit, video of aankondiging verstuur je bij dat item."
    />

    <section aria-labelledby="nieuw" class="panel">
      <h2 id="nieuw" class="mu-title-large">Eigen bericht versturen</h2>
      @if (!context.isAdmin()) {
        <mu-status-message
          kind="info"
          message="Alleen een beheerder van de moskee kan een pushmelding versturen."
        />
      } @else if (audience(); as a) {
        @if (!a.configured) {
          <mu-status-message
            kind="info"
            message="Pushmeldingen zijn nog niet ingesteld voor deze omgeving. Er gaat niets naar telefoons."
          />
        } @else if (!a.enabledForMosque) {
          <mu-status-message
            kind="info"
            message="De platformbeheerder heeft pushmeldingen voor deze moskee uitgezet."
          />
        } @else {
          <form class="mu-form" [formGroup]="form" (ngSubmit)="send()" novalidate>
            <mat-form-field appearance="outline" class="mu-field-full">
              <mat-label>Doelgroep</mat-label>
              <mat-select formControlName="audience">
                @for (t of targets; track t) {
                  <mat-option [value]="t">{{ targetLabels[t] }}</mat-option>
                }
              </mat-select>
              <mat-hint>{{ reach(a) }}</mat-hint>
            </mat-form-field>
            @if (a.simulated) {
              <mu-status-message
                kind="warning"
                message="Testomgeving: meldingen worden alleen gelogd en komen niet op telefoons aan."
              />
            }
            <mat-form-field appearance="outline" class="mu-field-full">
              <mat-label>Titel</mat-label>
              <input matInput formControlName="title" required [maxlength]="titleMax" />
              <mat-hint align="end">{{ form.controls.title.value.length }}/{{ titleMax }}</mat-hint>
              @if (form.controls.title.invalid) {
                <mat-error>{{ controlError(form.controls.title) }}</mat-error>
              }
            </mat-form-field>
            <mat-form-field appearance="outline" class="mu-field-full">
              <mat-label>Bericht</mat-label>
              <textarea
                matInput
                formControlName="body"
                rows="3"
                required
                [maxlength]="bodyMax"
              ></textarea>
              <mat-hint align="end">{{ form.controls.body.value.length }}/{{ bodyMax }}</mat-hint>
              @if (form.controls.body.invalid) {
                <mat-error>{{ controlError(form.controls.body) }}</mat-error>
              }
            </mat-form-field>
            @if (sendError(); as message) {
              <mu-status-message kind="error" [message]="message" />
            }
            <div class="mu-form-actions">
              <button matButton="filled" type="submit" [disabled]="busy() || a.devices === 0">
                Pushmelding versturen
              </button>
              @if (a.devices === 0) {
                <p class="mu-hint">Er is in deze doelgroep nog niemand die een melding ontvangt.</p>
              }
            </div>
          </form>
        }
      } @else if (audienceError(); as message) {
        <mu-status-message kind="error" [message]="message" />
      } @else {
        <mu-skeleton height="var(--mu-space-16)" />
      }
    </section>

    <section aria-labelledby="verstuurd">
      <h2 id="verstuurd" class="mu-title-large">Verstuurd</h2>
      @if (history.error(); as message) {
        <mu-status-message
          kind="error"
          [message]="message"
          actionLabel="Opnieuw proberen"
          (action)="history.load()"
        />
      } @else if (history.value(); as page) {
        @if (page.items.length === 0) {
          <p class="mu-body-medium mu-muted">Er zijn nog geen pushmeldingen verstuurd.</p>
        } @else {
          <div class="mu-table-wrap" [attr.aria-busy]="history.loading()">
            <table class="mu-table">
              <caption class="mu-visually-hidden">
                Verstuurde pushmeldingen, nieuwste eerst
              </caption>
              <thead>
                <tr>
                  <th scope="col">Verstuurd</th>
                  <th scope="col">Soort</th>
                  <th scope="col">Melding</th>
                  <th scope="col">Status</th>
                  <th scope="col">Resultaat</th>
                </tr>
              </thead>
              <tbody>
                @for (n of page.items; track n.id) {
                  <tr>
                    <td class="nowrap">{{ when(n.createdAt) }}</td>
                    <td>
                      {{ subjectLabels[n.contentType] }}
                      @if (n.audience) {
                        <span class="mu-body-small mu-muted block">{{
                          targetLabels[n.audience]
                        }}</span>
                      }
                    </td>
                    <td>
                      <span class="mu-title-small block">{{ n.title }}</span>
                      <span class="mu-body-small mu-muted">{{ n.body }}</span>
                    </td>
                    <td>
                      <mu-status-badge
                        [status]="
                          n.status === 'SENT'
                            ? 'PUBLISHED'
                            : n.status === 'FAILED'
                              ? 'CANCELLED'
                              : 'DRAFT'
                        "
                        [label]="statusLabels[n.status]"
                      />
                    </td>
                    <td>{{ result(n) }}</td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
          <mat-paginator
            [length]="page.totalItems"
            [pageIndex]="page.page"
            [pageSize]="pageSize"
            [hidePageSize]="true"
            (page)="goTo($event)"
            aria-label="Pagina's met pushmeldingen"
          />
          @if (hasSending(page)) {
            <button matButton="outlined" type="button" (click)="history.load()">
              Status vernieuwen
            </button>
          }
        }
      } @else {
        <mu-skeleton height="var(--mu-space-12)" />
        <mu-skeleton height="var(--mu-space-12)" />
      }
    </section>
  `,
  styles: `
    :host,
    section {
      display: grid;
      gap: var(--mu-space-4);
    }
    :host {
      gap: var(--mu-space-8);
    }
    h2 {
      margin: 0;
    }
    .block {
      display: block;
    }
  `,
})
export class PushPage implements OnInit {
  private readonly api = inject(AdminApi);
  private readonly confirm = inject(Confirm);
  private readonly notifier = inject(Notifier);
  protected readonly context = inject(MosqueContext);
  private readonly mosqueId = this.context.current()!.id;

  protected readonly targets = Object.keys(TARGET_LABELS) as PushTarget[];
  protected readonly targetLabels = TARGET_LABELS;
  protected readonly subjectLabels = SUBJECT_LABELS;
  protected readonly statusLabels = STATUS_LABELS;
  protected readonly titleMax = PUSH_TITLE_MAX;
  protected readonly bodyMax = PUSH_BODY_MAX;
  protected readonly pageSize = PAGE_SIZE;
  protected readonly controlError = controlError;

  protected readonly form = inject(NonNullableFormBuilder).group({
    audience: ['ALL_FOLLOWERS' as PushTarget],
    title: ['', [Validators.required, Validators.maxLength(PUSH_TITLE_MAX)]],
    body: ['', [Validators.required, Validators.maxLength(PUSH_BODY_MAX)]],
  });

  protected readonly busy = signal(false);
  protected readonly sendError = signal<string | null>(null);
  protected readonly audienceError = signal<string | null>(null);
  protected readonly audience = signal<PushAudience | null>(null);

  private readonly pageIndex = signal(0);
  protected readonly history = new Loadable<Page<PushNotification>>(() =>
    this.api.pushHistory(this.mosqueId, this.pageIndex(), PAGE_SIZE),
  );

  private readonly destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    void this.history.load();
    if (!this.context.isAdmin()) return;
    const target = this.form.controls.audience;
    target.valueChanges
      .pipe(
        startWith(target.value),
        distinctUntilChanged(),
        switchMap((t) =>
          this.api.messageAudience(this.mosqueId, t).pipe(
            catchError((error: unknown) => {
              this.audienceError.set(describeError(error));
              return EMPTY;
            }),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((a) => {
        this.audienceError.set(null);
        this.audience.set(a);
      });
  }

  protected reach(a: PushAudience): string {
    if (a.followers === 0) return 'Bereikt nu niemand.';
    const people = a.followers === 1 ? '1 volger' : `${a.followers} volgers`;
    const devices = a.devices === 1 ? '1 apparaat' : `${a.devices} apparaten`;
    return `Bereikt nu ${people} op ${devices}.`;
  }

  protected async send(): Promise<void> {
    if (this.busy()) return;
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    const v = this.form.getRawValue();
    const a = this.audience();
    const ok = await this.confirm.ask({
      title: 'Pushmelding versturen?',
      message: `“${v.title.trim()}” gaat naar ${TARGET_LABELS[v.audience].toLowerCase()}${a ? ` (${a.followers === 1 ? '1 volger' : `${a.followers} volgers`})` : ''}.`,
      note: 'Een verstuurde melding kan niet worden teruggehaald.',
      confirmLabel: 'Versturen',
    });
    if (!ok) return;
    this.busy.set(true);
    this.sendError.set(null);
    try {
      await firstValueFrom(
        this.api.sendMessage(this.mosqueId, {
          title: v.title.trim(),
          body: v.body.trim(),
          audience: v.audience,
        }),
      );
      this.form.reset({ audience: v.audience, title: '', body: '' });
      this.notifier.success('Pushmelding wordt verstuurd. De status staat hieronder.');
      this.pageIndex.set(0);
      void this.history.load();
    } catch (error) {
      this.sendError.set(describeError(error, PUSH_ERRORS));
    } finally {
      this.busy.set(false);
    }
  }

  protected goTo(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    void this.history.load();
  }

  protected hasSending(page: Page<PushNotification>): boolean {
    return page.items.some((n) => n.status === 'SENDING');
  }

  protected result(n: PushNotification): string {
    if (n.recipientCount === 0) return 'Niemand om te bereiken';
    if (n.status === 'SENDING') return `Bezig met ${n.recipientCount} apparaten`;
    const failed = n.failedCount ? `, ${n.failedCount} mislukt` : '';
    return `${n.deliveredCount} van ${n.recipientCount} afgeleverd${failed}`;
  }

  protected when(iso: string): string {
    return formatDateTime(iso, this.context.timeZone());
  }
}
