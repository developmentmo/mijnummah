#!/usr/bin/env node
// Zet testmoskeeën met beheerders en content klaar, uitsluitend via de API die ook de portalen gebruiken.
// Zo laat de pilot zien dat een moskee aansluiten geen code, configuratie of databasewijziging vraagt.
//
//   API_BASE_URL=http://localhost:8082 \
//   PLATFORM_ADMIN_EMAIL=... PLATFORM_ADMIN_PASSWORD=... DEMO_PASSWORD=... \
//   node tools/demo/seed-demo.mjs [tools/demo/demo-mosques.json]
//
// Een moskee waarvan de slug al bestaat, wordt overgeslagen: het script is veilig om opnieuw te draaien en wijzigt
// of verwijdert nooit bestaande gegevens. Wachtwoorden komen alleen uit de omgeving en worden niet gelogd.

import { readFile } from 'node:fs/promises';
import { deflateSync } from 'node:zlib';

const base = (process.env.API_BASE_URL ?? 'http://localhost:8082').replace(/\/$/, '') + '/api/v1';
const configPath = process.argv[2] ?? new URL('./demo-mosques.json', import.meta.url);

function required(name) {
  const value = process.env[name];
  if (!value) {
    console.error(`Zet ${name} in de omgeving (zie tools/demo/README.md).`);
    process.exit(2);
  }
  return value;
}

const platformEmail = required('PLATFORM_ADMIN_EMAIL');
const platformPassword = required('PLATFORM_ADMIN_PASSWORD');
const demoPassword = required('DEMO_PASSWORD');

class ApiError extends Error {
  constructor(method, path, status, body) {
    super(`${method} ${path} gaf ${status}: ${body?.code ?? ''} ${body?.message ?? ''}`.trim());
    this.status = status;
    this.code = body?.code;
  }
}

async function call(method, path, { token, json, form } = {}) {
  const headers = {};
  if (token) headers.Authorization = `Bearer ${token}`;
  let body;
  if (json !== undefined) {
    headers['Content-Type'] = 'application/json';
    body = JSON.stringify(json);
  } else if (form) {
    body = form;
  }
  const response = await fetch(base + path, { method, headers, body });
  const text = await response.text();
  const parsed = text ? JSON.parse(text) : null;
  if (!response.ok) throw new ApiError(method, path, response.status, parsed);
  return parsed;
}

async function login(email, password) {
  return (await call('POST', '/auth/login', { json: { email, password } })).accessToken;
}

/** Maakt het account (met DEMO_PASSWORD) als het nog niet bestaat. */
async function ensureAccount(platformToken, person) {
  try {
    await call('POST', '/admin/users', {
      token: platformToken,
      json: { email: person.email, displayName: person.displayName, password: demoPassword, platformRole: 'USER' },
    });
    return true;
  } catch (error) {
    if (error.code === 'EMAIL_ALREADY_REGISTERED') return false;
    throw error;
  }
}

// ---- Afbeeldingen: een eenvoudige PNG (logo met cirkel, omslag met verloop), zonder extra dependencies ----

const crcTable = Array.from({ length: 256 }, (_, n) => {
  let c = n;
  for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
  return c >>> 0;
});
function crc32(buffer) {
  let c = 0xffffffff;
  for (const byte of buffer) c = crcTable[(c ^ byte) & 0xff] ^ (c >>> 8);
  return (c ^ 0xffffffff) >>> 0;
}
function chunk(type, data) {
  const length = Buffer.alloc(4);
  length.writeUInt32BE(data.length);
  const typeAndData = Buffer.concat([Buffer.from(type, 'ascii'), data]);
  const crc = Buffer.alloc(4);
  crc.writeUInt32BE(crc32(typeAndData));
  return Buffer.concat([length, typeAndData, crc]);
}
function png(width, height, pixel) {
  const raw = Buffer.alloc((width * 3 + 1) * height);
  for (let y = 0; y < height; y++) {
    const row = y * (width * 3 + 1);
    for (let x = 0; x < width; x++) {
      const [r, g, b] = pixel(x, y);
      raw.set([r, g, b], row + 1 + x * 3);
    }
  }
  const header = Buffer.alloc(13);
  header.writeUInt32BE(width, 0);
  header.writeUInt32BE(height, 4);
  header.set([8, 2, 0, 0, 0], 8); // 8 bits, RGB
  return Buffer.concat([
    Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
    chunk('IHDR', header),
    chunk('IDAT', deflateSync(raw)),
    chunk('IEND', Buffer.alloc(0)),
  ]);
}
const lighten = ([r, g, b], f) => [r + (255 - r) * f, g + (255 - g) * f, b + (255 - b) * f].map(Math.round);
function logo(color) {
  const size = 512;
  return png(size, size, (x, y) => {
    const d = Math.hypot(x - size / 2, y - size / 2);
    const inner = Math.hypot(x - size * 0.58, y - size * 0.45);
    if (d < size * 0.3 && inner > size * 0.24) return [255, 255, 255]; // maansikkel
    return color;
  });
}
function cover(color) {
  return png(1600, 900, (x, y) => lighten(color, (x / 1600) * 0.5 + (y / 900) * 0.2));
}

