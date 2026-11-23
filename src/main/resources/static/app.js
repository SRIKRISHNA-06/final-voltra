'use strict';
/* VOLTRA UI (dark theme). Uses the existing /api endpoints; no backend changes needed. */

const $ = (s, r = document) => r.querySelector(s);
const $$ = (s, r = document) => [...r.querySelectorAll(s)];
const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const money = n => '₹' + Number(n || 0).toLocaleString('en-IN', { maximumFractionDigits: 2 });
const when = iso => iso ? new Date(iso).toLocaleString('en-IN', { day: 'numeric', month: 'short', hour: 'numeric', minute: '2-digit' }) : '';
const pad = n => String(n).padStart(2, '0');
const localInput = d => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
const sum = (arr, f) => arr.reduce((t, x) => t + Number(f(x) || 0), 0);
let remember = true;   // "Remember me": keep the session after the browser closes
const store = {
  get: k => { try { return localStorage.getItem(k) ?? sessionStorage.getItem(k); } catch (_) { return null; } },
  set: (k, v) => { try { (remember ? localStorage : sessionStorage).setItem(k, v); } catch (_) {} },
  del: k => { try { localStorage.removeItem(k); sessionStorage.removeItem(k); } catch (_) {} }
};

const S = { token: store.get('voltra_token'), user: JSON.parse(store.get('voltra_user') || 'null'), tab: null, stations: [], q: '', mode: 'near', limit: 4, pos: null, map: null, layer: null, station: null, rateBooking: null };

const CATEGORY = { P2P_HOME: 'Home charger', GOVT_PUBLIC: 'Public station', PRIVATE_COMMERCIAL: 'Commercial' };
const STATUS = {
  PENDING_PAYMENT: ['Awaiting payment', 'warn'], PENDING_APPROVAL: ['Awaiting host', 'warn'], CONFIRMED: ['Confirmed', 'ok'],
  CHARGING: ['Charging', 'live'], COMPLETED: ['Completed', 'mute'], REJECTED: ['Declined', 'bad'], CANCELLED: ['Cancelled', 'mute'], EXPIRED: ['Expired', 'mute']
};
const APPROVAL = { PENDING_VERIFICATION: ['Needs review', 'warn'], APPROVED: ['Approved', 'ok'], REJECTED: ['Rejected', 'bad'] };
const pill = ([label, tone]) => `<span class="pill ${tone}">${esc(label)}</span>`;
const statusPill = s => pill(STATUS[s] || [s, 'mute']);
const empty = (title, text, btn = '') => `<div class="empty"><h3>${esc(title)}</h3><p>${esc(text)}</p>${btn}</div>`;

/* ---------- icons & artwork ---------- */
const ICON = {
  home: '<path d="M3 11l9-8 9 8v10a1 1 0 0 1-1 1h-5v-7H9v7H4a1 1 0 0 1-1-1z"/>',
  pin: '<path d="M12 22s7-6.2 7-12a7 7 0 1 0-14 0c0 5.8 7 12 7 12z"/><circle cx="12" cy="10" r="2.5"/>',
  calendar: '<rect x="3" y="4" width="18" height="17" rx="2"/><path d="M3 9h18M8 2v4M16 2v4"/>',
  card: '<rect x="2" y="5" width="20" height="14" rx="2"/><path d="M2 10h20"/>',
  user: '<circle cx="12" cy="8" r="4"/><path d="M4 21c0-4 4-6 8-6s8 2 8 6"/>',
  users: '<circle cx="9" cy="8" r="3.5"/><path d="M2 20c0-3.5 3-5.5 7-5.5s7 2 7 5.5M17 4.5a3.5 3.5 0 0 1 0 7M22 20c0-2.5-1.5-4.2-4-5"/>',
  logout: '<path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9"/>',
  bolt: '<path d="M13 2 4 14h6l-1 8 9-12h-6z"/>',
  bell: '<path d="M6 8a6 6 0 1 1 12 0c0 7 3 8 3 8H3s3-1 3-8M10 21a2 2 0 0 0 4 0"/>',
  search: '<circle cx="11" cy="11" r="7"/><path d="M21 21l-4.3-4.3"/>',
  menu: '<path d="M4 6h16M4 12h16M4 18h16"/>',
  leaf: '<path d="M11 20A7 7 0 0 1 4 13c0-6 6-9 16-9 0 10-3 16-9 16zM4 21c3-6 6-9 10-11"/>',
  plug: '<rect x="5" y="3" width="10" height="18" rx="2"/><path d="M9 8l-1 4h3l-1 4M15 8h2a2 2 0 0 1 2 2v6a1.5 1.5 0 0 0 3 0V8l-2-2"/>',
  rupee: '<path d="M6 4h12M6 9h12M9 4c5 0 7 2 7 5s-2 5-7 5h-1l8 7"/>',
  wallet: '<path d="M3 7a2 2 0 0 1 2-2h13v4M3 7v10a2 2 0 0 0 2 2h15V9H5a2 2 0 0 1-2-2zM16 14h2"/>',
  arrow: '<path d="M5 12h14M13 6l6 6-6 6"/>',
  check: '<path d="M4 12l5 5L20 6"/>',
  shield: '<path d="M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6z"/><path d="M9 12l2 2 4-4"/>',
  mail: '<rect x="3" y="5" width="18" height="14" rx="2"/><path d="M3 7l9 6 9-6"/>',
  lock: '<rect x="5" y="11" width="14" height="10" rx="2"/><path d="M8 11V8a4 4 0 0 1 8 0v3"/>',
  eye: '<path d="M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12z"/><circle cx="12" cy="12" r="3"/>',
  eyeoff: '<path d="M3 3l18 18M10.6 5.1A9.7 9.7 0 0 1 12 5c6 0 10 7 10 7a17 17 0 0 1-3.2 3.9M6.6 6.7C3.6 8.6 2 12 2 12s4 7 10 7c1.6 0 3-.4 4.2-1M9.9 9.9a3 3 0 0 0 4.2 4.2"/>',
  car: '<path d="M3 17v-4l2-6h14l2 6v4zM3 13h18M7 17v2M17 17v2"/><circle cx="7.5" cy="14.5" r=".8"/><circle cx="16.5" cy="14.5" r=".8"/>'
};
const ico = (n, cls = '') => `<svg class="ic ${cls}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${ICON[n]}</svg>`;
const brandHtml = `<span class="logo">${ico('bolt')}</span><span>VOLTRA</span>`;

