#!/usr/bin/env node
// Genereert de design tokens voor Flutter (Dart) en het webportaal (CSS) uit tokens.json.
//
//   node design/tokens/build.mjs           schrijft de gegenereerde bestanden
//   node design/tokens/build.mjs --check   faalt als de gegenereerde bestanden niet actueel zijn (CI)

import { mkdirSync, readFileSync, writeFileSync, existsSync } from 'node:fs';
import { dirname, relative, resolve } from 'node:path';
import { entries, loadTokens, parseHex, repoRoot, resolveValue, walk } from './lib.mjs';

const tokens = loadTokens();
const r = (value) => resolveValue(tokens, value);

export const outputs = {
  dart: resolve(repoRoot, 'frontend/app/lib/core/design/tokens.g.dart'),
  css: resolve(repoRoot, 'frontend/webportaal/src/styles/tokens.generated.css'),
};

const HEADER_NOTE = 'GEGENEREERD BESTAND, NIET HANDMATIG AANPASSEN. Bron: design/tokens/tokens.json';

// ---------- validatie ----------

function validate() {
  for (const [path, token] of walk(tokens)) {
    const value = r(token.$value);
    switch (token.$type) {
      case 'color':
        parseHex(value);
        break;
      case 'dimension':
        if (typeof value?.value !== 'number' || value.unit !== 'px') throw new Error(`${path}: dimension moet {value, unit: "px"} zijn`);
        break;
      case 'typography':
        for (const key of ['fontFamily', 'fontWeight', 'fontSize', 'lineHeight', 'letterSpacing']) {
          if (value[key] === undefined) throw new Error(`${path}: typography mist ${key}`);
        }
        break;
      default:
        break;
    }
  }
  const light = Object.keys(tokens.color.light).filter((k) => !k.startsWith('$')).sort();
  const dark = Object.keys(tokens.color.dark).filter((k) => !k.startsWith('$')).sort();
  if (JSON.stringify(light) !== JSON.stringify(dark)) {
    const missing = light.filter((k) => !dark.includes(k)).concat(dark.filter((k) => !light.includes(k)));
    throw new Error(`Licht- en donkerthema hebben niet dezelfde kleuren: ${missing.join(', ')}`);
  }
}

// ---------- helpers ----------

const camel = (...parts) =>
  parts
    .join('-')
    .split(/[-_.\s]+/)
    .filter(Boolean)
    .map((p, i) => (i === 0 ? p.charAt(0).toLowerCase() + p.slice(1) : p.charAt(0).toUpperCase() + p.slice(1)))
    .join('');
const dartName = (...parts) => {
  const name = camel(...parts);
  return /^[0-9]/.test(name) ? `v${name}` : name;
};
const kebab = (...parts) =>
  parts
    .join('-')
    .replace(/([a-z0-9])([A-Z])/g, '$1-$2')
    .toLowerCase();

const dartColor = (hex) => {
  const { r: red, g, b, a } = parseHex(hex);
  const argb = (Math.round(a * 255) << 24) | (red << 16) | (g << 8) | b;
  return `Color(0x${(argb >>> 0).toString(16).toUpperCase().padStart(8, '0')})`;
};
const num = (n) => (Number.isInteger(n) ? `${n}.0` : `${n}`);
const px = (d) => d.value;
const cssColor = (hex) => hex.toLowerCase();

// ---------- Dart ----------

