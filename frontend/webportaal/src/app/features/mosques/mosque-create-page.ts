import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { MatSlideToggle } from '@angular/material/slide-toggle';
import { Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { AdminApi } from '../../core/api/admin-api';
import { describeError } from '../../core/api/api-error';
import { MosqueContext } from '../../core/mosque/mosque-context';
import {
  applyServerErrors,
  blankToNull,
  controlError,
  httpUrl,
  slug,
} from '../../shared/forms/form-errors';
import { HasUnsavedChanges } from '../../shared/forms/unsaved-changes.guard';
import { Notifier } from '../../shared/ui/notifier';
import { PageHeader } from '../../shared/ui/page-header';
import { StatusMessage } from '../../shared/ui/status-message';

const CREATE_ERRORS: Record<string, string> = {
  SLUG_ALREADY_TAKEN: 'Er bestaat al een moskee met dit adres in de app. Kies een ander.',
  ACCOUNT_BLOCKED:
    'Het account van de eerste beheerder is geblokkeerd. Hef de blokkade op bij Accounts of kies een ander e-mailadres.',
  PASSWORD_RESET_UNAVAILABLE:
    'E-mail is op deze server niet ingesteld, dus de nieuwe beheerder kan geen uitnodiging krijgen. Gebruik een bestaand account, of registreer de moskee zonder beheerder en maak het account aan bij Accounts.',
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
 * Platformbeheer: een moskee registreren. In één keer: haar gegevens, haar adres in de app (de slug, zelf te kiezen
 * of automatisch uniek gemaakt), haar configuratie en eventueel haar eerste beheerder. Een bestaand account krijgt
 * de rol Beheerder; voor een nieuw e-mailadres maakt de backend een account met een uitnodiging per e-mail.
 */
@Component({
  selector: 'mu-mosque-create-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    MatButton,
    MatFormFieldModule,
    MatInput,
    MatSlideToggle,
    PageHeader,
    ReactiveFormsModule,
    RouterLink,
    StatusMessage,
  ],
  template: `
    <mu-page-header
      heading="Moskee registreren"
      subtitle="Een nieuwe moskee op het platform, met haar eerste beheerder en instellingen."
    />

    <form class="mu-form" [formGroup]="form" (ngSubmit)="create()" novalidate>
      <fieldset class="mu-fieldset">
        <legend>Gegevens</legend>
        <mat-form-field appearance="outline">
          <mat-label>Naam</mat-label>
          <input matInput formControlName="name" required maxlength="150" />
          @if (form.controls.name.invalid) {
            <mat-error>{{ controlError(form.controls.name) }}</mat-error>
          }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Beschrijving</mat-label>
          <textarea matInput formControlName="description" rows="3" maxlength="4000"></textarea>
          @if (form.controls.description.invalid) {
            <mat-error>{{ controlError(form.controls.description) }}</mat-error>
          }
        </mat-form-field>
        <div class="mu-form-row">
          <mat-form-field appearance="outline">
            <mat-label>Straat en huisnummer</mat-label>
            <input matInput formControlName="street" maxlength="150" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Postcode</mat-label>
            <input matInput formControlName="postalCode" maxlength="20" />
          </mat-form-field>
        </div>
        <mat-form-field appearance="outline">
          <mat-label>Plaats</mat-label>
          <input matInput formControlName="city" maxlength="100" />
          @if (form.controls.city.invalid) {
            <mat-error>{{ controlError(form.controls.city) }}</mat-error>
          }
        </mat-form-field>
        <div class="mu-form-row">
          <mat-form-field appearance="outline">
            <mat-label>E-mailadres van de moskee</mat-label>
            <input matInput type="email" formControlName="email" maxlength="254" />
            @if (form.controls.email.invalid) {
              <mat-error>{{ controlError(form.controls.email) }}</mat-error>
            }
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Telefoon</mat-label>
            <input matInput type="tel" formControlName="phone" maxlength="30" />
          </mat-form-field>
        </div>
        <mat-form-field appearance="outline">
          <mat-label>Website</mat-label>
          <input matInput type="url" formControlName="websiteUrl" maxlength="500" />
          @if (form.controls.websiteUrl.invalid) {
            <mat-error>{{ controlError(form.controls.websiteUrl) }}</mat-error>
          }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Adres in de app (slug)</mat-label>
          <input
            matInput
            formControlName="slug"
            maxlength="60"
            autocomplete="off"
            spellcheck="false"
          />
          <mat-hint>
            Volgt de naam; bestaat het al, dan komt er een nummer achter. Vast na registreren.
          </mat-hint>
          @if (form.controls.slug.invalid) {
            <mat-error>{{ controlError(form.controls.slug) }}</mat-error>
          }
        </mat-form-field>
      </fieldset>

      <fieldset class="mu-fieldset">
        <legend>Eerste beheerder</legend>
        <mat-slide-toggle formControlName="withAdministrator"
          >Nu een beheerder toevoegen</mat-slide-toggle
        >
        @if (withAdministrator()) {
          <div class="mu-form-row">
            <mat-form-field appearance="outline">
              <mat-label>Naam van de beheerder</mat-label>
              <input matInput formControlName="adminName" maxlength="100" />
              @if (form.controls.adminName.invalid) {
                <mat-error>{{ controlError(form.controls.adminName) }}</mat-error>
              }
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>E-mailadres van de beheerder</mat-label>
              <input
                matInput
                type="email"
                formControlName="adminEmail"
                maxlength="254"
                autocomplete="off"
              />
              @if (form.controls.adminEmail.invalid) {
                <mat-error>{{ controlError(form.controls.adminEmail) }}</mat-error>
              }
            </mat-form-field>
          </div>
          <p class="mu-body-small mu-muted">
            Heeft dit adres al een account, dan krijgt dat de rol Beheerder. Anders maken we een
            account en krijgt de beheerder een e-mail om zelf een wachtwoord te kiezen.
          </p>
        }
      </fieldset>

      <fieldset class="mu-fieldset">
        <legend>Instellingen</legend>
        <mat-slide-toggle formControlName="registrationsEnabled"
          >Aanmelden voor activiteiten mogelijk</mat-slide-toggle
        >
        <mat-slide-toggle formControlName="pushEnabled"
          >Beheerders mogen pushmeldingen versturen</mat-slide-toggle
        >
        <p class="mu-body-small mu-muted">Je kunt dit later wijzigen bij Moskeegegevens.</p>
      </fieldset>

      @if (error(); as message) {
        <mu-status-message kind="error" [message]="message" />
      }
      <div class="mu-form-actions">
        <button matButton="filled" type="submit" [disabled]="busy()">Moskee registreren</button>
        <a matButton routerLink="/platform/moskeeen">Annuleren</a>
      </div>
    </form>
  `,
  styles: `
    .mu-fieldset {
      display: grid;
      gap: var(--mu-space-3);
    }
  `,
})
export class MosqueCreatePage implements HasUnsavedChanges {
  private readonly api = inject(AdminApi);
  private readonly context = inject(MosqueContext);
  private readonly notifier = inject(Notifier);
  private readonly router = inject(Router);

  protected readonly form = inject(NonNullableFormBuilder).group({
    name: ['', [Validators.required, Validators.maxLength(150)]],
    description: ['', [Validators.maxLength(4000)]],
    street: ['', [Validators.maxLength(150)]],
    postalCode: ['', [Validators.maxLength(20)]],
    city: ['', [Validators.maxLength(100)]],
    email: ['', [Validators.email, Validators.maxLength(254)]],
    phone: ['', [Validators.maxLength(30)]],
    websiteUrl: ['', [Validators.maxLength(500), httpUrl]],
    slug: ['', [Validators.maxLength(60), slug]],
    withAdministrator: [true],
    adminName: ['', [Validators.maxLength(100)]],
    adminEmail: ['', [Validators.email, Validators.maxLength(254)]],
    registrationsEnabled: [true],
    pushEnabled: [true],
  });
  protected readonly withAdministrator = toSignal(
    this.form.controls.withAdministrator.valueChanges,
    { initialValue: true },
  );
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly controlError = controlError;
  private created = false;

  constructor() {
    // Zolang de slug niet zelf is aangepast, volgt hij de naam.
    this.form.controls.name.valueChanges.pipe(takeUntilDestroyed()).subscribe((name) => {
      if (!this.form.controls.slug.dirty) this.form.controls.slug.setValue(slugFrom(name));
    });
    // De beheerdersvelden zijn alleen verplicht als er een beheerder bij komt.
    this.form.controls.withAdministrator.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((on) => this.requireAdministrator(on));
    this.requireAdministrator(true);
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
      const created = await firstValueFrom(
        this.api.createMosque({
          // Niet zelf aangepast: de backend maakt een unieke slug uit de naam.
          slug: this.form.controls.slug.dirty ? blankToNull(v.slug) : null,
          details: {
            name: v.name.trim(),
            description: blankToNull(v.description),
            street: blankToNull(v.street),
            postalCode: blankToNull(v.postalCode),
            city: blankToNull(v.city),
            countryCode: 'NL',
            email: blankToNull(v.email),
            phone: blankToNull(v.phone),
            websiteUrl: blankToNull(v.websiteUrl),
            timeZone: null,
          },
          firstAdministrator: v.withAdministrator
            ? { email: v.adminEmail.trim(), displayName: v.adminName.trim() }
            : null,
          settings: { registrationsEnabled: v.registrationsEnabled, pushEnabled: v.pushEnabled },
        }),
      );
      this.created = true;
      await this.context.load(true);
      const admin = created.firstAdministrator;
      this.notifier.success(
        `${created.mosque.name} is geregistreerd.` +
          (admin === null
            ? ' Voeg een beheerder toe bij Beheerders.'
            : admin.invited
              ? ` ${v.adminName.trim()} krijgt een e-mail om een wachtwoord te kiezen.`
              : ` ${v.adminName.trim()} is beheerder.`),
      );
      await this.router.navigate(['/m', created.mosque.id, 'gegevens']);
    } catch (error) {
      const applied = applyServerErrors(this.form, error, {
        'details.name': 'name',
        'details.description': 'description',
        'details.city': 'city',
        'details.email': 'email',
        'details.websiteUrl': 'websiteUrl',
        'firstAdministrator.email': 'adminEmail',
        'firstAdministrator.displayName': 'adminName',
      });
      this.error.set(
        applied ? 'Controleer de gemarkeerde velden.' : describeError(error, CREATE_ERRORS),
      );
    } finally {
      this.busy.set(false);
    }
  }

  private requireAdministrator(on: boolean): void {
    for (const control of [this.form.controls.adminName, this.form.controls.adminEmail]) {
      if (on) control.addValidators(Validators.required);
      else control.removeValidators(Validators.required);
      control.updateValueAndValidity({ emitEvent: false });
    }
  }
}