/* Night-city scene with an EV and a charger (inline SVG, no external images). */
function scene(k, fill) {
  const b = [[6, 90, 34], [46, 130, 28], [80, 70, 36], [122, 150, 30], [158, 100, 40], [208, 165, 34], [250, 110, 30], [286, 140, 38], [336, 85, 32], [376, 155, 36], [420, 105, 30], [456, 135, 40], [506, 80, 34], [548, 115, 36]];
  let city = '';
  b.forEach(([x, h, w]) => {
    city += `<rect x="${x}" y="${210 - h}" width="${w}" height="${h}" fill="#0A2330" opacity=".9"/>`;
    for (let r = 0; r < Math.floor(h / 15); r++) for (let c = 0; c < Math.floor(w / 11); c++)
      if ((r * 7 + c * 13 + x) % 5 === 0) city += `<rect x="${x + 4 + c * 11}" y="${210 - h + 6 + r * 15}" width="5" height="7" fill="#12E58E" opacity="${(r + c) % 3 ? .55 : .9}"/>`;
  });
  return `<svg viewBox="0 0 600 260" preserveAspectRatio="xMaxYMax ${fill ? 'slice' : 'meet'}" role="img" aria-label="Electric car charging at a Voltra charger in a city at night">
  <defs>
    <filter id="g${k}" x="-30%" y="-30%" width="160%" height="160%"><feGaussianBlur stdDeviation="4" result="b"/><feMerge><feMergeNode in="b"/><feMergeNode in="SourceGraphic"/></feMerge></filter>
    <radialGradient id="gr${k}" cx=".5" cy=".5" r=".5"><stop offset="0" stop-color="#0F4A45"/><stop offset="1" stop-color="#0F4A45" stop-opacity="0"/></radialGradient>
  </defs>
  ${city}
  <ellipse cx="300" cy="214" rx="330" ry="34" fill="url(#gr${k})"/>
  <ellipse cx="300" cy="236" rx="190" ry="8" fill="#12E58E" opacity=".25" filter="url(#g${k})"/>
  <g transform="translate(150 150)" filter="url(#g${k})">
    <path d="M6 58C6 46 20 42 42 38L78 18C94 8 134 6 168 8C200 10 228 24 246 38C272 42 292 46 294 58L294 66L6 66Z" fill="#0E3C3C" stroke="#12E58E" stroke-width="2"/>
    <path d="M86 22C102 14 132 12 160 14C186 16 206 26 220 36L90 36Z" fill="#07202A" stroke="#12E58E" stroke-opacity=".6"/>
    <path d="M10 52H290" stroke="#12E58E" stroke-opacity=".5"/>
    <circle cx="70" cy="66" r="17" fill="#04100F" stroke="#12E58E" stroke-width="2"/><circle cx="70" cy="66" r="7" fill="#0E3C3C"/>
    <circle cx="232" cy="66" r="17" fill="#04100F" stroke="#12E58E" stroke-width="2"/><circle cx="232" cy="66" r="7" fill="#0E3C3C"/>
    <path d="M284 46L296 50" stroke="#9BFFD6" stroke-width="3" stroke-linecap="round"/>
  </g>
  <g filter="url(#g${k})">
    <path d="M470 220V92a12 12 0 0 1 12-12h40a12 12 0 0 1 12 12v128z" fill="#07201F" stroke="#12E58E" stroke-width="2.5"/>
    <rect x="482" y="96" width="40" height="52" rx="6" fill="#06151D" stroke="#12E58E" stroke-opacity=".6"/>
    <path d="M504 102l-12 24h9l-3 16 14-26h-9z" fill="#12E58E"/>
    <path d="M470 160C440 160 440 190 410 196" fill="none" stroke="#12E58E" stroke-width="3" stroke-linecap="round"/>
  </g>
  </svg>`;
}

/* Area chart (cumulative host earnings) */
function niceMax(v) {
  if (v <= 0) return 100;
  const m = Math.pow(10, Math.floor(Math.log10(v)));
  return ([1, 2, 2.5, 5, 10].map(f => f * m).find(x => x >= v)) || v;
}
const compact = v => v >= 1000 ? (v / 1000).toFixed(v % 1000 ? 1 : 0).replace('.0', '') + 'k' : String(Math.round(v));
function areaChart(pts) {
  const W = 560, H = 220, L = 46, B = 26, T = 12, R = 24;
  const max = niceMax(Math.max(...pts.map(p => p.v)));
  const t0 = pts[0].t, t1 = pts[pts.length - 1].t, span = Math.max(t1 - t0, 1);
  const x = t => L + (W - L - R) * (t - t0) / span, y = v => T + (H - T - B) * (1 - v / max);
  const line = pts.map((p, i) => `${i ? 'L' : 'M'}${x(p.t).toFixed(1)} ${y(p.v).toFixed(1)}`).join('');
  const last = pts[pts.length - 1];
  let grid = '';
  for (let i = 0; i <= 4; i++) { const v = max * i / 4; grid += `<line x1="${L}" x2="${W - R}" y1="${y(v)}" y2="${y(v)}" stroke="#14343F"/><text x="${L - 8}" y="${y(v) + 4}" text-anchor="end">${compact(v)}</text>`; }
  let xl = '';
  for (let i = 0; i < 5; i++) { const t = t0 + span * i / 4; xl += `<text x="${x(t)}" y="${H - 6}" text-anchor="middle">${new Date(t).toLocaleDateString('en-IN', { day: 'numeric', month: 'short' })}</text>`; }
  return `<svg class="chart" viewBox="0 0 ${W} ${H}" role="img" aria-label="Earnings over the last 30 days">
    <defs><linearGradient id="ea" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#12E58E" stop-opacity=".45"/><stop offset="1" stop-color="#12E58E" stop-opacity="0"/></linearGradient></defs>
    ${grid}${xl}
    <path d="${line}L${x(t1)} ${H - B}L${x(t0)} ${H - B}Z" fill="url(#ea)"/>
    <path d="${line}" fill="none" stroke="#12E58E" stroke-width="2.5" stroke-linejoin="round"/>
    <circle cx="${x(last.t)}" cy="${y(last.v)}" r="5" fill="#12E58E"/>
    <text x="${x(last.t) - 8}" y="${y(last.v) - 12}" text-anchor="end" style="fill:#12E58E;font-weight:700">${money(last.v)}</text></svg>`;
}
function earningsSeries(ledger) {
  const day = 86400000, end = new Date(); end.setHours(0, 0, 0, 0);
  const start = end.getTime() - 29 * day;
  const entries = ledger.map(l => ({ t: new Date(l.createdAt).getTime(), v: Number(l.hostNetAmount || 0) }));
  let run = sum(entries.filter(e => e.t < start), e => e.v);
  const pts = [];
  for (let d = start; d <= end.getTime(); d += day) {
    run += sum(entries.filter(e => e.t >= d && e.t < d + day), e => e.v);
    pts.push({ t: d, v: run });
  }
  return pts;
}
function donut(parts) {
  const r = 66, c = 2 * Math.PI * r, total = sum(parts, p => p.v) || 1;
  let off = 0, arcs = '';
  parts.forEach(p => { const len = c * p.v / total; if (len > 0) arcs += `<circle cx="85" cy="85" r="${r}" fill="none" stroke="${p.color}" stroke-width="16" stroke-dasharray="${len} ${c - len}" stroke-dashoffset="${-off}" transform="rotate(-90 85 85)"/>`; off += len; });
  return `<svg viewBox="0 0 170 170" aria-hidden="true"><circle cx="85" cy="85" r="${r}" fill="none" stroke="#14343F" stroke-width="16"/>${arcs}</svg>`;
}

/* ---------- plumbing ---------- */
async function api(path, opt = {}) {
  const headers = { 'Content-Type': 'application/json' };
  if (S.token) headers.Authorization = 'Bearer ' + S.token;
  const res = await fetch(path, { ...opt, headers });
  const data = await res.json().catch(() => ({}));
  if (res.status === 401 && !path.startsWith('/api/auth/login')) {
    signOut(false);
    throw new Error('Your session ended. Sign in again.');
  }
  if (!res.ok) throw new Error(data.message || 'Something went wrong. Try again.');
  return data;
}
const send = (method, path, body) => api(path, { method, body: body === undefined ? undefined : JSON.stringify(body) });

let toastTimer;
function toast(msg, isErr) {
  const t = $('#toast');
  t.textContent = msg;
  t.className = 'show' + (isErr ? ' err' : '');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { t.className = ''; }, 3800);
}
const fail = e => toast(e.message, true);
async function guarded(btn, fn) {
  if (btn) btn.disabled = true;
  try { await fn(); } catch (e) { fail(e); } finally { if (btn) btn.disabled = false; }
}

