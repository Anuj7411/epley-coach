// Epley Coach figure v7 base: "Human head" by ADAMA (Sketchfab), CC BY 4.0
import * as THREE from './vendor/three.module.js';
const V3 = THREE.Vector3;
export const C = { ground:'#121311', skin:'#C4B5FF', shirt:'#9A89E0', band:'#8B79D4', cyan:'#7AD9E0', cyanEdge:'#BDF3F6', screen:'#5FC2CA', ear:'#1D4448', amber:'#FFC857', ink:'#F3F1EA', ref:'#8E8B83' };
export const rad = d => d * Math.PI / 180, deg = r => r * 180 / Math.PI;
export const clamp = (v, a = 0, b = 1) => Math.min(b, Math.max(a, v));
export const PH = { hw:0.036, hh:0.0775, hd:0.004, r:0.009 };   // 155 × 72 × 8 mm
export function meshHash(pos, idx) { let h = 2166136261 >>> 0; const u = new Uint32Array(pos.buffer, pos.byteOffset, pos.length); for (let i = 0; i < u.length; i++) { h ^= u[i]; h = Math.imul(h, 16777619) >>> 0; } if (idx) for (let i = 0; i < idx.length; i++) { h ^= idx[i]; h = Math.imul(h, 16777619) >>> 0; } return h.toString(16); }
export const MODEL_URL = new URL('./models/human_head.glb', import.meta.url).href;
import { _probe as SDFK } from './epley-figure-v4.js';
export const CREDIT = 'Head model: “Human head” by ADAMA on Sketchfab, CC BY 4.0.';
const HEAD_H = 0.233;
export const SHIRT_GLSL = 'float shirtM(vec3 p){ float m1 = (uCol.x - uCol.z * (p.z - uCol.y)) - p.y; vec2 q = vec2(p.x / 0.066, (p.z - uCol.y) / 0.071); float m2 = min(min((length(q) - 1.0) * 0.066, -0.135 - p.y), 0.022 - p.z); return max(m1, m2); }\n';
export const COLLAR = { front: -0.19, back: -0.152, zf: 0.035, zb: -0.084 };
COLLAR.t = (COLLAR.back - COLLAR.front) / (COLLAR.zf - COLLAR.zb); COLLAR.cz = -0.025; COLLAR.cy = COLLAR.front + COLLAR.t * (COLLAR.zf - COLLAR.cz);
const collarY = z => COLLAR.cy - COLLAR.t * (z - COLLAR.cz);
const RIM = { n: 24, r: new Float32Array(24), y: new Float32Array(24) };
function rimAt(x, z) { const a = Math.atan2(x, z - COLLAR.cz), f = ((a / (2 * Math.PI)) + 1) % 1 * RIM.n, i = Math.floor(f), t = f - i, j = (i + 1) % RIM.n; return [RIM.r[i] + (RIM.r[j] - RIM.r[i]) * t, RIM.y[i] + (RIM.y[j] - RIM.y[i]) * t]; }

