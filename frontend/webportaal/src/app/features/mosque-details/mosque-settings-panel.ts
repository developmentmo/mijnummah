import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { MatSlideToggle } from '@angular/material/slide-toggle';
import { firstValueFrom } from 'rxjs';

import { AdminApi } from '../../core/api/admin-api';
import { describeError } from '../../core/api/api-error';
import { MosqueSettings } from '../../core/api/models';
import { AuthService } from '../../core/auth/auth.service';
import { MosqueContext } from '../../core/mosque/mosque-context';
import { Loadable } from '../../shared/data/loadable';
import { Notifier } from '../../shared/ui/notifier';
import { Skeleton } from '../../shared/ui/skeleton';
import { StatusMessage } from '../../shared/ui/status-message';

/**
 * Instellingen van de moskee. Een platformbeheerder wijzigt ze; een beheerder van de moskee ziet ze alleen, zodat
 * duidelijk is waarom iets uit staat.
 */
@Component({
  selector: 'mu-mosque-settings-panel',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatSlideToggle, Skeleton, StatusMessage],
  template: `
    <h2 class="mu-title-large">Instellingen</h2>
    <p class="mu-body-medium mu-muted">
      @if (canEdit) {
        Als platformbeheerder zet je deze functies per moskee aan of uit.
      } @else {
        Deze instellingen beheert de platformbeheerder.
      }
    </p>
    @if (error(); as message) {
      <mu-status-message kind="error" [message]="message" />
    }
    @if (settings.error(); as message) {
      <mu-status-message
        kind="error"
        [message]="message"
        actionLabel="Opnieuw proberen"
        (action)="settings.load()"
      />
    } @else if (settings.value(); as value) {
      <mat-slide-toggle
        [checked]="value.registrationsEnabled"
        [disabled]="!canEdit || busy()"
        (change)="save({ ...value, registrationsEnabled: $event.checked })"
        >Aanmelden voor activiteiten mogelijk</mat-slide-toggle
      >
      <mat-slide-toggle
        [checked]="value.pushEnabled"
        [disabled]="!canEdit || busy()"
        (change)="save({ ...value, pushEnabled: $event.checked })"
        >Beheerders mogen pushmeldingen versturen</mat-slide-toggle
      >
    } @else {
      <mu-skeleton height="var(--mu-space-12)" />
    }
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
  `,
})
export class MosqueSettingsPanel implements OnInit {
  private readonly api = inject(AdminApi);
  private readonly notifier = inject(Notifier);
  private readonly mosqueId = inject(MosqueContext).current()!.id;

  protected readonly canEdit = inject(AuthService).isPlatformAdmin();
  protected readonly settings = new Loadable(() => this.api.mosqueSettings(this.mosqueId));
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    void this.settings.load();
  }

  protected async save(changed: MosqueSettings): Promise<void> {
    if (!this.canEdit || this.busy()) return;
    this.busy.set(true);
    this.error.set(null);
    try {
      this.settings.set(
        await firstValueFrom(this.api.updateMosqueSettings(this.mosqueId, changed)),
      );
      this.notifier.success('Instellingen opgeslagen.');
    } catch (error) {
      this.error.set(describeError(error));
      // Terug naar wat de server heeft.
      void this.settings.load();
    } finally {
      this.busy.set(false);
    }
  }
}
