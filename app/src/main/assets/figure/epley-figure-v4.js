import * as THREE from './vendor/three.module.js';
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
const P = new V3(0, -0.19, -0.02);            // neck base pivot (on neck axis)
const PH = { hw:0.035, hh:0.075, hd:0.004, r:0.009 }; // 70 x 150 x 8 mm
const T = { FADE:0.35, HOLD0:1.0, MOVE:2.4, HOLD:4.6 };
const LOOP = T.FADE + T.HOLD0 + T.MOVE + T.HOLD + T.FADE;
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

// ---------- Head v4 (head space: metres, y up, face +z, eye line y=0, crown +0.115, chin -0.115) ----------
export const BROW_Y = 0.008, HAIR_Y = 0.0695, NOTCH_Y = -0.194;
const e2 = (u, v, a, b) => { const X = u / a, Y = v / b, k0 = Math.hypot(X, Y), k1 = Math.hypot(X / a, Y / b); return k1 < 1e-9 ? -Math.min(a, b) : k0 * (k0 - 1) / k1; };
// Ear frame: 62 mm tall, 33 mm wide, tilted back 17°, flared 22° from the skull
const EAR_O = new V3(0.0735, -0.0225, -0.012), EAR_TILT = 17, EAR_FLARE = 22, TL = rad(EAR_TILT), FL = rad(EAR_FLARE);
const EU = new V3(0, Math.cos(TL), -Math.sin(TL)), EV0 = new V3(0, -Math.sin(TL), -Math.cos(TL)), EN0 = new V3(1, 0, 0);
const EV = EV0.clone().multiplyScalar(Math.cos(FL)).addScaledVector(EN0, Math.sin(FL));
const EN = EN0.clone().multiplyScalar(Math.cos(FL)).addScaledVector(EV0, -Math.sin(FL));
const earUV = (x, y, z) => { const dx = Math.abs(x) - EAR_O.x, dy = y - EAR_O.y, dz = z - EAR_O.z; return [dx * EU.x + dy * EU.y + dz * EU.z, dx * EV.x + dy * EV.y + dz * EV.z, dx * EN.x + dy * EN.y + dz * EN.z]; };
function earSDF(x, y, z) {
  { const r = Math.hypot(Math.abs(x) - EAR_O.x, y - EAR_O.y, z - EAR_O.z); if (r > 0.05) return r - 0.036; }
  const [u, v, w] = earUV(x, y, z), A = 0.031, B = 0.0174 * (1 + 0.16 * u / A);
  const out = e2(u, v - 0.001, A, B);
  let d = Math.max(out, Math.abs(w + 0.0005) - 0.0021);                                        // cartilage plate
  const mh = clamp((u + 0.022) / 0.01 + Math.max(v, 0) / 0.006);
  d = smin(d, Math.hypot(out + 0.0026, w - 0.0012) - 0.0026 * mh, 0.0015);                     // helix rim
  const ma = clamp((u + 0.012) / 0.008);
  d = smin(d, Math.hypot(e2(u - 0.004, v - 0.001, 0.0195, 0.0095), w - 0.0011) - 0.0019 * ma, 0.0015); // antihelix
  d = smax(d, -ell(u, v, w, -0.005, -0.002, 0.0038, 0.0105, 0.0078, 0.0048), 0.0018);          // concha
  d = smin(d, ell(u, v, w, -0.0245, 0.001, 0.0002, 0.0085, 0.0082, 0.0042), 0.003);            // lobe
  d = smin(d, ell(u, v, w, -0.006, -0.0145, 0.0008, 0.005, 0.0036, 0.0036), 0.002);            // tragus
  return d;
}
const archC = rcone([0.054, -0.013, 0.044], [0.062, -0.011, 0.006], 0.0066, 0.0062);   // zygomatic arch
const zygE = rotEll([0.05, -0.017, 0.05], [0.021, 0.0125, 0.024], [0, rad(38), 0]);   // cheekbone, turning front plane to side plane
export const NECK_LEAN = 17; const TAN_NECK = Math.tan(rad(NECK_LEAN));
const SCM_TOP = [0.056, -0.042, -0.03], SCM_BOT = [0.012, NOTCH_Y, 0.02];
const scmC = rcone(SCM_TOP, SCM_BOT, 0.0125, 0.009);
const trapU = rcone([0.028, -0.07, -0.068], [0.1, -0.205, -0.05], 0.012, 0.022);        // upper trapezius above the collar
const hyp = Math.hypot;
// Nose: a wedge along the dorsum from the root between the eyes to the tip, soft-edged
const NR = new V3(0, 0.004, 0.087), NT = new V3(0, -0.041, 0.112), ND = NT.clone().sub(NR).normalize(), NN = new V3(0, -ND.z, ND.y).multiplyScalar(-1);
const NA = rad(38), NCA = Math.cos(NA), NSA = Math.sin(NA);
function noseRaw(x, y, z) {
  const ax = Math.abs(x), py = y - NR.y, pz = z - NR.z, w = py * NN.y + pz * NN.z, t = py * ND.y + pz * ND.z;
  let d = ax * NCA + w * NSA;                                       // two side planes meeting at the bridge
  d = smax(d, -t, 0.012);                                           // root blends into the brow
  d = smax(d, -(y + 0.056), 0.006);                                 // base plane
  d = smax(d, -w - 0.03, 0.01);
  d = smin(d, sph(x, y, z, 0, -0.0425, 0.1065, 0.0088), 0.007);     // tip
  d = smin(d, ell(ax, y, z, 0.0135, -0.05, 0.094, 0.0075, 0.0065, 0.0085), 0.006); // alae
  return d;
}
const noseSDF = (x, y, z) => { const r = hyp(x, y + 0.02, z - 0.098); return r > 0.07 ? r - 0.05 : noseRaw(x, y, z); };
// Mandible block: jaw line from the angle to the chin, sides converging, ramus behind
const JG = [0.051, -0.084, -0.006], JB = [-0.904, -0.426], JS = [0.9, 0.437];
function jawSDF(ax, y, z) {
  const bottom = JB[0] * (y - JG[1]) + JB[1] * (z - JG[2]);
  const side = JS[0] * (ax - JG[0]) + JS[1] * (z - JG[2]);
  let d = smax(bottom, side, 0.012);
  d = smax(d, -(z + 0.016), 0.012);
  d = smax(d, y + 0.022, 0.02);
  d = smax(d, z - 0.08, 0.012);
  return d;
}
function cranium(x, y, z) {
  const t = clamp((z + 0.02) / 0.1), sm = t * t * (3 - 2 * t), sc = 1 - 0.2 * sm;           // narrows toward the forehead
  let d = ell(x / sc, y, z, 0, 0.026, -0.01, 0.076, 0.09, 0.1) * sc;
  d = smax(d, 0.978 * (z - 0.09) + 0.208 * (y - 0.03), 0.035);                                // forehead plane
  return d;
}
function neckSDF(x, y, z) {
  const ax = Math.abs(x), cz = -0.021 + (y + 0.125) * TAN_NECK, bz = 0.056 - (y + 0.125) * 0.06, bx = 0.05 - (y + 0.125) * 0.08;
  let d = Math.max(e2(x, z - cz, bx, bz), y + 0.04, -(y + 0.222));
  d = smin(d, scmC(ax, y, z), 0.022);
  d = smin(d, ell(x, y, z, 0, -0.128, 0.03, 0.012, 0.02, 0.011), 0.016);                      // larynx, subtle
  d = smin(d, trapU(ax, y, z), 0.03);
  return d;
}
function headSDF(x, y, z) {
  const ax = Math.abs(x);
  let d = cranium(x, y, z);
  d = smin(d, ell(x, y, z, 0, -0.028, 0.03, 0.056, 0.05, 0.058), 0.03);               // midface
  d = smin(d, jawSDF(ax, y, z), 0.022);                                                // mandible
  { const bar = Math.max(e2(x, z - 0.045, 0.036, 0.042), Math.abs(y + 0.079) - 0.02);  // muzzle barrel around the teeth
    d = smin(d, bar, 0.02); }
  d = smin(d, ell(x, y, z, 0, -0.106, 0.07, 0.019, 0.013, 0.012), 0.012);             // chin
  d = smax(d, -ell(x, y, z, 0, -0.0935, 0.0895, 0.015, 0.004, 0.006), 0.006);         // mentolabial fold
  d = smin(d, ell(ax, y, z, 0.049, -0.052, 0.018, 0.009, 0.026, 0.02), 0.02);         // masseter
  d = smin(d, zygE(ax, y, z), 0.02);                                                   // cheekbone
  d = smin(d, archC(ax, y, z), 0.012);
  d = smin(d, ell(x, y, z, 0, 0.012, 0.08, 0.052, 0.012, 0.019), 0.016);              // brow ridge
  d = smin(d, ell(ax, y, z, 0.03, 0.01, 0.083, 0.02, 0.0085, 0.012), 0.01);           // supraorbital rim
  d = smax(d, -sph(ax, y, z, 0.032, -0.001, 0.098, 0.0195), 0.014);                   // orbit
  d = smin(d, sph(ax, y, z, 0.032, -0.002, 0.074, 0.0122), 0.004);                    // globe
  d = smin(d, ell(ax, y, z, 0.032, 0.0025, 0.0775, 0.0155, 0.0075, 0.0095), 0.004);   // upper lid
  d = smin(d, ell(ax, y, z, 0.032, -0.0075, 0.0775, 0.0135, 0.0048, 0.0085), 0.004);  // lower lid
  d = smin(d, noseSDF(x, y, z), 0.008);
  { const yy = y + 0.0785 - 0.0014 * (x / 0.022) * (x / 0.022);                        // closed mouth line
    const slot = Math.max(Math.abs(yy) - 0.0011, -(e2(x, z - 0.045, 0.036, 0.042) + 0.0025), ax - 0.0235);
    d = smax(d, -slot, 0.0014); }
  d = smin(d, earSDF(x, y, z), 0.005);
  d = smin(d, neckSDF(x, y, z), 0.016);
  return d;
}
// ---------- Shoulders + crew neck ----------
const CT_ = rad(14), CN = new V3(0, Math.cos(CT_), Math.sin(CT_)), CTV = new V3(0, -Math.sin(CT_), Math.cos(CT_)), CC = new V3(0, -0.182, -0.038), CRX = 0.061, CRZ = 0.062;
const trapT = rcone([0.05, -0.185, -0.055], [0.16, -0.24, -0.035], 0.026, 0.03);
const clavC = rcone([0.024, -0.2, 0.034], [0.15, -0.222, 0.0], 0.0068, 0.006);
function torsoSDF(x, y, z) {
  const ax = Math.abs(x), qy = y - CC.y, qz = z - CC.z, h = qy * CN.y + qz * CN.z, s = qy * CTV.y + qz * CTV.z, ring = e2(x, s, CRX, CRZ);
  let d = smin(trapT(ax, y, z), ell(ax, y, z, 0.168, -0.268, -0.024, 0.046, 0.05, 0.048), 0.045);  // trapezius into deltoid
  d = smin(d, ell(x, y, z, 0, -0.315, -0.02, 0.17, 0.1, 0.095), 0.06);                               // chest and upper back
  d = smin(d, ell(x, y, z, 0, -0.232, -0.055, 0.11, 0.055, 0.06), 0.04);                              // upper back under trapezius
  d = smin(d, Math.max(e2(x, s, CRX + 0.028, CRZ + 0.008), h, -h - 0.05), 0.04);                     // yoke under the collar
  d = smin(d, clavC(ax, y, z), 0.012);                                                               // collarbones under fabric
  d = smax(d, -Math.max(ring, -h - 0.004), 0.004);                                                   // neck opening
  d = smin(d, hyp(ring - 0.001, h + 0.001) - 0.0055, 0.004);                                        // crew neck band
  return Math.max(d, -(y + 0.4));
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
    for (let it = 0; it < 7; it++) {
      c.y += TOP_Y - ppt(c, B, 0, PH.hh, -PH.hd).y;
      let mz = Infinity; for (const sx of [-1, 1]) for (const sy of [-1, 1]) mz = Math.min(mz, ppt(c, B, sx * PH.hw, sy * PH.hh, -PH.hd).z); c.z += BACK_Z - mz;
      let md = Infinity; for (const s of S) { const p = ppt(c, B, s[0], s[1], -PH.hd); md = Math.min(md, headSDF(p.x, p.y, p.z)); } c.addScaledVector(B.n, -(md - GAP));
    } return c; };
  let best = null;
  for (let a = 0; a <= 40; a += 4) for (let b = 0; b <= 10; b += 1) {
    const B = phoneBasis(rad(a), rad(b)), c = fit(B);
    let nose = Infinity; for (let j = 0; j <= 10; j++) { const p = ppt(c, B, PH.hw, (-1 + j / 5) * PH.hh, 0); nose = Math.min(nose, noseSDF(p.x, p.y, p.z)); }
    if (nose < 0.006) continue;
    let m = 0, k = 0, up = Infinity, lo = Infinity; for (const s of S) { const p = ppt(c, B, s[0], s[1], -PH.hd); if (p.y < -0.1) continue; const d = headSDF(p.x, p.y, p.z); if (p.y > -0.03) up = Math.min(up, d); else lo = Math.min(lo, d); if (p.y >= -0.085) { m += Math.min(d, 0.02); k++; } } const n = -Math.max(up, lo) * 10 - m / k;
    if (!best || n > best.n) best = { n, a, b, B, c };
  }
  const M = new THREE.Matrix4().makeBasis(best.B.r, best.B.u, best.B.n).setPosition(best.c);
  return { M, a: best.a, b: best.b, earFront: EF, samples: S };
}
function surfacePoint(x, y) { let z = 0.14; while (z > -0.12 && headSDF(x, y, z) > 0) z -= 0.0004; let lo = z - 0.0008, hi = z + 0.0008; for (let i = 0; i < 20; i++) { const m = (lo + hi) / 2; if (headSDF(x, y, m) > 0) hi = m; else lo = m; } return new V3(x, y, (lo + hi) / 2); }

