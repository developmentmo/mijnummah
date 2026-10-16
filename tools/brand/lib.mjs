// Merkconfiguraties in brands/<slug>/: laden, valideren en opsommen. Geen externe dependencies.
//
// De validator ondersteunt precies het deel van JSON Schema dat brands/brand.schema.json gebruikt (type, required,
// properties, additionalProperties: false, pattern, minLength, maxLength en $ref naar #/$defs). Een onbekend
// sleutelwoord in het schema is een fout, zodat het schema nooit stilzwijgend strenger lijkt dan de controle.

import { existsSync, readdirSync, readFileSync, statSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

export const repoRoot = resolve(dirname(fileURLToPath(import.meta.url)), '..', '..');
export const brandsDir = resolve(repoRoot, 'brands');

const readJson = (path) => JSON.parse(readFileSync(path, 'utf8'));
const schema = () => readJson(resolve(brandsDir, 'brand.schema.json'));

/** Het merk dat in de repository is toegepast; CI controleert de gegenereerde bestanden hiertegen. */
export function defaultBrandSlug() {
  return readJson(resolve(brandsDir, 'index.json')).default;
}

/** Alle merken: elke map in brands/ met een brand.json. */
export function brandSlugs() {
  return readdirSync(brandsDir)
    .filter((name) => statSync(resolve(brandsDir, name)).isDirectory())
    .filter((name) => existsSync(resolve(brandsDir, name, 'brand.json')))
    .sort();
}

/** Het merk uit --brand=<slug>, de omgevingsvariabele BRAND, of het standaardmerk. */
export function selectedBrandSlug(argv = process.argv) {
  const flag = argv.find((a) => a.startsWith('--brand='));
  return flag ? flag.slice('--brand='.length) : process.env.BRAND || defaultBrandSlug();
}

const KNOWN_KEYWORDS = new Set([
  '$schema', '$id', '$defs', '$ref', 'title', 'description', 'type', 'required', 'properties',
  'additionalProperties', 'pattern', 'minLength', 'maxLength',
]);

/** Controleert een waarde tegen (een deel van) het schema; geeft een lijst met Nederlandse foutmeldingen. */
export function validateAgainstSchema(value, node, root = node, path = '') {
  const where = path || '(hoofdniveau)';
  for (const key of Object.keys(node)) {
    if (!KNOWN_KEYWORDS.has(key)) throw new Error(`Schema gebruikt een niet-ondersteund sleutelwoord: ${key}`);
  }
  if (node.$ref) {
    const target = node.$ref.match(/^#\/\$defs\/(.+)$/);
    if (!target || !root.$defs?.[target[1]]) throw new Error(`Onbekende $ref in schema: ${node.$ref}`);
    return validateAgainstSchema(value, root.$defs[target[1]], root, path);
  }
  const errors = [];
  if (node.type === 'object') {
    if (value === null || typeof value !== 'object' || Array.isArray(value)) return [`${where}: moet een object zijn`];
    for (const key of node.required ?? []) {
      if (!(key in value)) errors.push(`${where}: "${key}" ontbreekt`);
    }
    for (const [key, child] of Object.entries(value)) {
      const childSchema = node.properties?.[key];
      if (childSchema) errors.push(...validateAgainstSchema(child, childSchema, root, path ? `${path}.${key}` : key));
      else if (node.additionalProperties === false) errors.push(`${where}: onbekend veld "${key}"`);
    }
  } else if (node.type === 'string') {
    if (typeof value !== 'string') return [`${where}: moet tekst zijn`];
    if (node.minLength !== undefined && value.length < node.minLength) errors.push(`${where}: minimaal ${node.minLength} tekens`);
    if (node.maxLength !== undefined && value.length > node.maxLength) errors.push(`${where}: hoogstens ${node.maxLength} tekens`);
    if (node.pattern && !new RegExp(node.pattern).test(value)) errors.push(`${where}: "${value}" heeft niet het juiste formaat`);
  } else if (node.type !== undefined) {
    throw new Error(`Schema gebruikt een niet-ondersteund type: ${node.type}`);
  }
  return errors;
}

/**
 * Controleert het beeldmerk: een eenvoudige, zelfstandige SVG die de generator per variant kan herkleuren.
 * Alleen primary.800 en accent.400 als kleur, zodat lichte, donkere en monochrome varianten kloppen.
 */
export function validateMark(svgText, brand) {
  const errors = [];
  if (!/^<svg[^>]*\sviewBox="0 0 96 96"/.test(svgText.trim())) errors.push('beeldmerk: moet beginnen met <svg ... viewBox="0 0 96 96">');
  for (const forbidden of ['<script', '<text', '<image', '<foreignObject', '<style', 'href=', 'url(', '@import']) {
    if (svgText.includes(forbidden)) errors.push(`beeldmerk: "${forbidden}" is niet toegestaan`);
  }
  if (/\son[a-z]+=/i.test(svgText)) errors.push('beeldmerk: event-attributen (on...) zijn niet toegestaan');
  const allowed = new Set([brand.colors.primary['800'].toUpperCase(), brand.colors.accent['400'].toUpperCase()]);
  const colors = [...svgText.matchAll(/(?:fill|stroke|stop-color|color)="([^"]+)"/g)].map((m) => m[1]);
  for (const color of colors) {
    if (color === 'none') continue;
    if (!allowed.has(color.toUpperCase())) {
      errors.push(`beeldmerk: kleur ${color} is niet toegestaan; gebruik primary.800 (${brand.colors.primary['800']}) of accent.400 (${brand.colors.accent['400']})`);
    }
  }
  if (!colors.some((c) => c.toUpperCase() === brand.colors.primary['800'].toUpperCase())) {
    errors.push('beeldmerk: gebruikt primary.800 nergens');
  }
  if (!markBody(svgText).trim()) errors.push('beeldmerk: bevat geen vormen');
  return errors;
}

/** De inhoud van het beeldmerk zonder de <svg>-omhulling, commentaar en regeleinden. */
export function markBody(svgText) {
  return svgText
    .replace(/<!--[\s\S]*?-->/g, '')
    .replace(/^[\s\S]*?<svg[^>]*>/, '')
    .replace(/<\/svg>[\s\S]*$/, '')
    .split('\n')
    .map((line) => line.trim())
    .join('');
}

/** Laadt en valideert een merk. Gooit een fout met alle problemen tegelijk. */
export function loadBrand(slug) {
  const dir = resolve(brandsDir, slug);
  const file = resolve(dir, 'brand.json');
  if (!/^[a-z0-9]+(-[a-z0-9]+)*$/.test(slug) || !existsSync(file)) {
    throw new Error(`Merk "${slug}" bestaat niet (verwacht brands/${slug}/brand.json)`);
  }
  let brand;
  try {
    brand = readJson(file);
  } catch (e) {
    throw new Error(`brands/${slug}/brand.json is geen geldige JSON: ${e.message}`);
  }
  const errors = validateAgainstSchema(brand, schema());
  if (errors.length === 0) {
    if (brand.slug !== slug) errors.push(`slug "${brand.slug}" moet gelijk zijn aan de mapnaam "${slug}"`);
    const markFile = resolve(dir, brand.logo.mark);
    if (!existsSync(markFile)) errors.push(`beeldmerk brands/${slug}/${brand.logo.mark} ontbreekt`);
    else errors.push(...validateMark(readFileSync(markFile, 'utf8'), brand));
  }
  if (errors.length) {
    throw new Error(`brands/${slug}/brand.json is ongeldig:\n  - ${errors.join('\n  - ')}`);
  }
  return {
    ...brand,
    launcherName: brand.launcherName ?? brand.name,
    links: brand.links ?? {},
    backend: brand.backend ?? {},
    markSvg: readFileSync(resolve(dir, brand.logo.mark), 'utf8'),
  };
}

/** Problemen die alleen tussen merken samen bestaan: dubbele ID's. */
export function crossBrandErrors(brands) {
  const errors = [];
  for (const key of ['android.applicationId', 'ios.bundleId', 'name']) {
    const seen = new Map();
    for (const brand of brands) {
      const value = key.split('.').reduce((n, k) => n[k], brand);
      if (seen.has(value)) errors.push(`${key} "${value}" komt voor bij ${seen.get(value)} en ${brand.slug}`);
      seen.set(value, brand.slug);
    }
  }
  return errors;
}
