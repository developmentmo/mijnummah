import { ChangeDetectionStrategy, Component, Injectable, inject } from '@angular/core';
import { MatButton } from '@angular/material/button';
import {
  MAT_DIALOG_DATA,
  MatDialog,
  MatDialogActions,
  MatDialogClose,
  MatDialogContent,
  MatDialogTitle,
} from '@angular/material/dialog';
import { firstValueFrom } from 'rxjs';

export interface ConfirmOptions {
  title: string;
  message: string;
  /** Extra uitleg onder de vraag, bijv. wat er níet gebeurt. */
  note?: string;
  confirmLabel: string;
  cancelLabel?: string;
  /** Rode knop voor acties die niet ongedaan te maken zijn of gevolgen hebben voor bezoekers. */
  destructive?: boolean;
}

/** Bevestigingsvenster voor risicovolle acties. De veilige keuze (annuleren) heeft de focus. */
@Component({
  selector: 'mu-confirm-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButton, MatDialogActions, MatDialogClose, MatDialogContent, MatDialogTitle],
  template: `
    <h2 mat-dialog-title>{{ data.title }}</h2>
    <mat-dialog-content>
      <p class="mu-body-large">{{ data.message }}</p>
      @if (data.note) {
        <p class="note mu-body-medium">{{ data.note }}</p>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button matButton type="button" [mat-dialog-close]="false" cdkFocusInitial>
        {{ data.cancelLabel ?? 'Annuleren' }}
      </button>
      <button
        matButton="filled"
        type="button"
        [class.destructive]="data.destructive"
        [mat-dialog-close]="true"
      >
        {{ data.confirmLabel }}
      </button>
    </mat-dialog-actions>
  `,
  styles: `
    .note {
      margin-top: var(--mu-space-3);
      padding: var(--mu-space-3);
      border-radius: var(--mu-radius-sm);
      background: var(--mu-color-info-container);
      color: var(--mu-color-on-info-container);
    }
    .destructive {
      --mat-button-filled-container-color: var(--mu-color-error);
      --mat-button-filled-label-text-color: var(--mu-color-on-primary);
    }
  `,
})
export class ConfirmDialog {
  protected readonly data = inject<ConfirmOptions>(MAT_DIALOG_DATA);
}

@Injectable({ providedIn: 'root' })
export class Confirm {
  private readonly dialog = inject(MatDialog);

  async ask(options: ConfirmOptions): Promise<boolean> {
    const ref = this.dialog.open<ConfirmDialog, ConfirmOptions, boolean>(ConfirmDialog, {
      data: options,
      maxWidth: 'var(--mu-modal-max-width)',
      role: options.destructive ? 'alertdialog' : 'dialog',
    });
    return (await firstValueFrom(ref.afterClosed())) === true;
  }
}
