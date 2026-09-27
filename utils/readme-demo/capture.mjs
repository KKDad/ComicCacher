#!/usr/bin/env node
// Captures the README screenshots from a running comic-hub that points at
// demo-api.mjs. Drives headless Chrome over the DevTools protocol with Node's
// built-in WebSocket, so it needs no npm packages.
//
// Usage: node utils/readme-demo/capture.mjs [--ui http://localhost:3000] [--out docs/images/readme]
//        [--banner http://localhost:8099/banner] [--chrome google-chrome]

import { spawn } from 'node:child_process';
import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const HERE = dirname(fileURLToPath(import.meta.url));
const args = Object.fromEntries(
  process.argv.slice(2).reduce((acc, a, i, all) => (a.startsWith('--') ? [...acc, [a.slice(2), all[i + 1]]] : acc), []),
);
const UI = args.ui ?? 'http://localhost:3000';
const OUT = args.out ?? join(HERE, '..', '..', 'docs', 'images', 'readme');
const BANNER = args.banner ?? 'http://localhost:8099/banner';
const CHROME = args.chrome ?? process.env.CHROME ?? 'google-chrome';
const PORT = 9333;

const pad = (n) => String(n).padStart(2, '0');
const localDate = (daysAgo) => {
  const d = new Date();
  d.setDate(d.getDate() - daysAgo);
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
};

const DESKTOP = { width: 1440, height: 900, deviceScaleFactor: 1, mobile: false };
const PHONE = { width: 390, height: 844, deviceScaleFactor: 2, mobile: true };

const SHOTS = [
  { file: 'dashboard.png', path: '/', viewport: { ...DESKTOP, height: 1012 } },
  { file: 'reader.png', path: `/comics/102/read?date=${localDate(3)}`, viewport: DESKTOP },
  { file: 'today.png', path: '/read', viewport: DESKTOP },
  { file: 'library.png', path: '/comics', viewport: DESKTOP },
  // Phone shots are combined into phones.png rather than saved on their own
  { file: 'phone-home.png', path: '/', viewport: PHONE, phone: true },
  { file: 'phone-today.png', path: '/read', viewport: PHONE, phone: true },
];

// Two phone screens side by side in simple bezels, sized like the desktop shots
const phonesPage = (shots) => `<!doctype html><html><head><style>
  html, body { margin: 0; height: 100%; background: #F2ECDF; }
  body { display: flex; align-items: center; justify-content: center; gap: 72px; }
  img { height: 780px; border: 10px solid #1E1B16; border-radius: 44px; box-shadow: 0 18px 40px rgb(30 27 22 / 0.18); }
</style></head><body>${shots.map((b64) => `<img src="data:image/png;base64,${b64}">`).join('')}</body></html>`;

// The Next.js dev badge and toasts aren't part of the product
const HIDE_DEV_UI = 'nextjs-portal{display:none!important}';

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

// --- a tiny CDP client ---------------------------------------------------------

class Page {
  constructor(ws) {
    this.ws = ws;
    this.id = 0;
    this.pending = new Map();
    this.listeners = new Set();
    this.inflight = new Set();
    ws.addEventListener('message', (e) => {
      const msg = JSON.parse(e.data);
      if (msg.id && this.pending.has(msg.id)) {
        const { resolve, reject } = this.pending.get(msg.id);
        this.pending.delete(msg.id);
        msg.error ? reject(new Error(`${msg.error.message}`)) : resolve(msg.result);
      } else if (msg.method) {
        if (msg.method === 'Network.requestWillBeSent') this.inflight.add(msg.params.requestId);
        if (msg.method === 'Network.loadingFinished' || msg.method === 'Network.loadingFailed') {
          this.inflight.delete(msg.params.requestId);
        }
        for (const l of this.listeners) l(msg);
      }
    });
  }

  send(method, params = {}) {
    const id = ++this.id;
    this.ws.send(JSON.stringify({ id, method, params }));
    return new Promise((resolve, reject) => this.pending.set(id, { resolve, reject }));
  }

  once(method, timeout = 30000) {
    return new Promise((resolve, reject) => {
      const t = setTimeout(() => reject(new Error(`Timed out waiting for ${method}`)), timeout);
      const l = (msg) => {
        if (msg.method === method) {
          clearTimeout(t);
          this.listeners.delete(l);
          resolve(msg.params);
        }
      };
      this.listeners.add(l);
    });
  }

  async eval(expression) {
    const { result, exceptionDetails } = await this.send('Runtime.evaluate', { expression, awaitPromise: true, returnByValue: true });
    if (exceptionDetails) throw new Error(exceptionDetails.exception?.description ?? exceptionDetails.text);
    return result.value;
  }

