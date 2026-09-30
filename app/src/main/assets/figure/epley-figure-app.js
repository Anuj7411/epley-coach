import * as THREE from './vendor/three.module.js';
import { buildBase, CREDIT, SHIRT_GLSL, meshHash, PH as PH8 } from './epley-figure-v8-base.js';
const V7_HEAD = { hash: '95bc4757', verts: 18097, tris: 35954, bbox: '-100.16,-210.44,-97.59,100.16,116.50,135.79' };
const V3 = THREE.Vector3;
const C = { ground:'#121311', skin:'#C4B5FF', shirt:'#9A89E0', cyan:'#7AD9E0', cyanEdge:'#BDF3F6', screen:'#5FC2CA', ear:'#1D4448', lid:'#6A5DAE', amber:'#FFC857', ink:'#F3F1EA', ref:'#8E8B83' };
const rad = d => d * Math.PI / 180, deg = r => r * 180 / Math.PI;
const clamp = (v, a = 0, b = 1) => Math.min(b, Math.max(a, v));
export const PRIMARY = { 1:'crown', 2:'profile', 3:'crown', 4:'crown', 5:'profile' };
// Authored for a right-affected ear (phone on right cheek, subject's right = -x). Left = exact mirror.
const POSES = [
  { recline:0,  pitch:0,   yaw:0,   roll:0 },
  { recline:0,  pitch:0,   yaw:-45, roll:0 },
  { recline:90, pitch:-25, yaw:-45, roll:0 },
  { recline:90, pitch:-25, yaw:45,  roll:0 },
  { recline:90, pitch:0,   yaw:45,  roll:90 },
  { recline:0,  pitch:0,   yaw:0,   roll:0 },
];
const GHOST = { 1:POSES[0], 2:{ ...POSES[2], pitch:0 }, 3:POSES[2], 4:POSES[3], 5:null };
let P = new V3(0, -0.19, -0.02);            // neck base pivot (on neck axis)
const PH = PH8; // 70 x 150 x 8 mm
const T = { FADE:0.35, HOLD0:1.0, MOVE:2.4, HOLD:4.6 };
const LOOP = T.FADE + T.HOLD0 + T.MOVE + T.HOLD + T.FADE;
const THEMES = {
  light: { skin:'#C9BCFF', shirt:'#9A89E0', band:'#8B79D4', phone:'#17161C', phoneEdge:'#3A3942', screen:'#A8EBD6', ear:'#17161C', contour:'#17161C', contourA:0.85, target:'#17161C', halo:'rgba(255,255,255,0.92)', ref:'#55535E', pillBg:'#17161C', pillFg:'#FFFFFF', ghost:'#17161C', ghostA:0.3, ghostFillA:0 },
  night: { skin:'#C9BCFF', shirt:'#9A89E0', band:'#8B79D4', phone:'#F1F0F5', phoneEdge:'#FFFFFF', screen:'#A8EBD6', ear:'#17161C', contour:'#111015', contourA:0.9, target:'#FFE38A', halo:'rgba(17,16,21,0.9)', ref:'#A9A7B3', pillBg:'#FFE38A', pillFg:'#17161C', ghost:'#C9BCFF', ghostA:0.55, ghostFillA:0.06 },
};
let PR = 1; // canvas px per CSS px, so small tiles keep legible labels and lines
const ease = u => 0.5 - 0.5 * Math.cos(Math.PI * clamp(u));

// ---------- SDF toolkit ----------
const smin = (a, b, k) => { const h = Math.max(k - Math.abs(a - b), 0) / k; return Math.min(a, b) - h * h * k * 0.25; };
const smax = (a, b, k) => -smin(-a, -b, k);
function ell(x, y, z, cx, cy, cz, rx, ry, rz) { const X = (x - cx) / rx, Y = (y - cy) / ry, Z = (z - cz) / rz; const k0 = Math.sqrt(X * X + Y * Y + Z * Z), k1 = Math.sqrt(X * X / (rx * rx) + Y * Y / (ry * ry) + Z * Z / (rz * rz)); return k1 < 1e-9 ? -Math.min(rx, ry, rz) : k0 * (k0 - 1) / k1; }
const sph = (x, y, z, cx, cy, cz, r) => Math.hypot(x - cx, y - cy, z - cz) - r;
function rcone(a, b, r1, r2) {
  const bx = b[0] - a[0], by = b[1] - a[1], bz = b[2] - a[2], l2 = bx * bx + by * by + bz * bz, rr = r1 - r2, a2 = l2 - rr * rr, il2 = 1 / l2;
  return (x, y, z) => { const px = x - a[0], py = y - a[1], pz = z - a[2], yy = px * bx + py * by + pz * bz, zz = yy - l2; const qx = px * l2 - bx * yy, qy = py * l2 - by * yy, qz = pz * l2 - bz * yy, x2 = qx * qx + qy * qy + qz * qz; const y2 = yy * yy * l2, z2 = zz * zz * l2, k = Math.sign(rr) * rr * rr * x2;
    if (Math.sign(zz) * a2 * z2 > k) return Math.sqrt(x2 + z2) * il2 - r2; if (Math.sign(yy) * a2 * y2 < k) return Math.sqrt(x2 + y2) * il2 - r1; return (Math.sqrt(x2 * a2 * il2) + yy * rr) * il2 - r1; };
}
function rotEll(c, r, euler) { const e = new THREE.Matrix4().makeRotationFromEuler(new THREE.Euler(...euler)).invert().elements; return (x, y, z) => { const dx = x - c[0], dy = y - c[1], dz = z - c[2]; return ell(e[0] * dx + e[4] * dy + e[8] * dz, e[1] * dx + e[5] * dy + e[9] * dz, e[2] * dx + e[6] * dy + e[10] * dz, 0, 0, 0, r[0], r[1], r[2]); }; }

