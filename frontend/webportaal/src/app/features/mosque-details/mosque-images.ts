import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { firstValueFrom } from 'rxjs';

import { AdminApi } from '../../core/api/admin-api';
import { describeError } from '../../core/api/api-error';
import { AdminMosque } from '../../core/api/models';
import { APP_CONFIG } from '../../core/config/app-config';
import { MosqueContext } from '../../core/mosque/mosque-context';
import { Notifier } from '../../shared/ui/notifier';
import { StatusMessage } from '../../shared/ui/status-message';

type Kind = 'logo' | 'cover';

const MAX_BYTES = 10 * 1024 * 1024;
const TYPES = ['image/jpeg', 'image/png'];

/**
 * Logo en omslagfoto van de moskee. Een bestand gaat eerst naar de backend (die het controleert, verkleint en de
 * metadata verwijdert) en wordt daarna gekoppeld; de app toont het direct.
 */
@Component({
  selector: 'mu-mosque-images',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButton, StatusMessage],
  template: `
    <h2 id="afbeeldingen" class="mu-title-large">Logo en omslagfoto</h2>
    <p class="mu-body-medium mu-muted">
      JPEG of PNG, hooguit 10 MB. Grote foto's worden verkleind; locatie- en toestelgegevens worden
      verwijderd.
    </p>
    @if (error(); as message) {
      <mu-status-message kind="error" [message]="message" />
    }
    <div class="images">
      @for (slot of slots(); track slot.kind) {
        <figure class="slot">
          <div class="preview" [class.cover]="slot.kind === 'cover'">
            @if (slot.url) {
              <img [src]="slot.url" [alt]="slot.label + ' van ' + mosque()?.name" />
            } @else {
              <span class="mu-body-small mu-muted">Geen {{ slot.label.toLowerCase() }}</span>
            }
          </div>
          <figcaption class="mu-title-small">{{ slot.label }}</figcaption>
          <div class="actions">
            <label class="mu-label-large upload" [class.disabled]="busy()">
              {{ slot.url ? 'Vervangen' : 'Uploaden' }}
              <input
                type="file"
                accept="image/jpeg,image/png"
                [disabled]="busy()"
                (change)="upload(slot.kind, $event)"
                [attr.aria-label]="slot.label + ' kiezen'"
              />
            </label>
            @if (slot.url) {
              <button matButton type="button" [disabled]="busy()" (click)="remove(slot.kind)">
                Verwijderen
              </button>
            }
          </div>
        </figure>
      }
    </div>
  `,
  styles: `
    :host {
      display: grid;
      gap: var(--mu-space-3);
    }
    h2,
    p {
      margin: 0;
    }
    .images {
      display: grid;
      grid-template-columns: minmax(0, 1fr) minmax(0, 2fr);
      gap: var(--mu-space-4);
    }
    .slot {
      display: grid;
      gap: var(--mu-space-2);
      margin: 0;
    }
    .preview {
      display: grid;
      place-items: center;
      aspect-ratio: 1;
      overflow: hidden;
      border: 1px dashed var(--mu-color-border-strong);
      border-radius: var(--mu-radius-lg);
      background: var(--mu-color-surface-muted);
    }
    .preview.cover {
      aspect-ratio: 2 / 1;
    }
    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
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
    @media (max-width: 599px) {
      .images {
        grid-template-columns: minmax(0, 1fr);
      }
    }
  `,
})
export class MosqueImages {
  private readonly api = inject(AdminApi);
  private readonly context = inject(MosqueContext);
  private readonly notifier = inject(Notifier);
  private readonly apiBaseUrl = inject(APP_CONFIG).apiBaseUrl;

  protected readonly mosque = this.context.current;
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly slots = computed(() => {
    const m = this.mosque();
    return [
      { kind: 'logo' as Kind, label: 'Logo', url: this.absolute(m?.logoUrl) },
      { kind: 'cover' as Kind, label: 'Omslagfoto', url: this.absolute(m?.coverUrl) },
    ];
  });

  protected async upload(kind: Kind, event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    const mosque = this.mosque();
    if (!file || !mosque || this.busy()) return;
    if (!TYPES.includes(file.type)) {
      this.error.set('Kies een JPEG- of PNG-bestand.');
      return;
    }
    if (file.size > MAX_BYTES) {
      this.error.set('Dit bestand is groter dan 10 MB. Kies een kleinere afbeelding.');
      return;
    }
    await this.run(async () => {
      const image = await firstValueFrom(this.api.uploadImage(mosque.id, file));
      const updated = await this.link(mosque, kind, image.id);
      this.notifier.success(`${kind === 'logo' ? 'Logo' : 'Omslagfoto'} opgeslagen.`);
      return updated;
    });
  }

  protected async remove(kind: Kind): Promise<void> {
    const mosque = this.mosque();
    if (!mosque || this.busy()) return;
    await this.run(async () => {
      const updated = await this.link(mosque, kind, null);
      this.notifier.success(`${kind === 'logo' ? 'Logo' : 'Omslagfoto'} verwijderd.`);
      return updated;
    });
  }

  private link(mosque: AdminMosque, kind: Kind, imageId: string | null): Promise<AdminMosque> {
    return firstValueFrom(
      this.api.setMosqueImages(
        mosque.id,
        kind === 'logo' ? imageId : mosque.logoImageId,
        kind === 'cover' ? imageId : mosque.coverImageId,
      ),
    );
  }

  private async run(action: () => Promise<AdminMosque>): Promise<void> {
    this.busy.set(true);
    this.error.set(null);
    try {
      this.context.replace(await action());
    } catch (error) {
      this.error.set(describeError(error));
    } finally {
      this.busy.set(false);
    }
  }

  private absolute(path: string | null | undefined): string | null {
    return path ? `${this.apiBaseUrl}${path}` : null;
  }
}