/* ---------- auth ---------- */
function authMsg(text) { const m = $('#authMsg'); m.textContent = text || ''; m.hidden = !text; }
function authTab(login) {
  $('#loginForm').hidden = !login;
  $('#registerForm').hidden = login;
  $('#authTitle').textContent = login ? 'Welcome Back!' : 'Create your account';
  $('#authSub').textContent = login ? 'Sign in to continue your EV journey' : 'Join the VOLTRA community';
  authMsg('');
}
function setRegisterRole(role) {
  $(`input[name=role][value=${role}]`).checked = true;
  $('#hostFields').hidden = role !== 'HOST';
  $('#driverFields').hidden = role === 'HOST';
}
function startSession(res) {
  S.token = res.token;
  S.user = res.user;
  store.set('voltra_token', S.token);
  store.set('voltra_user', JSON.stringify(S.user));
  showApp();
}
function signOut(callServer = true) {
  if (callServer && S.token) send('POST', '/api/auth/logout').catch(() => {});
  S.token = null; S.user = null;
  store.del('voltra_token'); store.del('voltra_user');
  if (S.map) { S.map.remove(); S.map = null; }
  $('#appView').hidden = true;
  $('#appView').classList.remove('open');
  $('#authView').hidden = false;
  authTab(true);
}

/* ---------- shell ---------- */
const TABS = {
  DRIVER: [['dash', 'Dashboard', 'home'], ['find', 'Find Chargers', 'pin'], ['trips', 'My Bookings', 'calendar'], ['payments', 'Payments', 'card'], ['profile', 'Profile', 'user']],
  HOST: [['hdash', 'Host Dashboard', 'home'], ['chargers', 'My Chargers', 'plug'], ['requests', 'Booking Requests', 'calendar'], ['earnings', 'Earnings', 'wallet'], ['payouts', 'Payouts', 'rupee'], ['profile', 'Profile', 'user']],
  ADMIN: [['adash', 'Admin Dashboard', 'home'], ['users', 'Users', 'users'], ['stations', 'Chargers', 'plug'], ['bookings', 'Bookings', 'calendar']]
};
const BELL_TAB = { DRIVER: 'trips', HOST: 'requests', ADMIN: 'stations' };
const VIEWS = { dash: vDash, find: vFind, trips: vTrips, payments: vPayments, profile: vProfile, hdash: vHostDash, chargers: vChargers, requests: vRequests, earnings: vEarnings, payouts: vPayouts, adash: vAdminDash, users: vUsers, stations: vStations, bookings: vBookings };

function showApp() {
  $('#authView').hidden = true;
  $('#appView').hidden = false;
  window.scrollTo(0, 0);
  $('#whoName').textContent = S.user.fullName;
  $('#avatar').textContent = S.user.fullName.trim().charAt(0).toUpperCase();
  $('#topSearch').hidden = S.user.role !== 'DRIVER';
  const tabs = TABS[S.user.role] || [];
  $('#nav').innerHTML = tabs.map(([id, label, ic]) => `<button data-tab="${id}">${ico(ic)}<span>${esc(label)}</span><b class="badge" data-badge="${id}" hidden></b></button>`).join('') +
    `<button data-act="logout">${ico('logout')}<span>Logout</span></button>`;
  go(tabs[0][0]);
}
async function go(tab) {
  S.tab = tab;
  $('#appView').classList.remove('open');
  $$('#nav button[data-tab]').forEach(b => b.classList.toggle('on', b.dataset.tab === tab));
  if (S.map) { S.map.remove(); S.map = null; }
  const view = $('#view');
  view.innerHTML = '<p class="loading">Loading…</p>';
  try {
    await VIEWS[tab]();
  } catch (e) {
    if (S.token) view.innerHTML = empty("Couldn't load this page", e.message, `<button class="btn" data-tab="${tab}">Try again</button>`);
  }
  view.focus({ preventScroll: true });
  if (S.token) refreshBadges();
}
const reload = () => go(S.tab);

async function refreshBadges() {
  let n = 0, tab = BELL_TAB[S.user.role];
  try {
    if (S.user.role === 'HOST') n = (await api('/api/host/bookings')).filter(b => b.status === 'PENDING_APPROVAL').length;
    else if (S.user.role === 'DRIVER') n = (await api('/api/driver/bookings')).filter(b => b.status === 'CONFIRMED' || b.status === 'CHARGING').length;
    else n = (await api('/api/admin/chargers')).filter(s => s.approvalStatus === 'PENDING_VERIFICATION').length;
  } catch (_) { return; }
  $$('[data-badge]').forEach(b => { b.hidden = true; });
  const nb = $(`[data-badge="${tab}"]`);
  if (nb) { nb.textContent = n; nb.hidden = !n; }
  $('#bellN').textContent = n;
  $('#bellN').hidden = !n;
}

/* ---------- small UI parts ---------- */
const tile = (icon, color, value, label) => `<div class="tile"><span class="ico ${color}">${ico(icon)}</span><div><strong>${value}</strong><small>${esc(label)}</small></div></div>`;
const km = (a, b) => {
  const r = Math.PI / 180, dLat = (b.latitude - a.lat) * r, dLng = (b.longitude - a.lng) * r;
  const h = Math.sin(dLat / 2) ** 2 + Math.cos(a.lat * r) * Math.cos(b.latitude * r) * Math.sin(dLng / 2) ** 2;
  return 12742 * Math.asin(Math.sqrt(h));
};
const thumb = () => `<span class="thumb">${ico('plug')}</span>`;

/* ---------- driver: dashboard + find ---------- */
const MODES = [['near', 'Nearby Chargers'], ['fast', 'Fast Charging'], ['avail', 'Available Now']];

