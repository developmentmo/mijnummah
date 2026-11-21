import { ChangeDetectionStrategy, Component, OnInit, inject, input, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { Router, RouterLink } from '@angular/router';

import { describeError } from '../../core/api/api-error';
import { AuthService } from '../../core/auth/auth.service';
import { controlError } from '../../shared/forms/form-errors';
import { BrandLogo } from '../../shared/ui/brand-logo';
import { StatusMessage } from '../../shared/ui/status-message';
import { ThemeToggle } from '../../shared/ui/theme-toggle';

const MIN_PASSWORD = 12;

/**
 * Wachtwoord instellen met de code uit een e-mail: de uitnodiging voor een nieuw account, of een code die de
 * platformbeheerder heeft laten sturen. Daarna is de gebruiker ingelogd.
 */
@Component({
  selector: 'mu-set-password-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    BrandLogo,
    MatButton,
    MatFormFieldModule,
    MatInput,
    ReactiveFormsModule,
    RouterLink,
    StatusMessage,
    ThemeToggle,
  ],
  template: `
    <main id="inhoud" class="page">
      <div class="theme"><mu-theme-toggle /></div>
      <section class="card" aria-labelledby="titel">
        <mu-brand-logo [height]="48" />
        <div class="intro">
          <h1 id="titel" class="mu-headline-medium">Wachtwoord instellen</h1>
          <p class="mu-body-medium mu-muted">
            Vul de code uit de e-mail in en kies je eigen wachtwoord. De code werkt één keer.
          </p>
        </div>
        @if (error(); as message) {
          <mu-status-message kind="error" [message]="message" />
        }
        <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
          <mat-form-field appearance="outline">
            <mat-label>E-mailadres</mat-label>
            <input matInput type="email" formControlName="email" autocomplete="username" required />
            @if (form.controls.email.invalid) {
              <mat-error>{{ controlError(form.controls.email) }}</mat-error>
            }
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Code uit de e-mail</mat-label>
            <input
              matInput
              formControlName="code"
              autocomplete="one-time-code"
              spellcheck="false"
              required
            />
            <mat-hint>Bijvoorbeeld ABCD-EFGH</mat-hint>
            @if (form.controls.code.invalid) {
              <mat-error>{{ controlError(form.controls.code) }}</mat-error>
            }
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Nieuw wachtwoord</mat-label>
            <input
              matInput
              type="password"
              formControlName="password"
              autocomplete="new-password"
              required
            />
            <mat-hint>Minstens {{ minPassword }} tekens.</mat-hint>
            @if (form.controls.password.invalid) {
              <mat-error>{{ controlError(form.controls.password) }}</mat-error>
            }
          </mat-form-field>
          <button matButton="filled" type="submit" [disabled]="busy()">
            {{ busy() ? 'Bezig…' : 'Wachtwoord instellen en inloggen' }}
          </button>
        </form>
        <p class="mu-body-small mu-muted">
          Al een wachtwoord? <a routerLink="/inloggen">Inloggen</a>. Code verlopen? Vraag de
          platformbeheerder om een nieuwe.
        </p>
      </section>
    </main>
  `,
  styleUrl: './login-page.scss',
})
export class SetPasswordPage implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  /** Queryparameter, zodat een link het adres al kan invullen. */
  readonly email = input<string>();

  protected readonly form = inject(NonNullableFormBuilder).group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
    code: ['', [Validators.required, Validators.maxLength(20)]],
    password: [
      '',
      [Validators.required, Validators.minLength(MIN_PASSWORD), Validators.maxLength(200)],
    ],
  });
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly controlError = controlError;
  protected readonly minPassword = MIN_PASSWORD;

  ngOnInit(): void {
    const email = this.email();
    if (email) this.form.controls.email.setValue(email);
  }

  protected async submit(): Promise<void> {
    if (this.busy()) return;
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    this.busy.set(true);
    this.error.set(null);
    const { email, code, password } = this.form.getRawValue();
    try {
      await this.auth.setPasswordWithCode(email.trim(), code.trim(), password);
      await this.router.navigateByUrl('/');
    } catch (error) {
      this.error.set(
        describeError(error, {
          INVALID_RESET_CODE:
            'Deze code klopt niet, is verlopen of al gebruikt. Vraag zo nodig een nieuwe code.',
          INVALID_PASSWORD: `Kies een wachtwoord van minstens ${MIN_PASSWORD} tekens.`,
          ACCOUNT_BLOCKED: 'Dit account is geblokkeerd. Neem contact op met de platformbeheerder.',
        }),
      );
      this.form.controls.password.reset();
    } finally {
      this.busy.set(false);
    }
  }
}