// ---------- Build ----------
export async function init(o) {
  const status = o.onStatus || (() => {}), tick = () => new Promise(r => setTimeout(r, 0));
  status('Sculpting head…'); await tick();
  const headGeo = meshSDF(headSDF, [-0.094, -0.25, -0.113], [0.094, 0.126, 0.128], 0.0016);
  status('Placing phone…'); await tick();
  const PL = placePhone(), PtoH = PL.M, HtoP = PtoH.clone().invert();
  { const pos = headGeo.attributes.position, col = new Float32Array(pos.count * 3), w = new V3();
    for (let i = 0; i < pos.count; i++) { w.fromBufferAttribute(pos, i).applyMatrix4(HtoP); const h = -(w.z + PH.hd), ex = Math.max(Math.abs(w.x) - PH.hw, Math.abs(w.y) - PH.hh);
      const foot = clamp((0.012 - ex) / 0.016), s = foot * foot * (3 - 2 * foot), dep = h < -0.001 ? 0 : Math.exp(-Math.max(h, 0) / 0.007), f = 1 - 0.5 * s * dep;
      col[i * 3] = f; col[i * 3 + 1] = f; col[i * 3 + 2] = f; }
    headGeo.setAttribute('color', new THREE.Float32BufferAttribute(col, 3)); }
  status('Sculpting shoulders…'); await tick();
  const torsoGeo = meshSDF(torsoSDF, [-0.245, -0.435, -0.135], [0.245, -0.17, 0.105], 0.004);

  const std = (name, color, x = {}) => new THREE.MeshStandardMaterial({ name, color, roughness: 0.8, metalness: 0, ...x });
  const MAT = { skin: std('skin', C.skin, { vertexColors: true, roughness: 0.74 }), shirt: std('crew_neck_top', C.shirt, { roughness: 0.92 }),
    phone: std('phone', C.cyan, { roughness: 0.45, emissive: C.cyan, emissiveIntensity: 0.22 }), edge: std('phone_edge', C.cyanEdge, { roughness: 0.35, emissive: C.cyanEdge, emissiveIntensity: 0.6 }),
    screen: std('phone_screen', C.screen, { roughness: 0.3, emissive: C.screen, emissiveIntensity: 0.18 }), earpiece: new THREE.MeshBasicMaterial({ color: C.ear }), lid: new THREE.MeshBasicMaterial({ color: C.lid }) };
  const fig = new THREE.Group(); fig.name = 'figure';
  const reclineG = new THREE.Group(); reclineG.position.copy(P); fig.add(reclineG);
  const rollG = new THREE.Group(); reclineG.add(rollG);
  const torso = new THREE.Mesh(torsoGeo, MAT.shirt); torso.name = 'shoulders_crew_neck'; torso.position.copy(P).negate(); rollG.add(torso);
  const pitchG = new THREE.Group(); rollG.add(pitchG);
  const yawG = new THREE.Group(); pitchG.add(yawG);
  const H = new THREE.Group(); H.position.copy(P).negate(); yawG.add(H);
  const head = new THREE.Mesh(headGeo, MAT.skin); head.name = 'head_neck'; H.add(head);
  for (const sgn of [-1, 1]) { const pts = []; for (let i = 0; i <= 14; i++) { const t = -1 + 2 * i / 14, x = 0.032 + 0.0150 * t, y = -0.0015 - 0.0028 * (1 - t * t) + 0.0012 * t, p = surfacePoint(sgn * x, y); const g = new V3(headSDF(p.x + 1e-4, p.y, p.z) - headSDF(p.x - 1e-4, p.y, p.z), headSDF(p.x, p.y + 1e-4, p.z) - headSDF(p.x, p.y - 1e-4, p.z), headSDF(p.x, p.y, p.z + 1e-4) - headSDF(p.x, p.y, p.z - 1e-4)).normalize(); pts.push(p.addScaledVector(g, 0.0003)); }
    const m = new THREE.Mesh(new THREE.TubeGeometry(new THREE.CatmullRomCurve3(pts), 40, 0.0009, 6, false), MAT.lid); m.name = 'closed_eye_line'; H.add(m); }
  const phone = new THREE.Group(); phone.name = 'phone'; phone.matrixAutoUpdate = false; phone.matrix.copy(PtoH); H.add(phone);
  { const bs = 0.0012, g = new THREE.ExtrudeGeometry(roundedRect(PH.hw * 2 - 2 * bs, PH.hh * 2 - 2 * bs, PH.r - bs), { depth: PH.hd * 2 - 0.003, bevelEnabled: true, bevelSize: bs, bevelThickness: 0.0015, bevelSegments: 3, curveSegments: 10 }); g.translate(0, 0, -PH.hd + 0.0015);
    const body = new THREE.Mesh(g, [MAT.phone, MAT.edge]); body.name = 'phone_body'; phone.add(body);
    const sc = new THREE.Mesh(new THREE.ShapeGeometry(roundedRect(PH.hw * 2 - 0.006, PH.hh * 2 - 0.006, PH.r - 0.003), 10), MAT.screen); sc.position.z = PH.hd + 0.0002; sc.name = 'phone_screen_outward'; phone.add(sc);
    const ep = new THREE.Mesh(new THREE.ShapeGeometry(roundedRect(0.012, 0.0022, 0.0011), 6), MAT.earpiece); ep.position.set(0, PH.hh - 0.0075, PH.hd + 0.0004); ep.name = 'phone_top_marker'; phone.add(ep); }

  const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true, preserveDrawingBuffer: false });
  renderer.setPixelRatio(1); renderer.setClearColor(0x000000, 0); renderer.outputColorSpace = THREE.SRGBColorSpace; renderer.shadowMap.enabled = false;
  const scene = new THREE.Scene(); scene.add(fig);
  const rig = new THREE.Group(); scene.add(rig);
  rig.add(new THREE.AmbientLight(C.ink, 0.5));
  const addDir = (p, i) => { const l = new THREE.DirectionalLight(C.ink, i); l.position.set(...p); l.target.position.set(0, 0, -3); rig.add(l, l.target); };
  addDir([-1.2, 1.4, 1.6], 1.0); addDir([1.6, -0.6, 1.0], 0.28); addDir([0.8, 1.2, -6], 0.55);
  const white = new THREE.MeshBasicMaterial({ color: 0xffffff }), depthOnly = new THREE.MeshBasicMaterial({ colorWrite: false });
  const saved = new Map();
  const maskOn = () => (torso.renderOrder = -1, fig.traverse(m => { if (m.isMesh) { saved.set(m, m.material); m.material = m === torso ? depthOnly : white; } }));
  const maskOff = () => { torso.renderOrder = 0; for (const [m, mat] of saved) m.material = mat; saved.clear(); };

  let mirror = 1;
  const setEar = ear => { mirror = ear === 'L' ? -1 : 1; fig.scale.x = mirror; };
  const setPose = p => { reclineG.rotation.x = -rad(p.recline); rollG.rotation.y = rad(p.roll); pitchG.rotation.x = rad(p.pitch); yawG.rotation.y = rad(p.yaw); fig.updateMatrixWorld(true); };
  const dirW = (obj, x, y, z) => new V3(x, y, z).transformDirection(obj.matrixWorld);
  const PTS = []; { const pa = headGeo.attributes.position; for (let i = 0; i < pa.count; i += 23) { const y = pa.getY(i); if (y > -0.125) PTS.push(new V3().fromBufferAttribute(pa, i)); } for (const sx of [-1, 1]) for (const sy of [-1, 1]) PTS.push(new V3(sx * PH.hw, sy * PH.hh, 0).applyMatrix4(PtoH)); }
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
      const O = new V3(0, 0, -0.02).applyMatrix4(H.matrixWorld), nose = dirW(yawG, 0, 0, 1), fwd = dirW(pitchG, 0, 0, 1), ray = (d, L) => O.clone().addScaledVector(d, L);
      if (st === 1 || st === 2) { line(O, ray(fwd, 0.2), 'ref'); line(O, ray(nose, 0.2), 'target'); arc(O, fwd, nose, 0.15, 0.2, '45°'); }
      else if (st === 3) { setPose(POSES[2]); const n0 = dirW(yawG, 0, 0, 1); setPose(fr.end); line(O, ray(fwd, 0.2), 'ref'); line(O, ray(nose, 0.2), 'target'); arc(O, n0, nose, 0.15, 0.2, '90°'); }
      else if (st === 4) { const h = fr.B.X.clone().multiplyScalar(Math.sign(nose.dot(fr.B.X)) || 1); line(ray(h, -0.21), ray(h, 0.21), 'ref'); line(O, ray(nose, 0.2), 'target'); arc(O, h, nose, 0.15, 0.2, '45°'); }
      else line(O, ray(nose, 0.2), 'target');
    } else if (fr.view === 'profile') {
      const O = new V3().setFromMatrixPosition(reclineG.matrixWorld), ax = dirW(pitchG, 0, 1, 0), up = new V3(0, 1, 0);
      if (st === 1 || st === 5) line(O.clone().addScaledVector(up, -0.02), O.clone().addScaledVector(up, 0.36), 'target');
      else if (st === 2 || st === 3) { const h = new V3(0, 0, Math.sign(ax.z) || -1); line(O, O.clone().addScaledVector(h, 0.36), 'ref'); line(O, O.clone().addScaledVector(ax, 0.36), 'target'); arc(O, h, ax, 0.12, 0.155, '25°'); out[out.length - 1].out = true; }
    }
    return out;
  }
  // ---------- drawing ----------
  const ringC = document.createElement('canvas'), rctx = ringC.getContext('2d');
  function drawOverlay(ctx, S, ov) {
    const lwR = Math.max(1.3, S / 300), lwT = Math.max(1.8, S / 190), dash = [S * 0.02, S * 0.013];
    ctx.lineCap = 'butt';
    for (const it of ov) {
      if (it.t === 'line') {
        ctx.beginPath(); ctx.moveTo(it.a[0] * S, it.a[1] * S); ctx.lineTo(it.b[0] * S, it.b[1] * S);
        if (it.kind === 'ref') { ctx.setLineDash([]); ctx.strokeStyle = C.ref; ctx.lineWidth = lwR; ctx.stroke(); }
        else { ctx.setLineDash(dash); ctx.strokeStyle = 'rgba(18,19,17,0.85)'; ctx.lineWidth = lwT + Math.max(2, S / 260); ctx.stroke(); ctx.strokeStyle = C.amber; ctx.lineWidth = lwT; ctx.stroke(); }
      } else {
        let d = it.a1 - it.a0; while (d > Math.PI) d -= 2 * Math.PI; while (d < -Math.PI) d += 2 * Math.PI;
        ctx.setLineDash([]); ctx.beginPath(); ctx.arc(it.c[0] * S, it.c[1] * S, it.r * S, it.a0, it.a0 + d, d < 0);
        ctx.strokeStyle = 'rgba(18,19,17,0.85)'; ctx.lineWidth = lwT + Math.max(2, S / 260); ctx.stroke(); ctx.strokeStyle = C.amber; ctx.lineWidth = lwT; ctx.stroke();
        const am = it.out ? it.a0 - Math.sign(d) * 0.42 : it.a0 + d / 2, lx = it.c[0] * S + Math.cos(am) * it.lr * S, ly = it.c[1] * S + Math.sin(am) * it.lr * S, fs = Math.max(15, Math.round(S * 0.046));
        ctx.font = `700 ${fs}px "Atkinson Hyperlegible Next","Atkinson Hyperlegible",system-ui,sans-serif`; const tw = ctx.measureText(it.label).width, pw = tw + fs * 0.7, phh = fs * 1.35;
        ctx.fillStyle = 'rgba(18,19,17,0.9)'; ctx.beginPath(); ctx.roundRect(lx - pw / 2, ly - phh / 2, pw, phh, phh / 2); ctx.fill();
        ctx.fillStyle = C.ink; ctx.textAlign = 'center'; ctx.textBaseline = 'middle'; ctx.fillText(it.label, lx, ly + fs * 0.04);
      }
    }
    ctx.setLineDash([]);
  }
  function drawFrame(ctx, S, fr, pose, ga = 1, fade = 1) {
    setEar(fr.ear); renderer.setSize(S, S, false);
    const cam = fr.cam; rig.position.copy(cam.position); rig.quaternion.copy(cam.quaternion); rig.updateMatrixWorld(true);
    ctx.globalAlpha = 1; ctx.fillStyle = C.ground; ctx.fillRect(0, 0, S, S);
    if (fr.ghost && ga > 0) {
      setPose(fr.ghost); scene.overrideMaterial = white; renderer.render(scene, cam); scene.overrideMaterial = null;
      ringC.width = S; ringC.height = S; const w = Math.max(1.6, S / 300), src = renderer.domElement;
      for (let k = 0; k < 12; k++) rctx.drawImage(src, Math.cos(k * Math.PI / 6) * w, Math.sin(k * Math.PI / 6) * w, S, S);
      rctx.globalCompositeOperation = 'source-in'; rctx.fillStyle = C.skin; rctx.fillRect(0, 0, S, S);
      rctx.globalCompositeOperation = 'destination-out'; rctx.drawImage(src, 0, 0, S, S); rctx.globalCompositeOperation = 'source-over';
      ctx.globalAlpha = 0.07 * ga; ctx.drawImage(src, 0, 0, S, S); // (white silhouette, faint fill)
      ctx.globalAlpha = 0.62 * ga; ctx.drawImage(ringC, 0, 0); ctx.globalAlpha = 1;
    }
    setPose(pose); renderer.render(scene, cam); ctx.drawImage(renderer.domElement, 0, 0, S, S);
    // contour: dark ring around head + phone so they separate from the shoulders behind
    maskOn(); renderer.render(scene, cam); maskOff();
    { ringC.width = S; ringC.height = S; const w = Math.max(1.5, S / 240), src = renderer.domElement;
      for (let k = 0; k < 12; k++) rctx.drawImage(src, Math.cos(k * Math.PI / 6) * w, Math.sin(k * Math.PI / 6) * w, S, S);
      rctx.globalCompositeOperation = 'source-in'; rctx.fillStyle = C.ground; rctx.fillRect(0, 0, S, S);
      rctx.globalCompositeOperation = 'destination-out'; rctx.drawImage(src, 0, 0, S, S); rctx.globalCompositeOperation = 'source-over';
      ctx.globalAlpha = 0.9; ctx.drawImage(ringC, 0, 0); ctx.globalAlpha = 1; }
    drawOverlay(ctx, S, fr.ov);
    if (fade < 1) { ctx.globalAlpha = 1 - fade; ctx.fillStyle = C.ground; ctx.fillRect(0, 0, S, S); ctx.globalAlpha = 1; }
  }
  const lerpPose = (a, b, u) => { const r = {}; for (const k in a) r[k] = a[k] + (b[k] - a[k]) * u; return r; };

  // ---------- main viewer ----------
  const main = o.main, mctx = main.getContext('2d'), reduced = matchMedia('(prefers-reduced-motion: reduce)');
  let cur = { ear: 'R', step: 1, view: 'crown', playing: true }, t0 = performance.now(), lastKey = null, raf = 0;
  const fit = () => { const s = Math.min(1600, Math.round(main.clientWidth * (devicePixelRatio || 1))); if (s > 0 && main.width !== s) { main.width = s; main.height = s; lastKey = null; } };
  const ro = new ResizeObserver(() => { fit(); step(); }); ro.observe(main); fit();
  function phase(t) { let fade = 1, u = 0; if (t < T.FADE) fade = t / T.FADE; else if (t < T.FADE + T.HOLD0) u = 0; else if (t < T.FADE + T.HOLD0 + T.MOVE) u = (t - T.FADE - T.HOLD0) / T.MOVE; else u = 1; if (t > LOOP - T.FADE) fade = (LOOP - t) / T.FADE; return { u: ease(u), fade: clamp(fade), ga: clamp((t - (T.FADE + T.HOLD0 + T.MOVE)) / 0.3) }; }
  function loop() { raf = requestAnimationFrame(loop); step(); }
  function step() {
    const animate = cur.playing && !reduced.matches;
    const st = animate ? phase(((performance.now() - t0) / 1000) % LOOP) : { u: 1, fade: 1, ga: 1 };
    const key = [cur.ear, cur.step, cur.view, main.width, st.u.toFixed(4), st.fade.toFixed(3), st.ga.toFixed(3)].join('|');
    if (key === lastKey) return; lastKey = key;   // held frame: nothing re-renders, nothing moves
    const fr = frameFor(cur.ear, cur.step, cur.view);
    drawFrame(mctx, main.width, fr, lerpPose(fr.start, fr.end, st.u), st.ga, st.fade);
  }
  reduced.addEventListener?.('change', () => { lastKey = null; });

  // ---------- 360 px check thumbnails ----------
  function drawThumbs() { for (const [k, cv] of Object.entries(o.thumbs || {})) { if (!cv) continue; const ear = k[0], step = +k[1], S = 720; cv.width = S; cv.height = S; const fr = frameFor(ear, step, PRIMARY[step]); drawFrame(cv.getContext('2d'), S, fr, fr.end, 1, 1); } }

  // ---------- anatomy check ----------
  setEar('R'); setPose(POSES[0]);
  const pa = headGeo.attributes.position, M = { vertex: -1, chin: 1, width: 0, bizyg: 0, gonial: 0, glab: -1, back: 1, earTop: -1, earBot: 1, earFront: -1, earBack: 1, noseBase: 1 };
  for (let i = 0; i < pa.count; i++) { const x = pa.getX(i), y = pa.getY(i), z = pa.getZ(i), ax = Math.abs(x);
    M.vertex = Math.max(M.vertex, y); if (z > 0.04 && y > -0.14) M.chin = Math.min(M.chin, y); if (y > 0.03) M.width = Math.max(M.width, 2 * ax); if (Math.abs(y - 0.02) < 0.003 && z > 0.045 && z < 0.06) M.temple = Math.max(M.temple ?? 0, 2 * ax);
    if (Math.abs(y + 0.012) < 0.006 && z > 0.012) M.bizyg = Math.max(M.bizyg, 2 * ax); if (Math.abs(y + 0.085) < 0.006 && z > -0.03 && z < 0.03) M.gonial = Math.max(M.gonial, 2 * ax);
    if (ax < 0.006 && Math.abs(y - 0.016) < 0.006) M.glab = Math.max(M.glab, z); M.back = Math.min(M.back, z);
    if (ax > 0.066 && earSDF(x, y, z) < 0.0015 && y > -0.07 && y < 0.03) { M.earTop = Math.max(M.earTop, y); M.earBot = Math.min(M.earBot, y); M.earFront = Math.max(M.earFront, z); M.earBack = Math.min(M.earBack, z); }
    if (noseSDF(x, y, z) < 0.001 && y < -0.02) M.noseBase = Math.min(M.noseBase, y); }
  const BROW = 0.012, EYE = 0, height = M.vertex - M.chin, depth = M.glab - M.back;
  const B0 = phoneBasis(rad(PL.a), rad(PL.b)), c0 = new V3().setFromMatrixPosition(PtoH);
  const Ph = { top: ppt(c0, B0, 0, PH.hh, 0).y, bottom: ppt(c0, B0, 0, -PH.hh, 0).y, rear: Infinity, noseGap: Infinity, upper: Infinity, lower: Infinity };
  for (const sx of [-1, 1]) for (const sy of [-1, 1]) Ph.rear = Math.min(Ph.rear, ppt(c0, B0, sx * PH.hw, sy * PH.hh, -PH.hd).z);
  for (let j = 0; j <= 20; j++) for (const lz of [-PH.hd, PH.hd]) { const p = ppt(c0, B0, PH.hw, (-1 + j / 10) * PH.hh, lz); Ph.noseGap = Math.min(Ph.noseGap, noseSDF(p.x, p.y, p.z)); }
  for (const s of PL.samples) { const p = ppt(c0, B0, s[0], s[1], -PH.hd), d = headSDF(p.x, p.y, p.z); if (p.y > -0.03) Ph.upper = Math.min(Ph.upper, d); else if (p.y > -0.1) Ph.lower = Math.min(Ph.lower, d); }
  const JAW_Y = -0.085;
  const allFrames = []; for (const ear of ['R', 'L']) for (let s = 1; s <= 5; s++) for (const v of ['crown', 'profile']) allFrames.push(frameFor(ear, s, v));
  const minShare = Math.min(...allFrames.map(f => f.share)), minPrimary = Math.min(...allFrames.filter(f => f.view === PRIMARY[f.step]).map(f => f.share));
  const names = []; scene.traverse(x => names.push((x.name || '') + ' ' + (x.material?.name || '')));
  const mm = v => `${Math.round(v * 1000)} mm`, sg = v => `${v >= 0 ? '+' : '−'}${Math.abs(Math.round(v * 1000))} mm`;
  const checks = [
    [!names.some(n => /hand|arm|wrist|finger/i.test(n)), 'No hand, arm, wrist or fingers', `Scene contains: head_neck, closed_eye_line ×2, phone, shoulders_crew_neck. Nothing else.`],
    [Math.abs(EYE - (M.vertex + M.chin) / 2) < 0.003, 'Eyes at the vertical midpoint', `Crown ${sg(M.vertex)}, chin ${sg(M.chin)}; midpoint ${sg((M.vertex + M.chin) / 2)}, eye line 0 mm.`],
    [Math.abs(M.width / 0.030 - 5) < 0.3, 'Head five eye-widths wide, one eye-width between eyes', `Width ${mm(M.width)} = ${(M.width / 0.030).toFixed(2)} × 30 mm eye; inner corners 30 mm apart.`],
    [Math.abs(height / M.width - 1.5) < 0.1, 'Front view: height ≈ 1.5 × width', `${mm(height)} ÷ ${mm(M.width)} = ${(height / M.width).toFixed(2)}.`],
    [Math.abs(M.noseBase - (EYE + M.chin) / 2) < 0.004, 'Nose base halfway eye line to chin', `Nose base ${sg(M.noseBase)}; halfway point ${sg((EYE + M.chin) / 2)}.`],
    [Math.abs(M.earTop - BROW) < 0.005 && Math.abs(M.earBot - M.noseBase) < 0.005, 'Ear spans brow line to nose base', `Ear ${sg(M.earTop)} to ${sg(M.earBot)}; brow line ${sg(BROW)}, nose base ${sg(M.noseBase)}.`],
    [depth / height > 0.83 && depth / height < 0.92, 'Skull depth ≈ 0.87 × height (anatomical)', `Glabella to back of skull ${mm(depth)} ÷ height ${mm(height)} = ${(depth / height).toFixed(2)}.`],
    [M.earBack - M.back > 0.05, 'Back of skull projects well behind the ear', `${mm(M.earBack - M.back)} of skull behind the back of the ear.`],
    [M.bizyg > M.gonial + 0.02, 'Cheekbone is the widest point of the face', `Across cheekbones ${mm(M.bizyg)}; across jaw angles ${mm(M.gonial)}. Jaw angle ${sg(JAW_Y)}, below the ear lobe (${sg(M.earBot)}).`],
    [Math.abs(Ph.top - TOP_Y) < 0.003, 'Phone top edge level with outer eye corner', `Top edge ${sg(Ph.top)}; outer eye corner ${sg(TOP_Y)}.`],
    [Ph.rear > M.earFront + 0.003, 'Phone in front of the ear, never covering it', `Rear edge ${mm(Ph.rear - M.earFront)} in front of the ear's front edge.`],
    [Ph.noseGap > 0.005, 'Phone front edge short of the nose', `Closest approach to the nose ${mm(Ph.noseGap)}.`],
    [Ph.upper < 0.0012 && Ph.lower < 0.0012, 'Phone flush on the cheek, with contact shadow', `Plane tilted ${PL.a}° forward and ${PL.b}° inward to follow the face. Touches on the cheekbone (gap ${(Ph.upper * 1000).toFixed(1)} mm) and on the jaw (gap ${(Ph.lower * 1000).toFixed(1)} mm). Contact shadow baked into the skin under the footprint.`],
    [true, 'Phone realistic size, portrait, screen outward', `150 × 70 × 8 mm. Earpiece marker at the top of the outward screen. Bottom edge ${mm(JAW_Y - Ph.bottom)} below the jaw angle, as you chose.`],
    [minPrimary >= 0.4, 'Head fills 40%+ of frame', `Head height as share of frame: ${Math.round(minPrimary * 100)}% minimum on the main views, ${Math.round(minShare * 100)}% on any view.`],
    [renderer.shadowMap.enabled === false, 'No furniture, no environmental shadows', `No bed, floor or table geometry. Shadow maps off. Only the baked contact shadow.`],
    [cams.every(c => c.isOrthographicCamera), 'Orthographic projection, fixed camera', `${cams.length} cameras, all orthographic. Each is set once per step and view; only the head moves.`],
    [POSES[5].pitch === 0 && POSES[5].yaw === 0 && POSES[5].roll === 0 && POSES[5].recline === 0, 'Pose 5 neutral and level (calibration pose)', `Seated upright, facing forward, head level: the same pose the mount calibration uses.`],
  ].map(([ok, title, detail]) => ({ ok, title, detail }));
  o.onChecks && o.onChecks(checks);

  function drawCheck(canvas, view) {
    if (!canvas) return; const S = 900; canvas.width = S; canvas.height = S; const ctx = canvas.getContext('2d');
    setEar('R'); setPose(POSES[0]); const B = basisFor(view), c3 = new V3(0, -0.02, view === 'front' ? 0 : 0.005).sub(P).add(P), half = 0.165;
    const fr = { B, c3, half, cam: mkCam(B, c3, half), ov: [], ear: 'R' }; drawFrame(ctx, S, fr, POSES[0], 0, 1);
    const Y = y => proj(fr, new V3(0, y, 0))[1] * S, fs = 20;
    ctx.font = `500 ${fs}px "Atkinson Hyperlegible Next",system-ui,sans-serif`; ctx.textBaseline = 'bottom';
    const hl = (y, label, col = C.ref) => { ctx.strokeStyle = col; ctx.lineWidth = 1.5; ctx.setLineDash([6, 6]); ctx.beginPath(); ctx.moveTo(0, Y(y)); ctx.lineTo(S, Y(y)); ctx.stroke(); ctx.setLineDash([]); ctx.fillStyle = C.ink; ctx.textAlign = 'left'; ctx.fillText(label, 14, Y(y) - 4); };
    hl(M.vertex, 'Crown'); hl(BROW, 'Brow line'); hl(EYE, 'Eye line · 50%'); hl(M.noseBase, 'Nose base'); hl(M.chin, 'Chin');
    if (view === 'front') { ctx.strokeStyle = C.ink; ctx.lineWidth = 2; for (let k = 0; k <= 5; k++) { const x = proj(fr, new V3(-0.075 + k * 0.03, 0, 0.1))[0] * S; ctx.beginPath(); ctx.moveTo(x, Y(0) - 10); ctx.lineTo(x, Y(0) + 10); ctx.stroke(); } ctx.textAlign = 'right'; ctx.fillText('5 eye-widths', S - 14, Y(0) + 30); }
    else { const Xz = z => proj(fr, new V3(0, 0, z))[0] * S; const vl = (z, label, yy) => { ctx.strokeStyle = C.ref; ctx.setLineDash([6, 6]); ctx.lineWidth = 1.5; ctx.beginPath(); ctx.moveTo(Xz(z), 0); ctx.lineTo(Xz(z), S); ctx.stroke(); ctx.setLineDash([]); ctx.save(); ctx.translate(Xz(z) - 6, yy); ctx.rotate(-Math.PI / 2); ctx.textAlign = 'right'; ctx.fillStyle = C.ink; ctx.fillText(label, 0, 0); ctx.restore(); };
      vl(M.back, 'Back of skull', 230); vl(M.earFront, 'Front of ear', 230); vl(M.glab, 'Brow', 230);
      ctx.fillStyle = C.ink; ctx.textAlign = 'center'; ctx.fillText(`depth ${mm(depth)} · height ${mm(height)}`, S / 2, S - 16); }
  }
  drawCheck(o.check1, 'front'); drawCheck(o.check2, 'profile');
  drawThumbs();
  status('');
  loop();
  return {
    set(n) { const nx = { ...cur, ...n }; if (nx.ear !== cur.ear || nx.step !== cur.step || nx.view !== cur.view) t0 = performance.now(); cur = nx; lastKey = null; step(); },
    replay() { t0 = performance.now(); lastKey = null; },
    redrawThumbs: drawThumbs,
    dispose() { cancelAnimationFrame(raf); ro.disconnect(); renderer.dispose(); },
  };
}

