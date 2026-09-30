// Renders the design into pixel references + a layout spec for every screen × theme × device.
//   cd tools && npm i && npx playwright install chromium && npm run refs
//   node render-references.mjs --device pixel-412x915 --only home,find-p4
// Output: ../reference/<device>/<theme>/<id>.png   (device pixels, no bezel, 3D figure in its held frame)
//         ../reference/<device>/<theme>/<id>.json  (every visible element: box in dp, colours, type, radius, border, text)
import { chromium } from 'playwright';
import handler from 'serve-handler';
import http from 'node:http';
import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url)), root = path.resolve(here, '..');
const cfg = JSON.parse(await fs.readFile(path.join(here, 'screens.json'), 'utf8'));
const arg = (k) => { const i = process.argv.indexOf('--' + k); return i > 0 ? process.argv[i + 1] : null; };
const onlyDev = arg('device'), only = arg('only')?.split(',');

// cleanUrls off: serve-handler otherwise 301s *.html to an extensionless path and drops the
// query string, so every reference silently rendered as the default screen at 390 x 844.
const server = http.createServer((req, res) => handler(req, res, { public: root, cleanUrls: false }));
await new Promise(r => server.listen(4173, r));
const browser = await chromium.launch();

// Collect a layout spec from the rendered DOM (values are in CSS px = dp at scale 1 of the design frame).
const SPEC = () => {
  const frame = document.querySelector('#shot div[style*="width:390px"], #shot div[style*="width: 390px"]');
  const fb = frame.getBoundingClientRect(), out = [];
  for (const el of frame.querySelectorAll('*')) {
    const cs = getComputedStyle(el), b = el.getBoundingClientRect();
    if (!b.width || !b.height || cs.visibility === 'hidden' || +cs.opacity === 0) continue;
    const text = [...el.childNodes].filter(n => n.nodeType === 3).map(n => n.textContent.trim()).join(' ').trim();
    const icon = cs.fontFamily.includes('Material Symbols') ? el.textContent.trim() : null;
    const paints = cs.backgroundColor !== 'rgba(0, 0, 0, 0)' || cs.boxShadow !== 'none' || cs.backgroundImage !== 'none' || text || icon || el.tagName === 'CANVAS' || el.tagName === 'IMG';
    if (!paints) continue;
    out.push({
      tag: el.tagName.toLowerCase(), text: icon ? undefined : text || undefined, icon: icon || undefined,
      x: +(b.left - fb.left).toFixed(2), y: +(b.top - fb.top).toFixed(2), w: +b.width.toFixed(2), h: +b.height.toFixed(2),
      bg: cs.backgroundColor, bgImage: cs.backgroundImage !== 'none' ? cs.backgroundImage : undefined, color: text || icon ? cs.color : undefined,
      font: text || icon ? { family: cs.fontFamily, size: cs.fontSize, weight: cs.fontWeight, lineHeight: cs.lineHeight, letterSpacing: cs.letterSpacing, variation: cs.fontVariationSettings, numeric: cs.fontVariantNumeric } : undefined,
      radius: cs.borderRadius !== '0px' ? cs.borderRadius : undefined, ring: cs.boxShadow !== 'none' ? cs.boxShadow : undefined,
      src: el.tagName === 'IMG' ? el.getAttribute('src') : undefined,
    });
  }
  return { frame: { w: fb.width, h: fb.height }, elements: out };
};

for (const d of cfg.devices) {
  if (onlyDev && d.name !== onlyDev) continue;
  const ctx = await browser.newContext({ viewport: { width: d.w, height: d.h }, deviceScaleFactor: d.density });
  const page = await ctx.newPage();
  for (const theme of cfg.themes) for (const s of cfg.screens) {
    if (only && !only.includes(s.id)) continue;
    const q = s.q.replace(/screen=([a-z0-9]+)/, (m, n) => 'screen=' + n + (theme === 'night' ? '-night' : ''));
    const url = `http://localhost:4173/Screenshot%20Rig.dc.html?${q}&w=${d.w}&h=${d.h}&bare=1&still=1`;
    await page.goto(url, { waitUntil: 'networkidle' });
    await page.evaluate(() => document.fonts.ready);
    await page.waitForTimeout(s.q.match(/find|hold|result|welcome|calibrate|direction/) ? 3500 : 600); // scrolling screens (s.scroll) are captured at full length // figure build + held frame
    const dir = path.join(root, 'reference', d.name, theme); await fs.mkdir(dir, { recursive: true });
    await page.locator('#shot').screenshot({ path: path.join(dir, s.id + '.png') });
    await fs.writeFile(path.join(dir, s.id + '.json'), JSON.stringify(await page.evaluate(SPEC), null, 1));
    console.log('✓', d.name, theme, s.id);
  }
  await ctx.close();
}
await browser.close(); server.close();