// ---------- Head (head space: metres, y up, face +z, eye line y=0, crown +0.115, chin -0.115) ----------
const EAR_E = [rad(-15), rad(-18), 0];
const earO = rotEll([0.073, -0.022, -0.012], [0.0065, 0.032, 0.0175], EAR_E);
const earI = rotEll([0.0785, -0.026, -0.009], [0.005, 0.017, 0.010], EAR_E);
const jawC = rcone([0.051, -0.074, -0.004], [0.018, -0.104, 0.064], 0.011, 0.011);
const archC = rcone([0.059, -0.014, 0.042], [0.063, -0.012, 0.006], 0.0072, 0.0068);
const noseC = rcone([0, 0.004, 0.090], [0, -0.045, 0.104], 0.0065, 0.0095);
const earSDF = (x, y, z) => { const ax = Math.abs(x); return smax(earO(ax, y, z), -earI(ax, y, z), 0.003); };
const noseSDF = (x, y, z) => smin(smin(noseC(x, y, z), sph(x, y, z, 0, -0.048, 0.105, 0.0092), 0.007), ell(Math.abs(x), y, z, 0.011, -0.051, 0.094, 0.0085, 0.0065, 0.0075), 0.009);
function headSDF(x, y, z) {
  const ax = Math.abs(x);
  let d = ell(x, y, z, 0, 0.024, -0.004, 0.076, 0.092, 0.098);               // cranium
  d = smin(d, ell(x, y, z, 0, -0.036, 0.030, 0.060, 0.066, 0.062), 0.03);      // face mass
  d = smin(d, jawC(ax, y, z), 0.03);                                           // mandible
  d = smin(d, ell(x, y, z, 0, -0.103, 0.068, 0.022, 0.014, 0.014), 0.015);     // chin
  d = smin(d, ell(ax, y, z, 0.052, -0.018, 0.048, 0.018, 0.016, 0.022), 0.03);// zygoma body
  d = smin(d, archC(ax, y, z), 0.012);                                         // zygomatic arch
  d = smin(d, ell(ax, y, z, 0.056, -0.052, 0.022, 0.011, 0.026, 0.02), 0.022);  // masseter over the ramus
  d = smin(d, ell(x, y, z, 0, 0.016, 0.074, 0.052, 0.013, 0.022), 0.02);       // brow ridge
  d = smax(d, -sph(ax, y, z, 0.032, 0.0, 0.094, 0.019), 0.012);                // orbits
  d = smin(d, ell(ax, y, z, 0.032, -0.002, 0.074, 0.0155, 0.011, 0.011), 0.005);// closed lids
  d = smin(d, noseSDF(x, y, z), 0.008);
  d = smin(d, earSDF(x, y, z), 0.005);
  d = smin(d, Math.max(Math.hypot(x, z + 0.02) - 0.057, y + 0.03, -(y + 0.205)), 0.014); // neck
  return d;
}
// ---------- Shoulders + crew neck (same space) ----------
const shC = rcone([-0.16, -0.285, -0.025], [0.16, -0.285, -0.025], 0.052, 0.052);
const trC = rcone([0, -0.245, -0.025], [0.15, -0.28, -0.025], 0.05, 0.045);
function torsoSDF(x, y, z) {
  const ax = Math.abs(x); y -= 0.016;
  let d = smin(shC(x, y, z), ell(x, y, z, 0, -0.36, -0.015, 0.175, 0.13, 0.1), 0.06);
  d = smin(d, trC(ax, y, z), 0.04);
  d = smin(d, ell(x, y, z, 0, -0.235, -0.022, 0.1, 0.055, 0.09), 0.03);
  const r = Math.hypot(x, z + 0.02);
  d = smax(d, -Math.max(r - 0.062, -(y + 0.215)), 0.006);
  d = smin(d, Math.hypot(r - 0.0655, y + 0.206) - 0.0055, 0.004);
  return Math.max(d, -(y + 0.43));
}

