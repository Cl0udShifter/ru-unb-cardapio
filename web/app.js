// RU UnB - Progressive Web App (PWA)
// Client-side application logic

const CAMPUS_NAMES = {
  darcy: 'Darcy Ribeiro',
  gama: 'FGA (Gama)',
  ceilandia: 'FCE (Ceilândia)',
  planaltina: 'FUP (Planaltina)'
};

const MEAL_TIMES = {
  cafe: 'Café da Manhã • 07h às 09h30',
  almoco: 'Almoço • 11h às 14h30',
  jantar: 'Jantar • 17h às 19h30'
};

const RAW_GITHUB_BASE = 'https://raw.githubusercontent.com/Cl0udShifter/ru-unb-cardapio/master/parser';

// State
let state = {
  campus: localStorage.getItem('ru_campus') || 'darcy',
  diet: localStorage.getItem('ru_diet') || 'padrao',
  meal: detectInitialMeal(),
  dayIndex: 0,
  menu: null,
  student: JSON.parse(localStorage.getItem('ru_student')) || {
    nome: 'Estudante UnB',
    matricula: '190000000',
    curso: 'Graduação',
    validade: 'Março / 2027',
    qrData: '1900000000000000',
    saldo: 'R$ 16,65'
  }
};

function detectInitialMeal() {
  const now = new Date();
  const minutes = now.getHours() * 60 + now.getMinutes();
  if (minutes < 9 * 60 + 30) return 'cafe';
  if (minutes < 14 * 60 + 30) return 'almoco';
  return 'jantar';
}

// Theme handling
function initTheme() {
  const saved = localStorage.getItem('ru_theme');
  const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
  const theme = saved || (prefersDark ? 'dark' : 'light');
  document.documentElement.setAttribute('data-theme', theme);
  updateThemeIcon(theme);
}

function updateThemeIcon(theme) {
  const icon = document.getElementById('themeIcon');
  if (theme === 'dark') {
    icon.innerHTML = '<path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"></path>';
  } else {
    icon.innerHTML = '<circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/>';
  }
}

function toggleTheme() {
  const current = document.documentElement.getAttribute('data-theme') || 'light';
  const next = current === 'dark' ? 'light' : 'dark';
  document.documentElement.setAttribute('data-theme', next);
  localStorage.setItem('ru_theme', next);
  updateThemeIcon(next);
}

// Platform detection banner
function initPlatformBanner() {
  const dismissed = localStorage.getItem('ru_banner_dismissed');
  if (dismissed) return;

  const isIOS = /iPad|iPhone|iPod/.test(navigator.userAgent) && !window.MSStream;
  const isStandalone = window.navigator.standalone || window.matchMedia('(display-mode: standalone)').matches;
  const banner = document.getElementById('platformBanner');
  const bannerText = document.getElementById('bannerText');
  const actionBtn = document.getElementById('bannerActionBtn');

  if (isStandalone) return;

  if (isIOS) {
    banner.style.display = 'flex';
    bannerText.innerHTML = '📲 <strong>Instale no iPhone:</strong> Toque em <strong>Compartilhar</strong> e selecione <strong>Adicionar à Tela de Início</strong>.';
    actionBtn.style.display = 'none';
  } else if (/Android/.test(navigator.userAgent)) {
    banner.style.display = 'flex';
    bannerText.innerHTML = '🤖 <strong>App Nativo Android:</strong> Instale o APK com notificações diárias de refeição.';
    actionBtn.style.display = 'block';
    actionBtn.textContent = 'Baixar APK';
    actionBtn.onclick = () => {
      window.location.href = 'https://github.com/Cl0udShifter/ru-unb-cardapio/releases/latest/download/RU-UnB.apk';
    };
  }
}