function buildDart() {
  const out = [];
  const w = (line = '') => out.push(line);
  w(`// ${HEADER_NOTE}`);
  w('// ignore_for_file: public_member_api_docs');
  w();
  w("import 'package:flutter/material.dart';");
  w();

  w('/// Primitieve kleuren. Gebruik in widgets de semantische kleuren via `AppColors`.');
  w('abstract final class TokPalette {');
  for (const [group, shades] of entries(tokens.color.palette)) {
    for (const [shade, token] of entries(shades)) {
      if (token.$description) w(`  /// ${token.$description}`);
      w(`  static const ${dartName(group, shade)} = ${dartColor(r(token.$value))};`);
    }
  }
  w('}');
  w();

  for (const theme of ['light', 'dark']) {
    const cls = theme === 'light' ? 'TokLight' : 'TokDark';
    w(`/// Semantische kleuren voor het ${theme === 'light' ? 'lichte' : 'donkere'} thema.`);
    w(`abstract final class ${cls} {`);
    for (const [name, token] of entries(tokens.color[theme])) {
      w(`  static const ${dartName(name)} = ${dartColor(r(token.$value))};`);
    }
    w('}');
    w();
  }

  // Semantische kleuren als ThemeExtension, zodat widgets ze via het thema opvragen en licht/donker vanzelf werkt.
  const names = entries(tokens.color.light).map(([name]) => dartName(name));
  w('/// Semantische kleuren van het actieve thema: `Theme.of(context).extension<AppColors>()!` of `context.colors`.');
  w('@immutable');
  w('class AppColors extends ThemeExtension<AppColors> {');
  w('  const AppColors({');
  for (const n of names) w(`    required this.${n},`);
  w('  });');
  w();
  for (const n of names) w(`  final Color ${n};`);
  w();
  for (const [theme, cls] of [['light', 'TokLight'], ['dark', 'TokDark']]) {
    w(`  static const ${theme} = AppColors(`);
    for (const n of names) w(`    ${n}: ${cls}.${n},`);
    w('  );');
    w();
  }
  w('  @override');
  w('  AppColors copyWith({');
  for (const n of names) w(`    Color? ${n},`);
  w('  }) {');
  w('    return AppColors(');
  for (const n of names) w(`      ${n}: ${n} ?? this.${n},`);
  w('    );');
  w('  }');
  w();
  w('  @override');
  w('  AppColors lerp(AppColors? other, double t) {');
  w('    if (other == null) return this;');
  w('    return AppColors(');
  for (const n of names) w(`      ${n}: Color.lerp(${n}, other.${n}, t)!,`);
  w('    );');
  w('  }');
  w('}');
  w();

  w('/// Lettertypefamilies zoals geregistreerd in pubspec.yaml.');
  w('abstract final class TokFont {');
  for (const [name, token] of entries(tokens.font.family)) {
    w(`  static const ${dartName(name)} = '${r(token.$value)[0]}';`);
  }
  w('}');
  w();

  w('/// Tekststijlen zonder kleur; de kleur komt uit het thema.');
  w('abstract final class TokType {');
  for (const [name, token] of entries(tokens.typography)) {
    const v = r(token.$value);
    w(`  static const ${dartName(name)} = TextStyle(`);
    w(`    fontFamily: '${v.fontFamily[0]}',`);
    w(`    fontWeight: FontWeight.w${v.fontWeight},`);
    w(`    fontSize: ${num(v.fontSize.value)},`);
    w(`    height: ${num(v.lineHeight)},`);
    w(`    letterSpacing: ${num(v.letterSpacing.value)},`);
    w('  );');
  }
  w('}');
  w();

  const dimensionClass = (cls, doc, group, prefix = []) => {
    w(`/// ${doc}`);
    w(`abstract final class ${cls} {`);
    for (const [name, token] of entries(group)) {
      w(`  static const ${dartName(...prefix, name)} = ${num(px(r(token.$value)))};`);
    }
    w('}');
    w();
  };
  dimensionClass('TokSpace', 'Afstanden (logische pixels). Stap 1 = 4.', tokens.space, ['s']);
  dimensionClass('TokRadius', 'Afrondingen van hoeken.', tokens.radius);
  dimensionClass('TokIconSize', 'Standaardmaten voor iconen.', tokens.size.icon);

  w('/// Overige maten.');
  w('abstract final class TokSize {');
  w(`  static const touchTarget = ${num(px(r(tokens.size.touchTarget.$value)))};`);
  w(`  static const contentMaxWidth = ${num(px(r(tokens.size.contentMaxWidth.$value)))};`);
  w('}');
  w();

  w('/// Schaduwen.');
  w('abstract final class TokShadow {');
  for (const [name, token] of entries(tokens.shadow)) {
    const layers = r(token.$value);
    w(`  static const ${dartName(name)} = <BoxShadow>[`);
    for (const s of layers) {
      w(
        `    BoxShadow(color: ${dartColor(s.color)}, offset: Offset(${num(s.offsetX.value)}, ${num(s.offsetY.value)}), blurRadius: ${num(s.blur.value)}, spreadRadius: ${num(s.spread.value)}),`,
      );
    }
    w('  ];');
  }
  w('}');
  w();

  w('/// Animatieduur en easing.');
  w('abstract final class TokMotion {');
  for (const [name, token] of entries(tokens.motion.duration)) {
    w(`  static const ${dartName(name)} = Duration(milliseconds: ${r(token.$value).value});`);
  }
  for (const [name, token] of entries(tokens.motion.easing)) {
    const [a, b, c, d] = r(token.$value);
    w(`  static const ${dartName(name)} = Cubic(${num(a)}, ${num(b)}, ${num(c)}, ${num(d)});`);
  }
  w('}');
  w();

  w('/// Componentmaten.');
  w('abstract final class TokComponent {');
  for (const [component, group] of entries(tokens.component)) {
    for (const [name, token] of entries(group)) {
      w(`  static const ${dartName(component, name)} = ${num(px(r(token.$value)))};`);
    }
  }
  w('}');
  return out.join('\n') + '\n';
}

// ---------- CSS ----------

