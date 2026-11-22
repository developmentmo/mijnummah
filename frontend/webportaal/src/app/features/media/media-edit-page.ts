import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  input,
  signal,
} from '@angular/core';
import { FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { MatRadioModule } from '@angular/material/radio';
import { MatSelectModule } from '@angular/material/select';
import { Router, RouterLink } from '@angular/router';
import { firstValueFrom, map } from 'rxjs';

import { AdminApi } from '../../core/api/admin-api';
import { ApiError, describeError } from '../../core/api/api-error';
import { AdminEvent, AdminMedia } from '../../core/api/models';
import { APP_CONFIG } from '../../core/config/app-config';
import { formatDate, formatDateTime, fromZonedParts, toZonedParts } from '../../core/format/dates';
import {
  CONTENT_STATUS_LABELS,
  MEDIA_CATEGORY_LABELS,
  VISIBILITY_LABELS,
  options,
} from '../../core/format/labels';
import { MosqueContext } from '../../core/mosque/mosque-context';
import { Loadable } from '../../shared/data/loadable';
import { applyServerErrors, controlError } from '../../shared/forms/form-errors';
import { HasUnsavedChanges } from '../../shared/forms/unsaved-changes.guard';
import { PushPanel } from '../notifications/push-panel';
import { Confirm } from '../../shared/ui/confirm-dialog';
import { ImageField } from '../../shared/ui/image-field';
import { Notifier } from '../../shared/ui/notifier';
import { PageHeader } from '../../shared/ui/page-header';
import { Skeleton } from '../../shared/ui/skeleton';
import { StatusBadge } from '../../shared/ui/status-badge';
import { StatusMessage } from '../../shared/ui/status-message';
import {
  MEDIA_FIELD_MAP,
  createMediaForm,
  formToMediaRequest,
  mediaToFormValue,
} from './media-form';

const MEDIA_ERRORS: Record<string, string> = {
  INVALID_VIDEO_URL: 'Deze link wordt niet herkend als YouTube-video.',
  VIDEO_ALREADY_ADDED: 'Deze video staat al in de mediabibliotheek.',
  INVALID_RELATED_EVENT: 'De gekoppelde activiteit hoort niet bij deze moskee.',
  INVALID_MEDIA: 'Controleer de duur van de video.',
  INVALID_PUBLICATION_DATE: 'De publicatiedatum mag hooguit een jaar vooruit liggen.',
};

type Action = 'publish' | 'unpublish' | 'delete';

/** Video toevoegen of wijzigen, plus publiceren, terugzetten en verwijderen. */
@Component({
  selector: 'mu-media-edit-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ImageField,
    MatButton,
    MatFormFieldModule,
    MatInput,
    MatRadioModule,
    MatSelectModule,
    PageHeader,
    PushPanel,
    ReactiveFormsModule,
    RouterLink,
    Skeleton,
    StatusBadge,
    StatusMessage,
  ],
  templateUrl: './media-edit-page.html',
  styles: `
    .thumb {
      width: 100%;
      aspect-ratio: 16 / 9;
      object-fit: cover;
      border-radius: var(--mu-radius-sm);
      background: var(--mu-color-surface-muted);
    }
  `,
})
export class MediaEditPage implements OnInit, HasUnsavedChanges {
  private readonly api = inject(AdminApi);
  private readonly router = inject(Router);
  private readonly confirm = inject(Confirm);
  private readonly notifier = inject(Notifier);
  protected readonly context = inject(MosqueContext);
  protected readonly mosqueId = this.context.current()!.id;
  private readonly apiBaseUrl = inject(APP_CONFIG).apiBaseUrl;

  /** Publicatiedatum en -tijd in de tijdzone van de moskee; los van het formulier met de gegevens. */
  protected readonly publishDate = new FormControl('', {
    nonNullable: true,
    validators: Validators.required,
  });
  protected readonly publishTime = new FormControl('', {
    nonNullable: true,
    validators: Validators.required,
  });

  readonly mediaId = input<string>();

  protected readonly form = createMediaForm(inject(FormBuilder));
  protected readonly media = signal<AdminMedia | null>(null);
  protected readonly loadError = signal<string | null>(null);
  protected readonly saveError = signal<string | null>(null);
  protected readonly actionError = signal<string | null>(null);
  protected readonly busy = signal(false);

  protected readonly isNew = computed(() => !this.mediaId());
  protected readonly listLink = ['/m', this.mosqueId, 'media'];
  protected readonly categories = options(MEDIA_CATEGORY_LABELS);
  protected readonly visibilities = options(VISIBILITY_LABELS);
  protected readonly statusLabels = CONTENT_STATUS_LABELS;
  protected readonly controlError = controlError;

  /** Recente activiteiten om een opname aan te koppelen. */
  protected readonly events = new Loadable<AdminEvent[]>(() =>
    this.api.events(this.mosqueId, { size: 50 }).pipe(map((page) => page.items)),
  );
  protected readonly relatedMissing = computed(() => {
    const id = this.media()?.relatedEventId;
    const list = this.events.value();
    return !!id && !!list && !list.some((e) => e.id === id);
  });

  ngOnInit(): void {
    void this.events.load();
    if (!this.isNew()) void this.load();
  }

  hasUnsavedChanges(): boolean {
    return this.form.dirty;
  }