// Fetch menu
async function fetchMenu(campus = state.campus) {
  const cacheKey = `ru_menu_cache_${campus}`;
  const cached = localStorage.getItem(cacheKey);
  if (cached) {
    try {
      state.menu = JSON.parse(cached);
      determineInitialDayIndex();
      renderAll();
    } catch (e) {}
  }

  try {
    const res = await fetch(`${RAW_GITHUB_BASE}/menu_${campus}.json?t=${Date.now()}`);
    if (res.ok) {
      const data = await res.json();
      state.menu = data;
      localStorage.setItem(cacheKey, JSON.stringify(data));
      determineInitialDayIndex();
      renderAll();
      return;
    }
  } catch (err) {
    console.warn('Raw GitHub fetch failed, trying local data:', err);
  }

  // Fallback to local data
  try {
    const localRes = await fetch(`./data/menu_${campus}.json`);
    if (localRes.ok) {
      const data = await localRes.json();
      state.menu = data;
      localStorage.setItem(cacheKey, JSON.stringify(data));
      determineInitialDayIndex();
      renderAll();
    }
  } catch (err) {}
}

function determineInitialDayIndex() {
  if (!state.menu || !state.menu.dias || state.menu.dias.length === 0) return;
  const now = new Date();
  const dayOfWeek = now.getDay(); // 0 = Sun, 1 = Mon ... 6 = Sat
  // Map JS day to menu index (0=Seg..6=Dom)
  const menuIndex = dayOfWeek === 0 ? 6 : dayOfWeek - 1;
  state.dayIndex = Math.min(menuIndex, state.menu.dias.length - 1);
}

// Render Functions
function renderAll() {
  document.getElementById('currentCampusLabel').textContent = CAMPUS_NAMES[state.campus] || 'Darcy Ribeiro';
  renderDayRail();
  renderMealCard();
  updateBottomNav();
  updateDietBar();
}

function renderDayRail() {
  const container = document.getElementById('dayRail');
  container.innerHTML = '';
  if (!state.menu || !state.menu.dias) return;

  const now = new Date();
  const dayOfWeek = now.getDay();
  const todayIdx = dayOfWeek === 0 ? 6 : dayOfWeek - 1;

  state.menu.dias.forEach((day, idx) => {
    const chip = document.createElement('div');
    const isToday = idx === todayIdx;
    const isActive = idx === state.dayIndex;
    
    chip.className = `day-chip ${isActive ? 'active' : ''} ${isToday ? 'today' : ''}`;
    
    // Parse short weekday and day number
    const parts = (day.dia || '').split(' ');
    const shortWd = parts[0] ? parts[0].substring(0, 3) : `D${idx+1}`;
    const dayNum = parts[1] || `${idx+1}`;

    chip.innerHTML = `
      <span class="weekday">${shortWd}</span>
      <span class="day-num">${dayNum}</span>
    `;

    chip.onclick = () => {
      state.dayIndex = idx;
      renderAll();
    };

    container.appendChild(chip);
  });
}

function updateDietBar() {
  const tabs = document.querySelectorAll('.diet-tab');
  tabs.forEach(tab => {
    const diet = tab.getAttribute('data-diet');
    if (diet === state.diet) {
      tab.classList.add('active');
    } else {
      tab.classList.remove('active');
    }
  });
}

function updateBottomNav() {
  const btns = document.querySelectorAll('.meal-tab-btn');
  btns.forEach(btn => {
    if (btn.getAttribute('data-meal') === state.meal) {
      btn.classList.add('active');
    } else {
      btn.classList.remove('active');
    }
  });
  document.getElementById('mealTimeBadge').textContent = MEAL_TIMES[state.meal];
}

function renderMealCard() {
  const card = document.getElementById('mealCard');
  if (!state.menu || !state.menu.dias || !state.menu.dias[state.dayIndex]) {
    card.innerHTML = '<div style="text-align:center; padding: 24px; color: var(--text-muted);">Cardápio indisponível para este dia.</div>';
    return;
  }

  const currentDay = state.menu.dias[state.dayIndex];
  document.getElementByIdCurrentDayTitle = document.getElementById('currentDayTitle');
  if (document.getElementByIdCurrentDayTitle) {
    document.getElementByIdCurrentDayTitle.textContent = currentDay.dia || 'Cardápio do Dia';
  }

  if (state.meal === 'cafe') {
    renderBreakfast(card, currentDay.cafe);
  } else {
    const menuData = state.meal === 'almoco' ? currentDay.almoco : currentDay.jantar;
    renderLunchDinner(card, menuData);
  }
}

