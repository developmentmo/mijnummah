import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';

import { BrandLogo } from '../shared/ui/brand-logo';
import { Skeleton } from '../shared/ui/skeleton';
import { StatePlaceholder } from '../shared/ui/state-placeholder';
import { StatusMessage } from '../shared/ui/status-message';
import { ThemeToggle } from '../shared/ui/theme-toggle';

interface Swatch {
  name: string;
  token: string;
  on: string;
}

/**
 * Interne pagina met alle tokens en basiscomponenten van het portaal, in licht en donker.
 * Bevat alleen voorbeeldcontent.
 */
@Component({
  selector: 'mu-design-preview',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    BrandLogo,
    MatButton,
    MatCardModule,
    MatFormFieldModule,
    MatInput,
    ReactiveFormsModule,
    Skeleton,
    StatePlaceholder,
    StatusMessage,
    ThemeToggle,
  ],
  templateUrl: './design-preview.html',
  styleUrl: './design-preview.scss',
})
export class DesignPreview {
  private readonly snackBar = inject(MatSnackBar);

  protected readonly email = new FormControl('geen-geldig-adres', [
    Validators.required,
    Validators.email,
  ]);

  protected readonly swatches: Swatch[] = [
    { name: 'Primair', token: 'primary', on: 'on-primary' },
    { name: 'Secundair (jade)', token: 'secondary', on: 'on-secondary' },
    { name: 'Accent (goud)', token: 'accent', on: 'on-primary-container' },
    { name: 'Achtergrond', token: 'background', on: 'on-background' },
    { name: 'Oppervlak', token: 'surface', on: 'on-surface' },
    { name: 'Gedempt oppervlak', token: 'surface-muted', on: 'on-surface' },
    { name: 'Primaire container', token: 'primary-container', on: 'on-primary-container' },
    { name: 'Succes', token: 'success-container', on: 'on-success-container' },
    { name: 'Waarschuwing', token: 'warning-container', on: 'on-warning-container' },
    { name: 'Fout', token: 'error-container', on: 'on-error-container' },
    { name: 'Informatie', token: 'info-container', on: 'on-info-container' },
    { name: 'Omgekeerd', token: 'surface-inverse', on: 'on-surface-inverse' },
  ];

  constructor() {
    this.email.markAsTouched();
  }

  protected showExample(): void {
    this.snackBar.open('Dit is een voorbeeldknop in de designpreview.', undefined, {
      duration: 4000,
    });
  }
}
