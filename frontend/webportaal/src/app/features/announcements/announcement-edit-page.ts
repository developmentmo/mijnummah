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
import { MatSlideToggle } from '@angular/material/slide-toggle';
import { Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { AdminApi } from '../../core/api/admin-api';
import { describeError } from '../../core/api/api-error';
import { AdminAnnouncement } from '../../core/api/models';
import { formatDateTime } from '../../core/format/dates';
import { CONTENT_STATUS_LABELS } from '../../core/format/labels';
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
  announcementToFormValue,
  createAnnouncementForm,
  formToAnnouncementRequest,
} from './announcement-form';

const ANNOUNCEMENT_ERRORS: Record<string, string> = {
  INVALID_ANNOUNCEMENT:
    'De vervaldatum ligt in het verleden. Kies een latere datum of maak het veld leeg en sla eerst op.',
};

type Action = 'publish' | 'unpublish' | 'delete';

/**
 * Aankondiging opstellen en publiceren. Publiceren zet het bericht in de app; een pushmelding is een aparte
 * actie in het paneel ernaast (D-029). Het scherm zegt dat expliciet, zodat niemand denkt dat volgers bij
 * publiceren een melding krijgen.
 */
@Component({
  selector: 'mu-announcement-edit-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    MatButton,
    MatFormFieldModule,
    MatInput,
    MatSlideToggle,
    PageHeader,
    PushPanel,
    ReactiveFormsModule,
    RouterLink,
    Skeleton,
    StatusBadge,
    StatusMessage,
  ],
  templateUrl: './announcement-edit-page.html',
})
export class AnnouncementEditPage implements OnInit, HasUnsavedChanges {
  private readonly api = inject(AdminApi);
  private readonly router = inject(Router);
  private readonly confirm = inject(Confirm);
  private readonly notifier = inject(Notifier);
  protected readonly context = inject(MosqueContext);
  private readonly mosqueId = this.context.current()!.id;

  readonly announcementId = input<string>();

  protected readonly form = createAnnouncementForm(inject(FormBuilder));
  protected readonly announcement = signal<AdminAnnouncement | null>(null);
  protected readonly loadError = signal<string | null>(null);
  protected readonly saveError = signal<string | null>(null);
  protected readonly actionError = signal<string | null>(null);
  protected readonly busy = signal(false);

  protected readonly isNew = computed(() => !this.announcementId());
  protected readonly listLink = ['/m', this.mosqueId, 'aankondigingen'];
  protected readonly statusLabels = CONTENT_STATUS_LABELS;
  protected readonly controlError = controlError;

  ngOnInit(): void {
    if (!this.isNew()) void this.load();
  }

  hasUnsavedChanges(): boolean {
    return this.form.dirty;
  }

  protected async load(): Promise<void> {
    this.loadError.set(null);
    try {
      this.show(await firstValueFrom(this.api.announcement(this.mosqueId, this.announcementId()!)));
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
    const request = formToAnnouncementRequest(this.form, this.context.timeZone());
    try {
      if (this.isNew()) {
        const created = await firstValueFrom(this.api.createAnnouncement(this.mosqueId, request));
        this.form.markAsPristine();
        this.notifier.success(
          'Concept opgeslagen. Publiceer de aankondiging om hem in de app te tonen.',
        );
        await this.router.navigate([...this.listLink, created.id], { replaceUrl: true });
      } else {
        this.show(
          await firstValueFrom(
            this.api.updateAnnouncement(this.mosqueId, this.announcementId()!, request),
          ),
        );
        this.notifier.success('Wijzigingen opgeslagen.');
      }
    } catch (error) {
      const applied = applyServerErrors(this.form, error, { expiresAt: 'expiresDate' });
      this.saveError.set(
        applied ? 'Controleer de gemarkeerde velden.' : describeError(error, ANNOUNCEMENT_ERRORS),
      );
    } finally {
      this.busy.set(false);
    }
  }

  protected async run(action: Action): Promise<void> {
    const announcement = this.announcement();
    if (!announcement || this.busy() || !(await this.confirmAction(action, announcement))) return;
    this.busy.set(true);
    this.actionError.set(null);
    try {
      if (action === 'delete') {
        await firstValueFrom(this.api.deleteAnnouncement(this.mosqueId, announcement.id));
        this.form.markAsPristine();
        this.notifier.success('Aankondiging verwijderd.');
        await this.router.navigate(this.listLink);
        return;
      }
      this.show(
        await firstValueFrom(this.api.announcementAction(this.mosqueId, announcement.id, action)),
      );
      this.notifier.success(
        action === 'publish'
          ? 'Gepubliceerd in de app. Er is geen pushmelding verstuurd.'
          : 'Teruggezet naar concept. De aankondiging is niet meer zichtbaar in de app.',
      );
    } catch (error) {
      this.actionError.set(describeError(error, ANNOUNCEMENT_ERRORS));
    } finally {
      this.busy.set(false);
    }
  }

  protected when(iso: string | null): string {
    return iso ? formatDateTime(iso, this.context.timeZone()) : '—';
  }

  private show(announcement: AdminAnnouncement): void {
    this.announcement.set(announcement);
    this.form.reset(announcementToFormValue(announcement, this.context.timeZone()));
  }

  private confirmAction(action: Action, announcement: AdminAnnouncement): Promise<boolean> {
    switch (action) {
      case 'publish':
        return this.confirm.ask({
          title: 'Aankondiging publiceren in de app?',
          message: `“${announcement.title}” verschijnt in de app${announcement.featured ? ', uitgelicht op de startpagina' : ''}.`,
          note: 'Publiceren verstuurt géén pushmelding. Dat is een aparte actie die je daarna kunt kiezen.',
          confirmLabel: 'Publiceren in de app',
        });
      case 'unpublish':
        return this.confirm.ask({
          title: 'Terugzetten naar concept?',
          message: `“${announcement.title}” verdwijnt uit de app tot je hem opnieuw publiceert.`,
          confirmLabel: 'Terugzetten',
        });
      case 'delete':
        return this.confirm.ask({
          title: 'Aankondiging verwijderen?',
          message: `“${announcement.title}” wordt definitief verwijderd. Dit kan niet ongedaan worden gemaakt.`,
          confirmLabel: 'Verwijderen',
          destructive: true,
        });
    }
  }
}
