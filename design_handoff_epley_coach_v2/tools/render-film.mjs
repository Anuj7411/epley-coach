// Renders video/film.html frame by frame (deterministic seek) — stills or an encoded segment.
//   node render-film.mjs stills 0.5,3,10            -> video/review/t_<t>.png
//   node render-film.mjs video <from> <to> <out.mp4>  (60 fps, piped to ffmpeg)
import { chromium } from 'playwright';
import handler from 'serve-handler';
import http from 'node:http';
import path from 'node:path';
import fs from 'node:fs/promises';
import { spawn } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(here, '../../video');
const port = +(process.env.PORT || 4300);
const server = http.createServer((q, r) => handler(q, r, { public: root, cleanUrls: false }));
await new Promise(r => server.listen(port, r));
const browser = await chromium.launch({ args: ['--force-color-profile=srgb', '--disable-gpu-vsync'] });
const page = await (await browser.newContext({ viewport: { width: 1920, height: 1080 }, deviceScaleFactor: +(process.env.SCALE || 1) })).newPage();
page.on('pageerror', e => console.error('pageerror:', e.message));
page.on('console', m => { if (m.type() === 'error') console.error('console:', m.text()); });
await page.goto(`http://localhost:${port}/film.html`, { waitUntil: 'networkidle' });
await page.evaluate(() => window.ready);
const [mode, a, b, out] = process.argv.slice(2);
if (mode === 'stills') {
  await fs.mkdir(path.join(root, 'review'), { recursive: true });
  for (const t of a.split(',').map(Number)) {
    await page.evaluate(([t]) => window.seekTo(t), [t]);
    await page.screenshot({ path: path.join(root, 'review', `t_${t.toFixed(2)}.png`) });
  }
} else {
  const FPS = +(process.env.FPS || 60), from = Math.round(+a * FPS), to = Math.round(+b * FPS);
  const ff = spawn('ffmpeg', ['-v', 'error', '-y', '-f', 'image2pipe', '-framerate', String(FPS), '-c:v', 'png', '-i', '-',
    '-c:v', 'libx264', '-preset', 'medium', '-crf', '14', '-pix_fmt', 'yuv420p', '-color_primaries', 'bt709', '-color_trc', 'bt709', '-colorspace', 'bt709', out], { stdio: ['pipe', 'inherit', 'inherit'] });
  for (let f = from; f < to; f++) {
    await page.evaluate(([t, f]) => window.seekTo(t, f), [f / FPS, Math.round(f * 60 / FPS)]);
    const buf = await page.screenshot({ type: 'png' });
    if (!ff.stdin.write(buf)) await new Promise(r => ff.stdin.once('drain', r));
    if (f % 300 === 0) console.log('frame', f, '/', to);
  }
  ff.stdin.end(); await new Promise(r => ff.on('close', r));
}
await browser.close(); server.close();
