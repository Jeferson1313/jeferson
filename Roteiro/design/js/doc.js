/* =========================================================
   Roteiro — monta a documentação a partir dos componentes
   ========================================================= */
(function () {
  const U = window.UI, I = U.I, S = window.SCREENS;
  const $ = (id) => document.getElementById(id);
  const ref = (id) => '<a class="ref" href="#' + id + '">' + id + '</a>';
  const arrow = '<svg class="ar" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12h14M13.5 6.5 19 12l-5.5 5.5"/></svg>';

  /* ---------- Fluxogramas ---------- */
  /* passo: "texto" | ["texto","T07"] | {sys:"..."} | {dec:"..."} | {end:"..."} | {br:[["rótulo", [passos]]]} */
  function node(p) {
    if (typeof p === 'string') return '<span class="nd">' + p + '</span>';
    if (Array.isArray(p)) return '<span class="nd">' + ref(p[1]) + p[0] + '</span>';
    if (p.sys) return '<span class="nd sys">' + p.sys + '</span>';
    if (p.dec) return '<span class="nd dec">' + p.dec + '</span>';
    if (p.end) return '<span class="nd end">' + (p.ref ? ref(p.ref) : '') + p.end + '</span>';
    return '';
  }
  function chain(steps) {
    let h = '', parts = [];
    steps.forEach(function (p) {
      if (p && p.br) {
        if (parts.length) { h += '<div class="fl-row">' + parts.join(arrow) + arrow + '</div>'; parts = []; }
        h += '<div class="branches">' + p.br.map((b) => '<div class="br"><span class="br-l">' + b[0] + '</span>' + b[1].map(node).join(arrow) + '</div>').join('') + '</div>';
      } else parts.push(node(p));
    });
    if (parts.length) h += '<div class="fl-row">' + parts.join(arrow) + '</div>';
    return h;
  }
  const FLOWS = {
    'flow-main': [
      ['Agora', 'T07'], ['Toca em +', 'T24'], { dec: 'O que criar?' },
      { br: [['Tarefa', [['Nome', 'T25']]], ['Memória', [['Nome', 'T27']]], ['Gatilho', [['Quando…', 'T29']]]] },
      ['Contexto (já vem o lugar atual)', 'T26'], 'Quando lembrar', 'Salvar', { sys: 'Item fica no lugar certo' }, ['Aparece no Lugar', 'T22'],
      { sys: 'Usuário entra na cerca e fica 2 min' }, ['Notificação agrupada', 'T16'], ['Abre direto no contexto', 'T11'], { dec: 'O que faz?' },
      { br: [['Conclui', [['Toque no círculo', 'T13'], { end: 'Some da lista, com "Desfazer"' }]],
        ['Adia', [['Próxima vez aqui', 'T12'], { end: 'Volta na próxima visita' }]],
        ['Ignora', [{ sys: 'Continua pendente' }, { end: 'Após 3 visitas: "Ainda precisa disso?"' }]]] }
    ],
    'flow-task': [
      ['Toca em +', 'T24'], 'Tarefa', ['Digita o nome (teclado já aberto)', 'T25'], { sys: 'Onde = lugar atual' }, { dec: 'Lugar certo?' },
      { br: [['Sim', ['Mantém']], ['Não', ['Toca em outro chip ou "Outro"', ['Escolher lugar', 'T26']]]] },
      { sys: 'Quando = Ao chegar' }, 'Repetir (opcional)', 'Lê a frase-resumo', { end: 'Salvar tarefa · 3 toques + nome' }
    ],
    'flow-mem': [
      ['Toca em +', 'T24'], { dec: 'Rápido ou completo?' },
      { br: [['Rápido', ['Escreve em "Anotar algo aqui"', { end: 'Enter salva no lugar atual · 2 toques' }]],
        ['Completo', [['Memória', 'T27'], 'Nome', 'Presa a (mapa)', 'Mostrar: sempre / próxima vez', 'Guardar junto: nota, foto ou campo', { end: 'Salvar memória' }]]] },
      { sys: 'Usuário chega ao lugar' }, ['Memória aparece em Agora', 'T07'], ['Abre e anota o valor', 'T28'], { end: 'Arquiva ou mantém' }
    ],
    'flow-trig': [
      ['Toca em +', 'T24'], 'Gatilho', ['Quando: 9 condições', 'T29'], 'Detalhe (qual carro, rede, tag, horário)', { dec: 'Permissão ok?' },
      { br: [['Não', [['Aviso com a solução', 'T38'], 'Liga Bluetooth / permite', 'volta']], ['Sim', ['Continua']]] },
      ['Então: 5 ações', 'T30'], 'Condições (opcional)', { end: 'Criar gatilho · aparece no Lugar e em Rotinas' }
    ],
    'flow-arrive': [
      { sys: 'Entra na cerca virtual, conecta ao Wi-Fi ou lê NFC/QR' }, { dec: 'Wi-Fi/NFC/QR?' },
      { br: [['Sim', [{ sys: 'Confirma na hora' }]], ['Só GPS', [{ sys: 'Espera 2 min no local' }, { dec: 'Sinal fraco?' }, { sys: 'Sim → mostra "Talvez você esteja em…" (T09)' }]]] },
      { dec: 'Há pendências aqui?' },
      { br: [['Não', [{ end: 'Silêncio. Agora só troca o contexto.' }]],
        ['Sim', [['1 notificação com o total', 'T16'], { dec: 'Resolve onde?' }]]] },
      { br: [['Na notificação', [['Marca "Feito" em cada item', 'T17']]], ['No app', [['Agora já no lugar', 'T11'], 'Conclui / adia / ignora']]] }
    ],
    'flow-leave': [
      { sys: 'Sai da cerca, perde o Wi-Fi do lugar ou conecta ao carro' }, { dec: 'Algo pendente com "ao sair" ou não concluído?' },
      { br: [['Não', [{ end: 'Nada acontece' }]], ['Sim', [['"Você está saindo…"', 'T16']]]] },
      { dec: 'Resposta' },
      { br: [['Peguei', [{ end: 'Concluída sem abrir o app' }]],
        ['Próxima vez', [{ end: 'Volta na próxima visita (T15)' }]],
        ['Abrir', [['Antes de sair', 'T14'], 'Peguei tudo / Próxima vez']],
        ['Ainda estou aqui', [{ sys: 'Cancela e recalibra a saída' }]]] }
    ]
  };
  Object.keys(FLOWS).forEach(function (k) { const el = $(k); if (el) el.innerHTML = chain(FLOWS[k]); });

  /* ---------- Arquitetura ---------- */
  $('tabdemo').innerHTML = U.tabBar('agora');
  const byGroup = (g) => S.filter((s) => s.group === g);
  const li = (s) => '<li>' + ref(s.id) + '<span>' + s.name + '</span></li>';
  const TREE = [
    ['now', 'Agora', ['Agora', 'Chegada e saída']],
    ['places', 'Lugares', ['Lugares']],
    ['plus', '+ Adicionar', ['Criação']],
    ['list', 'Lista', ['Lista']],
    ['routine', 'Rotinas', ['Rotinas']],
    ['user', 'Você (avatar)', ['Você', 'Erros e permissões']],
    ['bell', 'Sistema', ['Notificações']],
    ['play', 'Primeiro uso', ['Abertura']]
  ];
  $('tree').innerHTML = TREE.map(function (t, i) {
    const items = [].concat.apply([], t[2].map(byGroup));
    return '<div class="tcol' + (i > 4 ? ' layer' : '') + '"><h5>' + I(t[0]) + t[1] + '</h5><ul>' + items.map(li).join('') + '</ul></div>';
  }).join('');

  /* ---------- Tabela de telas ---------- */
  $('screen-table').innerHTML = '<table><thead><tr><th>ID</th><th>Tela</th><th>Grupo</th><th>O que o usuário quer</th><th>Ação principal</th><th>O que foi removido / simplificado</th></tr></thead><tbody>' +
    S.map((s) => '<tr><td>' + ref(s.id) + '</td><td class="nm">' + s.name + '</td><td class="grp">' + s.group + '</td><td>' + s.goal + '</td><td>' + s.action + '</td><td>' + (s.cut || '—') + '</td></tr>').join('') + '</tbody></table>';

  /* ---------- Galeria ---------- */
  const groups = [];
  S.forEach((s) => { if (groups.indexOf(s.group) < 0) groups.push(s.group); });
  $('gallery').innerHTML = groups.map(function (g) {
    const list = byGroup(g);
    return '<div class="grp-h"><h3>' + g + '</h3><span>' + list.length + (list.length > 1 ? ' telas' : ' tela') + '</span></div><div class="gallery">' +
      list.map((s) => '<figure class="shot" id="' + s.id + '">' + s.html + '<figcaption class="figcap"><div class="figcap-h"><span class="ref">' + s.id + '</span><b>' + s.name + '</b>' + (s.main ? '<span class="key">tela-chave</span>' : '') + '</div>' +
        '<dl><dt>Objetivo</dt><dd>' + s.goal + '</dd><dt>Ação</dt><dd>' + s.action + '</dd>' + (s.cut ? '<dt>Decisão</dt><dd>' + s.cut + '</dd>' : '') + '</dl></figcaption></figure>').join('') + '</div>';
  }).join('');

  /* ---------- Design system ---------- */
  const SW = [
    ['--a-accent', 'Sinal', 'Ações e contexto atual', '#2F4FF5 · #7D93FF'],
    ['--a-accent-soft', 'Sinal suave', 'Fundo de destaque', '#E9EDFF · #1C2347'],
    ['--a-mem', 'Marcador', 'Memórias', '#A86400 · #F2B650'],
    ['--a-mem-soft', 'Marcador suave', 'Fundo de memória', '#FFF2DB · #2B2213'],
    ['--a-late', 'Alerta', 'Atraso e erro', '#D5373A · #FF6D6D'],
    ['--a-ok', 'Feito', 'Concluído e ativo', '#138A5A · #42CC90'],
    ['--a-ink', 'Tinta', 'Texto principal', '#0F121A · #EEF0F6'],
    ['--a-ink-2', 'Tinta 2', 'Texto secundário', '#555B6E · #A4AABB'],
    ['--a-ink-3', 'Tinta 3', 'Metadados, rótulos', '#9197A8 · #6B7185'],
    ['--a-line', 'Linha', 'Divisores', '#E6E8EF · #242834'],
    ['--a-surface', 'Superfície', 'Campos e blocos', '#F4F5F9 · #181B24'],
    ['--a-bg', 'Fundo', 'Tela', '#FFFFFF · #0F1117']
  ];
  $('ds-colors').innerHTML = SW.map((c) => '<div class="sw"><i style="background:var(' + c[0] + ')"></i><div><b>' + c[1] + '</b>' + c[2] + '<span>' + c[3] + '</span></div></div>').join('');

  $('ds-type').innerHTML = [
    ['Contexto · 32/36', 'Schibsted Grotesk 600', '<span class="ctx-t" style="margin:0">Supermercado</span>'],
    ['Título 1 · 30/34', 'Schibsted Grotesk 600', '<span class="h1" style="margin:0">Cheguei em casa</span>'],
    ['Título 2 · 22/28', 'Schibsted Grotesk 600', '<span class="h2">Saindo do Supermercado?</span>'],
    ['Corpo · 16/22', 'Onest 500', '<span class="row-t">Comprar sabão em pó</span>'],
    ['Apoio · 13–14/20', 'Onest 400', '<span class="sub" style="margin:0">Depois de 2 min no local</span>'],
    ['Rótulo · 12', 'Onest 600, +0.08em', '<span class="sec-t">Pendências aqui</span>'],
    ['Dados · 12–13', 'IBM Plex Mono 500', '<span class="dist" style="font-size:14px;color:var(--a-ink-2)">350 m · ±140 m · 18:07</span>']
  ].map((t) => '<div class="ts-r"><code>' + t[0] + '<br>' + t[1] + '</code><div>' + t[2] + '</div></div>').join('');

  $('ds-space').innerHTML = [4, 8, 12, 16, 20, 24, 32, 40].map((n) => '<div><i style="width:' + n + 'px;height:' + n + 'px"></i>' + n + '</div>').join('') +
    '<div style="margin-left:24px"><i style="width:44px;height:44px;border-radius:8px;opacity:.25"></i>r 8</div><div><i style="width:44px;height:44px;border-radius:12px;opacity:.25"></i>r 12</div><div><i style="width:44px;height:44px;border-radius:16px;opacity:.25"></i>r 16</div><div><i style="width:44px;height:44px;border-radius:24px 24px 0 0;opacity:.25"></i>r 24</div>';

  $('ds-buttons').innerHTML =
    '<div class="demo-lbl">Variantes</div><div class="demo-row">' + U.btn('Salvar tarefa') + U.btn('Próxima vez', 'o') + U.btn('Testar agora', 's', 'play') + U.btn('Não preciso mais', 'g') + U.btn('Apagar tudo', 'd', 'trash') + '</div>' +
    '<div class="demo-lbl" style="margin-top:20px">Estados do principal</div><div class="demo-row">' + U.btn('Padrão') + U.btn('Pressionado', 'p', null, 'press') + U.btn('Indisponível', 'p', null, 'dis') + '<div class="btn btn-p" style="width:auto;padding:0 20px"><span class="spin"></span>Salvando</div></div>' +
    '<div class="demo-lbl" style="margin-top:20px">Pequenos · chips · segmento · alternador</div><div class="demo-row">' + U.btn('Peguei', 's', null, 'sm') + U.btn('Ligar', 'p', null, 'sm') +
    U.chip('Todas', true) + U.chip('Supermercado', false, 'cart') + U.chip('Wi-Fi Oficina_2G', false, 'wifi', 'acc') + U.chip('Outro', false, 'plus', 'dash') + '</div>' +
    '<div class="demo-row"><div style="width:280px">' + U.seg(['Uma vez', 'Diária', 'Semanal'], 2) + '</div>' + U.tg(true) + U.tg(false) + U.tg(false, true) + '<span class="rd on"></span><span class="rd"></span></div>';

  $('ds-rows').innerHTML =
    '<div><div class="demo-lbl">Linhas de item</div>' +
    U.row({ t: 'Comprar café', meta: [U.m('arrive', 'Ao chegar'), U.m('repeat', 'Semanal')] }) +
    U.row({ t: 'Enviar relatório mensal', done: true, meta: [U.m('clock', 'até 10:00')] }) +
    U.row({ t: 'Pagar conta de luz', late: true, meta: ['<span class="late">Venceu ontem</span>', U.m('home', 'Casa')] }) +
    U.row({ kind: 'mem', t: 'Ver preço do arroz', meta: [U.tag('Memória', 'mem'), 'anotar o valor'] }) +
    U.row({ kind: 'trig', icon: 'car', t: 'Ao conectar ao HB20', meta: ['Mostrar tarefas do Carro'] }) + '</div>' +
    '<div><div class="demo-lbl">Cartões e blocos</div>' +
    '<div class="near-row" style="margin:0 0 12px;overflow:visible"><div class="near"><div class="near-h">' + I('pill') + 'Farmácia</div><div class="near-m"><span class="mono">350 m</span> · 1 tarefa</div></div>' +
    '<div class="near"><div class="near-h">' + I('cart') + 'Mercado</div><div class="near-m"><span class="mono">900 m</span> · 3 itens</div></div></div>' +
    U.place({ icon: 'work', name: 'Trabalho', meta: '3 tarefas · 1 memória', trg: ['arrive', 'leave', 'wifi'], dist: '2,4 km' }) +
    '<div class="rt"><div class="rt-h"><b>Cheguei em casa</b>' + U.tg(true) + '</div><div class="rt-w">' + I('arrive') + 'Ao chegar em Casa · depois das 17h</div><div class="rt-steps">' + U.tag('Guardar compras') + U.tag('Alimentar o Thor') + '</div></div></div>' +
    '<div><div class="demo-lbl">Contexto atual · 3 estados</div><div style="display:flex;gap:16px;align-items:center">' + U.ring('in') + U.ring('out') + U.ring('warn') + '</div>' +
    '<div class="demo-lbl" style="margin-top:18px">Etiquetas e status</div><div class="demo-row">' + U.tag('Memória', 'mem', 'memory') + U.tag('Você está aqui', 'acc', 'now') + U.tag('Venceu ontem', 'late') + U.tag('Ativa', 'ok') + U.tag('Semanal', null, 'repeat') + '<span class="cnt">3</span></div>' +
    '<div class="demo-row"><span class="st ok">Ativo</span><span class="st mid">Parcial</span><span class="st bad">Desligado</span><span class="st na">Indisponível</span></div>' +
    '<div style="margin-top:16px">' + U.banner('warn', 'locoff', 'Localização desativada', 'Avisos de chegada e saída estão pausados.', ['Ativar']) + '</div>' +
    '<div style="margin-top:8px">' + U.banner('info', 'info', 'Economia de bateria ligada', 'Avisos podem atrasar até 15 min.') + '</div></div>';

  $('ds-fields').innerHTML =
    '<div>' + U.field({ label: 'Padrão', icon: 'search', ph: 'Buscar lugar ou endereço' }) + U.field({ label: 'Preenchido', icon: 'pin', value: 'Supermercado', trail: I('chevR') }) + '</div>' +
    '<div>' + U.field({ label: 'Em foco', value: 'Oficina do Marcos', state: 'focus', help: 'O nome aparece nas notificações.' }) + U.field({ label: 'Erro', icon: 'wifi', value: 'Oficina_2G', state: 'err', help: 'Você não está conectado a esta rede agora.' }) + '</div>' +
    '<div><div class="fl"><div class="fl-l">Opções</div><div class="opts">' + U.opt({ icon: 'arrive', t: 'Ao chegar', s: 'Depois de 2 min no local', radio: true }) + U.opt({ icon: 'leave', t: 'Ao sair', radio: false }) + U.opt({ icon: 'nfc', t: 'NFC', s: 'Este aparelho não tem NFC', dis: true }) + '</div></div>' +
    '<div class="fl"><div class="fl-l">Captura em memória</div><div class="cap"><div class="cap-l">Leitura do medidor</div><div class="cap-v">04821<span class="u">kWh</span></div></div></div></div>';

  $('ds-icons').innerHTML = Object.keys(window.ICONS).map((k) => '<div class="ic-c">' + I(k) + k + '</div>').join('');
  $('ds-logo').innerHTML = '<div style="display:flex;gap:28px;align-items:center;flex-wrap:wrap">' +
    '<div style="display:flex;align-items:center;gap:12px">' + U.logo(48) + '<span class="wm" style="font-size:36px;color:var(--a-ink)">roteiro</span></div>' +
    '<div style="width:88px;height:88px;border-radius:22px;background:var(--a-accent);display:grid;place-items:center">' + U.logo(56, '#fff', '#fff') + '</div>' +
    '<div style="width:88px;height:88px;border-radius:22px;background:var(--a-ink);display:grid;place-items:center">' + U.logo(56, 'var(--a-bg)', 'var(--a-accent)') + '</div>' +
    '<p style="flex:1;min-width:220px;margin:0;font-size:14px;color:var(--a-ink-2)">O símbolo é um anel com um ponto dentro: você, dentro de um contexto. O mesmo desenho aparece na tela Agora, onde muda de estado (dentro, fora, sem sinal). O ponto fica deslocado para cima e à direita para não parecer um alvo.</p></div>';

  /* ---------- Modo final / wireframe ---------- */
  const btns = document.querySelectorAll('.mode button');
  function setMode(m) {
    document.body.classList.toggle('wf', m === 'wf');
    btns.forEach((b) => b.setAttribute('aria-pressed', String(b.dataset.m === m)));
    try { localStorage.setItem('roteiro-mode', m); } catch (e) { /* sem armazenamento */ }
  }
  btns.forEach((b) => b.addEventListener('click', () => setMode(b.dataset.m)));
  let saved = 'final';
  try { saved = localStorage.getItem('roteiro-mode') || 'final'; } catch (e) { /* sem armazenamento */ }
  setMode(saved);
})();
