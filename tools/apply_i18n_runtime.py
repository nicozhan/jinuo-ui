#!/usr/bin/env python3
"""Rewrite kiosk.js as the single-language runtime (keeps the CATALOG block).

  python3 tools/apply_i18n_runtime.py

The original v2 runtime is archived at partials/kiosk-pre-i18n.js.
This script is idempotent: it regenerates kiosk.js from the template below.
"""
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OLD = ROOT / 'partials' / 'kiosk-pre-i18n.js'
TARGET = ROOT / 'kiosk.js'

TEMPLATE = r'''/* Jetinno Coffee Kiosk — interactive state machine (single-language edition)
   Injected into pages/*.html. Artboard is always 1080x1920; all hit regions use those coordinates.
   Every visible string comes from i18n.js in exactly one language at a time. */
(function () {
  'use strict';

  var CUR = document.currentScript;
  var SCREEN = CUR.getAttribute('data-screen') || location.pathname.split('/').pop().replace('.html', '');
  var QS = new URLSearchParams(location.search);
  var DEBUG = QS.has('debug');
  var FREEZE = QS.has('freeze'); /* static reference mode: no timers, no auto transitions */
  var DRAWER_EL = document.getElementById('k-cart-drawer');
  var DRAWER = !!DRAWER_EL; /* 04-shopping + 06-cart both host the bottom sheet */
  var AUTO_DRAWER = CUR.hasAttribute('data-drawer-open');
  var ROOT = '../';
  var KEY = 'jetinno.kiosk.v2';
  var I18N = window.KIOSK_I18N;
  if (!I18N) throw new Error('i18n.js must be loaded before kiosk.js');
  var ORDER = ['01-home', '02-lang', '03-help', '04-shopping', '05-custom', '06-cart',
    '11-card', '15-wrong', '16-pay', '17-making', '18-ready', '19-order-confirm'];

@@CATALOG@@

  var SLIDES_DEFAULT = [
    { img: 'assets/banner.jpg' },
    { img: 'assets/img/45f924271faa3343.jpg' },
    { img: 'assets/img/58cc1a4f9120bc08.jpg' }
  ];
  var LANGS = [
    { code: 'en', flag: '🇬🇧', key: 'k.lang.name.en' },
    { code: 'fr', flag: '🇫🇷', key: 'k.lang.name.fr' },
    { code: 'zh', flag: '🇨🇳', key: 'k.lang.name.zh' }
  ];
  var LANGBY = {};
  LANGS.forEach(function (l) { LANGBY[l.code] = l; });
  var PAYMETHODS = ['Card', 'Tap', 'QR Pay', 'Scan Phone', 'Cash'];
  var ERRKEYS = ['generic', 'payment', 'machine', 'timeout'];
  var TIMEOUTS = {
    '01-home': 0, '02-lang': 100, '03-help': 60, '04-shopping': 100,
    '05-custom': 100, '06-cart': 100, '11-card': 100, '15-wrong': 100,
    '16-pay': 100, '17-making': 100, '18-ready': 30, '19-order-confirm': 100
  };
  var DEFAULTS = {
    lang: 'en', slide: 0, slides: null,
    cart: [
      { pid: 'americano', name: 'Americano', price: 3.50, qty: 1, opts: { sugar: 0, temp: 2 } },
      { pid: 'caramel-macchiato', name: 'Caramel Macchiato', price: 3.00, qty: 1, opts: { sugar: 0, temp: 3 } },
      { pid: 'coconut-mocha', name: 'Coconut Mocha', price: 4.00, qty: 2, opts: { sugar: 1, temp: 4 } }
    ],
    category: 'CLASSICS', custom: { bean: 1, sugar: 0, temp: 2 },
    payMethod: 'Card', qrIndex: 0, errorType: 'generic', payResult: 'success',
    returnTo: '01-home', brewStage: 0, langOpen: false, selectedProduct: 'americano'
  };

  function clone(o) { return JSON.parse(JSON.stringify(o)); }
  function load() {
    var s;
    try { s = JSON.parse(localStorage.getItem(KEY) || '{}'); } catch (e) { s = {}; }
    var out = clone(DEFAULTS);
    Object.keys(s).forEach(function (k) { out[k] = s[k]; });
    if (!out.slides || out.slides.length !== 3) out.slides = clone(SLIDES_DEFAULT);
    if (!LANGBY[out.lang]) out.lang = 'en';
    return out;
  }
  var state = load();
  function save() { try { localStorage.setItem(KEY, JSON.stringify(state)); } catch (e) { /* private mode */ } }
  if (QS.get('lang') && LANGBY[QS.get('lang')]) { state.lang = QS.get('lang'); save(); }

  /* ---------- language helpers ----------
     tr() is the only path from a key to visible text. Static nodes carry
     data-i18n="<original Figma string>"; runtime strings use k.* keys. */
  function tr(key, vars) {
    var d = I18N[state.lang] || I18N.en;
    var v = (d[key] !== undefined) ? d[key] : (I18N.en[key] !== undefined ? I18N.en[key] : key);
    if (vars) Object.keys(vars).forEach(function (k) {
      v = String(v).split('{' + k + '}').join(vars[k]);
    });
    return String(v);
  }
  function trPage(p) { return tr('k.page.' + p); }
  function catLabel(c) { return tr('k.cat.' + c); }
  function payLabel(m) { return tr('k.pay.' + m); }
  function pname(p) { return tr('p.' + p.id); }
  function errName(k) { return tr('k.errname.' + k); }
  function optsText(o) {
    if (typeof o === 'string') {
      var legacy = {
        '少糖 · 热饮': { sugar: 0, temp: 2 }, '正常糖 · 热饮': { sugar: 1, temp: 2 },
        '多冰 · 少糖': { sugar: 0, temp: 3 }, '少冰 · 正常糖': { sugar: 1, temp: 4 },
        '自定义': null
      };
      o = (legacy[o] === undefined) ? null : legacy[o];
    }
    if (!o) return tr('k.opts.custom');
    return tr('k.opt.sugar.' + o.sugar) + ' · ' + tr('k.opt.temp.' + o.temp);
  }
  /* French strings are ~25% longer and Chinese ones shorter than the source
     English. Shrink only the boxes that actually overflow, keeping the 1:1
     layout intact for everything else. */
  function fitText(e) {
    if (!e || !e.style) return;
    if (!e.dataset.i18nFs) e.dataset.i18nFs = parseFloat(window.getComputedStyle(e).fontSize) || 24;
    var base = +e.dataset.i18nFs;
    e.style.fontSize = base + 'px';
    var box = e.clientWidth ? e : (e.parentElement || e);
    var ow = box.scrollWidth, cw = box.clientWidth, oh = box.scrollHeight, ch = box.clientHeight, k = 1;
    if (cw > 8 && ow > cw + 2) k = Math.min(k, cw / ow);
    if (ch > 8 && oh > ch + 3) k = Math.min(k, ch / oh);
    if (k < 0.995) e.style.fontSize = Math.max(base * 0.56, base * k * 0.97).toFixed(1) + 'px';
  }

  function resetTx() {
    state.cart = clone(DEFAULTS.cart);
    state.custom = clone(DEFAULTS.custom);
    state.payMethod = 'Card'; state.qrIndex = 0; state.errorType = 'generic';
    state.brewStage = 0; state.category = 'CLASSICS'; state.returnTo = '01-home';
    state.langOpen = false;
  }
  function go(page, extra) {
    save();
    var q = [];
    if (DEBUG) q.push('debug=1');
    if (FREEZE) q.push('freeze=1');
    if (extra) q.push(extra);
    location.href = page + '.html' + (q.length ? '?' + q.join('&') : '');
  }
  function goHome() { resetTx(); go('01-home'); }

  /* ---------- artboard scaling (identical to static pages) ---------- */
  function fit() {
    var s = Math.min(window.innerWidth / 1080, window.innerHeight / 1920);
    if (QS.has('raw')) s = 1;
    var st = document.getElementById('stage');
    if (!st) return;
    st.style.transform = 'scale(' + s + ')';
    st.style.left = ((window.innerWidth - 1080 * s) / 2) + 'px';
    st.style.top = ((window.innerHeight - 1920 * s) / 2) + 'px';
  }
  window.addEventListener('resize', fit);
  fit();
  if (QS.has('compare')) document.body.classList.add('compare');

  /* ---------- geometry helpers ---------- */
  function relRect(e) {
    var st = document.getElementById('stage');
    if (!st) return { l: 0, t: 0, w: 0, h: 0 };
    var s = st.getBoundingClientRect(), k = (s.width / 1080) || 1, r = e.getBoundingClientRect();
    return { l: (r.left - s.left) / k, t: (r.top - s.top) / k, w: r.width / k, h: r.height / k };
  }
  function rowContainer(e) {
    var p = e;
    for (var i = 0; i < 6 && p; i++) {
      var r = relRect(p);
      if (r.w > 700 && r.h > 80 && r.h < 170) return p;
      p = p.parentElement;
    }
    return null;
  }

  /* ---------- overlay DOM (all copy comes from the current language) ---------- */
  var stage = document.getElementById('stage');
  var L = document.createElement('div');
  L.id = 'k-layer';
  L.innerHTML =
    '<div id="k-scrim"></div>' +
    '<div id="k-lang"><div class="k-lang-head">' + tr('k.lang.select') + '</div></div>' +
    '<div id="k-qr"><button class="k-qr-close">×</button><h3>' + tr('k.qr.title') + '</h3>' +
    '<p>' + tr('k.qr.sub') + '</p><div class="k-qr-grid"></div></div>' +
    '<div id="k-qr-badge"><canvas></canvas><span></span></div>' +
    '<div id="k-brew"><div class="k-brew-label"></div><div class="k-brew-track"></div><div class="k-brew-names"></div></div>' +
    '<div id="k-ready"><div class="k-ready-track"></div></div>' +
    '<div id="k-countdown"></div>' +
    '<div id="k-toast"></div>';
  stage.appendChild(L);
  var $ = function (id) { return document.getElementById(id); };
  var toastTimer = null, undoPending = null;

  function toast(msg, undoFn) {
    var t = $('k-toast');
    t.innerHTML = '';
    t.appendChild(document.createTextNode(msg));
    if (undoFn) {
      var b = document.createElement('span');
      b.className = 'k-undo'; b.textContent = tr('k.toast.undo');
      b.onclick = function (e) { e.stopPropagation(); undoFn(); hideToast(); };
      t.appendChild(b);
    }
    t.classList.add('on');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(hideToast, undoFn ? 6000 : 2600);
  }
  function hideToast() { $('k-toast').classList.remove('on'); }
  function scrim(on) { $('k-scrim').classList.toggle('on', !!on); }
  function closeOverlays() {
    $('k-lang').classList.remove('on');
    $('k-qr').classList.remove('on');
    scrim(false);
  }

  /* ---------- hit regions ---------- */
  /* [x, y, w, h, action, opt] */
  var H = {
    '01-home': [
      [66, 240, 948, 950, 'shop'], [80, 665, 95, 105, 'slide', -1],
      [905, 665, 95, 105, 'slide', 1], [495, 1188, 32, 40, 'slideTo', 0],
      [531, 1188, 32, 40, 'slideTo', 1], [567, 1188, 32, 40, 'slideTo', 2],
      [66, 1380, 300, 140, 'payInfo', 'Card'], [390, 1380, 300, 140, 'payInfo', 'Tap'],
      [710, 1380, 304, 140, 'payInfo', 'QR Pay'],
      [60, 1565, 960, 135, 'shop'], [60, 1755, 480, 115, 'lang'],
      [545, 1755, 475, 115, 'help']
    ],
    '02-lang': [
      [66, 240, 948, 950, 'shop'], [80, 665, 95, 105, 'slide', -1],
      [905, 665, 95, 105, 'slide', 1], [495, 1188, 32, 40, 'slideTo', 0],
      [531, 1188, 32, 40, 'slideTo', 1], [567, 1188, 32, 40, 'slideTo', 2],
      [60, 1565, 960, 135, 'shop'], [60, 1755, 480, 115, 'lang'],
      [545, 1755, 475, 115, 'help']
    ],
    '03-help': [
      [0, 0, 1080, 1920, 'helpClose'], [820, 515, 90, 90, 'helpClose'],
      [200, 1275, 680, 130, 'helpClose']
    ],
    '04-shopping': [
      [50, 480, 190, 85, 'cat', 'CLASSICS'], [265, 480, 225, 85, 'cat', 'ESPRESSO'],
      [495, 480, 220, 85, 'cat', 'MILK COFFEE'], [755, 480, 110, 85, 'cat', 'ICED'],
      [895, 480, 160, 85, 'cat', 'OTHER'],
      [480, 62, 270, 90, 'lang'], [810, 62, 215, 90, 'help'],
      [60, 655, 305, 340, 'product', 'americano'], [390, 655, 305, 340, 'product', 'caramel-macchiato'],
      [710, 655, 305, 340, 'product', 'coconut-mocha'],
      [60, 1070, 305, 340, 'product', 'espresso'], [390, 1070, 305, 340, 'soldout'],
      [710, 1070, 305, 340, 'product', 'luckin-caffe-mocha'],
      [60, 1485, 305, 340, 'product', 'luckin-cappuccino'], [390, 1485, 305, 340, 'soldout'],
      [710, 1485, 305, 340, 'product', 'shaken-iced-green-tea'],
      [60, 1640, 960, 130, 'cart']
    ],
    '05-custom': [
      [0, 0, 1080, 1920, 'customClose'],
      [765, 45, 131, 64, 'lang'],
      [410, 995, 135, 85, 'opt', 'bean:0'], [544, 995, 165, 85, 'opt', 'bean:1'],
      [735, 995, 135, 85, 'opt', 'bean:2'],
      [370, 1092, 165, 95, 'opt', 'sugar:0'], [545, 1092, 165, 95, 'opt', 'sugar:1'],
      [715, 1092, 165, 95, 'opt', 'sugar:2'],
      [370, 1198, 165, 95, 'opt', 'temp:0'], [545, 1198, 165, 95, 'opt', 'temp:1'],
      [715, 1198, 165, 95, 'opt', 'temp:2'],
      [200, 1300, 350, 110, 'addCart'], [550, 1300, 350, 110, 'confirmCart'],
      [820, 480, 90, 90, 'customClose']
    ],
    '06-cart': [
      [0, 0, 1080, 1140, 'cartClose'], [900, 1175, 90, 90, 'cartClose'],
      [815, 1305, 70, 75, 'qty', '0:-1'], [900, 1305, 70, 75, 'qty', '0:1'],
      [815, 1418, 70, 75, 'qty', '1:-1'], [900, 1418, 70, 75, 'qty', '1:1'],
      [815, 1531, 70, 75, 'qty', '2:-1'], [900, 1531, 70, 75, 'qty', '2:1'],
      [110, 1685, 860, 130, 'pay']
    ],
    '11-card': [
      [0, 0, 1080, 1920, 'finishPay'], [755, 65, 140, 75, 'lang'],
      [910, 65, 140, 75, 'help']
    ],
    '15-wrong': [
      [340, 700, 400, 400, 'cycleError'],
      [200, 1275, 680, 130, 'errorPrimary'],
      [200, 1400, 680, 130, 'goHome']
    ],
    '16-pay': [[0, 0, 1080, 1920, 'skip']],
    '17-making': [[0, 0, 1080, 1920, 'skip']],
    '18-ready': [
      [0, 0, 1080, 1780, 'goHome'],
      [60, 1785, 960, 120, 'goHome']
    ],
    '19-order-confirm': [
      [45, 65, 90, 80, 'cart'],
      [300, 1180, 480, 250, 'qrPicker'],
      [70, 1415, 190, 170, 'method', 'Card'],
      [265, 1415, 190, 170, 'method', 'Tap'],
      [460, 1415, 190, 170, 'method', 'QR Pay'],
      [655, 1415, 190, 170, 'method', 'Scan Phone'],
      [850, 1415, 190, 170, 'method', 'Cash'],
      [80, 1705, 920, 140, 'submitPay']
    ]
  };

  /* A drawer host owns both the shopping hotspots and the sheet hotspots.
     The sheet ones are marked so CSS can mute them while the sheet is closed. */
  var HITS = [];
  if (DRAWER) {
    H['04-shopping'].forEach(function (h) { HITS.push(h.slice()); });
    H['06-cart'].forEach(function (h) {
      HITS.push([h[0], h[1], h[2], h[3], h[4], h[5], true]);
    });
  } else {
    (H[SCREEN] || []).forEach(function (h) { HITS.push(h.slice()); });
  }

  HITS.forEach(function (h) {
    var d = document.createElement('div');
    d.className = 'k-hit' + (h[6] ? ' k-cart-hit' : '');
    d.style.left = h[0] + 'px'; d.style.top = h[1] + 'px';
    d.style.width = h[2] + 'px'; d.style.height = h[3] + 'px';
    d.setAttribute('data-act', h[4]);
    if (h[5] !== undefined) d.setAttribute('data-opt', String(h[5]));
    /* No title attribute: tooltips must not leak internal English identifiers. */
    if (h[4] === 'product') {
      var row = h[1] < 900 ? 0 : (h[1] < 1400 ? 1 : 2);
      var col = h[0] < 380 ? 0 : (h[0] < 700 ? 1 : 2);
      d.setAttribute('data-ps', String(row * 3 + col));
      d.onclick = function (e) {
        e.stopPropagation(); bump(); act('product', this.getAttribute('data-opt'), e);
      };
    } else {
      d.onclick = function (e) { e.stopPropagation(); bump(); act(h[4], h[5], e); };
    }
    L.appendChild(d);
  });

  /* ---------- cart bottom sheet (lives over the shopping artboard) ---------- */
  var drawerOpen = false;
  function boardY(clientY) {
    var r = stage.getBoundingClientRect();
    return (clientY - r.top) / ((r.width / 1080) || 1);
  }
  function syncDrawerRows() {
    if (!DRAWER_EL) return;
    cartRows().forEach(function (row, i) {
      if (row) row.style.display = state.cart[i] ? '' : 'none';
    });
    var qs = DRAWER_EL.querySelectorAll('[data-cart-qty]');
    for (var i = 0; i < qs.length; i++) {
      if (state.cart[i]) qs[i].textContent = String(state.cart[i].qty);
    }
    var cnt = DRAWER_EL.querySelector('[data-cart-count]');
    if (cnt) { cnt.textContent = tr('k.cart.count', { N: cartQty() }); fitText(cnt); }
    /* Pristine state keeps the Figma placeholder prices. Once the cart has
       actually changed, show consistent numbers instead of stale $5.66. */
    if (JSON.stringify(state.cart) !== JSON.stringify(DEFAULTS.cart)) {
      var sub = DRAWER_EL.querySelector('[data-cart-subtotal]');
      var tot = DRAWER_EL.querySelector('[data-cart-total]');
      if (sub) sub.textContent = '$' + cartSum().toFixed(2);
      if (tot) tot.textContent = '$' + cartSum().toFixed(2);
    }
    applyRows();
  }
  function openDrawer() {
    if (!DRAWER_EL) return;
    drawerOpen = true;
    DRAWER_EL.classList.add('on');
    L.classList.add('cart-open');
    syncDrawerRows();
    bump();
  }
  function closeDrawer() {
    if (!DRAWER_EL) return;
    drawerOpen = false;
    DRAWER_EL.classList.remove('on');
    L.classList.remove('cart-open');
    bump();
  }
  if (DRAWER) {
    var dScrim = DRAWER_EL.querySelector('.k-cart-scrim');
    var dX = DRAWER_EL.querySelector('.k-cart-x');
    if (dScrim) dScrim.onclick = function (e) { e.stopPropagation(); closeDrawer(); };
    if (dX) dX.onclick = function (e) { e.stopPropagation(); closeDrawer(); };

    /* Swipe up from the bottom cart bar opens; swipe down on the sheet closes. */
    var gesture = null;
    document.addEventListener('pointerdown', function (e) {
      gesture = { y: e.clientY, by: boardY(e.clientY), open: drawerOpen };
    }, { passive: true });
    document.addEventListener('pointerup', function (e) {
      if (!gesture) return;
      var dy = e.clientY - gesture.y, g = gesture;
      gesture = null;
      if (!g.open && g.by > 1520 && dy < -60) { openDrawer(); return; }
      if (g.open && g.by >= 1160 && dy > 70) closeDrawer();
    }, { passive: true });
    document.addEventListener('pointercancel', function () { gesture = null; });
  }

  /* ---------- single-language rendering ---------- */
  function renderProductNames() {
    var slots = (SCREEN === '05-custom') ? CATALOG : listFor(state.category);
    document.querySelectorAll('[data-slot-name]').forEach(function (e, i) {
      var p = slots[i]; if (p) { e.textContent = tr('p.' + p.id); fitText(e); }
    });
    document.querySelectorAll('[data-slot-cn]').forEach(function (e, i) {
      var p = slots[i]; if (p) { e.textContent = tr('p.' + p.id + '.tag'); fitText(e); }
    });
    document.querySelectorAll('[data-cart-name]').forEach(function (e, i) {
      var it = state.cart[i]; if (it) { e.textContent = tr('p.' + byId(it.pid).id); fitText(e); }
    });
    document.querySelectorAll('[data-order-name]').forEach(function (e, i) {
      var it = state.cart[i]; if (it) { e.textContent = tr('p.' + byId(it.pid).id); fitText(e); }
    });
    document.querySelectorAll('[data-order-opts]').forEach(function (e, i) {
      var it = state.cart[i]; if (it) { e.textContent = optsText(it.opts); fitText(e); }
    });
    var mn = document.querySelector('[data-modal-name]');
    if (mn) { mn.textContent = tr('p.' + state.selectedProduct); fitText(mn); }
    var cc = document.querySelector('[data-cart-count]');
    if (cc) { cc.textContent = tr('k.cart.count', { N: cartQty() }); fitText(cc); }
    var oc = document.querySelector('[data-order-count]');
    if (oc) { oc.textContent = tr('k.order.count', { N: cartQty() }); fitText(oc); }
  }
  function applyDynamicI18n() {
    var head = document.querySelector('#k-lang .k-lang-head');
    if (head) head.textContent = tr('k.lang.select');
    var qh = document.querySelector('#k-qr h3'); if (qh) qh.textContent = tr('k.qr.title');
    var qp = document.querySelector('#k-qr p'); if (qp) qp.textContent = tr('k.qr.sub');
    buildLang();
    document.querySelectorAll('#k-qr .k-qr-cell').forEach(function (c, i) {
      for (var j = 0; j < c.childNodes.length; j++) {
        if (c.childNodes[j].nodeType === 3) c.childNodes[j].nodeValue = tr('k.qr.cell', { N: i + 1 });
      }
    });
    var badge = document.querySelector('#k-qr-badge span');
    if (badge) badge.textContent = tr('k.qr.badge', { N: state.qrIndex + 1 });
    document.querySelectorAll('#k-layer .k-opt').forEach(function (d) {
      var k = d.getAttribute('data-opt').split(':');
      if (k[0] === 'bean') return;
      var sp = d.querySelector('span');
      if (sp) sp.textContent = tr('k.opt.' + k[0] + '.' + k[1]);
    });
    if (SCREEN === '15-wrong') applyError(state.errorType);
    if (SCREEN === '17-making' && !FREEZE) { buildBrew(); paintBrew(state.brewStage || 0); }
    if (SCREEN === '01-home' || SCREEN === '02-lang') applySlide();
    if (DEBUG) { var old = $('k-console'); if (old) old.remove(); buildConsole(); }
    hideToast();
  }
  function applyI18n() {
    document.documentElement.setAttribute('lang', state.lang === 'zh' ? 'zh-CN' : state.lang);
    document.documentElement.setAttribute('data-i18n-lang', state.lang);
    document.querySelectorAll('[data-i18n]').forEach(function (e) {
      e.textContent = tr(e.getAttribute('data-i18n'));
      fitText(e);
    });
    renderProductNames();
    applyDynamicI18n();
    bump();
  }
  function setLang(code) {
    var lg = LANGBY[code];
    if (!lg) return;
    closeOverlays();
    if (code !== state.lang) {
      state.lang = code; save(); applyI18n();
      toast(tr('k.toast.langSet', { L: tr(lg.key) }));
    }
  }

  /* ---------- language dropdown ---------- */
  function langPos() {
    if (SCREEN === '01-home' || SCREEN === '02-lang') return { l: 66, t: 1380 };
    if (SCREEN === '04-shopping') return { l: 480, t: 155 };
    if (SCREEN === '05-custom') return { l: 765, t: 155 };
    return { l: 700, t: 155 };
  }
  function buildLang() {
    var box = $('k-lang');
    box.querySelectorAll('.k-lang-row').forEach(function (n) { n.remove(); });
    LANGS.forEach(function (lg) {
      var r = document.createElement('div');
      r.className = 'k-lang-row' + (lg.code === state.lang ? ' sel' : '');
      r.setAttribute('data-lang', lg.code);
      r.innerHTML = '<span class="k-flag">' + lg.flag + '</span><span>' + tr(lg.key) + '</span>' +
        (lg.code === state.lang ? '<span class="k-check">✓</span>' : '');
      r.onclick = function (e) { e.stopPropagation(); chooseLang(lg); };
      box.appendChild(r);
    });
  }
  function openLang() {
    buildLang();
    var p = langPos(), box = $('k-lang');
    box.style.left = p.l + 'px'; box.style.top = p.t + 'px';
    box.classList.add('on'); scrim(true);
  }
  function chooseLang(lg) {
    var fromHome = (SCREEN === '01-home' || SCREEN === '02-lang');
    state.lang = lg.code; closeOverlays(); save(); applyI18n();
    toast(tr(fromHome ? 'k.toast.langHome' : 'k.toast.langSet', { L: tr(lg.key) }));
    if (fromHome) setTimeout(function () { go('04-shopping'); }, 620);
  }

  /* ---------- carousel ---------- */
  function applySlide() {
    if (SCREEN !== '01-home' && SCREEN !== '02-lang') return;
    var s = state.slides[state.slide] || state.slides[0];
    var img = document.querySelector('.banner-photo');
    if (img) img.src = ROOT + s.img;
    var e = document.querySelector('.eyebrow'), t = document.querySelector('.headline');
    if (e) { e.textContent = tr('k.slide.' + state.slide + '.eyebrow'); fitText(e); }
    if (t) { t.textContent = tr('k.slide.' + state.slide + '.title'); fitText(t); }
    var dots = document.querySelectorAll('#dots .dot');
    for (var i = 0; i < dots.length; i++) dots[i].classList.toggle('active', i === state.slide);
  }
  function moveSlide(d) {
    state.slide = (state.slide + d + state.slides.length) % state.slides.length;
    save();
    if (SCREEN === '02-lang') { go('01-home'); return; }
    applySlide();
  }
  if (SCREEN === '01-home') {
    if (!FREEZE) {
      setInterval(function () {
        if ($('k-lang').classList.contains('on')) return;
        state.slide = (state.slide + 1) % state.slides.length; save(); applySlide();
      }, 6000);
    }
    applySlide();
  } else if (SCREEN === '02-lang') applySlide();

  /* ---------- QR codes (up to 9, deterministic placeholder patterns) ---------- */
  function drawQR(cv, seed) {
    var n = 25; cv.width = n; cv.height = n;
    var c = cv.getContext('2d');
    c.fillStyle = '#FFFDF9'; c.fillRect(0, 0, n, n);
    var v = seed * 2654435761 + 12345;
    function rnd() { v = (v * 1103515245 + 12345) & 0x7fffffff; return v / 0x7fffffff; }
    function finder(ox, oy) {
      c.fillStyle = '#3A2A1E'; c.fillRect(ox, oy, 7, 7);
      c.fillStyle = '#FFFDF9'; c.fillRect(ox + 1, oy + 1, 5, 5);
      c.fillStyle = '#3A2A1E'; c.fillRect(ox + 2, oy + 2, 3, 3);
    }
    c.fillStyle = '#3A2A1E';
    for (var y = 0; y < n; y++) for (var x = 0; x < n; x++) {
      var inFinder = (x < 8 && y < 8) || (x > n - 9 && y < 8) || (x < 8 && y > n - 9);
      if (!inFinder && rnd() > 0.52) c.fillRect(x, y, 1, 1);
    }
    finder(0, 0); finder(n - 7, 0); finder(0, n - 7);
  }
  function buildQRPicker() {
    var g = $('k-qr').querySelector('.k-qr-grid');
    g.innerHTML = '';
    for (var i = 0; i < 9; i++) {
      (function (i) {
        var cell = document.createElement('div');
        cell.className = 'k-qr-cell' + (i === state.qrIndex ? ' sel' : '');
        var cv = document.createElement('canvas'); drawQR(cv, i + 1);
        cell.appendChild(cv);
        cell.appendChild(document.createTextNode(tr('k.qr.cell', { N: i + 1 })));
        cell.onclick = function (e) {
          e.stopPropagation(); state.qrIndex = i; save();
          closeOverlays(); showQRBadge(); toast(tr('k.qr.picked', { N: i + 1 }));
        };
        g.appendChild(cell);
      })(i);
    }
    $('k-qr').querySelector('.k-qr-close').onclick = function (e) { e.stopPropagation(); closeOverlays(); };
  }
  function showQRBadge() {
    if (SCREEN !== '19-order-confirm') return;
    var b = $('k-qr-badge');
    drawQR(b.querySelector('canvas'), state.qrIndex + 1);
    b.querySelector('span').textContent = tr('k.qr.badge', { N: state.qrIndex + 1 });
    b.classList.add('on');
    b.onclick = function (e) { e.stopPropagation(); openQRPicker(); };
  }
  function openQRPicker() { buildQRPicker(); $('k-qr').classList.add('on'); scrim(true); }

  /* ---------- customization options ---------- */
  function paintOptions() {
    var opts = L.querySelectorAll('.k-opt');
    for (var i = 0; i < opts.length; i++) {
      var key = opts[i].dataset.opt.split(':');
      opts[i].classList.toggle('sel', state.custom[key[0]] === +key[1]);
    }
  }
  if (SCREEN === '05-custom') {
    var OPTBOX = [
      [410, 995, 135, 85, 'bean:0', '●'], [544, 995, 165, 85, 'bean:1', '●●'], [735, 995, 135, 85, 'bean:2', '●●●'],
      [370, 1092, 165, 95, 'sugar:0', tr('k.opt.sugar.0')], [545, 1092, 165, 95, 'sugar:1', tr('k.opt.sugar.1')],
      [715, 1092, 165, 95, 'sugar:2', tr('k.opt.sugar.2')],
      [370, 1198, 165, 95, 'temp:0', tr('k.opt.temp.0')], [545, 1198, 165, 95, 'temp:1', tr('k.opt.temp.1')],
      [715, 1198, 165, 95, 'temp:2', tr('k.opt.temp.2')]
    ];
    OPTBOX.forEach(function (o) {
      var d = document.createElement('div');
      d.className = 'k-opt'; d.dataset.opt = o[4];
      d.style.left = o[0] + 'px'; d.style.top = o[1] + 'px';
      d.style.width = o[2] + 'px'; d.style.height = o[3] + 'px';
      var lab = document.createElement('span'); lab.textContent = o[5]; d.appendChild(lab);
      d.onclick = function (e) {
        e.stopPropagation(); bump();
        var k = o[4].split(':'); state.custom[k[0]] = +k[1]; save();
        L.classList.add('opts-active'); paintOptions();
        toast(tr('k.toast.opt', { K: tr('k.opt.' + k[0]), V: +k[1] + 1 }));
      };
      L.appendChild(d);
    });
    /* Keep the original artboard pristine until the user actually changes an option. */
  }

  /* ---------- product catalog rendering ---------- */
  function applySlots(list) {
    if (!DRAWER) return;
    var imgs = document.querySelectorAll('.screen [data-slot]');
    var hits = document.querySelectorAll('#k-layer .k-hit[data-act="product"]');
    var i, p, s;
    for (i = 0; i < imgs.length; i++) {
      s = +imgs[i].getAttribute('data-slot'); p = list[s];
      if (p) imgs[i].style.backgroundImage = 'url(' + ROOT + p.card + ')';
    }
    for (i = 0; i < hits.length; i++) {
      s = +hits[i].getAttribute('data-ps'); p = list[s];
      if (p) hits[i].setAttribute('data-opt', p.id);
    }
    renderProductNames();
  }
  function applyModalProduct() {
    if (SCREEN !== '05-custom') return;
    var p = byId(state.selectedProduct);
    var img = document.querySelector('#stage [data-modal-img]');
    var t = document.querySelector('#stage [data-modal-name]');
    if (img) img.style.backgroundImage = 'url(' + ROOT + p.hero + ')';
    if (t) { t.textContent = tr('p.' + p.id); fitText(t); }
  }
  function applyRows() { renderProductNames(); }

  /* ---------- cart ---------- */
  function cartQty() { return state.cart.reduce(function (a, i) { return a + i.qty; }, 0); }
  function cartSum() { return state.cart.reduce(function (a, i) { return a + i.qty * i.price; }, 0); }
  function cartRows() {
    var out = [];
    if (!DRAWER_EL) return out;
    DRAWER_EL.querySelectorAll('[data-cart-name]').forEach(function (t) {
      out.push(rowContainer(t) || t.parentElement);
    });
    return out;
  }
  function refreshCart() {
    if (!DRAWER_EL) return;
    syncDrawerRows();
    var sub = DRAWER_EL.querySelector('[data-cart-subtotal]');
    var tot = DRAWER_EL.querySelector('[data-cart-total]');
    if (sub) sub.textContent = '$' + cartSum().toFixed(2);
    if (tot) tot.textContent = '$' + cartSum().toFixed(2);
  }
  /* Opening the sheet only syncs rows; totals stay 1:1 with Figma until an edit. */

  /* ---------- error page (icon + copy only) ---------- */
  var errorEls = null;
  function errorNodes() {
    if (errorEls) return errorEls;
    var t = [], s = [], b = [];
    document.querySelectorAll('#stage [data-i18n]').forEach(function (e) {
      var k = e.getAttribute('data-i18n');
      if (k === 'Something went wrong.') t.push(e);
      else if (k === 'Please try again.') s.push(e);
      else if (k === 'HELP') b.push(e);
    });
    errorEls = { t: t, s: s, b: b };
    return errorEls;
  }
  /* The design composes the status icon from an outer ring (kept) + an inner glyph (swapped). */
  function iconDataURI(kind) {
    var s = '#3A2A1E', b = '';
    if (kind === 'ban') b = '<line x1="26" y1="74" x2="74" y2="26" stroke="' + s + '" stroke-width="9" stroke-linecap="round"/>';
    else if (kind === 'card') b = '<rect x="20" y="32" width="60" height="38" rx="7" fill="none" stroke="' + s + '" stroke-width="7"/><line x1="20" y1="46" x2="80" y2="46" stroke="' + s + '" stroke-width="6"/><line x1="30" y1="60" x2="52" y2="60" stroke="' + s + '" stroke-width="6" stroke-linecap="round"/>';
    else if (kind === 'wrench') b = '<path d="M70 27a19 19 0 0 0-24 24L29 68a9 9 0 0 0 13 13l17-17a19 19 0 0 0 21-26L66 52l-11-3-3-11 18-11z" fill="none" stroke="' + s + '" stroke-width="7" stroke-linejoin="round"/>';
    else if (kind === 'clock') b = '<circle cx="50" cy="50" r="27" fill="none" stroke="' + s + '" stroke-width="7"/><line x1="50" y1="32" x2="50" y2="51" stroke="' + s + '" stroke-width="7" stroke-linecap="round"/><line x1="50" y1="51" x2="65" y2="59" stroke="' + s + '" stroke-width="7" stroke-linecap="round"/>';
    var svg = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">' + b + '</svg>';
    return 'data:image/svg+xml,' + encodeURIComponent(svg);
  }
  function applyError(kind) {
    if (SCREEN !== '15-wrong') return;
    if (!ERRKEYS.includes(kind)) kind = 'generic';
    var icons = { generic: 'ban', payment: 'card', machine: 'wrench', timeout: 'clock' };
    var title = tr('k.err.' + kind + '.title'), sub = tr('k.err.' + kind + '.sub'), btn = tr('k.err.' + kind + '.btn');
    var n = errorNodes();
    n.t.forEach(function (e) { e.textContent = title; fitText(e); });
    n.s.forEach(function (e) { e.textContent = sub; fitText(e); });
    n.b.forEach(function (e) { e.textContent = btn; fitText(e); });
    var imgs = document.querySelectorAll('.screen img'), glyph = null, minA = Infinity;
    for (var i = 0; i < imgs.length; i++) {
      var e2 = imgs[i], r = relRect(e2);
      if (r.l < 410 || r.l > 690 || r.t < 750 || r.t > 1030 || r.w < 40 || r.w > 170 || r.h < 40 || r.h > 170) continue;
      var a = r.w * r.h;
      if (a < minA) { minA = a; glyph = e2; }
    }
    if (glyph) glyph.src = iconDataURI(icons[kind]);
  }
  if (SCREEN === '15-wrong') { errorNodes(); if (state.errorType !== 'generic') applyError(state.errorType); }

  /* ---------- brewing / ready progress ---------- */
  function buildBrew() {
    var track = $('k-brew').querySelector('.k-brew-track'), names = $('k-brew').querySelector('.k-brew-names');
    track.innerHTML = ''; names.innerHTML = '';
    for (var i = 0; i < 4; i++) {
      var s = document.createElement('div'); s.className = 'k-brew-seg';
      s.innerHTML = '<i></i>'; track.appendChild(s);
      var t = document.createElement('span'); t.textContent = tr('k.brew.name.' + i); names.appendChild(t);
    }
  }
  function paintBrew(step) {
    var segs = $('k-brew').querySelectorAll('.k-brew-seg'), ns = $('k-brew').querySelectorAll('.k-brew-names span');
    for (var i = 0; i < segs.length; i++) { segs[i].classList.toggle('done', i < step); ns[i].classList.toggle('done', i < step); }
    $('k-brew').querySelector('.k-brew-label').textContent =
      step >= 4 ? tr('k.brew.ready') : tr('k.brew.' + step) + '  ·  ' + step + '/4';
    $('k-brew').classList.toggle('flash', step >= 4);
  }
  function buildReady() {
    var t = $('k-ready').querySelector('.k-ready-track'); t.innerHTML = '';
    for (var i = 0; i < 4; i++) { var s = document.createElement('i'); s.className = 'done'; t.appendChild(s); }
  }
  if (SCREEN === '17-making' && !FREEZE) { buildBrew(); $('k-brew').classList.add('on'); paintBrew(state.brewStage || 0); }
  if (SCREEN === '18-ready' && !FREEZE) { buildReady(); $('k-ready').classList.add('on'); }

  /* ---------- idle timeout ---------- */
  var last = Date.now();
  function bump() { last = Date.now(); }
  ['pointerdown', 'keydown', 'wheel', 'touchstart'].forEach(function (ev) {
    document.addEventListener(ev, bump, { passive: true });
  });
  $('k-scrim').onclick = function () { closeOverlays(); };

  var cd = $('k-countdown');
  if (DEBUG) cd.classList.add('on');
  function tick() {
    var limit = TIMEOUTS[SCREEN] || 0;
    if (limit) {
      var left = Math.max(0, limit - (Date.now() - last) / 1000);
      if (DEBUG) cd.textContent = tr('k.cd.auto', { N: Math.ceil(left) });
      if (left <= 0) { goHome(); return; }
    } else if (DEBUG) cd.textContent = tr('k.cd.home');
    setTimeout(tick, 250);
  }
  if (!FREEZE) tick();

  /* ---------- auto flow ---------- */
  function finishPayment() {
    if (state.payResult === 'success') { state.brewStage = 0; save(); go('16-pay'); }
    else { state.errorType = state.payResult; save(); go('15-wrong'); }
  }
  function skip() {
    if (SCREEN === '16-pay') go('17-making');
    else if (SCREEN === '17-making') { state.brewStage = 4; save(); go('18-ready'); }
    else if (SCREEN === '11-card') finishPayment();
    else if (SCREEN === '18-ready') goHome();
    else goHome();
  }
  function startAuto() {
    if (SCREEN === '16-pay') setTimeout(function () { go('17-making'); }, 2800);
    else if (SCREEN === '11-card') setTimeout(finishPayment, 4200);
    else if (SCREEN === '17-making') {
      if ((state.brewStage || 0) >= 4) { go('18-ready'); return; }
      setTimeout(function advance() {
        state.brewStage = (state.brewStage || 0) + 1; save(); paintBrew(state.brewStage);
        if (state.brewStage >= 4) setTimeout(function () { go('18-ready'); }, 900);
        else setTimeout(advance, 5000);
      }, 5000);
    }
  }
  if (!FREEZE) startAuto();

  /* ---------- initial product state ---------- */
  if (DRAWER && state.category !== 'CLASSICS') applySlots(listFor(state.category));
  if (SCREEN === '05-custom') applyModalProduct();
  if (DRAWER || SCREEN === '19-order-confirm') applyRows();

  /* ---------- action dispatcher ---------- */
  function act(a, o, e) {
    switch (a) {
      case 'shop': go('04-shopping'); break;
      case 'slide': moveSlide(o); break;
      case 'slideTo':
        state.slide = o; save();
        if (SCREEN === '02-lang') go('01-home'); else applySlide();
        break;
      case 'payInfo':
        state.payMethod = o; save();
        toast(tr('k.toast.payinfo', { M: payLabel(o) }));
        break;
      case 'lang': openLang(); break;
      case 'help': state.returnTo = SCREEN; save(); go('03-help'); break;
      case 'helpClose': closeOverlays(); go(state.returnTo && state.returnTo !== '03-help' ? state.returnTo : '01-home'); break;
      case 'cat':
        state.category = o; save(); applySlots(listFor(o));
        toast(tr('k.toast.cat', { C: catLabel(o), N: CATALOG.filter(function (p) { return p.cat === o; }).length }));
        break;
      case 'soldout': toast(tr('k.toast.soldout')); break;
      case 'product': state.selectedProduct = o; save(); go('05-custom'); break;
      case 'opt': break; /* handled by k-opt elements */
      case 'customClose': go('04-shopping'); break;
      case 'addCart': {
        var pa = byId(state.selectedProduct);
        state.cart.push({ pid: pa.id, name: pa.short, price: 3.50, qty: 1,
          opts: { sugar: state.custom.sugar, temp: state.custom.temp } });
        save(); toast(tr('k.toast.added', { P: pname(pa) }));
        setTimeout(function () { go('04-shopping'); }, 700);
        break;
      }
      case 'confirmCart': {
        var pb = byId(state.selectedProduct);
        state.cart.push({ pid: pb.id, name: pb.short, price: 3.50, qty: 1,
          opts: { sugar: state.custom.sugar, temp: state.custom.temp } });
        save(); go('06-cart');
        break;
      }
      case 'cart': if (DRAWER) openDrawer(); else go('06-cart'); break;
      case 'cartClose': closeDrawer(); break;
      case 'qty': {
        var p = String(o).split(':'), idx = +p[0], d = +p[1];
        var item = state.cart[idx]; if (!item) break;
        var label = tr('p.' + byId(item.pid).id);
        if (d > 0) { item.qty++; save(); refreshCart(); toast(label + ' × ' + item.qty); }
        else if (item.qty > 1) { item.qty--; save(); refreshCart(); toast(label + ' × ' + item.qty); }
        else {
          var removed = item, pos = idx;
          state.cart.splice(idx, 1); save(); refreshCart();
          toast(tr('k.toast.removed', { P: label }), function () {
            state.cart.splice(pos, 0, removed); save(); refreshCart();
            toast(tr('k.toast.restored', { P: label }));
          });
        }
        break;
      }
      case 'pay': go('19-order-confirm'); break;
      case 'method':
        state.payMethod = o; save(); toast(tr('k.toast.method', { M: payLabel(o) }));
        break;
      case 'qrPicker': openQRPicker(); break;
      case 'submitPay':
        if (state.payMethod === 'QR Pay' || state.payMethod === 'Scan Phone') {
          toast(tr('k.qr.scan')); showQRBadge();
          setTimeout(function () {
            if (state.payResult === 'success') { state.brewStage = 0; save(); go('16-pay'); }
            else { state.errorType = state.payResult; save(); go('15-wrong'); }
          }, 1800);
        } else go('11-card');
        break;
      case 'finishPay': finishPayment(); break;
      case 'skip': skip(); break;
      case 'goHome': goHome(); break;
      case 'cycleError': {
        var i = (ERRKEYS.indexOf(state.errorType) + 1) % ERRKEYS.length;
        state.errorType = ERRKEYS[i]; save(); applyError(state.errorType);
        toast(tr('k.toast.errorState', { E: errName(state.errorType) }));
        break;
      }
      case 'errorPrimary':
        if (state.errorType === 'machine') { state.returnTo = SCREEN; save(); go('03-help'); }
        else if (state.errorType === 'payment') go('19-order-confirm');
        else if (state.errorType === 'timeout') go('11-card');
        else { state.returnTo = SCREEN; save(); go('03-help'); }
        break;
      default: break;
    }
  }

  /* ---------- keyboard ---------- */
  document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape') {
      if (drawerOpen) closeDrawer(); else closeOverlays();
      return;
    }
    if (e.key === 'c' || e.key === 'C') {
      if (DRAWER) { if (drawerOpen) closeDrawer(); else openDrawer(); }
      return;
    }
    if (e.key === 'l' || e.key === 'L') {
      var ix = 0;
      LANGS.forEach(function (l, i) { if (l.code === state.lang) ix = i; });
      setLang(LANGS[(ix + 1) % LANGS.length].code);
      return;
    }
    if (e.key === 'g' || e.key === 'G') { location.href = '../all.html'; return; }
    if (e.key === 'h' || e.key === 'H') { state.returnTo = SCREEN; save(); go('03-help'); return; }
    if (e.key === ' ') { e.preventDefault(); skip(); return; }
    if (e.key === 'ArrowRight' || e.key === 'ArrowLeft') {
      var i = ORDER.indexOf(SCREEN), j = i + (e.key === 'ArrowRight' ? 1 : -1);
      if (j >= 0 && j < ORDER.length) go(ORDER[j]);
    }
  });

  /* ---------- debug console (also rendered in the current language) ---------- */
  var consoleTimer = null;
  function buildConsole() {
    var tg = $('k-toggle');
    if (!tg) {
      tg = document.createElement('button');
      tg.id = 'k-toggle'; tg.textContent = '⚙'; tg.className = 'on';
      document.body.appendChild(tg);
    }
    tg.title = tr('k.console.toggle');
    var box = document.createElement('div');
    box.id = 'k-console';
    box.innerHTML =
      '<div class="kc-h"><span>' + tr('k.console.head') + '</span><button id="kc-x">×</button></div>' +
      '<div class="kc-b">' +
      '<div class="kc-stat" id="kc-stat"></div>' +
      '<label>' + tr('k.console.catalog') + '</label><div class="kc-stat" id="kc-catalog"></div>' +
      '<label>' + tr('k.console.customize') + '</label><select id="kc-product"></select>' +
      '<label>' + tr('k.console.jump') + '</label><select id="kc-page"></select>' +
      '<label>' + tr('k.console.lang') + '</label><select id="kc-lang"></select>' +
      '<label>' + tr('k.console.result') + '</label><select id="kc-result"></select>' +
      '<label>' + tr('k.console.error') + '</label><select id="kc-error"></select>' +
      '<label>' + tr('k.console.method') + '</label><select id="kc-method"></select>' +
      '<div class="kc-row"><button class="kc-btn" id="kc-skip">' + tr('k.console.skip') + '</button>' +
      '<button class="kc-btn alt" id="kc-reset">' + tr('k.console.reset') + '</button></div>' +
      '<label>' + tr('k.console.img2') + '</label><input id="kc-img2" placeholder="assets/img/…">' +
      '<label>' + tr('k.console.img3') + '</label><input id="kc-img3" placeholder="assets/img/…">' +
      '<button class="kc-btn" id="kc-slides">' + tr('k.console.apply') + '</button>' +
      '<label>' + tr('k.console.drawer') + '</label><div class="kc-stat">' + tr('k.console.drawerHint') + '</div>' +
      '<label>' + tr('k.console.keys') + '</label><div class="kc-stat">' + tr('k.console.keysHint') + '</div>' +
      '</div>';
    document.body.appendChild(box);
    tg.onclick = function () { box.classList.toggle('on'); };
    $('kc-x').onclick = function () { box.classList.remove('on'); };

    function fill(sel, arr, val) {
      arr.forEach(function (x) {
        var o = document.createElement('option');
        o.value = (x.v !== undefined ? x.v : x); o.textContent = (x.t !== undefined ? x.t : x);
        if (String(o.value) === String(val)) o.selected = true;
        sel.appendChild(o);
      });
    }
    $('kc-catalog').textContent = tr('k.console.catalogStat', { N: CATALOG.length }) + '\n' +
      CATS.map(function (c) {
        return catLabel(c) + ': ' + CATALOG.filter(function (p) { return p.cat === c; }).length;
      }).join('  ·  ');
    var pr = $('kc-product');
    CATALOG.forEach(function (p) {
      var o = document.createElement('option');
      o.value = p.id; o.textContent = tr('k.console.productOpt', { P: pname(p), C: catLabel(p.cat) });
      if (p.id === state.selectedProduct) o.selected = true;
      pr.appendChild(o);
    });
    pr.onchange = function () { state.selectedProduct = this.value; save(); go('05-custom'); };
    var pg = $('kc-page');
    fill(pg, ORDER.map(function (p) { return { v: p, t: trPage(p) }; }), SCREEN);
    pg.onchange = function () { go(pg.value); };
    var lg = $('kc-lang');
    fill(lg, LANGS.map(function (l) { return { v: l.code, t: tr(l.key) }; }), state.lang);
    lg.onchange = function () { setLang(this.value); };
    fill($('kc-result'), [{ v: 'success', t: tr('k.result.success') }, { v: 'payment', t: tr('k.result.payment') },
      { v: 'machine', t: tr('k.result.machine') }, { v: 'timeout', t: tr('k.result.timeout') }], state.payResult);
    $('kc-result').onchange = function () {
      state.payResult = this.value; save();
      toast(tr('k.toast.nextPay', { R: tr('k.result.' + this.value) }));
    };
    fill($('kc-error'), ERRKEYS.map(function (k) { return { v: k, t: errName(k) }; }), state.errorType);
    $('kc-error').onchange = function () {
      state.errorType = this.value; save(); applyError(this.value);
      toast(tr('k.toast.errorCopy', { E: errName(this.value) }));
    };
    fill($('kc-method'), PAYMETHODS.map(function (m) { return { v: m, t: payLabel(m) }; }), state.payMethod);
    $('kc-method').onchange = function () {
      state.payMethod = this.value; save();
      toast(tr('k.toast.method', { M: payLabel(this.value) }));
    };
    $('kc-skip').onclick = function () { skip(); };
    $('kc-reset').onclick = function () { localStorage.removeItem(KEY); location.reload(); };
    $('kc-img2').value = state.slides[1].img;
    $('kc-img3').value = state.slides[2].img;
    $('kc-slides').onclick = function () {
      state.slides[1].img = $('kc-img2').value.trim() || SLIDES_DEFAULT[1].img;
      state.slides[2].img = $('kc-img3').value.trim() || SLIDES_DEFAULT[2].img;
      save(); applySlide(); toast(tr('k.toast.carousel'));
    };
    if (consoleTimer) clearInterval(consoleTimer);
    consoleTimer = setInterval(function () {
      var lim = TIMEOUTS[SCREEN] || 0;
      var left = lim ? Math.max(0, lim - (Date.now() - last) / 1000).toFixed(0) : '—';
      var el = $('kc-stat');
      if (!el) { clearInterval(consoleTimer); return; }
      el.textContent = [
        tr('k.console.line.screen', { V: trPage(SCREEN) }),
        tr('k.console.line.lang', { L: tr('k.lang.name.' + state.lang), S: state.slide + 1 }),
        tr('k.console.line.product', { P: pname(byId(state.selectedProduct)), C: catLabel(state.category) }),
        tr('k.console.line.cart', {
          N: cartQty(), T: '$' + cartSum().toFixed(2),
          D: DRAWER ? tr(drawerOpen ? 'k.console.sheetOpen' : 'k.console.sheetClosed') : ''
        }),
        tr('k.console.line.method', { M: payLabel(state.payMethod), Q: state.qrIndex + 1 }),
        tr('k.console.line.result', { R: tr('k.result.' + state.payResult), B: state.brewStage || 0 }),
        tr('k.console.line.idle', { V: left })
      ].join('\n');
    }, 300);
  }

  /* ---------- boot ---------- */
  document.querySelectorAll('[data-lang-toggle]').forEach(function (e) {
    e.style.cursor = 'pointer';
    e.onclick = function (ev) { ev.stopPropagation(); openLang(); };
  });
  applyI18n();
  /* Direct links to 06-cart.html show the sheet sliding up over the menu. */
  if (AUTO_DRAWER) {
    requestAnimationFrame(function () { requestAnimationFrame(openDrawer); });
    setTimeout(openDrawer, 120);
  }
  if (SCREEN === '19-order-confirm' && state.qrIndex > 0) showQRBadge();

  /* Test hooks: purity audit + programmatic language switching. */
  window.__kioskAudit = function () { return I18N.audit(state.lang); };
  window.__kioskSetLang = setLang;
  window.__kioskLang = function () { return state.lang; };
})();
'''


def main():
    old = OLD.read_text()
    m = re.search(r'/\* CATALOG_START \*/.*?/\* CATALOG_END \*/', old, re.S)
    if not m:
        raise RuntimeError('CATALOG block not found in ' + str(OLD))
    out = TEMPLATE.replace('@@CATALOG@@', m.group(0))
    TARGET.write_text(out)
    print('kiosk.js rewritten: %d lines, catalog block %d bytes preserved' % (
        len(out.splitlines()), len(m.group(0))))


if __name__ == '__main__':
    main()
