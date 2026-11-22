#!/usr/bin/env node
// Het merk van het platform (Mijn Ummah) toepassen en controleren. Er is één app voor alle moskeeën; een moskee
// krijgt geen eigen merk of app, haar naam, logo en omslagfoto komen uit de backend (zie design/README.md).
//
//   node tools/brand/brand.mjs list                        de merken in brands/ (alleen mijnummah)
//   node tools/brand/brand.mjs validate [slug...]          configuratie, beeldmerk en contrast controleren (standaard alle)
//   node tools/brand/brand.mjs apply [slug] [--skip-assets]
//                                                          merk toepassen op app en portaal (tokens, namen, ID's, logo's, iconen)
//   node tools/brand/brand.mjs check                       faalt als de repository niet het standaardmerk weergeeft (CI)
//   node tools/brand/brand.mjs show <slug> <veld>          één waarde, bijv. android.applicationId (voor scripts en CI)

import { spawnSync } from 'node:child_process';
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';

import { brandSlugs, crossBrandErrors, defaultBrandSlug, loadBrand, repoRoot } from './lib.mjs';

const HEADER = (slug) => `GEGENEREERD BESTAND, NIET HANDMATIG AANPASSEN. Bron: brands/${slug}/brand.json (node tools/brand/brand.mjs apply)`;

// ---------- gegenereerde bestanden per merk ----------

const dartString = (value) => `'${value.replace(/\\/g, '\\\\').replace(/'/g, "\\'").replace(/\$/g, '\\$')}'`;
const tsString = (value) => `'${value.replace(/\\/g, '\\\\').replace(/'/g, "\\'")}'`;
const htmlText = (value) => value.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

/** Bestanden die alleen van het merk afhangen (naast tokens en logo's), met hun inhoud. */
export function brandFiles(brand) {
  const dartOptional = (value) => (value ? dartString(value) : 'null');
  return {
    'frontend/app/lib/core/brand/brand.g.dart': `// ${HEADER(brand.slug)}
// ignore_for_file: unnecessary_nullable_for_final_variable_declarations

/// Het merk van de app (het platform, niet een moskee). Zie design/README.md.
abstract final class Brand {
  /// Map van het merk in brands/.
  static const slug = ${dartString(brand.slug)};

  /// Naam van de app.
  static const name = ${dartString(brand.name)};

  /// Privacyverklaring; zonder adres toont de app geen link.
  static const String? privacyPolicyUrl = ${dartOptional(brand.links.privacyPolicyUrl)};
}
`,
    // Gelezen door android/app/build.gradle.kts (UTF-8).
    'frontend/app/android/brand.properties': `# ${HEADER(brand.slug)}
applicationId=${brand.android.applicationId}
launcherName=${brand.launcherName.replace(/\\/g, '\\\\')}
`,
    // Ingevoegd door ios/Flutter/Debug.xcconfig en Release.xcconfig.
    'frontend/app/ios/Flutter/Brand.xcconfig': `// ${HEADER(brand.slug)}
BRAND_BUNDLE_IDENTIFIER = ${brand.ios.bundleId}
BRAND_DISPLAY_NAME = ${brand.launcherName}
`,
    'frontend/webportaal/src/app/core/brand/brand.generated.ts': `// ${HEADER(brand.slug)}

/** Het merk van het platform. Zie design/README.md. */
export const BRAND = {
  slug: ${tsString(brand.slug)},
  name: ${tsString(brand.name)},
} as const;
`,
  };
}

const INDEX_HTML = 'frontend/webportaal/src/index.html';
const indexHtml = (brand, current) => current.replace(/<title>[^<]*<\/title>/, `<title>${htmlText(brand.name)} · Beheer</title>`);

// ---------- hulpfuncties ----------

function run(command, args, { env = {} } = {}) {
  const result = spawnSync(command, args, { cwd: repoRoot, stdio: 'inherit', env: { ...process.env, ...env } });
  if (result.error) throw result.error;
  return result.status === 0;
}

function fail(message) {
  console.error(message);
  process.exit(1);
}

