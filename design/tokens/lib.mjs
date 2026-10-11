// Leest tokens.json (W3C Design Tokens-formaat) en lost aliassen zoals "{color.palette.green.800}" op.
// Geen externe dependencies, zodat de generator overal met alleen Node draait.

import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

export const tokensDir = dirname(fileURLToPath(import.meta.url));
export const repoRoot = resolve(tokensDir, '..', '..');

export function loadJson(path) {
  return JSON.parse(readFileSync(path, 'utf8'));
}

export function loadTokens(path = resolve(tokensDir, 'tokens.json')) {
  return loadJson(path);
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
