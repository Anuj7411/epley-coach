// Prints "Doctor PDF.dc.html" the way a browser would (A4, its own margins) to reference/doctor-pdf.pdf.
import { chromium } from 'playwright';
import path from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
const here = path.dirname(fileURLToPath(import.meta.url));
const src = path.join(here, '..', 'Doctor PDF.dc.html');
const out = path.join(here, '..', 'reference', 'doctor-pdf.pdf');
const browser = await chromium.launch();
const page = await browser.newPage();
await page.goto(pathToFileURL(src).href, { waitUntil: 'networkidle' });
await page.evaluate(() => document.fonts.ready);
await page.waitForTimeout(1500);
await page.pdf({ path: out, format: 'A4', printBackground: true });
await browser.close();
console.log(out);
