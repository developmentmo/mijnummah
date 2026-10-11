import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { Confirm } from '../ui/confirm-dialog';
import { unsavedChangesGuard } from './unsaved-changes.guard';

describe('unsavedChangesGuard', () => {
  const ask = vi.fn();

  beforeEach(() => {
    ask.mockReset();
    TestBed.configureTestingModule({ providers: [{ provide: Confirm, useValue: { ask } }] });
  });

  const run = (dirty: boolean) =>
    TestBed.runInInjectionContext(() =>
      unsavedChangesGuard(
        { hasUnsavedChanges: () => dirty },
        {} as ActivatedRouteSnapshot,
        {} as RouterStateSnapshot,
        {} as RouterStateSnapshot,
      ),
    );

  it('laat een formulier zonder wijzigingen direct gaan', async () => {
    expect(await run(false)).toBe(true);
    expect(ask).not.toHaveBeenCalled();
  });

  it('vraagt bevestiging bij niet-opgeslagen wijzigingen en volgt het antwoord', async () => {
    ask.mockResolvedValue(false);
    expect(await run(true)).toBe(false);
    expect(ask).toHaveBeenCalledWith(expect.objectContaining({ cancelLabel: 'Blijven' }));
    ask.mockResolvedValue(true);
    expect(await run(true)).toBe(true);
  });
});