  async idle(quietMs = 800, timeout = 30000) {
    const start = Date.now();
    let quietSince = Date.now();
    while (Date.now() - start < timeout) {
      if (this.inflight.size > 0) quietSince = Date.now();
      else if (Date.now() - quietSince >= quietMs) return;
      await sleep(100);
    }
    console.warn(`  network still busy after ${timeout}ms (${this.inflight.size} requests)`);
  }

  async goto(url) {
    const loaded = this.once('Page.loadEventFired', 60000);
    await this.send('Page.navigate', { url });
    await loaded;
    await this.idle();
    await this.eval('document.fonts.ready.then(() => true)');
  }
}

async function launchChrome() {
  const profile = mkdtempSync(join(tmpdir(), 'readme-shots-'));
  const chrome = spawn(
    CHROME,
    [
      '--headless=new',
      `--remote-debugging-port=${PORT}`,
      `--user-data-dir=${profile}`,
      '--hide-scrollbars',
      '--no-first-run',
      '--no-default-browser-check',
      '--disable-extensions',
      'about:blank',
    ],
    { stdio: 'ignore' },
  );
  for (let i = 0; i < 50; i++) {
    try {
      const targets = await (await fetch(`http://127.0.0.1:${PORT}/json/list`)).json();
      const page = targets.find((t) => t.type === 'page');
      if (page) return { chrome, profile, wsUrl: page.webSocketDebuggerUrl };
    } catch {
      // not up yet
    }
    await sleep(200);
  }
  chrome.kill();
  throw new Error(`Chrome didn't start (${CHROME})`);
}

async function screenshot(page, file) {
  const { data } = await page.send('Page.captureScreenshot', { format: 'png' });
  if (file) {
    writeFileSync(join(OUT, file), Buffer.from(data, 'base64'));
    console.log(`  ${file}`);
  }
  return data;
}

async function main() {
  mkdirSync(OUT, { recursive: true });
  const { chrome, profile, wsUrl } = await launchChrome();
  const ws = new WebSocket(wsUrl);
  await new Promise((resolve, reject) => {
    ws.addEventListener('open', resolve, { once: true });
    ws.addEventListener('error', reject, { once: true });
  });
  const page = new Page(ws);

  try {
    await page.send('Page.enable');
    await page.send('Network.enable');
    await page.send('Runtime.enable');
    // Light mode only: the app follows the OS theme until the user picks one
    await page.send('Emulation.setEmulatedMedia', { features: [{ name: 'prefers-color-scheme', value: 'light' }] });
    await page.send('Page.addScriptToEvaluateOnNewDocument', {
      source: `document.addEventListener('DOMContentLoaded', () => {
        const s = document.createElement('style'); s.textContent = ${JSON.stringify(HIDE_DEV_UI)};
        document.head.appendChild(s);
      });`,
    });

    console.log('Banner');
    await page.send('Emulation.setDeviceMetricsOverride', { width: 1280, height: 320, deviceScaleFactor: 2, mobile: false });
    await page.send('Emulation.setDefaultBackgroundColorOverride', { color: { r: 0, g: 0, b: 0, a: 0 } });
    await page.goto(BANNER);
    await screenshot(page, 'banner.png');
    await page.send('Emulation.setDefaultBackgroundColorOverride', {});

    console.log(`Signing in to ${UI}`);
    await page.send('Emulation.setDeviceMetricsOverride', DESKTOP);
    await page.goto(`${UI}/login`);
    const status = await page.eval(`fetch('/api/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: 'reader', password: 'readme-demo-pass', rememberMe: true }),
    }).then((r) => r.status)`);
    if (status !== 200) throw new Error(`Login returned HTTP ${status}; is the UI pointed at demo-api.mjs?`);

    console.log('Screens');
    const phones = [];
    for (const shot of SHOTS) {
      await page.send('Emulation.setDeviceMetricsOverride', shot.viewport);
      await page.send('Emulation.setTouchEmulationEnabled', { enabled: shot.viewport.mobile });
      await page.goto(`${UI}${shot.path}`);
      // Let the reader's initial scroll and any fade-ins settle
      await sleep(1500);
      await page.idle();
      if (shot.phone) phones.push(await screenshot(page));
      else await screenshot(page, shot.file);
    }

    await page.send('Emulation.setTouchEmulationEnabled', { enabled: false });
    await page.send('Emulation.setDeviceMetricsOverride', DESKTOP);
    await page.goto('about:blank');
    const { frameTree } = await page.send('Page.getFrameTree');
    await page.send('Page.setDocumentContent', { frameId: frameTree.frame.id, html: phonesPage(phones) });
    await page.eval('Promise.all([...document.images].map((i) => i.decode())).then(() => true)');
    await screenshot(page, 'phones.png');
  } finally {
    ws.close();
    chrome.kill();
    await sleep(300);
    rmSync(profile, { recursive: true, force: true });
  }
}

main().catch((e) => {
  console.error(e.message);
  process.exit(1);
});