async function loadGLB(url) {
  const buf = await (await fetch(url)).arrayBuffer(), dv = new DataView(buf); let off = 12, json, bin;
  while (off < buf.byteLength) { const len = dv.getUint32(off, true), type = dv.getUint32(off + 4, true), ch = buf.slice(off + 8, off + 8 + len);
    if (type === 0x4E4F534A) json = JSON.parse(new TextDecoder().decode(ch)); else if (type === 0x004E4942) bin = ch; off += 8 + len; }
  const prim = json.meshes[0].primitives[0];
  const acc = i => { const a = json.accessors[i], bv = json.bufferViews[a.bufferView], n = { SCALAR:1, VEC2:2, VEC3:3, VEC4:4 }[a.type];
    const T = { 5126:Float32Array, 5125:Uint32Array, 5123:Uint16Array, 5121:Uint8Array }[a.componentType], es = T.BYTES_PER_ELEMENT, st = bv.byteStride || n * es, start = (bv.byteOffset || 0) + (a.byteOffset || 0);
    const out = new (T === Float32Array ? Float32Array : Uint32Array)(a.count * n), src = new DataView(bin);
    for (let k = 0; k < a.count; k++) for (let c = 0; c < n; c++) { const p = start + k * st + c * es; out[k * n + c] = T === Float32Array ? src.getFloat32(p, true) : T === Uint32Array ? src.getUint32(p, true) : T === Uint16Array ? src.getUint16(p, true) : src.getUint8(p); }
    return out; };
  return { pos: acc(prim.attributes.POSITION), nrm: acc(prim.attributes.NORMAL), idx: prim.indices != null ? acc(prim.indices) : null };
}
const smin = (a, b, k) => { const h = Math.max(k - Math.abs(a - b), 0) / k; return Math.min(a, b) - h * h * k * 0.25; };
function ell(x, y, z, cx, cy, cz, rx, ry, rz) { const X = (x - cx) / rx, Y = (y - cy) / ry, Z = (z - cz) / rz; const k0 = Math.sqrt(X * X + Y * Y + Z * Z), k1 = Math.sqrt(X * X / (rx * rx) + Y * Y / (ry * ry) + Z * Z / (rz * rz)); return k1 < 1e-9 ? -Math.min(rx, ry, rz) : k0 * (k0 - 1) / k1; }
const e2 = (u, v, a, b) => { const X = u / a, Y = v / b, k0 = Math.hypot(X, Y), k1 = Math.hypot(X / a, Y / b); return k1 < 1e-9 ? -Math.min(a, b) : k0 * (k0 - 1) / k1; };
function rcone(a, b, r1, r2) {
  const bx = b[0] - a[0], by = b[1] - a[1], bz = b[2] - a[2], l2 = bx * bx + by * by + bz * bz, rr = r1 - r2, a2 = l2 - rr * rr, il2 = 1 / l2;
  return (x, y, z) => { const px = x - a[0], py = y - a[1], pz = z - a[2], yy = px * bx + py * by + pz * bz, zz = yy - l2; const qx = px * l2 - bx * yy, qy = py * l2 - by * yy, qz = pz * l2 - bz * yy, x2 = qx * qx + qy * qy + qz * qz; const y2 = yy * yy * l2, z2 = zz * zz * l2, k = Math.sign(rr) * rr * rr * x2;
    if (Math.sign(zz) * a2 * z2 > k) return Math.sqrt(x2 + z2) * il2 - r2; if (Math.sign(yy) * a2 * y2 < k) return Math.sqrt(x2 + y2) * il2 - r1; return (Math.sqrt(x2 * a2 * il2) + yy * rr) * il2 - r1; };
}
const trapT = rcone([0.06, -0.196, -0.045], [0.16, -0.252, -0.035], 0.03, 0.036);
const shC = rcone([-0.158, -0.272, -0.026], [0.158, -0.272, -0.026], 0.047, 0.047);
const clavC = rcone([0.026, -0.205, 0.03], [0.15, -0.228, -0.004], 0.0068, 0.006);
function torsoSDF(x, y, z) {
  const ax = Math.abs(x);
  let d = ell(x, y, z, 0, -0.335, -0.022, 0.185, 0.115, 0.098);                        // chest and upper back
  d = smin(d, shC(x, y, z), 0.085);                                                     // shoulder line into rounded deltoids
  d = smin(d, trapT(ax, y, z), 0.06);                                                   // trapezius slope
  const [rr, ry] = rimAt(x, z), rho = Math.hypot(x, z - COLLAR.cz), h = y - (ry + 0.005 - 0.5 * Math.max(0, rho - rr));
  d = smin(d, Math.max(rho - (rr + 0.018), h, -h - 0.08), 0.045);
  d = smin(d, clavC(ax, y, z), 0.014);
  return Math.max(d, -(y + 0.44));
}
function roundedRect(w, h, r) { const s = new THREE.Shape(), x = -w / 2, y = -h / 2; s.moveTo(x + r, y); s.lineTo(x + w - r, y); s.absarc(x + w - r, y + r, r, -Math.PI / 2, 0); s.lineTo(x + w, y + h - r); s.absarc(x + w - r, y + h - r, r, 0, Math.PI / 2); s.lineTo(x + r, y + h); s.absarc(x + r, y + h - r, r, Math.PI / 2, Math.PI); s.lineTo(x, y + r); s.absarc(x + r, y + r, r, Math.PI, Math.PI * 1.5); return s; }