function mapPanel(tall) {
  return `<section class="panel" aria-label="Chargers map">
    <div class="chips" id="chips">${MODES.map(([k, v]) => `<button class="chip" data-mode="${k}" aria-pressed="${S.mode === k}">${v}</button>`).join('')}</div>
    <div class="search" style="margin:.8rem 0;max-width:none">${ico('search')}<input id="q" type="search" placeholder="Search by area, name or connector" value="${esc(S.q)}" aria-label="Search chargers"></div>
    <div class="mapbox ${tall ? 'tall' : ''}" id="map"></div>
    <button class="link locate" data-act="locate">Use my location to sort by distance</button>
  </section>`;
}
function listPanel(title, viewAll) {
  return `<section class="panel"><div class="panel-h"><h3>${title}</h3>${viewAll ? `<button class="link" data-tab="find">View All</button>` : '<span class="mute" id="count"></span>'}</div><ul class="rows" id="stationList"></ul></section>`;
}
async function bindFind() {
  let timer;
  $('#q').addEventListener('input', e => { clearTimeout(timer); timer = setTimeout(() => { S.q = e.target.value.trim(); loadStations(); }, 350); });
  await loadStations();
}
async function loadStations() {
  try {
    S.stations = await api('/api/chargers' + (S.q ? '?q=' + encodeURIComponent(S.q) : ''));
    drawStations();
  } catch (e) { fail(e); }
}
function filtered() {
  let l = S.stations.slice();
  if (S.mode === 'fast') l = l.filter(s => s.powerKw >= 50);
  if (S.mode === 'avail') l = l.filter(s => s.online && s.availableBays > 0);
  if (S.pos) { l.forEach(s => { s._km = km(S.pos, s); }); l.sort((a, b) => a._km - b._km); }
  return l;
}
function drawStations() {
  if (!$('#stationList')) return;
  const all = filtered(), list = all.slice(0, S.limit);
  const c = $('#count'); if (c) c.textContent = all.length ? `${all.length} charger${all.length === 1 ? '' : 's'}` : '';
  $('#stationList').innerHTML = list.length ? list.map(stationRow).join('') : `<li>${empty('No chargers found', 'Try another area or switch the filter above.')}</li>`;
  drawMap(all);
}
function stationRow(s) {
  const total = s.totalBays || 1, free = s.online ? (s.availableBays ?? 0) : 0;
  const state = !s.online ? ['Offline', 'mute'] : free > 0 ? ['Available', 'ok'] : ['Busy', 'warn'];
  const rating = s.reviewCount ? `<button class="link" data-act="reviews" data-id="${s.id}">★ ${Number(s.averageRating).toFixed(1)} (${s.reviewCount})</button>` : '';
  return `<li id="st-${s.id}"><div class="row">
    ${thumb()}
    <div class="row-main">
      <h3>${esc(s.name)} ${pill(state)}</h3>
      <p class="mute">${esc(s.address)}, ${esc(s.city)}</p>
      <p class="meta"><span class="hl">⚡ ${s.powerKw} kW</span><span>${money(s.pricePerKwh)}/kWh</span><span>${money(s.pricePerHour)}/hr</span><span>${free}/${total} bays free</span>${rating}</p>
    </div>
    <div class="row-side">
      ${s._km != null && S.pos ? `<span class="mute">${s._km < 1 ? Math.round(s._km * 1000) + ' m' : s._km.toFixed(1) + ' km'} away</span>` : `<span class="tag">${esc(CATEGORY[s.category] || s.category)}</span>`}
      <button class="btn primary sm" data-act="book" data-id="${s.id}" ${free > 0 ? '' : 'disabled'}>Book Now</button>
    </div></div><div class="revs" hidden></div></li>`;
}
function drawMap(list) {
  const box = $('#map');
  if (!box) return;
  if (!window.L) { box.innerHTML = '<p class="map-off">The map could not load. You can still book from the list.</p>'; return; }
  if (!S.map) {
    S.map = L.map(box).setView([12.9716, 77.5946], 11);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19, attribution: '© OpenStreetMap contributors' }).addTo(S.map);
    S.layer = L.layerGroup().addTo(S.map);
  }
  S.layer.clearLayers();
  const pts = [];
  list.forEach(s => {
    if (s.latitude == null || s.longitude == null) return;
    const open = s.online && s.availableBays > 0;
    const icon = L.divIcon({ className: '', iconSize: [30, 30], iconAnchor: [15, 30], popupAnchor: [0, -30], html: `<div class="pin-ico ${open ? '' : 'off'}"><svg viewBox="0 0 24 24"><path d="M13 2 4 14h6l-1 8 9-12h-6z"/></svg></div>` });
    const m = L.marker([s.latitude, s.longitude], { icon, title: s.name }).addTo(S.layer);
    m.bindPopup(`<div class="pop"><strong>${esc(s.name)}</strong><span class="mute">${s.availableBays ?? 0}/${s.totalBays} bays free · ${s.powerKw} kW</span><button class="btn primary sm" ${open ? '' : 'disabled'}>Book Now</button></div>`);
    m.on('popupopen', e => { const b = e.popup.getElement().querySelector('button'); if (b && open) b.onclick = () => { m.closePopup(); openBook(s.id); }; });
    pts.push([s.latitude, s.longitude]);
  });
  if (S.pos) L.circleMarker([S.pos.lat, S.pos.lng], { radius: 7, color: '#fff', weight: 2, fillColor: '#3BA0FF', fillOpacity: 1 }).bindTooltip('You are here').addTo(S.layer);
  if (pts.length) S.map.fitBounds(pts, { padding: [30, 30], maxZoom: 14 });
  setTimeout(() => S.map && S.map.invalidateSize(), 60);
}
function locateUser() {
  if (!navigator.geolocation) return toast('This browser cannot share your location.', true);
  navigator.geolocation.getCurrentPosition(p => { S.pos = { lat: p.coords.latitude, lng: p.coords.longitude }; drawStations(); toast('Sorted by distance from you'); },
    () => toast('Location blocked. Allow location access in your browser.', true), { timeout: 10000 });
}
async function toggleReviews(id, btn) {
  const box = btn.closest('li').querySelector('.revs');
  if (!box.hidden) { box.hidden = true; return; }
  const revs = await api(`/api/chargers/${id}/reviews`);
  box.innerHTML = revs.length ? revs.map(r => `<div><strong style="color:var(--orange)">${'★'.repeat(r.rating)}</strong> ${esc(r.comment)}<br><small class="mute">${esc(r.driverName)}${r.vehicleModel ? ', ' + esc(r.vehicleModel) : ''}</small></div>`).join('') : '<span class="mute">No reviews yet.</span>';
  box.hidden = false;
}

async function vDash() {
  const [stations, bookings] = await Promise.all([api('/api/chargers' + (S.q ? '?q=' + encodeURIComponent(S.q) : '')), api('/api/driver/bookings')]);
  S.stations = stations; S.limit = 4;
  const hr = new Date().getHours(), greet = hr < 12 ? 'Good morning' : hr < 17 ? 'Good afternoon' : 'Good evening';
  const next = bookings.filter(b => ['CHARGING', 'CONFIRMED', 'PENDING_APPROVAL', 'PENDING_PAYMENT'].includes(b.status)).sort((a, b) => new Date(a.startTime) - new Date(b.startTime))[0];
  const done = bookings.filter(b => b.status === 'COMPLETED');
  $('#view').innerHTML = `
    <section class="banner"><div class="banner-copy"><h1>${greet}, ${esc(S.user.fullName.split(' ')[0])}!</h1><p>Power your journey with VOLTRA ⚡</p><span class="tagline">Clean Energy &bull; Smart Choices &bull; A Greener Bengaluru</span></div><div class="banner-art">${scene('b')}</div></section>
    <div class="grid3">
      ${mapPanel(false)}
      ${listPanel('Available Chargers', true)}
      <div class="stack">
        <section class="panel"><div class="panel-h"><h3>Upcoming Booking</h3><button class="link" data-tab="trips">View All</button></div>
          ${next ? `<div class="up">${thumb()}<div><h3>${esc(next.chargerName)}</h3><p class="mute">${when(next.startTime)}</p><p class="mute">${esc(next.chargerCity)}</p></div></div><p style="margin-top:.7rem">${statusPill(next.status)}</p>`
            : `<p class="mute">Nothing booked yet.</p><p style="margin-top:.7rem"><button class="btn primary sm" data-tab="find">Find a charger</button></p>`}</section>
        <section class="panel impact"><div><h3>Your Impact</h3><strong>${sum(done, b => b.actualKwh).toFixed(1)} kWh</strong><small class="mute">charged across ${done.length} session${done.length === 1 ? '' : 's'}</small></div><span class="big-leaf">${ico('leaf')}</span></section>
        <button class="host-cta" data-act="becomehost"><span><strong>Become a Host</strong><small>List your charger and earn money</small></span><span class="go">${ico('arrow')}</span></button>
      </div>
    </div>`;
  await bindFind();
}
async function vFind() {
  S.limit = Infinity;
  $('#view').innerHTML = `<div class="head"><h2>Find Chargers</h2></div><div class="grid2">${mapPanel(true)}${listPanel('Chargers')}</div>`;
  await bindFind();
}

