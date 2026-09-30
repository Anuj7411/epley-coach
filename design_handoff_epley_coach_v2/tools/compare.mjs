// Pixel-diff the Android screenshots against the design references.
//   node compare.mjs --device pixel-412x915 --actual ../../app/build/outputs/roborazzi
// Expects actual files named <theme>/<id>.png (same names as reference/<device>/<theme>/).
// Writes ../reference/<device>/diff/<theme>-<id>.png and prints a table. Exit code 1 if any screen fails.
import fs from 'node:fs/promises';
import path from 'node:path';
import { PNG } from 'pngjs';
import pixelmatch from 'pixelmatch';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url)), root = path.resolve(here, '..');
const arg = (k, d) => { const i = process.argv.indexOf('--' + k); return i > 0 ? process.argv[i + 1] : d; };
const device = arg('device', 'pixel-412x915'), actualDir = path.resolve(arg('actual', '../actual'));
const LIMIT = Number(arg('limit', '0.5'));   // % of pixels allowed to differ (anti-aliasing noise)
const refDir = path.join(root, 'reference', device), diffDir = path.join(refDir, 'diff');
await fs.mkdir(diffDir, { recursive: true });
let fail = 0; const rows = [];
for (const theme of ['day', 'night']) {
  for (const f of (await fs.readdir(path.join(refDir, theme))).filter(n => n.endsWith('.png'))) {
    const ref = PNG.sync.read(await fs.readFile(path.join(refDir, theme, f)));
    let act; try { act = PNG.sync.read(await fs.readFile(path.join(actualDir, theme, f))); } catch { rows.push([theme, f, 'MISSING', '']); fail++; continue; }
    if (act.width !== ref.width || act.height !== ref.height) { rows.push([theme, f, 'SIZE', `${act.width}×${act.height} vs ${ref.width}×${ref.height}`]); fail++; continue; }
    const diff = new PNG({ width: ref.width, height: ref.height });
    const n = pixelmatch(ref.data, act.data, diff.data, ref.width, ref.height, { threshold: 0.1, includeAA: false });
    const pct = n / (ref.width * ref.height) * 100;
    await fs.writeFile(path.join(diffDir, `${theme}-${f}`), PNG.sync.write(diff));
    rows.push([theme, f, pct <= LIMIT ? 'PASS' : 'FAIL', pct.toFixed(2) + '%']); if (pct > LIMIT) fail++;
  }
}
console.table(rows.map(([t, f, s, v]) => ({ theme: t, screen: f, status: s, diff: v })));
console.log(fail ? `${fail} screen(s) above ${LIMIT}%` : 'All screens within limit');
process.exit(fail ? 1 : 0);
