import { TestBed } from '@angular/core/testing';
import { describe, expect, it, vi } from 'vitest';

import { StatusKind, StatusMessage } from './status-message';

describe('StatusMessage', () => {
  function render(kind: StatusKind, actionLabel?: string) {
    const fixture = TestBed.createComponent(StatusMessage);
    fixture.componentRef.setInput('kind', kind);
    fixture.componentRef.setInput('message', 'Opslaan is mislukt.');
    if (actionLabel) fixture.componentRef.setInput('actionLabel', actionLabel);
    fixture.detectChanges();
    return fixture;
  }

  it('maakt foutmeldingen direct voorleesbaar met role="alert"', () => {
    const el: HTMLElement = render('error').nativeElement;
    expect(el.getAttribute('role')).toBe('alert');
    expect(el.textContent).toContain('Fout:');
    expect(el.classList).toContain('mu-status--error');
  });

  it('gebruikt role="status" voor andere meldingen', () => {
    const el: HTMLElement = render('success').nativeElement;
    expect(el.getAttribute('role')).toBe('status');
    expect(el.textContent).toContain('Gelukt:');
  });

  it('toont alleen een actieknop als er een label is, en meldt een klik', () => {
    expect(render('info').nativeElement.querySelector('button')).toBeNull();

    const fixture = render('error', 'Opnieuw proberen');
    const spy = vi.fn();
    fixture.componentInstance.action.subscribe(spy);
    const button: HTMLButtonElement = fixture.nativeElement.querySelector('button');
    expect(button.textContent?.trim()).toBe('Opnieuw proberen');
    button.click();
    expect(spy).toHaveBeenCalledOnce();
  });
});