  protected async load(): Promise<void> {
    this.loadError.set(null);
    try {
      this.show(await firstValueFrom(this.api.media(this.mosqueId, this.mediaId()!)));
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
    const request = formToMediaRequest(this.form);
    try {
      if (this.isNew()) {
        const created = await firstValueFrom(this.api.createMedia(this.mosqueId, request));
        this.form.markAsPristine();
        this.notifier.success('Concept opgeslagen. Publiceer de video om hem in de app te tonen.');
        await this.router.navigate([...this.listLink, created.id], { replaceUrl: true });
      } else {
        this.show(
          await firstValueFrom(this.api.updateMedia(this.mosqueId, this.mediaId()!, request)),
        );
        this.notifier.success('Wijzigingen opgeslagen.');
      }
    } catch (error) {
      const applied = applyServerErrors(this.form, error, MEDIA_FIELD_MAP);
      const code = ApiError.from(error).code;
      if (code === 'INVALID_VIDEO_URL' || code === 'VIDEO_ALREADY_ADDED') {
        this.form.controls.videoUrl.setErrors({ server: MEDIA_ERRORS[code] });
      }
      this.saveError.set(
        applied ? 'Controleer de gemarkeerde velden.' : describeError(error, MEDIA_ERRORS),
      );
    } finally {
      this.busy.set(false);
    }
  }

  protected async run(action: Action): Promise<void> {
    const media = this.media();
    if (!media || this.busy() || !(await this.confirmAction(action, media))) return;
    this.busy.set(true);
    this.actionError.set(null);
    try {
      if (action === 'delete') {
        await firstValueFrom(this.api.deleteMedia(this.mosqueId, media.id));
        this.form.markAsPristine();
        this.notifier.success('Video verwijderd uit de bibliotheek.');
        await this.router.navigate(this.listLink);
        return;
      }
      this.show(await firstValueFrom(this.api.mediaAction(this.mosqueId, media.id, action)));
      this.notifier.success(
        action === 'publish'
          ? 'Gepubliceerd. De video staat nu in de app.'
          : 'Teruggezet naar concept. De video is niet meer zichtbaar in de app.',
      );
    } catch (error) {
      this.actionError.set(describeError(error, MEDIA_ERRORS));
    } finally {
      this.busy.set(false);
    }
  }

  /** Publiceren op de gekozen datum: in het verleden (oudere opname) of later (gepland). */
  protected async publishAt(): Promise<void> {
    const media = this.media();
    if (!media || this.busy()) return;
    this.publishDate.markAsTouched();
    this.publishTime.markAsTouched();
    if (this.publishDate.invalid || this.publishTime.invalid) return;
    const at = fromZonedParts(
      this.publishDate.value,
      this.publishTime.value,
      this.context.timeZone(),
    );
    const future = Date.parse(at) > Date.now();
    const ok = await this.confirm.ask({
      title: future ? 'Video inplannen?' : 'Publiceren met deze datum?',
      message: future
        ? `“${media.title}” verschijnt op ${this.when(at)} in de app.`
        : `“${media.title}” staat direct in de app, met ${this.when(at)} als publicatiedatum.`,
      note: 'Er wordt geen pushmelding verstuurd.',
      confirmLabel: future ? 'Inplannen' : 'Publiceren',
    });
    if (!ok) return;
    this.busy.set(true);
    this.actionError.set(null);
    try {
      this.show(await firstValueFrom(this.api.publishMediaAt(this.mosqueId, media.id, at)));
      this.notifier.success(
        future ? `Ingepland voor ${this.when(at)}.` : 'Gepubliceerd. De video staat nu in de app.',
      );
    } catch (error) {
      this.actionError.set(describeError(error, MEDIA_ERRORS));
    } finally {
      this.busy.set(false);
    }
  }

  /** Gepubliceerd, maar de publicatiedatum moet nog komen. */
  protected isScheduled(media: AdminMedia): boolean {
    return (
      media.status === 'PUBLISHED' &&
      !!media.publishedAt &&
      Date.parse(media.publishedAt) > Date.now()
    );
  }

  protected thumbnail(media: AdminMedia): string | null {
    const url = media.thumbnailUrl;
    return url && url.startsWith('/') ? `${this.apiBaseUrl}${url}` : url;
  }

  protected eventLabel(event: AdminEvent): string {
    return `${formatDate(event.startsAt, this.context.timeZone())} · ${event.title}`;
  }

  protected when(iso: string | null): string {
    return iso ? formatDateTime(iso, this.context.timeZone()) : '—';
  }

  private show(media: AdminMedia): void {
    this.media.set(media);
    this.form.reset(mediaToFormValue(media));
    const at = media.publishedAt ? toZonedParts(media.publishedAt, this.context.timeZone()) : null;
    this.publishDate.reset(at?.date ?? '');
    this.publishTime.reset(at?.time ?? '');
  }

  private confirmAction(action: Action, media: AdminMedia): Promise<boolean> {
    switch (action) {
      case 'publish':
        return this.confirm.ask({
          title: 'Video publiceren?',
          message: `“${media.title}” wordt zichtbaar in de mediabibliotheek van de app ${media.visibility === 'PUBLIC' ? 'voor iedereen' : 'voor ingelogde gebruikers'}.`,
          note: 'Er wordt geen pushmelding verstuurd.',
          confirmLabel: 'Publiceren',
        });
      case 'unpublish':
        return this.confirm.ask({
          title: 'Terugzetten naar concept?',
          message: `“${media.title}” verdwijnt uit de app tot je hem opnieuw publiceert.`,
          confirmLabel: 'Terugzetten',
        });
      case 'delete':
        return this.confirm.ask({
          title: 'Video verwijderen?',
          message: `“${media.title}” wordt uit de bibliotheek verwijderd. De video zelf blijft op YouTube staan.`,
          confirmLabel: 'Verwijderen',
          destructive: true,
        });
    }
  }
}
