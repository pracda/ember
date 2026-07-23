// Captures README screenshots of all four Ember apps in a seeded state.
// Prereqs: backend on :8899 and the four Vite dev servers running, with some
// demo data seeded. Run:  node scripts/screenshots.mjs
import { chromium } from 'playwright';
import { mkdirSync } from 'node:fs';

const API = 'http://localhost:8899';
const OUT = 'docs/images';
mkdirSync(OUT, { recursive: true });

async function post(path, body) {
  const r = await fetch(API + path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  return r.json();
}
async function rosterId(name) {
  const list = await (await fetch(API + '/api/auth/roster')).json();
  return list.find((s) => s.displayName === name).id;
}

const cashier = await post('/api/auth/pin', { staffId: await rosterId('Cashier'), pin: '1111' });
const cook = await post('/api/auth/pin', { staffId: await rosterId('Cook'), pin: '2222' });
const manager = await post('/api/auth/login', { username: 'manager', password: 'manager123' });

const browser = await chromium.launch();
const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 }, deviceScaleFactor: 2 });

async function open(url, session) {
  const page = await ctx.newPage();
  await page.goto(url, { waitUntil: 'networkidle' }).catch(() => {});
  if (session) {
    await page.evaluate((s) => localStorage.setItem('ember.auth', JSON.stringify(s)), session);
    await page.reload({ waitUntil: 'networkidle' }).catch(() => {});
  }
  await page.waitForTimeout(1800);
  return page;
}
const shot = (page, name) => page.screenshot({ path: `${OUT}/${name}.png` });

// POS — drop a couple of items on the ticket, then show the Drinks tab
// (Fountain Soda "low", Milkshake sold out).
const pos = await open('http://localhost:5173', cashier);
try {
  await pos.getByRole('tab', { name: 'Sweets' }).click();
  await pos.getByRole('button').filter({ hasText: 'Cookie' }).first().click();
  await pos.getByRole('button').filter({ hasText: 'Apple Pie' }).first().click();
  await pos.getByRole('tab', { name: 'Drinks' }).click();
  await pos.waitForTimeout(700);
} catch (e) {
  console.warn('pos setup:', e.message);
}
await shot(pos, 'pos');

await shot(await open('http://localhost:5174', cook), 'kitchen');
await shot(await open('http://localhost:5175', null), 'board');

const admin = await open('http://localhost:5176', manager);
for (const [tab, name] of [
  ['Menu', 'admin-menu'],
  ['Orders', 'admin-orders'],
  ['Schedule', 'admin-schedule'],
  ['Reports', 'admin-analytics'],
  ['Employees', 'admin-employees'],
]) {
  await admin.getByRole('button', { name: tab }).click();
  await admin.waitForTimeout(tab === 'Reports' || tab === 'Schedule' ? 1300 : 700);
  await shot(admin, name);
}

await browser.close();
console.log('screenshots written to', OUT);