// ---------- Mesher: surface nets on a narrow band, SDF-projected vertices, gradient normals ----------
function meshSDF(f, min, max, step) {
  const nx = Math.ceil((max[0] - min[0]) / step) + 1, ny = Math.ceil((max[1] - min[1]) / step) + 1, nz = Math.ceil((max[2] - min[2]) / step) + 1;
  const cs = step * 3, cnx = Math.ceil((max[0] - min[0]) / cs) + 2, cny = Math.ceil((max[1] - min[1]) / cs) + 2, cnz = Math.ceil((max[2] - min[2]) / cs) + 2, sxy = cnx * cny;
  const cv0 = new Float32Array(cnx * cny * cnz); let p = 0;
  for (let k = 0; k < cnz; k++) { const z = min[2] + k * cs; for (let j = 0; j < cny; j++) { const y = min[1] + j * cs; for (let i = 0; i < cnx; i++) cv0[p++] = f(min[0] + i * cs, y, z); } }
  const band = cs * 2.2, v = new Float32Array(nx * ny * nz); p = 0;
  for (let k = 0; k < nz; k++) { const z = min[2] + k * step, fz = k * step / cs, k0 = Math.min(Math.floor(fz), cnz - 2), tz = fz - k0;
    for (let j = 0; j < ny; j++) { const y = min[1] + j * step, fy = j * step / cs, j0 = Math.min(Math.floor(fy), cny - 2), ty = fy - j0;
      for (let i = 0; i < nx; i++) { const x = min[0] + i * step, fx = i * step / cs, i0 = Math.min(Math.floor(fx), cnx - 2), tx = fx - i0, b = i0 + cnx * j0 + sxy * k0;
        const c00 = cv0[b] + (cv0[b + 1] - cv0[b]) * tx, c10 = cv0[b + cnx] + (cv0[b + cnx + 1] - cv0[b + cnx]) * tx, c01 = cv0[b + sxy] + (cv0[b + sxy + 1] - cv0[b + sxy]) * tx, c11 = cv0[b + sxy + cnx] + (cv0[b + sxy + cnx + 1] - cv0[b + sxy + cnx]) * tx;
        const c0 = c00 + (c10 - c00) * ty, c1 = c01 + (c11 - c01) * ty, val = c0 + (c1 - c0) * tz;
        v[p++] = Math.abs(val) > band ? val : f(x, y, z);
      } } }
  const id = (i, j, k) => i + nx * (j + ny * k), cx = nx - 1, cy = ny - 1, cid = (i, j, k) => i + cx * (j + cy * k);
  const cellV = new Int32Array(cx * cy * (nz - 1)).fill(-1), pos = [], cv = new Float32Array(8);
  const E = [[0,1],[2,3],[4,5],[6,7],[0,2],[1,3],[4,6],[5,7],[0,4],[1,5],[2,6],[3,7]];
  for (let k = 0; k < nz - 1; k++) for (let j = 0; j < ny - 1; j++) for (let i = 0; i < nx - 1; i++) {
    let mask = 0; for (let c = 0; c < 8; c++) { const val = v[id(i + (c & 1), j + ((c >> 1) & 1), k + ((c >> 2) & 1))]; cv[c] = val; if (val < 0) mask |= 1 << c; }
    if (mask === 0 || mask === 255) continue;
    let sx = 0, sy = 0, sz = 0, n = 0;
    for (const [a, b] of E) { if (((mask >> a) & 1) === ((mask >> b) & 1)) continue; const t = cv[a] / (cv[a] - cv[b]); sx += (a & 1) + ((b & 1) - (a & 1)) * t; sy += ((a >> 1) & 1) + (((b >> 1) & 1) - ((a >> 1) & 1)) * t; sz += ((a >> 2) & 1) + (((b >> 2) & 1) - ((a >> 2) & 1)) * t; n++; }
    cellV[cid(i, j, k)] = pos.length / 3; pos.push(min[0] + (i + sx / n) * step, min[1] + (j + sy / n) * step, min[2] + (k + sz / n) * step);
  }
  const idx = [], quad = (a, b, c, d) => { a = cellV[a]; b = cellV[b]; c = cellV[c]; d = cellV[d]; if (a < 0 || b < 0 || c < 0 || d < 0) return; idx.push(a, b, c, a, c, d); };
  for (let k = 0; k < nz; k++) for (let j = 0; j < ny; j++) for (let i = 0; i < nx; i++) {
    const s0 = v[id(i, j, k)] < 0;
    if (i < nx - 1 && j > 0 && k > 0 && j < ny - 1 && k < nz - 1 && s0 !== (v[id(i + 1, j, k)] < 0)) quad(cid(i, j - 1, k - 1), cid(i, j, k - 1), cid(i, j, k), cid(i, j - 1, k));
    if (j < ny - 1 && i > 0 && k > 0 && i < nx - 1 && k < nz - 1 && s0 !== (v[id(i, j + 1, k)] < 0)) quad(cid(i - 1, j, k - 1), cid(i, j, k - 1), cid(i, j, k), cid(i - 1, j, k));
    if (k < nz - 1 && i > 0 && j > 0 && i < nx - 1 && j < ny - 1 && s0 !== (v[id(i, j, k + 1)] < 0)) quad(cid(i - 1, j - 1, k), cid(i, j - 1, k), cid(i, j, k), cid(i - 1, j, k));
  }
  const e = step * 0.5, nrm = new Float32Array(pos.length);
  const grad = (x, y, z) => { const gx = f(x + e, y, z) - f(x - e, y, z), gy = f(x, y + e, z) - f(x, y - e, z), gz = f(x, y, z + e) - f(x, y, z - e), l = Math.hypot(gx, gy, gz) || 1; return [gx / l, gy / l, gz / l]; };
  for (let q = 0; q < pos.length; q += 3) { let x = pos[q], y = pos[q + 1], z = pos[q + 2]; for (let it = 0; it < 3; it++) { const d = f(x, y, z), g = grad(x, y, z); x -= g[0] * d; y -= g[1] * d; z -= g[2] * d; } pos[q] = x; pos[q + 1] = y; pos[q + 2] = z; const g = grad(x, y, z); nrm[q] = g[0]; nrm[q + 1] = g[1]; nrm[q + 2] = g[2]; }
  for (let t = 0; t < idx.length; t += 3) { const a = idx[t] * 3, b = idx[t + 1] * 3, c = idx[t + 2] * 3;
    const ux = pos[b] - pos[a], uy = pos[b + 1] - pos[a + 1], uz = pos[b + 2] - pos[a + 2], wx = pos[c] - pos[a], wy = pos[c + 1] - pos[a + 1], wz = pos[c + 2] - pos[a + 2];
    const fx = uy * wz - uz * wy, fy = uz * wx - ux * wz, fz = ux * wy - uy * wx;
    if (fx * (nrm[a] + nrm[b] + nrm[c]) + fy * (nrm[a + 1] + nrm[b + 1] + nrm[c + 1]) + fz * (nrm[a + 2] + nrm[b + 2] + nrm[c + 2]) < 0) { const s = idx[t + 1]; idx[t + 1] = idx[t + 2]; idx[t + 2] = s; } }
  const g = new THREE.BufferGeometry(); g.setAttribute('position', new THREE.Float32BufferAttribute(pos, 3)); g.setAttribute('normal', new THREE.Float32BufferAttribute(nrm, 3)); g.setIndex(idx); g.computeBoundingSphere(); return g;
}
function roundedRect(w, h, r) { const s = new THREE.Shape(), x = -w / 2, y = -h / 2; s.moveTo(x + r, y); s.lineTo(x + w - r, y); s.absarc(x + w - r, y + r, r, -Math.PI / 2, 0); s.lineTo(x + w, y + h - r); s.absarc(x + w - r, y + h - r, r, 0, Math.PI / 2); s.lineTo(x + r, y + h); s.absarc(x + r, y + h - r, r, Math.PI / 2, Math.PI); s.lineTo(x, y + r); s.absarc(x + r, y + r, r, Math.PI, Math.PI * 1.5); return s; }

