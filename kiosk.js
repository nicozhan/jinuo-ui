/* Jetinno Coffee Kiosk — interactive state machine (single-language edition)
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
  /* v13 · visual demo: the shopping screen renders 30 product cards, repeating
     catalogue artwork when there are fewer than 30 unique drinks. */
  var GRID30 = (SCREEN === '04-shopping' || SCREEN === '05-custom');
  var GRID_COUNT = 30, GRID30_H = 0;
  var CART_MAX = 2; /* v7 · two drinks per order */
  var I18N = window.KIOSK_I18N;
  if (!I18N) throw new Error('i18n.js must be loaded before kiosk.js');
  var ORDER = ['01-home', '02-lang', '03-help', '04-shopping', '05-custom', '06-cart',
    '11-card', '15-wrong', '16-pay', '17-making', '18-ready', '19-order-confirm'];

  /* ---- v3 refinement (all 12 screens carry body[data-refine]) ---- */
  var REFINE = !!(document.body && document.body.hasAttribute('data-refine'));
  var TYPE_TOKENS = [76, 64, 56, 48, 40, 32, 28, 24, 20, 18, 16, 14];
  function snapNear(px) {
    var best = TYPE_TOKENS[0], d = Infinity;
    for (var i = 0; i < TYPE_TOKENS.length; i++) {
      var dd = Math.abs(TYPE_TOKENS[i] - px);
      if (dd < d || (dd === d && TYPE_TOKENS[i] > best)) {
        best = TYPE_TOKENS[i]; d = dd;
      }
    }
    return best;
  }
  function snapFloor(px) {
    var best = TYPE_TOKENS[TYPE_TOKENS.length - 1];
    for (var i = 0; i < TYPE_TOKENS.length; i++)
      if (TYPE_TOKENS[i] <= px && TYPE_TOKENS[i] > best) best = TYPE_TOKENS[i];
    return best;
  }

    /* CATALOG_START */
  var CURRENCY = "CHF";
  var DEFAULT_PRICE = 3.5;
  var CATALOG = [{"id":"americano","name":"Americano","short":"Americano","cn":"美式咖啡","cat":"CLASSICS","kind":"classic","price":3.5,"card":"assets/products/card/americano.png","hero":"assets/products/hero/americano.png"},{"id":"caramel-macchiato","name":"Caramel Macchiato","short":"Caramel Macchiato","cn":"焦糖玛奇朵","cat":"CLASSICS","kind":"milk","price":3.5,"card":"assets/products/card/caramel-macchiato.png","hero":"assets/products/hero/caramel-macchiato.png"},{"id":"coconut-mocha","name":"Coconut Mocha","short":"Coconut Mocha","cn":"生椰摩卡","cat":"CLASSICS","kind":"milk","price":3.5,"card":"assets/products/card/coconut-mocha.png","hero":"assets/products/hero/coconut-mocha.png"},{"id":"espresso","name":"Espresso","short":"Espresso","cn":"意式浓缩","cat":"ESPRESSO","kind":"espresso","price":3.5,"card":"assets/products/card/espresso.png","hero":"assets/products/hero/espresso.png"},{"id":"luckin-americano","name":"Luckin Americano","short":"Luckin Americano","cn":"瑞幸冰美式","cat":"ESPRESSO","kind":"espresso","price":3.5,"card":"assets/products/card/luckin-americano.png","hero":"assets/products/hero/luckin-americano.png"},{"id":"luckin-caffe-mocha","name":"Luckin Caffe Mocha","short":"Luckin Caffe Mocha","cn":"瑞幸摩卡","cat":"MILK","kind":"milk","price":3.5,"card":"assets/products/card/luckin-caffe-mocha.png","hero":"assets/products/hero/luckin-caffe-mocha.png"},{"id":"luckin-cappuccino","name":"Luckin Cappuccino","short":"Luckin Cappuccino","cn":"瑞幸卡布奇诺","cat":"MILK","kind":"milk","price":3.5,"card":"assets/products/card/luckin-cappuccino.png","hero":"assets/products/hero/luckin-cappuccino.png"},{"id":"luckin-latte","name":"Luckin Latte","short":"Luckin Latte","cn":"瑞幸拿铁","cat":"MILK","kind":"milk","price":3.5,"card":"assets/products/card/luckin-latte.png","hero":"assets/products/hero/luckin-latte.png"},{"id":"shaken-iced-green-tea","name":"Shaken Iced Green Tea","short":"Shaken Iced Green Tea","cn":"冰摇柠檬绿茶","cat":"TEA","kind":"tea","price":3.5,"card":"assets/products/card/shaken-iced-green-tea.png","hero":"assets/products/hero/shaken-iced-green-tea.png"},{"id":"tims-coconut-caramel","name":"Tims Coconut Caramel Macchiato","short":"Coconut Caramel","cn":"生椰焦糖玛奇朵","cat":"MILK","kind":"milk","price":3.5,"card":"assets/products/card/tims-coconut-caramel.png","hero":"assets/products/hero/tims-coconut-caramel.png"},{"id":"tims-coconut-matcha","name":"Tims Coconut Iced Matcha","short":"Coconut Iced Matcha","cn":"生椰冰抹茶","cat":"TEA","kind":"tea","price":3.5,"card":"assets/products/card/tims-coconut-matcha.png","hero":"assets/products/hero/tims-coconut-matcha.png"},{"id":"tims-french-vanilla","name":"Tims French Vanilla","short":"French Vanilla","cn":"法式香草拿铁","cat":"MILK","kind":"milk","price":3.5,"card":"assets/products/card/tims-french-vanilla.png","hero":"assets/products/hero/tims-french-vanilla.png"},{"id":"tims-hibiscus","name":"Tims Hibiscus Cold Brew","short":"Hibiscus Cold Brew","cn":"洛神花冷萃","cat":"TEA","kind":"tea","price":3.5,"card":"assets/products/card/tims-hibiscus.png","hero":"assets/products/hero/tims-hibiscus.png"},{"id":"snowcap-mocha","name":"Snowcap Marshmallow Mocha","short":"Snowcap Mocha","cn":"雪顶棉花糖摩卡","cat":"FRAPPE","kind":"frape","price":3.5,"card":"assets/products/card/snowcap-mocha.png","hero":"assets/products/hero/snowcap-mocha.png"},{"id":"tims-cafe-mocha","name":"Tims Cafe Mocha","short":"Tims Cafe Mocha","cn":"咖啡摩卡","cat":"MILK","kind":"milk","price":3.5,"card":"assets/products/card/tims-cafe-mocha.png","hero":"assets/products/hero/tims-cafe-mocha.png"},{"id":"tims-caramel-macchiato","name":"Tims Caramel Macchiato","short":"Tims Caramel Macchiato","cn":"焦糖玛奇朵","cat":"MILK","kind":"milk","price":3.5,"card":"assets/products/card/tims-caramel-macchiato.png","hero":"assets/products/hero/tims-caramel-macchiato.png"},{"id":"java-chip-frappuccino","name":"Java Chip Frappuccino","short":"Java Chip Frappé","cn":"摩卡星冰乐","cat":"FRAPPE","kind":"frape","price":3.5,"card":"assets/products/card/java-chip-frappuccino.png","hero":"assets/products/hero/java-chip-frappuccino.png"},{"id":"tims-iced-mango-matcha","name":"Tims Iced Mango Matcha","short":"Mango Matcha","cn":"芒果冰抹茶","cat":"TEA","kind":"tea","price":3.5,"card":"assets/products/card/tims-iced-mango-matcha.png","hero":"assets/products/hero/tims-iced-mango-matcha.png"},{"id":"tims-iced-maple-cinnamon-latte","name":"Tims Iced Maple Cinnamon Latte","short":"Maple Cinnamon Latte","cn":"枫糖肉桂冰拿铁","cat":"MILK","kind":"milk","price":3.5,"card":"assets/products/card/tims-iced-maple-cinnamon-latte.png","hero":"assets/products/hero/tims-iced-maple-cinnamon-latte.png"},{"id":"tims-mango-shake","name":"Tims Mango Shake","short":"Mango Shake","cn":"芒果奶昔","cat":"FRAPPE","kind":"frape","price":3.5,"card":"assets/products/card/tims-mango-shake.png","hero":"assets/products/hero/tims-mango-shake.png"},{"id":"very-berry-hibiscus","name":"Very Berry Hibiscus","short":"Very Berry Hibiscus","cn":"莓果洛神花","cat":"TEA","kind":"tea","price":3.5,"card":"assets/products/card/very-berry-hibiscus.png","hero":"assets/products/hero/very-berry-hibiscus.png"},{"id":"vietnamese-spanish-latte","name":"Vietnamese Spanish Latte","short":"Vietnamese Latte","cn":"越南冰咖拿铁","cat":"MILK","kind":"milk","price":3.5,"card":"assets/products/card/vietnamese-spanish-latte.png","hero":"assets/products/hero/vietnamese-spanish-latte.png"}];
  var CATS = ['CLASSICS', 'ESPRESSO', 'MILK', 'TEA', 'FRAPPE'];
  function byId(id) {
    for (var i = 0; i < CATALOG.length; i++) if (CATALOG[i].id === id) return CATALOG[i];
    return CATALOG[0];
  }
  /* v13 · 30 slots. Own-category drinks come first, then the rest of the
     catalogue; artwork repeats cyclically so the whole grid is populated even
     though there are only 22 unique drinks. Purely a visual demo. */
  function listFor(cat) {
    var own = CATALOG.filter(function (p) { return p.cat === cat; });
    var rest = CATALOG.filter(function (p) { return p.cat !== cat; });
    var pool = own.concat(rest), out = [], i;
    for (i = 0; i < GRID_COUNT; i++) out.push(pool[i % pool.length]);
    return out;
  }
  /* CATALOG_END */

  /* Per-product-type secondary configuration (二次配置).
     v13 · Plan A: a group carries N options (not a fixed 3); the option track
     is 672 px wide and splits equally by N. Group labels / values stay i18n
     keys; English-only literals are used for the new 4-step sweetness demo. */
  var OPT_SCHEMAS = {
    classic:  [ { grp: 'k.grp.strength', opts: ['k.opt.strength.0', 'k.opt.strength.1', 'k.opt.strength.2'] },
                { grp: 'k.grp.sugar',    opts: ['k.opt.sugar.0', 'k.opt.sugar.1', 'k.opt.sugar.2'] },
                { grp: 'k.grp.temp',     opts: ['k.opt.temp.0', 'k.opt.temp.1', 'k.opt.temp.2'] } ],
    espresso: [ { grp: 'k.grp.shot',     opts: ['k.opt.shot.0', 'k.opt.shot.1', 'k.opt.shot.2'] },
                { grp: 'k.grp.sugar',    opts: ['k.opt.sugar.0', 'k.opt.sugar.1', 'k.opt.sugar.2'] },
                { grp: 'k.grp.temp',     opts: ['k.opt.temp.0', 'k.opt.temp.1', 'k.opt.temp.2'] } ],
    milk:     [ { grp: 'k.grp.milk',     opts: ['k.opt.milk.0', 'k.opt.milk.1', 'k.opt.milk.2'] },
                { grp: 'k.grp.sugar',    opts: ['k.opt.sugar.0', 'k.opt.sugar.1', 'k.opt.sugar.2'] },
                { grp: 'k.grp.temp',     opts: ['k.opt.temp.0', 'k.opt.temp.1', 'k.opt.temp.2'] } ],
    tea:      [ { grp: 'k.grp.sugar',    opts: ['k.opt.sugar.0', 'k.opt.sugar.1', 'k.opt.sugar.2'] },
                { grp: 'k.grp.temp',     opts: ['k.opt.temp.0', 'k.opt.temp.1', 'k.opt.temp.2'] },
                { grp: 'k.grp.topping',  opts: ['k.opt.topping.0', 'k.opt.topping.1', 'k.opt.topping.2'] } ],
    frape:    [ { grp: 'k.grp.cream',    opts: ['k.opt.cream.0', 'k.opt.cream.1', 'k.opt.cream.2'] },
                { grp: 'k.grp.sugar',    opts: ['k.opt.sugar.0', 'k.opt.sugar.1', 'k.opt.sugar.2'] },
                { grp: 'k.grp.temp',     opts: ['k.opt.temp.0', 'k.opt.temp.1', 'k.opt.temp.2'] } ]
  };
  /* v13 · per-product group overrides. Keyed by product id, then group index.
     Only Caramel Macchiato needs the 4-step sweetness for this visual demo. */
  var SUGAR_4 = ['No Sugar', 'Light', 'Less Sugar', 'Regular'];
  var OPT_OVERRIDES = { 'caramel-macchiato': { 1: { grp: 'k.grp.sugar', opts: SUGAR_4 } } };
  function kindOf(id) {
    var p = byId(id);
    return (p && OPT_SCHEMAS[p.kind]) ? p.kind : 'classic';
  }
  /* Effective schema for one product: base by kind, then any group override. */
  function schemaOf(id) {
    var base = OPT_SCHEMAS[kindOf(id)], ov = OPT_OVERRIDES[id];
    if (!ov) return base;
    return base.map(function (g, i) { return ov[i] || g; });
  }
  function ensureCustom() {
    var sch = schemaOf(state.selectedProduct);
    if (!Array.isArray(state.custom) || state.custom.length !== sch.length)
      state.custom = sch.map(function () { return 1; });
    for (var g = 0; g < sch.length; g++) {
      var n = sch[g].opts.length;
      if (!(state.custom[g] >= 0 && state.custom[g] < n)) state.custom[g] = Math.min(1, n - 1);
    }
  }

  /* ---------- pricing (v12 → v13) -------------------------------------
     Price bar shows ONE number: the total for this drink = catalog price +
     surcharges of the selected options (extra shots / milk / toppings / cream).
     Per the v13 brief the bar must NOT itemise the add-ons — no "+ CHF 0.60"
     detail, no "N extras" text — only the resulting total. CURRENCY and
     DEFAULT_PRICE come from products.json via sync_kiosk_catalog.py, so the
     whole kiosk renders one currency: CHF. Edit OPT_ADD to change tariff. */
  var OPT_ADD = {
    'k.grp.shot':    [0, 0, 0.80],   /* 3 Shots */
    'k.grp.milk':    [0, 0, 0.60],   /* Almond */
    'k.grp.topping': [0, 0, 0.50],   /* Jelly */
    'k.grp.cream':   [0, 0, 0.50]    /* Extra cream */
  };
  function money(v) { return CURRENCY + ' ' + (Math.round((+v || 0) * 100) / 100).toFixed(2); }
  function basePriceOf(p) {
    var v = p && typeof p.price === 'number' ? p.price : DEFAULT_PRICE;
    return isFinite(v) ? v : DEFAULT_PRICE;
  }
  function optionAdd(sch, vals) {
    var sum = 0;
    for (var g = 0; g < sch.length; g++) {
      var row = OPT_ADD[sch[g].grp];
      if (!row) continue;
      var i = vals && vals[g] != null ? vals[g] : 1;
      /* N may be 4; any option index beyond the tariff array adds nothing. */
      if (i < 0 || i >= row.length) continue;
      sum += (+row[i] || 0);
    }
    return Math.round(sum * 100) / 100;
  }
  function unitPriceOf(pid, vals) {
    return Math.round((basePriceOf(byId(pid)) + optionAdd(schemaOf(pid), vals)) * 100) / 100;
  }
  function paintPrice() {
    if (SCREEN !== '05-custom') return;
    var bar = document.querySelector('#stage .k-pricebar');
    if (!bar) return;
    ensureCustom();
    var val = bar.querySelector('[data-price-value]');
    /* English-only label is static in the markup (data-nolang); the number is
       language-neutral, so no fr/zh keys are needed for the price bar. */
    if (val) val.textContent = money(unitPriceOf(state.selectedProduct, state.custom));
  }

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
    /* v7 · a kiosk starts empty; the bar appears on the first add. */
    cart: [],
    category: 'CLASSICS', custom: [1, 1, 1],
    payMethod: 'Card', qrIndex: 0, errorType: 'generic', payResult: 'success',
    returnTo: '01-home', brewStage: 0, langOpen: false, selectedProduct: 'americano',
    flashPending: false
  };

  function clone(o) { return JSON.parse(JSON.stringify(o)); }
  function load() {
    var s;
    try { s = JSON.parse(localStorage.getItem(KEY) || '{}'); } catch (e) { s = {}; }
    var out = clone(DEFAULTS);
    Object.keys(s).forEach(function (k) { out[k] = s[k]; });
    if (!out.slides || out.slides.length !== 3) out.slides = clone(SLIDES_DEFAULT);
    if (!LANGBY[out.lang]) out.lang = 'en';
    /* v7 · a persisted cart from an older build must not bypass the limit. */
    if (Array.isArray(out.cart)) {
      var room = CART_MAX, kept = [];
      for (var ci = 0; ci < out.cart.length && room > 0; ci++) {
        var it = out.cart[ci];
        var q = Math.min(Math.max(1, parseInt(it.qty, 10) || 1), room);
        it.qty = q; room -= q; kept.push(it);
      }
      out.cart = kept;
    }
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
    if (!o || !o.pid) return tr('k.opts.custom');
    var sch = schemaOf(o.pid);
    var vals = o.vals || [1, 1, 1];
    var parts = [];
    for (var g = 0; g < sch.length; g++) {
      var n = sch[g].opts.length;
      var v = vals[g]; if (v == null || v < 0 || v >= n) v = Math.min(1, n - 1);
      parts.push(tr(sch[g].opts[v]));
    }
    return parts.join(' · ');
  }
  /* French strings are ~25% longer and Chinese ones shorter than the source
     English. Shrink only the boxes that actually overflow, keeping the 1:1
     layout intact for everything else. */
  function fitText(e) {
    if (!e || !e.style) return;
    if (!e.dataset.i18nFs) e.dataset.i18nFs = parseFloat(window.getComputedStyle(e).fontSize) || 24;
    var base = +e.dataset.i18nFs;
    if (REFINE) base = snapNear(base);
    e.style.fontSize = base + 'px';
    var box = e.clientWidth ? e : (e.parentElement || e);
    var ow = box.scrollWidth, cw = box.clientWidth, oh = box.scrollHeight, ch = box.clientHeight, k = 1;
    if (cw > 8 && ow > cw + 2) k = Math.min(k, cw / ow);
    if (ch > 8 && oh > ch + 3) k = Math.min(k, ch / oh);
    if (k < 0.995) {
      var raw = base * k * 0.97;
      /* v1: arbitrary fractional sizes (12.7px, 14.2px, 25.7px ...).
         v3: every rendered size belongs to the shared token scale. */
      e.style.fontSize = REFINE
        ? snapFloor(Math.max(raw, 14)) + 'px'
        : Math.max(base * 0.56, raw).toFixed(1) + 'px';
    }
  }
  /* One group = one size. Long French labels must not shrink one-by-one:
     pick the largest token that fits every member, then apply it to all. */
  function unifyGroup(sel, token) {
    var els = document.querySelectorAll('#stage ' + sel);
    if (!els.length) return;
    var base = parseFloat(getComputedStyle(document.documentElement)
      .getPropertyValue(token)) || 24;
    var i, box, r = 1;
    for (i = 0; i < els.length; i++) els[i].style.fontSize = base + 'px';
    for (i = 0; i < els.length; i++) {
      box = els[i].clientWidth ? els[i] : (els[i].parentElement || els[i]);
      if (box && box.clientWidth > 8)
        r = Math.min(r, box.clientWidth / Math.max(box.scrollWidth, 1));
    }
    /* r measures the tightest member; keep the base token when all fit,
       otherwise step the WHOLE group down to one shared token. */
    var size = (r >= 0.995) ? base : snapFloor(base * r * 0.97);
    for (i = 0; i < els.length; i++) els[i].style.fontSize = size + 'px';
  }
  /* Final guarantee for refined screens: snap every visible text node,
     including static price / quantity labels that fitText never touches. */
  function snapStageType() {
    if (!REFINE) return;
    unifyGroup('[data-kc="category-tab"]', '--k-fs-body');
    unifyGroup('[data-kc="section-title"]', '--k-fs-title');
    unifyGroup('[data-kc="info-label"]', '--k-fs-row');
    unifyGroup('[data-kc="step-label"]', '--k-fs-row');
    var nodes = document.querySelectorAll('#stage *');
    for (var i = 0; i < nodes.length; i++) {
      var e = nodes[i], own = false;
      for (var j = 0; j < e.childNodes.length; j++) {
        var n = e.childNodes[j];
        if (n.nodeType === 3 && n.nodeValue.replace(/\s/g, '')) { own = true; break; }
      }
      if (!own) continue;
      /* fitText already token-quantised every data-i18n node for this language;
         re-snapping it here could inflate a carefully shrunk French string. */
      if (e.hasAttribute && e.hasAttribute('data-i18n')) continue;
      var fs = parseFloat(window.getComputedStyle(e).fontSize);
      if (!fs) continue;
      /* Static numbers/arrows snap DOWN: inflating 22px -> 24px would clip
         the narrow Figma price boxes (P2: widen the box, never the type). */
      var t = snapFloor(fs);
      if (Math.abs(t - fs) > 0.01) e.style.fontSize = t + 'px';
    }
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
    '<div id="k-toast"></div>' +
    '<div id="k-limit"><div class="k-limit-emblem" data-nolang="1">2</div>' +
    '<div class="k-limit-title"></div><div class="k-limit-body"></div>' +
    '<button class="k-limit-ok" type="button"></button></div>';
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
    var lim = $('k-limit'); if (lim) lim.classList.remove('on');
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
      /* v13 · Plan A chips are real DOM controls in #k-layer; the old 3x3
         fixed-coordinate hotspots are gone so they cannot swallow clicks. */
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
      /* v14 · language / Help are real DOM buttons in the shared header. */
      [0, 0, 1080, 1920, 'finishPay']
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
      [45, 65, 90, 80, 'payBack'],
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

  /* v7 · scrollable hotspots translate with the artboard; the cart bar and
     the sheet hotspots stay locked. */
  var SCROLL_HITS = null;
  if (DRAWER) {
    SCROLL_HITS = document.createElement('div');
    SCROLL_HITS.id = 'k-scroll-hits';
    L.appendChild(SCROLL_HITS);
  }
  HITS.forEach(function (h) {
    /* v13 · the generated 30 cards own their own click handlers; the nine
       fixed-coordinate artboard product hotspots would sit on top of them. */
    if (GRID30 && (h[4] === 'product' || h[4] === 'soldout')) return;
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
    (SCROLL_HITS && !h[6] && h[4] !== 'cart' && h[4] !== 'lang' &&
     h[4] !== 'help' ? SCROLL_HITS : L).appendChild(d);
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
      if (!row) return;
      var has = !!state.cart[i];
      /* opacity (not display:none) keeps the row invisible AND inert while
         the typography regression can still measure its role geometry. */
      row.style.opacity = has ? '' : '0';
      row.style.pointerEvents = has ? '' : 'none';
    });
    var qs = DRAWER_EL.querySelectorAll('[data-cart-qty]');
    for (var i = 0; i < qs.length; i++) {
      if (state.cart[i]) qs[i].textContent = String(state.cart[i].qty);
    }
    var cnt = DRAWER_EL.querySelector('[data-cart-count]');
    if (cnt) { cnt.textContent = tr('k.cart.count', { N: cartQty() }); fitText(cnt); }
    var sub = DRAWER_EL.querySelector('[data-cart-subtotal]');
    var tot = DRAWER_EL.querySelector('[data-cart-total]');
    if (sub) sub.textContent = money(cartSum());
    if (tot) tot.textContent = money(cartSum());
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
    /* v14 · if a stale 06-cart URL is ever reached, closing the sheet
       returns to the canonical 04-shopping screen. */
    if (SCREEN === '06-cart') { go('04-shopping'); return; }
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
    /* v14 · the generated grid always follows the active category list;
       the modal title is handled separately by applyModalProduct(). */
    var slots = listFor(state.category);
    /* v13 · the 30-card grid is emitted in DOM order, but the original Figma
       artboard interleaves its nine cards (0,2,1 / 3,5,4 / 6,8,7). Always
       honour the explicit slot index when one is present. */
    document.querySelectorAll('[data-slot-name]').forEach(function (e, i) {
      var k = e.hasAttribute('data-slot-name') ? +e.getAttribute('data-slot-name') : i;
      var p = slots[k]; if (p) { e.textContent = tr('p.' + p.id); fitText(e); }
    });
    document.querySelectorAll('[data-slot-cn]').forEach(function (e, i) {
      var k = e.hasAttribute('data-slot-cn') ? +e.getAttribute('data-slot-cn') : i;
      var p = slots[k]; if (p) { e.textContent = tr('p.' + p.id + '.tag'); fitText(e); }
    });
    /* v7 · an empty cart leaves the Figma placeholder rows invisible but
       still in the DOM. Localise those placeholders as well, otherwise a
       French / Chinese screen carries stale English product names. */
    function rowName(e, i) {
      var it = state.cart[i];
      var p = it ? byId(it.pid) : CATALOG[i % CATALOG.length];
      if (p) { e.textContent = tr('p.' + p.id); fitText(e); }
    }
    document.querySelectorAll('[data-cart-name]').forEach(rowName);
    document.querySelectorAll('[data-order-name]').forEach(rowName);
    document.querySelectorAll('[data-order-opts]').forEach(function (e, i) {
      var it = state.cart[i]; if (it) { e.textContent = optsText(it.opts); fitText(e); }
    });
    var mn = document.querySelector('[data-modal-name]');
    if (mn) { mn.textContent = tr('p.' + state.selectedProduct); fitText(mn); }
    var cc = document.querySelector('[data-cart-count]');
    if (cc) { cc.textContent = tr('k.cart.count', { N: cartQty() }); fitText(cc); }
    var oc = document.querySelector('[data-order-count]');
    if (oc) { oc.textContent = tr('k.order.count', { N: cartQty() }); fitText(oc); }
    syncCartBar();
  }
  function applyDynamicI18n() {
    applyFlags();
    syncEmptyText();
    if ($('k-limit') && $('k-limit').classList.contains('on')) showLimitDialog();
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
    /* v4: applyDynamicI18n() used to overwrite the option boxes with a
       SECOND dictionary (k.opt.sugar.* = "Less sugar"), while the static
       sheet says "Low". The customise panel is now fully schema-driven and
       localised, so just repaint the adaptive labels for the active product. */
    paintOptionLabels();
    if (GRID30) buildCatTabs();
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
    snapStageType();
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

  /* ---------- customization options · Plan A (v13) --------------------
     Group title sits ABOVE its option row (was: 170 px label on the left).
     The track now uses the full 672 px content width and splits equally by
     N = opts.length, so 3 / 4 / 5 steps all fit without touching the modal.
     Geometry is in 1080x1920 artboard coordinates: modal top 448 + option
     area top 509 = 957; each row is 88 px (title 24 + 4 gap + chip 52 + 8). */
  var OPT_TRACK = { left: 204, width: 672, gap: 12, h: 52, rowTop: 1016, rowH: 88 };
  function optSchema() { return schemaOf(state.selectedProduct); }

  /* v13 · read the real artboard geometry from the three option tracks instead
     of hard-coding it. This guarantees the group title (24px, at the top of
     each 88px row) always sits ABOVE its chips, at any stage scale. */
  function calibrateOptTrack() {
    if (SCREEN !== '05-custom' || !stage) return;
    var s = stage.getBoundingClientRect(), k = (s.width / 1080) || 1;
    var rows = ['.f360', '.f391', '.f408'].map(function (sel) {
      var e = document.querySelector('#stage ' + sel);
      if (!e) return null;
      var r = e.getBoundingClientRect();
      return { left: (r.left - s.left) / k, top: (r.top - s.top) / k,
        width: r.width / k, height: r.height / k };
    });
    if (!rows[0] || !rows[1]) return;
    OPT_TRACK.left = rows[0].left;
    OPT_TRACK.width = rows[0].width;
    OPT_TRACK.h = rows[0].height;
    OPT_TRACK.rowTop = rows[0].top;
    OPT_TRACK.rowH = rows[1].top - rows[0].top;
  }

  function ensureOptionBoxes() {
    calibrateOptTrack();
    var sch = optSchema();
    var sig = state.selectedProduct + '|' + sch.map(function (g) { return g.opts.length; }).join(',');
    if (L.dataset.optSig === sig) return;     // rebuild only when the schema changes
    var old = L.querySelectorAll('.k-opt');
    for (var k = 0; k < old.length; k++) old[k].remove();
    L.dataset.optSig = sig;
    for (var g = 0; g < sch.length; g++) {
      var n = sch[g].opts.length;
      var w = (OPT_TRACK.width - (n - 1) * OPT_TRACK.gap) / n;
      for (var i = 0; i < n; i++) {
        var d = document.createElement('div');
        d.className = 'k-opt';
        d.dataset.g = g; d.dataset.i = i;
        d.style.left = (OPT_TRACK.left + i * (w + OPT_TRACK.gap)) + 'px';
        d.style.top = (OPT_TRACK.rowTop + g * OPT_TRACK.rowH) + 'px';
        d.style.width = w + 'px'; d.style.height = OPT_TRACK.h + 'px';
        d.appendChild(document.createElement('span'));
        d.onclick = (function (g, i) {
          return function (e) {
            e.stopPropagation(); bump(); ensureCustom();
            state.custom[g] = i; save();
            L.classList.add('opts-active'); paintOptionSelection(); paintPrice();
            toast(tr('k.toast.opt', { K: tr(optSchema()[g].grp), V: i + 1 }));
          };
        })(g, i);
        L.appendChild(d);
      }
    }
  }
  /* English text must never be clipped: step the type down 24 → 20 → 18 px,
     then allow two lines inside the 52 px chip. No ellipsis, ever. */
  function fitOptionLabel(sp) {
    var sizes = [24, 20, 18], box = sp.parentElement;
    for (var s = 0; s < sizes.length; s++) {
      sp.style.fontSize = sizes[s] + 'px';
      if (sp.scrollWidth <= box.clientWidth - 10 && sp.scrollHeight <= box.clientHeight - 2) return;
    }
    sp.style.fontSize = '18px';
  }
  function paintOptionLabels() {
    if (SCREEN !== '05-custom') return;
    ensureCustom();
    ensureOptionBoxes();
    var sch = optSchema();
    var boxes = L.querySelectorAll('.k-opt');
    for (var n = 0; n < boxes.length; n++) {
      var g = +boxes[n].dataset.g, i = +boxes[n].dataset.i;
      var sp = boxes[n].querySelector('span');
      if (sp) { sp.textContent = tr(sch[g].opts[i]); fitOptionLabel(sp); }
    }
    /* Group titles are the static artboard labels, repainted per schema. */
    var labels = document.querySelectorAll('#stage [data-kc="option-row-label"]');
    for (var k = 0; k < labels.length && k < sch.length; k++) {
      labels[k].textContent = tr(sch[k].grp); fitText(labels[k]);
    }
    paintOptionSelection();
  }
  function paintOptionSelection() {
    ensureCustom();
    var boxes = L.querySelectorAll('.k-opt');
    for (var n = 0; n < boxes.length; n++) {
      var g = +boxes[n].dataset.g, i = +boxes[n].dataset.i;
      boxes[n].classList.toggle('sel', state.custom[g] === i);
    }
  }
  if (SCREEN === '05-custom') {
    var stg = document.getElementById('stage');
    if (stg) stg.classList.add('k-planA');   // retire the fixed 3-chip artwork
    ensureOptionBoxes();
    L.classList.add('opts-active');   // show the adaptive panel immediately
    paintOptionLabels();
    paintPrice();
  }

  /* ---------- v16 · adaptive, category-sectioned shopping grid ------------
     All categories live on ONE vertically scrolling page. Each section draws
     only its real products (no filler, no empty placeholders) and splits them
     into rows of at most 3, never leaving a single lonely card in a row:
       1 → [1] left   2 → [2] centre   3 → [3]
       4 → [2,2]      5 → [3,2]        6 → [3,3]
       7 → [3,2,2]    8 → [3,3,2]      9 → [3,3,3]
       >9 → full rows of 3, the incomplete tail centred.
     v17 · the card keeps its proportions but is uniformly scaled to 241 x 277
     (1:1 image at 241, radius 22, 19px gutters) so three rows fit above the
     fixed cart strip at y=1659. The artboard top (0..495) is untouched. */
  var G30_TOP = 8, G30_TAIL = 480;
  var G30_BADGES = ['Most Popular', 'Top Pick', 'Featured'];
  var G30_SEC_TOP = {};      // category -> y offset inside #k-grid30
  var G30_LAST_TOP = 0;      // last section's offset, drives the scroll ceiling
  var GRID_LIST = [];        // flat product list in display order

  /* Row sizes for n products. A remainder of 1 is folded into 2 + 2 so no
     row is left holding a single card; add/remove/disable a product and the
     layout recalculates on the next build. */
  function rowsFor(n) {
    if (n <= 0) return [];
    if (n <= 3) return [n];
    var out = [], full = Math.floor(n / 3), rem = n % 3, i;
    if (rem === 0) { for (i = 0; i < full; i++) out.push(3); }
    else if (rem === 2) { for (i = 0; i < full; i++) out.push(3); out.push(2); }
    else { for (i = 0; i < full - 1; i++) out.push(3); out.push(2); out.push(2); }
    return out;
  }
  function buildGrid30() {
    if (!GRID30) return;
    var host = document.querySelector('#stage .f68');
    if (!host || host.querySelector('#k-grid30')) return;
    host.classList.add('k-grid30-on');
    var wrap = document.createElement('div');
    wrap.id = 'k-grid30';

    /* Preserve the original catalogue order inside each category. */
    var sections = CATS.map(function (cat) {
      var list = CATALOG.filter(function (p) { return p.cat === cat; });
      return { cat: cat, list: list, rows: rowsFor(list.length) };
    });
    GRID_LIST = [];
    sections.forEach(function (s) { GRID_LIST = GRID_LIST.concat(s.list); });

    var html = '<div class="k-g30-head k-g30-all"><span>ALL DRINKS</span><span>' +
      GRID_LIST.length + ' ITEMS</span></div>';
    var slot = 0;
    sections.forEach(function (s) {
      html += '<div class="k-g30-sec" data-sec="' + s.cat + '">' +
        '<div class="k-g30-head"><span>' + s.cat + '</span><span>' +
        s.list.length + ' ITEMS</span></div>';
      var from = 0;
      s.rows.forEach(function (count) {
        /* Single-product categories stay left aligned (layout table, row 1);
           every other incomplete row is centred inside the 952px grid. */
        var cls = (count < 3 && s.list.length > 1) ? ' k-g30-row-center' : '';
        html += '<div class="k-g30-row' + cls + '">';
        for (var k = 0; k < count; k++) {
          var p = s.list[from + k];
          html += '<div class="k-g30-card" data-g30="' + slot + '">' +
            '<div class="k-g30-img" style="background-image:url(' + ROOT + p.card + ')"></div>' +
            '<div class="k-g30-badge">' + G30_BADGES[slot % 3] + '</div>' +
            '<div class="k-g30-info">' +
              '<div class="k-g30-name">' + p.name + '</div>' +
              '<div class="k-g30-price" data-nolang="1">' + money(DEFAULT_PRICE) + '</div>' +
            '</div></div>';
          slot++;
        }
        html += '</div>';
        from += count;
      });
      html += '</div>';
    });
    html += '<div class="k-g30-foot">END OF MENU</div>';
    wrap.innerHTML = html;
    host.appendChild(wrap);
    /* Tail whitespace lets even the last category scroll up to the top.
       Use measured offsets so the CSS section gap can never drift from the
       scroll anchors. */
    var secs = wrap.querySelectorAll('.k-g30-sec');
    for (var j = 0; j < secs.length; j++) {
      G30_SEC_TOP[secs[j].getAttribute('data-sec')] = secs[j].offsetTop;
    }
    G30_LAST_TOP = secs.length ? secs[secs.length - 1].offsetTop : 0;
    GRID30_H = G30_TOP + wrap.offsetHeight + G30_TAIL;
    host.style.height = GRID30_H + 'px';
    wrap.querySelectorAll('.k-g30-name').forEach(function (e) { fitText(e); });
    var cards = wrap.querySelectorAll('.k-g30-card');
    for (var i = 0; i < cards.length; i++) {
      (function (n) {
        cards[n].onclick = function (e) {
          e.stopPropagation(); bump();
          var p = GRID_LIST[n];
          if (p) act('product', p.id, e);
        };
      })(i);
    }
  }

  /* ---------- product catalog rendering ---------- */
  function applySlots(list) {
    /* v14 · the 30-card grid also exists on 05-custom as the modal's
       background, so it needs its artwork even though that page has no drawer. */
    if (!DRAWER && !GRID30) return;
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
    /* v9 · re-render the adaptive 3x3 option grid for the active product's
       kind so strength/shots/milk/topping/cream schemas show correctly. */
    paintOptionLabels();
    /* v12 · unit price follows the selected product + option surcharges. */
    paintPrice();
  }

  /* ---------- data-driven category tab bar (v9) ----------
     The five tab pills live in the fixed f55 container; their labels and the
     active underline are driven entirely by CATS + state.category, so renaming
     or reordering a category only touches data, never the artboard markup. */
  function paintSectionTitle() {
    var t = document.querySelector('#stage [data-kc="section-title"]');
    if (t) { t.textContent = catLabel(state.category); fitText(t); }
  }
  /* Toggle left/right chevrons based on whether the bar actually overflows and
     where the scroll position sits. Keeps "show more" discoverable without a
     visible scrollbar on the touch kiosk. */
  function updateCatScroll() {
    var bar = document.getElementById('k-catbar');
    var prev = document.getElementById('k-catprev');
    var next = document.getElementById('k-catnext');
    if (!bar || !prev || !next) return;
    var max = bar.scrollWidth - bar.clientWidth;
    var over = max > 4;
    prev.style.display = (over && bar.scrollLeft > 4) ? 'flex' : 'none';
    next.style.display = (over && bar.scrollLeft < max - 4) ? 'flex' : 'none';
  }
  /* v9 · data-driven category bar. Every pill is generated from CATS, so adding
     a 6th+ category needs no artboard changes — the row simply scrolls. Pills
     carry real click listeners (the old coordinate hit-zones were removed),
     which is required because a scrollable bar cannot use fixed coordinates. */
  function buildCatTabs() {
    if (!GRID30) return;
    var bar = document.getElementById('k-catbar');
    var row = document.getElementById('k-catrow');
    if (!bar || !row) return;
    row.innerHTML = '';
    CATS.forEach(function (cat) {
      var pill = document.createElement('div');
      pill.className = 'k-catpill' + (cat === state.category ? ' on' : '');
      pill.setAttribute('data-cat', cat);
      var lbl = document.createElement('span');
      lbl.className = 'lbl';
      lbl.textContent = tr('k.cat.' + cat);
      var und = document.createElement('span');
      und.className = 'bar';
      pill.appendChild(lbl); pill.appendChild(und);
      pill.addEventListener('click', function (e) {
        e.stopPropagation(); bump(); act('cat', cat, e);
      });
      row.appendChild(pill);
    });
    /* wire scroll affordances once */
    if (!bar.dataset.bound) {
      bar.dataset.bound = '1';
      bar.addEventListener('scroll', updateCatScroll, { passive: true });
      window.addEventListener('resize', updateCatScroll);
      var prev = document.getElementById('k-catprev');
      var next = document.getElementById('k-catnext');
      if (prev) prev.addEventListener('click', function (e) {
        e.stopPropagation(); bar.scrollBy({ left: -bar.clientWidth * 0.7, behavior: 'smooth' });
      });
      if (next) next.addEventListener('click', function (e) {
        e.stopPropagation(); bar.scrollBy({ left: bar.clientWidth * 0.7, behavior: 'smooth' });
      });
    }
    /* bring the active pill into view when it sits outside the visible window */
    var on = row.querySelector('.k-catpill.on');
    if (on) on.scrollIntoView({ inline: 'nearest', block: 'nearest' });
    updateCatScroll();
    paintSectionTitle();
  }
  function applyRows() { renderProductNames(); }

  /* ---------- cart ---------- */
  function cartQty() { return state.cart.reduce(function (a, i) { return a + i.qty; }, 0); }
  function cartSum() {
    return state.cart.reduce(function (a, i) {
      return a + i.qty * (typeof i.price === 'number' ? i.price : DEFAULT_PRICE);
    }, 0);
  }
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
    if (sub) sub.textContent = money(cartSum());
    if (tot) tot.textContent = money(cartSum());
    syncCartBar();
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
  buildGrid30();
  if (GRID30 || (DRAWER && state.category !== 'CLASSICS')) applySlots(listFor(state.category));
  if (SCREEN === '05-custom') applyModalProduct();
  if (DRAWER || SCREEN === '19-order-confirm') applyRows();


  /* ===================== v7 · shopping UX ===================== */
  /* v13 · the 30-card grid is much taller than the original 3-row artboard,
     so the scroll ceiling follows the generated content height. */
  /* v16 · the ceiling must reach the last category section so clicking any
     tab can actually pin that section to the top of the product area. */
  var CART_LOCK_Y = 360,
      MENU_MAX_Y = GRID30 ? Math.max(360, CART_LOCK_Y + G30_LAST_TOP + 80) : 360,
      MENU_Y = 0;
  var scrollRoot = document.getElementById('k-scroll-root');
  var scrollHits = document.getElementById('k-scroll-hits');
  var bannerEl = document.querySelector('#stage .f49');
  /* Only these three branches scroll. The status bar / logo / language /
     help header and the cart bar are deliberately NOT in this list. */
  var scrollNodes = [];
  ['.f49', '.f53', '.f68'].forEach(function (sel) {
    var e = document.querySelector('#stage ' + sel);
    if (e) scrollNodes.push(e);
  });

  function kScale() {
    var r = stage.getBoundingClientRect();
    return (r.width / 1080) || 1;
  }
  function applyFlags() {
    var lg = LANGBY[state.lang];
    if (!lg) return;
    var els = document.querySelectorAll('[data-kc="flag"]');
    for (var i = 0; i < els.length; i++) els[i].textContent = lg.flag;
  }
  function syncEmptyText() {
    var e = $('k-cart-empty'); if (!e) return;
    var a = e.querySelector('.k-empty-title'),
        b = e.querySelector('.k-empty-body'),
        c = e.querySelector('.k-empty-cta');
    if (a) a.textContent = tr('k.cart.empty.title');
    if (b) b.textContent = tr('k.cart.empty.body');
    if (c) c.textContent = tr('k.cart.empty.cta');
  }
  function ensureEmptyState() {
    if (!DRAWER_EL) return;
    var host = DRAWER_EL.querySelector('.cdf16');
    if (!host) return;
    var e = $('k-cart-empty');
    if (!e) {
      e = document.createElement('div');
      e.id = 'k-cart-empty';
      e.innerHTML = '<div class="k-empty-icon" data-nolang="1"></div>' +
        '<div class="k-empty-title"></div><div class="k-empty-body"></div>' +
        '<div class="k-empty-cta"></div>';
      host.appendChild(e);
      e.querySelector('.k-empty-cta').onclick = function (ev) {
        ev.stopPropagation(); closeDrawer(); setMenuScroll(0, true);
      };
    }
    syncEmptyText();
  }
  function syncCartBar() {
    var n = cartQty(), empty = (n === 0);
    document.body.classList.toggle('k-cart-empty', empty);
    if (L) L.classList.toggle('k-cart-empty', empty);
    var badges = document.querySelectorAll('[data-cart-badge]');
    for (var i = 0; i < badges.length; i++) badges[i].textContent = String(n);
    var totals = document.querySelectorAll('[data-cart-total-bar]');
    for (var j = 0; j < totals.length; j++)
      totals[j].textContent = money(cartSum());
    ensureEmptyState();
  }
  function flashCart() {
    var a = document.querySelectorAll('[data-cart-badge]'),
        b = document.querySelectorAll('[data-cart-bar]'), i;
    for (i = 0; i < a.length; i++) {
      a[i].classList.remove('k-cart-flash'); void a[i].offsetWidth;
      a[i].classList.add('k-cart-flash');
    }
    for (i = 0; i < b.length; i++) {
      b[i].classList.remove('k-bar-flash'); void b[i].offsetWidth;
      b[i].classList.add('k-bar-flash');
    }
    setTimeout(function () {
      for (i = 0; i < a.length; i++) a[i].classList.remove('k-cart-flash');
      for (i = 0; i < b.length; i++) b[i].classList.remove('k-bar-flash');
    }, 1700);
  }
  function showLimitDialog() {
    var d = $('k-limit'); if (!d) return;
    d.querySelector('.k-limit-title').textContent = tr('k.cart.limit.title');
    d.querySelector('.k-limit-body').textContent = tr('k.cart.limit.body');
    d.querySelector('.k-limit-ok').textContent = tr('k.cart.limit.ok');
    d.classList.add('on'); scrim(true); bump();
  }
  function closeLimitDialog() {
    var d = $('k-limit'); if (d) d.classList.remove('on');
    scrim(false);
  }
  function setMenuScroll(y, smooth) {
    if (!DRAWER || !scrollNodes.length) return;
    MENU_Y = Math.max(0, Math.min(MENU_MAX_Y, y));
    /* v13 · the banner / tabs / product column keep their original 360px
       "snap" ceiling (so the category bar never leaves the artboard); the
       extra distance scrolls the generated grid INSIDE the product column. */
    var headY = Math.min(MENU_Y, CART_LOCK_Y);
    var t = 'translate3d(0,' + (-headY) + 'px,0)', i, e;
    for (i = 0; i < scrollNodes.length; i++) {
      e = scrollNodes[i];
      if (smooth) e.classList.add('k-smooth');
      else e.classList.remove('k-smooth');
      e.style.transform = t;
    }
    var grid = document.getElementById('k-grid30');
    if (grid) {
      var extra = Math.max(0, MENU_Y - CART_LOCK_Y);
      if (smooth) grid.classList.add('k-g30-smooth');
      else grid.classList.remove('k-g30-smooth');
      grid.style.transform = 'translate3d(0,' + (-extra) + 'px,0)';
    }
    if (scrollHits) {
      if (smooth) scrollHits.classList.add('k-smooth');
      else scrollHits.classList.remove('k-smooth');
      scrollHits.style.transform = t;
    }
    if (bannerEl) bannerEl.style.opacity = String(Math.max(0, 1 - MENU_Y / 240));
    document.body.setAttribute('data-menu-scroll', String(Math.round(MENU_Y)));
    if (smooth) setTimeout(function () {
      for (var j = 0; j < scrollNodes.length; j++)
        scrollNodes[j].classList.remove('k-smooth');
      if (scrollHits) scrollHits.classList.remove('k-smooth');
      if (grid) grid.classList.remove('k-g30-smooth');
    }, 420);
  }
  function scrollMenu(y, smooth) {
    setMenuScroll(y, smooth);
    document.body.setAttribute('data-menu-lock',
      y >= CART_LOCK_Y ? '1' : '0');
  }
  function setupButtonHover() {
    if (!DRAWER) return;
    ['lang', 'help'].forEach(function (a) {
      var hit = L.querySelector('.k-hit[data-act="' + a + '"]');
      var btn = document.querySelector('[data-btn="' + a + '"]');
      if (!hit || !btn) return;
      hit.addEventListener('pointerenter', function () { btn.classList.add('k-btn-hot'); });
      hit.addEventListener('pointerleave', function () { btn.classList.remove('k-btn-hot'); });
      hit.addEventListener('pointerdown', function () { btn.classList.add('k-btn-hot'); });
      hit.addEventListener('pointerup', function () {
        setTimeout(function () { btn.classList.remove('k-btn-hot'); }, 180);
      });
    });
    var ok = document.querySelector('#k-limit .k-limit-ok');
    if (ok) ok.onclick = function (e) { e.stopPropagation(); closeLimitDialog(); };
  }
  function setupScrollGestures() {
    if (!DRAWER) return;
    var startY = 0, startX = 0, startScroll = 0, moved = false,
        lastY = 0, lastT = 0, vel = 0, suppress = false, active = false,
        catSwipe = false;
    document.addEventListener('touchstart', function (e) {
      if (drawerOpen || e.touches.length !== 1) { active = false; return; }
      active = true; moved = false;
      startY = e.touches[0].clientY; startX = e.touches[0].clientX;
      startScroll = MENU_Y; lastY = startY; lastT = Date.now(); vel = 0;
      /* a touch that begins on the category bar must scroll it natively,
         not cycle categories or scroll the product list. */
      catSwipe = !!(e.target && e.target.closest && e.target.closest('#k-catbar'));
    }, { passive: true });
    document.addEventListener('touchmove', function (e) {
      if (!active || drawerOpen || !e.touches.length) return;
      var t = e.touches[0], dy = t.clientY - startY,
          dx = t.clientX - startX, k = kScale();
      /* A purely horizontal swipe on the tab row is a gesture too; without
         this, touchend returns early and the category never changes. */
      if (Math.abs(dy) > 10 * k || Math.abs(dx) > 10 * k) moved = true;
      if (!moved) return;
      if (catSwipe && Math.abs(dx) > Math.abs(dy)) return; /* let the bar scroll horizontally */
      var now = Date.now();
      vel = (t.clientY - lastY) / Math.max(1, now - lastT) / k;
      lastY = t.clientY; lastT = now;
      /* Only a vertical-dominant drag moves the artboard. */
      setMenuScroll(Math.abs(dy) >= Math.abs(dx)
                    ? startScroll - dy / k : startScroll, false);
      if (e.cancelable) e.preventDefault();
    }, { passive: false });
    document.addEventListener('touchend', function (e) {
      if (!active) return; active = false;
      if (!moved || !e.changedTouches.length) return;
      if (catSwipe) { catSwipe = false; return; } /* bar handled its own scroll */
      var t = e.changedTouches[0], dx = t.clientX - startX,
          dy = t.clientY - startY, k = kScale();
      var by = (startY - stage.getBoundingClientRect().top) / k;
      var tabY = 494.96 - MENU_Y + 38;
      if (Math.abs(dx) > 60 * k && Math.abs(dy) < 60 * k &&
          Math.abs(by - tabY) < 90) {
        var i = CATS.indexOf(state.category);
        act('cat', CATS[(i + (dx < 0 ? 1 : CATS.length - 1)) % CATS.length]);
      } else if (!drawerOpen) {
        setMenuScroll(MENU_Y - vel * 190, true);
      }
      suppress = true;
      setTimeout(function () { suppress = false; }, 420);
    }, { passive: true });
    document.addEventListener('click', function (e) {
      if (!suppress) return;
      e.stopPropagation(); e.preventDefault(); suppress = false;
    }, true);
    document.addEventListener('wheel', function (e) {
      if (drawerOpen) return;
      if (e.cancelable) e.preventDefault();
      setMenuScroll(MENU_Y + e.deltaY / kScale(), false);
    }, { passive: false });
  }
  function setupDrawerGestures() {
    if (!DRAWER || !DRAWER_EL) return;
    var panel = DRAWER_EL.querySelector('.k-cart-panel');
    var drag = null, barStart = null;
    document.addEventListener('touchstart', function (e) {
      if (e.touches.length !== 1) return;
      var t = e.touches[0], el = e.target;
      if (drawerOpen) {
        if (el.closest && el.closest('.k-cart-drag')) {
          drag = { y: t.clientY, dy: 0 };
          panel.style.transition = 'none';
        }
      } else if (boardY(t.clientY) > 1590) {
        barStart = t.clientY;
      }
    }, { passive: true });
    document.addEventListener('touchmove', function (e) {
      if (!drag || !e.touches.length) return;
      drag.dy = e.touches[0].clientY - drag.y;
      panel.style.transform = 'translateY(' + Math.max(0, drag.dy) + 'px)';
      if (e.cancelable) e.preventDefault();
    }, { passive: false });
    document.addEventListener('touchend', function (e) {
      if (drag) {
        var dy = drag.dy; drag = null;
        panel.style.transition = ''; panel.style.transform = '';
        if (dy > 120) closeDrawer();
      } else if (barStart !== null && !drawerOpen && e.changedTouches.length) {
        var up = barStart - e.changedTouches[0].clientY;
        if (up > 60 * kScale()) openDrawer();
      }
      barStart = null;
    }, { passive: true });
  }
  function kioskAdd(pid) {
    if (cartQty() >= CART_MAX) { showLimitDialog(); return false; }
    var p = byId(pid || 'americano');
    ensureCustom();
    state.cart.push({ pid: p.id, name: p.short, price: unitPriceOf(p.id, state.custom), qty: 1,
      opts: { pid: p.id, vals: state.custom.slice() } });
    state.flashPending = true; save(); refreshCart(); syncCartBar(); flashCart();
    return true;
  }
  /* ---------- v14 · shared header + back-to-home confirmation ----------
     The payment screens used to carry three different ad-hoc header layouts.
     One DOM header (centred brand, language / Help pills, back arrow) now
     serves 11-card / 16-pay / 19-order-confirm; 19 keeps its Figma arrow. */
  var HEADER_PAGES = ['11-card', '16-pay', '19-order-confirm'];
  var CONFIRM_TITLE = '您确定返回主页吗？';
  var CONFIRM_BODY = '您的购物车和个性化配置将自动清空。';

  function buildSharedHeader() {
    if (!L || HEADER_PAGES.indexOf(SCREEN) < 0) return;
    var h = document.createElement('div');
    h.className = 'k-v14-head';
    h.innerHTML =
      (SCREEN === '19-order-confirm' ? '' :
        '<button class="k-v14-back" type="button" aria-label="Back">←</button>') +
      '<div class="k-v14-logo" data-kc="brand"></div>' +
      '<button class="k-v14-pill" type="button" data-hact="lang">English</button>' +
      '<button class="k-v14-pill" type="button" data-hact="help">Help</button>';
    L.appendChild(h);
    h.addEventListener('click', function (e) {
      var b = e.target.closest ? e.target.closest('[data-hact],.k-v14-back') : null;
      if (!b) return;
      e.stopPropagation();
      if (b.classList.contains('k-v14-back')) openHomeConfirm();
      else act(b.getAttribute('data-hact'));
    });
  }

  function openHomeConfirm() {
    var d = $('k-home-confirm');
    if (!d) {
      d = document.createElement('div');
      d.id = 'k-home-confirm';
      d.className = 'k-v14-confirm';
      d.innerHTML = '<div class="k-v14-confirm-card">' +
        '<div class="k-v14-confirm-title">' + CONFIRM_TITLE + '</div>' +
        '<div class="k-v14-confirm-body">' + CONFIRM_BODY + '</div>' +
        '<div class="k-v14-confirm-actions">' +
        '<button type="button" data-cact="cancel">取消</button>' +
        '<button type="button" data-cact="confirm">确定返回</button>' +
        '</div></div>';
      (L || document.body).appendChild(d);
      d.addEventListener('click', function (e) {
        var b = e.target.closest ? e.target.closest('[data-cact]') : null;
        if (!b) return;
        e.stopPropagation();
        if (b.getAttribute('data-cact') === 'confirm') { closeHomeConfirm(); goHome(); }
        else closeHomeConfirm();
      });
    }
    d.classList.add('on');
  }
  function closeHomeConfirm() {
    var d = $('k-home-confirm'); if (d) d.classList.remove('on');
  }

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
        state.category = o; save();
        /* v16 · every category is a section on the same scrolling page, so
           switching a tab scrolls that section to the top of the product area
           instead of filtering the list down to 30 cycled cards. */
        buildCatTabs();
        scrollMenu(CART_LOCK_Y + (G30_SEC_TOP[o] || 0), true);
        toast(tr('k.toast.cat', { C: catLabel(o), N: CATALOG.filter(function (p) { return p.cat === o; }).length }));
        break;
      case 'soldout': toast(tr('k.toast.soldout')); break;
      case 'product': state.selectedProduct = o; state.custom = [1, 1, 1]; save(); go('05-custom'); break;
      case 'opt': break; /* handled by k-opt elements */
      case 'customClose': go('04-shopping'); break;
      case 'addCart': {
        if (cartQty() >= CART_MAX) { showLimitDialog(); break; }
        var pa = byId(state.selectedProduct);
        ensureCustom();
        state.cart.push({ pid: pa.id, name: pa.short, price: unitPriceOf(pa.id, state.custom), qty: 1,
          opts: { pid: pa.id, vals: state.custom.slice() } });
        state.flashPending = true;
        save(); toast(tr('k.toast.added', { P: pname(pa) }));
        setTimeout(function () { go('04-shopping'); }, 700);
        break;
      }
      case 'confirmCart': {
        if (cartQty() >= CART_MAX) { showLimitDialog(); break; }
        var pb = byId(state.selectedProduct);
        ensureCustom();
        state.cart.push({ pid: pb.id, name: pb.short, price: unitPriceOf(pb.id, state.custom), qty: 1,
          opts: { pid: pb.id, vals: state.custom.slice() } });
        state.flashPending = true;
        save(); go('04-shopping', 'cart=1');
        break;
      }
      case 'cart': if (DRAWER) openDrawer(); else go('04-shopping', 'cart=1'); break;
      case 'cartClose': closeDrawer(); break;
      case 'payBack': openHomeConfirm(); break;
      case 'qty': {
        var p = String(o).split(':'), idx = +p[0], d = +p[1];
        var item = state.cart[idx]; if (!item) break;
        var label = tr('p.' + byId(item.pid).id);
        if (d > 0) {
          if (cartQty() >= CART_MAX) { showLimitDialog(); break; }
          item.qty++; save(); refreshCart(); flashCart();
          toast(label + ' × ' + item.qty);
        }
        else if (item.qty > 1) {
          item.qty--; save(); refreshCart(); syncCartBar();
          toast(label + ' × ' + item.qty);
        }
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
      case 'pay': if (!cartQty()) { syncCartBar(); break; } go('19-order-confirm'); break;
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
          N: cartQty(), T: money(cartSum()),
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
  /* v9 · (re)build the category tab bar once the base i18n pass has run. */
  if (SCREEN === '04-shopping') buildCatTabs();
  /* font-display:block means fitText can measure fallback metrics on a cold
     load. Re-apply once the real fonts are ready so sizes are deterministic. */
  if (REFINE && document.fonts && document.fonts.ready) {
    document.fonts.ready.then(function () { applyI18n(); if (GRID30) buildCatTabs(); });
  }
  /* ---- v7 boot: flags, cart bar, scroll + Pad gestures, button feedback ---- */
  applyFlags();
  syncCartBar();
  setMenuScroll(0, false);
  setupScrollGestures();
  setupDrawerGestures();
  setupButtonHover();
  buildSharedHeader();
  if (state.flashPending) {
    state.flashPending = false; save(); flashCart();
  }
  /* v14 · 06-cart.html is a redirect stub. The cart is always the sheet on
     04-shopping; ?cart=1 (or the legacy data-drawer-open attr) opens it. */
  if (AUTO_DRAWER || (SCREEN === '04-shopping' && QS.has('cart'))) {
    requestAnimationFrame(function () { requestAnimationFrame(openDrawer); });
    setTimeout(openDrawer, 120);
  }
  if (SCREEN === '19-order-confirm' && state.qrIndex > 0) showQRBadge();

  /* Test hooks: purity audit + programmatic language switching. */
  window.__kioskScroll = function () { return MENU_Y; };
  window.__kioskSetScroll = function (y) { setMenuScroll(y, false); return MENU_Y; };
  window.__kioskCartQty = cartQty;
  window.__kioskAdd = kioskAdd;
  window.__kioskClearCart = function () {
    state.cart = []; save(); refreshCart(); syncCartBar(); return 0;
  };
  window.__kioskLimitOpen = function () {
    return !!$('k-limit') && $('k-limit').classList.contains('on');
  };
  window.__kioskCloseLimit = closeLimitDialog;
  window.__kioskDrawerOpen = function () { return drawerOpen; };
  window.__kioskAudit = function () { return I18N.audit(state.lang); };
  window.__kioskSetLang = setLang;
  window.__kioskLang = function () { return state.lang; };
})();