async function upload(token, mosqueId, name, bytes) {
  const form = new FormData();
  form.append('file', new Blob([bytes], { type: 'image/png' }), name);
  return (await call('POST', `/admin/mosques/${mosqueId}/images`, { token, form })).id;
}

// ---- Content ----

function eventBody(event) {
  const start = new Date();
  start.setDate(start.getDate() + event.inDays);
  start.setHours(19, 0, 0, 0);
  const end = new Date(start.getTime() + (event.hours ?? 2) * 3600_000);
  return {
    title: event.title,
    description: event.description ?? `${event.title}. Testcontent voor de pilot.`,
    category: event.category,
    startsAt: start.toISOString(),
    endsAt: end.toISOString(),
    location: event.location,
    visibility: event.visibility ?? 'PUBLIC',
    registrationEnabled: event.registrationEnabled ?? false,
    capacity: event.capacity ?? null,
  };
}

async function createAndPublish(token, mosqueId, kind, body) {
  const created = await call('POST', `/admin/mosques/${mosqueId}/${kind}`, { token, json: body });
  await call('POST', `/admin/mosques/${mosqueId}/${kind}/${created.id}/publish`, { token });
  return created.id;
}

async function seedMosque(platformToken, mosque) {
  const madeAdmin = await ensureAccount(platformToken, mosque.admin);
  const created = await call('POST', '/admin/mosques', {
    token: platformToken,
    json: {
      slug: mosque.slug,
      details: { name: mosque.details.name, city: mosque.details.city },
      firstAdministrator: mosque.admin,
      settings: { registrationsEnabled: true, pushEnabled: true },
    },
  });
  const mosqueId = created.mosque.id;
  console.log(`+ ${mosque.details.name} (${mosque.slug}) geregistreerd; beheerder ${mosque.admin.email}${madeAdmin ? ' aangemaakt' : ' bestond al'}`);

  // Vanaf hier werkt de beheerder van de moskee zelf, zoals in het moskeeportaal.
  const adminToken = await login(mosque.admin.email, demoPassword);
  await call('PUT', `/admin/mosques/${mosqueId}`, { token: adminToken, json: mosque.details });
  const logoId = await upload(adminToken, mosqueId, 'logo.png', logo(mosque.color));
  const coverId = await upload(adminToken, mosqueId, 'omslag.png', cover(mosque.color));
  await call('PUT', `/admin/mosques/${mosqueId}/images`, {
    token: adminToken,
    json: { logoImageId: logoId, coverImageId: coverId },
  });

  let contentToken = adminToken;
  if (mosque.editor) {
    await ensureAccount(platformToken, mosque.editor);
    await call('POST', `/admin/mosques/${mosqueId}/administrators`, {
      token: adminToken,
      json: { email: mosque.editor.email, role: 'EDITOR' },
    });
    contentToken = await login(mosque.editor.email, demoPassword);
  }
  for (const event of mosque.events ?? []) await createAndPublish(contentToken, mosqueId, 'events', eventBody(event));
  for (const item of mosque.media ?? []) {
    await createAndPublish(contentToken, mosqueId, 'media', {
      title: item.title,
      description: item.description ?? 'Testvideo voor de pilot; vervang de link door een echte lezing.',
      category: item.category,
      speaker: item.speaker,
      videoUrl: `https://www.youtube.com/watch?v=${item.videoId}`,
    });
  }
  for (const news of mosque.announcements ?? []) {
    await createAndPublish(contentToken, mosqueId, 'announcements', {
      title: news.title,
      body: news.body,
      featured: news.featured ?? false,
    });
  }
  console.log(
    `  profiel, logo, omslag, ${mosque.events?.length ?? 0} activiteiten, ${mosque.media?.length ?? 0} video's, ` +
      `${mosque.announcements?.length ?? 0} aankondigingen${mosque.editor ? `, redacteur ${mosque.editor.email}` : ''}`,
  );
}

const config = JSON.parse(await readFile(configPath, 'utf8'));
const platformToken = await login(platformEmail, platformPassword);
const existing = new Set((await call('GET', '/admin/platform/mosques', { token: platformToken })).map((m) => m.slug));
for (const mosque of config.mosques) {
  if (existing.has(mosque.slug)) {
    console.log(`= ${mosque.details.name} (${mosque.slug}) bestaat al; overgeslagen`);
    continue;
  }
  await seedMosque(platformToken, mosque);
}
console.log(`Klaar. Open de app of het portaal en log in met een van de beheerders hierboven (wachtwoord: DEMO_PASSWORD).`);
