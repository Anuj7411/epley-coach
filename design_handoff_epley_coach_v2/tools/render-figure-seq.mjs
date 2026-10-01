// Renders each Epley position's move as a PNG sequence (transparent), driven by a fake clock so
// every frame is deterministic: the same renderer and timings as the app (T in epley-figure-app.js).
//   node render-figure-seq.mjs  ->  video/assets/fig/<step>/f_0000.png …
import { chromium } from 'playwright';
import handler from 'serve-handler';
import http from 'node:http';
import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const assets = path.resolve(here, '../../app/src/main/assets/figure');
const out = path.resolve(here, process.env.OUT || '../../video/assets/figseq');
const server = http.createServer((q, r) => handler(q, r, { public: assets, cleanUrls: false }));
await new Promise(r => server.listen(+(process.env.PORT || 4176), r));
const browser = await chromium.launch({ args: ['--use-angle=swiftshader', '--enable-unsafe-swiftshader'] });
const SIZE = 640, FPS = +(process.env.FPS || 30);
// FADE 0.35 + HOLD0 1.0 -> the move runs 1.35..3.75 s, then the ghost fades in over 0.3 s.
const FROM = 1.30, TO = 4.10;
const steps = (process.argv[2] || '1,2,3,4,5').split(',').map(Number);
for (const step of steps) {
  const ctx = await browser.newContext({ viewport: { width: SIZE, height: SIZE }, deviceScaleFactor: 1.5 });
  const page = await ctx.newPage();
  // The figure animates from performance.now(): pin it before any script runs and set it per frame.
  await page.emulateMedia({ reducedMotion: 'no-preference' });
  await page.addInitScript(() => { let T = 0; performance.now = () => T; window.__setT = ms => { T = ms; }; });
  await page.goto(`http://localhost:${process.env.PORT || 4176}/figure-view.html#step=${step}&ear=R&theme=${process.env.THEME || "night"}&playing=1`);
  await page.waitForFunction(() => typeof window.setFigure === 'function', null, { timeout: 60000 });
  await page.evaluate(() => document.fonts.ready);
  const dir = path.join(out, String(step)); await fs.mkdir(dir, { recursive: true });
  const n = Math.round((TO - FROM) * FPS);
  for (let k = 0; k < n; k++) {
    await page.evaluate(ms => new Promise(r => { window.__setT(ms); requestAnimationFrame(() => requestAnimationFrame(r)); }), (FROM + k / FPS) * 1000);
    await page.screenshot({ path: path.join(dir, `f_${String(k).padStart(4, '0')}.png`), omitBackground: true });
  }
  console.log('step', step, n, 'frames');
  await ctx.close();
}
await browser.close(); server.close();
