// Leest tokens.json (W3C Design Tokens-formaat), vult de merkkleuren aan uit brands/<slug>/brand.json en lost
// aliassen zoals "{color.palette.primary.800}" op.
// Geen externe dependencies, zodat de generator overal met alleen Node draait.

import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

import { hexToOklch, oklchToHex } from '../../tools/brand/color.mjs';
import { loadBrand, selectedBrandSlug } from '../../tools/brand/lib.mjs';

export const tokensDir = dirname(fileURLToPath(import.meta.url));
export const repoRoot = resolve(tokensDir, '..', '..');

export function loadJson(path) {
  return JSON.parse(readFileSync(path, 'utf8'));
}

const colorScale = (scale, description) =>
  Object.fromEntries(Object.entries(scale).map(([shade, hex]) => [shade, { $type: 'color', $value: hex }]).concat([['$description', description]]));

/**
 * De tokens voor een merk: tokens.json met daarin de merknaam en de paletten primary, accent en (optioneel) neutral
 * van het merk. Standaard het merk uit --brand=, BRAND of brands/index.json.
 */
export function loadTokens(path = resolve(tokensDir, 'tokens.json'), brand = loadBrand(selectedBrandSlug())) {
  const tokens = loadJson(path);
  const { $description, ...shared } = tokens.color.palette;
  tokens.brand.name = { $type: 'string', $value: brand.name };
  tokens.brand.slug = { $type: 'string', $value: brand.slug };
  // Merkpaletten vooraan, zodat de gegenereerde bestanden dezelfde volgorde houden.
  tokens.color.palette = {
    $description,
    primary: colorScale(brand.colors.primary, `Hoofdkleur van ${brand.name}`),
    accent: colorScale(brand.colors.accent, `Accentkleur van ${brand.name}, spaarzaam gebruiken`),
    ...shared,
    ...(brand.colors.neutral ? { neutral: colorScale(brand.colors.neutral, `Neutrale tinten van ${brand.name}`) } : {}),
  };
  tintDarkTones(tokens.color.dark, brand);
  return tokens;
}

/**
 * Draait de kleurtoon van de merkgebonden donkere tinten mee met het merk (zie color.dark.$extensions in
 * tokens.json). Voor het merk waarvoor ze zijn ontworpen blijven de waarden exact gelijk.
 */
function tintDarkTones(dark, brand) {
  const tint = dark.$extensions?.['nl.mijnummah.brandTint'];
  if (!tint) return;
  const brandColor = { primary: brand.colors.primary['800'], accent: brand.colors.accent['400'] };
  for (const kind of ['primary', 'accent']) {
    const designed = tint.designedFor[kind];
    if (brandColor[kind].toUpperCase() === designed.toUpperCase()) continue;
    const from = hexToOklch(designed);
    const to = hexToOklch(brandColor[kind]);
    const hueShift = to.h - from.h;
    const chromaRatio = Math.min(1.5, to.c / from.c);
    for (const name of tint[kind]) {
      const token = dark[name];
      if (!token || !/^#[0-9A-Fa-f]{6}$/.test(token.$value)) throw new Error(`brandTint: color.dark.${name} is geen #RRGGBB`);
      const color = hexToOklch(token.$value);
      token.$value = oklchToHex({ l: color.l, c: color.c * chromaRatio, h: color.h + hueShift });
    }
  }
}

/** Zoekt een token op via een pad met punten, bijvoorbeeld "color.light.primary". */
export function lookup(tokens, path) {
  const node = path.split('.').reduce((n, key) => (n == null ? undefined : n[key]), tokens);
  if (node === undefined || node.$value === undefined) {
    throw new Error(`Onbekend token: ${path}`);
  }
  return node;
}

/** Lost een waarde recursief op: aliassen worden vervangen door de waarde waar ze naar wijzen. */
export function resolveValue(tokens, value, seen = new Set()) {
  if (typeof value === 'string') {
    const alias = value.match(/^\{([^}]+)\}$/);
    if (!alias) return value;
    const path = alias[1];
    if (seen.has(path)) throw new Error(`Circulaire alias: ${[...seen, path].join(' -> ')}`);
    return resolveValue(tokens, lookup(tokens, path).$value, new Set([...seen, path]));
  }
  if (Array.isArray(value)) return value.map((v) => resolveValue(tokens, v, seen));
  if (value && typeof value === 'object') {
    return Object.fromEntries(Object.entries(value).map(([k, v]) => [k, resolveValue(tokens, v, seen)]));
  }
  return value;
}

/** Geeft [naam, token] terug voor alle directe kinderen van een groep, zonder $-sleutels. */
export function entries(group) {
  return Object.entries(group).filter(([key]) => !key.startsWith('$'));
}

/** Loopt door alle tokens heen en levert [pad, token]. */
export function* walk(node, path = []) {
  if (node && typeof node === 'object' && '$value' in node) {
    yield [path.join('.'), node];
    return;
  }
  for (const [key, child] of entries(node ?? {})) {
    yield* walk(child, [...path, key]);
  }
}

/** Valideert hex-kleuren (#RRGGBB of #RRGGBBAA) en geeft {r,g,b,a} terug. */
export function parseHex(hex) {
  const m = /^#([0-9a-fA-F]{6})([0-9a-fA-F]{2})?$/.exec(hex);
  if (!m) throw new Error(`Ongeldige kleur: ${hex}`);
  const n = parseInt(m[1], 16);
  return { r: (n >> 16) & 255, g: (n >> 8) & 255, b: n & 255, a: m[2] ? parseInt(m[2], 16) / 255 : 1 };
}