function renderLunchDinner(container, menu) {
  if (!menu || (!menu.pratoPrincipal && !menu.vegetariano && !menu.vegano)) {
    container.innerHTML = '<div style="text-align:center; padding: 24px; color: var(--text-muted);">Refeitório fechado nesta refeição.</div>';
    return;
  }

  // Dishes reordered by diet preference
  const rawDishes = [];
  if (menu.pratoPrincipal) rawDishes.push({ type: 'padrao', label: 'Padrão', name: menu.pratoPrincipal });
  if (menu.vegetariano) rawDishes.push({ type: 'ovolacto', label: 'Ovolacto', name: menu.vegetariano });
  if (menu.vegano) rawDishes.push({ type: 'vegano', label: 'Vegano', name: menu.vegano });

  const sortedDishes = [...rawDishes].sort((a, b) => {
    if (state.diet === 'ovolacto') {
      const order = { ovolacto: 0, vegano: 1, padrao: 2 };
      return (order[a.type] ?? 3) - (order[b.type] ?? 3);
    }
    if (state.diet === 'vegano') {
      const order = { vegano: 0, ovolacto: 1, padrao: 2 };
      return (order[a.type] ?? 3) - (order[b.type] ?? 3);
    }
    return 0; // standard
  });

  let html = `<div class="section-title">PRATOS PRINCIPAIS</div>`;
  sortedDishes.forEach((dish, idx) => {
    const isHighlight = (idx === 0 && dish.type === state.diet) || (state.diet === 'padrao' && dish.type === 'padrao');
    html += `
      <div class="dish-row">
        <span class="diet-chip chip-${dish.type}">${dish.label}</span>
        <span class="dish-name ${isHighlight ? 'highlight' : ''}">${escapeHtml(dish.name)}</span>
      </div>
    `;
  });

  // Guarnição
  if (menu.guarnicao || menu.acompanhamento) {
    html += `<div class="card-divider"></div><div class="section-title">GUARNIÇÃO & ACOMPANHAMENTOS</div>`;
    if (menu.guarnicao) html += renderSimpleRow('Guarnição', menu.guarnicao);
    if (menu.acompanhamento) html += renderSimpleRow('Acomp.', menu.acompanhamento);
  }

  // Sopa
  if (menu.sopa || menu.torrada) {
    html += `<div class="card-divider"></div><div class="section-title">SOPA DO DIA</div>`;
    if (menu.sopa) html += renderSimpleRow('Sopa', menu.sopa);
    if (menu.torrada) html += renderSimpleRow('Torrada', menu.torrada);
  }

  // Saladas
  if (menu.salada1 || menu.salada2 || menu.molho) {
    html += `<div class="card-divider"></div><div class="section-title">SALADAS</div>`;
    if (menu.salada1) html += renderSimpleRow('Salada 1', menu.salada1);
    if (menu.salada2) html += renderSimpleRow('Salada 2', menu.salada2);
    if (menu.molho) html += renderSimpleRow('Molho', menu.molho);
  }

  // Sobremesa & Bebida (clean rows without colored pills)
  if (menu.sobremesa || menu.bebida) {
    html += `<div class="card-divider"></div><div class="section-title">SOBREMESA & BEBIDA</div>`;
    if (menu.sobremesa) html += renderSimpleRow('Sobremesa', menu.sobremesa);
    if (menu.bebida) html += renderSimpleRow('Refresco', menu.bebida);
  }

  container.innerHTML = html;
}