/* ---------- driver: booking + UPI ---------- */
function openBook(id) {
  const s = S.stations.find(x => x.id === id);
  if (!s) return;
  S.station = s;
  const start = new Date();
  start.setMinutes(start.getMinutes() < 30 ? 30 : 60, 0, 0);
  $('#bookTitle').textContent = s.name;
  $('#bookSub').textContent = `${s.connectorType} · ${s.powerKw} kW · ${money(s.pricePerHour)}/hr`;
  $('#bookStart').min = localInput(new Date());
  $('#bookStart').value = localInput(start);
  $('#bookEnd').value = localInput(new Date(start.getTime() + 3600000));
  estimate();
  $('#bookDlg').showModal();
}
function estimate() {
  const hrs = (new Date($('#bookEnd').value) - new Date($('#bookStart').value)) / 3600000, ok = hrs > 0 && S.station;
  $('#bookEst').textContent = ok ? `${hrs.toFixed(1)} h slot ≈ ${money(hrs * S.station.pricePerHour)}. The final bill follows the energy you use.` : 'End time must be after start time.';
  $('#bookGo').disabled = !ok;
}
async function submitBooking(e) {
  e.preventDefault();
  await guarded($('#bookGo'), async () => {
    const r = await send('POST', '/api/driver/bookings', { chargerId: S.station.id, startTime: $('#bookStart').value, endTime: $('#bookEnd').value });
    $('#bookDlg').close();
    openUpi(r);
  });
}
function openUpi(r) {
  const ref = r.upiTxnRef || r.razorpayOrderId || '';
  $('#upiForm').dataset.booking = r.booking.id;
  $('#upiForm').dataset.ref = ref;
  $('#upiAmount').textContent = money(r.amountInr || r.booking.grossAmount);
  $('#upiPayee').textContent = (r.upiPayeeName ? r.upiPayeeName + ', ' : '') + (r.upiVpa || '');
  $('#upiRef').textContent = ref;
  $('#upiLink').href = r.upiUri || '#';
  $('#upiLink').hidden = !r.upiUri;
  $('#upiUtr').value = '';
  $('#upiDlg').showModal();
}
async function submitUpi(e) {
  e.preventDefault();
  const f = e.currentTarget;
  await guarded($('#upiGo'), async () => {
    const r = await send('POST', '/api/driver/payments/verify', { bookingId: Number(f.dataset.booking), upiTxnRef: f.dataset.ref, upiUtr: $('#upiUtr').value.trim().toUpperCase() });
    $('#upiDlg').close();
    showPin(r, 'Payment received');
    go('trips');
  });
}
function showPin(r, title) {
  $('#pinTitle').textContent = title;
  $('#pinNotice').textContent = r.notice || (r.oneTimePin ? 'Enter this PIN at the charger to start.' : '');
  $('#pinBox').hidden = !r.oneTimePin;
  $('#pinBox').textContent = r.oneTimePin || '';
  $('#pinExp').hidden = !r.expiresAt;
  $('#pinExp').textContent = r.expiresAt ? 'Valid until ' + when(r.expiresAt) : '';
  $('#pinDlg').showModal();
}

/* ---------- driver: bookings, payments ---------- */
async function vTrips() {
  const list = (await api('/api/driver/bookings')).sort((a, b) => new Date(b.startTime) - new Date(a.startTime));
  $('#view').innerHTML = `<div class="head"><h2>My Bookings</h2><button class="btn primary" data-tab="find">Find a charger</button></div>` +
    (list.length ? `<div class="panel page-rows"><ul class="rows">${list.map(tripRow).join('')}</ul></div>` : `<div class="panel">${empty('No bookings yet', 'Find a charger near you and book your first slot.')}</div>`);
}
function tripRow(b) {
  const id = b.id, st = b.status;
  let actions = '';
  if (st === 'CONFIRMED') actions = `
    <div class="pinrow"><input inputmode="numeric" maxlength="6" autocomplete="off" placeholder="6-digit PIN" aria-label="Charger PIN" data-pin="${id}"><button class="btn primary" data-act="start" data-id="${id}">Start</button></div>
    <div class="sub"><a class="link" target="_blank" rel="noopener" href="https://www.google.com/maps/dir/?api=1&destination=${b.chargerLat},${b.chargerLng}">Directions</a>
    <button class="link" data-act="newpin" data-id="${id}">Get a new PIN</button><button class="link danger" data-act="cancel" data-id="${id}">Cancel booking</button></div>`;
  else if (st === 'CHARGING') actions = `<div class="sub"><button class="btn primary" data-act="finish" data-id="${id}">Finish charging</button></div>`;
  else if (st === 'PENDING_PAYMENT' || st === 'PENDING_APPROVAL') actions = `<div class="sub"><button class="link danger" data-act="cancel" data-id="${id}">Cancel booking</button></div>`;
  else if (st === 'COMPLETED' && !b.reviewed) actions = `<div class="sub"><button class="btn sm" data-act="rate" data-id="${id}">Rate this session</button></div>`;
  return `<li><div class="row">${thumb()}
    <div class="row-main"><h3>${esc(b.chargerName)}</h3><p class="mute">${esc(b.chargerAddress)}, ${esc(b.chargerCity)}</p>
    <p class="meta"><span>${when(b.startTime)} to ${when(b.endTime)}</span>${b.actualKwh != null ? `<span class="hl">${b.actualKwh} kWh used</span>` : ''}</p></div>
    <div class="row-side">${statusPill(st)}<span class="price">${money(b.finalBilledAmount ?? b.grossAmount)}</span></div></div>${actions}</li>`;
}
async function tripAction(act, id, btn) {
  await guarded(btn, async () => {
    if (act === 'start') {
      await send('POST', `/api/driver/bookings/${id}/start`, { otp: $(`[data-pin="${id}"]`).value.trim() });
      toast('Charging started');
    } else if (act === 'finish') {
      await send('POST', `/api/driver/bookings/${id}/complete`);
      toast('Charging finished');
    } else if (act === 'cancel') {
      if (!confirm('Cancel this booking?')) return;
      await send('POST', `/api/driver/bookings/${id}/cancel`);
      toast('Booking cancelled');
    } else if (act === 'newpin') {
      showPin(await send('POST', `/api/driver/bookings/${id}/regenerate-pin`), 'Your new PIN');
      return;
    } else if (act === 'rate') {
      S.rateBooking = id;
      $('#rateText').value = '';
      $('#rateDlg').showModal();
      return;
    }
    reload();
  });
}
async function submitRating(e) {
  e.preventDefault();
  await guarded(null, async () => {
    await send('POST', '/api/driver/reviews', { bookingId: S.rateBooking, rating: Number(new FormData(e.currentTarget).get('rating')), comment: $('#rateText').value.trim() });
    $('#rateDlg').close();
    toast('Thanks for your review');
    reload();
  });
}
async function vPayments() {
  const tx = (await api('/api/driver/transactions')).sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
  $('#view').innerHTML = `<div class="head"><h2>Payments</h2></div>` + (tx.length
    ? `<div class="tbl-wrap"><table><thead><tr><th>Date</th><th>Details</th><th>Status</th><th class="num">Amount</th></tr></thead><tbody>${tx.map(t => `<tr><td>${when(t.createdAt)}</td><td>${esc(t.entryNote || 'Charging session')}</td><td>${esc(String(t.status).replace('_', ' ').toLowerCase())}</td><td class="num">${money(t.grossAmount)}</td></tr>`).join('')}</tbody></table></div>`
    : `<div class="panel">${empty('No payments yet', 'Payments appear here after you book a slot.')}</div>`);
}