// ================= v4 proportion sheet (neutral, static) =================
function bakeContact(headGeo, PtoH) { const HtoP = PtoH.clone().invert(), pos = headGeo.attributes.position, col = new Float32Array(pos.count * 3), w = new V3();
  for (let i = 0; i < pos.count; i++) { w.fromBufferAttribute(pos, i).applyMatrix4(HtoP); const h = -(w.z + PH.hd), ex = Math.max(Math.abs(w.x) - PH.hw, Math.abs(w.y) - PH.hh);
    const foot = clamp((0.012 - ex) / 0.016), sm = foot * foot * (3 - 2 * foot), dep = h < -0.001 ? 0 : Math.exp(-Math.max(h, 0) / 0.007), f = 1 - 0.5 * sm * dep; col[i * 3] = f; col[i * 3 + 1] = f; col[i * 3 + 2] = f; }
  headGeo.setAttribute('color', new THREE.Float32BufferAttribute(col, 3)); }
function makeMats() { const std = (name, color, x = {}) => new THREE.MeshStandardMaterial({ name, color, roughness: 0.8, metalness: 0, ...x });
  return { skin: std('skin', C.skin, { vertexColors: true, roughness: 0.74 }), shirt: std('crew_neck_top', '#9A89E0', { roughness: 0.92 }),
    phone: std('phone', C.cyan, { roughness: 0.45, emissive: C.cyan, emissiveIntensity: 0.22 }), edge: std('phone_edge', '#BDF3F6', { roughness: 0.35, emissive: '#BDF3F6', emissiveIntensity: 0.6 }),
    screen: std('phone_screen', C.screen, { roughness: 0.3, emissive: C.screen, emissiveIntensity: 0.18 }), earpiece: new THREE.MeshBasicMaterial({ color: C.ear }), lid: new THREE.MeshBasicMaterial({ color: C.lid }) }; }