function renderBreakfast(container, menu) {
  if (!menu || (!menu.pratoPrincipal && !menu.vegetariano && !menu.vegano)) {
    container.innerHTML = '<div style="text-align:center; padding: 24px; color: var(--text-muted);">Refeitório fechado nesta refeição.</div>';
    return;
  }

  const rawComplements = [];
  if (menu.pratoPrincipal) rawComplements.push({ type: 'padrao', label: 'Padrão', name: menu.pratoPrincipal });
  if (menu.vegetariano) rawComplements.push({ type: 'ovolacto', label: 'Ovolacto', name: menu.vegetariano });
  if (menu.vegano) rawComplements.push({ type: 'vegano', label: 'Vegano', name: menu.vegano });

  const sorted = [...rawComplements].sort((a, b) => {
    if (state.diet === 'ovolacto') {
      const order = { ovolacto: 0, vegano: 1, padrao: 2 };
      return (order[a.type] ?? 3) - (order[b.type] ?? 3);
    }
    if (state.diet === 'vegano') {
      const order = { vegano: 0, ovolacto: 1, padrao: 2 };
      return (order[a.type] ?? 3) - (order[b.type] ?? 3);
    }
    return 0;
  });

  let html = `<div class="section-title">COMPLEMENTOS</div>`;
  sorted.forEach((dish, idx) => {
    const isHighlight = (idx === 0 && dish.type === state.diet) || (state.diet === 'padrao' && dish.type === 'padrao');
    html += `
      <div class="dish-row">
        <span class="diet-chip chip-${dish.type}">${dish.label}</span>
        <span class="dish-name ${isHighlight ? 'highlight' : ''}">${escapeHtml(dish.name)}</span>
      </div>
    `;
  });

  if (menu.opcaoExtra) {
    html += renderSimpleRow('Extra', menu.opcaoExtra);
  }

  if (menu.bebidasQuentes) {
    html += `<div class="card-divider"></div><div class="section-title">BEBIDAS QUENTES</div>`;
    html += renderSimpleRow('Bebidas', menu.bebidasQuentes);
  }

  if (menu.paes) {
    html += `<div class="card-divider"></div><div class="section-title">PADARIA</div>`;
    html += renderSimpleRow('Pães', menu.paes);
  }

  if (menu.fruta) {
    html += `<div class="card-divider"></div><div class="section-title">FRUTA DO DIA</div>`;
    html += renderSimpleRow('Fruta', menu.fruta);
  }

  container.innerHTML = html;
}

function renderSimpleRow(label, value) {
  return `
    <div class="simple-row">
      <span class="simple-label">${label}</span>
      <span class="simple-val">${escapeHtml(value)}</span>
    </div>
  `;
}

function escapeHtml(str) {
  if (!str) return '';
  return str.replace(/[&<>"']/g, m => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;'
  })[m]);
}

// Carteirinha QR Code
let qrCodeObj = null;
let qrFullscreenObj = null;

function renderCarteirinha() {
  document.getElementById('metaNome').textContent = state.student.nome;
  document.getElementById('metaMatricula').textContent = state.student.matricula;
  document.getElementById('metaCurso').textContent = state.student.curso;
  document.getElementById('metaValidade').textContent = state.student.validade;
  document.getElementById('saldoDisplay').textContent = state.student.saldo || 'R$ 16,65';
  document.getElementById('qrFullscreenMatricula').textContent = state.student.matricula;

  const container = document.getElementById('qrCodeContainer');
  container.innerHTML = '';
  
  if (typeof QRCode !== 'undefined') {
    qrCodeObj = new QRCode(container, {
      text: state.student.qrData || state.student.matricula,
      width: 160,
      height: 160,
      colorDark: '#000000',
      colorLight: '#ffffff',
      correctLevel: QRCode.CorrectLevel.M
    });
  }
}

function renderFullscreenQR() {
  const container = document.getElementById('qrFullscreenCode');
  container.innerHTML = '';
  if (typeof QRCode !== 'undefined') {
    qrFullscreenObj = new QRCode(container, {
      text: state.student.qrData || state.student.matricula,
      width: 260,
      height: 260,
      colorDark: '#000000',
      colorLight: '#ffffff',
      correctLevel: QRCode.CorrectLevel.H
    });
  }
}

