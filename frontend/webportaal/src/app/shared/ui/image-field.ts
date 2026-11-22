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
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { EMPTY, firstValueFrom, merge } from 'rxjs';

import { AdminApi } from '../../core/api/admin-api';
import { describeError } from '../../core/api/api-error';
import { APP_CONFIG } from '../../core/config/app-config';
import { StatusMessage } from './status-message';

const MAX_BYTES = 10 * 1024 * 1024;
const TYPES = ['image/jpeg', 'image/png'];

/** Pad van een geüploade afbeelding vanaf de API-basis (zoals de backend het teruggeeft). */
export function imagePath(imageId: string): string {
  return `/api/v1/public/images/${imageId}`;
}

/**
 * Afbeelding bij content: uploaden, vervangen of verwijderen. De afbeelding gaat direct naar de backend (die haar
 * controleert, verkleint en de metadata verwijdert); het formulier bewaart alleen het ID. Een oudere externe link
 * (urlControl) blijft zichtbaar tot er een nieuwe afbeelding is of tot "Verwijderen".
 */
@Component({
  selector: 'mu-image-field',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButton, StatusMessage],
  template: `
    <div class="field" role="group" [attr.aria-label]="label()">
      <span class="mu-label-large">{{ label() }}</span>
      <div class="preview" [style.aspect-ratio]="aspect()">
        @if (preview(); as src) {
          <img [src]="src" alt="" />
        } @else {
          <span class="mu-body-small mu-muted">Geen afbeelding</span>
        }
      </div>
      @if (error(); as message) {
        <mu-status-message kind="error" [message]="message" />
      }
      <div class="actions">
        <label class="mu-label-large upload" [class.disabled]="busy() || disabled()">
          {{
            busy() ? 'Bezig met uploaden…' : preview() ? 'Andere afbeelding' : 'Afbeelding kiezen'
          }}
          <input
            type="file"
            accept="image/jpeg,image/png"
            [disabled]="busy() || disabled()"
            (change)="upload($event)"
            [attr.aria-label]="label() + ' kiezen'"
          />
        </label>
        @if (preview()) {
          <button matButton type="button" [disabled]="busy() || disabled()" (click)="remove()">
            Verwijderen
          </button>
        }
      </div>
      <p class="mu-body-small mu-muted" data-testid="beeldrechten">
        JPEG of PNG, hooguit 10 MB. Gebruik alleen afbeeldingen waarvan de moskee de rechten heeft;
        vraag toestemming aan herkenbare mensen.
      </p>
    </div>
  `,
  styles: `
    .field {
      display: grid;
      gap: var(--mu-space-2);
      max-width: 28rem;
    }
    .preview {
      display: grid;
      place-items: center;
      overflow: hidden;
      border: 1px dashed var(--mu-color-border-strong);
      border-radius: var(--mu-radius-lg);
      background: var(--mu-color-surface-muted);
    }
    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
    p {
      margin: 0;
    }
    .actions {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: var(--mu-space-2);
    }
    .upload {
      position: relative;
      display: inline-flex;
      align-items: center;
      min-height: var(--mu-touch-target);
      padding: 0 var(--mu-space-4);
      border-radius: var(--mu-radius-full);
      background: var(--mu-color-primary);
      color: var(--mu-color-on-primary);
      cursor: pointer;
    }
    .upload:focus-within {
      outline: var(--mu-focus-ring-width) solid var(--mu-color-focus);
      outline-offset: var(--mu-focus-ring-offset);
    }
    .upload.disabled {
      opacity: 0.6;
      cursor: default;
    }
    .upload input {
      position: absolute;
      inset: 0;
      opacity: 0;
      cursor: inherit;
    }
  `,
})
export class ImageField implements OnInit {
  private readonly api = inject(AdminApi);
  private readonly destroyRef = inject(DestroyRef);
  private readonly apiBaseUrl = inject(APP_CONFIG).apiBaseUrl;

  readonly mosqueId = input.required<string>();
  readonly idControl = input.required<FormControl<string | null>>();
  /** Een oudere externe afbeelding (https-link), als die er is. */
  readonly urlControl = input<FormControl<string> | null>(null);
  readonly label = input('Afbeelding');
  readonly aspect = input('16 / 9');

  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  private readonly changes = signal(0);

  protected readonly disabled = computed(() => {
    this.changes();
    return this.idControl().disabled;
  });

  protected readonly preview = computed(() => {
    this.changes();
    const id = this.idControl().value;
    if (id) return `${this.apiBaseUrl}${imagePath(id)}`;
    return this.urlControl()?.value || null;
  });

  ngOnInit(): void {
    // Ook een reset van het formulier (na laden of opslaan) moet de voorvertoning bijwerken.
    const bump = () => this.changes.update((n) => n + 1);
    const id = this.idControl();
    merge(id.valueChanges, id.statusChanges, this.urlControl()?.valueChanges ?? EMPTY)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(bump);
  }

  protected async upload(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file || this.busy()) return;
    if (!TYPES.includes(file.type)) {
      this.error.set('Kies een JPEG- of PNG-bestand.');
      return;
    }
    if (file.size > MAX_BYTES) {
      this.error.set('Dit bestand is groter dan 10 MB. Kies een kleinere afbeelding.');
      return;
    }
    this.busy.set(true);
    this.error.set(null);
    try {
      const image = await firstValueFrom(this.api.uploadImage(this.mosqueId(), file));
      this.set(image.id);
    } catch (error) {
      this.error.set(describeError(error));
    } finally {
      this.busy.set(false);
    }
  }

  protected remove(): void {
    this.set(null);
    const url = this.urlControl();
    if (url && url.value) {
      url.setValue('');
      url.markAsDirty();
    }
  }

  private set(imageId: string | null): void {
    const control = this.idControl();
    control.setValue(imageId);
    control.markAsDirty();
    control.markAsTouched();
  }
}
