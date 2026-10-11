import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnInit,
  computed,
  inject,
  input,
  signal,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { firstValueFrom } from 'rxjs';

import { AdminApi } from '../../core/api/admin-api';
import { describeError } from '../../core/api/api-error';
import { PushAudience, PushContentType, PushNotification } from '../../core/api/models';
import { formatDateTime } from '../../core/format/dates';
import { MosqueContext } from '../../core/mosque/mosque-context';
import { blankToNull } from '../../shared/forms/form-errors';
import { Confirm } from '../../shared/ui/confirm-dialog';
import { Notifier } from '../../shared/ui/notifier';
import { Skeleton } from '../../shared/ui/skeleton';
import { StatusMessage } from '../../shared/ui/status-message';

export const PUSH_TITLE_MAX = 100;
export const PUSH_BODY_MAX = 250;

const PUSH_ERRORS: Record<string, string> = {
  PUSH_ALREADY_SENT: 'Over dit item is al een pushmelding verstuurd. Dat kan maar één keer.',
  PUSH_NOT_CONFIGURED: 'Pushmeldingen zijn nog niet ingesteld voor deze omgeving.',
  CONTENT_NOT_PUBLISHED: 'Alleen gepubliceerde inhoud kan als pushmelding worden verstuurd.',
};

/** Zo vaak (elke 2 seconden) wordt de status nagevraagd terwijl een melding nog wordt verstuurd. */
const STATUS_CHECKS = 10;

/**
 * Pushmelding over één gepubliceerd item: wie hem zou krijgen, versturen (één keer, alleen ADMIN) en het
 * resultaat. Staat los van publiceren; het paneel herhaalt dat, zodat niemand het een met het ander verwart.
 */
@Component({
  selector: 'mu-push-panel',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButton, MatFormFieldModule, MatInput, ReactiveFormsModule, Skeleton, StatusMessage],
  template: `
    <section class="mu-push" aria-labelledby="push-titel">
      <h2 id="push-titel" class="mu-title-medium">Pushmelding</h2>
      <p class="mu-body-small mu-hint">
        Publiceren verstuurt nooit een pushmelding. Een pushmelding is een aparte actie en kan één
        keer per {{ itemName() }}.
      </p>

      @if (loading()) {
        <mu-skeleton height="var(--mu-space-12)" />
      } @else if (loadError(); as message) {
        <mu-status-message
          kind="error"
          [message]="message"
          actionLabel="Opnieuw proberen"
          (action)="load()"
        />
      } @else if (sent(); as notification) {
        <dl>
          <div>
            <dt class="mu-label-medium">Verstuurd</dt>
            <dd class="mu-body-medium">{{ when(notification.createdAt) }}</dd>
          </div>
          <div>
            <dt class="mu-label-medium">Melding</dt>
            <dd class="mu-body-medium">
              <strong>{{ notification.title }}</strong
              ><br />{{ notification.body }}
            </dd>
          </div>
          <div>
            <dt class="mu-label-medium">Resultaat</dt>
            <dd class="mu-body-medium" aria-live="polite">{{ result(notification) }}</dd>
          </div>
        </dl>
        @if (notification.status === 'SENDING') {
          <button matButton="outlined" type="button" (click)="refresh()">Status vernieuwen</button>
        }
      } @else if (!audience()?.configured) {
        <mu-status-message
          kind="info"
          message="Pushmeldingen zijn nog niet ingesteld voor deze omgeving. Er gaat niets naar telefoons."
        />
      } @else if (!context.isAdmin()) {
        <mu-status-message
          kind="info"
          message="Alleen een beheerder van de moskee kan een pushmelding versturen."
        />
      } @else if (!eligible()) {
        <mu-status-message kind="info" [message]="ineligibleReason()" />
      } @else {
        @if (audience(); as a) {
          <p class="mu-body-medium">{{ reach(a) }}</p>
          @if (a.simulated) {
            <mu-status-message
              kind="warning"
              message="Testomgeving: meldingen worden alleen gelogd en komen niet op telefoons aan."
            />
          }
        }
        <form class="mu-form" [formGroup]="form" (ngSubmit)="send()" novalidate>
          <mat-form-field appearance="outline">
            <mat-label>Titel</mat-label>
            <input matInput formControlName="title" [maxlength]="titleMax" />
            <mat-hint>Leeg laten: {{ defaultTitleHint() }}</mat-hint>
            <mat-hint align="end">{{ form.controls.title.value.length }}/{{ titleMax }}</mat-hint>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Tekst</mat-label>
            <textarea matInput formControlName="body" rows="3" [maxlength]="bodyMax"></textarea>
            <mat-hint>Leeg laten: {{ defaultBodyHint() }}</mat-hint>
            <mat-hint align="end">{{ form.controls.body.value.length }}/{{ bodyMax }}</mat-hint>
          </mat-form-field>
          @if (sendError(); as message) {
            <mu-status-message kind="error" [message]="message" />
          }
          @if (blocked()) {
            <p class="mu-body-small mu-hint">Sla eerst je wijzigingen op.</p>
          }
          <button
            matButton="filled"
            type="submit"
            [disabled]="busy() || blocked() || audience()?.devices === 0"
          >
            Pushmelding versturen
          </button>
          @if (audience()?.devices === 0) {
            <p class="mu-body-small mu-hint">
              Er is nog niemand die deze melding zou ontvangen, dus versturen heeft geen zin.
            </p>
          }
        </form>
      }
    </section>
  `,
  styles: `
    .mu-push {
      display: grid;
      gap: var(--mu-space-3);
      padding-top: var(--mu-space-3);
      border-top: 1px solid var(--mu-color-border);
    }
    dl {
      display: grid;
      gap: var(--mu-space-2);
      margin: 0;
    }
    dd {
      margin: 0;
    }
  `,
})
export class PushPanel implements OnInit {
  private readonly api = inject(AdminApi);
  private readonly confirm = inject(Confirm);
  private readonly notifier = inject(Notifier);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly context = inject(MosqueContext);
  private readonly mosqueId = this.context.current()!.id;