/* ---------- profile (all roles) ---------- */
async function vProfile() {
  const u = await api('/api/auth/me');
  S.user = u; store.set('voltra_user', JSON.stringify(u));
  const drv = u.role === 'DRIVER', host = u.role === 'HOST';
  $('#view').innerHTML = `<div class="head"><h2>Profile</h2></div><section class="panel" style="max-width:640px"><form id="profileForm" class="form">
    <label>Email<input value="${esc(u.email)}" disabled></label>
    <label>Full name<input id="pfName" value="${esc(u.fullName)}" minlength="2" maxlength="100" required></label>
    <label>Phone<input id="pfPhone" type="tel" value="${esc(u.phone || '')}"></label>
    ${drv ? `<div class="two"><label>Vehicle model<input id="pfVeh" value="${esc(u.vehicleModel || '')}"></label><label>Number plate<input id="pfPlate" value="${esc(u.vehiclePlate || '')}"></label></div>` : ''}
    ${host ? `<div class="two"><label>UPI ID for payouts<input id="pfUpi" value="${esc(u.payoutUpi || '')}"></label><label>Address<input id="pfAddr" value="${esc(u.baseAddress || '')}"></label></div>` : ''}
    <div class="actions"><button class="btn primary" type="submit">Save changes</button></div></form></section>`;
  $('#profileForm').addEventListener('submit', async e => {
    e.preventDefault();
    await guarded(e.submitter, async () => {
      const g = (id, fallback) => $('#' + id) ? $('#' + id).value.trim() : (fallback || '');
      const up = await send('PUT', '/api/auth/profile', { fullName: g('pfName'), phone: g('pfPhone'), vehicleModel: g('pfVeh', u.vehicleModel), vehiclePlate: g('pfPlate', u.vehiclePlate), payoutUpi: g('pfUpi', u.payoutUpi), baseAddress: g('pfAddr', u.baseAddress) });
      S.user = up; store.set('voltra_user', JSON.stringify(up));
      $('#whoName').textContent = up.fullName;
      $('#avatar').textContent = up.fullName.trim().charAt(0).toUpperCase();
      toast('Profile saved');
    });
  });
}

/* ---------- host ---------- */
function requestRow(b, withActions) {
  return `<div class="req"><span class="avatar">${esc(b.driverName.charAt(0).toUpperCase())}</span>
    <div><strong>${esc(b.driverName)}</strong>${b.driverVehicle ? ` <small class="mute">${esc(b.driverVehicle)}</small>` : ''}<p class="mute">${esc(b.chargerName)} · ${when(b.startTime)}</p></div>
    ${b.status === 'PENDING_APPROVAL' && withActions ? `<span class="sub"><button class="btn primary sm" data-act="approve" data-id="${b.id}">Accept</button><button class="btn sm" data-act="reject" data-id="${b.id}">Reject</button></span>` : statusPill(b.status)}</div>`;
}
async function vHostDash() {
  const [stations, bookings, e] = await Promise.all([api('/api/host/chargers'), api('/api/host/bookings'), api('/api/host/earnings')]);
  const recent = bookings.slice().sort((a, b) => (b.status === 'PENDING_APPROVAL') - (a.status === 'PENDING_APPROVAL') || new Date(b.startTime) - new Date(a.startTime)).slice(0, 4);
  $('#view').innerHTML = `
    <div class="welcome"><div><h1>Welcome back, ${esc(S.user.fullName.split(' ')[0])}!</h1><p class="mute">Your charger is making a difference ⚡</p></div>
      <div class="total"><span class="tile" style="border:0;padding:0;background:none"><span class="ico c-green">${ico('wallet')}</span></span><div><small class="mute">Total Earnings</small><strong>${money(e.totalNetEarned)}</strong></div></div></div>
    <div class="tiles">${tile('plug', 'c-green', stations.length, 'Chargers')}${tile('calendar', 'c-blue', bookings.length, 'Total Bookings')}${tile('rupee', 'c-purple', money(e.totalNetEarned), 'Total Earnings')}${tile('bolt', 'c-orange', stations.filter(s => s.online).length, 'Online now')}</div>
    <div class="grid2">
      <section class="panel"><div class="panel-h"><h3>Earnings Overview</h3><span class="mute">Last 30 days</span></div>${(e.ledgerEntries || []).length ? areaChart(earningsSeries(e.ledgerEntries)) : empty('No earnings yet', 'Completed sessions will chart here.')}</section>
      <section class="panel"><div class="panel-h"><h3>Recent Booking Requests</h3><button class="link" data-tab="requests">View All</button></div>${recent.length ? `<div class="stack">${recent.map(b => requestRow(b, true)).join('')}</div>` : empty('No requests yet', 'Bookings on your chargers show up here.')}</section>
    </div>`;
}
async function vChargers() {
  const list = await api('/api/host/chargers');
  $('#view').innerHTML = `<div class="head"><h2>My Chargers</h2><button class="btn primary" data-act="addst">Add a charger</button></div>` +
    (list.length ? `<div class="panel page-rows"><ul class="rows">${list.map(s => `<li><div class="row">${thumb()}
      <div class="row-main"><h3>${esc(s.name)} ${pill(APPROVAL[s.approvalStatus] || [s.approvalStatus, 'mute'])}</h3><p class="mute">${esc(s.address)}, ${esc(s.city)}</p>
      <p class="meta"><span class="hl">⚡ ${s.powerKw} kW</span><span>${esc(s.connectorType)}</span><span>${s.totalBays} bay${s.totalBays === 1 ? '' : 's'}</span><span>${money(s.pricePerHour)}/hr + ${money(s.pricePerKwh)}/kWh</span></p></div>
      <div class="row-side"><button class="btn sm" data-act="toggle" data-id="${s.id}">${s.online ? 'Take offline' : 'Go online'}</button></div></div></li>`).join('')}</ul></div>`
      : `<div class="panel">${empty('No chargers yet', 'Add your first charger. It goes live after a quick admin check.')}</div>`);
}
async function submitStation(e) {
  e.preventDefault();
  await guarded(null, async () => {
    await send('POST', '/api/host/chargers', {
      name: $('#stName').value.trim(), category: $('#stCat').value, connectorType: $('#stConn').value,
      address: $('#stAddr').value.trim(), city: $('#stCity').value.trim(), hostPhone: $('#stPhone').value.trim() || undefined,
      latitude: Number($('#stLat').value), longitude: Number($('#stLng').value),
      powerKw: Number($('#stKw').value), totalBays: Number($('#stBays').value),
      pricePerHour: Number($('#stHr').value), pricePerKwh: Number($('#stKwh').value), online: true
    });
    $('#stDlg').close();
    toast('Charger submitted for review');
    reload();
  });
}
function locateMe() {
  if (!navigator.geolocation) return toast('This browser cannot share your location.', true);
  navigator.geolocation.getCurrentPosition(p => {
    $('#stLat').value = p.coords.latitude.toFixed(6);
    $('#stLng').value = p.coords.longitude.toFixed(6);
  }, () => toast('Location blocked. Allow location access or enter coordinates.', true), { enableHighAccuracy: true, timeout: 10000 });
}
async function vRequests() {
  const list = (await api('/api/host/bookings')).sort((a, b) => (b.status === 'PENDING_APPROVAL') - (a.status === 'PENDING_APPROVAL') || new Date(b.startTime) - new Date(a.startTime));
  $('#view').innerHTML = `<div class="head"><h2>Booking Requests</h2></div>` + (list.length
    ? `<div class="panel page-rows"><ul class="rows">${list.map(b => `<li>${requestRow(b, true)}<p class="mute" style="margin:.4rem 0 0 3.2rem">${when(b.startTime)} to ${when(b.endTime)} · your share ${money(b.hostNetAmount ?? b.grossAmount)}</p></li>`).join('')}</ul></div>`
    : `<div class="panel">${empty('No requests yet', 'When a driver books one of your chargers, it shows up here.')}</div>`);
}
async function vEarnings() {
  const e = await api('/api/host/earnings');
  const led = (e.ledgerEntries || []).slice().sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
  $('#view').innerHTML = `<div class="head"><h2>Earnings</h2></div>
    <div class="tiles">${tile('wallet', 'c-green', money(e.walletBalance), 'Available to withdraw')}${tile('card', 'c-blue', money(e.totalGrossEarned), 'Total billed')}${tile('rupee', 'c-orange', money(e.totalCommissionPaid), 'Voltra fee')}${tile('bolt', 'c-purple', money(e.totalNetEarned), 'You earned')}</div>
    <div class="grid2"><section class="panel"><div class="panel-h"><h3>Earnings Overview</h3><span class="mute">Last 30 days</span></div>${led.length ? areaChart(earningsSeries(led)) : empty('No earnings yet', 'Completed sessions will chart here.')}</section>
    <section class="panel"><h3 style="margin-bottom:.6rem">Sessions</h3>${led.length ? `<ul class="plain">${led.map(l => `<li><span>${esc(l.entryNote || 'Charging session')}<br><small class="mute">${when(l.createdAt)}</small></span><strong>${money(l.hostNetAmount)}</strong></li>`).join('')}</ul>` : '<p class="mute">Completed sessions appear here.</p>'}</section></div>`;
}
async function vPayouts() {
  const e = await api('/api/host/earnings');
  $('#view').innerHTML = `<div class="head"><h2>Payouts</h2></div><div class="grid2">
    <section class="panel"><h3 style="margin-bottom:.2rem">Withdraw to UPI</h3><p class="mute" style="margin-bottom:1rem">Available: <strong style="color:var(--green)">${money(e.walletBalance)}</strong></p>
      <form id="payoutForm" class="form"><label>Amount (₹)<input id="poAmt" type="number" min="100" step="0.01" required><small>Minimum ₹100.</small></label>
      <label>UPI ID<input id="poUpi" value="${esc(e.payoutUpi || '')}" required></label><button class="btn primary" type="submit">Request payout</button></form></section>
    <section class="panel"><h3 style="margin-bottom:.6rem">Payout history</h3>${(e.payouts || []).length ? `<ul class="plain">${e.payouts.map(p => `<li><span>${money(p.amount)}<br><small class="mute">${when(p.requestedAt)}</small></span>${pill(p.status === 'SETTLED' ? ['Paid', 'ok'] : p.status === 'REJECTED' ? ['Rejected', 'bad'] : ['Requested', 'warn'])}</li>`).join('')}</ul>` : '<p class="mute">No payouts yet.</p>'}</section></div>`;
  $('#payoutForm').addEventListener('submit', async ev => {
    ev.preventDefault();
    await guarded(null, async () => {
      await send('POST', '/api/host/payouts', { amount: Number($('#poAmt').value), payoutUpi: $('#poUpi').value.trim() });
      toast('Payout requested');
      reload();
    });
  });
}

