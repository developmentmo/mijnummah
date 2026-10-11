import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import {
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { firstValueFrom } from 'rxjs';

import { AdminApi } from '../../core/api/admin-api';
import { describeError } from '../../core/api/api-error';
import { AdminMosque, MosqueDetailsRequest } from '../../core/api/models';
import { MosqueContext } from '../../core/mosque/mosque-context';
import {
  applyServerErrors,
  blankToNull,
  controlError,
  httpUrl,
} from '../../shared/forms/form-errors';
import { HasUnsavedChanges } from '../../shared/forms/unsaved-changes.guard';
import { Notifier } from '../../shared/ui/notifier';
import { PageHeader } from '../../shared/ui/page-header';
import { StatusMessage } from '../../shared/ui/status-message';

const countryCode: ValidatorFn = (control): ValidationErrors | null =>
  !control.value || /^[A-Za-z]{2}$/.test(control.value) ? null : { countryCode: true };

/** Profiel van de moskee zoals de app het toont op Ontdekken. Alleen voor de rol Beheerder. */
@Component({
  selector: 'mu-mosque-details-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    MatButton,
    MatFormFieldModule,
    MatInput,
    PageHeader,
    ReactiveFormsModule,
    StatusMessage,
  ],
  templateUrl: './mosque-details-page.html',
})
export class MosqueDetailsPage implements OnInit, HasUnsavedChanges {
  private readonly api = inject(AdminApi);
  private readonly notifier = inject(Notifier);
  private readonly context = inject(MosqueContext);
  private readonly mosqueId = this.context.current()!.id;

  protected readonly form = inject(NonNullableFormBuilder).group({
    name: ['', [Validators.required, Validators.maxLength(150)]],
    description: ['', Validators.maxLength(4000)],
    street: ['', Validators.maxLength(150)],
    postalCode: ['', Validators.maxLength(20)],
    city: ['', Validators.maxLength(100)],
    countryCode: ['', countryCode],
    email: ['', [Validators.email, Validators.maxLength(254)]],
    phone: ['', Validators.maxLength(30)],
    websiteUrl: ['', [Validators.maxLength(500), httpUrl]],
    timeZone: ['', Validators.maxLength(50)],
  });
  protected readonly busy = signal(false);
  protected readonly saveError = signal<string | null>(null);
  protected readonly controlError = controlError;

  ngOnInit(): void {
    this.undo();
  }

  /** Terug naar de opgeslagen gegevens. */
  protected undo(): void {
    this.show(this.context.current()!);
  }

  hasUnsavedChanges(): boolean {
    return this.form.dirty;
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
    const v = this.form.getRawValue();
    const request: MosqueDetailsRequest = {
      name: v.name.trim(),
      description: blankToNull(v.description),
      street: blankToNull(v.street),
      postalCode: blankToNull(v.postalCode),
      city: blankToNull(v.city),
      countryCode: blankToNull(v.countryCode)?.toUpperCase() ?? null,
      email: blankToNull(v.email),
      phone: blankToNull(v.phone),
      websiteUrl: blankToNull(v.websiteUrl),
      timeZone: blankToNull(v.timeZone),
    };
    try {
      const updated = await firstValueFrom(this.api.updateMosque(this.mosqueId, request));
      this.context.replace(updated);
      this.show(updated);
      this.notifier.success('Moskeegegevens opgeslagen. De app toont ze direct.');
    } catch (error) {
      const applied = applyServerErrors(this.form, error);
      this.saveError.set(
        applied
          ? 'Controleer de gemarkeerde velden.'
          : describeError(error, {
              INVALID_MOSQUE: 'De tijdzone is ongeldig. Gebruik bijvoorbeeld Europe/Amsterdam.',
            }),
      );
    } finally {
      this.busy.set(false);
    }
  }

  private show(mosque: AdminMosque): void {
    this.form.reset({
      name: mosque.name,
      description: mosque.description ?? '',
      street: mosque.address.street ?? '',
      postalCode: mosque.address.postalCode ?? '',
      city: mosque.address.city ?? '',
      countryCode: mosque.address.countryCode ?? '',
      email: mosque.contact.email ?? '',
      phone: mosque.contact.phone ?? '',
      websiteUrl: mosque.contact.websiteUrl ?? '',
      timeZone: mosque.timeZone ?? '',
    });
  }
}
