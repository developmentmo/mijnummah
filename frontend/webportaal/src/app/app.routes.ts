import { isDevMode } from '@angular/core';
import { Routes } from '@angular/router';

import { authGuard, guestGuard, platformAdminGuard } from './core/auth/guards';
import { BRAND } from './core/brand/brand.generated';
import { mosqueAdminGuard, mosqueGuard } from './core/mosque/mosque-guards';
import { unsavedChangesGuard } from './shared/forms/unsaved-changes.guard';

const TITLE = `${BRAND.name} · Beheer`;
const title = (page: string) => `${page} · ${TITLE}`;

export const routes: Routes = [
  {
    path: 'inloggen',
    title: title('Inloggen'),
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/login-page').then((m) => m.LoginPage),
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./features/shell/shell').then((m) => m.Shell),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'moskeeen' },
      {
        path: 'moskeeen',
        title: title('Moskee kiezen'),
        loadComponent: () =>
          import('./features/mosques/mosque-picker-page').then((m) => m.MosquePickerPage),
      },
      {
        path: 'moskeeen/nieuw',
        title: title('Moskee toevoegen'),
        canActivate: [platformAdminGuard],
        canDeactivate: [unsavedChangesGuard],
        loadComponent: () =>
          import('./features/mosques/mosque-create-page').then((m) => m.MosqueCreatePage),
      },
      {
        path: 'accounts',
        title: title('Accounts'),
        canActivate: [platformAdminGuard],
        loadComponent: () =>
          import('./features/accounts/accounts-page').then((m) => m.AccountsPage),
      },
      {
        path: 'm/:mosqueId',
        canActivate: [mosqueGuard],
        children: [
          {
            path: '',
            title: title('Overzicht'),
            loadComponent: () =>
              import('./features/dashboard/dashboard-page').then((m) => m.DashboardPage),
          },
          {
            path: 'agenda',
            title: title('Agenda'),
            loadComponent: () => import('./features/events/events-page').then((m) => m.EventsPage),
          },
          {
            path: 'agenda/nieuw',
            title: title('Nieuwe activiteit'),
            canDeactivate: [unsavedChangesGuard],
            loadComponent: () =>
              import('./features/events/event-edit-page').then((m) => m.EventEditPage),
          },
          {
            path: 'agenda/:eventId',
            title: title('Activiteit'),
            canDeactivate: [unsavedChangesGuard],
            loadComponent: () =>
              import('./features/events/event-edit-page').then((m) => m.EventEditPage),
          },
          {
            path: 'agenda/:eventId/aanmeldingen',
            title: title('Aanmeldingen'),
            canActivate: [mosqueAdminGuard],
            loadComponent: () =>
              import('./features/events/registrations-page').then((m) => m.RegistrationsPage),
          },
          {
            path: 'media',
            title: title('Lezingen en video’s'),
            loadComponent: () => import('./features/media/media-page').then((m) => m.MediaPage),
          },
          {
            path: 'media/nieuw',
            title: title('Nieuw media-item'),
            canDeactivate: [unsavedChangesGuard],
            loadComponent: () =>
              import('./features/media/media-edit-page').then((m) => m.MediaEditPage),
          },
          {
            path: 'media/:mediaId',
            title: title('Media-item'),
            canDeactivate: [unsavedChangesGuard],
            loadComponent: () =>
              import('./features/media/media-edit-page').then((m) => m.MediaEditPage),
          },
          {
            path: 'aankondigingen',
            title: title('Aankondigingen'),
            loadComponent: () =>
              import('./features/announcements/announcements-page').then(
                (m) => m.AnnouncementsPage,
              ),
          },
          {
            path: 'aankondigingen/nieuw',
            title: title('Nieuwe aankondiging'),
            canDeactivate: [unsavedChangesGuard],
            loadComponent: () =>
              import('./features/announcements/announcement-edit-page').then(
                (m) => m.AnnouncementEditPage,
              ),
          },
          {
            path: 'aankondigingen/:announcementId',
            title: title('Aankondiging'),
            canDeactivate: [unsavedChangesGuard],
            loadComponent: () =>
              import('./features/announcements/announcement-edit-page').then(
                (m) => m.AnnouncementEditPage,
              ),
          },
          {
            path: 'gegevens',
            title: title('Moskeegegevens'),
            canActivate: [mosqueAdminGuard],
            canDeactivate: [unsavedChangesGuard],
            loadComponent: () =>
              import('./features/mosque-details/mosque-details-page').then(
                (m) => m.MosqueDetailsPage,
              ),
          },
          {
            path: 'beheerders',
            title: title('Beheerders'),
            canActivate: [mosqueAdminGuard],
            loadComponent: () =>
              import('./features/administrators/administrators-page').then(
                (m) => m.AdministratorsPage,
              ),
          },
        ],
      },
    ],
  },
  {
    // Interne designpreview uit fase 1; alleen in ontwikkelbuilds.
    path: 'design',
    title: title('Designpreview'),
    canMatch: [() => isDevMode()],
    loadComponent: () => import('./design-preview/design-preview').then((m) => m.DesignPreview),
  },
  {
    path: '**',
    title: title('Niet gevonden'),
    loadComponent: () => import('./features/shell/not-found-page').then((m) => m.NotFoundPage),
  },
];