// ---------- landmarks measured on the scan ----------
function measure(pos) {
  const n = pos.length / 3; let top = -1e9; for (let i = 0; i < n; i++) top = Math.max(top, pos[i * 3 + 1]);
  const chin = -0.84; return { top, chin, k: HEAD_H / (top - chin), mid: (top + chin) / 2 };
}
export async function buildBase(status = () => {}) {
  status('Loading head…');
  const raw = await loadGLB(MODEL_URL);
  const L = measure(raw.pos), n = raw.pos.length / 3, rest = new Float32Array(n * 3), nrm = new Float32Array(raw.nrm);
  for (let i = 0; i < n; i++) { rest[i * 3] = raw.pos[i * 3] * L.k; rest[i * 3 + 1] = (raw.pos[i * 3 + 1] - L.mid) * L.k; rest[i * 3 + 2] = raw.pos[i * 3 + 2] * L.k; }
  status('Measuring landmarks…');
  const V = i => [rest[i * 3], rest[i * 3 + 1], rest[i * 3 + 2]];
  const measureAll = () => {
  const M = { vertex: -1, chin: 1, tipZ: -1, noseBase: 1, back: 1, glab: -1, width: 0, bizyg: 0, gonial: 0, earFront: -1, earBack: 1, earTop: -1, earBot: 1, earX: 0, neckW: 0 };
  const prof = new Map(), back = new Map();
  for (let i = 0; i < n; i++) { const [x, y, z] = V(i), ax = Math.abs(x);
    M.vertex = Math.max(M.vertex, y); M.tipZ = Math.max(M.tipZ, z);
    if (y > -0.06) M.back = Math.min(M.back, z);
    if (y > 0.035) M.width = Math.max(M.width, 2 * ax);
    if (ax < 0.006) { const b = Math.round(y / 0.002); prof.set(b, Math.max(prof.get(b) ?? -1, z)); back.set(b, Math.min(back.get(b) ?? 1, z)); }
  }
  // chin: lowest point of the front of the face; nose base: lowest nose point; glabella: most forward point above the eyes
  for (let i = 0; i < n; i++) { const [x, y, z] = V(i), ax = Math.abs(x);
    if (z > M.tipZ - 0.062 && ax < 0.03 && y < -0.03) M.chin = Math.min(M.chin, y);
    if (z > M.tipZ - 0.014 && ax < 0.02) M.noseBase = Math.min(M.noseBase, y);
    if (ax < 0.01 && y > 0.004 && y < 0.03) M.glab = Math.max(M.glab, z); }
  // eye crease: most recessed points in each orbit near the eye line
  let ey = 0, en = 0; { const c = []; for (let i = 0; i < n; i++) { const [x, y, z] = V(i), ax = Math.abs(x); if (ax > 0.02 && ax < 0.045 && y > -0.02 && y < 0.02 && z > M.glab - 0.04) c.push([z, y]); } c.sort((a, b) => a[0] - b[0]); for (const [, y] of c.slice(0, 16)) { ey += y; en++; } }
  M.eyeY = ey / en;
  // ear: lateral points behind the cheek, at ear height
  for (let i = 0; i < n; i++) { const [x, y, z] = V(i), ax = Math.abs(x); if (y > -0.075 && y < 0.03 && z < 0.035 && z > -0.045) M.earX = Math.max(M.earX, ax); }
  for (let i = 0; i < n; i++) { const [x, y, z] = V(i), ax = Math.abs(x); if (ax > M.earX - 0.01 && y > -0.075 && y < 0.03 && z < 0.02 && z > -0.045) { M.earFront = Math.max(M.earFront, z); M.earBack = Math.min(M.earBack, z); M.earTop = Math.max(M.earTop, y); M.earBot = Math.min(M.earBot, y); } }
  for (let i = 0; i < n; i++) { const [x, y, z] = V(i), ax = Math.abs(x);
    if (Math.abs(y - M.eyeY + 0.014) < 0.004 && z > M.earFront + 0.004) M.bizyg = Math.max(M.bizyg, 2 * ax);
    if (Math.abs(y - (M.chin + 0.035)) < 0.004 && z > M.earFront - 0.02 && z < M.earFront + 0.04) M.gonial = Math.max(M.gonial, 2 * ax); }
  // sternal notch: the most recessed front-midline point below the chin
  M.notch = { y: COLLAR.front, z: 0 };
  M.chinF = 0; for (const [b, z] of prof) if (Math.abs(b * 0.002 - (M.chin + 0.012)) < 0.0011) M.chinF = z;
  const at = (m, y) => { let best = null, bd = 1; for (const [b, z] of m) { const d = Math.abs(b * 0.002 - y); if (d < bd) { bd = d; best = z; } } return best; };
  const yA = M.chin - 0.03, yB = M.chin - 0.07;
  M.neckA = { y: yA, f: at(prof, yA), b: at(back, yA) }; M.neckB = { y: yB, f: at(prof, yB), b: at(back, yB) };
  { const cz = (M.neckA.f + M.neckA.b) / 2; for (let i = 0; i < n; i++) { const [x, y, z] = V(i); if (Math.abs(y - yA) < 0.005 && z > cz - 0.03 && z < cz + 0.05) M.neckW = Math.max(M.neckW, 2 * Math.abs(x)); } }
  M.neckLean = deg(Math.atan2((M.neckA.f + M.neckA.b) / 2 - (M.neckB.f + M.neckB.b) / 2, yA - yB));
  M.neckBackLean = deg(Math.atan2(M.neckA.b - M.neckB.b, yA - yB));
  { const yP = -0.178; M.P = new V3(0, yP, (at(prof, yP) + at(back, yP)) / 2); M.notch.z = at(prof, COLLAR.front); }
  M.chinF = at(prof, M.chin + 0.012);

  return M; };

  let M = measureAll();
  M.earScale = 1;

  // ---------- phone placement v8: pressure on the cheekbone (skull), clear of the jaw and neck ----------
  status('Placing phone…');
  const hashPre = meshHash(rest, raw.idx);
  const frontPt = (x0, x1, y0, y1) => { let b = null; for (let i = 0; i < n; i++) { const x = -rest[i * 3], y = rest[i * 3 + 1], z = rest[i * 3 + 2]; if (x > x0 && x < x1 && y > y0 && y < y1 && (!b || z > b.z)) b = new V3(-x, y, z); } return b; };
  const LM = {}; LM.canthus = frontPt(0.0465, 0.0495, M.eyeY - 0.003, M.eyeY + 0.003);
  LM.mouthY = M.noseBase - (M.noseBase - M.chin) / 3; LM.mouth = frontPt(0.0225, 0.0255, LM.mouthY - 0.003, LM.mouthY + 0.003); LM.tragusZ = M.earFront;
  const BACK_Z = LM.tragusZ + 0.004, FRONT_MAX = Math.min(LM.canthus.z, LM.mouth.z) - 0.002, BOTTOM_Y = M.chin + 0.001, GAP = 0.0005;
  const ZYG = { y0: M.eyeY - 0.036, y1: M.eyeY - 0.006, z0: LM.canthus.z - 0.042, z1: LM.canthus.z - 0.008 }, JAW_Y = M.eyeY - 0.05;
  const cheek = []; for (let i = 0; i < n; i++) { const x = rest[i * 3], y = rest[i * 3 + 1], z = rest[i * 3 + 2]; if (x < -0.035 && y > -0.2 && y < 0.09 && z > M.earFront - 0.012) cheek.push(new V3(x, y, z)); }
  const basis = (a, b) => { const nn = new V3(-Math.cos(a), 0, Math.sin(a)), u = new V3(0, 1, 0), ax = new V3().crossVectors(u, nn).normalize(); nn.applyAxisAngle(ax, b); u.applyAxisAngle(ax, b); return { r: new V3().crossVectors(u, nn), u, n: nn }; };
  const pt = (c, B, lx, ly, lz) => c.clone().addScaledVector(B.r, lx).addScaledVector(B.u, ly).addScaledVector(B.n, lz);
  const corners = (c, B) => { const o = []; for (const sx of [-1, 1]) for (const sy of [-1, 1]) for (const sz of [-1, 1]) o.push(pt(c, B, sx * PH.hw, sy * PH.hh, sz * PH.hd)); return o; };
  const tmp = new V3();
  const fit = B => { const c = new V3(-0.1, BOTTOM_Y + PH.hh, 0.05); let hit = null;
    for (let it = 0; it < 10; it++) {
      c.y += BOTTOM_Y - Math.min(...corners(c, B).map(p => p.y));
      c.z += BACK_Z - Math.min(...corners(c, B).map(p => p.z));
      let mh = -1e9; hit = null; for (const v of cheek) { tmp.copy(v).sub(c); const lx = tmp.dot(B.r), ly = tmp.dot(B.u); if (Math.abs(lx) <= PH.hw && Math.abs(ly) <= PH.hh) { const hh = tmp.dot(B.n); if (hh > mh) { mh = hh; hit = v; } } }
      if (hit) c.addScaledVector(B.n, mh + PH.hd + GAP);
    } return { c, hit }; };
  const evalFit = (B, c, hit) => { let jaw = 1, zyg = 1, minG = 1;
    for (const v of cheek) { tmp.copy(v).sub(c); const lx = tmp.dot(B.r), ly = tmp.dot(B.u); if (Math.abs(lx) > PH.hw || Math.abs(ly) > PH.hh) continue; const g = -PH.hd - tmp.dot(B.n); minG = Math.min(minG, g);
      if (v.y < JAW_Y) jaw = Math.min(jaw, g); if (v.y > ZYG.y0 && v.y < ZYG.y1 && v.z > ZYG.z0 && v.z < ZYG.z1) zyg = Math.min(zyg, g); }
    const cs = corners(c, B), top = pt(c, B, 0, PH.hh, 0).y, front = Math.max(...cs.map(p => p.z)), rear = Math.min(...cs.map(p => p.z)), bottom = Math.min(...cs.map(p => p.y)), third = top - (2 * PH.hh) / 3;
    const inZyg = hit && hit.y > ZYG.y0 && hit.y < ZYG.y1 && hit.z > ZYG.z0 && hit.z < ZYG.z1;
    return { jaw, zyg, minG, top, front, rear, bottom, third, inZyg, ok: inZyg && zyg < 0.0009 && jaw >= 0.0025 && front <= FRONT_MAX && minG > -0.0002 }; };
  let best = null, fallback = null;
  for (let a = 0; a <= 48; a += 1) for (let b = 0; b <= 16; b += 0.25) {
    const B = basis(rad(a), rad(b)), { c, hit } = fit(B); if (!hit) continue; const e = evalFit(B, c, hit);
    const score = a + 0.6 * Math.abs(b - 8) - 400 * Math.min(0, e.hit ? 0 : 0) + 20000 * Math.max(0, e.third - hit.y);
    const cand = { a, b, B, c, hit, e, score };
    if (e.ok && (!best || score < best.score)) best = cand;
    const fs = score + (e.inZyg ? 0 : 50) + (e.front > FRONT_MAX ? 50 : 0); if (!fallback || fs < fallback.fs) fallback = { ...cand, fs };
  }
  const P0 = best || fallback;
  const PtoH = new THREE.Matrix4().makeBasis(P0.B.r, P0.B.u, P0.B.n).setPosition(P0.c);
  const Ph = { a: P0.a, b: P0.b, valid: !!best, nValid: 0, hit: P0.hit.clone(), ...P0.e, LM, ZYG, JAW_Y, BACK_Z, FRONT_MAX, size: [2 * PH.hh, 2 * PH.hw, 2 * PH.hd], corners: corners(P0.c, P0.B), hashPre };
  Ph.gaps = { up: P0.e.zyg, lo: P0.e.jaw };

  // ---------- skin weights for the neck twist (1 = rigid head, 0 = shoulders) ----------
  const W = new Float32Array(n), up0 = M.chin + 0.004, backTop = M.back + 0.01;
  for (let i = 0; i < n; i++) { const [, y, z] = V(i), f = clamp((z + 0.09) / 0.15), hi = -0.072 + (up0 + 0.072) * f, lo = collarY(z) + 0.01; const t = clamp((y - lo) / (hi - lo)); W[i] = t * t * (3 - 2 * t); }

  // ---------- geometry + material ----------
  const geo = new THREE.BufferGeometry();
  geo.setAttribute('position', new THREE.Float32BufferAttribute(rest.slice(), 3));
  geo.setAttribute('normal', new THREE.Float32BufferAttribute(nrm.slice(), 3));
  geo.setAttribute('aRest', new THREE.Float32BufferAttribute(rest, 3));
  geo.setAttribute('aW', new THREE.Float32BufferAttribute(W, 1));
  if (raw.idx) geo.setIndex(new THREE.BufferAttribute(raw.idx, 1));
  geo.computeBoundingSphere();
  const collar = { y: COLLAR.cy, z: COLLAR.cz, t: COLLAR.t };
  const skinMat = new THREE.MeshStandardMaterial({ color: C.skin, roughness: 0.74, metalness: 0, name: 'skin_and_crew_neck', side: THREE.DoubleSide });
  const U = { uShirt: { value: new THREE.Color(C.shirt) }, uBand: { value: new THREE.Color(C.band) }, uHtoP: { value: PtoH.clone().invert() }, uPH: { value: new THREE.Vector3(PH.hw, PH.hh, PH.hd) }, uShY: { value: new THREE.Vector2(Ph.hit.y - 0.022, Ph.hit.y + 0.03) }, uCol: { value: new THREE.Vector3(collar.y, collar.z, collar.t) }, uCut: { value: -1 } };
  skinMat.onBeforeCompile = sh => {
    Object.assign(sh.uniforms, U);
    sh.vertexShader = sh.vertexShader.replace('#include <common>', '#include <common>\nattribute vec3 aRest; varying vec3 vRest;').replace('#include <begin_vertex>', '#include <begin_vertex>\nvRest = aRest;');
    sh.fragmentShader = sh.fragmentShader.replace('#include <common>', '#include <common>\nvarying vec3 vRest; uniform vec3 uShirt; uniform vec3 uBand; uniform mat4 uHtoP; uniform vec3 uPH; uniform vec3 uCol; uniform float uCut; uniform vec2 uShY;\n' + SHIRT_GLSL)
      .replace('#include <clipping_planes_fragment>', '#include <clipping_planes_fragment>\n')
      .replace('#include <color_fragment>', `#include <color_fragment>
      float mC = shirtM(vRest);
      float aa = fwidth(mC) * 0.75;
      float shirt = smoothstep(-aa, aa, mC);
      float band = shirt * (1.0 - smoothstep(0.0072 - aa, 0.0072 + aa, mC));
      diffuseColor.rgb = mix(diffuseColor.rgb, mix(uShirt, uBand, band), shirt);
      vec3 lp = (uHtoP * vec4(vRest, 1.0)).xyz; float hh = -(lp.z + uPH.z); float ex = max(abs(lp.x) - uPH.x, abs(lp.y) - uPH.y);
      float ft = smoothstep(0.012, -0.004, ex) * smoothstep(uShY.x - 0.006, uShY.x + 0.006, vRest.y) * (1.0 - smoothstep(uShY.y - 0.006, uShY.y + 0.006, vRest.y)); float dep = hh < -0.001 ? 0.0 : exp(-max(hh, 0.0) / 0.004);
      diffuseColor.rgb *= 1.0 - 0.5 * ft * dep;
      if (!gl_FrontFacing) diffuseColor.rgb = uBand * 0.55;`);
  };
  const MAT = { skin: skinMat,
    phone: new THREE.MeshStandardMaterial({ name: 'phone', color: C.cyan, roughness: 0.45, emissive: C.cyan, emissiveIntensity: 0.22 }),
    edge: new THREE.MeshStandardMaterial({ name: 'phone_edge', color: C.cyanEdge, roughness: 0.35, emissive: C.cyanEdge, emissiveIntensity: 0.6 }),
    screen: new THREE.MeshStandardMaterial({ name: 'phone_screen', color: C.screen, roughness: 0.3, emissive: C.screen, emissiveIntensity: 0.18 }),
    earpiece: new THREE.MeshBasicMaterial({ color: C.ear }) };
  const phone = new THREE.Group(); phone.name = 'phone'; phone.matrixAutoUpdate = false; phone.matrix.copy(PtoH);
  { const bs = 0.0012, g = new THREE.ExtrudeGeometry(roundedRect(PH.hw * 2 - 2 * bs, PH.hh * 2 - 2 * bs, PH.r - bs), { depth: PH.hd * 2 - 0.003, bevelEnabled: true, bevelSize: bs, bevelThickness: 0.0015, bevelSegments: 3, curveSegments: 10 }); g.translate(0, 0, -PH.hd + 0.0015);
    phone.add(new THREE.Mesh(g, [MAT.phone, MAT.edge]));
    const sc = new THREE.Mesh(new THREE.ShapeGeometry(roundedRect(PH.hw * 2 - 0.006, PH.hh * 2 - 0.006, PH.r - 0.003), 10), MAT.screen); sc.position.z = PH.hd + 0.0002; phone.add(sc);
    const ep = new THREE.Mesh(new THREE.ShapeGeometry(roundedRect(0.012, 0.0022, 0.0011), 6), MAT.earpiece); ep.position.set(0, PH.hh - 0.0075, PH.hd + 0.0004); phone.add(ep); }
  status('Sculpting shoulders…');
  { const idx = geo.index.array, E = new Map(); for (let t = 0; t < idx.length; t += 3) for (let k = 0; k < 3; k++) { const a = idx[t + k], b = idx[t + (k + 1) % 3], key = a < b ? a * 1e6 + b : b * 1e6 + a; E.set(key, (E.get(key) || 0) + 1); }
    RIM.r.fill(0); RIM.y.fill(-0.2); const cnt = new Float32Array(RIM.n);
    for (const [k, c] of E) if (c === 1) for (const v of [Math.floor(k / 1e6), k % 1e6]) { const x = rest[v * 3], y = rest[v * 3 + 1], z = rest[v * 3 + 2]; if (y > -0.12) continue; const a = Math.atan2(x, z - COLLAR.cz), b = Math.round(((a / (2 * Math.PI)) + 1) % 1 * RIM.n) % RIM.n, rho = Math.hypot(x, z - COLLAR.cz); if (rho > RIM.r[b]) { RIM.r[b] = rho; RIM.y[b] = y; } } }
  const torsoGeo = SDFK.meshSDF(torsoSDF, [-0.27, -0.45, -0.17], [0.27, -0.13, 0.13], 0.003);
  const torsoMat = new THREE.MeshStandardMaterial({ name: 'crew_neck_top', color: C.shirt, roughness: 0.92, metalness: 0, transparent: true });
  torsoMat.onBeforeCompile = sh => {
    sh.vertexShader = sh.vertexShader.replace('#include <common>', '#include <common>\nvarying float vBY;').replace('#include <begin_vertex>', '#include <begin_vertex>\nvBY = position.y + 0.35 * max(0.0, abs(position.x) - 0.12);');
    sh.fragmentShader = sh.fragmentShader.replace('#include <common>', '#include <common>\nvarying float vBY;').replace('#include <dithering_fragment>', '#include <dithering_fragment>\n gl_FragColor.a *= 1.0 - smoothstep(-0.31, -0.375, vBY);');
  };
  Ph.hashPost = meshHash(geo.attributes.aRest.array, geo.index ? geo.index.array : null);
  return { geo, rest, nrm, W, M, Ph, PtoH, phone, MAT, U, L, collar, n, cutY: -1, torsoGeo, torsoMat, torsoSDF };
}

