/* =========================================================
   Roteiro — definição de todas as telas
   Cada tela: { id, group, name, goal, action, cut, html }
   ========================================================= */
(function () {
  const U = window.UI, I = U.I;
  const { phone, top, ib, sec, row, m, chip, tag, seg, tg, btn, field, opt, banner, sheet, toast, empty, place, map, pins, notif, ring, logo } = U;

  /* ---------- Ilustrações (onboarding e vazios) ---------- */
  function art(ic, tone) {
    const c = tone === 'ok' ? 'var(--a-ok)' : tone === 'mem' ? 'var(--a-mem)' : 'var(--a-accent)';
    const bg = tone === 'ok' ? 'var(--a-ok-soft)' : tone === 'mem' ? 'var(--a-mem-soft)' : 'var(--a-accent-soft)';
    return '<svg class="art" viewBox="0 0 132 132" aria-hidden="true"><circle cx="66" cy="66" r="62" fill="none" stroke="' + c + '" stroke-opacity=".5" stroke-width="1.5" stroke-dasharray="4 6"/>' +
      '<circle cx="66" cy="66" r="44" fill="' + bg + '"/>' + window.iconAt(ic, 46, 46, 40, c) + '</svg>';
  }

  const obArt1 = '<svg viewBox="0 0 280 280" aria-hidden="true">' +
    '<circle cx="140" cy="140" r="96" fill="var(--a-accent-soft)"/><circle cx="140" cy="140" r="96" fill="none" stroke="var(--a-accent)" stroke-width="1.5" stroke-dasharray="4 6"/>' +
    '<circle cx="140" cy="140" r="30" fill="var(--a-accent)"/>' + window.iconAt('cart', 124, 124, 32, 'var(--a-accent-ink)') +
    '<g font-family="Onest, sans-serif" font-size="13" font-weight="500" fill="var(--a-ink)">' +
    '<rect x="14" y="54" width="128" height="38" rx="12" fill="var(--a-bg)" stroke="var(--a-line)"/><circle cx="34" cy="73" r="7" fill="none" stroke="var(--a-ink-3)" stroke-width="1.75"/><text x="48" y="78">Comprar café</text>' +
    '<rect x="150" y="196" width="118" height="38" rx="12" fill="var(--a-bg)" stroke="var(--a-line)"/><circle cx="170" cy="215" r="7" fill="none" stroke="var(--a-ink-3)" stroke-width="1.75"/><text x="184" y="220">Sabão em pó</text>' +
    '<rect x="160" y="40" width="108" height="38" rx="12" fill="var(--a-bg)" stroke="var(--a-line)"/><rect x="172" y="51" width="16" height="16" rx="4" fill="var(--a-mem-soft)"/><path d="M176.5 54.5h7v10l-3.5-2.6-3.5 2.6z" fill="var(--a-mem)"/><text x="196" y="64">Arroz?</text></g>' +
    '<path d="M142 92 L150 112M214 78 L180 116M190 196 L162 166" stroke="var(--a-accent)" stroke-width="1.5" stroke-dasharray="3 4"/></svg>';

  const obArt2 = '<svg viewBox="0 0 280 280" aria-hidden="true">' +
    '<circle cx="164" cy="160" r="84" fill="var(--a-accent-soft)"/><circle cx="164" cy="160" r="84" fill="none" stroke="var(--a-accent)" stroke-width="1.5" stroke-dasharray="4 6"/>' +
    '<path d="M24 250 C 60 230, 80 200, 120 186 S 150 170, 160 164" fill="none" stroke="var(--a-ink-3)" stroke-width="2" stroke-dasharray="2 7" stroke-linecap="round"/>' +
    '<circle cx="164" cy="162" r="13" fill="var(--a-accent)" stroke="var(--a-bg)" stroke-width="4"/>' +
    '<rect x="40" y="36" width="210" height="64" rx="18" fill="var(--a-bg)" stroke="var(--a-line)"/>' +
    '<rect x="56" y="52" width="32" height="32" rx="9" fill="var(--a-accent)"/>' + window.iconAt('bell', 62, 58, 20, 'var(--a-accent-ink)') +
    '<text x="100" y="64" font-family="Onest, sans-serif" font-size="13.5" font-weight="600" fill="var(--a-ink)">Você chegou ao mercado</text>' +
    '<text x="100" y="83" font-family="Onest, sans-serif" font-size="12.5" fill="var(--a-ink-2)">3 coisas esperam por você</text></svg>';

  function ctxToken(x, y, ic, on) {
    return '<circle cx="' + x + '" cy="' + y + '" r="30" fill="' + (on ? 'var(--a-accent)' : 'var(--a-bg)') + '" stroke="' + (on ? 'var(--a-accent)' : 'var(--a-line)') + '"/>' + window.iconAt(ic, x - 13, y - 13, 26, on ? 'var(--a-accent-ink)' : 'var(--a-ink)');
  }
  const obArt3 = '<svg viewBox="0 0 280 280" aria-hidden="true">' +
    '<circle cx="140" cy="140" r="100" fill="none" stroke="var(--a-line)" stroke-width="1.5"/><circle cx="140" cy="140" r="54" fill="none" stroke="var(--a-line)" stroke-width="1.5" stroke-dasharray="3 5"/>' +
    '<g transform="translate(112 112)">' + logo(56) + '</g>' +
    ctxToken(140, 40, 'home') + ctxToken(240, 140, 'work') + ctxToken(140, 240, 'car', true) + ctxToken(40, 140, 'cart') +
    ctxToken(211, 69, 'wifi') + ctxToken(69, 211, 'nfc') + '</svg>';

  const obArtPerm = '<svg viewBox="0 0 280 280" aria-hidden="true">' +
    '<circle cx="140" cy="150" r="104" fill="none" stroke="var(--a-accent)" stroke-opacity=".35" stroke-width="1.5" stroke-dasharray="4 6"/>' +
    '<circle cx="140" cy="150" r="70" fill="var(--a-accent-soft)"/>' + window.iconAt('pin', 104, 106, 72, 'var(--a-accent)') +
    '<rect x="176" y="70" width="64" height="30" rx="15" fill="var(--a-bg)" stroke="var(--a-line)"/><text x="208" y="90" text-anchor="middle" font-family="IBM Plex Mono, monospace" font-size="12" font-weight="500" fill="var(--a-ink-2)">80 m</text></svg>';

  function onboarding(n, artSvg, title, text, cta) {
    return '<div class="ob"><div class="ob-skip">' + (n < 3 ? 'Pular' : '&nbsp;') + '</div><div class="ob-art">' + artSvg + '</div>' +
      '<h2>' + title + '</h2><p>' + text + '</p><div class="dots">' + [1, 2, 3].map((i) => '<i' + (i === n ? ' class="on"' : '') + '></i>').join('') + '</div>' + btn(cta) + '</div>';
  }

  /* ---------- Blocos repetidos ---------- */
  const homeTop = (date) => top({ left: '<span class="date">' + logo(22) + (date || 'Sexta, 24 set') + '</span>', right: '<span class="av">LA</span>' });

  function hero(k, title, meta, state, kOff) {
    return '<div class="ctx"><div class="ctx-tx"><div class="ctx-k' + (kOff ? ' off' : '') + '">' + (kOff ? '' : '<span class="live"></span>') + k + '</div>' +
      '<div class="ctx-t">' + title + '</div><div class="ctx-m">' + meta + '</div></div>' + ring(state) + '</div>';
  }

  const mercadoRows = (doneFirst) =>
    row({ t: 'Comprar café', done: doneFirst, meta: [m('arrive', 'Ao chegar'), m('repeat', 'Semanal')] }) +
    row({ t: 'Comprar sabão em pó', meta: [m('arrive', 'Ao chegar')] }) +
    row({ kind: 'mem', t: 'Ver preço do arroz', meta: [tag('Memória', 'mem'), 'anotar o valor'] });

  const homeMercado = homeTop() +
    hero('Agora · você está em', 'Supermercado', '<b>Pão de Açúcar</b><span>·</span><span>chegou há 6 min</span><span>·</span><span class="link" style="font-size:13px">Não é aqui?</span>', 'in') +
    sec('Pendências aqui', 3, 'Ver lugar') + mercadoRows(false) +
    sec('Atrasada', 1, null, true) +
    row({ t: 'Pagar conta de luz', late: true, meta: ['<span class="late">Venceu ontem</span>', m('home', 'Casa')] }) +
    sec('Próximos gatilhos') +
    '<div class="nt"><span class="nt-when">saída</span><span class="nt-ic">' + I('leave') + '</span><span class="nt-t">Ao sair daqui<small>Confere se algo ficou para trás</small></span></div>' +
    '<div class="nt"><span class="nt-when">carro</span><span class="nt-ic">' + I('car') + '</span><span class="nt-t">Ao conectar ao HB20<small>Passar na farmácia</small></span></div>';

  const workRows = (done) =>
    row({ t: 'Enviar relatório mensal', done: done, meta: [m('clock', 'até 10:00')] }) +
    row({ t: 'Pegar documento no RH', meta: [m('leave', 'Também ao sair')] }) +
    row({ t: 'Falar com João', meta: ['sobre as férias de outubro'] });

  const heroWork = hero('Você chegou', 'Trabalho', '<b>Escritório</b><span>·</span><span>há 1 min</span><span>·</span>' + m('wifi', 'Wi-Fi Escritorio-5G'), 'in');

  /* =========================================================
     TELAS
     ========================================================= */
  const S = [];
  const add = (o) => S.push(o);

  /* ---------- Abertura ---------- */
  add({ id: 'T01', group: 'Abertura', name: 'Splash',
    goal: 'Saber que abriu o app certo.', action: 'Nenhuma. Some em menos de 1 s.', cut: 'Sem carregamento, sem slogan longo.',
    html: phone({ invStatus: true, raw: '<div class="splash">' + logo(84, '#fff', '#fff') + '<div class="wm">roteiro</div><div class="tl">Lembre no lugar certo.</div><div class="ver">v1.0</div></div>' }) });

  add({ id: 'T02', group: 'Abertura', name: 'Onboarding 1 · Conceito',
    goal: 'Entender o que o app faz em 5 segundos.', action: 'Continuar', cut: 'Três telas no máximo. "Pular" sempre visível.',
    html: phone({ raw: onboarding(1, obArt1, 'Coloque cada coisa no lugar onde ela acontece.', 'Comprar café fica no mercado. Enviar o relatório fica no trabalho. Sua cabeça fica livre.', 'Continuar') }) });

  add({ id: 'T03', group: 'Abertura', name: 'Onboarding 2 · Gatilho',
    goal: 'Entender que o app avisa sozinho.', action: 'Continuar', cut: 'Nada de explicar "gatilho" como termo técnico.',
    html: phone({ raw: onboarding(2, obArt2, 'Chegou no lugar certo? A gente lembra você.', 'Ao chegar ou sair de um lugar, o Roteiro mostra só o que importa ali. O resto espera.', 'Continuar') }) });

  add({ id: 'T04', group: 'Abertura', name: 'Onboarding 3 · Contextos',
    goal: 'Perceber que "lugar" vai além de endereço.', action: 'Começar', cut: '',
    html: phone({ raw: onboarding(3, obArt3, 'Casa, trabalho, carro ou mercado.', 'Qualquer lugar vira um contexto: um endereço, o Bluetooth do carro, o Wi-Fi do escritório ou uma tag NFC.', 'Começar') }) });

  add({ id: 'T05', group: 'Abertura', name: 'Pedido de localização',
    goal: 'Decidir se confia no app.', action: 'Permitir localização', cut: 'O pedido do sistema só aparece depois desta explicação.',
    html: phone({ raw: '<div class="ob"><div class="ob-skip">&nbsp;</div><div class="ob-art" style="flex:0 0 220px">' + obArtPerm.replace('viewBox="0 0 280 280"', 'viewBox="0 0 280 280" style="width:200px;height:200px"') + '</div>' +
      '<h2>Para lembrar no lugar certo, precisamos saber onde você está.</h2>' +
      '<div class="perm-list"><div class="perm-li">' + I('shield') + '<div><b>Fica no seu aparelho</b>Sua localização não é enviada a ninguém.</div></div>' +
      '<div class="perm-li">' + I('battery') + '<div><b>Leve para a bateria</b>Usamos as cercas do próprio sistema, não GPS contínuo.</div></div></div>' +
      '<div style="flex:1"></div>' + btn('Permitir localização') + '<div class="btn btn-g" style="height:44px;margin-top:4px">Agora não</div></div>' }) });

  add({ id: 'T06', group: 'Abertura', name: 'Primeiros lugares',
    goal: 'Chegar à tela Agora já com lugares úteis.', action: 'Concluir', cut: 'Endereço exato é opcional; Casa usa a posição atual.',
    html: phone({ body: top({ left: ib('chevL') }) +
      '<div class="h1">Onde você passa seu dia?</div><div class="sub">Toque para confirmar. Você ajusta os endereços depois.</div>' +
      '<div style="margin-top:14px">' +
      place({ icon: 'home', name: 'Casa', meta: 'Você está aqui agora', here: true }).replace('</div></div>', '</div><span class="chk on">' + I('check') + '</span></div>') +
      place({ icon: 'work', name: 'Trabalho', meta: 'Av. Paulista, 1578' }).replace('</div></div>', '</div><span class="chk on">' + I('check') + '</span></div>') +
      place({ icon: 'cart', name: 'Supermercado', meta: 'Escolher qual' }).replace('</div></div>', '</div><span class="link">Definir</span></div>') +
      place({ icon: 'car', name: 'Carro', meta: 'Pelo Bluetooth do carro' }).replace('</div></div>', '</div><span class="link">Definir</span></div>') +
      place({ icon: 'gym', name: 'Academia', meta: 'Opcional' }).replace('</div></div>', '</div><span class="link">Definir</span></div>') +
      '</div><div class="qa">' + I('plus') + 'Outro lugar</div>' +
      '<div class="foot">' + btn('Concluir') + '</div>' }) });

  /* ---------- Agora ---------- */
  add({ id: 'T07', group: 'Agora', name: 'Agora · no supermercado', main: true,
    goal: 'Saber o que fazer aqui, sem procurar.', action: 'Marcar item como feito (1 toque).',
    cut: 'Nada de contadores globais ("47 tarefas"). Só o que vale para este lugar e este momento.',
    html: phone({ tab: 'agora', time: '18:12', body: homeMercado }) });

  add({ id: 'T08', group: 'Agora', name: 'Agora · fora dos seus lugares',
    goal: 'Saber o que está perto e o que vem a seguir.', action: 'Abrir um lugar próximo.', cut: 'Não mostra lista geral: mostra proximidade e horário.',
    html: phone({ tab: 'agora', time: '07:48', body: homeTop() +
      hero('Agora', 'Em trânsito', '<span>Fora dos seus lugares</span><span>·</span><span>Av. Paulista</span>', 'out', true) +
      sec('Perto de você', null, 'Mapa') +
      '<div class="near-row">' +
      '<div class="near"><div class="near-h">' + I('pill') + 'Farmácia</div><div class="near-m"><span class="mono">350 m</span> · 1 tarefa</div></div>' +
      '<div class="near"><div class="near-h">' + I('cart') + 'Mercado</div><div class="near-m"><span class="mono">900 m</span> · 3 itens</div></div>' +
      '<div class="near"><div class="near-h">' + I('tool') + 'Oficina</div><div class="near-m"><span class="mono">2,1 km</span> · 1 memória</div></div></div>' +
      sec('Próximo contexto') +
      '<div class="nt"><span class="nt-when">~12 min</span><span class="nt-ic">' + I('work') + '</span><span class="nt-t">Trabalho<small>3 coisas esperando por você</small></span><span class="cnt">3</span></div>' +
      sec('Hoje, em qualquer lugar', 2) +
      row({ t: 'Tomar vitamina D', meta: [m('clock', '08:00'), m('repeat', 'Diária')] }) +
      row({ t: 'Pagar conta de luz', late: true, meta: ['<span class="late">Venceu ontem</span>', m('home', 'Casa')] }) }) });

  add({ id: 'T09', group: 'Agora', name: 'Agora · local incerto',
    goal: 'Corrigir o app quando o GPS erra.', action: 'Confirmar ou trocar o lugar (1 toque).', cut: 'O app nunca afirma um lugar quando o sinal é fraco.',
    html: phone({ tab: 'agora', time: '18:10', body: homeTop() +
      hero('Talvez você esteja em', 'Supermercado?', '<span>Sinal de localização fraco</span><span>·</span><span class="mono">±140 m</span>', 'out', true) +
      '<div class="btn-row" style="margin-top:14px">' + btn('Sim, estou aqui', 'p', null, 'sm') + btn('Não estou', 'o', null, 'sm') + '</div>' +
      sec('Ou escolha onde está') +
      '<div class="chips" style="margin-top:8px">' + chip('Casa', false, 'home') + chip('Farmácia', false, 'pill') + chip('Trabalho', false, 'work') + chip('Outro', false, 'plus', 'dash') + '</div>' +
      sec('Se for o supermercado', 3) + mercadoRows(false) }) });

  add({ id: 'T10', group: 'Agora', name: 'Agora · primeiro uso (vazio)',
    goal: 'Criar o primeiro item sem pensar muito.', action: 'Adicionar em Casa', cut: 'Tela vazia com sugestões reais, não com ilustração genérica.',
    html: phone({ tab: 'agora', body: homeTop() +
      hero('Agora · você está em', 'Casa', '<span>R. Harmonia, 88</span><span>·</span><span>detectado agora</span>', 'in') +
      '<div style="margin-top:36px">' + empty(art('plus'), 'Nada esperando por você aqui', 'Adicione algo para lembrar quando estiver em Casa, ou em qualquer outro lugar.', btn('Adicionar em Casa', 'p', 'plus')) + '</div>' +
      sec('Ideias para começar') +
      '<div class="chips" style="margin-top:8px">' + chip('Tirar o lixo · ao sair') + chip('Regar as plantas') + chip('Levar marmita · ao sair') + '</div>' }) });

  /* ---------- Chegada e saída ---------- */
  add({ id: 'T11', group: 'Chegada e saída', name: 'Chegada · aberto pela notificação',
    goal: 'Ver tudo que precisa ser feito ao chegar.', action: 'Concluir ou adiar cada item.', cut: 'Abre direto no contexto, sem passar pela lista.',
    html: phone({ tab: 'agora', time: '08:57', body: homeTop() + heroWork +
      sec('3 coisas para lembrar', null, 'Ver lugar') + workRows(false) +
      sec('Memória deste lugar') + row({ kind: 'mem', t: 'Vaga do estacionamento: G2-214', meta: [tag('Memória', 'mem'), 'salva em 12 set'] }) }) });

  add({ id: 'T12', group: 'Chegada e saída', name: 'Chegada · adiar item',
    goal: 'Tirar o item da frente sem perdê-lo.', action: 'Adiar para a próxima vez aqui.', cut: 'Adiar por lugar vem antes de adiar por hora.',
    html: phone({ tab: 'agora', time: '08:58', body: homeTop() + heroWork + sec('3 coisas para lembrar') + workRows(false),
      overlay: sheet('<div class="h2">Adiar "Falar com João"</div><div class="sub">Quando você quer ver isso de novo?</div>' +
        '<div class="opts" style="margin-top:16px">' +
        opt({ icon: 'repeat', t: 'Na próxima vez que eu vier aqui', s: 'Provavelmente amanhã, 09:00', radio: true }) +
        opt({ icon: 'clock', t: 'Daqui a 1 hora', radio: false }) +
        opt({ icon: 'home', t: 'Quando eu chegar em Casa', s: 'Muda o lugar da tarefa', radio: false }) +
        opt({ icon: 'calendar', t: 'Escolher dia e hora', radio: false }) + '</div>' +
        '<div style="margin-top:16px">' + btn('Adiar') + '</div><div class="btn btn-g" style="height:40px;margin-top:4px">Não preciso mais</div>') }) });

  add({ id: 'T13', group: 'Chegada e saída', name: 'Chegada · item concluído',
    goal: 'Ter certeza de que marcou.', action: 'Desfazer, se errou.', cut: 'Sem confirmação modal; o toque já conclui.',
    html: phone({ tab: 'agora', time: '09:24', body: homeTop() + heroWork + sec('Faltam 2 aqui') + workRows(true) +
      sec('Memória deste lugar') + row({ kind: 'mem', t: 'Vaga do estacionamento: G2-214', meta: [tag('Memória', 'mem')] }),
      overlay: toast('Relatório enviado. Faltam 2 aqui.', 'Desfazer') }) });

  add({ id: 'T14', group: 'Chegada e saída', name: 'Saída · "Antes de sair"',
    goal: 'Não ir embora esquecendo algo.', action: 'Peguei tudo / Deixar para a próxima.', cut: 'Só lista o que ainda está pendente, nunca o que já foi feito.',
    html: phone({ tab: 'agora', time: '18:31', body: homeMercado,
      overlay: sheet('<div style="display:flex;gap:14px;align-items:center"><span class="add-ic t">' + I('leave') + '</span><div><div class="h2">Saindo do Supermercado?</div><div class="sub" style="margin-top:2px">Antes de ir, ainda falta:</div></div></div>' +
        '<div style="margin-top:10px">' +
        row({ t: 'Comprar café', meta: [m('repeat', 'Semanal')], trail: '<span class="btn btn-s sm">Peguei</span>' }) +
        row({ kind: 'mem', t: 'Ver preço do arroz', meta: ['sem valor anotado'], trail: '<span class="btn btn-s sm">Anotar</span>' }) + '</div>' +
        '<div class="btn-row" style="margin-top:14px">' + btn('Próxima vez', 'o') + btn('Peguei tudo') + '</div>' +
        '<div class="btn btn-g" style="height:40px;margin-top:4px">Ainda estou aqui</div>') }) });

  add({ id: 'T15', group: 'Chegada e saída', name: 'Saída · confirmação',
    goal: 'Saber o que vai acontecer com o que ficou.', action: 'Nenhuma.', cut: '',
    html: phone({ tab: 'agora', time: '18:33', body: homeTop() +
      hero('Agora', 'Em trânsito', '<span>Saiu do Supermercado há 1 min</span>', 'out', true) +
      sec('Ficou para a próxima visita', 1) +
      row({ t: 'Comprar café', meta: [m('cart', 'Supermercado'), 'aparece de novo ao chegar'] }) +
      sec('Próximo contexto') +
      '<div class="nt"><span class="nt-when">~15 min</span><span class="nt-ic">' + I('home') + '</span><span class="nt-t">Casa<small>Rotina "Cheguei em casa" vai começar</small></span><span class="cnt">3</span></div>',
      overlay: toast('Café fica para a próxima ida ao mercado.') }) });

  /* ---------- Notificações ---------- */
  add({ id: 'T16', group: 'Notificações', name: 'Tela de bloqueio',
    goal: 'Entender o aviso sem abrir o app.', action: 'Ver / Peguei', cut: 'Uma notificação por chegada, com o número de itens, não uma por item.',
    html: phone({ invStatus: true, time: '18:07', raw: '<div class="lock"><div class="lock-t"><div class="d">Sexta-feira, 24 de setembro</div><div class="h">18:07</div></div>' +
      '<div class="nstack" style="top:250px">' +
      notif({ sub: 'Supermercado', title: 'Você chegou ao supermercado.', body: '3 coisas estão esperando por você aqui.', actions: ['Ver', 'Mais tarde'], primary: true }) +
      notif({ sub: 'Trabalho', time: '17:58', title: 'Você está saindo do trabalho.', body: 'Você ainda precisa pegar o documento no RH.', actions: ['Peguei', 'Próxima vez'] }) +
      notif({ sub: 'Carro', time: '17:40', cls: 'lite', title: 'Carro conectado', body: 'Farmácia: comprar protetor solar.' }) + '</div></div>' }) });

  add({ id: 'T17', group: 'Notificações', name: 'Notificação expandida',
    goal: 'Resolver sem abrir o app.', action: 'Marcar cada item direto na notificação.', cut: 'Máximo de 3 itens; o resto vai para "Abrir".',
    html: phone({ invStatus: true, time: '18:08', raw: '<div class="lock"><div class="lock-t"><div class="d">Sexta-feira, 24 de setembro</div><div class="h">18:08</div></div>' +
      '<div class="nstack" style="top:250px">' +
      notif({ sub: 'Supermercado', title: '3 coisas aqui', items: [{ t: 'Comprar café', a: 'Feito' }, { t: 'Comprar sabão em pó', a: 'Feito' }, { t: 'Ver preço do arroz', mem: true, a: 'Anotar' }], actions: ['Abrir', 'Silenciar hoje'], primary: true }) + '</div></div>' }) });

  add({ id: 'T18', group: 'Notificações', name: 'Aviso com o app aberto',
    goal: 'Perceber a chegada sem perder o que estava fazendo.', action: 'Ver', cut: 'Some sozinho em 6 s; a tela Agora já foi atualizada.',
    html: phone({ tab: 'agora', time: '08:56', body: homeTop() +
      hero('Agora', 'Em trânsito', '<span>Fora dos seus lugares</span>', 'out', true) +
      sec('Perto de você') +
      '<div class="near-row"><div class="near"><div class="near-h">' + I('work') + 'Trabalho</div><div class="near-m"><span class="mono">60 m</span> · 3 itens</div></div>' +
      '<div class="near"><div class="near-h">' + I('pill') + 'Farmácia</div><div class="near-m"><span class="mono">1,2 km</span> · 1 tarefa</div></div></div>',
      overlay: notif({ cls: 'heads', sub: 'Trabalho', title: 'Você chegou ao trabalho.', body: '3 coisas para lembrar: relatório, documento e João.', actions: ['Ver', 'Depois'], primary: true }) }) });

  /* ---------- Lugares ---------- */
  const placesSheet = '<div class="sheet" style="padding-bottom:6px;box-shadow:0 -8px 30px rgba(15,18,40,.10)"><div class="grab"></div>' +
    '<div style="display:flex;justify-content:space-between;align-items:baseline"><div class="h2">Seus lugares</div><span class="dist">6 lugares</span></div>' +
    '<div style="margin-top:6px">' +
    place({ icon: 'cart', name: 'Supermercado', meta: '<span style="color:var(--a-accent);font-weight:600">Você está aqui</span> · 2 tarefas · 1 memória', here: true, trg: ['arrive', 'leave'] }) +
    place({ icon: 'work', name: 'Trabalho', meta: '3 tarefas · 1 memória', trg: ['arrive', 'leave', 'wifi'], dist: '2,4 km' }) +
    place({ icon: 'home', name: 'Casa', meta: '2 tarefas · rotina', trg: ['arrive'], dist: '1,1 km' }) +
    place({ icon: 'car', name: 'Carro', meta: 'Sem endereço · Bluetooth HB20', trg: ['bt'] }) + '</div></div>';

  add({ id: 'T19', group: 'Lugares', name: 'Lugares · mapa', main: true,
    goal: 'Ver onde as coisas estão e o que tem em cada lugar.', action: 'Tocar em um lugar.', cut: 'O mapa não mostra tarefas soltas, só lugares com contagem.',
    html: phone({ tab: 'lugares', flush: true, body: '<div class="map-wrap" style="position:absolute;inset:0 0 300px 0">' + map({ pins: pins('mercado'), me: { x: 176, y: 214 } }) +
      '<div class="map-top"><div class="srch">' + I('search') + 'Buscar lugar ou endereço</div><span class="fab-s">' + I('target') + '</span></div></div>' + placesSheet }) });

  add({ id: 'T20', group: 'Lugares', name: 'Lugares · prévia do lugar',
    goal: 'Espiar um lugar sem sair do mapa.', action: 'Abrir lugar', cut: '',
    html: phone({ tab: 'lugares', flush: true, body: '<div class="map-wrap" style="position:absolute;inset:0">' + map({ pins: pins('trabalho'), me: { x: 176, y: 214 }, radius: { x: 272, y: 105, r: 46 } }) +
      '<div class="map-top"><div class="srch">' + I('search') + 'Buscar lugar ou endereço</div><span class="fab-s">' + I('list') + '</span></div></div>' +
      '<div class="sheet" style="padding-bottom:16px"><div class="grab"></div>' +
      '<div style="display:flex;justify-content:space-between;align-items:flex-start"><div><div class="h2">Trabalho</div><div class="sub">Av. Paulista, 1578 · <span class="mono">2,4 km</span></div></div><span class="ib s">' + I('x') + '</span></div>' +
      '<div class="stats"><span><b>3</b>tarefas</span><span><b>1</b>memória</span><span><b>3</b>gatilhos</span></div>' +
      '<div style="margin-top:6px">' + row({ t: 'Enviar relatório mensal', tight: true, meta: [m('clock', 'até 10:00')] }) + row({ t: 'Pegar documento no RH', tight: true }) + '</div>' +
      '<div style="margin-top:10px">' + btn('Abrir lugar') + '</div></div>' }) });

  add({ id: 'T21', group: 'Lugares', name: 'Lugares · vazio',
    goal: 'Cadastrar o primeiro lugar.', action: 'Adicionar lugar', cut: 'Sugestões em vez de formulário.',
    html: phone({ tab: 'lugares', flush: true, body: '<div class="map-wrap" style="position:absolute;inset:0">' + map({ me: { x: 176, y: 214 } }) +
      '<div class="map-top"><div class="srch">' + I('search') + 'Buscar lugar ou endereço</div></div></div>' +
      '<div class="sheet" style="padding-bottom:16px"><div class="grab"></div><div class="h2">Onde suas coisas acontecem?</div>' +
      '<div class="sub">Salve os lugares que você frequenta. Comece por estes:</div>' +
      '<div class="chips" style="margin-top:14px">' + chip('Casa', false, 'home') + chip('Trabalho', false, 'work') + chip('Supermercado', false, 'cart') + chip('Carro', false, 'car') + chip('Academia', false, 'gym') + '</div>' +
      '<div style="margin-top:18px">' + btn('Adicionar lugar atual', 'p', 'target') + '</div></div>' }) });

  add({ id: 'T22', group: 'Lugares', name: 'Lugar · Supermercado', main: true,
    goal: 'Ver e organizar tudo que pertence a um lugar.', action: 'Adicionar item neste lugar.', cut: 'Configurações do lugar (raio, nome) ficam atrás de "Editar".',
    html: phone({ body: '<div class="pd-map">' + map({ vb: '70 150 200 100', pins: [{ x: 170, y: 206, ic: 'cart', active: true }], radius: { x: 170, y: 206, r: 34 }, me: { x: 182, y: 214 } }) +
      '<div class="map-top" style="justify-content:space-between"><span class="fab-s">' + I('chevL') + '</span><span class="fab-s">' + I('edit') + '</span></div></div>' +
      '<div class="pd-head">' + tag('Você está aqui', 'acc', 'now') + '<div class="h1" style="margin-top:8px">Supermercado</div><div class="sub">Pão de Açúcar · R. Augusta, 1200 · raio <span class="mono">80 m</span></div></div>' +
      sec('Tarefas', 2, '+ Adicionar') + row({ t: 'Comprar café', tight: true, meta: [m('repeat', 'Semanal')] }) + row({ t: 'Comprar sabão em pó', tight: true }) +
      sec('Memórias', 1, '+ Adicionar') + row({ kind: 'mem', tight: true, t: 'Ver preço do arroz', meta: ['último: R$ 27,90 em 10 set'] }) +
      sec('Quando lembrar', 2, '+ Gatilho') +
      '<div class="trg-li">' + I('arrive') + '<div class="b">Ao chegar<small>Depois de 2 min no local</small></div>' + tg(true) + '</div>' +
      '<div class="trg-li">' + I('leave') + '<div class="b">Ao sair<small>Se algo ficou pendente</small></div>' + tg(true) + '</div>' }) });

  add({ id: 'T23', group: 'Lugares', name: 'Novo lugar',
    goal: 'Salvar um lugar com o mínimo de esforço.', action: 'Salvar lugar', cut: 'Endereço é preenchido pelo mapa. Wi-Fi é sugerido quando o aparelho está conectado.',
    html: phone({ flush: true, body: '<div class="map-wrap" style="position:absolute;inset:0 0 360px 0">' + map({ vb: '230 250 180 150', radius: { x: 320, y: 330, r: 40 }, pins: [{ x: 320, y: 330, ic: 'tool', active: true }] }) +
      '<div class="map-top" style="justify-content:space-between"><span class="fab-s">' + I('x') + '</span><span class="srch" style="flex:0 1 auto">R. Cardeal Arcoverde, 412</span></div></div>' +
      '<div class="sheet" style="padding-bottom:16px;box-shadow:none;border-top:1px solid var(--a-line)">' +
      '<div class="fl-l">Nome</div><div class="fld focus"><span class="v">Oficina do Marcos</span></div>' +
      '<div class="chips nowrap" style="margin-top:12px">' + chip('', true, 'tool') + chip('', false, 'home') + chip('', false, 'work') + chip('', false, 'cart') + chip('', false, 'friends') + chip('', false, 'pill') + chip('', false, 'gym') + '</div>' +
      '<div class="fl"><div class="fl-l">Raio<span class="fl-o mono">80 m</span></div><div style="height:22px;display:flex;align-items:center"><div style="flex:1;height:4px;border-radius:2px;background:linear-gradient(90deg,var(--a-accent) 32%,var(--a-surface-2) 32%);position:relative"><span style="position:absolute;left:32%;top:50%;width:22px;height:22px;margin:-11px;border-radius:50%;background:var(--a-bg);box-shadow:var(--a-shadow);border:1px solid var(--a-line)"></span></div></div></div>' +
      '<div class="fl"><div class="fl-l">Reconhecer também por<span class="fl-o">opcional</span></div><div class="chips">' + chip('Wi-Fi Oficina_2G', false, 'wifi', 'acc') + chip('Tag NFC', false, 'nfc') + chip('QR Code', false, 'qr') + '</div></div>' +
      '<div style="margin-top:16px">' + btn('Salvar lugar') + '</div></div>' }) });

  /* ---------- Criação ---------- */
  add({ id: 'T24', group: 'Criação', name: 'Adicionar',
    goal: 'Escolher o tipo certo sem dúvida.', action: 'Tarefa (mais usada, primeiro).', cut: 'Captura rápida no topo: escreve e salva como memória aqui, em 2 toques.',
    html: phone({ tab: 'agora', time: '18:12', body: homeMercado, overlay: sheet(
      '<div class="fld" style="background:var(--a-surface)">' + I('pin') + '<span class="v ph-t">Anotar algo aqui no Supermercado…</span></div>' +
      '<div style="margin-top:10px">' +
      '<div class="add-opt"><span class="add-ic t">' + I('check') + '</span><div class="add-b"><b>Tarefa</b><small>Algo para fazer em um lugar</small></div>' + I('chevR') + '</div>' +
      '<div class="add-opt"><span class="add-ic m">' + I('memory') + '</span><div class="add-b"><b>Memória</b><small>Algo para ver ou anotar quando estiver lá</small></div>' + I('chevR') + '</div>' +
      '<div class="add-opt"><span class="add-ic g">' + I('bolt') + '</span><div class="add-b"><b>Gatilho</b><small>Quando algo acontecer, o app age</small></div>' + I('chevR') + '</div>' +
      '<div class="add-opt"><span class="add-ic g">' + I('pin') + '</span><div class="add-b"><b>Lugar</b><small>Endereço, carro, Wi-Fi ou tag NFC</small></div>' + I('chevR') + '</div></div>') }) });

  add({ id: 'T25', group: 'Criação', name: 'Nova tarefa', main: true,
    goal: 'Criar uma tarefa presa a um lugar em segundos.', action: 'Salvar tarefa', cut: 'Prazo, nota e lista ficam em "Mais opções". O lugar atual já vem selecionado.',
    html: phone({ body: top({ left: ib('x'), title: 'Nova tarefa' }) +
      '<div class="title-in">Comprar café<span class="caret"></span></div>' +
      '<div class="fl"><div class="fl-l">Onde</div><div class="chips nowrap">' + chip('Supermercado', true, 'cart') + chip('Casa', false, 'home') + chip('Trabalho', false, 'work') + chip('Outro', false, 'plus', 'dash') + '</div></div>' +
      '<div class="fl"><div class="fl-l">Quando lembrar</div><div class="opts">' +
      opt({ icon: 'arrive', t: 'Ao chegar', s: 'Depois de 2 min no local', radio: true }) +
      opt({ icon: 'leave', t: 'Ao sair', s: 'Só se ainda estiver pendente', radio: false }) +
      opt({ icon: 'clock', t: 'Em um horário', radio: false }) +
      opt({ icon: 'eyeoff', t: 'Sem aviso', s: 'Aparece só quando eu abrir o lugar', radio: false }) + '</div></div>' +
      '<div class="fl"><div class="fl-l">Repetir</div>' + seg(['Uma vez', 'Diária', 'Semanal', 'Mensal'], 2) + '</div>' +
      '<div class="foot line"><div class="summary">' + I('bell') + '<span>Vamos lembrar você <b>ao chegar no Supermercado</b>, toda semana.</span></div>' + btn('Salvar tarefa') + '</div>' }) });

  add({ id: 'T26', group: 'Criação', name: 'Escolher lugar',
    goal: 'Achar o lugar certo, ou criar um novo ali mesmo.', action: 'Tocar em um lugar.', cut: '',
    html: phone({ body: top({ left: ib('chevL'), title: 'Onde?' }) +
      '<div class="fld">' + I('search') + '<span class="v ph-t">Buscar lugar ou endereço</span></div>' +
      sec('Seus lugares') +
      place({ icon: 'cart', name: 'Supermercado', meta: 'Você está aqui', here: true }) + place({ icon: 'home', name: 'Casa', meta: 'R. Harmonia, 88', dist: '1,1 km' }) +
      place({ icon: 'work', name: 'Trabalho', meta: 'Av. Paulista, 1578', dist: '2,4 km' }) + place({ icon: 'car', name: 'Carro', meta: 'Bluetooth HB20' }) +
      sec('Qualquer lugar do tipo') +
      place({ icon: 'pill', name: 'Qualquer farmácia', meta: 'Lembra na farmácia mais perto de você' }) +
      '<div class="qa">' + I('plus') + 'Novo lugar</div>' }) });

  add({ id: 'T27', group: 'Criação', name: 'Nova memória', main: true,
    goal: 'Guardar uma informação para ver no lugar certo.', action: 'Salvar memória', cut: 'Sem prazo e sem caixa de seleção: memória não é tarefa.',
    html: phone({ body: top({ left: ib('x'), title: 'Nova memória' }) +
      tag('Memória', 'mem', 'memory') +
      '<div class="title-in">Medir parede da sala<span class="caret" style="background:var(--a-mem)"></span></div>' +
      '<div class="fl"><div class="fl-l">Presa a</div><div class="map-wrap" style="height:120px;border-radius:16px">' + map({ vb: '13 222 150 56', pins: [{ x: 88, y: 250, ic: 'home', active: true }], radius: { x: 88, y: 250, r: 24 } }) + '</div>' +
      '<div style="display:flex;justify-content:space-between;align-items:center;margin-top:10px"><span style="font-weight:600">' + '<span style="display:inline-flex;gap:8px;align-items:center">' + I('home') + 'Casa</span></span><span class="link">Trocar</span></div></div>' +
      '<div class="fl"><div class="fl-l">Mostrar</div>' + seg(['Sempre que estiver lá', 'Só na próxima'], 0) + '</div>' +
      '<div class="fl"><div class="fl-l">Guardar junto<span class="fl-o">opcional</span></div><div class="chips">' + chip('Nota', false, 'note') + chip('Foto', false, 'camera') + chip('Campo para anotar lá', true, 'edit') + '</div></div>' +
      '<div class="foot line"><div class="summary"><span style="color:var(--a-mem)">' + I('memory') + '</span><span>Aparece na tela Agora <b>sempre que você estiver em Casa</b>. Não tem prazo.</span></div>' + btn('Salvar memória') + '</div>' }) });

  add({ id: 'T28', group: 'Criação', name: 'Memória no local',
    goal: 'Usar a informação no momento certo.', action: 'Anotar o valor e arquivar.', cut: '',
    html: phone({ body: top({ left: ib('chevL'), right: ib('more') }) +
      tag('Memória · Casa', 'mem', 'memory') + '<div class="h1" style="margin-top:10px">Anotar o número do medidor</div>' +
      '<div class="sub">Você está em Casa agora · criada há 5 dias</div>' +
      '<div class="cap" style="margin-top:22px"><div class="cap-l">Leitura do medidor</div><div class="cap-v">04821<span class="caret"></span><span class="u">kWh</span></div></div>' +
      '<div class="card" style="margin-top:12px;display:flex;gap:12px">' + I('note') + '<span style="font-size:15px;color:var(--a-ink-2)">Fica na garagem, atrás do portão lateral. Leitura anterior: <span class="mono">04577</span>.</span></div>' +
      '<div class="foot">' + btn('Salvar e arquivar') + '<div class="btn btn-g" style="height:44px;margin-top:4px">Manter para a próxima vez</div></div>' }) });

  const sentence = (when, then) => '<div class="sentence"><span class="k">Quando</span>' + when + '<br><span class="k">Então</span>' + then + '</div>';
  const tk = (ic, t, empty) => '<span class="tk' + (empty ? ' empty' : '') + '">' + (ic ? I(ic) : '') + t + '</span>';
  const tgrid = (on, dis, btOff) => '<div class="tgrid">' + [
    ['arrive', 'Chegar'], ['leave', 'Sair'], ['clock', 'Horário'], ['car', 'Carro'], ['wifi', 'Wi-Fi'], ['bt', 'Bluetooth'], ['qr', 'QR Code'], ['nfc', 'NFC'], ['routine', 'Rotina']
  ].map((x) => '<div class="tcell' + (x[0] === on ? ' on' : '') + (dis && x[0] === dis ? ' dis' : '') + '">' + I(x[0]) + x[1] + (dis && x[0] === dis ? '<small>Sem NFC</small>' : '') + '</div>').join('') + '</div>';

  add({ id: 'T29', group: 'Criação', name: 'Novo gatilho · Quando',
    goal: 'Escolher o que dispara o lembrete.', action: 'Escolher uma condição.', cut: 'A frase no topo substitui a explicação técnica.',
    html: phone({ body: top({ left: ib('x'), title: 'Novo gatilho', right: '<span class="dist">1 de 2</span>' }) +
      sentence(tk('car', 'conectar ao carro'), tk(null, 'escolha a ação', true)) +
      sec('Quando…') + '<div style="margin-top:8px">' + tgrid('car') + '</div>' +
      '<div class="fl"><div class="fl-l">Qual carro?</div><div class="opts">' +
      opt({ icon: 'bt', t: 'HB20 · Multimídia', s: 'Pareado', radio: true }) + opt({ icon: 'bt', t: 'Outro dispositivo…', radio: false }) + '</div></div>' +
      '<div class="foot">' + btn('Continuar') + '</div>' }) });

  add({ id: 'T30', group: 'Criação', name: 'Novo gatilho · Então',
    goal: 'Dizer o que o app faz quando o gatilho acontece.', action: 'Criar gatilho', cut: 'Condições avançadas desligadas por padrão.',
    html: phone({ body: top({ left: ib('chevL'), title: 'Novo gatilho', right: '<span class="dist">2 de 2</span>' }) +
      sentence(tk('car', 'conectar ao HB20'), tk('list', 'mostrar tarefas do Carro')) +
      sec('Então…') + '<div class="opts" style="margin-top:8px">' +
      opt({ icon: 'bell', t: 'Mostrar um lembrete', radio: false }) +
      opt({ icon: 'list', t: 'Mostrar tarefas de um lugar', s: 'Carro · 2 tarefas', radio: true }) +
      opt({ icon: 'memory', t: 'Mostrar uma memória', radio: false }) +
      opt({ icon: 'check', t: 'Perguntar se concluí', radio: false }) +
      opt({ icon: 'layers', t: 'Abrir uma lista', radio: false }) + '</div>' +
      sec('Condições') +
      '<div class="trg-li">' + I('calendar') + '<div class="b">Só em dias úteis</div>' + tg(false) + '</div>' +
      '<div class="trg-li">' + I('repeat') + '<div class="b">No máximo 1 vez por dia</div>' + tg(true) + '</div>' +
      '<div class="foot line">' + btn('Criar gatilho') + '</div>' }) });

  /* ---------- Lista ---------- */
  add({ id: 'T31', group: 'Lista', name: 'Lista · todas',
    goal: 'Revisar tudo de vez em quando.', action: 'Filtrar ou abrir um item.', cut: 'Agrupada por lugar, para manter a ideia de contexto até aqui.',
    html: phone({ tab: 'lista', body: top({ left: '<span class="h2">Lista</span>', right: ib('search') }) +
      seg(['Tarefas · 9', 'Memórias · 4'], 0) +
      '<div class="chips nowrap" style="margin-top:12px">' + chip('Todas', true) + chip('Hoje') + chip('Por lugar') + chip('Atrasadas · 1') + chip('Concluídas') + '</div>' +
      sec('Supermercado · aqui', 2) + row({ t: 'Comprar café', tight: true, meta: [m('repeat', 'Semanal')] }) + row({ t: 'Comprar sabão em pó', tight: true }) +
      sec('Trabalho', 3) + row({ t: 'Enviar relatório mensal', tight: true, meta: [m('clock', 'seg, 10:00')] }) + row({ t: 'Pegar documento no RH', tight: true }) + row({ t: 'Falar com João', tight: true }) +
      sec('Casa', 2) + row({ t: 'Pagar conta de luz', tight: true, late: true, meta: ['<span class="late">Venceu ontem</span>'] }) }) });

  add({ id: 'T32', group: 'Lista', name: 'Lista · atrasadas (vazio)',
    goal: 'Confirmar que nada está atrasado.', action: 'Voltar para Todas.', cut: '',
    html: phone({ tab: 'lista', body: top({ left: '<span class="h2">Lista</span>', right: ib('search') }) +
      seg(['Tarefas · 9', 'Memórias · 4'], 0) +
      '<div class="chips nowrap" style="margin-top:12px">' + chip('Todas') + chip('Hoje') + chip('Por lugar') + chip('Atrasadas', true) + chip('Concluídas') + '</div>' +
      '<div style="margin-top:80px">' + empty(art('check', 'ok'), 'Nada atrasado', 'Tudo que tinha prazo foi resolvido. O resto espera o lugar certo, sem pressa.') + '</div>' }) });

  /* ---------- Rotinas ---------- */
  add({ id: 'T33', group: 'Rotinas', name: 'Rotinas',
    goal: 'Ver e ligar/desligar sequências automáticas.', action: 'Ativar uma rotina.', cut: 'Cada cartão mostra o gatilho e os passos em uma linha.',
    html: phone({ tab: 'rotinas', body: top({ left: '<span class="h2">Rotinas</span>', right: ib('plus', true) }) +
      '<div class="sub" style="margin-top:0">Sequências que começam sozinhas quando algo acontece.</div>' +
      '<div class="rt"><div class="rt-h"><b>Cheguei em casa</b>' + tg(true) + '</div><div class="rt-w">' + I('arrive') + 'Ao chegar em Casa · depois das 17h</div><div class="rt-steps">' + tag('Guardar compras') + tag('Alimentar o Thor') + tag('Rotina da noite') + '</div></div>' +
      '<div class="rt"><div class="rt-h"><b>Saindo para o trabalho</b>' + tg(true) + '</div><div class="rt-w">' + I('leave') + 'Ao sair de Casa · dias úteis</div><div class="rt-steps">' + tag('Crachá') + tag('Marmita') + tag('Garrafa') + '</div></div>' +
      '<div class="rt"><div class="rt-h"><b>Entrei no carro</b>' + tg(true) + '</div><div class="rt-w">' + I('car') + 'Ao conectar ao HB20</div><div class="rt-steps">' + tag('Tarefas do Carro') + tag('Lista: no caminho') + '</div></div>' +
      '<div class="rt off"><div class="rt-h"><b>Treino</b>' + tg(false) + '</div><div class="rt-w">' + I('nfc') + 'Ao encostar na tag do armário</div></div>' +
      sec('Modelos') + '<div class="chips nowrap" style="margin-top:8px">' + chip('Fim do expediente', false, 'plus') + chip('Mercado da semana', false, 'plus') + chip('Viagem', false, 'plus') + '</div>' }) });

  add({ id: 'T34', group: 'Rotinas', name: 'Rotina · editar',
    goal: 'Montar a sequência na ordem certa.', action: 'Adicionar passo.', cut: 'Uma só linha do tempo vertical; sem blocos de programação.',
    html: phone({ body: top({ left: ib('chevL'), right: '<span class="link">Salvar</span>' }) +
      '<div style="display:flex;justify-content:space-between;align-items:center"><div class="h1">Cheguei em casa</div>' + tg(true) + '</div>' +
      '<div class="sub">3 passos · rodou 4 vezes esta semana</div>' +
      '<div class="flow" style="margin-top:18px">' +
      '<div class="fs"><span class="fs-dot trg">' + I('arrive') + '</span><div class="fs-k">Quando</div><div class="fs-t">Chegar em Casa<small>Depois das 17h · todos os dias</small></div></div>' +
      '<div class="fs"><span class="fs-dot">1</span><div class="fs-k">Lembrar</div><div class="fs-t">Guardar compras<small>Só se você passou no Supermercado hoje</small></div></div>' +
      '<div class="fs"><span class="fs-dot">2</span><div class="fs-k">Mostrar tarefa</div><div class="fs-t">Alimentar o Thor<small>Repete todo dia</small></div></div>' +
      '<div class="fs"><span class="fs-dot">3</span><div class="fs-k">Iniciar rotina</div><div class="fs-t">Rotina da noite<small>3 passos · às 21:00</small></div></div>' +
      '<div class="fs add"><span class="fs-dot">' + I('plus') + '</span><div class="fs-t" style="padding-top:5px">Adicionar passo</div></div></div>' +
      '<div class="foot">' + btn('Testar agora', 'o', 'play') + '</div>' }) });

  /* ---------- Você ---------- */
  add({ id: 'T35', group: 'Você', name: 'Você · ajustes',
    goal: 'Controlar como e quando o app avisa.', action: 'Ajustar detecção e avisos.', cut: 'Sem conta obrigatória. Status em linguagem simples.',
    html: phone({ body: top({ left: ib('chevL'), title: 'Você' }) +
      '<div style="display:flex;gap:14px;align-items:center;margin-top:4px"><span class="av" style="width:52px;height:52px;font-size:17px">LA</span><div><div style="font-weight:600;font-size:17px">Lucas Almeida</div><div class="sub" style="margin-top:0">Dados salvos só neste aparelho</div></div></div>' +
      sec('Detecção') +
      '<div class="set">' + I('pin') + '<div class="set-b">Localização</div><span class="st ok">Sempre</span></div>' +
      '<div class="set">' + I('bt') + '<div class="set-b">Bluetooth</div><span class="st ok">Ativo</span></div>' +
      '<div class="set">' + I('wifi') + '<div class="set-b">Wi-Fi</div><span class="st ok">Ativo</span></div>' +
      '<div class="set">' + I('nfc') + '<div class="set-b">NFC</div><span class="st na">Indisponível</span></div>' +
      sec('Avisos') +
      '<div class="set">' + I('clock') + '<div class="set-b">Tempo mínimo no lugar<small>Evita aviso quando você só passa perto</small></div><span class="set-v mono">2 min' + I('chevR') + '</span></div>' +
      '<div class="set">' + I('moon') + '<div class="set-b">Horário silencioso</div><span class="set-v mono">22–07h' + I('chevR') + '</span></div>' +
      '<div class="set">' + I('layers') + '<div class="set-b">Agrupar avisos da chegada</div>' + tg(true) + '</div>' +
      sec('Privacidade') +
      '<div class="set">' + I('eyeoff') + '<div class="set-b">Guardar histórico de visitas</div>' + tg(false) + '</div>' }) });

  /* ---------- Erros e permissões ---------- */
  add({ id: 'T36', group: 'Erros e permissões', name: 'Agora · localização negada',
    goal: 'Entender o que parou de funcionar e como resolver.', action: 'Ativar localização', cut: 'O app continua útil: oferece alternativas e escolha manual.',
    html: phone({ tab: 'agora', body: homeTop() +
      hero('Agora', 'Onde você está?', '<span>Localização desativada</span>', 'warn', true) +
      '<div style="margin-top:14px">' + banner('warn', 'locoff', 'Não conseguimos perceber quando você chega', 'Sem localização, avisos de chegada e saída ficam pausados.', ['Ativar localização', 'Por quê?']) + '</div>' +
      sec('Escolha onde está') + '<div class="chips" style="margin-top:8px">' + chip('Casa', false, 'home') + chip('Trabalho', false, 'work') + chip('Supermercado', false, 'cart') + '</div>' +
      sec('Continua funcionando') +
      '<div class="set">' + I('wifi') + '<div class="set-b">Wi-Fi<small>Trabalho reconhece o Escritorio-5G</small></div><span class="st ok">Ativo</span></div>' +
      '<div class="set">' + I('bt') + '<div class="set-b">Bluetooth do carro</div><span class="st ok">Ativo</span></div>' +
      '<div class="set">' + I('clock') + '<div class="set-b">Horários</div><span class="st ok">Ativo</span></div>' }) });

  add({ id: 'T37', group: 'Erros e permissões', name: 'Permissões',
    goal: 'Ver o que falta e corrigir em um toque.', action: 'Corrigir cada item.', cut: 'Cada permissão diz o que libera, em vez do nome técnico.',
    html: phone({ body: top({ left: ib('chevL'), title: 'Permissões' }) +
      '<div class="h2">O que o Roteiro consegue perceber</div><div class="sub">Cada permissão libera um tipo de lembrete. Nada sai do seu aparelho.</div>' +
      '<div class="card o" style="margin-top:16px"><div class="set" style="padding-top:0">' + I('pin') + '<div class="set-b">Localização<small>Só enquanto usa o app</small></div><span class="st mid">Parcial</span></div>' +
      '<div style="font-size:14px;color:var(--a-ink-2);padding-top:10px">Para avisar ao chegar ou sair, escolha <b>Permitir sempre</b>.</div><div style="margin-top:10px">' + btn('Ajustar', 's', null, 'sm') + '</div></div>' +
      '<div class="card o" style="margin-top:10px"><div class="set" style="padding:0;border:0">' + I('bt') + '<div class="set-b">Bluetooth<small>Desligado · o carro não será reconhecido</small></div>' + btn('Ligar', 's', null, 'sm') + '</div></div>' +
      '<div class="card o" style="margin-top:10px"><div class="set" style="padding:0;border:0">' + I('battery') + '<div class="set-b">Economia de bateria<small>Pode atrasar avisos em até 15 min</small></div>' + btn('Resolver', 's', null, 'sm') + '</div></div>' +
      '<div class="card o" style="margin-top:10px"><div class="set" style="padding:0;border:0">' + I('bell') + '<div class="set-b">Notificações</div><span class="st ok">Ativas</span></div></div>' +
      '<div class="card o" style="margin-top:10px"><div class="set" style="padding:0;border:0">' + I('nfc') + '<div class="set-b">NFC<small>Este aparelho não tem NFC. Use QR Code.</small></div><span class="st na">—</span></div></div>' }) });

  add({ id: 'T38', group: 'Erros e permissões', name: 'Gatilho indisponível',
    goal: 'Saber por que uma opção não funciona.', action: 'Ligar Bluetooth', cut: 'Opções impossíveis aparecem apagadas com o motivo, nunca somem.',
    html: phone({ body: top({ left: ib('x'), title: 'Novo gatilho', right: '<span class="dist">1 de 2</span>' }) +
      sentence(tk('car', 'conectar ao carro'), tk(null, 'escolha a ação', true)) +
      sec('Quando…') + '<div style="margin-top:8px">' + tgrid('car', 'nfc') + '</div>' +
      '<div style="margin-top:16px">' + banner('warn', 'bt', 'Bluetooth desligado', 'Ligue para o Roteiro perceber quando você entra no carro.', ['Ligar Bluetooth']) + '</div>' +
      '<div class="foot">' + btn('Continuar', 'p', null, 'dis') + '</div>' }) });

  window.SCREENS = S;
  window.ART = { obArt1: obArt1, obArt2: obArt2, obArt3: obArt3, obArtPerm: obArtPerm, art: art };
})();
