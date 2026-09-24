/* Contexto — conjunto de ícones (24×24, traço 1.75, pontas arredondadas).
   Uso: icon('pin') devolve o <svg> pronto. */
(function () {
  const P = {
    now: '<circle cx="12" cy="12" r="8.25"/><circle cx="12" cy="12" r="3" fill="currentColor" stroke="none"/>',
    places: '<path d="M9 4.5 3.5 6.8v12.7L9 17.2l6 2.3 5.5-2.3V4.5L15 6.8z"/><path d="M9 4.5v12.7M15 6.8v12.7"/>',
    list: '<path d="M9.5 6.5h10.5M9.5 12h10.5M9.5 17.5h10.5"/><path d="M4 6.5h.01M4 12h.01M4 17.5h.01" stroke-width="2.6"/>',
    routine: '<circle cx="6" cy="6" r="2.4"/><circle cx="18" cy="18" r="2.4"/><path d="M8.4 6H15a3 3 0 0 1 0 6H9a3 3 0 0 0 0 6h6.6"/>',
    plus: '<path d="M12 5v14M5 12h14"/>',
    check: '<path d="m5 12.5 4.5 4.5L19 7.5"/>',
    pin: '<path d="M12 21s-7-6.2-7-11.5a7 7 0 0 1 14 0C19 14.8 12 21 12 21z"/><circle cx="12" cy="9.5" r="2.5"/>',
    memory: '<path d="M6.5 3.8h11v16.7L12 16.3l-5.5 4.2z"/>',
    bolt: '<path d="M13 3 5 13.5h6L10.5 21 19 10.5h-6z"/>',
    clock: '<circle cx="12" cy="12" r="8.5"/><path d="M12 7.5V12l3 2"/>',
    car: '<path d="M4.5 15.5V12l1.9-4.8A1.8 1.8 0 0 1 8.1 6h7.8a1.8 1.8 0 0 1 1.7 1.2l1.9 4.8v3.5a1 1 0 0 1-1 1h-13a1 1 0 0 1-1-1z"/><path d="M4.5 12h15M7 16.5v2M17 16.5v2"/><path d="M8 13.9h.01M16 13.9h.01" stroke-width="2.4"/>',
    wifi: '<path d="M3.5 9.2a12.5 12.5 0 0 1 17 0M6.5 12.6a8 8 0 0 1 11 0M9.4 16a3.6 3.6 0 0 1 5.2 0"/><path d="M12 19.2h.01" stroke-width="2.6"/>',
    bt: '<path d="m7 7.5 10 9-5 4v-17l5 4-10 9"/>',
    nfc: '<path d="M6.5 9.5a4 4 0 0 1 0 5M9.8 7.3a8 8 0 0 1 0 9.4M13.1 5a12 12 0 0 1 0 14M16.4 3a15.5 15.5 0 0 1 0 18"/>',
    qr: '<rect x="4" y="4" width="6" height="6" rx="1.2"/><rect x="14" y="4" width="6" height="6" rx="1.2"/><rect x="4" y="14" width="6" height="6" rx="1.2"/><path d="M14 14h2.5v2.5H14zM17.5 17.5H20V20h-2.5zM14 19.5v.5M20 14v.5"/>',
    bell: '<path d="M6 16v-5a6 6 0 0 1 12 0v5l1.5 2h-15z"/><path d="M10 20.5a2 2 0 0 0 4 0"/>',
    chevR: '<path d="m9.5 6 6 6-6 6"/>',
    chevL: '<path d="m14.5 6-6 6 6 6"/>',
    chevD: '<path d="m6 9.5 6 6 6-6"/>',
    x: '<path d="M6.5 6.5l11 11M17.5 6.5l-11 11"/>',
    more: '<path d="M5.5 12h.01M12 12h.01M18.5 12h.01" stroke-width="2.8"/>',
    sliders: '<path d="M4 7h9M17 7h3M4 17h3M11 17h9"/><circle cx="15" cy="7" r="2"/><circle cx="9" cy="17" r="2"/>',
    user: '<circle cx="12" cy="8.5" r="3.6"/><path d="M5 20a7 7 0 0 1 14 0"/>',
    shield: '<path d="M12 3.5 5 6v5.5c0 4.3 3 7.7 7 9 4-1.3 7-4.7 7-9V6z"/><path d="m9 12 2.2 2.2L15.5 10"/>',
    arrive: '<path d="M3.5 12h11M11 8l4 4-4 4"/><path d="M14 4h4a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-4"/>',
    leave: '<path d="M10 4H6a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h4"/><path d="M9.5 12h11M17 8l4 4-4 4"/>',
    repeat: '<path d="M4 11V9.5A3.5 3.5 0 0 1 7.5 6H19l-3-3M20 13v1.5a3.5 3.5 0 0 1-3.5 3.5H5l3 3"/>',
    calendar: '<rect x="4" y="5.5" width="16" height="14.5" rx="2.5"/><path d="M4 10h16M8.5 3.5v4M15.5 3.5v4"/>',
    search: '<circle cx="11" cy="11" r="6.5"/><path d="m16 16 4 4"/>',
    alert: '<path d="M10.3 4.6 2.9 17.5A2 2 0 0 0 4.6 20.5h14.8a2 2 0 0 0 1.7-3L13.7 4.6a2 2 0 0 0-3.4 0z"/><path d="M12 9.5v4.5M12 17h.01"/>',
    locoff: '<path d="M8.5 5A7 7 0 0 1 19 9.5c0 1.8-.8 3.6-1.8 5.2M14.6 17.8C13.2 19.7 12 21 12 21s-7-6.2-7-11.5c0-.9.2-1.8.5-2.6"/><path d="M3.5 3.5l17 17"/>',
    home: '<path d="M4 10.5 12 4l8 6.5V20H4z"/><path d="M10 20v-5.5h4V20"/>',
    work: '<rect x="3.5" y="7.5" width="17" height="12" rx="2.2"/><path d="M9 7.5v-2A1.5 1.5 0 0 1 10.5 4h3A1.5 1.5 0 0 1 15 5.5v2M3.5 12.5h17"/>',
    cart: '<path d="M3.5 4.5h2.2l2.1 10.2h10.1l2-7.4H6.4"/><circle cx="9" cy="19" r="1.4"/><circle cx="16.5" cy="19" r="1.4"/>',
    tool: '<path d="M14.5 4.2a4.8 4.8 0 0 0-4.3 6.6L4.2 16.8a1.7 1.7 0 0 0 2.4 2.4l6-6a4.8 4.8 0 0 0 6.6-4.3l-2.9 1.1-2.4-2.4z"/>',
    pill: '<path d="M10.1 20.3a4.9 4.9 0 0 1-6.9-6.9l6.7-6.7a4.9 4.9 0 0 1 6.9 6.9z"/><path d="m8.5 8.5 7 7"/>',
    gym: '<path d="M6.5 7v10M17.5 7v10M3.5 9.5v5M20.5 9.5v5M6.5 12h11"/>',
    friends: '<circle cx="9" cy="9" r="3.2"/><path d="M3.5 19.5a5.5 5.5 0 0 1 11 0"/><path d="M15.5 6.2a3 3 0 0 1 0 5.8M17 14.3a5.5 5.5 0 0 1 3.5 5.2"/>',
    note: '<path d="M6 3.5h8l4 4v13H6z"/><path d="M14 3.5v4h4M9 12.5h6M9 16h4"/>',
    camera: '<path d="M4 8.2h3l1.6-2.7h6.8L17 8.2h3V19H4z"/><circle cx="12" cy="13.2" r="3.4"/>',
    edit: '<path d="M4 20h4L19 9l-4-4L4 16z"/><path d="m13.5 6.5 4 4"/>',
    moon: '<path d="M19 14.5A7.5 7.5 0 0 1 9.5 5a7.5 7.5 0 1 0 9.5 9.5z"/>',
    battery: '<rect x="3" y="8" width="16" height="8" rx="2.2"/><path d="M21.5 11v2M6 11v2"/>',
    trash: '<path d="M5 7h14M9.5 7V4.5h5V7M6.8 7l1 13h8.4l1-13"/>',
    radius: '<circle cx="12" cy="12" r="8.5" stroke-dasharray="2.6 2.6"/><circle cx="12" cy="12" r="2" fill="currentColor" stroke="none"/>',
    play: '<path d="M8 5.5v13l10-6.5z"/>',
    grip: '<path d="M9 6h.01M15 6h.01M9 12h.01M15 12h.01M9 18h.01M15 18h.01" stroke-width="2.6"/>',
    export: '<path d="M12 4v11M7.5 10.5 12 15l4.5-4.5M5 19.5h14"/>',
    info: '<circle cx="12" cy="12" r="8.5"/><path d="M12 11v5M12 8h.01"/>',
    lock: '<rect x="5" y="10.5" width="14" height="10" rx="2.2"/><path d="M8.5 10.5V8a3.5 3.5 0 0 1 7 0v2.5"/>',
    snooze: '<path d="M12 21s-7-6.2-7-11.5a7 7 0 0 1 12.2-4.7"/><path d="M14 9.5h5.5L14 15.5h5.5"/>',
    eyeoff: '<path d="M3.5 3.5l17 17M10.5 5.2A9.6 9.6 0 0 1 12 5c5 0 8.5 4.6 9 7-.2 1-.9 2.3-2 3.6M6.3 6.8C4.3 8.1 3.3 10.2 3 12c.5 2.4 4 7 9 7 1.6 0 3-.4 4.2-1.1"/><path d="M9.9 10a3 3 0 0 0 4.1 4.2"/>',
    layers: '<path d="m12 4 8.5 4.5L12 13 3.5 8.5z"/><path d="m3.5 12.5 8.5 4.5 8.5-4.5M3.5 16.5 12 21l8.5-4.5"/>',
    target: '<circle cx="12" cy="12" r="7.5"/><path d="M12 2.5v4M12 17.5v4M2.5 12h4M17.5 12h4"/><circle cx="12" cy="12" r="1.6" fill="currentColor" stroke="none"/>',
    arrowR: '<path d="M5 12h14M13.5 6.5 19 12l-5.5 5.5"/>',
    signal: '<path d="M4 18v-2M9 18v-5M14 18v-8M19 18V6"/>'
  };

  function icon(name, cls) {
    const body = P[name] || P.info;
    return '<svg class="i' + (cls ? ' ' + cls : '') + '" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">' + body + '</svg>';
  }

  /* Ícone para ser aninhado dentro de outro SVG (mapa). */
  function iconAt(name, x, y, size, color) {
    return '<svg x="' + x + '" y="' + y + '" width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" stroke="' + color + '" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">' + (P[name] || '') + '</svg>';
  }

  window.ICONS = P;
  window.icon = icon;
  window.iconAt = iconAt;
})();