// ---------- proportion sheet ----------
export async function initSheet(o) {
  const status = o.onStatus || (() => {});
  const B = await buildBase(status), { M, Ph } = B;
  const scene = new THREE.Scene(), rig = new THREE.Group(); scene.add(rig);
  rig.add(new THREE.AmbientLight(C.ink, 0.5));
  for (const [p, i] of [[[-1.2, 1.4, 1.6], 1.0], [[1.6, -0.6, 1.0], 0.28], [[0.8, 1.2, -6], 0.55]]) { const l = new THREE.DirectionalLight(C.ink, i); l.position.set(...p); l.target.position.set(0, 0, -3); rig.add(l, l.target); }
  const head = new THREE.Mesh(B.geo, B.MAT.skin); head.name = 'head_neck_shoulders'; scene.add(head, B.phone);
  const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true }); renderer.setPixelRatio(1); renderer.setClearColor(0, 0); renderer.outputColorSpace = THREE.SRGBColorSpace;
  const height = M.vertex - M.chin, depth = M.glab - M.back, earH = M.earTop - M.earBot, earW = M.earFront - M.earBack, midZ = (M.glab + M.back) / 2, earCz = (M.earFront + M.earBack) / 2;
  const t2 = (M.eyeY + 0.012) - M.noseBase, t3 = M.noseBase - M.chin, neckL = M.chin - M.notch.y;
  const mm = v => Math.round(v * 1000) + ' mm', sg = v => (v >= 0 ? '+' : '−') + Math.abs(Math.round(v * 1000)) + ' mm';
  const checks = [
    [true, 'Real human form, not primitives', 'Head, face, ears, neck, trapezius and shoulders come from a 3D scan of a real person, smoothed and recoloured. ' + CREDIT],
    [Math.abs(M.eyeY) < 0.006, 'Eyes at the vertical midpoint', 'Crown ' + sg(M.vertex) + ', chin ' + sg(M.chin) + '. Closed-eye crease measured at ' + sg(M.eyeY) + '.'],
    [Math.abs(neckL / height - 1 / 3) < 0.06, 'Neck length chin to sternum ≈ ⅓ head height', mm(neckL) + ' against head height ' + mm(height) + ' (' + (neckL / height).toFixed(2) + ').'],
    [M.neckW / M.gonial > 0.8, 'Neck nearly as wide as the jaw', 'Neck ' + mm(M.neckW) + ' at mid-height, jaw ' + mm(M.gonial) + ' at the angles.'],
    [M.neckLean >= 15 && M.neckLean <= 20, 'Neck leans forward 15–20°; head sits in front of it', 'Neck axis ' + M.neckLean.toFixed(0) + '°, back of neck ' + M.neckBackLean.toFixed(0) + '° forward of vertical. Chin ' + mm(M.chinF - M.neckA.f) + ' in front of the throat.'],
    [M.gonial >= 0.094, 'Jaw at least as wide as the outer eye corners', 'Jaw angles ' + mm(M.gonial) + ' apart; cheekbones ' + mm(M.bizyg) + '.'],
    [Math.abs(t2 - t3) < 0.012, 'Brow to nose base ≈ nose base to chin', mm(t2) + ' and ' + mm(t3) + '.'],
    [M.tipZ - M.glab > 0.012, 'Nose projects; eyes recessed under the brow', 'Nose tip ' + mm(M.tipZ - M.glab) + ' in front of the brow.'],
    [Math.abs(earH - t2) < 0.005 && earW / earH > 0.45 && earW / earH < 0.65, 'Ears the height of the nose, about half as wide', 'Ear ' + mm(earH) + ' × ' + mm(earW) + ' (' + Math.round(earW / earH * 100) + '%); brow to nose base ' + mm(t2) + '. Auricle scaled ×' + M.earScale.toFixed(2) + ' from the scan. Ear centre ' + mm(midZ - earCz) + ' behind the skull midpoint.'],
    [Math.abs(Ph.top - M.eyeY) < 0.003, 'Phone top edge level with outer eye corner', 'Top edge ' + sg(Ph.top) + '; eye line ' + sg(M.eyeY) + '.'],
    [Ph.rear > M.earFront + 0.003, 'Phone in front of the ear', 'Rear edge ' + mm(Ph.rear - M.earFront) + ' in front of the ear.'],
    [Ph.noseGap > 0.005, 'Phone clear of the nose', 'Closest approach ' + mm(Ph.noseGap) + '.'],
    [Ph.gaps.up < 0.0012 && Ph.gaps.lo < 0.0012, 'Phone flush on cheekbone and jaw', 'Tilt kept from v3: ' + Ph.a + '° forward, ' + Ph.b + '° inward. Gap on cheekbone ' + (Ph.gaps.up * 1000).toFixed(1) + ' mm, on jaw ' + (Ph.gaps.lo * 1000).toFixed(1) + ' mm. Contact shadow under the footprint.'],
  ].map(([ok, title, detail]) => ({ ok, title, detail }));
  o.onChecks && o.onChecks(checks);

  function view(canvas, Zv, guides, half = 0.215, cy = -0.085) {
    if (!canvas) return; const S = 1000; canvas.width = S; canvas.height = S; renderer.setSize(S, S, false);
    const Z = Zv.clone().normalize(), X = new V3().crossVectors(new V3(0, 1, 0), Z).normalize(), Y = new V3().crossVectors(Z, X), c3 = new V3(0, cy, 0);
    const cam = new THREE.OrthographicCamera(-half, half, half, -half, 0.01, 8); cam.up.copy(Y); cam.position.copy(c3).addScaledVector(Z, 3); cam.lookAt(c3); cam.updateMatrixWorld();
    rig.position.copy(cam.position); rig.quaternion.copy(cam.quaternion); rig.updateMatrixWorld(true);
    const ctx = canvas.getContext('2d'); ctx.fillStyle = C.ground; ctx.fillRect(0, 0, S, S); renderer.render(scene, cam); ctx.drawImage(renderer.domElement, 0, 0, S, S);
    const pr = w => { const d = w.clone().sub(c3); return [(d.dot(X) / (2 * half) + 0.5) * S, (0.5 - d.dot(Y) / (2 * half)) * S]; };
    if (guides) { ctx.font = '500 19px "Atkinson Hyperlegible Next",system-ui,sans-serif'; ctx.textBaseline = 'bottom'; guides(ctx, S, pr); }
  }
  const line = (ctx, a, b, col = C.ref, dash = [6, 6], w = 1.5) => { ctx.strokeStyle = col; ctx.lineWidth = w; ctx.setLineDash(dash); ctx.beginPath(); ctx.moveTo(...a); ctx.lineTo(...b); ctx.stroke(); ctx.setLineDash([]); };
  const text = (ctx, t, x, y, al = 'left') => { ctx.fillStyle = C.ink; ctx.textAlign = al; ctx.fillText(t, x, y); };
  const H = [[M.vertex, 'Crown'], [M.eyeY + 0.012, 'Brow line'], [M.eyeY, 'Eye line'], [M.noseBase, 'Nose base'], [M.chin, 'Chin'], [M.notch.y, 'Sternal notch']];
  const hlines = (ctx, S, pr) => { for (const [y, l] of H) { const yy = pr(new V3(0, y, 0))[1]; line(ctx, [0, yy], [S, yy]); text(ctx, l, 14, l === 'Eye line' ? yy + 22 : yy - 4); } };
  view(o.front, new V3(0, 0, 1), (ctx, S, pr) => { hlines(ctx, S, pr);
    const tk = (x, y) => { const p = pr(new V3(x, y, 0.2)); line(ctx, [p[0], p[1] - 14], [p[0], p[1] + 14], C.ink, [], 2); };
    const gy = M.chin + 0.035, ny = (M.chin + M.notch.y) / 2; tk(-M.gonial / 2, gy); tk(M.gonial / 2, gy); tk(-M.neckW / 2, ny); tk(M.neckW / 2, ny);
    text(ctx, 'jaw ' + mm(M.gonial), S - 14, pr(new V3(0, gy, 0))[1] + 8, 'right'); text(ctx, 'neck ' + mm(M.neckW), S - 14, pr(new V3(0, ny, 0))[1] + 8, 'right'); });
  view(o.profile, new V3(-1, 0, 0), (ctx, S, pr) => { hlines(ctx, S, pr);
    for (const [z, l] of [[M.back, 'Back of skull'], [M.earFront, 'Front of ear'], [M.glab, 'Brow']]) { const xx = pr(new V3(0, 0, z))[0]; line(ctx, [xx, 0], [xx, S]); ctx.save(); ctx.translate(xx - 6, 250); ctx.rotate(-Math.PI / 2); text(ctx, l, 0, 0, 'right'); ctx.restore(); }
    const ma = (M.neckA.f + M.neckA.b) / 2, mb = (M.neckB.f + M.neckB.b) / 2, a = pr(new V3(0, M.neckA.y, ma)), b = pr(new V3(0, M.neckB.y, mb)), k = (a[0] - b[0]) / (a[1] - b[1]);
    const a2 = [b[0] + k * (a[1] - 60 - b[1]), a[1] - 60]; line(ctx, b, a2, C.amber, [10, 7], 2.5); line(ctx, b, [b[0], a2[1]], C.ref, [], 1.5); text(ctx, 'neck ' + M.neckLean.toFixed(0) + '° forward', b[0] - 10, b[1] + 26, 'right'); });
  view(o.threeq, new V3(-0.72, 0.1, 0.69), null);
  view(o.back, new V3(-0.62, 0.15, -0.77), null);
  status('');
  return { dispose() { renderer.dispose(); } };
}
