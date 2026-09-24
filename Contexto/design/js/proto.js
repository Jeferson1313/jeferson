/* =========================================================
   Contexto — protótipo interativo
   Estado + telas montadas com os componentes de ui.js.
   Toda interação usa data-act="acao:arg1:arg2".
   ========================================================= */
(function () {
  const U = window.UI, I = U.I, A = window.ART;
  const esc = (s) => String(s == null ? '' : s).replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));
  const phoneEl = document.getElementById('phone');
  const simEl = document.getElementById('sim');

  /* ---------- Dados ---------- */
  const BASE_PLACES = {
    casa: { name: 'Casa', icon: 'home', sub: 'R. Harmonia, 88', x: 88, y: 250, em: 'em Casa', de: 'de casa', ao: 'em casa' },
    mercado: { name: 'Supermercado', icon: 'cart', sub: 'Pão de Açúcar · R. Augusta, 1200', x: 170, y: 206, em: 'no Supermercado', de: 'do supermercado', ao: 'ao supermercado' },
    trabalho: { name: 'Trabalho', icon: 'work', sub: 'Av. Paulista, 1578', x: 272, y: 105, em: 'no Trabalho', de: 'do trabalho', ao: 'ao trabalho', wifi: 'Escritorio-5G' },
    farmacia: { name: 'Farmácia', icon: 'pill', sub: 'R. da Consolação, 900', x: 262, y: 296, em: 'na Farmácia', de: 'da farmácia', ao: 'à farmácia' },
    academia: { name: 'Academia', icon: 'gym', sub: 'R. Frei Caneca, 320', x: 80, y: 104, em: 'na Academia', de: 'da academia', ao: 'à academia' },
    oficina: { name: 'Oficina', icon: 'tool', sub: 'R. Cardeal Arcoverde, 412', x: 320, y: 330, em: 'na Oficina', de: 'da oficina', ao: 'à oficina' },
    carro: { name: 'Carro', icon: 'car', sub: 'Bluetooth · HB20 Multimídia', geo: false, em: 'no Carro', de: 'do carro', ao: 'ao carro' }
  };
  const BASE_ITEMS = [
    { id: 1, kind: 'task', t: 'Alimentar o Thor', place: 'casa', when: 'arrive', rep: 'Diária' },
    { id: 2, kind: 'task', t: 'Pagar conta de luz', place: 'casa', when: 'arrive', late: 'Venceu ontem' },
    { id: 3, kind: 'mem', t: 'Anotar o número do medidor', place: 'casa', field: 'Leitura do medidor', unit: 'kWh', note: 'Fica na garagem, atrás do portão lateral. Leitura anterior: 04577.' },
    { id: 4, kind: 'task', t: 'Levar a marmita', place: 'casa', when: 'leave' },
    { id: 5, kind: 'task', t: 'Comprar sabão em pó', place: 'mercado', when: 'arrive' },
    { id: 6, kind: 'mem', t: 'Ver preço do arroz', place: 'mercado', field: 'Preço do pacote de 5 kg', unit: 'R$', note: 'Último preço anotado: R$ 27,90, em 10 set.' },
    { id: 7, kind: 'task', t: 'Enviar relatório mensal', place: 'trabalho', when: 'arrive', time: 'até 10:00' },
    { id: 8, kind: 'task', t: 'Pegar documento no RH', place: 'trabalho', when: 'leave' },
    { id: 9, kind: 'task', t: 'Falar com João', place: 'trabalho', when: 'arrive', note: 'sobre as férias de outubro' },
    { id: 10, kind: 'mem', t: 'Vaga do estacionamento: G2-214', place: 'trabalho' },
    { id: 11, kind: 'task', t: 'Comprar protetor solar', place: 'farmacia', when: 'arrive' },
    { id: 12, kind: 'task', t: 'Passar na farmácia', place: 'carro', when: 'arrive' },
    { id: 13, kind: 'mem', t: 'Perguntar o preço da pastilha de freio', place: 'oficina' },
    { id: 14, kind: 'task', t: 'Tomar vitamina D', place: null, when: 'time', time: '20:00', rep: 'Diária' }
  ];
  const BASE_ROUTINES = [
    { id: 'r1', name: 'Cheguei em casa', on: true, icon: 'arrive', w: 'Ao chegar em Casa · depois das 17h', steps: [['Lembrar', 'Guardar compras', 'Só se você passou no Supermercado hoje'], ['Mostrar tarefa', 'Alimentar o Thor', 'Repete todo dia'], ['Iniciar rotina', 'Rotina da noite', '3 passos · às 21:00']] },
    { id: 'r2', name: 'Saindo para o trabalho', on: true, icon: 'leave', w: 'Ao sair de Casa · dias úteis', steps: [['Lembrar', 'Crachá', ''], ['Lembrar', 'Marmita', ''], ['Lembrar', 'Garrafa d’água', '']] },
    { id: 'r3', name: 'Entrei no carro', on: true, icon: 'car', w: 'Ao conectar ao HB20', steps: [['Mostrar tarefas', 'Tarefas do Carro', ''], ['Abrir lista', 'No caminho', '']] },
    { id: 'r4', name: 'Treino', on: false, icon: 'nfc', w: 'Ao encostar na tag do armário', steps: [['Lembrar', 'Registrar treino', '']] }
  ];
  const clone = (o) => JSON.parse(JSON.stringify(o));
  const PIN_SPOTS = [[200, 340], [34, 196], [330, 36]];

  let S;
  function reset(skipIntro) {
    const places = clone(BASE_PLACES);
    Object.keys(places).forEach((k) => { places[k].trig = { arrive: true, leave: true }; });
    S = {
      screen: skipIntro ? { n: 'agora' } : { n: 'splash' }, stack: [], tab: 'agora',
      places: places, order: Object.keys(places), items: clone(BASE_ITEMS), routines: clone(BASE_ROUTINES), triggers: [],
      nextId: 100, here: skipIntro ? 'casa' : null, lastLeft: null, visit: 1, justArrived: false, uncertain: false,
      locDenied: false, locked: false, notif: null, overlay: null, toast: null, run: null,
      min: 17 * 60 + 40, log: [], draft: null, pinSel: null, listSeg: 'task', listFilter: 'todas',
      settings: { dwell: 2, group: true, history: false },
      tour: {}
    };
    if (skipIntro) addLog('Detectado em Casa pela localização');
    render('none');
    if (!skipIntro) setTimeout(() => { if (S.screen.n === 'splash') { S.screen = { n: 'ob', k: 1 }; render('fade'); } }, 1400);
  }

  /* ---------- Utilidades ---------- */
  const P = (id) => S.places[id];
  const clock = () => { const h = Math.floor(S.min / 60) % 24, m = S.min % 60; return h + ':' + String(m).padStart(2, '0'); };
  function addLog(t) { S.log.unshift({ t: t, at: clock() }); S.log = S.log.slice(0, 7); }
  const alive = (it) => !it.done && !it.archived && !it.snoozed;
  const pendingAt = (p, kind) => S.items.filter((it) => it.place === p && alive(it) && (!kind || it.kind === kind));
  const shownAt = (p) => S.items.filter((it) => it.place === p && !it.archived && (alive(it) || (it.done && it.doneVisit === S.visit)));
  function mePos() {
    if (S.here && P(S.here).x != null) return { x: P(S.here).x + 8, y: P(S.here).y + 8 };
    if (S.lastLeft && P(S.lastLeft).x != null) return { x: P(S.lastLeft).x + 34, y: P(S.lastLeft).y + 26 };
    return { x: 196, y: 180 };
  }
  function dist(p) {
    const pl = P(p); if (pl.x == null) return '';
    const me = mePos(), d = Math.round(Math.hypot(pl.x - me.x, pl.y - me.y) * 11);
    if (d < 60) return 'aqui';
    return d < 1000 ? (Math.round(d / 10) * 10) + ' m' : (d / 1000).toFixed(1).replace('.', ',') + ' km';
  }
  let toastTimer;
  function toast(t, undo) {
    S.toast = { t: t, undo: undo };
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => { S.toast = null; render('none'); }, 3400);
  }

  /* ---------- Navegação ---------- */
  function go(n, extra) {
    S.stack.push(S.screen);
    S.screen = Object.assign({ n: n }, extra || {});
    S.overlay = null;
    render('in');
  }
  function back() {
    S.overlay = null;
    S.screen = S.stack.pop() || { n: S.tab };
    render('back');
  }
  function tab(t) {
    S.tab = t; S.stack = []; S.overlay = null; S.pinSel = null; S.justArrived = false;
    S.screen = { n: t };
    if (t !== 'agora') { S.tour['x_' + t] = true; }
    render('fade');
  }

  /* ---------- Componentes locais (com ações) ---------- */
  function tabBar(active) {
    const T = [['agora', 'now', 'Agora'], ['lugares', 'places', 'Lugares'], ['add'], ['lista', 'list', 'Lista'], ['rotinas', 'routine', 'Rotinas']];
    return '<nav class="tab">' + T.map((t) => t[0] === 'add'
      ? '<div class="tab-add" data-act="addsheet" role="button" aria-label="Adicionar"><span>' + I('plus') + '</span></div>'
      : '<div class="tab-i' + (t[0] === active ? ' on' : '') + '" data-act="tab:' + t[0] + '" role="button">' + I(t[1]) + '<span>' + t[2] + '</span></div>').join('') + '</nav>';
  }
  const back_ = (ic) => '<span class="ib" data-act="back" role="button" aria-label="Voltar">' + I(ic || 'chevL') + '</span>';
  const WHEN = { arrive: ['arrive', 'Ao chegar'], leave: ['leave', 'Ao sair'], time: ['clock', 'Horário'], none: ['eyeoff', 'Sem aviso'] };

  function itemRow(it, o) {
    o = o || {};
    const meta = [];
    if (it.kind === 'mem') {
      meta.push(U.tag('Memória', 'mem'));
      if (it.value) meta.push('<span class="mono">' + esc(it.value) + (it.unit ? ' ' + esc(it.unit) : '') + '</span>');
      else if (it.field) meta.push('anotar o valor');
    } else {
      if (it.late && !it.done) meta.push('<span class="late">' + it.late + '</span>');
      if (it.time) meta.push(U.m('clock', esc(it.time)));
      else if (it.when && WHEN[it.when] && it.when !== 'arrive') meta.push(U.m(WHEN[it.when][0], WHEN[it.when][1]));
      if (it.rep && it.rep !== 'Uma vez') meta.push(U.m('repeat', it.rep));
      if (it.note) meta.push(esc(it.note));
      if (it.snoozed) meta.push(U.tag('próxima visita', 'acc'));
    }
    if (o.place && it.place) meta.push(U.m(P(it.place).icon, P(it.place).name));
    const lead = it.kind === 'mem'
      ? '<span class="row-lead"><span class="mk">' + I('memory') + '</span></span>'
      : '<span class="row-lead" data-act="toggle:' + it.id + '" role="checkbox" aria-checked="' + !!it.done + '"><span class="chk' + (it.done ? ' on' : '') + (it.late && !it.done ? ' late' : '') + '">' + (it.done ? I('check') : '') + '</span></span>';
    return '<div class="row' + (it.done ? ' done' : '') + (o.tight ? ' tight' : '') + '">' + lead +
      '<div class="row-b" data-act="' + (it.kind === 'mem' ? 'mem' : 'item') + ':' + it.id + '"><div class="row-t">' + esc(it.t) + '</div>' + (meta.length ? '<div class="row-m">' + meta.join('<span aria-hidden="true">·</span>') + '</div>' : '') + '</div></div>';
  }
  function placeRow(id, o) {
    const pl = P(id), t = pendingAt(id, 'task').length, m = pendingAt(id, 'mem').length;
    const parts = [];
    if (id === S.here) parts.push('<span style="color:var(--a-accent);font-weight:600">Você está aqui</span>');
    if (pl.geo === false) parts.push('Sem endereço');
    parts.push(t ? t + (t > 1 ? ' tarefas' : ' tarefa') : 'sem tarefas');
    if (m) parts.push(m + (m > 1 ? ' memórias' : ' memória'));
    const trg = [];
    if (pl.geo !== false) { if (pl.trig.arrive) trg.push('arrive'); if (pl.trig.leave) trg.push('leave'); }
    if (pl.wifi) trg.push('wifi');
    if (pl.geo === false) trg.push('bt');
    return '<div class="pl" data-act="' + (o && o.act ? o.act : 'place:' + id) + '"><span class="pl-ic' + (id === S.here ? ' here' : '') + '">' + I(pl.icon) + '</span><div class="pl-b"><b>' + esc(pl.name) + '</b><small>' + parts.join(' · ') + '</small></div>' +
      (o && o.noTrg ? '' : '<span class="pl-trg">' + trg.map((x) => I(x)).join('') + '</span>') + (id !== S.here && dist(id) ? '<span class="dist">' + dist(id) + '</span>' : '') + '</div>';
  }
  function mapHTML(o) {
    const pins = S.order.filter((k) => P(k).x != null).map((k) => {
      const pl = P(k), n = pendingAt(k).length, onlyMem = n && !pendingAt(k, 'task').length;
      const active = o.active ? k === o.active : false;
      return { k: k, x: pl.x, y: pl.y, ic: pl.icon, n: n || 0, mem: onlyMem, active: active, label: active ? pl.name : null };
    });
    let svg = U.map({ vb: o.vb, pins: o.only ? pins.filter((p) => p.k === o.only) : pins, me: o.noMe ? null : mePos(), radius: o.radius });
    if (o.clickable) pins.forEach((p) => { svg = svg.replace('<g transform="translate(' + p.x + ' ' + p.y + ')">', '<g data-act="pin:' + p.k + '" transform="translate(' + p.x + ' ' + p.y + ')">'); });
    return svg;
  }
  const vbAround = (id, w, h) => { const p = P(id); return (p.x - w / 2) + ' ' + (p.y - h / 2) + ' ' + w + ' ' + h; };
  function hero(k, title, meta, state, off) {
    return '<div class="ctx"><div class="ctx-tx"><div class="ctx-k' + (off ? ' off' : '') + '">' + (off ? '' : '<span class="live"></span>') + k + '</div><div class="ctx-t">' + title + '</div><div class="ctx-m">' + meta + '</div></div>' + U.ring(state) + '</div>';
  }
  const homeTop = () => U.top({ left: '<span class="date">' + U.logo(22) + 'Sexta, 24 set</span>', right: '<span class="av" data-act="go:voce" role="button" aria-label="Você">LA</span>' });
  function chipA(t, on, ic, act, extra) { return '<span class="chip' + (on ? ' on' : '') + (extra ? ' ' + extra : '') + '" data-act="' + act + '">' + (ic ? I(ic) : '') + t + '</span>'; }
  function optA(o) {
    return '<div class="opt' + (o.dis ? ' dis' : '') + '"' + (o.act && !o.dis ? ' data-act="' + o.act + '"' : '') + '>' + (o.icon ? I(o.icon) : '') + '<div class="opt-b">' + o.t + (o.s ? '<small>' + o.s + '</small>' : '') + '</div>' +
      (o.radio != null ? '<span class="rd' + (o.radio ? ' on' : '') + '"></span>' : '') + (o.trail || '') + '</div>';
  }
  const tgA = (on, act) => '<span class="tg' + (on ? ' on' : '') + '" data-act="' + act + '" role="switch" aria-checked="' + !!on + '"></span>';
  const btnA = (t, kind, act, ic, extra) => '<div class="btn btn-' + kind + (extra ? ' ' + extra : '') + '" data-act="' + act + '" role="button">' + (ic ? I(ic) : '') + t + '</div>';
  const segA = (items, on, pre) => '<div class="seg">' + items.map((x) => '<span' + (x[0] === on ? ' class="on"' : '') + ' data-act="' + pre + ':' + x[0] + '">' + x[1] + '</span>').join('') + '</div>';

  /* =========================================================
     TELAS
     ========================================================= */
  const SCR = {};

  SCR.splash = () => ({ inv: true, raw: '<div class="splash">' + U.logo(84, '#fff', '#fff') + '<div class="wm">contexto</div><div class="tl">Lembre no lugar certo.</div><div class="ver">v1.0</div></div>' });

  SCR.ob = (s) => {
    const T = [
      [A.obArt1, 'Coloque cada coisa no lugar onde ela acontece.', 'Comprar café fica no mercado. Enviar o relatório fica no trabalho. Sua cabeça fica livre.'],
      [A.obArt2, 'Chegou no lugar certo? A gente lembra você.', 'Ao chegar ou sair de um lugar, o Contexto mostra só o que importa ali. O resto espera.'],
      [A.obArt3, 'Casa, trabalho, carro ou mercado.', 'Qualquer lugar vira um contexto: um endereço, o Bluetooth do carro, o Wi-Fi do escritório ou uma tag NFC.']
    ][s.k - 1];
    return { raw: '<div class="ob"><div class="ob-skip">' + (s.k < 3 ? '<span data-act="obskip">Pular</span>' : '&nbsp;') + '</div><div class="ob-art">' + T[0] + '</div><h2>' + T[1] + '</h2><p>' + T[2] + '</p>' +
      '<div class="dots">' + [1, 2, 3].map((i) => '<i' + (i === s.k ? ' class="on"' : '') + '></i>').join('') + '</div>' + btnA(s.k < 3 ? 'Continuar' : 'Começar', 'p', 'obnext') + '</div>' };
  };

  SCR.perm = () => ({ raw: '<div class="ob"><div class="ob-skip">&nbsp;</div><div class="ob-art" style="flex:0 0 220px">' + A.obArtPerm.replace('viewBox="0 0 280 280"', 'viewBox="0 0 280 280" style="width:200px;height:200px"') + '</div>' +
    '<h2>Para lembrar no lugar certo, precisamos saber onde você está.</h2>' +
    '<div class="perm-list"><div class="perm-li">' + I('shield') + '<div><b>Fica no seu aparelho</b>Sua localização não é enviada a ninguém.</div></div>' +
    '<div class="perm-li">' + I('battery') + '<div><b>Leve para a bateria</b>Usamos as cercas do próprio sistema, não GPS contínuo.</div></div></div>' +
    '<div style="flex:1"></div>' + btnA('Permitir localização', 'p', 'permit:1') + '<div class="btn btn-g" style="height:44px;margin-top:4px" data-act="permit:0">Agora não</div></div>' });

  SCR.setup = () => {
    const rows = [['casa', 'Você está aqui agora', 1], ['trabalho', 'Av. Paulista, 1578', 1], ['mercado', 'Pão de Açúcar · R. Augusta', 1], ['carro', 'Pelo Bluetooth do carro', 1], ['academia', 'Opcional', 0]];
    return { foot: 110, body: U.top({ left: '' }) + '<div class="h1">Onde você passa seu dia?</div><div class="sub">Toque para confirmar. Você ajusta os endereços depois.</div><div style="margin-top:14px">' +
      rows.map((r) => { const on = S.setupSel ? S.setupSel[r[0]] : r[2]; return '<div class="pl" data-act="setupsel:' + r[0] + '"><span class="pl-ic' + (r[0] === 'casa' ? ' here' : '') + '">' + I(P(r[0]).icon) + '</span><div class="pl-b"><b>' + P(r[0]).name + '</b><small>' + r[1] + '</small></div><span class="chk' + (on ? ' on' : '') + '">' + (on ? I('check') : '') + '</span></div>'; }).join('') +
      '</div>', footHTML: '<div class="foot">' + btnA('Concluir', 'p', 'setupdone') + '</div>' };
  };

  SCR.agora = () => {
    let b = homeTop();
    const h = S.here;
    if (!h && S.locDenied) {
      b += hero('Agora', 'Onde você está?', '<span>Localização desativada</span>', 'warn', true) +
        '<div style="margin-top:14px">' + '<div class="banner warn">' + I('locoff') + '<div><b>Não conseguimos perceber quando você chega</b>Sem localização, avisos de chegada e saída ficam pausados.<div class="ba"><span class="link" data-act="go:perms">Ativar localização</span></div></div></div></div>' +
        U.sec('Escolha onde está') + '<div class="chips" style="margin-top:8px">' + ['casa', 'trabalho', 'mercado'].map((k) => chipA(P(k).name, false, P(k).icon, 'sethere:' + k)).join('') + '</div>' +
        U.sec('Continua funcionando') +
        '<div class="set">' + I('wifi') + '<div class="set-b">Wi-Fi<small>Trabalho reconhece o Escritorio-5G</small></div><span class="st ok">Ativo</span></div>' +
        '<div class="set">' + I('bt') + '<div class="set-b">Bluetooth do carro</div><span class="st ok">Ativo</span></div>' +
        '<div class="set">' + I('clock') + '<div class="set-b">Horários</div><span class="st ok">Ativo</span></div>';
      return { tab: 'agora', body: b };
    }
    if (!h && S.uncertain) {
      b += hero('Talvez você esteja em', 'Supermercado?', '<span>Sinal de localização fraco</span><span>·</span><span class="mono">±140 m</span>', 'out', true) +
        '<div class="btn-row" style="margin-top:14px">' + btnA('Sim, estou aqui', 'p', 'sethere:mercado', null, 'sm') + btnA('Não estou', 'o', 'notuncertain', null, 'sm') + '</div>' +
        U.sec('Ou escolha onde está') + '<div class="chips" style="margin-top:8px">' + ['casa', 'farmacia', 'trabalho'].map((k) => chipA(P(k).name, false, P(k).icon, 'sethere:' + k)).join('') + '</div>' +
        U.sec('Se for o supermercado', pendingAt('mercado').length) + pendingAt('mercado').map((it) => itemRow(it)).join('');
      return { tab: 'agora', body: b };
    }
    if (h) {
      const pl = P(h), shown = shownAt(h), pend = pendingAt(h);
      b += hero(S.justArrived ? 'Você chegou' : 'Agora · você está em', esc(pl.name),
        '<b>' + esc(pl.sub.split(' · ')[0]) + '</b><span>·</span><span>' + (S.justArrived ? 'agora mesmo' : 'há alguns min') + '</span><span>·</span><span class="link" style="font-size:13px" data-act="nothere">Não é aqui?</span>', 'in');
      if (S.run) {
        const r = S.routines.find((x) => x.id === S.run.id), st = r.steps[S.run.step];
        b += '<div class="run"><div class="run-b"><small>Rotina · ' + esc(r.name) + '</small>' + esc(st[1]) + '<div class="bar">' + r.steps.map((x, i) => '<i' + (i <= S.run.step ? ' class="on"' : '') + '></i>').join('') + '</div></div>' + btnA('Feito', 'p', 'runnext', null, 'sm') + '</div>';
      }
      if (shown.length) {
        b += U.sec(pend.length ? (S.justArrived ? pend.length + (pend.length > 1 ? ' coisas para lembrar' : ' coisa para lembrar') : 'Pendências aqui') : 'Tudo feito aqui', pend.length && !S.justArrived ? pend.length : null, '<span data-act="place:' + h + '">Ver lugar</span>');
        b += shown.filter((i) => i.kind === 'task').map((it) => itemRow(it)).join('');
        const mems = shown.filter((i) => i.kind === 'mem');
        if (mems.length) b += U.sec(mems.length > 1 ? 'Memórias deste lugar' : 'Memória deste lugar') + mems.map((it) => itemRow(it)).join('');
      } else {
        b += '<div class="mini-empty">Nada esperando por você ' + pl.em + '.' + btnA('Adicionar ' + pl.em, 's', 'newtask:' + h, 'plus', 'sm') + '</div>';
      }
      const late = S.items.filter((it) => it.late && alive(it) && it.place !== h);
      if (late.length) b += U.sec('Atrasada', late.length, null, true) + late.map((it) => itemRow(it, { place: true })).join('');
      b += U.sec('Próximos gatilhos');
      const lp = pendingAt(h, 'task').length;
      b += '<div class="nt"><span class="nt-when">saída</span><span class="nt-ic">' + I('leave') + '</span><span class="nt-t">Ao sair daqui<small>' + (lp ? 'Confere ' + lp + (lp > 1 ? ' pendências' : ' pendência') : 'Nada pendente, sem aviso') + '</small></span></div>';
      const cp = pendingAt('carro').length;
      if (cp) b += '<div class="nt" data-act="place:carro"><span class="nt-when">carro</span><span class="nt-ic">' + I('car') + '</span><span class="nt-t">Ao conectar ao HB20<small>' + esc(pendingAt('carro')[0].t) + '</small></span></div>';
      return { tab: 'agora', body: b };
    }
    /* Em trânsito */
    b += hero('Agora', 'Em trânsito', '<span>' + (S.lastLeft ? 'Saiu ' + P(S.lastLeft).de + ' há pouco' : 'Fora dos seus lugares') + '</span>', 'out', true);
    const near = S.order.filter((k) => P(k).x != null && pendingAt(k).length && k !== S.lastLeft)
      .sort((a, c) => Math.hypot(P(a).x - mePos().x, P(a).y - mePos().y) - Math.hypot(P(c).x - mePos().x, P(c).y - mePos().y));
    if (near.length) {
      b += U.sec('Perto de você', null, '<span data-act="tab:lugares">Mapa</span>') + '<div class="near-row">' + near.map((k) => {
        const n = pendingAt(k).length, t = pendingAt(k, 'task').length;
        return '<div class="near" data-act="place:' + k + '"><div class="near-h">' + I(P(k).icon) + esc(P(k).name) + '</div><div class="near-m"><span class="mono">' + dist(k) + '</span> · ' + (t ? n + (n > 1 ? ' itens' : ' item') : n + (n > 1 ? ' memórias' : ' memória')) + '</div></div>';
      }).join('') + '</div>';
    }
    const snoozed = S.lastLeft ? S.items.filter((it) => it.place === S.lastLeft && it.snoozed && !it.done) : [];
    if (snoozed.length) b += U.sec('Ficou para a próxima visita', snoozed.length) + snoozed.map((it) => itemRow(it, { place: true })).join('');
    const nextK = S.lastLeft === 'casa' ? 'mercado' : 'casa';
    b += U.sec('Próximo contexto') + '<div class="nt" data-act="place:' + nextK + '"><span class="nt-when">~12 min</span><span class="nt-ic">' + I(P(nextK).icon) + '</span><span class="nt-t">' + P(nextK).name + '<small>' + (pendingAt(nextK).length ? pendingAt(nextK).length + ' coisas esperando por você' : 'Nada pendente') + '</small></span>' + (pendingAt(nextK).length ? '<span class="cnt">' + pendingAt(nextK).length + '</span>' : '') + '</div>';
    const today = S.items.filter((it) => alive(it) && (it.place == null || it.late));
    if (today.length) b += U.sec('Hoje, em qualquer lugar', today.length) + today.map((it) => itemRow(it, { place: true })).join('');
    return { tab: 'agora', body: b };
  };

  SCR.lugares = () => {
    const sel = S.pinSel;
    let body = '<div class="map-wrap" style="position:absolute;inset:0 0 ' + (sel ? '0' : '300px') + ' 0">' + mapHTML({ clickable: true, vb: sel ? '-40 0 440 600' : '-10 20 380 400', active: sel || S.here, radius: sel && P(sel).x != null ? { x: P(sel).x, y: P(sel).y, r: 40 } : null }) +
      '<div class="map-top"><div class="srch">' + I('search') + 'Buscar lugar ou endereço</div><span class="fab-s" data-act="' + (sel ? 'pinclose' : 'go:newplace') + '" aria-label="' + (sel ? 'Fechar' : 'Novo lugar') + '">' + I(sel ? 'x' : 'plus') + '</span></div></div>';
    if (sel) {
      const pl = P(sel), items = pendingAt(sel);
      body += '<div class="sheet a-up" style="padding-bottom:16px"><div class="grab"></div>' +
        '<div style="display:flex;justify-content:space-between;align-items:flex-start"><div><div class="h2">' + esc(pl.name) + '</div><div class="sub">' + esc(pl.sub) + (dist(sel) ? ' · <span class="mono">' + dist(sel) + '</span>' : '') + '</div></div><span class="ib s" data-act="pinclose">' + I('x') + '</span></div>' +
        '<div class="stats"><span><b>' + pendingAt(sel, 'task').length + '</b>tarefas</span><span><b>' + pendingAt(sel, 'mem').length + '</b>memórias</span><span><b>' + ((pl.trig.arrive ? 1 : 0) + (pl.trig.leave ? 1 : 0)) + '</b>gatilhos</span></div>' +
        '<div style="margin-top:6px">' + (items.slice(0, 2).map((it) => itemRow(it, { tight: true })).join('') || '<div class="sub" style="padding:12px 0">Nada pendente aqui.</div>') + '</div>' +
        '<div style="margin-top:10px">' + btnA('Abrir lugar', 'p', 'place:' + sel) + '</div></div>';
    } else {
      body += '<div class="sheet" style="padding-bottom:6px;max-height:300px;box-shadow:0 -8px 30px rgba(15,18,40,.10)"><div class="grab"></div>' +
        '<div style="display:flex;justify-content:space-between;align-items:baseline"><div class="h2">Seus lugares</div><span class="dist">' + S.order.length + ' lugares</span></div>' +
        '<div style="margin-top:6px">' + S.order.slice().sort((a, c) => (c === S.here) - (a === S.here)).map((k) => placeRow(k)).join('') + '</div></div>';
    }
    return { tab: 'lugares', flush: true, body: body };
  };

  SCR.place = (s) => {
    const id = s.id, pl = P(id), shown = shownAt(id);
    const tasks = shown.filter((i) => i.kind === 'task'), mems = shown.filter((i) => i.kind === 'mem');
    let b;
    if (pl.x != null) {
      b = '<div class="pd-map">' + mapHTML({ vb: vbAround(id, 200, 100), only: id, active: id, radius: { x: pl.x, y: pl.y, r: 34 }, noMe: S.here !== id }) +
        '<div class="map-top" style="justify-content:space-between"><span class="fab-s" data-act="back" aria-label="Voltar">' + I('chevL') + '</span></div></div>';
    } else {
      b = U.top({ left: back_() }) + '<div class="car-card"><span class="pl-ic" style="background:var(--a-bg)">' + I(pl.icon) + '</span><div><b style="display:block">Reconhecido pelo Bluetooth</b><span class="sub" style="margin:0">HB20 Multimídia · pareado</span></div></div>';
    }
    b += '<div class="pd-head">' + (id === S.here ? U.tag('Você está aqui', 'acc', 'now') : '') + '<div class="h1" style="margin-top:8px">' + esc(pl.name) + '</div><div class="sub">' + esc(pl.sub) + (pl.x != null ? ' · raio <span class="mono">80 m</span>' : '') + '</div></div>';
    b += U.sec('Tarefas', tasks.length, '<span data-act="newtask:' + id + '">+ Adicionar</span>') + (tasks.map((it) => itemRow(it, { tight: true })).join('') || '<div class="sub" style="padding:8px 0">Nenhuma tarefa aqui.</div>');
    b += U.sec('Memórias', mems.length, '<span data-act="newmem:' + id + '">+ Adicionar</span>') + (mems.map((it) => itemRow(it, { tight: true })).join('') || '<div class="sub" style="padding:8px 0">Nenhuma memória aqui.</div>');
    b += U.sec('Quando lembrar', null, '<span data-act="newtrig:' + id + '">+ Gatilho</span>');
    if (pl.x != null) {
      b += '<div class="trg-li">' + I('arrive') + '<div class="b">Ao chegar<small>Depois de ' + S.settings.dwell + ' min no local</small></div>' + tgA(pl.trig.arrive, 'ptrig:' + id + ':arrive') + '</div>' +
        '<div class="trg-li">' + I('leave') + '<div class="b">Ao sair<small>Se algo ficou pendente</small></div>' + tgA(pl.trig.leave, 'ptrig:' + id + ':leave') + '</div>';
    } else {
      b += '<div class="trg-li">' + I('bt') + '<div class="b">Ao conectar ao HB20<small>Mostra as tarefas do Carro</small></div>' + tgA(pl.trig.arrive, 'ptrig:' + id + ':arrive') + '</div>';
    }
    if (pl.wifi) b += '<div class="trg-li">' + I('wifi') + '<div class="b">Wi-Fi ' + pl.wifi + '<small>Confirma a chegada na hora</small></div>' + U.tg(true) + '</div>';
    S.triggers.filter((t) => t.place === id).forEach((t) => { b += '<div class="trg-li">' + I(t.icon) + '<div class="b">' + esc(t.label) + '<small>' + esc(t.then) + '</small></div>' + tgA(t.on, 'ctrig:' + t.id) + '</div>'; });
    return { body: b, flushTop: pl.x != null };
  };

  SCR.lista = () => {
    const seg = S.listSeg, f = S.listFilter, kind = seg;
    const base = S.items.filter((it) => it.kind === kind && !it.archived);
    let items;
    if (f === 'concluidas') items = base.filter((it) => it.done);
    else if (f === 'atrasadas') items = base.filter((it) => it.late && !it.done);
    else if (f === 'hoje') items = base.filter((it) => !it.done && (it.place == null || it.late || it.place === S.here || it.time));
    else items = base.filter((it) => !it.done);
    const nT = S.items.filter((it) => it.kind === 'task' && !it.done && !it.archived).length, nM = S.items.filter((it) => it.kind === 'mem' && !it.done && !it.archived).length;
    const nLate = base.filter((it) => it.late && !it.done).length;
    let b = U.top({ left: '<span class="h2">Lista</span>' }) + segA([['task', 'Tarefas · ' + nT], ['mem', 'Memórias · ' + nM]], seg, 'lseg') +
      '<div class="chips nowrap" style="margin-top:12px">' + [['todas', 'Todas'], ['hoje', 'Hoje'], ['lugar', 'Por lugar'], ['atrasadas', 'Atrasadas' + (nLate ? ' · ' + nLate : '')], ['concluidas', 'Concluídas']].map((c) => chipA(c[1], f === c[0], null, 'lfilter:' + c[0])).join('') + '</div>';
    if (!items.length) {
      const E = { atrasadas: ['check', 'ok', 'Nada atrasado', 'Tudo que tinha prazo foi resolvido. O resto espera o lugar certo, sem pressa.'], concluidas: ['check', null, 'Nada concluído ainda', 'Marque um item em Agora e ele aparece aqui.'], hoje: ['calendar', null, 'Nada para hoje', 'Os itens presos a lugares aparecem quando você chegar lá.'] }[f] || ['plus', null, 'Lista vazia', 'Toque em + para adicionar.'];
      b += '<div style="margin-top:64px">' + U.empty(A.art(E[0], E[1]), E[2], E[3]) + '</div>';
      return { tab: 'lista', body: b };
    }
    if (f === 'lugar') {
      const groups = {};
      items.forEach((it) => { const k = it.place || '_'; (groups[k] = groups[k] || []).push(it); });
      Object.keys(groups).sort((a, c) => (c === S.here) - (a === S.here)).forEach((k) => {
        b += U.sec(k === '_' ? 'Qualquer lugar' : P(k).name + (k === S.here ? ' · aqui' : ''), groups[k].length, k === '_' ? null : '<span data-act="place:' + k + '">Abrir</span>') + groups[k].map((it) => itemRow(it, { tight: true })).join('');
      });
    } else {
      b += '<div style="margin-top:8px">' + items.map((it) => itemRow(it, { tight: true, place: true })).join('') + '</div>';
    }
    return { tab: 'lista', body: b };
  };

  SCR.rotinas = () => {
    let b = U.top({ left: '<span class="h2">Rotinas</span>' }) + '<div class="sub" style="margin-top:0">Sequências que começam sozinhas quando algo acontece.</div>';
    S.routines.forEach((r) => {
      b += '<div class="rt' + (r.on ? '' : ' off') + '" data-act="go:rotina:' + r.id + '"><div class="rt-h"><b>' + esc(r.name) + '</b>' + tgA(r.on, 'rtog:' + r.id) + '</div><div class="rt-w">' + I(r.icon) + esc(r.w) + '</div>' +
        (r.steps.length ? '<div class="rt-steps">' + r.steps.map((s) => U.tag(esc(s[1]))).join('') + '</div>' : '') + '</div>';
    });
    const models = [['m1', 'Fim do expediente'], ['m2', 'Mercado da semana'], ['m3', 'Viagem']].filter((m) => !S.routines.some((r) => r.id === m[0]));
    if (models.length) b += U.sec('Modelos') + '<div class="chips" style="margin-top:8px">' + models.map((m) => chipA(m[1], false, 'plus', 'model:' + m[0])).join('') + '</div>';
    return { tab: 'rotinas', body: b };
  };

  SCR.rotina = (s) => {
    const r = S.routines.find((x) => x.id === s.id);
    let b = U.top({ left: back_() }) +
      '<div style="display:flex;justify-content:space-between;align-items:center;gap:12px"><div class="h1">' + esc(r.name) + '</div>' + tgA(r.on, 'rtog:' + r.id) + '</div>' +
      '<div class="sub">' + r.steps.length + ' passos · ' + (r.on ? 'ativa' : 'pausada') + '</div><div class="flow" style="margin-top:18px">' +
      '<div class="fs"><span class="fs-dot trg">' + I(r.icon) + '</span><div class="fs-k">Quando</div><div class="fs-t">' + esc(r.w.split(' · ')[0]) + '<small>' + esc(r.w.split(' · ').slice(1).join(' · ') || 'Sempre') + '</small></div></div>' +
      r.steps.map((st, i) => '<div class="fs"><span class="fs-dot">' + (i + 1) + '</span><div class="fs-k">' + esc(st[0]) + '</div><div class="fs-t">' + esc(st[1]) + (st[2] ? '<small>' + esc(st[2]) + '</small>' : '') + '</div></div>').join('') +
      '<div class="fs add" data-act="addstep:' + r.id + '"><span class="fs-dot">' + I('plus') + '</span><div class="fs-t" style="padding-top:5px">Adicionar passo</div></div></div>';
    return { body: b, foot: 110, footHTML: '<div class="foot">' + btnA('Testar agora', 'o', 'testrun:' + r.id, 'play') + '</div>' };
  };

  SCR.voce = () => {
    const st = S.settings;
    let b = U.top({ left: back_(), title: 'Você' }) +
      '<div style="display:flex;gap:14px;align-items:center;margin-top:4px"><span class="av" style="width:52px;height:52px;font-size:17px">LA</span><div><div style="font-weight:600;font-size:17px">Lucas Almeida</div><div class="sub" style="margin-top:0">Dados salvos só neste aparelho</div></div></div>' +
      U.sec('Detecção') +
      '<div class="set" data-act="go:perms">' + I('pin') + '<div class="set-b">Localização</div>' + (S.locDenied ? '<span class="st bad">Negada</span>' : '<span class="st ok">Sempre</span>') + '</div>' +
      '<div class="set" data-act="go:perms">' + I('bt') + '<div class="set-b">Bluetooth</div><span class="st ok">Ativo</span></div>' +
      '<div class="set" data-act="go:perms">' + I('wifi') + '<div class="set-b">Wi-Fi</div><span class="st ok">Ativo</span></div>' +
      '<div class="set" data-act="go:perms">' + I('nfc') + '<div class="set-b">NFC</div><span class="st na">Indisponível</span></div>' +
      U.sec('Avisos') +
      '<div class="set" data-act="dwell">' + I('clock') + '<div class="set-b">Tempo mínimo no lugar<small>Evita aviso quando você só passa perto</small></div><span class="set-v mono">' + st.dwell + ' min' + I('chevR') + '</span></div>' +
      '<div class="set">' + I('moon') + '<div class="set-b">Horário silencioso</div><span class="set-v mono">22–07h</span></div>' +
      '<div class="set">' + I('layers') + '<div class="set-b">Agrupar avisos da chegada</div>' + tgA(st.group, 'setg:group') + '</div>' +
      U.sec('Privacidade') +
      '<div class="set">' + I('eyeoff') + '<div class="set-b">Guardar histórico de visitas</div>' + tgA(st.history, 'setg:history') + '</div>' +
      '<div class="set" data-act="go:perms">' + I('shield') + '<div class="set-b">Permissões</div><span class="set-v">' + I('chevR') + '</span></div>';
    return { body: b };
  };

  SCR.perms = () => {
    const card = (inner) => '<div class="card o" style="margin-top:10px">' + inner + '</div>';
    let b = U.top({ left: back_(), title: 'Permissões' }) + '<div class="h2">O que o Contexto consegue perceber</div><div class="sub">Cada permissão libera um tipo de lembrete. Nada sai do seu aparelho.</div>';
    b += S.locDenied
      ? card('<div class="set" style="padding:0;border:0">' + I('pin') + '<div class="set-b">Localização<small>Negada · chegadas e saídas pausadas</small></div>' + btnA('Permitir', 'p', 'allowloc', null, 'sm') + '</div>')
      : card('<div class="set" style="padding:0;border:0">' + I('pin') + '<div class="set-b">Localização<small>Sempre · chegadas e saídas ativas</small></div><span class="st ok">Ativa</span></div>');
    b += card('<div class="set" style="padding:0;border:0">' + I('bell') + '<div class="set-b">Notificações</div><span class="st ok">Ativas</span></div>');
    b += card('<div class="set" style="padding:0;border:0">' + I('bt') + '<div class="set-b">Bluetooth<small>Reconhece o carro</small></div><span class="st ok">Ativo</span></div>');
    b += card('<div class="set" style="padding:0;border:0">' + I('battery') + '<div class="set-b">Economia de bateria<small>' + (S.battFixed ? 'Contexto liberado' : 'Pode atrasar avisos em até 15 min') + '</small></div>' + (S.battFixed ? '<span class="st ok">Ok</span>' : btnA('Resolver', 's', 'battfix', null, 'sm')) + '</div>');
    b += card('<div class="set" style="padding:0;border:0">' + I('nfc') + '<div class="set-b">NFC<small>Este aparelho não tem NFC. Use QR Code.</small></div><span class="st na">—</span></div>');
    return { body: b };
  };

  /* ---------- Criação ---------- */
  const placeChips = (sel, pre) => {
    const keys = [S.here || 'casa', 'casa', 'mercado', 'trabalho'].filter((k, i, a) => a.indexOf(k) === i).slice(0, 3);
    if (keys.indexOf(sel) < 0) keys.unshift(sel);
    return '<div class="chips nowrap">' + keys.map((k) => chipA(esc(P(k).name), k === sel, P(k).icon, pre + ':' + k)).join('') + chipA('Outro', false, 'plus', 'pick', 'dash') + '</div>';
  };
  function taskSummary(d) {
    const pl = P(d.place);
    const w = { arrive: 'ao chegar ' + pl.em.replace('em Casa', 'em casa'), leave: 'ao sair ' + pl.de, time: 'às ' + d.time, none: '' }[d.when];
    const rep = { 'Uma vez': '', 'Diária': ', todo dia', 'Semanal': ', toda semana', 'Mensal': ', todo mês' }[d.rep];
    if (d.when === 'none') return 'Fica ' + pl.em + ', sem aviso. Aparece quando você abrir o lugar' + rep + '.';
    return 'Vamos lembrar você <b>' + w + '</b>' + rep + '.';
  }
  SCR.newtask = () => {
    const d = S.draft;
    let b = U.top({ left: back_('x'), title: 'Nova tarefa' }) +
      '<input class="t-in" id="f-title" placeholder="O que precisa ser feito?" value="' + esc(d.title) + '" autocomplete="off" maxlength="60">' +
      '<div class="fl"><div class="fl-l">Onde</div>' + placeChips(d.place, 'dplace') + '</div>' +
      '<div class="fl"><div class="fl-l">Quando lembrar</div><div class="opts">' +
      (P(d.place).geo === false
        ? optA({ icon: 'bt', t: 'Ao conectar ao carro', s: 'HB20 Multimídia', radio: d.when === 'arrive', act: 'dwhen:arrive' })
        : optA({ icon: 'arrive', t: 'Ao chegar', s: 'Depois de ' + S.settings.dwell + ' min no local', radio: d.when === 'arrive', act: 'dwhen:arrive' }) +
          optA({ icon: 'leave', t: 'Ao sair', s: 'Só se ainda estiver pendente', radio: d.when === 'leave', act: 'dwhen:leave' })) +
      optA({ icon: 'clock', t: 'Em um horário', s: d.when === 'time' ? null : null, radio: d.when === 'time', act: 'dwhen:time' }) +
      (d.when === 'time' ? '<div style="padding:0 0 12px 32px" class="chips">' + ['08:00', '12:00', '18:00', '20:00'].map((t) => chipA(t, d.time === t, null, 'dtime:' + t)).join('') + '</div>' : '') +
      optA({ icon: 'eyeoff', t: 'Sem aviso', s: 'Aparece só quando eu abrir o lugar', radio: d.when === 'none', act: 'dwhen:none' }) + '</div></div>' +
      '<div class="fl"><div class="fl-l">Repetir</div>' + segA([['Uma vez', 'Uma vez'], ['Diária', 'Diária'], ['Semanal', 'Semanal'], ['Mensal', 'Mensal']], d.rep, 'drep') + '</div>';
    return { body: b, foot: 150, footHTML: '<div class="foot line"><div class="summary">' + I('bell') + '<span id="f-sum">' + taskSummary(d) + '</span></div>' + btnA('Salvar tarefa', 'p', 'savetask', null, d.title.trim() ? '' : 'dis') + '</div>' };
  };

  SCR.newmem = () => {
    const d = S.draft, pl = P(d.place);
    let b = U.top({ left: back_('x'), title: 'Nova memória' }) + U.tag('Memória', 'mem', 'memory') +
      '<input class="t-in mem" id="f-title" placeholder="O que você quer lembrar lá?" value="' + esc(d.title) + '" autocomplete="off" maxlength="60">' +
      '<div class="fl"><div class="fl-l">Presa a</div>' +
      (pl.x != null
        ? '<div class="map-wrap" style="height:110px;border-radius:16px">' + mapHTML({ vb: vbAround(d.place, 150, 52), only: d.place, active: d.place, radius: { x: pl.x, y: pl.y, r: 22 }, noMe: true }) + '</div>'
        : '<div class="car-card">' + I('bt') + '<span>Aparece quando o celular conectar ao HB20.</span></div>') +
      '<div style="margin-top:10px">' + placeChips(d.place, 'dplace') + '</div></div>' +
      '<div class="fl"><div class="fl-l">Mostrar</div>' + segA([['always', 'Sempre que estiver lá'], ['once', 'Só na próxima']], d.show, 'dshow') + '</div>' +
      '<div class="fl"><div class="fl-l">Guardar junto<span class="fl-o">opcional</span></div><div class="chips">' + chipA('Nota', d.nota, 'note', 'datt:nota') + chipA('Campo para anotar lá', d.campo, 'edit', 'datt:campo') + '</div></div>';
    const sum = 'Aparece na tela Agora <b>' + (d.show === 'always' ? 'sempre que você estiver ' : 'na próxima vez que estiver ') + pl.em.replace('em Casa', 'em casa') + '</b>. Não tem prazo.';
    return { body: b, foot: 150, footHTML: '<div class="foot line"><div class="summary"><span style="color:var(--a-mem)">' + I('memory') + '</span><span>' + sum + '</span></div>' + btnA('Salvar memória', 'p', 'savemem', null, d.title.trim() ? '' : 'dis') + '</div>' };
  };

  SCR.pick = () => {
    let b = U.top({ left: back_(), title: 'Onde?' }) + '<div class="fld">' + I('search') + '<span class="v ph-t">Buscar lugar ou endereço</span></div>' + U.sec('Seus lugares') +
      S.order.map((k) => placeRow(k, { act: 'picked:' + k, noTrg: true })).join('') +
      U.sec('Qualquer lugar do tipo') + '<div class="pl" data-act="picked:farmacia"><span class="pl-ic">' + I('pill') + '</span><div class="pl-b"><b>Qualquer farmácia</b><small>Lembra na farmácia mais perto de você</small></div></div>';
    return { body: b };
  };

  const TT = [['arrive', 'Chegar'], ['leave', 'Sair'], ['clock', 'Horário'], ['car', 'Carro'], ['wifi', 'Wi-Fi'], ['bt', 'Bluetooth'], ['qr', 'QR Code'], ['nfc', 'NFC'], ['routine', 'Rotina']];
  const TDET = {
    arrive: null, leave: null,
    clock: ['07:30', '12:00', '18:00', '21:00'],
    car: ['HB20 · Multimídia'], wifi: ['Escritorio-5G', 'Casa_Fibra', 'Oficina_2G'], bt: ['Fone JBL Tune', 'Relógio Galaxy'], qr: ['Geladeira', 'Porta de casa'], routine: ['Cheguei em casa', 'Saindo para o trabalho']
  };
  const THEN = [['remind', 'bell', 'Mostrar um lembrete'], ['tasks', 'list', 'Mostrar tarefas de um lugar'], ['mem', 'memory', 'Mostrar uma memória'], ['ask', 'check', 'Perguntar se concluí'], ['list', 'layers', 'Abrir uma lista']];
  function trigWhenText(d) {
    if (d.type === 'arrive') return 'chegar ' + P(d.detail).em.replace('em Casa', 'em casa');
    if (d.type === 'leave') return 'sair ' + P(d.detail).de;
    if (d.type === 'clock') return 'forem ' + d.detail;
    if (d.type === 'car') return 'conectar ao HB20';
    if (d.type === 'wifi') return 'entrar no Wi-Fi ' + d.detail;
    if (d.type === 'bt') return 'conectar ao ' + d.detail;
    if (d.type === 'qr') return 'escanear o QR da ' + d.detail.toLowerCase();
    return 'a rotina "' + d.detail + '" rodar';
  }
  function trigThenText(d) {
    const pl = d.type === 'car' ? 'carro' : (d.type === 'arrive' || d.type === 'leave') ? d.detail : d.type === 'wifi' && d.detail === 'Escritorio-5G' ? 'trabalho' : 'casa';
    return { remind: 'mostrar um lembrete', tasks: 'mostrar tarefas ' + P(pl).de.replace('de casa', 'de Casa'), mem: 'mostrar uma memória', ask: 'perguntar se concluí', list: 'abrir uma lista' }[d.then];
  }
  const tk = (ic, t, empty) => '<span class="tk' + (empty ? ' empty' : '') + '">' + (ic ? I(ic) : '') + t + '</span>';
  SCR.newtrig = (s) => {
    const d = S.draft; d.step = s.step || 1;
    const sent = '<div class="sentence"><span class="k">Quando</span>' + tk(d.type, trigWhenText(d)) + '<br><span class="k">Então</span>' + (d.step === 2 ? tk(THEN.find((x) => x[0] === d.then)[1], trigThenText(d)) : tk(null, 'escolha a ação', true)) + '</div>';
    let b = U.top({ left: back_(d.step === 1 ? 'x' : 'chevL'), title: 'Novo gatilho', right: '<span class="dist">' + d.step + ' de 2</span>' }) + sent;
    if (d.step === 1) {
      b += U.sec('Quando…') + '<div class="tgrid" style="margin-top:8px">' + TT.map((x) => '<div class="tcell' + (x[0] === d.type ? ' on' : '') + (x[0] === 'nfc' ? ' dis' : '') + '"' + (x[0] === 'nfc' ? '' : ' data-act="ttype:' + x[0] + '"') + '>' + I(x[0]) + x[1] + (x[0] === 'nfc' ? '<small>Sem NFC</small>' : '') + '</div>').join('') + '</div>';
      const label = { arrive: 'Onde?', leave: 'De onde?', clock: 'Que horas?', car: 'Qual carro?', wifi: 'Qual rede?', bt: 'Qual dispositivo?', qr: 'Onde vai colar o QR?', routine: 'Qual rotina?' }[d.type];
      b += '<div class="fl"><div class="fl-l">' + label + '</div>';
      if (d.type === 'arrive' || d.type === 'leave') b += '<div class="chips">' + S.order.filter((k) => P(k).x != null).map((k) => chipA(esc(P(k).name), d.detail === k, P(k).icon, 'tdet:' + k)).join('') + '</div>';
      else if (d.type === 'clock') b += '<div class="chips">' + TDET.clock.map((t) => chipA(t, d.detail === t, null, 'tdet:' + t)).join('') + '</div>';
      else b += '<div class="opts">' + TDET[d.type].map((t, i) => optA({ icon: d.type === 'wifi' ? 'wifi' : d.type === 'qr' ? 'qr' : d.type === 'routine' ? 'routine' : 'bt', t: t, s: i === 0 && d.type !== 'routine' ? (d.type === 'wifi' ? 'Conectado agora' : d.type === 'qr' ? 'Vamos gerar o código para imprimir' : 'Pareado') : null, radio: d.detail === t, act: 'tdet:' + t })).join('') + '</div>';
      b += '</div>';
      return { body: b, foot: 110, footHTML: '<div class="foot">' + btnA('Continuar', 'p', 'tstep2') + '</div>' };
    }
    b += U.sec('Então…') + '<div class="opts" style="margin-top:8px">' + THEN.map((x) => optA({ icon: x[1], t: x[2], radio: d.then === x[0], act: 'tthen:' + x[0] })).join('') + '</div>' +
      U.sec('Condições') +
      '<div class="trg-li">' + I('calendar') + '<div class="b">Só em dias úteis</div>' + tgA(d.weekdays, 'tcond:weekdays') + '</div>' +
      '<div class="trg-li">' + I('repeat') + '<div class="b">No máximo 1 vez por dia</div>' + tgA(d.once, 'tcond:once') + '</div>';
    return { body: b, foot: 110, footHTML: '<div class="foot line">' + btnA('Criar gatilho', 'p', 'savetrig') + '</div>' };
  };

  SCR.newplace = () => {
    const d = S.draft;
    const spot = PIN_SPOTS[Math.min(S.order.length - 7, PIN_SPOTS.length - 1)];
    let body = '<div class="map-wrap" style="position:absolute;inset:0 0 380px 0">' + U.map({ vb: (spot[0] - 90) + ' ' + (spot[1] - 75) + ' 180 150', radius: { x: spot[0], y: spot[1], r: 36 }, pins: [{ x: spot[0], y: spot[1], ic: d.icon, active: true }] }) +
      '<div class="map-top"><span class="fab-s" data-act="back" aria-label="Fechar">' + I('x') + '</span></div></div>' +
      '<div class="sheet" style="padding-bottom:16px;box-shadow:none;border-top:1px solid var(--a-line);max-height:none">' +
      '<div class="fl-l">Nome</div><div class="fld"><input class="q-in" id="f-title" placeholder="Ex.: Casa da Ana" value="' + esc(d.title) + '" maxlength="30"></div>' +
      '<div class="chips nowrap" style="margin-top:12px">' + ['friends', 'tool', 'home', 'work', 'cart', 'pill', 'gym'].map((ic) => chipA('', d.icon === ic, ic, 'picon:' + ic)).join('') + '</div>' +
      '<div class="fl"><div class="fl-l">Raio<span class="fl-o mono">80 m</span></div><div style="height:22px;display:flex;align-items:center"><div style="flex:1;height:4px;border-radius:2px;background:linear-gradient(90deg,var(--a-accent) 32%,var(--a-surface-2) 32%);position:relative"><span style="position:absolute;left:32%;top:50%;width:22px;height:22px;margin:-11px;border-radius:50%;background:var(--a-bg);box-shadow:var(--a-shadow);border:1px solid var(--a-line)"></span></div></div></div>' +
      '<div class="fl"><div class="fl-l">Reconhecer também por<span class="fl-o">opcional</span></div><div class="chips">' + chipA('Wi-Fi', d.wifi, 'wifi', 'pwifi', d.wifi ? 'acc' : '') + chipA('QR Code', false, 'qr', 'noop') + '</div></div>' +
      '<div style="margin-top:16px">' + btnA('Salvar lugar', 'p', 'saveplace', null, d.title.trim() ? '' : 'dis') + '</div></div>';
    return { flush: true, body: body };
  };

  SCR.mem = (s) => {
    const it = S.items.find((x) => x.id === s.id), pl = it.place ? P(it.place) : null;
    let b = U.top({ left: back_() }) + U.tag('Memória' + (pl ? ' · ' + esc(pl.name) : ''), 'mem', 'memory') +
      '<div class="h1" style="margin-top:10px">' + esc(it.t) + '</div><div class="sub">' + (it.place === S.here ? 'Você está ' + pl.em + ' agora' : pl ? 'Aparece quando você estiver ' + pl.em : '') + '</div>';
    if (it.field) b += '<div class="cap" style="margin-top:22px"><div class="cap-l">' + esc(it.field) + '</div><div class="cap-v">' + (it.unit === 'R$' ? '<span class="u" style="margin:0 4px 0 0">R$</span>' : '') + '<input class="cap-in" id="f-cap" inputmode="decimal" placeholder="Toque para anotar" value="' + esc(it.value || '') + '">' + (it.unit && it.unit !== 'R$' ? '<span class="u">' + esc(it.unit) + '</span>' : '') + '</div></div>';
    if (it.note) b += '<div class="card" style="margin-top:12px;display:flex;gap:12px">' + I('note') + '<span style="font-size:15px;color:var(--a-ink-2)">' + esc(it.note) + '</span></div>';
    return { body: b, foot: 130, footHTML: '<div class="foot">' + btnA(it.field ? 'Salvar e arquivar' : 'Arquivar', 'p', 'memarch:' + it.id) + '<div class="btn btn-g" style="height:44px;margin-top:4px" data-act="memkeep:' + it.id + '">Manter para a próxima vez</div></div>' };
  };

  /* ---------- Sobreposições ---------- */
  function overlayHTML(anim) {
    const o = S.overlay; if (!o) return '';
    const a = anim ? ' a-up' : '', sc = '<div class="scrim' + (anim ? ' a-fade' : '') + '" data-act="close"></div>';
    if (o.n === 'add') {
      const here = S.here ? P(S.here) : null;
      return sc + '<div class="sheet' + a + '"><div class="grab"></div>' +
        (here ? '<div class="fld">' + I('pin') + '<input class="q-in" id="f-quick" placeholder="Anotar algo aqui ' + here.em + '…" enterkeyhint="done" maxlength="60" value="' + esc(S.quickDraft || '') + '"><span class="link" data-act="quick">Salvar</span></div>' : '') +
        '<div style="margin-top:10px">' +
        '<div class="add-opt" data-act="newtask"><span class="add-ic t">' + I('check') + '</span><div class="add-b"><b>Tarefa</b><small>Algo para fazer em um lugar</small></div>' + I('chevR') + '</div>' +
        '<div class="add-opt" data-act="newmem"><span class="add-ic m">' + I('memory') + '</span><div class="add-b"><b>Memória</b><small>Algo para ver ou anotar quando estiver lá</small></div>' + I('chevR') + '</div>' +
        '<div class="add-opt" data-act="newtrig"><span class="add-ic g">' + I('bolt') + '</span><div class="add-b"><b>Gatilho</b><small>Quando algo acontecer, o app age</small></div>' + I('chevR') + '</div>' +
        '<div class="add-opt" data-act="go:newplace"><span class="add-ic g">' + I('pin') + '</span><div class="add-b"><b>Lugar</b><small>Endereço, carro, Wi-Fi ou tag NFC</small></div>' + I('chevR') + '</div></div></div>';
    }
    if (o.n === 'item') {
      const it = S.items.find((x) => x.id === o.id), pl = it.place ? P(it.place) : null;
      return sc + '<div class="sheet' + a + '"><div class="grab"></div><div class="h2">' + esc(it.t) + '</div><div class="sub">' + (pl ? esc(pl.name) + ' · ' : '') + (WHEN[it.when] ? WHEN[it.when][1] : '') + (it.rep ? ' · ' + it.rep : '') + '</div>' +
        '<div style="margin-top:16px">' + btnA(it.done ? 'Marcar como pendente' : 'Concluir', 'p', 'toggle:' + it.id, 'check') + '</div>' +
        (it.done ? '' : '<div class="fl-l" style="margin-top:20px">Adiar</div><div class="opts">' +
          (pl ? optA({ icon: 'repeat', t: 'Na próxima vez que eu vier aqui', s: 'Some até você voltar ' + pl.em.replace('em Casa', 'para casa'), act: 'snooze:' + it.id }) : '') +
          optA({ icon: 'clock', t: 'Daqui a 1 hora', act: 'snoozeh:' + it.id }) +
          (it.place !== 'casa' ? optA({ icon: 'home', t: 'Quando eu chegar em Casa', s: 'Muda o lugar da tarefa', act: 'move:' + it.id + ':casa' }) : optA({ icon: 'work', t: 'Quando eu chegar no Trabalho', s: 'Muda o lugar da tarefa', act: 'move:' + it.id + ':trabalho' })) + '</div>') +
        '<div class="btn btn-g" style="height:44px;margin-top:8px;color:var(--a-late)" data-act="archive:' + it.id + '">Não preciso mais</div></div>';
    }
    if (o.n === 'leave') {
      const p = S.lastLeft, pend = S.items.filter((it) => it.place === p && it.kind === 'task' && !it.archived && !it.snoozed && (!it.done || it.doneVisit === S.visit));
      return sc + '<div class="sheet' + a + '"><div class="grab"></div><div style="display:flex;gap:14px;align-items:center"><span class="add-ic t">' + I('leave') + '</span><div><div class="h2">Saindo ' + P(p).de + '?</div><div class="sub" style="margin-top:2px">Antes de ir, ainda falta:</div></div></div>' +
        '<div style="margin-top:10px">' + pend.map((it) => '<div class="row' + (it.done ? ' done' : '') + '"><span class="row-lead"><span class="chk' + (it.done ? ' on' : '') + '">' + (it.done ? I('check') : '') + '</span></span><div class="row-b"><div class="row-t">' + esc(it.t) + '</div></div><div class="row-trail">' + (it.done ? '' : btnA('Peguei', 's', 'toggle:' + it.id, null, 'sm')) + '</div></div>').join('') + '</div>' +
        '<div class="btn-row" style="margin-top:14px">' + btnA('Próxima vez', 'o', 'nnext') + btnA('Peguei tudo', 'p', 'allgot') + '</div>' +
        '<div class="btn btn-g" style="height:40px;margin-top:4px" data-act="stillhere">Ainda estou aqui</div></div>';
    }
    return '';
  }

  function notifHTML(n, heads, anim) {
    return '<div class="nt-card' + (heads ? ' heads' : '') + (anim ? ' a-down' : '') + '" data-act="nopen"><div class="nt-hd"><span class="app-i">' + U.logo(14, '#fff', '#fff') + '</span>Contexto' + (n.sub ? ' · ' + esc(n.sub) : '') + '<span class="tm">agora</span></div>' +
      '<div class="nt-ti">' + esc(n.title) + '</div><div class="nt-bd">' + esc(n.body) + '</div>' +
      '<div class="nt-ac">' + n.actions.map((x, i) => '<span' + (i === 0 ? ' class="p"' : '') + ' data-act="nact:' + i + '">' + x[0] + '</span>').join('') + '</div></div>';
  }

  /* =========================================================
     RENDER
     ========================================================= */
  let lastKey = '', lastOverlay = null, lastNotif = null;
  function render(dir) {
    const key = JSON.stringify(S.screen);
    const pbOld = phoneEl.querySelector('.pb');
    const keepScroll = key === lastKey && pbOld ? pbOld.scrollTop : 0;
    const active = document.activeElement;
    const focusId = active && phoneEl.contains(active) ? active.id : null;
    let html;
    if (S.locked) {
      html = '<div class="ph proto"><div class="lock"><div class="lock-t"><div class="d">Sexta-feira, 24 de setembro</div><div class="h">' + clock() + '</div></div>' +
        (S.notif ? '<div class="nstack" style="top:250px">' + notifHTML(S.notif, false, S.notif !== lastNotif) + '</div>' : '') +
        '<div class="lock-hint" data-act="unlock">Toque para desbloquear</div></div>' + U.statusBar(clock(), true) + '<div class="hi inv"></div></div>';
    } else {
      const f = SCR[S.screen.n](S.screen);
      const anim = dir === 'in' ? ' a-in' : dir === 'back' ? ' a-back' : dir === 'fade' ? ' a-fade' : '';
      html = '<div class="ph proto' + (f.tab ? ' has-tab' : '') + '">' + (f.raw ? '<div class="' + anim.trim() + '" style="position:absolute;inset:0">' + f.raw + '</div>' : '') + U.statusBar(clock(), f.inv) +
        (f.body != null ? '<div class="pb' + (f.flush ? ' flush' : '') + anim + '"' + ' style="' + (f.foot ? 'bottom:' + f.foot + 'px;' : '') + (f.flushTop ? 'top:0;' : '') + '"' + '>' + f.body + '</div>' : '') +
        (f.footHTML || '') + (f.tab ? tabBar(f.tab) : '') + overlayHTML(S.overlay && (!lastOverlay || lastOverlay.n !== S.overlay.n)) +
        (S.notif ? notifHTML(S.notif, true, S.notif !== lastNotif) : '') +
        (S.toast ? '<div class="toast a-fade' + (f.tab ? '' : ' no-tab') + '">' + I('check') + '<span>' + S.toast.t + '</span>' + (S.toast.undo ? '<b data-act="undo">Desfazer</b>' : '') + '</div>' : '') +
        '<div class="hi' + (f.inv ? ' inv' : '') + '"></div></div>';
    }
    phoneEl.innerHTML = html;
    lastKey = key; lastOverlay = S.overlay; lastNotif = S.notif;
    const pb = phoneEl.querySelector('.pb');
    if (pb && keepScroll) pb.scrollTop = keepScroll;
    if (focusId) { const el = document.getElementById(focusId); if (el) { el.focus(); const v = el.value; el.value = ''; el.value = v; } }
    renderSim();
  }

  /* =========================================================
     AÇÕES
     ========================================================= */
  function newDraft(kind, place) {
    const p = place || S.here || 'mercado';
    if (kind === 'task') S.draft = { kind: 'task', title: '', place: p, when: P(p).geo === false ? 'arrive' : 'arrive', rep: 'Uma vez', time: '18:00' };
    if (kind === 'mem') S.draft = { kind: 'mem', title: '', place: p, show: 'always', nota: false, campo: true };
    if (kind === 'trig') S.draft = { kind: 'trig', step: 1, type: place ? 'arrive' : 'car', detail: place && P(place).x != null ? place : 'HB20 · Multimídia', then: 'tasks', weekdays: false, once: true };
    if (kind === 'place') S.draft = { kind: 'place', title: '', icon: 'friends', wifi: false };
    if (kind === 'trig' && place && P(place).geo === false) { S.draft.type = 'car'; S.draft.detail = 'HB20 · Multimídia'; }
  }
  function readTitle() { const el = document.getElementById('f-title'); if (el && S.draft) S.draft.title = el.value; }
  function complete(it, viaNotif) {
    it.done = true; it.doneVisit = S.visit;
    if (!viaNotif) S.tour.done = true;
  }

  function arrive(p) {
    if (S.here) return;
    S.min += 14; S.uncertain = false;
    const pl = P(p);
    addLog('Cerca virtual: entrou ' + pl.em.replace('em Casa', 'em Casa'));
    if (S.locDenied && !pl.wifi) { addLog('Localização negada: a chegada não foi percebida'); render('none'); return; }
    addLog(pl.wifi ? 'Wi-Fi ' + pl.wifi + ' conectado · confirmado na hora' : 'Ficou ' + S.settings.dwell + ' min no local · chegada confirmada');
    S.here = p; S.lastLeft = null; S.visit++; S.justArrived = true;
    S.items.forEach((it) => { if (it.place === p && it.snoozed) it.snoozed = false; });
    if (p === 'mercado') S.tour.arrived = true;
    if (!S.locked) { S.stack = []; S.tab = 'agora'; S.screen = { n: 'agora' }; S.overlay = null; }
    const pend = pendingAt(p);
    const r1 = S.routines.find((r) => r.id === 'r1');
    if (p === 'casa' && r1.on) { S.run = { id: 'r1', step: 0 }; addLog('Rotina "Cheguei em casa" iniciada'); }
    if (!pl.trig.arrive) { addLog('Aviso de chegada desligado neste lugar'); render('fade'); return; }
    if (!pend.length) { addLog('Nada pendente aqui · sem notificação'); render('fade'); return; }
    addLog('1 notificação com ' + pend.length + (pend.length > 1 ? ' itens' : ' item'));
    S.notif = { kind: 'arrive', sub: pl.name, title: 'Você chegou ' + pl.ao + '.', body: (pend.length === 1 ? pend[0].t + '.' : pend.length + ' coisas estão esperando por você aqui.') + (S.run && p === 'casa' ? ' A rotina "Cheguei em casa" começou.' : ''), open: 'nview', actions: [['Ver', 'nview'], ['Mais tarde', 'ndismiss']] };
    render('fade');
  }
  function leave() {
    const p = S.here; if (!p) return;
    S.min += 3; const pl = P(p);
    addLog('Saiu da cerca ' + pl.de + (pl.wifi ? ' · Wi-Fi desconectado' : ''));
    S.here = null; S.lastLeft = p; S.justArrived = false; S.run = null;
    if (!S.locked) { S.stack = []; S.tab = 'agora'; S.screen = { n: 'agora' }; S.overlay = null; }
    const pend = pendingAt(p, 'task');
    if (p === 'casa' && pend.length) S.tour.leftPending = true;
    if (!pl.trig.leave || !pend.length) { addLog(pend.length ? 'Aviso de saída desligado' : 'Nada pendente · sem aviso'); render('fade'); return; }
    if (p === 'mercado') S.tour.leftMercado = true;
    addLog('Aviso de saída: ' + pend.length + (pend.length > 1 ? ' pendências' : ' pendência'));
    S.notif = pend.length === 1
      ? { kind: 'leave', open: 'nleave', sub: pl.name, title: 'Você está saindo ' + pl.de + '.', body: 'Você ainda precisa: ' + pend[0].t.charAt(0).toLowerCase() + pend[0].t.slice(1) + '.', actions: [['Peguei', 'ndone'], ['Próxima vez', 'nnext']] }
      : { kind: 'leave', open: 'nleave', sub: pl.name, title: 'Você está saindo ' + pl.de + '.', body: 'Ainda faltam ' + pend.length + ' coisas: ' + pend.slice(0, 2).map((x) => x.t.toLowerCase()).join(', ') + (pend.length > 2 ? '…' : '.'), actions: [['Ver', 'nleave'], ['Próxima vez', 'nnext']] };
    render('fade');
  }
  function carConnect() {
    S.min += 1; addLog('Bluetooth: HB20 conectado'); S.tour.car = true;
    const pend = pendingAt('carro');
    if (!P('carro').trig.arrive || !pend.length) { addLog('Nada pendente no Carro · sem aviso'); render('none'); return; }
    S.notif = { kind: 'car', open: 'place:carro', sub: 'Carro', title: 'Carro conectado', body: pend.map((x) => x.t).join(' · ') + '.', actions: [['Ver', 'place:carro'], ['OK', 'ndismiss']] };
    addLog('1 notificação: ' + pend.length + (pend.length > 1 ? ' tarefas' : ' tarefa') + ' do Carro');
    render('none');
  }

  const RESOLVE_IN_PLACE = { ndone: 1, nnext: 1, ndismiss: 1 };
  function act(str, el) {
    const a = str.split(':'), n = a[0];
    const item = (id) => S.items.find((x) => x.id === Number(id));
    switch (n) {
      case 'noop': return;
      case 'obnext': S.screen = S.screen.k < 3 ? { n: 'ob', k: S.screen.k + 1 } : { n: 'perm' }; return render('in');
      case 'obskip': S.screen = { n: 'perm' }; return render('in');
      case 'permit': S.locDenied = a[1] === '0'; S.screen = { n: 'setup' }; addLog(S.locDenied ? 'Localização: não permitida' : 'Localização: permitida (sempre)'); return render('in');
      case 'setupsel': S.setupSel = S.setupSel || { casa: 1, trabalho: 1, mercado: 1, carro: 1, academia: 0 }; S.setupSel[a[1]] = S.setupSel[a[1]] ? 0 : 1; return render('none');
      case 'setupdone': S.screen = { n: 'agora' }; S.tab = 'agora'; if (!S.locDenied) { S.here = 'casa'; addLog('Detectado em Casa pela localização'); } return render('fade');
      case 'tab': return tab(a[1]);
      case 'back': return back();
      case 'go': if (a[1] === 'newplace') newDraft('place'); return go(a[1], a[2] ? { id: a[2] } : null);
      case 'place': S.notif = null; S.locked = false; S.pinSel = null; return go('place', { id: a[1] });
      case 'pin': S.pinSel = a[1]; return render('none');
      case 'pinclose': S.pinSel = null; return render('none');
      case 'addsheet': S.overlay = { n: 'add' }; return render('none');
      case 'close': S.overlay = null; return render('none');
      case 'toggle': {
        const it = item(a[1]);
        if (it.done) { it.done = false; S.overlay = S.overlay && S.overlay.n === 'item' ? null : S.overlay; toast('Voltou a ficar pendente.'); return render('none'); }
        complete(it);
        if (S.overlay && S.overlay.n === 'item') S.overlay = null;
        const left = it.place ? pendingAt(it.place, 'task').length : 0;
        S.lastToggle = it.id;
        toast(it.place && it.place === S.here ? (left ? 'Feito. ' + (left > 1 ? 'Faltam ' + left : 'Falta 1') + ' aqui.' : 'Tudo feito aqui.') : 'Concluída.', true);
        return render('none');
      }
      case 'undo': { const it = item(S.lastToggle); if (it) it.done = false; S.toast = null; return render('none'); }
      case 'item': S.overlay = { n: 'item', id: Number(a[1]) }; return render('none');
      case 'mem': return go('mem', { id: Number(a[1]) });
      case 'snooze': { const it = item(a[1]); it.snoozed = true; S.overlay = null; toast('Volta na próxima vez que você vier aqui.'); addLog('"' + it.t + '" adiada para a próxima visita'); return render('none'); }
      case 'snoozeh': { const it = item(a[1]); S.overlay = null; toast('Vamos lembrar às ' + (Math.floor((S.min + 60) / 60) % 24) + ':' + String(S.min % 60).padStart(2, '0') + '.'); it.time = 'às ' + (Math.floor((S.min + 60) / 60) % 24) + ':' + String(S.min % 60).padStart(2, '0'); it.snoozed = true; return render('none'); }
      case 'move': { const it = item(a[1]); it.place = a[2]; S.overlay = null; toast('Movida para ' + P(a[2]).name + '.'); return render('none'); }
      case 'archive': { const it = item(a[1]); it.archived = true; S.overlay = null; toast('Arquivada.'); return render('none'); }
      case 'memarch': { const it = item(a[1]); const c = document.getElementById('f-cap'); if (c) it.value = c.value; it.done = true; it.doneVisit = S.visit; back(); toast(it.value ? 'Valor salvo: ' + esc(it.value) + (it.unit && it.unit !== 'R$' ? ' ' + it.unit : '') + '. Memória arquivada.' : 'Memória arquivada.'); return render('none'); }
      case 'memkeep': { const it = item(a[1]); const c = document.getElementById('f-cap'); if (c) it.value = c.value; back(); if (it.value) toast('Valor salvo. A memória continua aqui.'); return render('none'); }
      case 'nothere': addLog('Usuário corrigiu: não está ' + P(S.here).em); S.lastLeft = S.here; S.here = null; S.justArrived = false; S.run = null; return render('none');
      case 'sethere': S.here = a[1]; S.uncertain = false; S.visit++; S.justArrived = false; S.lastLeft = null; addLog('Lugar escolhido manualmente: ' + P(a[1]).name); return render('fade');
      case 'notuncertain': S.uncertain = false; addLog('Usuário: não está no supermercado'); return render('none');
      case 'runnext': { const r = S.routines.find((x) => x.id === S.run.id); if (S.run.step < r.steps.length - 1) { S.run.step++; } else { S.run = null; toast('Rotina concluída.'); } return render('none'); }
      /* notificações */
      case 'nact': {
        const n0 = S.notif; if (!n0) return;
        const target = n0.actions[Number(a[1])][1];
        S.notif = null;
        if (!RESOLVE_IN_PLACE[target]) S.locked = false;
        return act(target);
      }
      case 'nopen': { const n0 = S.notif; if (!n0) return; S.notif = null; S.locked = false; return act(n0.open); }
      case 'nview': S.stack = []; S.tab = 'agora'; S.screen = { n: 'agora' }; return render('fade');
      case 'ndismiss': return render('none');
      case 'ndone': { const p = S.lastLeft; const it = pendingAt(p, 'task')[0]; if (it) complete(it, true); toast('Concluída sem abrir o app.'); addLog('"' + (it ? it.t : '') + '" concluída pela notificação'); return render('none'); }
      case 'nnext': { const p = S.lastLeft; S.items.forEach((it) => { if (it.place === p && it.kind === 'task' && alive(it)) it.snoozed = true; }); S.overlay = null; toast('Fica para a próxima vez que você for ' + (p === 'casa' ? 'para casa' : P(p).ao) + '.'); addLog('Pendências adiadas para a próxima visita'); return render('none'); }
      case 'nleave': S.stack = []; S.tab = 'agora'; S.screen = { n: 'agora' }; S.overlay = { n: 'leave' }; return render('fade');
      case 'allgot': { const p = S.lastLeft; S.items.forEach((it) => { if (it.place === p && it.kind === 'task' && alive(it)) complete(it); }); S.overlay = null; toast('Tudo feito.'); return render('none'); }
      case 'stillhere': S.here = S.lastLeft; S.lastLeft = null; S.overlay = null; addLog('Saída cancelada pelo usuário · raio recalibrado'); return render('none');
      case 'unlock': S.locked = false; S.notif = null; return render('fade');
      /* criação */
      case 'newtask': newDraft('task', a[1]); S.pickFor = 'task'; return go('newtask');
      case 'newmem': newDraft('mem', a[1]); S.pickFor = 'mem'; return go('newmem');
      case 'newtrig': newDraft('trig', a[1]); return go('newtrig');
      case 'dplace': readTitle(); S.draft.place = a[1]; if (P(a[1]).geo === false && S.draft.when === 'leave') S.draft.when = 'arrive'; return render('none');
      case 'dwhen': readTitle(); S.draft.when = a[1]; return render('none');
      case 'dtime': readTitle(); S.draft.time = a[1] + ':' + a[2]; return render('none');
      case 'drep': readTitle(); S.draft.rep = a[1]; return render('none');
      case 'dshow': readTitle(); S.draft.show = a[1]; return render('none');
      case 'datt': readTitle(); S.draft[a[1]] = !S.draft[a[1]]; return render('none');
      case 'pick': readTitle(); return go('pick');
      case 'picked': S.draft.place = a[1]; return back();
      case 'savetask': {
        readTitle(); const d = S.draft; if (!d.title.trim()) return;
        S.items.push({ id: S.nextId++, kind: 'task', t: d.title.trim(), place: d.place, when: d.when, rep: d.rep, time: d.when === 'time' ? d.time : null });
        if (d.place === 'mercado') S.tour.created = true;
        addLog('Nova tarefa ' + P(d.place).em + ': "' + d.title.trim() + '"');
        S.screen = S.stack.pop() || { n: S.tab }; while (S.screen.n === 'pick') S.screen = S.stack.pop() || { n: S.tab };
        toast('Tarefa salva ' + P(d.place).em + '.'); return render('back');
      }
      case 'savemem': {
        readTitle(); const d = S.draft; if (!d.title.trim()) return;
        S.items.push({ id: S.nextId++, kind: 'mem', t: d.title.trim(), place: d.place, field: d.campo ? 'Valor anotado' : null, unit: '', once: d.show === 'once' });
        addLog('Nova memória ' + P(d.place).em + ': "' + d.title.trim() + '"');
        S.screen = S.stack.pop() || { n: S.tab }; while (S.screen.n === 'pick') S.screen = S.stack.pop() || { n: S.tab };
        toast('Memória salva ' + P(d.place).em + '.'); return render('back');
      }
      case 'quick': {
        const q = document.getElementById('f-quick'); if (!q || !q.value.trim() || !S.here) return;
        S.items.push({ id: S.nextId++, kind: 'mem', t: q.value.trim(), place: S.here });
        addLog('Captura rápida ' + P(S.here).em + ': "' + q.value.trim() + '"');
        S.overlay = null; S.quickDraft = ''; toast('Memória salva ' + P(S.here).em + '.'); return render('none');
      }
      case 'ttype': { const d = S.draft; d.type = a[1]; d.detail = (a[1] === 'arrive' || a[1] === 'leave') ? (S.here || 'casa') : TDET[a[1]][0]; return render('none'); }
      case 'tdet': S.draft.detail = a.slice(1).join(':'); return render('none');
      case 'tstep2': return go('newtrig', { step: 2 });
      case 'tthen': S.draft.then = a[1]; return render('none');
      case 'tcond': S.draft[a[1]] = !S.draft[a[1]]; return render('none');
      case 'savetrig': {
        const d = S.draft;
        const place = d.type === 'car' ? 'carro' : (d.type === 'arrive' || d.type === 'leave') ? d.detail : d.type === 'wifi' && d.detail === 'Escritorio-5G' ? 'trabalho' : d.type === 'qr' || d.detail === 'Casa_Fibra' ? 'casa' : null;
        const label = trigWhenText(d);
        S.triggers.push({ id: S.nextId++, place: place, icon: d.type, label: label.charAt(0).toUpperCase() + label.slice(1), then: trigThenText(d).charAt(0).toUpperCase() + trigThenText(d).slice(1), on: true });
        addLog('Novo gatilho: quando ' + label + ', ' + trigThenText(d));
        S.stack = S.stack.filter((s) => s.n !== 'newtrig'); S.screen = S.stack.pop() || { n: S.tab };
        toast('Gatilho criado' + (place ? ' ' + P(place).em : '') + '.'); return render('back');
      }
      case 'ptrig': { const pl = P(a[1]); pl.trig[a[2]] = !pl.trig[a[2]]; toast((a[2] === 'arrive' ? 'Aviso ao chegar ' : 'Aviso ao sair ') + (pl.trig[a[2]] ? 'ligado.' : 'desligado.')); return render('none'); }
      case 'ctrig': { const t = S.triggers.find((x) => x.id === Number(a[1])); t.on = !t.on; return render('none'); }
      case 'picon': readTitle(); S.draft.icon = a[1]; return render('none');
      case 'pwifi': readTitle(); S.draft.wifi = !S.draft.wifi; return render('none');
      case 'saveplace': {
        readTitle(); const d = S.draft; if (!d.title.trim()) return;
        const spot = PIN_SPOTS[Math.min(S.order.length - 7, PIN_SPOTS.length - 1)], id = 'p' + S.nextId++;
        const nm = d.title.trim();
        S.places[id] = { name: nm, icon: d.icon, sub: 'Endereço marcado no mapa', x: spot[0], y: spot[1], em: 'em ' + nm, de: 'de ' + nm, ao: 'em ' + nm, trig: { arrive: true, leave: true }, wifi: d.wifi ? 'Rede_' + nm.split(' ')[0] : null };
        S.order.push(id); addLog('Novo lugar: ' + nm);
        S.screen = { n: 'place', id: id }; toast('Lugar salvo. Adicione o que lembrar lá.'); return render('in');
      }
      /* lista, rotinas, ajustes */
      case 'lseg': S.listSeg = a[1]; return render('none');
      case 'lfilter': S.listFilter = a[1]; return render('none');
      case 'rtog': { const r = S.routines.find((x) => x.id === a[1]); r.on = !r.on; toast('Rotina ' + (r.on ? 'ativada.' : 'pausada.')); return render('none'); }
      case 'model': {
        const M = { m1: ['Fim do expediente', 'leave', 'Ao sair do Trabalho · dias úteis', [['Lembrar', 'Pegar documento no RH', ''], ['Mostrar tarefas', 'Tarefas de Casa', '']]], m2: ['Mercado da semana', 'arrive', 'Ao chegar no Supermercado · sábados', [['Abrir lista', 'Compras da semana', '']]], m3: ['Viagem', 'clock', 'Na véspera · 20:00', [['Lembrar', 'Documentos', ''], ['Lembrar', 'Carregador', '']]] }[a[1]];
        S.routines.push({ id: a[1], name: M[0], on: false, icon: M[1], w: M[2], steps: M[3] }); toast('Modelo adicionado. Ative quando quiser.'); return render('none');
      }
      case 'addstep': { const r = S.routines.find((x) => x.id === a[1]); r.steps.push(['Lembrar', 'Novo lembrete', 'Toque para editar']); return render('none'); }
      case 'testrun': { const r = S.routines.find((x) => x.id === a[1]); S.run = { id: r.id, step: 0 }; S.stack = []; S.tab = 'agora'; S.screen = { n: 'agora' }; addLog('Teste da rotina "' + r.name + '"'); if (!S.here) { S.here = 'casa'; S.lastLeft = null; } toast('Rotina iniciada. Veja o passo 1.'); return render('fade'); }
      case 'setg': S.settings[a[1]] = !S.settings[a[1]]; return render('none');
      case 'dwell': S.settings.dwell = { 1: 2, 2: 5, 5: 1 }[S.settings.dwell]; return render('none');
      case 'allowloc': S.locDenied = false; addLog('Localização permitida'); if (!S.here) { S.here = 'casa'; addLog('Detectado em Casa'); } toast('Localização ativa.'); return render('none');
      case 'battfix': S.battFixed = true; toast('Avisos sem atraso.'); return render('none');
    }
  }

  phoneEl.addEventListener('click', (e) => {
    const t = e.target.closest('[data-act]');
    if (!t || !phoneEl.contains(t)) return;
    e.preventDefault();
    act(t.getAttribute('data-act'), t);
  });
  phoneEl.addEventListener('input', (e) => {
    if (e.target.id === 'f-cap') { const it = S.items.find((x) => x.id === S.screen.id); if (it) it.value = e.target.value; return; }
    if (e.target.id === 'f-quick') { S.quickDraft = e.target.value; return; }
    if (e.target.id !== 'f-title' || !S.draft) return;
    S.draft.title = e.target.value;
    const btn = phoneEl.querySelector('[data-act="savetask"],[data-act="savemem"],[data-act="saveplace"]');
    if (btn) btn.classList.toggle('dis', !e.target.value.trim());
  });
  phoneEl.addEventListener('keydown', (e) => {
    if (e.key !== 'Enter') return;
    if (e.target.id === 'f-quick') { e.preventDefault(); act('quick'); }
    if (e.target.id === 'f-title') { e.preventDefault(); const b = phoneEl.querySelector('[data-act^="save"]'); if (b && !b.classList.contains('dis')) act(b.getAttribute('data-act')); }
  });

  /* =========================================================
     SIMULADOR
     ========================================================= */
  const TOUR = [
    ['created', 'Toque em <b>+</b> e crie a tarefa <b>"Comprar café"</b> no Supermercado.'],
    ['leftPending', 'No simulador, <b>saia de Casa</b> e veja o aviso de saída.'],
    ['arrived', '<b>Chegue ao Supermercado</b> e toque em "Ver" na notificação.'],
    ['done', 'Conclua um item tocando no círculo.'],
    ['leftMercado', '<b>Saia do mercado</b> sem terminar tudo.'],
    ['car', '<b>Conecte ao carro</b>.'],
    ['explore', 'Explore <b>Lugares</b>, <b>Lista</b> e <b>Rotinas</b>.']
  ];
  function renderSim() {
    const intro = ['splash', 'ob', 'perm', 'setup'].indexOf(S.screen.n) >= 0;
    S.tour.explore = S.tour.x_lugares && S.tour.x_lista && S.tour.x_rotinas;
    const cur = TOUR.findIndex((t) => !S.tour[t[0]]);
    const where = S.here ? P(S.here).name : S.uncertain ? 'Sinal fraco perto do Supermercado' : 'Em trânsito';
    simEl.innerHTML =
      '<div class="sim-box"><h2>Simulador de contexto</h2><p>No app real, estes eventos vêm do GPS, do Wi-Fi e do Bluetooth. Aqui você dispara cada um e vê como o app reage.</p>' +
      '<div class="now-line"><span class="now-dot' + (S.here ? '' : ' off') + '"></span><span>Você está: <b>' + esc(where) + '</b></span><span class="mono" style="margin-left:auto;color:var(--d-ink-3);font-size:13px">' + clock() + '</span></div>' +
      '<h3>Movimento</h3><div class="sbtns">' +
      '<button class="sb-btn pri" data-sim="leave" ' + (!S.here || intro ? 'disabled' : '') + '>' + I('leave') + (S.here ? 'Sair ' + P(S.here).de : 'Sair do lugar') + '</button></div>' +
      '<div class="sbtns" style="margin-top:8px">' + S.order.filter((k) => P(k).x != null).map((k) => '<button class="sb-btn" data-sim="arrive:' + k + '" ' + (S.here || intro ? 'disabled' : '') + '>' + I('arrive') + esc(P(k).name) + '</button>').join('') + '</div>' +
      (S.here && !intro ? '<p style="font-size:13px">Para chegar a outro lugar, saia do atual primeiro.</p>' : '') +
      '<h3>Outros sinais</h3><div class="sbtns">' +
      '<button class="sb-btn" data-sim="car" ' + (intro ? 'disabled' : '') + '>' + I('car') + 'Conectar ao carro</button>' +
      '<button class="sb-btn" data-sim="weak" ' + (S.here || intro || S.locDenied ? 'disabled' : '') + '>' + I('signal') + 'Sinal fraco perto do mercado</button></div>' +
      '<h3>Aparelho</h3>' +
      '<div class="sw-row"><span>Celular bloqueado<small>Os avisos aparecem na tela de bloqueio</small></span><button class="sw" role="switch" aria-checked="' + S.locked + '" data-sim="lock" aria-label="Celular bloqueado" ' + (intro ? 'disabled' : '') + '></button></div>' +
      '<div class="sw-row"><span>Localização negada<small>O app continua útil com Wi-Fi, carro e escolha manual</small></span><button class="sw" role="switch" aria-checked="' + S.locDenied + '" data-sim="deny" aria-label="Localização negada" ' + (intro ? 'disabled' : '') + '></button></div>' +
      '</div>' +
      '<div class="sim-box"><h2>O que o app percebeu</h2><ul class="log" style="margin-top:12px">' + (S.log.length ? S.log.map((l) => '<li><time>' + l.at + '</time><span>' + esc(l.t) + '</span></li>').join('') : '<li><span class="empty-log">Nenhum evento ainda.</span></li>') + '</ul></div>' +
      '<div class="sim-box"><h2>Roteiro sugerido</h2><p>Um caminho de 2 minutos pelas partes mais importantes.</p><ol class="tour" style="margin-top:14px">' +
      TOUR.map((t, i) => '<li class="' + (S.tour[t[0]] ? 'ok' : i === cur ? 'cur' : '') + '">' + t[1] + '</li>').join('') + '</ol>' +
      '<div class="sim-foot" style="margin-top:18px"><button class="sb-btn" data-sim="restart">' + I('repeat') + 'Recomeçar com a abertura</button><button class="sb-btn" data-sim="skip">' + I('arrowR') + 'Recomeçar direto no app</button></div></div>';
  }
  simEl.addEventListener('click', (e) => {
    const b = e.target.closest('[data-sim]'); if (!b || b.disabled) return;
    const [n, arg] = b.getAttribute('data-sim').split(':');
    if (n === 'leave') leave();
    else if (n === 'arrive') arrive(arg);
    else if (n === 'car') carConnect();
    else if (n === 'weak') { S.uncertain = true; S.min += 6; addLog('GPS com precisão de ±140 m · talvez Supermercado'); S.stack = []; S.tab = 'agora'; S.screen = { n: 'agora' }; S.overlay = null; render('fade'); }
    else if (n === 'lock') { S.locked = !S.locked; if (!S.locked) S.notif = null; render('fade'); }
    else if (n === 'deny') {
      S.locDenied = !S.locDenied;
      addLog(S.locDenied ? 'Permissão de localização removida' : 'Localização permitida');
      if (S.locDenied && S.here && !P(S.here).wifi) { S.lastLeft = S.here; S.here = null; S.run = null; }
      render('none');
    }
    else if (n === 'restart') { S = null; reset(false); }
    else if (n === 'skip') { S = null; reset(true); }
  });

  reset(false);
})();
