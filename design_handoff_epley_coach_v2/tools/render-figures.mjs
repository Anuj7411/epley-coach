// Pre-renders the 3D figure's held frames with the supplied renderer (README §7, option B).
// Same code as the live WebView, so the same pixels; used wherever the figure is still.
//   node render-figures.mjs   ->  app/src/main/assets/figure/held/<size>/<theme>-<ear>-<step>.png
import { chromium } from 'playwright';
import handler from 'serve-handler';
import http from 'node:http';
import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const assets = path.resolve(here, '../../app/src/main/assets/figure');
const server = http.createServer((q, r) => handler(q, r, { public: assets, cleanUrls: false }));
await new Promise(r => server.listen(4175, r));
const browser = await chromium.launch({ args: ['--use-angle=swiftshader', '--enable-unsafe-swiftshader'] });

// Which sizes need which steps: Find and Hold show all five; Welcome and the ear result show 1;
// Calibrate shows position 5's end pose (seated, facing forward, head level) at 208.
const only = process.argv.includes('--calibrate');
const jobs = only ? [[208, [5]]] : [[248, [1, 2, 3, 4, 5]], [144, [1, 2, 3, 4, 5]], [208, [1, 5]], [136, [1]]];
for (const [size, steps] of jobs) {
  const ctx = await browser.newContext({ viewport: { width: size, height: size }, deviceScaleFactor: 3 });
  const page = await ctx.newPage();
  for (const theme of ['light', 'night']) for (const ear of ['R', 'L']) for (const step of steps) {
    await page.goto(`http://localhost:4175/figure-view.html#step=${step}&ear=${ear}&theme=${theme}&playing=0`, { waitUntil: 'networkidle' });
    await page.waitForFunction(() => typeof window.setFigure === 'function', null, { timeout: 30000 });
    await page.evaluate(() => document.fonts.ready);
    await page.waitForTimeout(1500);
    const dir = path.join(assets, 'held', String(size)); await fs.mkdir(dir, { recursive: true });
    await page.locator('#fig').screenshot({ path: path.join(dir, `${theme}-${ear}-${step}.png`), omitBackground: true });
    console.log('✓', size, theme, ear, step);
  }
  await ctx.close();
}
await browser.close(); server.close();