// Event Listeners setup
function setupListeners() {
  // Theme toggle
  document.getElementById('themeToggleBtn').onclick = toggleTheme;

  // Refresh
  document.getElementById('refreshBtn').onclick = () => {
    fetchMenu(state.campus);
  };

  // Close banner
  document.getElementById('closeBannerBtn').onclick = () => {
    document.getElementById('platformBanner').style.display = 'none';
    localStorage.setItem('ru_banner_dismissed', 'true');
  };

  // Campus modal
  const campusModal = document.getElementById('campusModal');
  document.getElementById('campusDropdownBtn').onclick = () => {
    updateCampusModalChecks();
    campusModal.classList.add('open');
  };

  campusModal.onclick = (e) => {
    if (e.target === campusModal || e.target.classList.contains('sheet-handle')) {
      campusModal.classList.remove('open');
    }
  };

  document.querySelectorAll('.campus-option').forEach(opt => {
    opt.onclick = () => {
      const c = opt.getAttribute('data-campus');
      if (c && c !== state.campus) {
        state.campus = c;
        localStorage.setItem('ru_campus', c);
        fetchMenu(c);
      }
      campusModal.classList.remove('open');
    };
  });

  function updateCampusModalChecks() {
    document.querySelectorAll('.campus-option').forEach(opt => {
      const c = opt.getAttribute('data-campus');
      const check = opt.querySelector('.check-icon');
      if (c === state.campus) {
        opt.classList.add('active');
        if (check) check.style.display = 'block';
      } else {
        opt.classList.remove('active');
        if (check) check.style.display = 'none';
      }
    });
  }

  // Diet tabs
  document.querySelectorAll('.diet-tab').forEach(tab => {
    tab.onclick = () => {
      const d = tab.getAttribute('data-diet');
      state.diet = d;
      localStorage.setItem('ru_diet', d);
      renderAll();
    };
  });

  // Meal tabs
  document.querySelectorAll('.meal-tab-btn').forEach(btn => {
    btn.onclick = () => {
      const m = btn.getAttribute('data-meal');
      state.meal = m;
      renderAll();
    };
  });

  // Carteirinha Modal
  const carteirinhaModal = document.getElementById('carteirinhaModal');
  document.getElementById('openCarteirinhaBtn').onclick = () => {
    renderCarteirinha();
    carteirinhaModal.classList.add('open');
  };

  carteirinhaModal.onclick = (e) => {
    if (e.target === carteirinhaModal || e.target.classList.contains('sheet-handle')) {
      carteirinhaModal.classList.remove('open');
    }
  };

  // Edit student form
  const editBtn = document.getElementById('editStudentBtn');
  const editForm = document.getElementById('editStudentForm');
  editBtn.onclick = () => {
    const isHidden = editForm.style.display === 'none';
    editForm.style.display = isHidden ? 'block' : 'none';
    if (isHidden) {
      document.getElementById('inputNome').value = state.student.nome;
      document.getElementById('inputMatricula').value = state.student.matricula;
      document.getElementById('inputCurso').value = state.student.curso;
      document.getElementById('inputValidade').value = state.student.validade;
      document.getElementById('inputQrData').value = state.student.qrData;
    }
  };

  document.getElementById('saveStudentBtn').onclick = () => {
    state.student = {
      nome: document.getElementById('inputNome').value.trim() || 'Estudante UnB',
      matricula: document.getElementById('inputMatricula').value.trim() || '190000000',
      curso: document.getElementById('inputCurso').value.trim() || 'Graduação',
      validade: document.getElementById('inputValidade').value.trim() || 'Março / 2027',
      qrData: document.getElementById('inputQrData').value.trim() || '1900000000000000',
      saldo: state.student.saldo || 'R$ 16,65'
    };
    localStorage.setItem('ru_student', JSON.stringify(state.student));
    editForm.style.display = 'none';
    renderCarteirinha();
  };

  // QR Fullscreen
  const qrFullModal = document.getElementById('qrFullscreenModal');
  document.getElementById('qrCodeContainer').onclick = () => {
    renderFullscreenQR();
    qrFullModal.classList.add('open');
  };

  document.getElementById('closeQrFullscreenBtn').onclick = () => {
    qrFullModal.classList.remove('open');
  };
}

// Service worker registration
function registerSW() {
  if ('serviceWorker' in navigator) {
    window.addEventListener('load', () => {
      navigator.serviceWorker.register('sw.js').catch(e => console.log('SW reg fail:', e));
    });
  }
}

// App Initialization
document.addEventListener('DOMContentLoaded', () => {
  initTheme();
  setupListeners();
  initPlatformBanner();
  fetchMenu();
  registerSW();
});