  readonly contentType = input.required<PushContentType>();
  readonly contentId = input.required<string>();
  /** Of het item nu verstuurd mag worden (gepubliceerd, niet verlopen of voorbij). */
  readonly eligible = input.required<boolean>();
  readonly ineligibleReason = input.required<string>();
  /** Niet-opgeslagen wijzigingen in het formulier ernaast: eerst opslaan. */
  readonly blocked = input(false);
  readonly defaultTitleHint = input.required<string>();
  readonly defaultBodyHint = input.required<string>();

  protected readonly titleMax = PUSH_TITLE_MAX;
  protected readonly bodyMax = PUSH_BODY_MAX;
  protected readonly form = inject(FormBuilder).nonNullable.group({
    title: ['', Validators.maxLength(PUSH_TITLE_MAX)],
    body: ['', Validators.maxLength(PUSH_BODY_MAX)],
  });

  protected readonly loading = signal(true);
  protected readonly loadError = signal<string | null>(null);
  protected readonly sendError = signal<string | null>(null);
  protected readonly busy = signal(false);
  protected readonly audience = signal<PushAudience | null>(null);
  protected readonly sent = signal<PushNotification | null>(null);
  protected readonly itemName = computed(
    () =>
      ({ ANNOUNCEMENT: 'aankondiging', EVENT: 'activiteit', MEDIA: 'video' })[this.contentType()],
  );

  private statusChecks = 0;
  private statusTimer: ReturnType<typeof setTimeout> | undefined;

  constructor() {
    this.destroyRef.onDestroy(() => clearTimeout(this.statusTimer));
  }

  ngOnInit(): void {
    void this.load();
  }

  protected async load(): Promise<void> {
    this.loading.set(true);
    this.loadError.set(null);
    try {
      const [audience, existing] = await Promise.all([
        firstValueFrom(this.api.pushAudience(this.mosqueId, this.contentType())),
        firstValueFrom(this.api.pushFor(this.mosqueId, this.contentType(), this.contentId())),
      ]);
      this.audience.set(audience);
      this.showSent(existing.items[0] ?? null);
    } catch (error) {
      this.loadError.set(describeError(error));
    } finally {
      this.loading.set(false);
    }
  }

  protected async send(): Promise<void> {
    const audience = this.audience();
    if (this.busy() || this.blocked() || this.form.invalid || !audience) return;
    const title = this.form.controls.title.value.trim();
    const confirmed = await this.confirm.ask({
      title: 'Pushmelding versturen?',
      message: `${this.reach(audience)} Dit kan maar één keer per ${this.itemName()}.`,
      note: 'Een verstuurde melding kan niet worden teruggehaald.',
      confirmLabel: 'Versturen',
    });
    if (!confirmed) return;
    this.busy.set(true);
    this.sendError.set(null);
    try {
      const notification = await firstValueFrom(
        this.api.sendPush(this.mosqueId, this.contentType(), this.contentId(), {
          title: blankToNull(title),
          body: blankToNull(this.form.controls.body.value),
        }),
      );
      this.showSent(notification);
      this.notifier.success('Pushmelding wordt verstuurd.');
    } catch (error) {
      this.sendError.set(describeError(error, PUSH_ERRORS));
    } finally {
      this.busy.set(false);
    }
  }

  protected async refresh(): Promise<void> {
    try {
      const page = await firstValueFrom(
        this.api.pushFor(this.mosqueId, this.contentType(), this.contentId()),
      );
      this.showSent(page.items[0] ?? null);
    } catch {
      // the next check, or the button, tries again
    }
  }

  protected reach(audience: PushAudience): string {
    const people = audience.followers === 1 ? '1 volger' : `${audience.followers} volgers`;
    return `Deze melding gaat nu naar ${people} op ${devices(audience.devices)}.`;
  }

  protected result(notification: PushNotification): string {
    switch (notification.status) {
      case 'SENDING':
        return `Wordt verstuurd naar ${devices(notification.recipientCount)}…`;
      case 'FAILED':
        return 'Versturen is mislukt: de pushdienst was niet bereikbaar of weigerde de melding.';
      case 'SENT':
        if (notification.recipientCount === 0)
          return 'Niemand ontving deze melding (geen apparaten).';
        return notification.failedCount === 0
          ? `Afgeleverd op ${devices(notification.deliveredCount)}.`
          : `Afgeleverd op ${devices(notification.deliveredCount)}, ${notification.failedCount} niet bereikt.`;
    }
  }

  protected when(iso: string): string {
    return formatDateTime(iso, this.context.timeZone());
  }

  private showSent(notification: PushNotification | null): void {
    this.sent.set(notification);
    clearTimeout(this.statusTimer);
    if (notification?.status === 'SENDING' && this.statusChecks < STATUS_CHECKS) {
      this.statusChecks++;
      this.statusTimer = setTimeout(() => void this.refresh(), 2000);
    }
  }
}

function devices(count: number): string {
  return count === 1 ? '1 apparaat' : `${count} apparaten`;
}