function buildCss() {
  const prefix = r(tokens.brand.tokenPrefix.$value);
  const v = (...parts) => `--${prefix}-${kebab(...parts)}`;
  const out = [];
  const w = (line = '') => out.push(line);

  w(`/* ${HEADER_NOTE} */`);
  w();
  w(':root {');
  for (const [group, shades] of entries(tokens.color.palette)) {
    for (const [shade, token] of entries(shades)) w(`  ${v('palette', group, shade)}: ${cssColor(r(token.$value))};`);
  }
  for (const [name, token] of entries(tokens.font.family)) {
    w(`  ${v('font', name)}: ${r(token.$value).map((f) => (f.includes(' ') ? `'${f}'` : f)).join(', ')};`);
  }
  for (const [name, token] of entries(tokens.font.weight)) w(`  ${v('font-weight', name)}: ${r(token.$value)};`);
  for (const [name, token] of entries(tokens.typography)) {
    const t = r(token.$value);
    w(`  ${v('type', name, 'size')}: ${t.fontSize.value / 16}rem;`);
    w(`  ${v('type', name, 'line-height')}: ${t.lineHeight};`);
    w(`  ${v('type', name, 'weight')}: ${t.fontWeight};`);
    w(`  ${v('type', name, 'letter-spacing')}: ${t.letterSpacing.value}px;`);
    w(`  ${v('type', name, 'family')}: var(${v('font', name.startsWith('display') || name.startsWith('headline') ? 'display' : name === 'arabic' ? 'arabic' : 'body')});`);
  }
  for (const [name, token] of entries(tokens.space)) w(`  ${v('space', name)}: ${px(r(token.$value))}px;`);
  for (const [name, token] of entries(tokens.radius)) w(`  ${v('radius', name)}: ${px(r(token.$value))}px;`);
  for (const [name, token] of entries(tokens.size.icon)) w(`  ${v('icon', name)}: ${px(r(token.$value))}px;`);
  w(`  ${v('touch-target')}: ${px(r(tokens.size.touchTarget.$value))}px;`);
  w(`  ${v('content-max-width')}: ${px(r(tokens.size.contentMaxWidth.$value))}px;`);
  for (const [name, token] of entries(tokens.shadow)) {
    const layers = r(token.$value)
      .map((s) => `${s.offsetX.value}px ${s.offsetY.value}px ${s.blur.value}px ${s.spread.value}px ${cssColor(s.color)}`)
      .join(', ');
    w(`  ${v('shadow', name)}: ${layers};`);
  }
  for (const [name, token] of entries(tokens.motion.duration)) w(`  ${v('duration', name)}: ${r(token.$value).value}ms;`);
  for (const [name, token] of entries(tokens.motion.easing)) w(`  ${v('easing', name)}: cubic-bezier(${r(token.$value).join(', ')});`);
  for (const [component, group] of entries(tokens.component)) {
    for (const [name, token] of entries(group)) w(`  ${v(component, name)}: ${px(r(token.$value))}px;`);
  }
  w('}');
  w();

  const semantic = (theme, indent) =>
    entries(tokens.color[theme]).map(([name, token]) => `${indent}${v('color', name)}: ${cssColor(r(token.$value))};`);

  w('/* Licht thema (standaard). */');
  w(':root,');
  w(":root[data-theme='light'] {");
  w('  color-scheme: light;');
  out.push(...semantic('light', '  '));
  w('}');
  w();
  w('/* Donker thema: volgt het systeem, tenzij expliciet licht gekozen. */');
  w('@media (prefers-color-scheme: dark) {');
  w("  :root:not([data-theme='light']) {");
  w('    color-scheme: dark;');
  out.push(...semantic('dark', '    '));
  w('  }');
  w('}');
  w();
  w(":root[data-theme='dark'] {");
  w('  color-scheme: dark;');
  out.push(...semantic('dark', '  '));
  w('}');
  return out.join('\n') + '\n';
}

// ---------- main ----------

function main() {
  validate();
  const generated = { [outputs.dart]: buildDart(), [outputs.css]: buildCss() };
  const check = process.argv.includes('--check');
  let stale = false;
  for (const [file, content] of Object.entries(generated)) {
    const rel = relative(repoRoot, file);
    if (check) {
      const current = existsSync(file) ? readFileSync(file, 'utf8') : null;
      if (current !== content) {
        console.error(`Verouderd: ${rel}. Draai: node design/tokens/build.mjs`);
        stale = true;
      } else {
        console.log(`Actueel: ${rel}`);
      }
    } else {
      mkdirSync(dirname(file), { recursive: true });
      writeFileSync(file, content);
      console.log(`Geschreven: ${rel}`);
    }
  }
  if (stale) process.exit(1);
}

main();