// ---------- Phone placement: flat plane resting on the right cheek ----------
const TOP_Y = 0.0; // outer canthus height
function earFrontZ() { let f = -1; for (let y = -0.056; y <= 0.012; y += 0.002) for (let z = 0.03; z > -0.04; z -= 0.0005) { let hit = false; for (let x = 0.064; x < 0.092; x += 0.001) if (earSDF(x, y, z) < 0) { hit = true; break; } if (hit) { f = Math.max(f, z); break; } } return f; }
function phoneBasis(a, b) { const n = new V3(-Math.cos(a), 0, Math.sin(a)), u = new V3(0, 1, 0), ax = new V3().crossVectors(u, n).normalize(); n.applyAxisAngle(ax, b); u.applyAxisAngle(ax, b); return { r: new V3().crossVectors(u, n), u, n }; }
const ppt = (c, B, lx, ly, lz) => new V3().copy(c).addScaledVector(B.r, lx).addScaledVector(B.u, ly).addScaledVector(B.n, lz);
function placePhone() {
  const EF = earFrontZ(), BACK_Z = EF + 0.007, GAP = 0.0006, S = [];
  for (let i = 0; i < 9; i++) for (let j = 0; j < 21; j++) S.push([(-1 + 2 * i / 8) * (PH.hw - 0.003), (-1 + 2 * j / 20) * (PH.hh - 0.003)]);
  const fit = B => { const c = new V3(-0.085, -0.075, 0.045);
    for (let it = 0; it < 10; it++) {
      c.y += TOP_Y - ppt(c, B, 0, PH.hh, -PH.hd).y;
      let mz = Infinity; for (const sx of [-1, 1]) for (const sy of [-1, 1]) mz = Math.min(mz, ppt(c, B, sx * PH.hw, sy * PH.hh, -PH.hd).z); c.z += BACK_Z - mz;
      let md = Infinity; for (const s of S) { const p = ppt(c, B, s[0], s[1], -PH.hd); md = Math.min(md, headSDF(p.x, p.y, p.z)); } c.addScaledVector(B.n, -(md - GAP));
    } return c; };
  let best = null;
  for (let a = 0; a <= 40; a += 2) for (let b = 0; b <= 8; b += 0.5) {
    const B = phoneBasis(rad(a), rad(b)), c = fit(B);
    let nose = Infinity; for (let j = 0; j <= 10; j++) { const p = ppt(c, B, PH.hw, (-1 + j / 5) * PH.hh, 0); nose = Math.min(nose, noseSDF(p.x, p.y, p.z)); }
    if (nose < 0.006) continue;
    let m = 0, k = 0, up = Infinity, lo = Infinity; for (const s of S) { const p = ppt(c, B, s[0], s[1], -PH.hd); if (p.y < -0.1) continue; const d = headSDF(p.x, p.y, p.z); if (p.y > -0.03) up = Math.min(up, d); else lo = Math.min(lo, d); if (p.y >= -0.085) { m += Math.min(d, 0.02); k++; } } const n = -Math.max(up, lo) * 10 - m / k;
    if (!best || n > best.n) best = { n, a, b, B, c };
  }
  const M = new THREE.Matrix4().makeBasis(best.B.r, best.B.u, best.B.n).setPosition(best.c);
  return { M, a: best.a, b: best.b, earFront: EF, samples: S };
}
function surfacePoint(x, y) { let z = 0.14; while (z > -0.12 && headSDF(x, y, z) > 0) z -= 0.0004; for (let i = 0; i < 6; i++) z += headSDF(x, y, z) * -0.5 + 0; let lo = z - 0.0008, hi = z + 0.0008; for (let i = 0; i < 20; i++) { const m = (lo + hi) / 2; if (headSDF(x, y, m) > 0) hi = m; else lo = m; } return new V3(x, y, (lo + hi) / 2); }

