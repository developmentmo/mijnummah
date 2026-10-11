#!/usr/bin/env node
// Controleert dat kleuren en fonts alleen uit de design tokens komen.
// Vindt losse kleurwaarden in de Flutter-app en het webportaal, buiten de gegenereerde tokenbestanden.

import { readdirSync, readFileSync, statSync } from 'node:fs';
import { join, relative } from 'node:path';
import { repoRoot } from './lib.mjs';

const rules = [
  {
    root: 'frontend/app/lib',
    ext: ['.dart'],
    skip: ['tokens.g.dart'],
    patterns: [
      { re: /Color\(0x[0-9a-fA-F]+\)/, what: 'losse Color(0x…)' },
      { re: /Color\.fromARGB|Color\.fromRGBO/, what: 'losse kleur' },
      { re: /(?<![A-Za-z])Colors\.(?!transparent\b)\w+/, what: 'Material-kleur (Colors.*)' },
      { re: /fontFamily:\s*'/, what: 'losse fontFamily' },
    ],
  },
  {
    root: 'frontend/webportaal/src',
    ext: ['.ts', '.html', '.scss', '.css'],
    skip: ['tokens.generated.css'],
    patterns: [
      { re: /#[0-9a-fA-F]{3,8}\b(?![\w-]*\s*\{)/, what: 'losse hex-kleur', strip: /&#\d+;|href="#[^"]*"|#inhoud/g },
      { re: /\b(rgb|rgba|hsl|hsla)\(/, what: 'losse kleurfunctie' },
      { re: /font-family:(?!\s*var\()/, what: 'losse font-family', allowIn: ['fonts.css'] },
    ],
  },
];

function* files(dir, ext) {
  for (const name of readdirSync(dir)) {
    const path = join(dir, name);
    if (statSync(path).isDirectory()) yield* files(path, ext);
    else if (ext.some((e) => name.endsWith(e))) yield path;
  }
}

let problems = 0;
for (const rule of rules) {
  for (const file of files(join(repoRoot, rule.root), rule.ext)) {
    if (rule.skip.some((s) => file.endsWith(s))) continue;
    readFileSync(file, 'utf8')
      .split('\n')
      .forEach((line, i) => {
        if (/^\s*(\/\/|\*|\/\*)/.test(line)) return;
        for (const p of rule.patterns) {
          if (p.allowIn?.some((a) => file.endsWith(a))) continue;
          const text = p.strip ? line.replace(p.strip, '') : line;
          if (p.re.test(text)) {
            console.error(`${relative(repoRoot, file)}:${i + 1}: ${p.what}: ${line.trim()}`);
            problems++;
          }
        }
      });
  }
}
if (problems) {
  console.error(`\n${problems} losse stijlwaarde(n) gevonden. Gebruik de design tokens (design/tokens/tokens.json).`);
  process.exit(1);
}
console.log('Geen losse kleuren of fonts gevonden buiten de design tokens.');
