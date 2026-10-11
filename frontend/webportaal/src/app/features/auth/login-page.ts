import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { Router } from '@angular/router';

import { ApiError, describeError } from '../../core/api/api-error';
import { AuthService } from '../../core/auth/auth.service';
import { safeReturnUrl } from '../../core/auth/guards';
import { controlError } from '../../shared/forms/form-errors';
import { BrandLogo } from '../../shared/ui/brand-logo';
import { StatusMessage } from '../../shared/ui/status-message';
import { ThemeToggle } from '../../shared/ui/theme-toggle';

@Component({
  selector: 'mu-login-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    BrandLogo,
    MatButton,
    MatFormFieldModule,
    MatInput,
    ReactiveFormsModule,
    StatusMessage,
    ThemeToggle,
  ],
  templateUrl: './login-page.html',
  styleUrl: './login-page.scss',
})
export class LoginPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  /** Pagina om na het inloggen naar terug te keren (queryparameter `terug`). */
  readonly terug = input<string>();

  protected readonly form = inject(NonNullableFormBuilder).group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
    password: ['', [Validators.required, Validators.maxLength(200)]],
  });
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly sessionExpired = this.auth.lastEndReason() === 'expired';
  protected readonly controlError = controlError;

  protected async submit(): Promise<void> {
    if (this.busy()) return;
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    this.busy.set(true);
    this.error.set(null);
    const { email, password } = this.form.getRawValue();
    try {
      await this.auth.login(email.trim(), password);
      await this.router.navigateByUrl(safeReturnUrl(this.terug()));
    } catch (error) {
      this.error.set(this.describe(error));
      this.form.controls.password.reset();
    } finally {
      this.busy.set(false);
    }
  }

  private describe(error: unknown): string {
    const apiError = ApiError.from(error);
    const minutes = Math.max(1, Math.ceil((apiError.retryAfterSeconds ?? 60) / 60));
    return describeError(apiError, {
      INVALID_CREDENTIALS: 'Het e-mailadres of wachtwoord klopt niet.',
      ACCOUNT_BLOCKED: 'Dit account is geblokkeerd. Neem contact op met de platformbeheerder.',
      TOO_MANY_LOGIN_ATTEMPTS: `Te veel mislukte pogingen. Probeer het over ${minutes} ${minutes === 1 ? 'minuut' : 'minuten'} opnieuw.`,
      VALIDATION_FAILED: 'Vul een geldig e-mailadres en wachtwoord in.',
    });
  }
}
