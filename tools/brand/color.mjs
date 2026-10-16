// Kleurschalen afleiden uit één merkkleur, in OKLCH (gelijkmatige lichtheid voor het oog).
// Alleen gebruikt door `brand.mjs new` als startpunt; het contrast wordt daarna altijd per merk gecontroleerd.

const toLinear = (c) => (c <= 0.04045 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4);
const toGamma = (c) => (c <= 0.0031308 ? 12.92 * c : 1.055 * c ** (1 / 2.4) - 0.055);

export function hexToOklch(hex) {
  const n = parseInt(hex.slice(1), 16);
  const [r, g, b] = [(n >> 16) & 255, (n >> 8) & 255, n & 255].map((v) => toLinear(v / 255));
  const l = Math.cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b);
  const m = Math.cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b);
  const s = Math.cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b);
  const L = 0.2104542553 * l + 0.793617785 * m - 0.0040720468 * s;
  const A = 1.9779984951 * l - 2.428592205 * m + 0.4505937099 * s;
  const B = 0.0259040371 * l + 0.7827717662 * m - 0.808675766 * s;
  return { l: L, c: Math.hypot(A, B), h: (Math.atan2(B, A) * 180) / Math.PI };
}

function oklchToRgb({ l, c, h }) {
  const a = c * Math.cos((h * Math.PI) / 180);
  const b = c * Math.sin((h * Math.PI) / 180);
  const l_ = (l + 0.3963377774 * a + 0.2158037573 * b) ** 3;
  const m_ = (l - 0.1055613458 * a - 0.0638541728 * b) ** 3;
  const s_ = (l - 0.0894841775 * a - 1.291485548 * b) ** 3;
  return [
    4.0767416621 * l_ - 3.3077115913 * m_ + 0.2309699292 * s_,
    -1.2684380046 * l_ + 2.6097574011 * m_ - 0.3413193965 * s_,
    -0.0041960863 * l_ - 0.7034186147 * m_ + 1.707614701 * s_,
  ];
}

/** OKLCH naar hex; verlaagt de verzadiging tot de kleur in sRGB past. */
export function oklchToHex(color) {
  let { c } = color;
  let rgb = oklchToRgb({ ...color, c });
  while (rgb.some((v) => v < -0.0005 || v > 1.0005) && c > 0) {
    c = Math.max(0, c - 0.002);
    rgb = oklchToRgb({ ...color, c });
  }
  return (
    '#' +
    rgb
      .map((v) => Math.round(Math.min(1, Math.max(0, toGamma(Math.min(1, Math.max(0, v))))) * 255))
      .map((v) => v.toString(16).padStart(2, '0'))
      .join('')
      .toUpperCase()
  );
}

// Lichtheid en relatieve verzadiging per tint, gemeten aan de schalen van Noeroel Islam (die het contrast halen).
const LADDER = {
  primary: {
    50: [0.964, 0.1], 100: [0.913, 0.26], 200: [0.826, 0.5], 300: [0.722, 0.75], 400: [0.609, 0.95], 500: [0.524, 1],
    600: [0.447, 0.875], 700: [0.381, 0.75], 800: [0.324, 0.625], 900: [0.252, 0.475], 950: [0.199, 0.36],
  },
  accent: {
    50: [0.974, 0.17], 100: [0.935, 0.39], 200: [0.874, 0.73], 300: [0.81, 0.94], 400: [0.734, 0.99], 500: [0.652, 1],
    600: [0.555, 0.85], 700: [0.467, 0.71], 800: [0.39, 0.56], 900: [0.31, 0.4],
  },
};

/**
 * Een schaal rond een merkkleur: tint van de merkkleur, lichtheid en verzadigingsverloop volgens de ladder. De
 * merkkleur komt op de tint met de dichtstbijzijnde lichtheid; de rest van de schaal volgt haar verzadiging.
 */
export function scaleFrom(hex, kind) {
  const seed = hexToOklch(hex);
  const ladder = LADDER[kind];
  const shades = Object.keys(ladder);
  const nearest = shades.reduce((best, shade) => (Math.abs(ladder[shade][0] - seed.l) < Math.abs(ladder[best][0] - seed.l) ? shade : best));
  const peak = seed.c / ladder[nearest][1];
  const scale = {};
  for (const shade of shades) {
    const [l, relative] = ladder[shade];
    scale[shade] = shade === nearest ? hex.toUpperCase() : oklchToHex({ l, c: peak * relative, h: seed.h });
  }
  return { scale, seedShade: nearest };
}
