import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { MatSelect, MatSelectModule } from '@angular/material/select';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { AdminApi } from '../../core/api/admin-api';
import { describeError } from '../../core/api/api-error';
import { Administrator, MosqueRole } from '../../core/api/models';
import { AuthService } from '../../core/auth/auth.service';
import { formatDate } from '../../core/format/dates';
import { MOSQUE_ROLE_DESCRIPTIONS, MOSQUE_ROLE_LABELS, options } from '../../core/format/labels';
import { MosqueContext } from '../../core/mosque/mosque-context';
import { Loadable } from '../../shared/data/loadable';
import { controlError } from '../../shared/forms/form-errors';
import { Confirm } from '../../shared/ui/confirm-dialog';
import { Notifier } from '../../shared/ui/notifier';
import { PageHeader } from '../../shared/ui/page-header';
import { Skeleton } from '../../shared/ui/skeleton';
import { StatusMessage } from '../../shared/ui/status-message';

const ADMIN_ERRORS: Record<string, string> = {
  USER_NOT_FOUND:
    'Er is geen actief account met dit e-mailadres. Een platformbeheerder kan het account aanmaken.',
  ADMINISTRATOR_ALREADY_EXISTS: 'Dit account heeft al een rol bij deze moskee.',
  LAST_ADMIN: 'Er moet altijd minstens één Beheerder overblijven.',
  ADMINISTRATOR_NOT_FOUND: 'Deze persoon heeft geen rol (meer) bij deze moskee.',
};

/** Wie deze moskee mag beheren. Alleen voor de rol Beheerder. */
@Component({
  selector: 'mu-administrators-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    MatButton,
    MatFormFieldModule,
    MatInput,
    MatSelectModule,
    PageHeader,
    ReactiveFormsModule,
    Skeleton,
    StatusMessage,
  ],
  templateUrl: './administrators-page.html',
  styles: `
    :host,
    section {
      display: grid;
      gap: var(--mu-space-3);
    }
    :host {
      gap: var(--mu-space-8);
    }
    .add {
      display: flex;
      flex-wrap: wrap;
      align-items: flex-start;
      gap: var(--mu-space-3);
    }
    .add mat-form-field:first-child {
      flex: 1 1 calc(var(--mu-space-16) * 4);
    }
    .add button {
      margin-top: var(--mu-space-2);
    }
    .roles {
      margin: 0;
      padding-left: var(--mu-space-5);
    }
  `,
})
export class AdministratorsPage implements OnInit {
  private readonly api = inject(AdminApi);
  private readonly confirm = inject(Confirm);
  private readonly notifier = inject(Notifier);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly context = inject(MosqueContext);
  private readonly mosqueId = this.context.current()!.id;

  protected readonly administrators = new Loadable<Administrator[]>(() =>
    this.api.administrators(this.mosqueId),
  );
  protected readonly form = inject(NonNullableFormBuilder).group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
    role: ['EDITOR' as MosqueRole, Validators.required],
  });
  protected readonly busy = signal(false);
  protected readonly addError = signal<string | null>(null);
  protected readonly rowError = signal<string | null>(null);
  protected readonly roles = options(MOSQUE_ROLE_LABELS);
  protected readonly roleLabels = MOSQUE_ROLE_LABELS;
  protected readonly roleDescriptions = MOSQUE_ROLE_DESCRIPTIONS;
  protected readonly controlError = controlError;
  protected readonly myUserId = computed(() => this.auth.user()?.id);

  ngOnInit(): void {
    void this.administrators.load();
  }

  protected async add(): Promise<void> {
    if (this.busy()) return;
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    this.busy.set(true);
    this.addError.set(null);
    const { email, role } = this.form.getRawValue();
    try {
      const added = await firstValueFrom(
        this.api.addAdministrator(this.mosqueId, email.trim(), role),
      );
      this.administrators.set([...(this.administrators.value() ?? []), added]);
      this.form.reset({ email: '', role: 'EDITOR' });
      this.notifier.success(
        `${added.displayName} is toegevoegd als ${MOSQUE_ROLE_LABELS[added.role]}.`,
      );
    } catch (error) {
      this.addError.set(describeError(error, ADMIN_ERRORS));
    } finally {
      this.busy.set(false);
    }
  }

  protected async changeRole(
    admin: Administrator,
    role: MosqueRole,
    select: MatSelect,
  ): Promise<void> {
    if (role === admin.role || this.busy()) return;
    const self = admin.userId === this.myUserId();
    const ok = await this.confirm.ask({
      title: 'Rol wijzigen?',
      message: `${admin.displayName} wordt ${MOSQUE_ROLE_LABELS[role]}: ${MOSQUE_ROLE_DESCRIPTIONS[role].toLowerCase()}.`,
      note:
        self && role === 'EDITOR'
          ? 'Je verliest zelf de rechten om beheerders en moskeegegevens te beheren.'
          : undefined,
      confirmLabel: 'Rol wijzigen',
      destructive: self,
    });
    if (!ok) {
      select.value = admin.role;
      return;
    }
    const failed = await this.mutate(async () => {
      const updated = await firstValueFrom(
        this.api.changeAdministratorRole(this.mosqueId, admin.id, role),
      );
      this.administrators.set(
        (this.administrators.value() ?? []).map((a) => (a.id === updated.id ? updated : a)),
      );
      this.notifier.success(`${updated.displayName} is nu ${MOSQUE_ROLE_LABELS[updated.role]}.`);
      if (self) {
        await this.context.load(true);
        await this.context.select(this.mosqueId);
        if (!this.context.isAdmin()) await this.router.navigate(['/m', this.mosqueId]);
      }
    });
    if (failed) select.value = admin.role;
  }

  protected async remove(admin: Administrator): Promise<void> {
    if (this.busy()) return;
    const self = admin.userId === this.myUserId();
    const ok = await this.confirm.ask({
      title: 'Rechten intrekken?',
      message: `${admin.displayName} (${admin.email}) kan deze moskee daarna niet meer beheren. Het account zelf blijft bestaan.`,
      note: self
        ? 'Dit is je eigen account: je verliest direct de toegang tot deze moskee.'
        : undefined,
      confirmLabel: 'Rechten intrekken',
      destructive: true,
    });
    if (!ok) return;
    await this.mutate(async () => {
      await firstValueFrom(this.api.removeAdministrator(this.mosqueId, admin.id));
      this.administrators.set((this.administrators.value() ?? []).filter((a) => a.id !== admin.id));
      this.notifier.success(`De rechten van ${admin.displayName} zijn ingetrokken.`);
      if (self && !this.auth.isPlatformAdmin()) {
        await this.context.load(true);
        await this.router.navigate(['/moskeeen'], { queryParams: { kies: 1 } });
      }
    });
  }

  protected since(iso: string): string {
    return formatDate(iso, this.context.timeZone());
  }

  /** Voert een wijziging uit; geeft true terug als die mislukte. */
  private async mutate(work: () => Promise<void>): Promise<boolean> {
    this.busy.set(true);
    this.rowError.set(null);
    try {
      await work();
      return false;
    } catch (error) {
      this.rowError.set(describeError(error, ADMIN_ERRORS));
      void this.administrators.load();
      return true;
    } finally {
      this.busy.set(false);
    }
  }
}