/* ---------- admin ---------- */
async function vAdminDash() {
  const [users, stations, bookings] = await Promise.all([api('/api/admin/users'), api('/api/admin/chargers'), api('/api/admin/bookings')]);
  const approved = stations.filter(s => s.approvalStatus === 'APPROVED');
  const online = approved.filter(s => s.online).length, offline = approved.length - online, pending = stations.filter(s => s.approvalStatus === 'PENDING_VERIFICATION').length;
  const fees = sum(bookings.filter(b => b.status === 'COMPLETED'), b => b.platformCommission);
  const recent = bookings.slice().sort((a, b) => b.id - a.id).slice(0, 4);
  const tone = { COMPLETED: ['c-green', 'check'], CHARGING: ['c-blue', 'bolt'], CONFIRMED: ['c-green', 'calendar'], CANCELLED: ['c-orange', 'calendar'], REJECTED: ['c-orange', 'calendar'] };
  $('#view').innerHTML = `<div class="head"><h2>Admin Dashboard</h2></div>
    <div class="tiles">${tile('users', 'c-blue', users.length, 'Total Users')}${tile('plug', 'c-green', stations.length, 'Total Chargers')}${tile('calendar', 'c-purple', bookings.length, 'Total Bookings')}${tile('rupee', 'c-orange', money(fees), 'Platform fees earned')}</div>
    <div class="grid2">
      <section class="panel"><h3 style="margin-bottom:1rem">System Overview</h3><div class="donut-wrap">
        <div class="donut">${donut([{ v: online, color: '#12E58E' }, { v: offline, color: '#6B7F84' }, { v: pending, color: '#F6DB3C' }])}<div><strong>${approved.length ? Math.round(online / approved.length * 100) : 0}%</strong><small class="mute">Online</small></div></div>
        <div class="legend"><div><i style="background:#12E58E"></i>Online<b>${online}</b></div><div><i style="background:#6B7F84"></i>Offline<b>${offline}</b></div><div><i style="background:#F6DB3C"></i>Pending review<b>${pending}</b></div></div></div></section>
      <section class="panel"><div class="panel-h"><h3>Recent Activity</h3><button class="link" data-tab="bookings">View All</button></div>
        ${recent.length ? `<ul class="feed">${recent.map(b => { const [c, i] = tone[b.status] || ['c-purple', 'calendar']; return `<li><span class="ico ${c}">${ico(i)}</span><div><strong>Booking #${b.id}: ${esc((STATUS[b.status] || [b.status])[0])}</strong><p class="mute">${esc(b.chargerName)} · ${esc(b.driverName)}</p></div></li>`; }).join('')}</ul>` : '<p class="mute">Activity appears as drivers book.</p>'}</section>
    </div>`;
}
async function vStations() {
  const list = (await api('/api/admin/chargers')).sort((a, b) => (a.approvalStatus === 'PENDING_VERIFICATION' ? 0 : 1) - (b.approvalStatus === 'PENDING_VERIFICATION' ? 0 : 1));
  $('#view').innerHTML = `<div class="head"><h2>Chargers</h2></div>` + (list.length ? `<div class="panel page-rows"><ul class="rows">${list.map(s => `<li><div class="row">${thumb()}
    <div class="row-main"><h3>${esc(s.name)} ${pill(APPROVAL[s.approvalStatus] || [s.approvalStatus, 'mute'])}</h3><p class="mute">${esc(s.address)}, ${esc(s.city)}</p>
    <p class="meta"><span class="tag">${esc(CATEGORY[s.category] || s.category)}</span><span>Host: ${esc(s.hostName)}</span><span class="hl">⚡ ${s.powerKw} kW</span><span>${esc(s.connectorType)}</span></p></div>
    <div class="row-side"><span class="sub" style="margin:0">${s.approvalStatus !== 'APPROVED' ? `<button class="btn primary sm" data-act="aapprove" data-id="${s.id}">Approve</button>` : ''}${s.approvalStatus !== 'REJECTED' ? `<button class="btn sm" data-act="areject" data-id="${s.id}">Reject</button>` : ''}</span></div></div></li>`).join('')}</ul></div>` : `<div class="panel">${empty('No chargers', 'Chargers added by hosts show up here for review.')}</div>`);
}
async function vUsers() {
  const u = await api('/api/admin/users');
  $('#view').innerHTML = `<div class="head"><h2>Users</h2></div><div class="tbl-wrap"><table><thead><tr><th>Name</th><th>Email</th><th>Role</th><th>Phone</th><th class="num">Wallet</th></tr></thead><tbody>${u.map(x => `<tr><td>${esc(x.fullName)}</td><td>${esc(x.email)}</td><td>${esc(x.role)}</td><td>${esc(x.phone || '')}</td><td class="num">${money(x.walletBalance)}</td></tr>`).join('')}</tbody></table></div>`;
}
async function vBookings() {
  const b = (await api('/api/admin/bookings')).sort((x, y) => y.id - x.id);
  $('#view').innerHTML = `<div class="head"><h2>Bookings</h2></div>` + (b.length ? `<div class="tbl-wrap"><table><thead><tr><th>#</th><th>Charger</th><th>Driver</th><th>When</th><th>Status</th><th class="num">Billed</th></tr></thead><tbody>${b.map(x => `<tr><td>${x.id}</td><td>${esc(x.chargerName)}</td><td>${esc(x.driverName)}</td><td>${when(x.startTime)}</td><td>${statusPill(x.status)}</td><td class="num">${money(x.finalBilledAmount ?? x.grossAmount)}</td></tr>`).join('')}</tbody></table></div>` : `<div class="panel">${empty('No bookings yet', 'Bookings appear here as drivers make them.')}</div>`);
}

