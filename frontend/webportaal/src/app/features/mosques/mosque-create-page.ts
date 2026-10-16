import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { AdminApi } from '../../core/api/admin-api';
import { describeError } from '../../core/api/api-error';
import { MosqueContext } from '../../core/mosque/mosque-context';
import { applyServerErrors, controlError, slug } from '../../shared/forms/form-errors';
import { HasUnsavedChanges } from '../../shared/forms/unsaved-changes.guard';
import { Notifier } from '../../shared/ui/notifier';
import { PageHeader } from '../../shared/ui/page-header';
import { StatusMessage } from '../../shared/ui/status-message';

const CREATE_ERRORS: Record<string, string> = {
  SLUG_ALREADY_TAKEN: 'Er bestaat al een moskee met deze slug.',
};

/** Slug-voorstel uit de naam: "Moskee Al-Fath Utrecht" wordt "moskee-al-fath-utrecht". */
export function slugFrom(name: string): string {
  return name
    .normalize('NFKD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 60)
    .replace(/-+$/, '');
}

/**
 * Platformbeheer: een nieuwe moskee op het gedeelde platform. De slug moet gelijk zijn aan die van de app van de
 * moskee (brands/<slug>/brand.json, zie WHITE_LABEL.md). De overige gegevens vult de beheerder daarna in.
 */
@Component({
  selector: 'mu-mosque-create-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    MatButton,
    MatFormFieldModule,
    MatInput,
    PageHeader,
    ReactiveFormsModule,
    RouterLink,
    StatusMessage,
  ],
  template: `
    <mu-page-header
      heading="Moskee toevoegen"
      subtitle="Platformbeheer: een nieuwe moskee op het platform, met een eigen app."
    />

    <form class="mu-form" [formGroup]="form" (ngSubmit)="create()" novalidate>
      <fieldset class="mu-fieldset">
        <legend>Nieuwe moskee</legend>
        <div class="mu-form-row">
          <mat-form-field appearance="outline">
            <mat-label>Naam</mat-label>
            <input matInput formControlName="name" required maxlength="150" />
            @if (form.controls.name.invalid) {
              <mat-error>{{ controlError(form.controls.name) }}</mat-error>
            }
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Plaats</mat-label>
            <input matInput formControlName="city" maxlength="100" />
            @if (form.controls.city.invalid) {
              <mat-error>{{ controlError(form.controls.city) }}</mat-error>
            }
          </mat-form-field>
        </div>
        <mat-form-field appearance="outline">
          <mat-label>Slug</mat-label>
          <input
            matInput
            formControlName="slug"
            required
            maxlength="60"
            autocomplete="off"
            spellcheck="false"
          />
          <mat-hint>
            Vast na aanmaken. Gelijk aan de slug van de app van deze moskee (brands/&lt;slug&gt;).
          </mat-hint>
          @if (form.controls.slug.invalid) {
            <mat-error>{{ controlError(form.controls.slug) }}</mat-error>
          }
        </mat-form-field>
      </fieldset>
      @if (error(); as message) {
        <mu-status-message kind="error" [message]="message" />
      }
      <div class="mu-form-actions">
        <button matButton="filled" type="submit" [disabled]="busy()">Moskee toevoegen</button>
        <a matButton routerLink="/moskeeen" [queryParams]="{ kies: 1 }">Annuleren</a>
      </div>
    </form>
  `,
})
export class MosqueCreatePage implements HasUnsavedChanges {
  private readonly api = inject(AdminApi);
  private readonly context = inject(MosqueContext);
  private readonly notifier = inject(Notifier);
  private readonly router = inject(Router);

  protected readonly form = inject(NonNullableFormBuilder).group({
    name: ['', [Validators.required, Validators.maxLength(150)]],
    city: ['', [Validators.maxLength(100)]],
    slug: ['', [Validators.required, Validators.maxLength(60), slug]],
  });
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly controlError = controlError;
  private created = false;

  constructor() {
    // Zolang de slug niet zelf is aangepast, volgt hij de naam.
    this.form.controls.name.valueChanges.pipe(takeUntilDestroyed()).subscribe((name) => {
      if (!this.form.controls.slug.dirty) this.form.controls.slug.setValue(slugFrom(name));
    });
  }

  hasUnsavedChanges(): boolean {
    return !this.created && this.form.dirty;
  }

  protected async create(): Promise<void> {
    if (this.busy()) return;
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    const v = this.form.getRawValue();
    this.busy.set(true);
    this.error.set(null);
    try {
      const mosque = await firstValueFrom(
        this.api.createMosque(v.slug, {
          name: v.name.trim(),
          description: null,
          street: null,
          postalCode: null,
          city: v.city.trim() || null,
          countryCode: 'NL',
          email: null,
          phone: null,
          websiteUrl: null,
          timeZone: null,
        }),
      );
      this.created = true;
      await this.context.load(true);
      this.notifier.success(
        `${mosque.name} is toegevoegd. Vul de gegevens aan en voeg een beheerder toe bij Beheerders.`,
      );
      await this.router.navigate(['/m', mosque.id, 'gegevens']);
    } catch (error) {
      const applied = applyServerErrors(this.form, error, {
        'details.name': 'name',
        'details.city': 'city',
      });
      this.error.set(
        applied ? 'Controleer de gemarkeerde velden.' : describeError(error, CREATE_ERRORS),
      );
    } finally {
      this.busy.set(false);
    }
  }
}
