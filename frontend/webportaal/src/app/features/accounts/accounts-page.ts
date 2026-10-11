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
import { MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';
import { firstValueFrom } from 'rxjs';

import { AdminApi } from '../../core/api/admin-api';
import { describeError } from '../../core/api/api-error';
import { CurrentUser, Page, PlatformRole } from '../../core/api/models';
import { AuthService } from '../../core/auth/auth.service';
import { formatDate } from '../../core/format/dates';
import { Loadable } from '../../shared/data/loadable';
import { applyServerErrors, controlError } from '../../shared/forms/form-errors';
import { Confirm } from '../../shared/ui/confirm-dialog';
import { Notifier } from '../../shared/ui/notifier';
import { PageHeader } from '../../shared/ui/page-header';
import { DUTCH_PAGINATOR } from '../../shared/ui/paginator-intl';
import { Skeleton } from '../../shared/ui/skeleton';
import { StatusBadge } from '../../shared/ui/status-badge';
import { StatusMessage } from '../../shared/ui/status-message';

const PAGE_SIZE = 20;
const MIN_PASSWORD = 12;

const ACCOUNT_ERRORS: Record<string, string> = {
  EMAIL_ALREADY_REGISTERED: 'Er bestaat al een account met dit e-mailadres.',
  INVALID_PASSWORD: `Het wachtwoord moet minstens ${MIN_PASSWORD} tekens hebben.`,
  CANNOT_CHANGE_OWN_ACCOUNT: 'Je kunt je eigen account niet blokkeren.',
  USER_NOT_FOUND: 'Dit account bestaat niet (meer).',
};

/** Platformbeheer: accounts aanmaken (bijv. voor nieuwe moskeebeheerders) en blokkeren. */
@Component({
  selector: 'mu-accounts-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [DUTCH_PAGINATOR],
  imports: [
    MatButton,
    MatFormFieldModule,
    MatInput,
    MatPaginator,
    MatSelectModule,
    PageHeader,
    ReactiveFormsModule,
    Skeleton,
    StatusBadge,
    StatusMessage,
  ],
  templateUrl: './accounts-page.html',
  styles: `
    :host,
    section {
      display: grid;
      gap: var(--mu-space-3);
    }
    :host {
      gap: var(--mu-space-8);
    }
  `,
})
export class AccountsPage implements OnInit {
  private readonly api = inject(AdminApi);
  private readonly confirm = inject(Confirm);
  private readonly notifier = inject(Notifier);
  private readonly auth = inject(AuthService);

  private readonly pageIndex = signal(0);
  protected readonly users = new Loadable<Page<CurrentUser>>(() =>
    this.api.users(this.pageIndex(), PAGE_SIZE),
  );
  protected readonly form = inject(NonNullableFormBuilder).group({
    displayName: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
    password: [
      '',
      [Validators.required, Validators.minLength(MIN_PASSWORD), Validators.maxLength(200)],
    ],
    platformRole: ['USER' as PlatformRole],
  });
  protected readonly busy = signal(false);
  protected readonly createError = signal<string | null>(null);
  protected readonly rowError = signal<string | null>(null);
  protected readonly controlError = controlError;
  protected readonly pageSize = PAGE_SIZE;
  protected readonly myUserId = computed(() => this.auth.user()?.id);

  ngOnInit(): void {
    void this.users.load();
  }

  protected goTo(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    void this.users.load();
  }

  protected async create(): Promise<void> {
    if (this.busy()) return;
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    const v = this.form.getRawValue();
    if (v.platformRole === 'PLATFORM_ADMIN') {
      const ok = await this.confirm.ask({
        title: 'Platformbeheerder aanmaken?',
        message: `${v.displayName} krijgt toegang tot alle moskeeën en alle accounts.`,
        confirmLabel: 'Platformbeheerder aanmaken',
        destructive: true,
      });
      if (!ok) return;
    }
    this.busy.set(true);
    this.createError.set(null);
    try {
      const created = await firstValueFrom(
        this.api.createUser({ ...v, email: v.email.trim(), displayName: v.displayName.trim() }),
      );
      this.form.reset({ displayName: '', email: '', password: '', platformRole: 'USER' });
      this.notifier.success(
        `Account voor ${created.displayName} aangemaakt. Geef het wachtwoord veilig door en voeg de persoon toe bij Beheerders van de moskee.`,
      );
      void this.users.load();
    } catch (error) {
      const applied = applyServerErrors(this.form, error);
      this.createError.set(
        applied ? 'Controleer de gemarkeerde velden.' : describeError(error, ACCOUNT_ERRORS),
      );
    } finally {
      this.busy.set(false);
    }
  }

  protected async toggleBlocked(user: CurrentUser): Promise<void> {
    if (this.busy()) return;
    const block = user.status === 'ACTIVE';
    const ok = await this.confirm.ask(
      block
        ? {
            title: 'Account blokkeren?',
            message: `${user.displayName} (${user.email}) kan niet meer inloggen en wordt direct overal uitgelogd.`,
            confirmLabel: 'Blokkeren',
            destructive: true,
          }
        : {
            title: 'Blokkade opheffen?',
            message: `${user.displayName} (${user.email}) kan weer inloggen.`,
            confirmLabel: 'Blokkade opheffen',
          },
    );
    if (!ok) return;
    this.busy.set(true);
    this.rowError.set(null);
    try {
      const updated = await firstValueFrom(this.api.setUserBlocked(user.id, block));
      const page = this.users.value();
      if (page) {
        this.users.set({
          ...page,
          items: page.items.map((u) => (u.id === updated.id ? updated : u)),
        });
      }
      this.notifier.success(block ? 'Account geblokkeerd.' : 'Blokkade opgeheven.');
    } catch (error) {
      this.rowError.set(describeError(error, ACCOUNT_ERRORS));
    } finally {
      this.busy.set(false);
    }
  }

  protected since(iso: string): string {
    return formatDate(iso);
  }
}
