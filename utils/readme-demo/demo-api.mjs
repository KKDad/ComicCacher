#!/usr/bin/env node
// A stand-in for comic-api that serves invented comics, for the README
// screenshots. It builds the real GraphQL schema from comic-api, so the UI's
// queries validate exactly as they would against the backend, and draws every
// strip and avatar with strips.mjs.
//
// Usage: node utils/readme-demo/demo-api.mjs   (DEMO_API_PORT, default 8099)
//
// Serves:
//   POST /graphql                         reader-side queries and mutations
//   GET  /api/v1/comics/:id/avatar        SVG avatar
//   GET  /api/v1/comics/:id/strip/:date   SVG strip
//   GET  /banner                          README banner page (logo lockup)

import { createServer } from 'node:http';
import { createRequire } from 'node:module';
import { readFileSync, readdirSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { COMICS, DAYS, FAVORITES, LAST_READ_DAYS_AGO, NEWEST_DAYS_AGO, FOUR_PANEL, DEMO_USER } from './fixtures.mjs';
import { stripSvg, stripSize, avatarSvg } from './strips.mjs';

const HERE = dirname(fileURLToPath(import.meta.url));
const ROOT = join(HERE, '..', '..');
const PORT = Number(process.env.DEMO_API_PORT ?? 8099);

// graphql comes from comic-hub's node_modules; this script has no dependencies of its own
const require = createRequire(join(ROOT, 'comic-hub', 'package.json'));
const { buildSchema, graphql } = require('graphql');

const SCHEMA_DIR = join(ROOT, 'comic-api', 'src', 'main', 'resources', 'graphql');
const schema = buildSchema(
  [
    readFileSync(join(SCHEMA_DIR, 'comics-schema.graphql'), 'utf8'),
    ...readdirSync(join(SCHEMA_DIR, 'types'))
      .filter((f) => f.endsWith('.graphql'))
      .map((f) => readFileSync(join(SCHEMA_DIR, 'types', f), 'utf8')),
  ].join('\n'),
);

// --- dates (local, like the browser on the same machine) -----------------------

const DAY_MS = 86_400_000;
const pad = (n) => String(n).padStart(2, '0');
const iso = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
const today = () => {
  const d = new Date();
  return new Date(d.getFullYear(), d.getMonth(), d.getDate());
};
const daysAgo = (n) => iso(new Date(today().getTime() - n * DAY_MS));
const addDays = (date, n) => {
  const [y, m, d] = date.split('-').map(Number);
  return iso(new Date(y, m - 1, d + n));
};
// Days since the epoch, so each date always gets the same gag
const dayNumber = (date) => {
  const [y, m, d] = date.split('-').map(Number);
  return Math.round(Date.UTC(y, m - 1, d) / DAY_MS);
};

// --- demo state -----------------------------------------------------------------

const prefs = {
  username: DEMO_USER.username,
  favoriteComics: [...FAVORITES],
  lastReadDates: Object.entries(LAST_READ_DAYS_AGO).map(([id, n]) => ({ comicId: Number(id), date: daysAgo(n) })),
  displaySettings: { theme: 'light' },
};

const newest = (c) => daysAgo(NEWEST_DAYS_AGO[c.id] ?? 0);
const oldest = (c) => addDays(newest(c), -(DAYS - 1));

function strip(c, date) {
  if (!date || date < oldest(c) || date > newest(c)) {
    return { date, available: false, imageUrl: null, width: null, height: null, transcript: null, previous: null, next: null };
  }
  const { width, height } = stripSize(c, FOUR_PANEL.has(c.id));
  return {
    date,
    available: true,
    imageUrl: `/api/v1/comics/${c.id}/strip/${date}`,
    width,
    height,
    transcript: null,
    previous: () => (date > oldest(c) ? strip(c, addDays(date, -1)) : null),
    next: () => (date < newest(c) ? strip(c, addDays(date, 1)) : null),
  };
}

function comic(c) {
  if (!c) return null;
  return {
    id: c.id,
    name: c.name,
    author: c.author,
    description: c.description,
    oldest: oldest(c),
    newest: newest(c),
    enabled: true,
    active: true,
    avatarAvailable: true,
    avatarUrl: `/api/v1/comics/${c.id}/avatar`,
    source: 'demo',
    sourceIdentifier: c.name.toLowerCase().replace(/[^a-z]+/g, '-'),
    publicationDays: null,
    strip: ({ date }) => strip(c, date ?? newest(c)),
    strips: ({ dates }) => dates.map((d) => strip(c, d)),
    firstStrip: () => strip(c, oldest(c)),
    lastStrip: () => strip(c, newest(c)),
    stripWindow: ({ center, before, after }) => {
      const out = [];
      for (let i = -before; i <= after; i++) {
        const s = strip(c, addDays(center, i));
        if (s.available) out.push(s);
      }
      return out;
    },
  };
}

const byId = (id) => COMICS.find((c) => c.id === id);
const sorted = () => [...COMICS].sort((a, b) => a.name.localeCompare(b.name));

function connection(list, first = 20, after) {
  const start = after ? Number(Buffer.from(after, 'base64').toString()) + 1 : 0;
  const page = list.slice(start, start + first);
  const cursor = (i) => Buffer.from(String(start + i)).toString('base64');
  return {
    edges: page.map((c, i) => ({ node: comic(c), cursor: cursor(i) })),
    pageInfo: {
      hasNextPage: start + first < list.length,
      hasPreviousPage: start > 0,
      startCursor: page.length ? cursor(0) : null,
      endCursor: page.length ? cursor(page.length - 1) : null,
    },
    totalCount: list.length,
  };
}

const auth = () => ({ token: 'demo-token', refreshToken: 'demo-refresh', username: DEMO_USER.username, displayName: DEMO_USER.displayName });
const prefPayload = () => ({ preference: prefs, errors: [] });

const root = {
  // Queries
  comics: ({ first, after, search }) =>
    connection(sorted().filter((c) => !search || c.name.toLowerCase().includes(search.toLowerCase())), first, after),
  comic: ({ id }) => comic(byId(id)),
  strip: ({ comicId, date }) => (byId(comicId) ? strip(byId(comicId), date) : null),
  randomStrip: ({ comicId }) => {
    const c = byId(comicId) ?? COMICS[0];
    return strip(c, addDays(newest(c), -Math.floor(Math.random() * DAYS)));
  },
  search: ({ query, limit = 20 }) => {
    const hits = sorted().filter((c) => c.name.toLowerCase().includes(query.toLowerCase())).slice(0, limit);
    return { comics: hits.map(comic), totalCount: hits.length, query };
  },
  validateToken: () => true,
  me: () => ({ ...DEMO_USER, created: '2024-01-15T09:00:00Z', lastLogin: new Date().toISOString() }),
  preferences: () => prefs,
  errorCodes: () => [],

  // Mutations
  login: auth,
  register: auth,
  refreshToken: auth,
  logout: () => true,
  addFavorite: ({ comicId }) => {
    if (!prefs.favoriteComics.includes(comicId)) prefs.favoriteComics.push(comicId);
    return prefPayload();
  },
  removeFavorite: ({ comicId }) => {
    prefs.favoriteComics = prefs.favoriteComics.filter((id) => id !== comicId);
    return prefPayload();
  },
  updateLastRead: ({ comicId, date }) => {
    prefs.lastReadDates = [...prefs.lastReadDates.filter((e) => e.comicId !== comicId), { comicId, date }];
    return prefPayload();
  },
  updateDisplaySettings: ({ settings }) => {
    prefs.displaySettings = { ...prefs.displaySettings, ...settings };
    return prefPayload();
  },
};

// --- HTTP -------------------------------------------------------------------------

function send(res, status, type, body) {
  res.writeHead(status, { 'Content-Type': type, 'Cache-Control': 'no-cache' });
  res.end(body);
}

async function handleGraphql(req, res) {
  let body = '';
  for await (const chunk of req) body += chunk;
  const { query, variables, operationName } = JSON.parse(body || '{}');
  const result = await graphql({ schema, source: query, rootValue: root, variableValues: variables, operationName });
  const name = operationName ?? /(?:query|mutation)\s+(\w+)/.exec(query ?? '')?.[1] ?? 'anonymous';
  if (result.errors) {
    console.warn(`[demo-api] ${name}: ${result.errors.map((e) => e.message).join('; ')}`);
  } else if (process.env.DEMO_API_VERBOSE) {
    console.log(`[demo-api] ${name}`);
  }
  send(res, 200, 'application/json', JSON.stringify(result));
}

const server = createServer(async (req, res) => {
  try {
    const url = new URL(req.url, 'http://localhost');
    let m;
    if (req.method === 'POST' && url.pathname === '/graphql') return await handleGraphql(req, res);
    if ((m = /^\/api\/v1\/comics\/(\d+)\/avatar$/.exec(url.pathname)) && byId(Number(m[1]))) {
      return send(res, 200, 'image/svg+xml', avatarSvg(byId(Number(m[1]))));
    }
    if ((m = /^\/api\/v1\/comics\/(\d+)\/strip\/(\d{4}-\d{2}-\d{2})$/.exec(url.pathname)) && byId(Number(m[1]))) {
      const c = byId(Number(m[1]));
      return send(res, 200, 'image/svg+xml', stripSvg(c, dayNumber(m[2]), FOUR_PANEL.has(c.id)));
    }
    if (url.pathname === '/banner') return send(res, 200, 'text/html', readFileSync(join(HERE, 'banner.html')));
    if (url.pathname === '/banner/icon.svg') {
      return send(res, 200, 'image/svg+xml', readFileSync(join(ROOT, 'comic-hub', 'src', 'app', 'icon.svg')));
    }
    console.warn(`[demo-api] 404 ${req.method} ${url.pathname}`);
    send(res, 404, 'text/plain', 'Not found');
  } catch (e) {
    console.error('[demo-api]', e);
    send(res, 500, 'text/plain', String(e));
  }
});

server.listen(PORT, () => console.log(`[demo-api] http://localhost:${PORT}/graphql`));
