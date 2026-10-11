import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  input,
  signal,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { MatRadioModule } from '@angular/material/radio';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggle } from '@angular/material/slide-toggle';
import { Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { AdminApi } from '../../core/api/admin-api';
import { ApiError, describeError } from '../../core/api/api-error';
import { AdminEvent } from '../../core/api/models';
import { formatDateTime } from '../../core/format/dates';
import {
  EVENT_CATEGORY_LABELS,
  EVENT_STATUS_LABELS,
  VISIBILITY_LABELS,
  options,
} from '../../core/format/labels';
import { MosqueContext } from '../../core/mosque/mosque-context';
import { applyServerErrors, controlError } from '../../shared/forms/form-errors';
import { HasUnsavedChanges } from '../../shared/forms/unsaved-changes.guard';
import { PushPanel } from '../notifications/push-panel';
import { Confirm } from '../../shared/ui/confirm-dialog';
import { Notifier } from '../../shared/ui/notifier';
import { PageHeader } from '../../shared/ui/page-header';
import { Skeleton } from '../../shared/ui/skeleton';
import { StatusBadge } from '../../shared/ui/status-badge';
import { StatusMessage } from '../../shared/ui/status-message';
import {
  EVENT_FIELD_MAP,
  createEventForm,
  eventToFormValue,
  formToEventRequest,
} from './event-form';

const EVENT_ERRORS: Record<string, string> = {
  EVENT_HAS_REGISTRATIONS:
    'Er zijn al aanmeldingen voor deze activiteit. Annuleer de activiteit als hij niet doorgaat.',
  EVENT_NOT_DRAFT: 'Dit kan alleen bij een concept.',
  EVENT_CANCELLED: 'Deze activiteit is geannuleerd en kan niet meer worden gewijzigd.',
  EVENT_NOT_PUBLISHED: 'Dit kan alleen bij een gepubliceerde activiteit.',
  CAPACITY_BELOW_REGISTRATIONS:
    'Het maximum aantal plaatsen is lager dan het aantal aanmeldingen. Kies een hoger maximum.',
  INVALID_EVENT: 'Controleer de tijden en het maximum aantal plaatsen.',
};

type Action = 'publish' | 'unpublish' | 'cancel' | 'delete';

/** Activiteit aanmaken of wijzigen, plus publiceren, terugzetten, annuleren en verwijderen. */
@Component({
  selector: 'mu-event-edit-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    MatButton,
    MatFormFieldModule,
    MatInput,
    MatRadioModule,
    MatSelectModule,
    MatSlideToggle,
    PageHeader,
    PushPanel,
    ReactiveFormsModule,
    RouterLink,
    Skeleton,
    StatusBadge,
    StatusMessage,
  ],
  templateUrl: './event-edit-page.html',
})
export class EventEditPage implements OnInit, HasUnsavedChanges {
  private readonly api = inject(AdminApi);
  private readonly router = inject(Router);
  private readonly confirm = inject(Confirm);
  private readonly notifier = inject(Notifier);
  protected readonly context = inject(MosqueContext);
  private readonly mosqueId = this.context.current()!.id;

  /** Uit de route; leeg bij een nieuwe activiteit. */
  readonly eventId = input<string>();

  protected readonly form = createEventForm(inject(FormBuilder));
  protected readonly event = signal<AdminEvent | null>(null);
  protected readonly loadError = signal<string | null>(null);
  protected readonly saveError = signal<string | null>(null);
  protected readonly actionError = signal<string | null>(null);
  protected readonly busy = signal(false);

  protected readonly isNew = computed(() => !this.eventId());
  protected readonly listLink = ['/m', this.mosqueId, 'agenda'];
  protected readonly categories = options(EVENT_CATEGORY_LABELS);
  protected readonly visibilities = options(VISIBILITY_LABELS);
  protected readonly statusLabels = EVENT_STATUS_LABELS;
  protected readonly controlError = controlError;
  protected readonly registrationEnabled = signal(false);

  ngOnInit(): void {
    this.form.controls.registrationEnabled.valueChanges.subscribe((on) =>
      this.registrationEnabled.set(on),
    );
    if (!this.isNew()) void this.load();
  }

  hasUnsavedChanges(): boolean {
    return this.form.dirty;
  }

  protected async load(): Promise<void> {
    this.loadError.set(null);
    try {
      this.show(await firstValueFrom(this.api.event(this.mosqueId, this.eventId()!)));
    } catch (error) {
      this.loadError.set(describeError(error));
    }
  }

