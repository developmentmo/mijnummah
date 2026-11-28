import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import { BRAND } from '../../core/brand/brand.generated';
import { BrandLogo } from '../../shared/ui/brand-logo';
import { ThemeToggle } from '../../shared/ui/theme-toggle';

/**
 * Concept: zolang dit true is, staat bovenaan dat de tekst nog niet is vastgesteld. Zet op false zodra de tekst met
 * de moskeeën (en een jurist) is afgestemd en alle punten tussen [haken] zijn ingevuld. Zie PRIVACY.md.
 */
export const PRIVACY_STATEMENT_IS_CONCEPT = true;

/**
 * Openbare privacyverklaring van de app en het portaal, zonder inloggen bereikbaar op /privacy. Dit adres is nodig
 * voor de App Store en Google Play en hoort in brands/mijnummah/brand.json (links.privacyPolicyUrl).
 * De inhoud volgt het gegevensoverzicht in PRIVACY.md; houd beide gelijk.
 */
@Component({
  selector: 'mu-privacy-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BrandLogo, RouterLink, ThemeToggle],
  template: `
    <main id="inhoud" class="page">
      <div class="theme"><mu-theme-toggle /></div>
      <article class="card" aria-labelledby="titel">
        <mu-brand-logo [height]="40" />
        <h1 id="titel" class="mu-headline-medium">Privacyverklaring {{ brand }}</h1>
        @if (concept) {
          <p class="concept mu-body-medium" role="note">
            Concept: deze tekst is nog niet vastgesteld. Punten tussen [haken] worden nog ingevuld.
          </p>
        }
        <p class="mu-muted mu-body-medium">Laatst bijgewerkt: [datum van vaststelling]</p>

        <section>
          <h2 class="mu-title-medium">Wie zijn wij</h2>
          <p>
            {{ brand }} is een app waarin moskeeën hun agenda, lezingen en aankondigingen delen. Het
            platform wordt beheerd door [naam rechtspersoon], [adres], KvK [nummer]. Vragen over
            privacy stuur je naar [e-mailadres voor privacy].
          </p>
          <p>
            Elke moskee bepaalt zelf wat zij in de app zet en is verantwoordelijk voor de gegevens
            van haar activiteiten, zoals de aanmeldingen. [Naam rechtspersoon] verwerkt die gegevens
            in opdracht van de moskee. Voor je account en de app zelf is [naam rechtspersoon]
            verantwoordelijk.
          </p>
        </section>

        <section>
          <h2 class="mu-title-medium">Gebruik zonder account</h2>
          <p>
            De agenda, lezingen, aankondigingen en gegevens van moskeeën kun je zonder account
            bekijken. We vragen dan niets van je en houden niet bij wat je bekijkt. De app gebruikt
            geen analytics, geen advertenties en geen trackers.
          </p>
        </section>

        <section>
          <h2 class="mu-title-medium">Welke gegevens we verwerken, en waarom</h2>
          <ul>
            <li>
              <strong>Account:</strong> je e-mailadres, naam en wachtwoord (alleen versleuteld
              opgeslagen), en wanneer je voor het laatst bent ingelogd. Nodig om in te loggen en je
              wachtwoord te herstellen.
            </li>
            <li>
              <strong>Moskeeën die je volgt</strong>, je favorieten en welke meldingen je wilt
              krijgen. Daarmee toont de app je eigen moskeeën en stuurt hij alleen de meldingen die
              je hebt gekozen. Een moskee ziet alleen hoeveel volgers zij heeft, niet wie.
            </li>
            <li><strong>Bewaarde video's:</strong> alleen jij ziet welke video's je bewaart.</li>
            <li>
              <strong>Aanmeldingen voor activiteiten:</strong> de beheerder van die moskee ziet je
              naam en e-mailadres in de aanmeldlijst.
            </li>
            <li>
              <strong>Pushmeldingen:</strong> een code van je toestel (pushtoken) en of het Android
              of iOS is. Pas nadat je toestemming geeft.
            </li>
            <li>
              <strong>Beheerders van een moskee:</strong> welke rol je hebt, en een logboek van wat
              je in het portaal doet (wanneer, welke actie, welk item).
            </li>
          </ul>
          <p>
            Dat je een moskee volgt of je aanmeldt voor een activiteit, kan iets zeggen over je
            geloof. Dat is een bijzonder persoonsgegeven. We verwerken het alleen [grondslag art. 9
            AVG, bijvoorbeeld: met je uitdrukkelijke toestemming, die je geeft door een moskee te
            volgen of je aan te melden, en die je altijd kunt intrekken door te ontvolgen of je af
            te melden].
          </p>
        </section>

        <section>
          <h2 class="mu-title-medium">Hoe lang we gegevens bewaren</h2>
          <ul>
            <li>Je account en wat je volgt: tot je je account verwijdert of ontvolgt.</li>
            <li>Aanmeldingen: tot 365 dagen na de activiteit.</li>
            <li>Pushtokens: tot je uitlogt, of na 270 dagen zonder gebruik.</li>
            <li>Het logboek van beheerders: 730 dagen.</li>
            <li>
              Back-ups: 30 dagen. Een verwijderd account verdwijnt daarna ook uit de back-ups.
            </li>
          </ul>
        </section>

        <section>
          <h2 class="mu-title-medium">Met wie we gegevens delen</h2>
          <ul>
            <li>[Hostingpartij], in [land in de EU]: de server waarop het platform draait.</li>
            <li>[E-maildienst]: voor e-mails om je wachtwoord in te stellen of te herstellen.</li>
            <li>
              Google Firebase Cloud Messaging en Apple: om pushmeldingen op je toestel af te
              leveren. Daarbij gaan je pushtoken en de tekst van de melding naar de Verenigde
              Staten, onder het EU-US Data Privacy Framework.
            </li>
            <li>
              YouTube: video's spelen af in de privacyvriendelijke modus van YouTube. Als je een
              video afspeelt, maakt je toestel verbinding met YouTube (Google).
            </li>
          </ul>
          <p>We verkopen geen gegevens en geven ze niet aan anderen voor reclame.</p>
        </section>

        <section>
          <h2 class="mu-title-medium">Je rechten</h2>
          <ul>
            <li>
              Je account verwijderen kan zelf in de app: Profiel, Account verwijderen. Dat gebeurt
              direct.
            </li>
            <li>Meldingen zet je uit in de app of in de instellingen van je toestel.</li>
            <li>
              Voor inzage, correctie of een kopie van je gegevens mail je naar [e-mailadres voor
              privacy]. Je krijgt binnen een maand antwoord.
            </li>
            <li>
              Ben je het niet eens met hoe we met je gegevens omgaan, dan kun je een klacht indienen
              bij de Autoriteit Persoonsgegevens.
            </li>
          </ul>
        </section>

        <section>
          <h2 class="mu-title-medium">Beveiliging</h2>
          <p>
            Alle verbindingen zijn versleuteld (HTTPS). Wachtwoorden en codes slaan we alleen
            versleuteld op. Een beheerder van een moskee ziet alleen de gegevens van de eigen
            moskee.
          </p>
        </section>

        <p class="mu-body-medium"><a routerLink="/inloggen">Naar het beheerportaal</a></p>
      </article>
    </main>
  `,
  styles: `
    .page {
      min-height: 100vh;
      display: grid;
      justify-items: center;
      padding: var(--mu-space-16) var(--mu-space-4) var(--mu-space-8);
    }
    .theme {
      position: absolute;
      top: var(--mu-space-4);
      right: var(--mu-space-4);
    }
    .card {
      width: 100%;
      max-width: 46rem;
      display: grid;
      gap: var(--mu-space-4);
      padding: var(--mu-space-8);
      border-radius: var(--mu-card-radius);
      background: var(--mu-color-surface);
      border: 1px solid var(--mu-color-border);
      line-height: 1.6;
    }
    section {
      display: grid;
      gap: var(--mu-space-2);
    }
    h2,
    p,
    ul {
      margin: 0;
    }
    ul {
      padding-left: var(--mu-space-5);
      display: grid;
      gap: var(--mu-space-1);
    }
    .concept {
      margin: 0;
      padding: var(--mu-space-3) var(--mu-space-4);
      border-radius: var(--mu-card-radius);
      border: 1px solid var(--mu-color-border);
      background: var(--mu-color-surface-muted);
    }
    @media (max-width: 600px) {
      .card {
        padding: var(--mu-space-5);
      }
    }
  `,
})
export class PrivacyPage {
  protected readonly brand = BRAND.name;
  protected readonly concept = PRIVACY_STATEMENT_IS_CONCEPT;
}