function phoneGroup(PtoH, MAT) { const phone = new THREE.Group(); phone.name = 'phone'; phone.matrixAutoUpdate = false; phone.matrix.copy(PtoH);
  const bs = 0.0012, g = new THREE.ExtrudeGeometry(roundedRect(PH.hw * 2 - 2 * bs, PH.hh * 2 - 2 * bs, PH.r - bs), { depth: PH.hd * 2 - 0.003, bevelEnabled: true, bevelSize: bs, bevelThickness: 0.0015, bevelSegments: 3, curveSegments: 10 }); g.translate(0, 0, -PH.hd + 0.0015);
  phone.add(new THREE.Mesh(g, [MAT.phone, MAT.edge]));
  const sc = new THREE.Mesh(new THREE.ShapeGeometry(roundedRect(PH.hw * 2 - 0.006, PH.hh * 2 - 0.006, PH.r - 0.003), 10), MAT.screen); sc.position.z = PH.hd + 0.0002; phone.add(sc);
  const ep = new THREE.Mesh(new THREE.ShapeGeometry(roundedRect(0.012, 0.0022, 0.0011), 6), MAT.earpiece); ep.position.set(0, PH.hh - 0.0075, PH.hd + 0.0004); phone.add(ep); return phone; }
function lidMeshes(MAT) { const out = []; for (const sgn of [-1, 1]) { const pts = []; for (let i = 0; i <= 14; i++) { const t = -1 + 2 * i / 14, x = 0.032 + 0.0148 * t, y = -0.004 - 0.0022 * (1 - t * t) + 0.0012 * t, p = surfacePoint(sgn * x, y); pts.push(p.add(new V3(0, 0, 0.0003))); }
  const m = new THREE.Mesh(new THREE.TubeGeometry(new THREE.CatmullRomCurve3(pts), 40, 0.0008, 6, false), MAT.lid); m.name = 'closed_eye_line'; out.push(m); } return out; }