// ---------- Build ----------
export async function init(o) {
  const status = o.onStatus || (() => {}), tick = () => new Promise(r => setTimeout(r, 0));
  const BASE = await buildBase(status); await tick();
  const { geo: headGeo, rest, nrm, W, M: BM, Ph: BPh, PtoH } = BASE, MAT = BASE.MAT, NV = BASE.n;
  P = BM.P.clone();
  const fig = new THREE.Group(); fig.name = 'figure';
  const reclineG = new THREE.Group(); reclineG.position.copy(P); fig.add(reclineG);
  const rollG = new THREE.Group(); reclineG.add(rollG);
  const torso = new THREE.Mesh(BASE.torsoGeo, BASE.torsoMat); torso.name = 'shoulders_crew_neck'; torso.position.copy(P).negate(); rollG.add(torso);
  const pitchG = new THREE.Group(); rollG.add(pitchG);
  const yawG = new THREE.Group(); pitchG.add(yawG);
  const H = new THREE.Group(); H.position.copy(P).negate(); yawG.add(H);
  const head = new THREE.Mesh(headGeo, MAT.skin); head.name = 'head_neck_shoulders'; head.frustumCulled = false; fig.add(head);
  H.add(BASE.phone);
  // skinning: w=0 follows the shoulders, w=1 the head; in between the neck turns by a slerped fraction about the neck pivot
  const LV = 48, lvl = new Uint8Array(NV); for (let i = 0; i < NV; i++) lvl[i] = Math.round(W[i] * LV);
  const qI = new THREE.Quaternion(), qR = new THREE.Quaternion(), qk = new THREE.Quaternion(), mats = Array.from({ length: LV + 1 }, () => new THREE.Matrix4()), nmats = Array.from({ length: LV + 1 }, () => new THREE.Matrix3());
  const Tm = new THREE.Matrix4().makeTranslation(-P.x, -P.y, -P.z), Tp = new THREE.Matrix4().makeTranslation(P.x, P.y, P.z), Rk = new THREE.Matrix4(), Mt = new THREE.Matrix4(), figInv = new THREE.Matrix4();
  let skinKey = '';
  function skin(p) {
    const key = [p.recline, p.roll, p.pitch, p.yaw].map(v => v.toFixed(3)).join(); if (key === skinKey) return; skinKey = key;
    figInv.copy(fig.matrixWorld).invert(); Mt.multiplyMatrices(figInv, rollG.matrixWorld);   // shoulder frame (pivot at origin)
    qR.setFromEuler(new THREE.Euler(rad(p.pitch), 0, 0)).multiply(new THREE.Quaternion().setFromEuler(new THREE.Euler(0, rad(p.yaw), 0)));
    for (let k = 0; k <= LV; k++) { qk.slerpQuaternions(qI, qR, k / LV); Rk.makeRotationFromQuaternion(qk); mats[k].multiplyMatrices(Mt, Rk).multiply(Tm); nmats[k].getNormalMatrix(mats[k]); }
    const pa = headGeo.attributes.position.array, na = headGeo.attributes.normal.array;
    for (let i = 0; i < NV; i++) { const e = mats[lvl[i]].elements, n3 = nmats[lvl[i]].elements, j = i * 3, x = rest[j], y = rest[j + 1], z = rest[j + 2], nx = nrm[j], ny = nrm[j + 1], nz = nrm[j + 2];
      pa[j] = e[0] * x + e[4] * y + e[8] * z + e[12]; pa[j + 1] = e[1] * x + e[5] * y + e[9] * z + e[13]; pa[j + 2] = e[2] * x + e[6] * y + e[10] * z + e[14];
      const ox = n3[0] * nx + n3[3] * ny + n3[6] * nz, oy = n3[1] * nx + n3[4] * ny + n3[7] * nz, oz = n3[2] * nx + n3[5] * ny + n3[8] * nz, l = Math.hypot(ox, oy, oz) || 1; na[j] = ox / l; na[j + 1] = oy / l; na[j + 2] = oz / l; }
    headGeo.attributes.position.needsUpdate = true; headGeo.attributes.normal.needsUpdate = true;
  }
  const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true, preserveDrawingBuffer: false });
  renderer.setPixelRatio(1); renderer.setClearColor(0x000000, 0); renderer.outputColorSpace = THREE.SRGBColorSpace; renderer.shadowMap.enabled = false;
  const scene = new THREE.Scene(); scene.add(fig);
  const rig = new THREE.Group(); scene.add(rig);
  rig.add(new THREE.AmbientLight(C.ink, 0.5));
  const addDir = (p, i) => { const l = new THREE.DirectionalLight(C.ink, i); l.position.set(...p); l.target.position.set(0, 0, -3); rig.add(l, l.target); };
  addDir([-1.2, 1.4, 1.6], 1.1); addDir([1.6, -0.6, 1.0], 0.28); addDir([0.8, 1.2, -6], 0.55);
  const white = new THREE.MeshBasicMaterial({ color: 0xffffff }), depthOnly = new THREE.MeshBasicMaterial({ colorWrite: false });
  const saved = new Map();
  const headMask = new THREE.ShaderMaterial({ side: THREE.DoubleSide, uniforms: { uCut: { value: BASE.cutY }, uCol: { value: new THREE.Vector3(BASE.collar.y, BASE.collar.z, BASE.collar.t) } },
    vertexShader: 'attribute float aW; attribute vec3 aRest; varying float vW; varying float vY; varying float vZ; varying float vX; void main(){ vW = aW; vX = aRest.x; vY = aRest.y; vZ = aRest.z; gl_Position = projectionMatrix * modelViewMatrix * vec4(position,1.0); }',
    fragmentShader: 'uniform float uCut; uniform vec3 uCol; varying float vW; varying float vY; varying float vZ; varying float vX;\n' + SHIRT_GLSL + 'void main(){ if (shirtM(vec3(vX, vY, vZ)) > 0.0) discard; gl_FragColor = vec4(1.0); }' });
  const maskOn = () => { torso.renderOrder = -1; fig.traverse(m => { if (m.isMesh) { saved.set(m, m.material); m.material = m === head ? headMask : m === torso ? depthOnly : white; } }); };
  const maskOff = () => { torso.renderOrder = 0; for (const [m, mat] of saved) m.material = mat; saved.clear(); };

  let mirror = 1;
  const setEar = ear => { mirror = ear === 'L' ? -1 : 1; fig.scale.x = mirror; };
  const setPose = p => { reclineG.rotation.x = -rad(p.recline); rollG.rotation.y = rad(p.roll); pitchG.rotation.x = rad(p.pitch); yawG.rotation.y = rad(p.yaw); fig.updateMatrixWorld(true); skin(p); };
  const dirW = (obj, x, y, z) => new V3(x, y, z).transformDirection(obj.matrixWorld);
  const PTS = []; for (let i = 0; i < NV; i += 7) if (W[i] > 0.97 && rest[i * 3 + 1] > BM.chin - 0.01) PTS.push(new V3(rest[i * 3], rest[i * 3 + 1], rest[i * 3 + 2])); for (const sx of [-1, 1]) for (const sy of [-1, 1]) PTS.push(new V3(sx * PH.hw, sy * PH.hh, 0).applyMatrix4(PtoH));
  const cams = [];
  function basisFor(view) {
    let Z, up;
    if (view === 'profile') { Z = new V3(-mirror, 0, 0); up = new V3(0, 1, 0); }
    else if (view === 'front') { Z = new V3(0, 0, 1); up = new V3(0, 1, 0); }
    else { Z = dirW(pitchG, 0, 1, 0); up = Math.abs(Z.y) > 0.7 ? dirW(pitchG, 0, 0, 1) : new V3(0, 1, 0); }
    const X = new V3().crossVectors(up, Z).normalize(), Y = new V3().crossVectors(Z, X); return { X, Y, Z };
  }
  function ext(pose, B) { setPose(pose); const m = H.matrixWorld, w = new V3(); let x0 = Infinity, x1 = -Infinity, y0 = Infinity, y1 = -Infinity;
    for (const p of PTS) { w.copy(p).applyMatrix4(m); const a = w.dot(B.X), b = w.dot(B.Y); x0 = Math.min(x0, a); x1 = Math.max(x1, a); y0 = Math.min(y0, b); y1 = Math.max(y1, b); }
    return { x0, x1, y0, y1, d: new V3().setFromMatrixPosition(m).dot(B.Z) }; }
  function mkCam(B, c3, half) { const cam = new THREE.OrthographicCamera(-half, half, half, -half, 0.01, 8); cam.up.copy(B.Y); cam.position.copy(c3).addScaledVector(B.Z, 3); cam.lookAt(c3); cam.updateMatrixWorld(); cams.push(cam); return cam; }
  const frames = {};
  function frameFor(ear, step, view) {
    const key = ear + step + view; if (frames[key]) return frames[key];
    setEar(ear); const end = POSES[step], start = POSES[step - 1], gh = view === PRIMARY[step] ? GHOST[step] : null;
    setPose(end); const B = basisFor(view);
    const e = ext(end, B), s = step === 5 ? e : ext(start, B), g = gh ? ext(gh, B) : e;
    const ux0 = Math.min(e.x0, s.x0, g.x0), ux1 = Math.max(e.x1, s.x1, g.x1), uy0 = Math.min(e.y0, s.y0, g.y0), uy1 = Math.max(e.y1, s.y1, g.y1);
    let half = Math.max(0.235, Math.max(ux1 - ux0, uy1 - uy0) / 2 + 0.02); const eh = Math.max(e.x1 - e.x0, e.y1 - e.y0); half = Math.min(half, eh / (2 * 0.46));
    setPose(end); const td = dirW(rollG, 0, -1, 0);
    let cx = (ux0 + ux1) / 2 + td.dot(B.X) * 0.03, cy = (uy0 + uy1) / 2 + td.dot(B.Y) * 0.03; const m = 0.012;
    cx = clamp(cx, e.x1 + m - half, e.x0 - m + half); cy = clamp(cy, e.y1 + m - half, e.y0 - m + half);
    const c3 = new V3().addScaledVector(B.X, cx).addScaledVector(B.Y, cy).addScaledVector(B.Z, e.d);
    const fr = { key, ear, step, view, B, c3, half, cam: mkCam(B, c3, half), ghost: gh, start, end, share: eh / (2 * half) };
    fr.ov = overlays(fr); frames[key] = fr; return fr;
  }
  const proj = (fr, w) => { const d = w.clone().sub(fr.c3); return [d.dot(fr.B.X) / (2 * fr.half) + 0.5, 0.5 - d.dot(fr.B.Y) / (2 * fr.half)]; };
  function overlays(fr) {
    setEar(fr.ear); setPose(fr.end); const out = [], st = fr.step;
    const line = (a, b, kind) => out.push({ t: 'line', a: proj(fr, a), b: proj(fr, b), kind });
    const arc = (O, da, db, R, LR, label) => { const o = proj(fr, O), pa = proj(fr, O.clone().addScaledVector(da, R)), pb = proj(fr, O.clone().addScaledVector(db, R));
      out.push({ t: 'arc', c: o, r: R / (2 * fr.half), lr: LR / (2 * fr.half), a0: Math.atan2(pa[1] - o[1], pa[0] - o[0]), a1: Math.atan2(pb[1] - o[1], pb[0] - o[0]), label }); };
    if (fr.view === 'crown') {
      const O = new V3(0, 0, P.z).applyMatrix4(H.matrixWorld), nose = dirW(yawG, 0, 0, 1), fwd = dirW(pitchG, 0, 0, 1), ray = (d, L) => O.clone().addScaledVector(d, L);
      if (st === 1 || st === 2) { line(O, ray(fwd, 0.2), 'ref'); line(O, ray(nose, 0.2), 'target'); arc(O, fwd, nose, 0.15, 0.2, '45°'); }
      else if (st === 3) { setPose(POSES[2]); const n0 = dirW(yawG, 0, 0, 1); setPose(fr.end); line(O, ray(fwd, 0.2), 'ref'); line(O, ray(nose, 0.2), 'target'); arc(O, n0, nose, 0.15, 0.2, '90°'); }
      else if (st === 4) { const h = fr.B.X.clone().multiplyScalar(Math.sign(nose.dot(fr.B.X)) || 1); line(ray(h, -0.21), ray(h, 0.21), 'ref'); line(O, ray(nose, 0.2), 'target'); arc(O, h, nose, 0.15, 0.2, '45°'); }
      else line(O, ray(nose, 0.2), 'target');
    } else if (fr.view === 'profile') {
      const O = new V3().setFromMatrixPosition(reclineG.matrixWorld), ax = dirW(pitchG, 0, 1, 0), up = new V3(0, 1, 0);
      if (st === 1 || st === 5) line(O.clone().addScaledVector(up, -0.02), O.clone().addScaledVector(up, 0.36), 'target');
      else if (st === 2 || st === 3) { const h = new V3(0, 0, Math.sign(ax.z) || -1); line(O, O.clone().addScaledVector(h, 0.36), 'ref'); line(O, O.clone().addScaledVector(ax, 0.36), 'target'); arc(O, h, ax, 0.12, 0.155, '25°'); { const it = out[out.length - 1]; it.out = false; it.fixed = proj(fr, O.clone().addScaledVector(ax, 0.3).add(new V3(0, -0.06, 0))); } }
    }
    return out;
  }
  // ---------- drawing ----------
  const ringC = document.createElement('canvas'), rctx = ringC.getContext('2d'), fadeC = document.createElement('canvas'), fctx = fadeC.getContext('2d');
  function drawOverlay(ctx, S, ov, th) {
    const lwR = Math.max(1.3 * PR, S / 300), lwT = Math.max(1.8 * PR, S / 190), dash = [S * 0.02, S * 0.013];
    ctx.lineCap = 'butt';
    for (const it of ov) {
      if (it.t === 'line') {
        ctx.beginPath(); ctx.moveTo(it.a[0] * S, it.a[1] * S); ctx.lineTo(it.b[0] * S, it.b[1] * S);
        if (it.kind === 'ref') { ctx.setLineDash([]); ctx.strokeStyle = th.ref; ctx.lineWidth = lwR; ctx.stroke(); }
        else { ctx.setLineDash(dash); ctx.strokeStyle = th.halo; ctx.lineWidth = lwT + Math.max(2 * PR, S / 260); ctx.stroke(); ctx.strokeStyle = th.target; ctx.lineWidth = lwT; ctx.stroke(); }
      } else {
        let d = it.a1 - it.a0; while (d > Math.PI) d -= 2 * Math.PI; while (d < -Math.PI) d += 2 * Math.PI;
        ctx.setLineDash([]); ctx.beginPath(); ctx.arc(it.c[0] * S, it.c[1] * S, it.r * S, it.a0, it.a0 + d, d < 0);
        ctx.strokeStyle = th.halo; ctx.lineWidth = lwT + Math.max(2 * PR, S / 260); ctx.stroke(); ctx.strokeStyle = th.target; ctx.lineWidth = lwT; ctx.stroke();
        const am = it.out ? it.a0 - Math.sign(d) * 0.42 : it.a0 + d / 2; let lx = it.fixed ? it.fixed[0] * S : it.c[0] * S + Math.cos(am) * it.lr * S; let ly = it.fixed ? it.fixed[1] * S : it.c[1] * S + Math.sin(am) * it.lr * S; const fs = Math.max(Math.round(14 * PR), Math.round(S * 0.046));
        ctx.font = `800 ${fs}px "Bricolage Grotesque",system-ui,sans-serif`; const tw = ctx.measureText(it.label).width, pw = tw + fs * 0.7, phh = fs * 1.35;
        { const m = 4 * PR; lx = Math.min(S - pw / 2 - m, Math.max(pw / 2 + m, lx)); ly = Math.min(S - phh / 2 - m, Math.max(phh / 2 + m, ly)); }
        ctx.fillStyle = th.pillBg; ctx.beginPath(); ctx.roundRect(lx - pw / 2, ly - phh / 2, pw, phh, phh / 2); ctx.fill();
        ctx.fillStyle = th.pillFg; ctx.textAlign = 'center'; ctx.textBaseline = 'middle'; ctx.fillText(it.label, lx, ly + fs * 0.04);
      }
    }
    ctx.setLineDash([]);
  }
  let lastTheme = null;
  function applyTheme(th) { if (th === lastTheme) return; lastTheme = th; MAT.skin.color.set(th.skin); BASE.U.uShirt.value.set(th.shirt); BASE.U.uBand.value.set(th.band); BASE.torsoMat.color.set(th.shirt); MAT.phone.color.set(th.phone); MAT.phone.emissive.set(th.phone); MAT.edge.color.set(th.phoneEdge); MAT.edge.emissive.set(th.phoneEdge); MAT.screen.color.set(th.screen); MAT.screen.emissive.set(th.screen); MAT.earpiece.color.set(th.ear); }
  function drawFrame(ctx, S, fr, pose, ga = 1, fade = 1, th = THEMES.light) {
    applyTheme(th);
    setEar(fr.ear); renderer.setSize(S, S, false);
    const cam = fr.cam; rig.position.copy(cam.position); rig.quaternion.copy(cam.quaternion); rig.updateMatrixWorld(true);
    ctx.globalAlpha = 1; ctx.globalCompositeOperation = 'source-over'; ctx.clearRect(0, 0, S, S);
    if (fr.ghost && ga > 0) {
      setPose(fr.ghost); torso.visible = false; fig.traverse(m => { if (m.isMesh) { saved.set(m, m.material); m.material = m === head ? headMask : white; } }); renderer.render(scene, cam); for (const [m, mat] of saved) m.material = mat; saved.clear(); torso.visible = true;
      ringC.width = S; ringC.height = S; const w = Math.max(1.6 * PR, S / 300), src = renderer.domElement;
      for (let k = 0; k < 12; k++) rctx.drawImage(src, Math.cos(k * Math.PI / 6) * w, Math.sin(k * Math.PI / 6) * w, S, S);
      rctx.globalCompositeOperation = 'source-in'; rctx.fillStyle = th.ghost; rctx.fillRect(0, 0, S, S);
      rctx.globalCompositeOperation = 'destination-out'; rctx.drawImage(src, 0, 0, S, S); rctx.globalCompositeOperation = 'source-over';
      if (th.ghostFillA) { ctx.globalAlpha = th.ghostFillA * ga; ctx.drawImage(src, 0, 0, S, S); } // (white silhouette, faint fill)
      ctx.globalAlpha = th.ghostA * ga; ctx.drawImage(ringC, 0, 0); ctx.globalAlpha = 1;
    }
    setPose(pose); renderer.render(scene, cam); ctx.drawImage(renderer.domElement, 0, 0, S, S);
    // contour: dark ring around head + phone so they separate from the shoulders behind
    maskOn(); renderer.render(scene, cam); maskOff();
    { const m = fadeC; m.width = S; m.height = S; const g = fctx.createRadialGradient(S / 2, S / 2, S * 0.26, S / 2, S / 2, S * 0.5);
      g.addColorStop(0, 'rgba(0,0,0,1)'); g.addColorStop(0.55, 'rgba(0,0,0,0.55)'); g.addColorStop(1, 'rgba(0,0,0,0)');
      fctx.fillStyle = g; fctx.fillRect(0, 0, S, S); fctx.drawImage(renderer.domElement, 0, 0, S, S);   // head + phone stay fully opaque
      ctx.globalCompositeOperation = 'destination-in'; ctx.drawImage(m, 0, 0); ctx.globalCompositeOperation = 'source-over'; }
    { ringC.width = S; ringC.height = S; const w = Math.max(1.5 * PR, S / 240), src = renderer.domElement;
      for (let k = 0; k < 12; k++) rctx.drawImage(src, Math.cos(k * Math.PI / 6) * w, Math.sin(k * Math.PI / 6) * w, S, S);
      rctx.globalCompositeOperation = 'source-in'; rctx.fillStyle = th.contour; rctx.fillRect(0, 0, S, S);
      rctx.globalCompositeOperation = 'destination-out'; rctx.drawImage(src, 0, 0, S, S); rctx.globalCompositeOperation = 'source-over';
      ctx.globalAlpha = th.contourA; ctx.drawImage(ringC, 0, 0); ctx.globalAlpha = 1; }
    drawOverlay(ctx, S, fr.ov, th);
    if (fade < 1) { ctx.globalCompositeOperation = 'destination-out'; ctx.fillStyle = `rgba(0,0,0,${1 - fade})`; ctx.fillRect(0, 0, S, S); ctx.globalCompositeOperation = 'source-over'; }
  }
  const lerpPose = (a, b, u) => { const r = {}; for (const k in a) r[k] = a[k] + (b[k] - a[k]) * u; return r; };

  // ---------- multi-view (app) : one build, many canvases ----------
  const reduced = matchMedia('(prefers-reduced-motion: reduce)');
  const views = new Set();
  function phase(t) { let fade = 1, u = 0; if (t < T.FADE) fade = t / T.FADE; else if (t < T.FADE + T.HOLD0) u = 0; else if (t < T.FADE + T.HOLD0 + T.MOVE) u = (t - T.FADE - T.HOLD0) / T.MOVE; else u = 1; if (t > LOOP - T.FADE) fade = (LOOP - t) / T.FADE; return { u: ease(u), fade: clamp(fade), ga: clamp((t - (T.FADE + T.HOLD0 + T.MOVE)) / 0.3) }; }
  function render(v) {
    const c = v.canvas; if (!c.isConnected) { views.delete(v); return; }
    const w = c.clientWidth; if (!w) return;
    const S = Math.min(900, Math.max(96, Math.round(w * Math.max(2, devicePixelRatio || 1))));
    if (c.width !== S) { c.width = S; c.height = S; v.lastKey = null; }
    const animate = v.playing && !reduced.matches;
    const st = animate ? phase(((performance.now() - v.t0) / 1000) % LOOP) : { u: 1, fade: 1, ga: 1 };
    const view = v.view || PRIMARY[v.step];
    const key = [v.ear, v.step, view, S, st.u.toFixed(4), st.fade.toFixed(3), st.ga.toFixed(3)].join('|');
    if (key === v.lastKey) return; v.lastKey = key;          // held frame: nothing re-renders
    const fr = frameFor(v.ear, v.step, view); PR = S / w;
    drawFrame(c.getContext('2d'), S, fr, lerpPose(fr.start, fr.end, st.u), st.ga, st.fade, THEMES[v.theme] || THEMES.light);
  }
  let raf = 0; const loop = () => { raf = requestAnimationFrame(loop); for (const v of views) render(v); }; loop();
  const invalidate = () => { for (const v of views) v.lastKey = null; };
  reduced.addEventListener?.('change', invalidate);
  document.fonts?.load('800 32px "Bricolage Grotesque"').then(invalidate, () => {});
  document.fonts?.ready.then(invalidate);
  status('');
  return {
    credit: CREDIT,
    attach(canvas, opts = {}) {
      const v = { canvas, ear: 'R', step: 1, view: null, playing: true, t0: performance.now(), lastKey: null, ...opts };
      views.add(v);
      return {
        set(n) { const moved = (n.theme ?? v.theme) !== v.theme || (n.ear ?? v.ear) !== v.ear || (n.step ?? v.step) !== v.step || (n.view ?? v.view) !== v.view; Object.assign(v, n); if (moved) v.t0 = performance.now(); v.lastKey = null; },
        replay() { v.t0 = performance.now(); v.lastKey = null; },
        dispose() { views.delete(v); },
      };
    },
  };
}
let _fig = null;
export function getFigure(onStatus) { return _fig || (_fig = init({ onStatus })); }
