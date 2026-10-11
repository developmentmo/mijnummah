import { Routes } from '@angular/router';

export const routes: Routes = [
  // Fase 1: alleen de interne designpreview. Het beheerportaal volgt in fase 4.
  {
    path: '',
    title: 'Designpreview · Noeroel Islam',
    loadComponent: () => import('./design-preview/design-preview').then((m) => m.DesignPreview),
  },
  { path: '**', redirectTo: '' },
];