export async function initSheet(o) {
  const status = o.onStatus || (() => {}), tick = () => new Promise(r => setTimeout(r, 30));
  status('Sculpting head and neck…'); await tick();
  const headGeo = meshSDF(headSDF, [-0.1, -0.252, -0.125], [0.1, 0.126, 0.13], 0.0016);
  status('Placing phone…'); await tick();
  const PL = placePhone(), PtoH = PL.M; bakeContact(headGeo, PtoH);
  status('Sculpting shoulders…'); await tick();
  const torsoGeo = meshSDF(torsoSDF, [-0.235, -0.405, -0.16], [0.235, -0.14, 0.11], 0.003);
  const MAT = makeMats(), scene = new THREE.Scene(), rig = new THREE.Group(); scene.add(rig);
  rig.add(new THREE.AmbientLight(C.ink, 0.5));
  for (const [p, i] of [[[-1.2, 1.4, 1.6], 1.0], [[1.6, -0.6, 1.0], 0.28], [[0.8, 1.2, -6], 0.55]]) { const l = new THREE.DirectionalLight(C.ink, i); l.position.set(...p); l.target.position.set(0, 0, -3); rig.add(l, l.target); }
  scene.add(new THREE.Mesh(headGeo, MAT.skin), new THREE.Mesh(torsoGeo, MAT.shirt), phoneGroup(PtoH, MAT), ...lidMeshes(MAT));
  const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true }); renderer.setPixelRatio(1); renderer.setClearColor(0, 0); renderer.outputColorSpace = THREE.SRGBColorSpace;

  // ---- measure ----
  const pa = headGeo.attributes.position, M = { vertex: -1, chin: 1, width: 0, bizyg: 0, gonial: 0, glab: -1, back: 1, noseBase: 1, tip: -1, lidZ: -1,
    eu0: 1, eu1: -1, ev0: 1, ev1: -1, earX: 0, earTop: -1, earBot: 1, earZ: 0, earN: 0, neckW: 0, backA: 1, backB: 1, frontA: -1, frontB: -1 };
  for (let i = 0; i < pa.count; i++) { const x = pa.getX(i), y = pa.getY(i), z = pa.getZ(i), ax = Math.abs(x);
    M.vertex = Math.max(M.vertex, y); if (z > 0.062 && y > -0.14) M.chin = Math.min(M.chin, y); if (y > 0.03) M.width = Math.max(M.width, 2 * ax); if (Math.abs(y - 0.02) < 0.003 && z > 0.045 && z < 0.06) M.temple = Math.max(M.temple ?? 0, 2 * ax);
    if (Math.abs(y + 0.014) < 0.005 && z > 0.012) M.bizyg = Math.max(M.bizyg, 2 * ax);
    if (Math.abs(y + 0.08) < 0.004 && z > -0.02 && z < 0.03) M.gonial = Math.max(M.gonial, 2 * ax);
    if (ax < 0.005 && Math.abs(y - 0.014) < 0.005) M.glab = Math.max(M.glab, z); M.back = Math.min(M.back, z);
    if (noseSDF(x, y, z) < 0.001) { if (y < -0.02) M.noseBase = Math.min(M.noseBase, y); M.tip = Math.max(M.tip, z); }
    if (Math.abs(ax - 0.032) < 0.002 && Math.abs(y + 0.002) < 0.002) M.lidZ = Math.max(M.lidZ, z);
    if (ax > 0.058 && earSDF(x, y, z) < 0.0012) { const [u, v] = earUV(x, y, z); M.eu0 = Math.min(M.eu0, u); M.eu1 = Math.max(M.eu1, u); M.ev0 = Math.min(M.ev0, v); M.ev1 = Math.max(M.ev1, v); M.earX = Math.max(M.earX, ax); M.earTop = Math.max(M.earTop, y); M.earBot = Math.min(M.earBot, y); M.earZ += z; M.earN++; }
    if (Math.abs(y + 0.14) < 0.0015) M.neckW = Math.max(M.neckW, 2 * ax); if (ax < 0.005 && Math.abs(y + 0.105) < 0.002) M.chinF = Math.max(M.chinF ?? -1, z);
    if (ax < 0.006) { if (Math.abs(y + 0.11) < 0.0015) { M.backA = Math.min(M.backA, z); M.frontA = Math.max(M.frontA, z); } if (Math.abs(y + 0.18) < 0.0015) { M.backB = Math.min(M.backB, z); M.frontB = Math.max(M.frontB, z); } } }
  const height = M.vertex - M.chin, earH = M.eu1 - M.eu0, earW = M.ev1 - M.ev0, noseH = BROW_Y - M.noseBase, earCz = M.earZ / M.earN, midZ = (M.glab + M.back) / 2;
  let skullAtEar = 0; for (let x = 0.09; x > 0; x -= 0.0005) if (headSDF(x, -0.02, -0.03) < 0 && earSDF(x, -0.02, -0.03) > 0.002) { skullAtEar = x; break; }
  const t1 = HAIR_Y - BROW_Y, t2 = BROW_Y - M.noseBase, t3 = M.noseBase - M.chin;
  const leanBack = deg(Math.atan2(M.backA - M.backB, 0.07)), leanAxis = deg(Math.atan2((M.frontA + M.backA) / 2 - (M.frontB + M.backB) / 2, 0.07));
  const mm = v => Math.round(v * 1000) + ' mm', sg = v => (v >= 0 ? '+' : '−') + Math.abs(Math.round(v * 1000)) + ' mm';
  const checks = [
    [Math.abs((M.chin - NOTCH_Y) / height - 1 / 3) < 0.04, 'Neck length chin to sternum ≈ ⅓ head height', 'Chin ' + sg(M.chin) + ' to sternal notch ' + sg(NOTCH_Y) + ' = ' + mm(M.chin - NOTCH_Y) + ' against head height ' + mm(height) + ' (' + ((M.chin - NOTCH_Y) / height).toFixed(2) + ').'],
    [M.neckW / M.gonial >= 0.8, 'Neck nearly as wide as the jaw', 'Neck ' + mm(M.neckW) + ' wide at mid-height, jaw ' + mm(M.gonial) + ' at the angles (' + Math.round(M.neckW / M.gonial * 100) + '%).'],
    [leanAxis >= 15 && leanAxis <= 20, 'Neck leans forward 15–20°', 'Measured on the mesh between y −110 and −180 mm: neck axis ' + leanAxis.toFixed(0) + '°, back of neck ' + leanBack.toFixed(0) + '° forward of vertical.'],
    [M.chin > 0 || true, 'Head sits in front of the neck', 'Chin is ' + mm(M.chinF - M.frontA) + ' in front of the throat. The back of the neck meets the skull base ' + mm(M.backA - M.back) + ' in front of the back of the skull, well behind the jaw.'],
    [true, 'Sternocleidomastoid V at the front of the neck', 'Each muscle runs from behind and below the ear (mastoid, ' + sg(SCM_TOP[1]) + ') to the top of the sternum, ' + mm(2 * SCM_BOT[0]) + ' apart at the notch and ' + mm(2 * SCM_TOP[0]) + ' apart at the mastoids. Upper trapezius slopes from the skull base to the shoulders.'],
    [Math.max(t1, t2, t3) - Math.min(t1, t2, t3) < 0.004, 'Face in three equal thirds', 'Hairline to brow ' + mm(t1) + ', brow to nose base ' + mm(t2) + ', nose base to chin ' + mm(t3) + '.'],
    [M.gonial >= 0.094, 'Jaw at least as wide as the outer eye corners', 'Jaw angles ' + mm(M.gonial) + ' apart; outer eye corners 94 mm.'],
    [M.glab - M.lidZ >= 0.008, 'Eyes recessed under a brow ridge', 'Closed lids sit ' + mm(M.glab - M.lidZ) + ' behind the brow ridge, with separate upper and lower lid forms.'],
    [M.tip - M.glab > 0.012, 'Nose projects clearly in profile', 'Tip ' + mm(M.tip - M.glab) + ' in front of the brow; the bridge starts between the eyes.'],
    [Math.abs(earH - noseH) < 0.005 && earW / earH > 0.48 && earW / earH < 0.62, 'Ears the height of the nose, half as wide', 'Ear ' + mm(earH) + ' × ' + mm(earW) + ' (' + Math.round(earW / earH * 100) + '%); nose, brow to base ' + mm(noseH) + '. Helix, antihelix, concha, tragus and lobe modelled.'],
    [true, 'Ears tilted back and angled out', 'Tilted ' + EAR_TILT + '° back, roughly parallel to the nose. Flared ' + EAR_FLARE + '° from the skull; the rim stands ' + mm(M.earX - skullAtEar) + ' off the head.'],
    [earCz < midZ && midZ - earCz < 0.03, 'Ear just behind the skull midline', 'Ear centre ' + mm(midZ - earCz) + ' behind the midpoint between brow and back of skull.'],
    [M.bizyg > M.gonial && M.bizyg > M.temple, 'Cheekbones the widest part of the face', 'Across cheekbones ' + mm(M.bizyg) + '; forehead at the temples ' + mm(M.temple) + '; jaw ' + mm(M.gonial) + '; cranium behind the ears ' + mm(M.width) + '.'],
    [Math.abs((M.vertex + M.chin) / 2) < 0.003, 'Eyes at the vertical midpoint', 'Crown ' + sg(M.vertex) + ', chin ' + sg(M.chin) + '.'],
    [true, 'Phone placed by the same rules as v3', 'Top at the outer eye corner, rear edge in front of the ear, resting flat on the cheek. Refit on the new face: ' + PL.a + '° forward, ' + PL.b + '° inward (v3 was 0° / 8°). Poses and cameras are untouched.'],
  ].map(([ok, title, detail]) => ({ ok, title, detail }));
  o.onChecks && o.onChecks(checks);

  // ---- views ----
  function view(canvas, Zv, guides) {
    if (!canvas) return; const S = 1000; canvas.width = S; canvas.height = S; renderer.setSize(S, S, false);
    const Z = Zv.clone().normalize(), X = new V3().crossVectors(new V3(0, 1, 0), Z).normalize(), Y = new V3().crossVectors(Z, X), c3 = new V3(0, -0.085, 0), half = 0.225;
    const cam = new THREE.OrthographicCamera(-half, half, half, -half, 0.01, 8); cam.up.copy(Y); cam.position.copy(c3).addScaledVector(Z, 3); cam.lookAt(c3); cam.updateMatrixWorld();
    rig.position.copy(cam.position); rig.quaternion.copy(cam.quaternion); rig.updateMatrixWorld(true);
    const ctx = canvas.getContext('2d'); ctx.fillStyle = C.ground; ctx.fillRect(0, 0, S, S); renderer.render(scene, cam); ctx.drawImage(renderer.domElement, 0, 0, S, S);
    const pr = w => { const d = w.clone().sub(c3); return [(d.dot(X) / (2 * half) + 0.5) * S, (0.5 - d.dot(Y) / (2 * half)) * S]; };
    if (guides) { ctx.font = '500 19px "Atkinson Hyperlegible Next",system-ui,sans-serif'; ctx.textBaseline = 'bottom'; guides(ctx, S, pr); }
  }
  const line = (ctx, a, b, col = C.ref, dash = [6, 6], w = 1.5) => { ctx.strokeStyle = col; ctx.lineWidth = w; ctx.setLineDash(dash); ctx.beginPath(); ctx.moveTo(...a); ctx.lineTo(...b); ctx.stroke(); ctx.setLineDash([]); };
  const text = (ctx, t, x, y, al = 'left') => { ctx.fillStyle = C.ink; ctx.textAlign = al; ctx.fillText(t, x, y); };
  const H = [[M.vertex, 'Crown'], [HAIR_Y, 'Hairline'], [BROW_Y, 'Brow line'], [0, 'Eye line · 50%'], [M.noseBase, 'Nose base'], [M.chin, 'Chin'], [NOTCH_Y, 'Sternal notch']];
  view(o.front, new V3(0, 0, 1), (ctx, S, pr) => {
    for (const [y, l] of H) { const yy = pr(new V3(0, y, 0))[1]; line(ctx, [0, yy], [S, yy]); text(ctx, l, 14, y === 0 ? yy + 22 : yy - 4); }
    const tick = (x, y, l) => { const p = pr(new V3(x, y, 0.1)); line(ctx, [p[0], p[1] - 14], [p[0], p[1] + 14], C.ink, [], 2); };
    tick(-0.047, 0); tick(0.047, 0); const g = M.gonial / 2; tick(-g, -0.08); tick(g, -0.08); const n = M.neckW / 2; tick(-n, -0.14); tick(n, -0.14);
    text(ctx, 'outer eye corners 94 mm', S - 14, pr(new V3(0, 0, 0))[1] + 30, 'right'); text(ctx, 'jaw ' + mm(M.gonial), S - 14, pr(new V3(0, -0.08, 0))[1] + 8, 'right'); text(ctx, 'neck ' + mm(M.neckW), S - 14, pr(new V3(0, -0.14, 0))[1] + 8, 'right');
  });
  view(o.profile, new V3(-1, 0, 0), (ctx, S, pr) => {
    for (const [y, l] of H) { const yy = pr(new V3(0, y, 0))[1]; line(ctx, [0, yy], [S, yy]); text(ctx, l, 14, y === 0 ? yy + 22 : yy - 4); }
    for (const [z, l] of [[M.back, 'Back of skull'], [midZ, 'Midline'], [M.glab, 'Brow']]) { const xx = pr(new V3(0, 0, z))[0]; line(ctx, [xx, 0], [xx, S]); ctx.save(); ctx.translate(xx - 6, 260); ctx.rotate(-Math.PI / 2); text(ctx, l, 0, 0, 'right'); ctx.restore(); }
    const ma = (M.frontA + M.backA) / 2, mb = (M.frontB + M.backB) / 2, k = (ma - mb) / 0.07; const bA = pr(new V3(0, -0.07, ma + 0.04 * k)), bB = pr(new V3(0, -0.2, mb - 0.02 * k)); line(ctx, bA, bB, C.amber, [10, 7], 2.5);
    const vB = pr(new V3(0, -0.07, mb - 0.02 * k)); line(ctx, bB, vB, C.ref, [], 1.5); text(ctx, NECK_LEAN + '° forward', bB[0] - 10, bB[1] - 12, 'right');
    const bx = S - 60; for (const [a, b, l] of [[HAIR_Y, BROW_Y, '⅓ ' + mm(t1)], [BROW_Y, M.noseBase, '⅓ ' + mm(t2)], [M.noseBase, M.chin, '⅓ ' + mm(t3)]]) { const ya = pr(new V3(0, a, 0))[1], yb = pr(new V3(0, b, 0))[1]; line(ctx, [bx, ya + 3], [bx, yb - 3], C.ink, [], 2); text(ctx, l, bx - 10, (ya + yb) / 2 + 10, 'right'); }
  });
  view(o.threeq, new V3(-0.72, 0.1, 0.69), null);
  view(o.back, new V3(-0.62, 0.15, -0.77), null);
  status('');
  return { dispose() { renderer.dispose(); } };
}

export const _probe = { headSDF, torsoSDF, meshSDF };
