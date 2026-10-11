import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { MosqueContext } from '../../core/mosque/mosque-context';
import { BrandLogo } from '../../shared/ui/brand-logo';
import { ThemeToggle } from '../../shared/ui/theme-toggle';

interface NavItem {
  label: string;
  path: string;
  exact?: boolean;
}

/** Vaste schil van het portaal: kopbalk met account, navigatie per moskee en het inhoudsgebied. */
@Component({
  selector: 'mu-shell',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    BrandLogo,
    MatButton,
    MatMenuModule,
    RouterLink,
    RouterLinkActive,
    RouterOutlet,
    ThemeToggle,
  ],
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
})
export class Shell {
  protected readonly auth = inject(AuthService);
  protected readonly context = inject(MosqueContext);
  private readonly router = inject(Router);

  protected readonly canSwitchMosque = computed(() => (this.context.mosques()?.length ?? 0) > 1);

  protected readonly navItems = computed<NavItem[]>(() => {
    const mosque = this.context.current();
    if (!mosque) return [];
    const base = `/m/${mosque.id}`;
    const items: NavItem[] = [
      { label: 'Overzicht', path: base, exact: true },
      { label: 'Agenda', path: `${base}/agenda` },
      { label: 'Lezingen en video’s', path: `${base}/media` },
      { label: 'Aankondigingen', path: `${base}/aankondigingen` },
    ];
    if (this.context.isAdmin()) {
      items.push(
        { label: 'Moskeegegevens', path: `${base}/gegevens` },
        { label: 'Beheerders', path: `${base}/beheerders` },
      );
    }
    return items;
  });

  protected readonly roleLabel = computed(() => {
    if (this.auth.isPlatformAdmin()) return 'Platformbeheerder';
    const role = this.context.role();
    return role === 'ADMIN' ? 'Beheerder' : role === 'EDITOR' ? 'Redacteur' : '';
  });

  protected async logout(): Promise<void> {
    await this.auth.logout();
    await this.router.navigate(['/inloggen']);
  }
}
