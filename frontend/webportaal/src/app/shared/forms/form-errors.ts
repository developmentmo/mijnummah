import { AbstractControl, FormGroup, ValidationErrors, ValidatorFn } from '@angular/forms';

import { ApiError, translateFieldMessage } from '../../core/api/api-error';

/** Eén Nederlandse foutmelding voor een formulierveld (de eerste fout telt). */
export function controlError(control: AbstractControl | null): string {
  const errors = control?.errors;
  if (!errors) return '';
  if (errors['required']) return 'Vul dit veld in.';
  if (errors['maxlength']) return `Maximaal ${errors['maxlength'].requiredLength} tekens.`;
  if (errors['minlength']) return `Minimaal ${errors['minlength'].requiredLength} tekens.`;
  if (errors['email']) return 'Vul een geldig e-mailadres in.';
  if (errors['min']) return `Minimaal ${errors['min'].min}.`;
  if (errors['https']) return 'Gebruik een link die met https:// begint.';
  if (errors['httpUrl']) return 'Gebruik een link die met http:// of https:// begint.';
  if (errors['duration']) return 'Gebruik u:mm:ss, mm:ss of een aantal minuten.';
  if (errors['countryCode']) return 'Gebruik een landcode van twee letters, bijv. NL.';
  if (errors['youtube']) return 'Plak een YouTube-link, bijv. https://www.youtube.com/watch?v=…';
  if (errors['server']) return errors['server'];
  return 'Controleer dit veld.';
}

export const httpsUrl: ValidatorFn = (control): ValidationErrors | null =>
  !control.value || /^https:\/\/\S+$/.test(control.value) ? null : { https: true };

export const httpUrl: ValidatorFn = (control): ValidationErrors | null =>
  !control.value || /^https?:\/\/\S+$/.test(control.value) ? null : { httpUrl: true };

/** Een YouTube-link of los video-ID (11 tekens); de backend controleert de link definitief. */
export const youtubeUrl: ValidatorFn = (control): ValidationErrors | null => {
  const value = String(control.value ?? '').trim();
  if (!value) return null;
  const ok =
    /^[A-Za-z0-9_-]{11}$/.test(value) ||
    /^https?:\/\/(www\.|m\.)?(youtube\.com|youtu\.be|youtube-nocookie\.com)\/\S+$/.test(value);
  return ok ? null : { youtube: true };
};

/**
 * Zet de veldfouten uit een VALIDATION_FAILED-antwoord op de bijbehorende velden. Geeft true als er minstens
 * één veld gevonden is; anders moet het scherm de fout als algemene melding tonen.
 */
export function applyServerErrors(
  form: FormGroup,
  error: unknown,
  fieldMap: Record<string, string> = {},
): boolean {
  const apiError = ApiError.from(error);
  let applied = false;
  for (const fieldError of apiError.fieldErrors) {
    const name = fieldMap[fieldError.field] ?? fieldError.field;
    const control = form.get(name);
    if (control) {
      control.setErrors({ ...control.errors, server: translateFieldMessage(fieldError.message) });
      control.markAsTouched();
      applied = true;
    }
  }
  return applied;
}

/** Leeg tekstveld naar null, zodat optionele velden niet als lege tekst worden opgeslagen. */
export function blankToNull(value: string | null | undefined): string | null {
  const trimmed = value?.trim();
  return trimmed ? trimmed : null;
}
