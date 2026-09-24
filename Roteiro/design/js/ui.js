/* =========================================================
   Roteiro — componentes reutilizáveis
   Cada função devolve uma string HTML. As telas (screens.js)
   são montadas apenas combinando estes componentes.
   ========================================================= */
(function () {
  const I = window.icon;
  const esc = (s) => String(s);

  /* ---------- Marca ---------- */
  function logo(size, ring, dot) {
    ring = ring || 'var(--a-accent)'; dot = dot || ring;
    return '<svg width="' + size + '" height="' + size + '" viewBox="0 0 48 48" aria-hidden="true">' +
      '<circle cx="24" cy="24" r="17" fill="none" stroke="' + ring + '" stroke-width="5"/>' +
      '<circle cx="27.5" cy="20.5" r="6" fill="' + dot + '"/></svg>';
  }

  /* Anel de contexto: "você está dentro" / "fora" / "sem sinal" */
  function ring(state) {
    if (state === 'in') {
      return '<svg class="ring" viewBox="0 0 64 64" aria-hidden="true">' +
        '<circle cx="32" cy="32" r="29" fill="var(--a-accent-soft)"/>' +
        '<circle cx="32" cy="32" r="29" fill="none" stroke="var(--a-accent)" stroke-width="1.5" stroke-dasharray="3 4"/>' +
        '<circle cx="36" cy="28" r="7.5" fill="var(--a-accent)" stroke="var(--a-bg)" stroke-width="3"/></svg>';
    }
    if (state === 'warn') {
      return '<svg class="ring" viewBox="0 0 64 64" aria-hidden="true">' +
        '<circle cx="32" cy="32" r="29" fill="var(--a-late-soft)"/>' +
        '<circle cx="32" cy="32" r="29" fill="none" stroke="var(--a-late)" stroke-width="1.5" stroke-dasharray="3 4"/>' +
        '<path d="M32 22v12M32 40v.5" stroke="var(--a-late)" stroke-width="3" stroke-linecap="round"/></svg>';
    }
    return '<svg class="ring" viewBox="0 0 64 64" aria-hidden="true">' +
      '<circle cx="30" cy="34" r="24" fill="none" stroke="var(--a-ink-3)" stroke-width="1.5" stroke-dasharray="3 4"/>' +
      '<circle cx="53" cy="12" r="6.5" fill="var(--a-ink-2)" stroke="var(--a-bg)" stroke-width="3"/></svg>';
  }

  /* ---------- Estrutura do celular ---------- */
  const sbIcons = '<span class="sb-r">' +
    '<svg viewBox="0 0 17 12"><rect x="0" y="8" width="3" height="4" rx="1" fill="currentColor"/><rect x="4.5" y="5.5" width="3" height="6.5" rx="1" fill="currentColor"/><rect x="9" y="3" width="3" height="9" rx="1" fill="currentColor"/><rect x="13.5" y="0" width="3" height="12" rx="1" fill="currentColor"/></svg>' +
    '<svg viewBox="0 0 16 12"><path d="M8 11.5 1 4.2a10 10 0 0 1 14 0z" fill="currentColor"/></svg>' +
    '<svg viewBox="0 0 26 12" style="width:26px"><rect x=".5" y=".5" width="22" height="11" rx="3.5" fill="none" stroke="currentColor" opacity=".45"/><rect x="2" y="2" width="16" height="8" rx="2" fill="currentColor"/><rect x="23.5" y="4" width="2" height="4" rx="1" fill="currentColor" opacity=".45"/></svg>' +
    '</span>';

  function statusBar(time, inv) {
    return '<div class="sb' + (inv ? ' inv' : '') + '"><span class="mono" style="font-family:var(--font-ui);letter-spacing:0">' + (time || '9:41') + '</span>' + sbIcons + '</div>';
  }

  const TABS = [
    ['agora', 'now', 'Agora'],
    ['lugares', 'places', 'Lugares'],
    ['add'],
    ['lista', 'list', 'Lista'],
    ['rotinas', 'routine', 'Rotinas']
  ];
  function tabBar(active) {
    return '<nav class="tab">' + TABS.map(function (t) {
      if (t[0] === 'add') return '<div class="tab-add"><span>' + I('plus') + '</span></div>';
      return '<div class="tab-i' + (t[0] === active ? ' on' : '') + '">' + I(t[1]) + '<span>' + t[2] + '</span></div>';
    }).join('') + '</nav>';
  }

  /* phone({ body, tab, time, flush, overlay, raw, invStatus }) */
  function phone(o) {
    const cls = 'ph' + (o.tab ? ' has-tab' : '');
    let h = '<div class="' + cls + '">';
    if (o.raw) h += o.raw;
    h += statusBar(o.time, o.invStatus);
    if (o.body != null) h += '<div class="pb' + (o.flush ? ' flush' : '') + '">' + o.body + '</div>';
    if (o.tab) h += tabBar(o.tab);
    if (o.overlay) h += o.overlay;
    h += '<div class="hi' + (o.invStatus ? ' inv' : '') + '"></div></div>';
    return h;
  }

  /* ---------- Blocos ---------- */
  function top(o) {
    return '<div class="top"><div class="top-l">' + (o.left || '') + '</div>' +
      (o.title ? '<div class="top-t">' + o.title + '</div>' : '') +
      '<div class="top-r">' + (o.right || '') + '</div></div>';
  }
  const ib = (name, soft) => '<span class="ib' + (soft ? ' s' : '') + '">' + I(name) + '</span>';

  function sec(title, n, right, late) {
    return '<div class="sec"><div class="sec-t' + (late ? ' late' : '') + '">' + title +
      (n != null ? '<span class="n mono">' + n + '</span>' : '') + '</div>' + (right ? '<span class="link">' + right + '</span>' : '') + '</div>';
  }

  /* row({ kind:'task'|'mem'|'trig', t, meta:[...], trail, done, late, tight }) */
  function row(o) {
    let lead;
    if (o.kind === 'mem') lead = '<span class="mk">' + I('memory') + '</span>';
    else if (o.kind === 'trig') lead = '<span class="gk">' + I(o.icon || 'bolt') + '</span>';
    else lead = '<span class="chk' + (o.done ? ' on' : '') + (o.late ? ' late' : '') + '">' + (o.done ? I('check') : '') + '</span>';
    const meta = (o.meta || []).filter(Boolean).join('<span aria-hidden="true">·</span>');
    return '<div class="row' + (o.done ? ' done' : '') + (o.tight ? ' tight' : '') + '"><span class="row-lead">' + lead + '</span>' +
      '<div class="row-b"><div class="row-t">' + o.t + '</div>' + (meta ? '<div class="row-m">' + meta + '</div>' : '') + '</div>' +
      (o.trail ? '<div class="row-trail">' + o.trail + '</div>' : '') + '</div>';
  }
  const m = (ic, text) => '<span style="display:inline-flex;gap:4px;align-items:center">' + I(ic) + text + '</span>';

  const chip = (t, on, ic, extra) => '<span class="chip' + (on ? ' on' : '') + (extra ? ' ' + extra : '') + '">' + (ic ? I(ic) : '') + t + '</span>';
  const tag = (t, kind, ic) => '<span class="tag' + (kind ? ' ' + kind : '') + '">' + (ic ? I(ic) : '') + t + '</span>';
  const seg = (items, on) => '<div class="seg">' + items.map((x, i) => '<span' + (i === on ? ' class="on"' : '') + '>' + x + '</span>').join('') + '</div>';
  const tg = (on, dis) => '<span class="tg' + (on ? ' on' : '') + (dis ? ' dis' : '') + '"></span>';
  const btn = (t, kind, ic, extra) => '<div class="btn btn-' + (kind || 'p') + (extra ? ' ' + extra : '') + '">' + (ic ? I(ic) : '') + t + '</div>';

  function field(o) {
    return '<div class="fl">' + (o.label ? '<div class="fl-l">' + o.label + (o.opt ? '<span class="fl-o">' + o.opt + '</span>' : '') + '</div>' : '') +
      '<div class="fld' + (o.state ? ' ' + o.state : '') + '">' + (o.icon ? I(o.icon) : '') +
      '<span class="v' + (o.ph ? ' ph-t' : '') + '">' + (o.value || o.ph || '') + '</span>' + (o.state === 'focus' ? '<span class="caret"></span>' : '') +
      (o.trail || '') + '</div>' +
      (o.help ? '<div class="fl-h' + (o.state === 'err' ? ' err' : '') + '">' + (o.state === 'err' ? I('alert') : '') + o.help + '</div>' : '') + '</div>';
  }

  function opt(o) {
    return '<div class="opt' + (o.dis ? ' dis' : '') + '">' + (o.icon ? I(o.icon) : '') + '<div class="opt-b">' + o.t + (o.s ? '<small>' + o.s + '</small>' : '') + '</div>' +
      (o.radio != null ? '<span class="rd' + (o.radio ? ' on' : '') + '"></span>' : '') + (o.trail || '') + '</div>';
  }

  function banner(kind, icn, title, text, actions) {
    return '<div class="banner ' + kind + '">' + I(icn) + '<div><b>' + title + '</b>' + (text || '') +
      (actions ? '<div class="ba">' + actions.map((a) => '<span class="link">' + a + '</span>').join('') + '</div>' : '') + '</div></div>';
  }

  function sheet(content, noScrim) {
    return (noScrim ? '' : '<div class="scrim"></div>') + '<div class="sheet"><div class="grab"></div>' + content + '</div>';
  }
  const toast = (t, action) => '<div class="toast">' + I('check') + '<span>' + t + '</span>' + (action ? '<b>' + action + '</b>' : '') + '</div>';

  function empty(art, title, text, action) {
    return '<div class="empty">' + art + '<h3>' + title + '</h3><p>' + text + '</p>' + (action ? '<div style="margin-top:24px;width:100%">' + action + '</div>' : '') + '</div>';
  }

  function place(o) {
    return '<div class="pl"><span class="pl-ic' + (o.here ? ' here' : '') + '">' + I(o.icon) + '</span><div class="pl-b"><b>' + o.name + '</b><small>' + o.meta + '</small></div>' +
      (o.trg ? '<span class="pl-trg">' + o.trg.map((x) => I(x)).join('') + '</span>' : '') +
      (o.dist ? '<span class="dist">' + o.dist + '</span>' : '') + '</div>';
  }

  /* ---------- Mapa (SVG vetorial, sem serviço externo) ---------- */
  function mapPin(p) {
    const s = p.active ? 'var(--a-accent)' : 'var(--a-bg)';
    const c = p.active ? 'var(--a-accent-ink)' : 'var(--a-ink)';
    let g = '<g transform="translate(' + p.x + ' ' + p.y + ')">' +
      '<circle r="19" fill="' + s + '" stroke="' + (p.active ? 'var(--a-bg)' : 'var(--a-line)') + '" stroke-width="' + (p.active ? 3 : 1) + '" filter="url(#sh)"/>' +
      window.iconAt(p.ic, -10, -10, 20, c);
    if (p.n) {
      g += '<circle cx="14" cy="-14" r="9.5" fill="' + (p.mem ? 'var(--a-mem)' : 'var(--a-accent)') + '" stroke="var(--a-bg)" stroke-width="2"/>' +
        '<text x="14" y="-10.3" text-anchor="middle" font-family="IBM Plex Mono, monospace" font-size="10.5" font-weight="600" fill="' + (p.active ? 'var(--a-accent-ink)' : 'var(--a-bg)') + '">' + p.n + '</text>';
    }
    if (p.label) {
      const w = p.label.length * 7.2 + 18;
      g += '<rect x="' + (-w / 2) + '" y="24" width="' + w + '" height="22" rx="11" fill="var(--a-bg)" filter="url(#sh)"/>' +
        '<text y="39" text-anchor="middle" font-family="Onest, sans-serif" font-size="12" font-weight="600" fill="var(--a-ink)">' + p.label + '</text>';
    }
    return g + '</g>';
  }

  function map(o) {
    o = o || {};
    const vb = o.vb || '0 0 360 520';
    let s = '<svg class="map" viewBox="' + vb + '" preserveAspectRatio="xMidYMid slice" aria-hidden="true">' +
      '<defs><filter id="sh" x="-50%" y="-50%" width="200%" height="200%"><feDropShadow dx="0" dy="2" stdDeviation="2.5" flood-color="#0F1228" flood-opacity=".16"/></filter></defs>' +
      '<rect x="-200" y="-200" width="760" height="920" fill="var(--m-land)"/>' +
      '<g fill="var(--m-block)"><rect x="10" y="70" width="95" height="75" rx="6"/><rect x="160" y="70" width="80" height="60" rx="6"/><rect x="10" y="160" width="95" height="120" rx="6"/><rect x="235" y="170" width="70" height="75" rx="6"/><rect x="160" y="275" width="100" height="60" rx="6"/><rect x="315" y="175" width="60" height="80" rx="6"/><rect x="-20" y="325" width="70" height="40" rx="6"/></g>' +
      '<rect x="200" y="-10" width="120" height="70" rx="18" fill="var(--m-park)"/>' +
      '<rect x="72" y="315" width="70" height="44" rx="14" fill="var(--m-park)"/>' +
      '<path d="M-20 400 C 60 372, 130 420, 200 402 S 320 366, 400 392 V 560 H -20 Z" fill="var(--m-water)"/>' +
      '<g stroke="var(--m-road)" stroke-linecap="round" fill="none">' +
      '<path d="M-20 152 L380 162" stroke-width="11"/><path d="M142 -20 L152 400" stroke-width="11"/>' +
      '<path d="M-20 300 L380 262" stroke-width="9"/><path d="M308 -20 L312 380" stroke-width="9"/>' +
      '<path d="M-20 60 L380 64" stroke-width="5"/><path d="M-20 222 L380 224" stroke-width="5"/>' +
      '<path d="M52 -20 L60 390" stroke-width="5"/><path d="M226 -20 L230 390" stroke-width="5"/></g>' +
      '<g font-family="Onest, sans-serif" font-size="9" font-weight="500" fill="var(--m-label)" letter-spacing=".04em">' +
      '<text x="12" y="147" transform="rotate(1.4 12 147)">R. AUGUSTA</text><text x="158" y="370" transform="rotate(-88 158 370)">AV. PAULISTA</text></g>';
    if (o.radius) s += '<circle cx="' + o.radius.x + '" cy="' + o.radius.y + '" r="' + o.radius.r + '" fill="var(--a-accent)" fill-opacity=".1" stroke="var(--a-accent)" stroke-width="1.5" stroke-dasharray="4 4"/>';
    (o.pins || []).forEach((p) => { s += mapPin(p); });
    if (o.me) s += '<circle cx="' + o.me.x + '" cy="' + o.me.y + '" r="16" fill="var(--a-accent)" fill-opacity=".16"/><circle cx="' + o.me.x + '" cy="' + o.me.y + '" r="7" fill="var(--a-accent)" stroke="#fff" stroke-width="3"/>';
    return s + '</svg>';
  }

  /* Coleção padrão de lugares do usuário de exemplo */
  const PLACES = {
    mercado: { x: 170, y: 206, ic: 'cart', n: 3, name: 'Supermercado' },
    casa: { x: 88, y: 250, ic: 'home', n: 2, name: 'Casa' },
    trabalho: { x: 272, y: 105, ic: 'work', n: 3, name: 'Trabalho' },
    farmacia: { x: 262, y: 296, ic: 'pill', n: 1, name: 'Farmácia' },
    academia: { x: 80, y: 104, ic: 'gym', name: 'Academia' },
    oficina: { x: 320, y: 330, ic: 'tool', n: 1, mem: true, name: 'Oficina' }
  };
  /* pins('mercado') destaca um lugar e mostra o nome dele */
  function pins(activeKey) {
    return Object.keys(PLACES).map(function (k) {
      const p = Object.assign({}, PLACES[k]);
      p.active = k === activeKey;
      if (p.active) p.label = p.name;
      return p;
    });
  }

  /* ---------- Notificações ---------- */
  function notif(o) {
    let h = '<div class="nt-card' + (o.cls ? ' ' + o.cls : '') + '"><div class="nt-hd"><span class="app-i">' + logo(14, '#fff', '#fff') + '</span>Roteiro' + (o.sub ? ' · ' + o.sub : '') + '<span class="tm">' + (o.time || 'agora') + '</span></div>' +
      '<div class="nt-ti">' + o.title + '</div>' + (o.body ? '<div class="nt-bd">' + o.body + '</div>' : '');
    if (o.items) {
      h += '<div class="nt-list">' + o.items.map(function (it) {
        return '<div class="nt-li">' + (it.mem ? '<span class="mb">' + I('memory') + '</span>' : '<span class="c"></span>') + it.t + (it.a ? '<em>' + it.a + '</em>' : '') + '</div>';
      }).join('') + '</div>';
    }
    if (o.actions) h += '<div class="nt-ac">' + o.actions.map((a, i) => '<span' + (i === 0 && o.primary ? ' class="p"' : '') + '>' + a + '</span>').join('') + '</div>';
    return h + '</div>';
  }

  window.UI = { I, esc, logo, ring, phone, statusBar, tabBar, top, ib, sec, row, m, chip, tag, seg, tg, btn, field, opt, banner, sheet, toast, empty, place, map, pins, PLACES, notif };
})();
