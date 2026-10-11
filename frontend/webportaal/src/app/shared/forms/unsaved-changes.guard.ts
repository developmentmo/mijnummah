import { Injector, inject } from '@angular/core';
import { CanDeactivateFn } from '@angular/router';

export interface HasUnsavedChanges {
  hasUnsavedChanges(): boolean;
}

/**
 * Vraagt bevestiging voordat een formulier met niet-opgeslagen wijzigingen verlaten wordt. Het dialoogvenster
 * wordt pas geladen als het nodig is, zodat het niet in de startbundel zit.
 */
export const unsavedChangesGuard: CanDeactivateFn<HasUnsavedChanges> = async (component) => {
  if (!component.hasUnsavedChanges()) return true;
  const injector = inject(Injector);
  const { Confirm } = await import('../ui/confirm-dialog');
  return injector.get(Confirm).ask({
    title: 'Wijzigingen niet opgeslagen',
    message: 'Je hebt wijzigingen die nog niet zijn opgeslagen. Wil je deze pagina verlaten?',
    confirmLabel: 'Pagina verlaten',
    cancelLabel: 'Blijven',
    destructive: true,
  });
};