/* ---------- events ---------- */
document.addEventListener('click', e => {
  const t = e.target.closest('button,a,[data-tab]');
  if (!t) return;
  const d = t.dataset;
  if (d.close !== undefined) return t.closest('dialog').close();
  if (d.tab) return go(d.tab);
  if (d.mode) { S.mode = d.mode; $$('#chips .chip').forEach(c => c.setAttribute('aria-pressed', c.dataset.mode === S.mode)); return drawStations(); }
  if (d.showpw) { const i = $('#' + d.showpw); const show = i.type === 'password'; i.type = show ? 'text' : 'password'; t.innerHTML = ico(show ? 'eyeoff' : 'eye'); t.setAttribute('aria-label', show ? 'Hide password' : 'Show password'); return; }
  if (d.demo) { const [em, pw, role] = d.demo.split('|'); $('#loginEmail').value = em; $('#loginPass').value = pw; $(`input[name=loginRole][value=${role}]`).checked = true; return; }
  const id = Number(d.id), act = d.act;
  if (!act) return;
  const run = fn => guarded(t, fn);
  switch (act) {
    case 'logout': return signOut(true);
    case 'toreg': return authTab(false);
    case 'tologin': return authTab(true);
    case 'authhome': authTab(true); return $('#loginEmail').focus();
    case 'authfind': return toast('Sign in to find chargers near you');
    case 'becomehost': signOut(true); authTab(false); return setRegisterRole('HOST');
    case 'locate': return locateUser();
    case 'book': return openBook(id);
    case 'reviews': return run(() => toggleReviews(id, t));
    case 'start': case 'finish': case 'cancel': case 'newpin': case 'rate': return tripAction(act, id, t);
    case 'addst': return $('#stDlg').showModal();
    case 'toggle': return run(async () => { await send('PATCH', `/api/host/chargers/${id}/toggle`); reload(); });
    case 'approve': case 'reject': return run(async () => { await send('POST', `/api/host/bookings/${id}/${act}`); toast(act === 'approve' ? 'Booking approved' : 'Booking declined'); reload(); });
    case 'aapprove': case 'areject': return run(async () => { await send('PATCH', `/api/admin/chargers/${id}/${act === 'aapprove' ? 'approve' : 'reject'}`); toast(act === 'aapprove' ? 'Charger approved' : 'Charger rejected'); reload(); });
  }
});

$('#stLocate').onclick = locateMe;
$('#menuBtn').onclick = () => $('#appView').classList.toggle('open');
$('#scrim').onclick = () => $('#appView').classList.remove('open');
$('#bell').onclick = () => S.user && go(BELL_TAB[S.user.role]);
$('#topSearch').onsubmit = e => { e.preventDefault(); S.q = $('#topQ').value.trim(); go('find'); };
$('#bookStart').oninput = $('#bookEnd').oninput = estimate;
$('#bookForm').onsubmit = submitBooking;
$('#upiForm').onsubmit = submitUpi;
$('#rateForm').onsubmit = submitRating;
$('#stForm').onsubmit = submitStation;
$$('input[name=role]').forEach(r => r.addEventListener('change', () => setRegisterRole($('input[name=role]:checked').value)));

$('#loginForm').addEventListener('submit', async e => {
  e.preventDefault();
  authMsg('');
  const btn = e.submitter;
  btn.disabled = true;
  try {
    const want = $('input[name=loginRole]:checked').value;
    const res = await send('POST', '/api/auth/login', { email: $('#loginEmail').value.trim(), password: $('#loginPass').value });
    if (res.user.role !== 'ADMIN' && res.user.role !== want) {
      fetch('/api/auth/logout', { method: 'POST', headers: { Authorization: 'Bearer ' + res.token } }).catch(() => {});
      const have = res.user.role === 'HOST' ? 'Host' : 'Driver';
      throw new Error(`This is a ${have} account. Switch to ${have} above to sign in.`);
    }
    remember = $('#remember').checked;
    startSession(res);
  } catch (err) { authMsg(err.message); }
  finally { btn.disabled = false; }
});
$('#registerForm').addEventListener('submit', async e => {
  e.preventDefault();
  authMsg('');
  const role = $('input[name=role]:checked').value, btn = e.submitter, v = id => $('#' + id).value.trim() || undefined;
  btn.disabled = true;
  try {
    startSession(await send('POST', '/api/auth/register', {
      fullName: v('regName'), email: v('regEmail'), password: $('#regPass').value, role, phone: v('regPhone'),
      vehicleModel: role === 'DRIVER' ? v('regVehicle') : undefined, vehiclePlate: role === 'DRIVER' ? v('regPlate') : undefined,
      payoutUpi: role === 'HOST' ? v('regUpi') : undefined, baseAddress: role === 'HOST' ? v('regAddress') : undefined
    }));
    toast('Welcome to VOLTRA');
  } catch (err) { authMsg(err.message); }
  finally { btn.disabled = false; }
});

/* ---------- boot ---------- */
$$('[data-brand]').forEach(b => { b.innerHTML = brandHtml; });
$('#menuBtn').innerHTML = ico('menu');
$('#searchIcon').innerHTML = ico('search');
$('#bellIcon').innerHTML = ico('bell');
$('#promoIcon').innerHTML = ico('leaf');
$$('[data-ico]').forEach(e => { e.innerHTML = ico(e.dataset.ico); });
if (['localhost', '127.0.0.1'].includes(location.hostname)) $('#demo').hidden = false;
if (S.token && S.user && TABS[S.user.role]) {
  showApp();
  api('/api/auth/me').then(u => { S.user = u; store.set('voltra_user', JSON.stringify(u)); $('#whoName').textContent = u.fullName; }).catch(() => {});
} else {
  signOut(false);
}