  protected async save(): Promise<void> {
    if (this.busy()) return;
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      this.saveError.set('Controleer de gemarkeerde velden.');
      return;
    }
    this.busy.set(true);
    this.saveError.set(null);
    const request = formToEventRequest(this.form, this.context.timeZone());
    try {
      if (this.isNew()) {
        const created = await firstValueFrom(this.api.createEvent(this.mosqueId, request));
        this.form.markAsPristine();
        this.notifier.success(
          'Concept opgeslagen. Publiceer de activiteit om hem in de app te tonen.',
        );
        await this.router.navigate([...this.listLink, created.id], { replaceUrl: true });
      } else {
        this.show(
          await firstValueFrom(this.api.updateEvent(this.mosqueId, this.eventId()!, request)),
        );
        this.notifier.success('Wijzigingen opgeslagen.');
      }
    } catch (error) {
      const applied = applyServerErrors(this.form, error, EVENT_FIELD_MAP);
      this.saveError.set(
        applied ? 'Controleer de gemarkeerde velden.' : describeError(error, EVENT_ERRORS),
      );
    } finally {
      this.busy.set(false);
    }
  }

  protected async run(action: Action): Promise<void> {
    const event = this.event();
    if (!event || this.busy() || !(await this.confirmAction(action, event))) return;
    this.busy.set(true);
    this.actionError.set(null);
    try {
      if (action === 'delete') {
        await firstValueFrom(this.api.deleteEvent(this.mosqueId, event.id));
        this.form.markAsPristine();
        this.notifier.success('Concept verwijderd.');
        await this.router.navigate(this.listLink);
        return;
      }
      this.show(await firstValueFrom(this.api.eventAction(this.mosqueId, event.id, action)));
      this.notifier.success(
        {
          publish: 'Gepubliceerd. De activiteit staat nu in de app.',
          unpublish: 'Teruggezet naar concept. De activiteit is niet meer zichtbaar in de app.',
          cancel: 'Activiteit geannuleerd.',
        }[action],
      );
    } catch (error) {
      this.actionError.set(describeError(error, EVENT_ERRORS));
      if (ApiError.from(error).status === 409) void this.load();
    } finally {
      this.busy.set(false);
    }
  }

  /** Zoals in de agenda: voorbij na het einde, of na het begin als er geen einde is. */
  protected isOver(event: AdminEvent): boolean {
    return Date.parse(event.endsAt ?? event.startsAt) <= Date.now();
  }

  protected when(iso: string | null): string {
    return iso ? formatDateTime(iso, this.context.timeZone()) : '—';
  }

  private show(event: AdminEvent): void {
    this.event.set(event);
    this.form.reset(eventToFormValue(event, this.context.timeZone()));
    this.registrationEnabled.set(event.registrationEnabled);
    if (event.status === 'CANCELLED') this.form.disable();
    else this.form.enable();
  }

  private confirmAction(action: Action, event: AdminEvent): Promise<boolean> {
    const audience = event.visibility === 'PUBLIC' ? 'voor iedereen' : 'voor ingelogde gebruikers';
    switch (action) {
      case 'publish':
        return this.confirm.ask({
          title: 'Activiteit publiceren?',
          message: `“${event.title}” wordt zichtbaar in de app ${audience}.`,
          note: 'Er wordt geen pushmelding verstuurd.',
          confirmLabel: 'Publiceren',
        });
      case 'unpublish':
        return this.confirm.ask({
          title: 'Terugzetten naar concept?',
          message: `“${event.title}” verdwijnt uit de app tot je hem opnieuw publiceert.`,
          confirmLabel: 'Terugzetten',
        });
      case 'cancel':
        return this.confirm.ask({
          title: 'Activiteit annuleren?',
          message: `“${event.title}” blijft in de app staan als geannuleerd. Aanmelden kan dan niet meer en de activiteit kan daarna niet meer worden gewijzigd.`,
          note:
            event.confirmedRegistrations > 0
              ? `Er zijn ${event.confirmedRegistrations} aanmeldingen. Zij krijgen nog geen automatisch bericht; laat het ze zelf weten.`
              : undefined,
          confirmLabel: 'Activiteit annuleren',
          cancelLabel: 'Niet annuleren',
          destructive: true,
        });
      case 'delete':
        return this.confirm.ask({
          title: 'Concept verwijderen?',
          message: `“${event.title}” wordt definitief verwijderd. Dit kan niet ongedaan worden gemaakt.`,
          confirmLabel: 'Verwijderen',
          destructive: true,
        });
    }
  }
}
