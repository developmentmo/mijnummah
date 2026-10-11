#!/usr/bin/env node
// Controleert of alle kleurparen uit contrast-pairs.json voldoen aan het vereiste contrast (WCAG 2.2).
// Transparante kleuren worden eerst over de achtergrond van het thema gelegd.
//
//   node design/tokens/contrast.mjs            controleert en faalt bij een overtreding
//   node design/tokens/contrast.mjs --report   toont ook alle geslaagde paren

import { resolve } from 'node:path';
import { loadJson, loadTokens, lookup, parseHex, resolveValue, tokensDir } from './lib.mjs';

const tokens = loadTokens();
const { pairs } = loadJson(resolve(tokensDir, 'contrast-pairs.json'));

const channel = (c) => {
  const s = c / 255;
  return s <= 0.04045 ? s / 12.92 : ((s + 0.055) / 1.055) ** 2.4;
};
const luminance = ({ r, g, b }) => 0.2126 * channel(r) + 0.7152 * channel(g) + 0.0722 * channel(b);
const blend = (fg, bg) => ({
  r: fg.r * fg.a + bg.r * (1 - fg.a),
  g: fg.g * fg.a + bg.g * (1 - fg.a),
  b: fg.b * fg.a + bg.b * (1 - fg.a),
});

export function contrastRatio(fgHex, bgHex, baseHex = '#FFFFFF') {
  const base = parseHex(baseHex);
  const bg = blend(parseHex(bgHex), base);
  const fg = blend(parseHex(fgHex), bg);
  const [hi, lo] = [luminance(fg), luminance(bg)].sort((a, b) => b - a);
  return (hi + 0.05) / (lo + 0.05);
}

const color = (theme, name) => resolveValue(tokens, lookup(tokens, `color.${theme}.${name}`).$value);

let failures = 0;
const report = process.argv.includes('--report');
for (const pair of pairs) {
  const ratio = contrastRatio(color(pair.theme, pair.foreground), color(pair.theme, pair.background), color(pair.theme, 'background'));
  const ok = ratio >= pair.min;
  if (!ok) failures++;
  if (!ok || report) {
    console.log(
      `${ok ? 'OK  ' : 'FOUT'} ${pair.theme.padEnd(5)} ${pair.foreground} op ${pair.background}: ${ratio.toFixed(2)}:1 (minimaal ${pair.min}:1)`,
    );
  }
}
console.log(`${pairs.length - failures} van ${pairs.length} kleurparen voldoen.`);
if (failures > 0) process.exit(1);