const positional = () => process.argv.slice(3).filter((a) => !a.startsWith('--'));

// ---------- commando's ----------

function validate(slugs) {
  const brands = [];
  let ok = true;
  for (const slug of slugs) {
    try {
      brands.push(loadBrand(slug));
    } catch (e) {
      console.error(e.message);
      ok = false;
      continue;
    }
    console.log(`Contrast ${slug}:`);
    ok = run('node', ['design/tokens/contrast.mjs', `--brand=${slug}`]) && ok;
  }
  const cross = crossBrandErrors(brands);
  for (const error of cross) console.error(`Dubbel: ${error}`);
  if (!ok || cross.length) fail('Validatie mislukt.');
  console.log(`Geldig: ${slugs.join(', ')}`);
}

function apply(slug, { skipAssets }) {
  const brand = loadBrand(slug);
  for (const [path, content] of Object.entries(brandFiles(brand))) {
    const file = resolve(repoRoot, path);
    mkdirSync(dirname(file), { recursive: true });
    writeFileSync(file, content);
    console.log(`Geschreven: ${path}`);
  }
  const index = resolve(repoRoot, INDEX_HTML);
  writeFileSync(index, indexHtml(brand, readFileSync(index, 'utf8')));
  console.log(`Bijgewerkt: ${INDEX_HTML}`);
  if (!run('node', ['design/tokens/build.mjs', `--brand=${slug}`])) fail('Tokens genereren mislukt.');
  if (!skipAssets && !run('python3', ['design/branding/build_branding.py', `--brand=${slug}`])) {
    fail('Logo\'s en iconen genereren mislukt (vereist: pip install -r design/requirements.txt).');
  }
  console.log(`Merk ${slug} toegepast${skipAssets ? ' (zonder logo\'s en iconen)' : ''}.`);
}

function check() {
  const slug = defaultBrandSlug();
  const brand = loadBrand(slug);
  const stale = [];
  for (const [path, content] of Object.entries(brandFiles(brand))) {
    const file = resolve(repoRoot, path);
    if (!existsSync(file) || readFileSync(file, 'utf8') !== content) stale.push(path);
  }
  const index = readFileSync(resolve(repoRoot, INDEX_HTML), 'utf8');
  if (indexHtml(brand, index) !== index) stale.push(INDEX_HTML);
  for (const path of stale) console.error(`Niet actueel voor merk ${slug}: ${path}`);
  const tokensOk = run('node', ['design/tokens/build.mjs', `--brand=${slug}`, '--check']);
  const assetsOk = run('python3', ['design/branding/build_branding.py', `--brand=${slug}`, '--check']);
  if (stale.length || !tokensOk || !assetsOk) {
    fail(`De repository geeft merk ${slug} niet weer. Draai: node tools/brand/brand.mjs apply ${slug}`);
  }
  console.log(`Gegenereerde bestanden zijn actueel voor merk ${slug}.`);
}

function show(slug, field) {
  if (!slug || !field) fail('Gebruik: brand.mjs show <slug> <veld>');
  const value = field.split('.').reduce((node, key) => (node == null ? undefined : node[key]), loadBrand(slug));
  if (value === undefined || typeof value === 'object') fail(`Geen waarde voor ${field} in merk ${slug}`);
  console.log(value);
}

// ---------- start ----------

const command = process.argv[2];
try {
  switch (command) {
    case 'list':
      for (const slug of brandSlugs()) console.log(slug === defaultBrandSlug() ? `${slug} (standaard)` : slug);
      break;
    case 'validate':
      validate(positional().length ? positional() : brandSlugs());
      break;
    case 'apply':
      apply(positional()[0] ?? defaultBrandSlug(), { skipAssets: process.argv.includes('--skip-assets') });
      break;
    case 'check':
      check();
      break;
    case 'show':
      show(...positional());
      break;
    default:
      fail(readFileSync(new URL(import.meta.url), 'utf8').split('\n').slice(1, 10).map((l) => l.replace(/^\/\/ ?/, '')).join('\n'));
  }
} catch (e) {
  fail(e.message);
}
