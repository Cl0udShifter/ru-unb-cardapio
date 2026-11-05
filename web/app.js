// RU UnB - Web PWA (1:1 Android Jetpack Compose Port)
// Exact match of MenuHomeScreen.kt, MealSectionCard.kt, DaySelectorRail.kt, 
// MealBottomControl.kt, CampusSelectorBottomSheet.kt, NotificationSettingsBottomSheet.kt, 
// StudentCardScreen.kt and Notion Warm Paper theme.

const CAMPUSES = {
  darcy: { name: 'Darcy Ribeiro', sub: 'Plano Piloto' },
  gama: { name: 'FGA (Gama)', sub: 'Faculdade do Gama' },
  ceilandia: { name: 'FCE (Ceilândia)', sub: 'Faculdade de Ceilândia' },
  planaltina: { name: 'FUP (Planaltina)', sub: 'Faculdade de Planaltina' }
};

const WEEKDAYS_SHORT = ['SEG', 'TER', 'QUA', 'QUI', 'SEX', 'SÁB', 'DOM'];
const WEEKDAYS_LONG = [
  'Segunda-feira', 'Terça-feira', 'Quarta-feira', 'Quinta-feira',
  'Sexta-feira', 'Sábado', 'Domingo'
];

const MEAL_INFO = {
  cafe: { title: 'Café da Manhã', hours: '07h às 09h30' },
  almoco: { title: 'Almoço', hours: '11h às 14h30' },
  jantar: { title: 'Jantar', hours: '17h às 19h30' }
};

const RAW_GITHUB_BASE = 'https://raw.githubusercontent.com/Cl0udShifter/ru-unb-cardapio/master/parser';

function getInitialStudentCard() {
  try {
    const raw = localStorage.getItem('ru_student_card');
    if (!raw) return { isConfigured: false };
    const parsed = JSON.parse(raw);
    if (parsed && parsed.isConfigured && parsed.matricula) {
      return parsed;
    }
  } catch (e) {
    localStorage.removeItem('ru_student_card');
    localStorage.removeItem('ru_sigaa_disciplinas');
    appState.disciplinas = [];
  }
  return { isConfigured: false };
}

function getInitialStatement() {
  try {
    const raw = localStorage.getItem('ru_statement');
    if (!raw) return null;
    return JSON.parse(raw);
  } catch (e) {
    return null;
  }
}

// App State
let appState = {
  campus: localStorage.getItem('ru_campus') || 'darcy',
  meal: detectCurrentMeal(),
  selectedDayIndex: 0,
  todayIndex: -1,
  menu: null,
  notifications: JSON.parse(localStorage.getItem('ru_notifications_config')) || {
    cafeEnabled: true,
    cafeDays: [1, 2, 3, 4, 5],
    almocoEnabled: true,
    almocoDays: [1, 2, 3, 4, 5],
    jantarEnabled: true,
    jantarDays: [1, 2, 3, 4, 5],
    dietPreference: 'padrao'
  },
  studentCard: getInitialStudentCard(),
  statement: getInitialStatement()
};

function detectCurrentMeal() {
  const now = new Date();
  const mins = now.getHours() * 60 + now.getMinutes();
  if (mins <= 9 * 60 + 30) return 'cafe';
  if (mins <= 14 * 60 + 30) return 'almoco';
  return 'jantar';
}

function getEffectiveDiet() {
  return appState.notifications.dietPreference || 'padrao';
}

function cleanTrailingText(str) {
  if (!str) return '';
  return str.replace(/\s+(ou|e)\s*$/i, '').trim();
}

function escapeHtml(str) {
  if (!str) return '';
  return str.replace(/[&<>"']/g, m => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
  })[m]);
}

function decodeHtmlEntities(str) {
  if (!str) return '';
  const txt = document.createElement('textarea');
  txt.innerHTML = str;
  return txt.value;
}

// Theme handling
function initTheme() {
  const saved = localStorage.getItem('ru_theme');
  const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
  const theme = saved || (prefersDark ? 'dark' : 'light');
  applyTheme(theme);
}

function applyTheme(theme) {
  document.documentElement.setAttribute('data-theme', theme);
  localStorage.setItem('ru_theme', theme);
  const icon = document.getElementById('themeIcon');
  if (theme === 'dark') {
    icon.innerHTML = '<path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"></path>';
  } else {
    icon.innerHTML = '<circle cx="12" cy="12" r="5"></circle><line x1="12" y1="1" x2="12" y2="3"></line><line x1="12" y1="21" x2="12" y2="23"></line><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"></line><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"></line><line x1="1" y1="12" x2="3" y2="12"></line><line x1="21" y1="12" x2="23" y2="12"></line><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"></line><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"></line>';
  }
}

function toggleTheme() {
  const current = document.documentElement.getAttribute('data-theme') || 'dark';
  applyTheme(current === 'dark' ? 'light' : 'dark');
}

function isMenuExpired(menu) {
  if (!menu || !menu.dias || menu.dias.length === 0) return true;
  const lastDay = menu.dias[menu.dias.length - 1];
  if (!lastDay || !lastDay.data) return true;
  const parts = lastDay.data.trim().split('/');
  if (parts.length !== 3) return false;
  const lastDate = new Date(parseInt(parts[2], 10), parseInt(parts[1], 10) - 1, parseInt(parts[0], 10), 23, 59, 59);
  return lastDate < new Date();
}

// Menu Loading
async function loadMenu(campusId = appState.campus, forceRefresh = false) {
  const cacheKey = `ru_menu_json_${campusId}`;
  const cached = localStorage.getItem(cacheKey);
  let cachedMenu = null;
  if (cached) {
    try {
      cachedMenu = JSON.parse(cached);
    } catch (e) {}
  }

  if (cachedMenu && !isMenuExpired(cachedMenu) && !forceRefresh) {
    appState.menu = cachedMenu;
    calculateTodayIndex();
    renderHomeScreen();
  }

  // 1. Try raw GitHub master (always fresh)
  try {
    const res = await fetch(`${RAW_GITHUB_BASE}/menu_${campusId}.json?t=${Date.now()}`, { cache: 'no-cache' });
    if (res.ok) {
      const data = await res.json();
      if (data && data.dias && data.dias.length > 0) {
        appState.menu = data;
        localStorage.setItem(cacheKey, JSON.stringify(data));
        calculateTodayIndex();
        renderHomeScreen();
        return;
      }
    }
  } catch (err) {}

  // 2. Fallback to local files
  try {
    const localRes = await fetch(`./data/menu_${campusId}.json?t=${Date.now()}`, { cache: 'no-cache' });
    if (localRes.ok) {
      const data = await localRes.json();
      if (data && data.dias && data.dias.length > 0) {
        appState.menu = data;
        localStorage.setItem(cacheKey, JSON.stringify(data));
        calculateTodayIndex();
        renderHomeScreen();
        return;
      }
    }
  } catch (err) {}

  try {
    const rootRes = await fetch(`./menu_${campusId}.json?t=${Date.now()}`, { cache: 'no-cache' });
    if (rootRes.ok) {
      const data = await rootRes.json();
      if (data && data.dias && data.dias.length > 0) {
        appState.menu = data;
        localStorage.setItem(cacheKey, JSON.stringify(data));
        calculateTodayIndex();
        renderHomeScreen();
        return;
      }
    }
  } catch (err) {}

  if (cachedMenu && !appState.menu) {
    appState.menu = cachedMenu;
    calculateTodayIndex();
    renderHomeScreen();
  }
}

function calculateTodayIndex() {
  if (!appState.menu || !appState.menu.dias || appState.menu.dias.length === 0) {
    appState.todayIndex = -1;
    return;
  }
  const now = new Date();
  
  // Format today as dd/MM/yyyy
  const dd = String(now.getDate()).padStart(2, '0');
  const mm = String(now.getMonth() + 1).padStart(2, '0');
  const yyyy = now.getFullYear();
  const todayStr = `${dd}/${mm}/${yyyy}`;

  // Strict date string matching
  const foundIdx = appState.menu.dias.findIndex(d => d.data && d.data.trim() === todayStr);

  appState.todayIndex = foundIdx;
  if (foundIdx !== -1) {
    appState.selectedDayIndex = foundIdx;
  } else {
    if (appState.selectedDayIndex >= appState.menu.dias.length || appState.selectedDayIndex < 0) {
      appState.selectedDayIndex = 0;
    }
  }
}

// Rendering Main UI
const SVG_ACTION_REFRESH = '<path d=\"M 17.65 6.35 C 16.2 4.9 14.21 4.0 12.0 4.0 c -4.42 0.0 -7.99 3.58 -7.99 8.0 s 3.57 8.0 7.99 8.0 c 3.73 0.0 6.84 -2.55 7.73 -6.0 h -2.08 c -0.82 2.33 -3.04 4.0 -5.65 4.0 c -3.31 0.0 -6.0 -2.69 -6.0 -6.0 s 2.69 -6.0 6.0 -6.0 c 1.66 0.0 3.14 0.69 4.22 1.78 L 13.0 11.0 h 7.0 V 4.0 l -2.35 2.35 Z\"/>';
const SVG_ACTION_TODAY = '<path d=\"M 19.0 3.0 h -1.0 L 18.0 1.0 h -2.0 v 2.0 L 8.0 3.0 L 8.0 1.0 L 6.0 1.0 v 2.0 L 5.0 3.0 c -1.11 0.0 -2.0 0.9 -2.0 2.0 v 14.0 c 0.0 1.1 0.89 2.0 2.0 2.0 h 14.0 c 1.1 0.0 2.0 -0.9 2.0 -2.0 L 21.0 5.0 c 0.0 -1.1 -0.9 -2.0 -2.0 -2.0 Z M 19.0 19.0 L 5.0 19.0 L 5.0 9.0 h 14.0 v 10.0 Z M 19.0 7.0 L 5.0 7.0 L 5.0 5.0 h 14.0 v 2.0 Z M 7.0 11.0 h 5.0 v 5.0 L 7.0 16.0 Z\"/>';

function renderHomeScreen() {
  const campusObj = CAMPUSES[appState.campus] || CAMPUSES.darcy;
  document.getElementById('currentCampusLabel').textContent = campusObj.name;

  renderDaySelectorRail();
  renderMealContent();
  updateMealBottomBar();
  updateNotificationBellIcon();
  updateHeaderActionButton();
}

function updateHeaderActionButton() {
  const btn = document.getElementById('refreshBtn');
  if (!btn) return;
  btn.title = 'Atualizar';
  btn.setAttribute('aria-label', 'Atualizar');
  const svg = btn.querySelector('svg');
  if (svg) svg.innerHTML = SVG_ACTION_REFRESH;
}

// Day Selector Rail (DaySelectorRail.kt 1:1)
function renderDaySelectorRail() {
  const rail = document.getElementById('dayRail');
  rail.innerHTML = '';
  if (!appState.menu || !appState.menu.dias) return;

  appState.menu.dias.forEach((dayObj, index) => {
    const isSelected = index === appState.selectedDayIndex;
    const isToday = index === appState.todayIndex;

    const shortDay = WEEKDAYS_SHORT[index] || `DIA`;
    const dateStr = dayObj.data || '';
    const dayNumber = dateStr.split('/')[0] || `${index + 1}`;

    const item = document.createElement('div');
    item.className = `day-item ${isSelected ? 'active' : ''}`;
    
    item.innerHTML = `
      <div class="day-item-header">
        <span class="day-short">${shortDay}</span>
        ${isToday ? '<span class="day-today-dot"></span>' : ''}
      </div>
      <div class="day-number">${dayNumber}</div>
    `;

    item.onclick = () => {
      appState.selectedDayIndex = index;
      renderHomeScreen();
    };

    rail.appendChild(item);
  });
}

// Render Meal Content (MenuHomeScreen.kt & MealSectionCard.kt 1:1)
function renderMealContent() {
  const cardContainer = document.getElementById('mealCardContainer');
  if (!appState.menu || !appState.menu.dias || !appState.menu.dias[appState.selectedDayIndex]) {
    cardContainer.innerHTML = '<div style="text-align:center; padding: 24px; color: var(--text-secondary);">Nenhum cardápio disponível para este dia.</div>';
    return;
  }

  const currentDay = appState.menu.dias[appState.selectedDayIndex];
  const dateStr = currentDay.data || '';
  const weekdayStr = currentDay.dia_semana || currentDay.diaSemana || WEEKDAYS_LONG[appState.selectedDayIndex] || 'Dia';
  
  // Header: `${currentDay.diaSemana}, ${currentDay.data}`
  document.getElementById('currentDayHeader').textContent = `${weekdayStr}, ${dateStr}`;
  
  const mealInfo = MEAL_INFO[appState.meal] || MEAL_INFO.almoco;
  document.getElementById('mealTitleLabel').textContent = mealInfo.title;
  document.getElementById('mealHoursLabel').textContent = mealInfo.hours;

  const effectiveDiet = getEffectiveDiet();

  if (appState.meal === 'cafe') {
    renderBreakfastCard(cardContainer, currentDay.cafe, effectiveDiet);
  } else {
    const mealData = appState.meal === 'almoco' ? currentDay.almoco : currentDay.jantar;
    renderLunchDinnerCard(cardContainer, mealData, effectiveDiet, appState.meal === 'jantar');
  }
}

// Lunch / Dinner Card (LunchDinnerCard 1:1)
function renderLunchDinnerCard(container, menu, dietPreference, isJantar) {
  if (!menu) {
    container.innerHTML = '<div style="text-align:center; padding: 24px; color: var(--text-secondary); font-size: 14px;">Refeitório fechado nesta refeição.</div>';
    return;
  }

  const pratoPrincipal = menu.prato_principal || menu.pratoPrincipal || '';
  const vegetariano = menu.vegetariano || '';
  const vegano = menu.vegano || '';

  if (!pratoPrincipal && !vegetariano && !vegano) {
    container.innerHTML = '<div style="text-align:center; padding: 24px; color: var(--text-secondary); font-size: 14px;">Refeitório fechado nesta refeição.</div>';
    return;
  }

  const rawDishes = [];
  if (pratoPrincipal.trim()) {
    rawDishes.push({ type: 'padrao', label: 'Padrão', name: pratoPrincipal });
  }
  if (vegetariano.trim()) {
    rawDishes.push({ type: 'ovolacto', label: 'Ovolacto', name: vegetariano });
  }
  if (vegano.trim()) {
    rawDishes.push({ type: 'vegano', label: 'Vegano', name: vegano });
  }

  // Sorted dishes based on diet preference (MealSectionCard.kt lines 59-75)
  const sortedDishes = [...rawDishes].sort((a, b) => {
    if (dietPreference === 'ovolacto') {
      const order = { ovolacto: 0, vegano: 1, padrao: 2 };
      return (order[a.type] ?? 3) - (order[b.type] ?? 3);
    }
    if (dietPreference === 'vegano') {
      const order = { vegano: 0, ovolacto: 1, padrao: 2 };
      return (order[a.type] ?? 3) - (order[b.type] ?? 3);
    }
    return 0; // padrao
  });

  let html = `<div class="section-label">PRATOS PRINCIPAIS</div>`;
  sortedDishes.forEach((dish, idx) => {
    const isHighlight = (idx === 0 && dish.type === dietPreference) ||
                        (dietPreference === 'padrao' && dish.type === 'padrao');
    html += `
      <div class="dish-item">
        <span class="diet-pill pill-${dish.type}">${dish.label}</span>
        <span class="dish-text ${isHighlight ? 'highlight' : ''}">${escapeHtml(cleanTrailingText(dish.name))}</span>
      </div>
    `;
  });

  // Guarnição & Acompanhamentos
  const guarnicao = menu.guarnicao || '';
  const acomp = menu.acompanhamentos || menu.acompanhamento || '';
  if (guarnicao.trim() || acomp.trim()) {
    html += `<div class="notion-divider"></div><div class="section-label">GUARNIÇÃO & ACOMPANHAMENTOS</div>`;
    if (guarnicao.trim()) html += renderCleanRow('Guarnição', guarnicao);
    if (acomp.trim()) html += renderCleanRow('Acomp.', acomp);
  }

  // Sopa do Dia (se for Jantar)
  const sopa = menu.sopa || '';
  const torrada = menu.torrada || '';
  if (isJantar && (sopa.trim() || torrada.trim())) {
    html += `<div class="notion-divider"></div><div class="section-label">SOPA DO DIA</div>`;
    if (sopa.trim()) html += renderCleanRow('Sopa', sopa);
    if (torrada.trim()) html += renderCleanRow('Torrada', torrada);
  }

  // Saladas
  const salada1 = menu.salada_1 || menu.salada1 || '';
  const salada2 = menu.salada_2 || menu.salada2 || '';
  const molho = menu.molho || '';
  if (salada1.trim() || salada2.trim() || molho.trim()) {
    html += `<div class="notion-divider"></div><div class="section-label">SALADAS</div>`;
    if (salada1.trim()) html += renderCleanRow('Salada 1', salada1);
    if (salada2.trim()) html += renderCleanRow('Salada 2', salada2);
    if (molho.trim()) html += renderCleanRow('Molho', molho);
  }

  // Sobremesa & Bebida
  const sobremesa = menu.sobremesa || '';
  const bebida = menu.bebida || '';
  if (sobremesa.trim() || bebida.trim()) {
    html += `<div class="notion-divider"></div><div class="section-label">SOBREMESA & BEBIDA</div>`;
    if (sobremesa.trim()) html += renderCleanRow('Sobremesa', sobremesa);
    if (bebida.trim()) html += renderCleanRow('Refresco', bebida);
  }

  container.innerHTML = html;
}

// Breakfast Card (BreakfastCard 1:1)
function renderBreakfastCard(container, menu, dietPreference) {
  if (!menu) {
    container.innerHTML = '<div style="text-align:center; padding: 24px; color: var(--text-secondary); font-size: 14px;">Refeitório fechado nesta refeição.</div>';
    return;
  }

  const pratoPrincipal = menu.prato_principal || menu.pratoPrincipal || '';
  const vegetariano = menu.vegetariano || '';
  const vegano = menu.vegano || '';

  if (!pratoPrincipal && !vegetariano && !vegano) {
    container.innerHTML = '<div style="text-align:center; padding: 24px; color: var(--text-secondary); font-size: 14px;">Refeitório fechado nesta refeição.</div>';
    return;
  }

  const rawComplements = [];
  if (pratoPrincipal.trim()) {
    rawComplements.push({ type: 'padrao', label: 'Padrão', name: pratoPrincipal });
  }
  if (vegetariano.trim()) {
    rawComplements.push({ type: 'ovolacto', label: 'Ovolacto', name: vegetariano });
  }
  if (vegano.trim()) {
    rawComplements.push({ type: 'vegano', label: 'Vegano', name: vegano });
  }

  const sorted = [...rawComplements].sort((a, b) => {
    if (dietPreference === 'ovolacto') {
      const order = { ovolacto: 0, vegano: 1, padrao: 2 };
      return (order[a.type] ?? 3) - (order[b.type] ?? 3);
    }
    if (dietPreference === 'vegano') {
      const order = { vegano: 0, ovolacto: 1, padrao: 2 };
      return (order[a.type] ?? 3) - (order[b.type] ?? 3);
    }
    return 0;
  });

  let html = `<div class="section-label">COMPLEMENTOS</div>`;
  sorted.forEach((dish, idx) => {
    const isHighlight = (idx === 0 && dish.type === dietPreference) ||
                        (dietPreference === 'padrao' && dish.type === 'padrao');
    html += `
      <div class="dish-item">
        <span class="diet-pill pill-${dish.type}">${dish.label}</span>
        <span class="dish-text ${isHighlight ? 'highlight' : ''}">${escapeHtml(cleanTrailingText(dish.name))}</span>
      </div>
    `;
  });

  const opcaoExtra = menu.opcao_extra || menu.opcaoExtra || '';
  if (opcaoExtra.trim()) {
    html += renderCleanRow('Extra', opcaoExtra);
  }

  const bebidas = menu.bebidas || menu.bebidasQuentes || '';
  const paes = menu.panificacao || menu.paes || '';
  const gordura = menu.gordura || '';

  if (bebidas.trim() || paes.trim() || gordura.trim()) {
    html += `<div class="notion-divider"></div><div class="section-label">BEBIDAS & PANIFICAÇÃO</div>`;
    if (bebidas.trim()) html += renderCleanRow('Bebidas', bebidas);
    if (paes.trim()) html += renderCleanRow('Pães', paes);
    if (gordura.trim()) html += renderCleanRow('Gordura', gordura);
  }

  const fruta = menu.fruta || '';
  if (fruta.trim()) {
    html += `<div class="notion-divider"></div><div class="section-label">FRUTA DO DIA</div>`;
    html += renderCleanRow('Fruta', fruta);
  }

  container.innerHTML = html;
}

function renderCleanRow(label, val) {
  return `
    <div class="clean-item-row">
      <span class="clean-item-label">${label}</span>
      <span class="clean-item-val">${escapeHtml(cleanTrailingText(val))}</span>
    </div>
  `;
}

function updateMealBottomBar() {
  document.querySelectorAll('.meal-top-btn, .meal-control-btn').forEach(btn => {
    const m = btn.getAttribute('data-meal');
    if (m === appState.meal) {
      btn.classList.add('active');
    } else {
      btn.classList.remove('active');
    }
  });
}

function updateNotificationBellIcon() {
  const cfg = appState.notifications;
  const hasActiveAlerts = cfg.cafeEnabled || cfg.almocoEnabled || cfg.jantarEnabled;
  const bell = document.getElementById('notifBellIcon');
  if (hasActiveAlerts) {
    bell.style.opacity = '1';
  } else {
    bell.style.opacity = '0.45';
  }
}

// Campus Selector Sheet (CampusSelectorBottomSheet.kt 1:1)
function setupCampusModal() {
  const overlay = document.getElementById('campusSheetOverlay');
  const triggerBtn = document.getElementById('campusDropdownBtn');

  triggerBtn.onclick = () => {
    updateCampusSheetSelection();
    overlay.classList.add('open');
  };

  overlay.onclick = (e) => {
    if (e.target === overlay || e.target.classList.contains('sheet-drag-handle')) {
      overlay.classList.remove('open');
    }
  };

  document.querySelectorAll('.campus-card-item').forEach(item => {
    item.onclick = () => {
      const c = item.getAttribute('data-campus');
      if (c && c !== appState.campus) {
        appState.campus = c;
        localStorage.setItem('ru_campus', c);
        loadMenu(c);
      }
      overlay.classList.remove('open');
    };
  });
}

function updateCampusSheetSelection() {
  document.querySelectorAll('.campus-card-item').forEach(item => {
    const c = item.getAttribute('data-campus');
    if (c === appState.campus) {
      item.classList.add('selected');
    } else {
      item.classList.remove('selected');
    }
  });
}

// Notifications Bottom Sheet (NotificationSettingsBottomSheet.kt 1:1)
function setupNotificationsModal() {
  const overlay = document.getElementById('notificationsSheetOverlay');
  const openBtn = document.getElementById('openNotificationsBtn');

  if (openBtn) openBtn.onclick = () => {
    populateNotificationSettingsUI();
    overlay.classList.add('open');
  };

  overlay.onclick = (e) => {
    if (e.target === overlay || e.target.classList.contains('sheet-drag-handle')) {
      overlay.classList.remove('open');
    }
  };

  // Switches
  document.getElementById('switchCafe').onchange = (e) => {
    appState.notifications.cafeEnabled = e.target.checked;
    updateNotifDaysVisibility();
    saveNotificationsConfig();
    renderHomeScreen();
  };
  document.getElementById('switchAlmoco').onchange = (e) => {
    appState.notifications.almocoEnabled = e.target.checked;
    updateNotifDaysVisibility();
    saveNotificationsConfig();
    renderHomeScreen();
  };
  document.getElementById('switchJantar').onchange = (e) => {
    appState.notifications.jantarEnabled = e.target.checked;
    updateNotifDaysVisibility();
    saveNotificationsConfig();
    renderHomeScreen();
  };

  // Day Dots
  setupDayDotGroup('daysRowCafe', 'cafeDays');
  setupDayDotGroup('daysRowAlmoco', 'almocoDays');
  setupDayDotGroup('daysRowJantar', 'jantarDays');

  // Diet Choices
  document.querySelectorAll('.diet-choice-btn').forEach(btn => {
    btn.onclick = () => {
      const d = btn.getAttribute('data-diet');
      appState.notifications.dietPreference = d;
      saveNotificationsConfig();
      updateDietButtonsUI();
      renderHomeScreen();
    };
  });

  updateNotifDaysVisibility();
}

function setupDayDotGroup(containerId, stateKey) {
  const container = document.getElementById(containerId);
  if (!container) return;
  container.querySelectorAll('.notif-day-dot').forEach(dot => {
    dot.onclick = () => {
      const dayNum = parseInt(dot.getAttribute('data-day'), 10);
      const arr = appState.notifications[stateKey];
      const exists = arr.includes(dayNum);
      if (exists) {
        appState.notifications[stateKey] = arr.filter(d => d !== dayNum);
        dot.classList.remove('active');
      } else {
        appState.notifications[stateKey].push(dayNum);
        dot.classList.add('active');
      }
      saveNotificationsConfig();
      renderHomeScreen();
    };
  });
}

function updateNotifDaysVisibility() {
  const cafeGroup = document.getElementById('daysGroupCafe');
  const almocoGroup = document.getElementById('daysGroupAlmoco');
  const jantarGroup = document.getElementById('daysGroupJantar');
  if (cafeGroup) cafeGroup.classList.toggle('hidden', !appState.notifications.cafeEnabled);
  if (almocoGroup) almocoGroup.classList.toggle('hidden', !appState.notifications.almocoEnabled);
  if (jantarGroup) jantarGroup.classList.toggle('hidden', !appState.notifications.jantarEnabled);
}

function populateNotificationSettingsUI() {
  const cfg = appState.notifications;
  document.getElementById('switchCafe').checked = cfg.cafeEnabled;
  document.getElementById('switchAlmoco').checked = cfg.almocoEnabled;
  document.getElementById('switchJantar').checked = cfg.jantarEnabled;

  syncDayDots('daysRowCafe', cfg.cafeDays);
  syncDayDots('daysRowAlmoco', cfg.almocoDays);
  syncDayDots('daysRowJantar', cfg.jantarDays);

  updateNotifDaysVisibility();
  updateDietButtonsUI();
}

function syncDayDots(containerId, activeDays) {
  const container = document.getElementById(containerId);
  if (!container) return;
  container.querySelectorAll('.notif-day-dot').forEach(dot => {
    const dayNum = parseInt(dot.getAttribute('data-day'), 10);
    if (activeDays.includes(dayNum)) {
      dot.classList.add('active');
    } else {
      dot.classList.remove('active');
    }
  });
}

function updateDietButtonsUI() {
  const currentDiet = appState.notifications.dietPreference || 'padrao';
  document.querySelectorAll('.diet-choice-btn').forEach(btn => {
    if (btn.getAttribute('data-diet') === currentDiet) {
      btn.classList.add('selected');
    } else {
      btn.classList.remove('selected');
    }
  });
}

function saveNotificationsConfig() {
  localStorage.setItem('ru_notifications_config', JSON.stringify(appState.notifications));
  updateNotificationBellIcon();
}

// Carteirinha Digital (StudentCardScreen.kt 1:1)
let qrCodeInstance = null;
let turnstileQrInstance = null;


// ==========================================================================
// MÓDULO 1: PAINEL DISCENTE & NAVEGAÇÃO GLOBAL (Bottom Navigation & 3D Flip Card)
// ==========================================================================

async function executeSigaaSync(mat, pass) {
  const isVercelHost = window.location.hostname.endsWith('vercel.app') || (window.location.hostname === 'localhost' && window.location.port !== '');
  const apiUrl = window.SIGAA_API_URL || (isVercelHost ? '/api' : 'https://ru-unb-cardapio-3zjv.vercel.app/api');

  const response = await fetch(apiUrl, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ matricula: mat, senha: pass })
  });

  const data = await response.json().catch(() => null);

  if (response.ok && data && data.success) {
    let decodedQr = data.codigoQr || `${mat.padEnd(16, '0')}`;
    if (data.qrImageBase64 && typeof jsQR !== 'undefined') {
      try {
        const tempImg = new Image();
        tempImg.src = data.qrImageBase64;
        await new Promise((resolve) => { tempImg.onload = resolve; tempImg.onerror = resolve; });
        const canvas = document.createElement('canvas');
        canvas.width = tempImg.naturalWidth || 250;
        canvas.height = tempImg.naturalHeight || 250;
        const ctx = canvas.getContext('2d');
        ctx.drawImage(tempImg, 0, 0);
        const imgData = ctx.getImageData(0, 0, canvas.width, canvas.height);
        const decoded = jsQR(imgData.data, canvas.width, canvas.height);
        if (decoded && decoded.data) {
          decodedQr = decoded.data;
        }
      } catch (_) {}
    }

    appState.studentCard = {
      isConfigured: true,
      nome: data.nome || 'Estudante UnB',
      matricula: data.matricula || mat,
      curso: data.curso || 'GRADUAÇÃO',
      nivel: data.nivel || 'GRADUAÇÃO',
      status: data.status || 'ATIVO',
      semestreAtual: data.semestreAtual || '2026.2',
      ira: data.ira || '--',
      mediaPonderada: data.mediaPonderada || '--',
      chTotal: data.chTotal || 0,
      chObrigatoriaPendente: data.chObrigatoriaPendente || 0,
      chOptativaPendente: data.chOptativaPendente || 0,
      chComplementarPendente: data.chComplementarPendente || 0,
      chConcluida: data.chConcluida || 0,
      percentualConcluido: data.percentualConcluido != null ? Number(data.percentualConcluido) : 0,
      fotoPerfilUrl: data.fotoPerfilUrl || '',
      validade: data.validade || 'Março / 2027',
      codigoQr: decodedQr,
      qrImageBase64: data.qrImageBase64 || '',
      saldo: data.saldo || 'R$ 0,00'
    };

    const now = new Date();
    const dd = String(now.getDate()).padStart(2, '0');
    const mm = String(now.getMonth() + 1).padStart(2, '0');
    const yyyy = now.getFullYear();
    const hh = String(now.getHours()).padStart(2, '0');
    const min = String(now.getMinutes()).padStart(2, '0');
    const timeStr = `${dd}/${mm}/${yyyy} ${hh}:${min}`;

    const existingStmt = JSON.parse(localStorage.getItem('ru_statement') || '{}');
    const existingTrans = existingStmt.transacoes || [];
    const newTrans = data.transacoes || [];
    const mergedMap = new Map();
    existingTrans.forEach(t => {
      t.descricao = decodeHtmlEntities(t.descricao);
      mergedMap.set(`${t.dataHora}|${t.descricao}|${t.valor}`, t);
    });
    newTrans.forEach(t => {
      t.descricao = decodeHtmlEntities(t.descricao);
      mergedMap.set(`${t.dataHora}|${t.descricao}|${t.valor}`, t);
    });
    const mergedList = Array.from(mergedMap.values());

    appState.statement = {
      saldoAtual: data.saldo || 'R$ 0,00',
      saldoAnterior: data.saldoAnterior || data.saldo || 'R$ 0,00',
      ultimaAtualizacao: timeStr,
      transacoes: mergedList
    };

        if (data.disciplinas) {
      appState.disciplinas = data.disciplinas;
      localStorage.setItem('ru_sigaa_disciplinas', JSON.stringify(appState.disciplinas));
    }
    localStorage.setItem('ru_student_card', JSON.stringify(appState.studentCard));
    localStorage.setItem('ru_statement', JSON.stringify(appState.statement));
    localStorage.setItem('ru_sigaa_creds', JSON.stringify({ matricula: mat, senha: pass }));

    renderDiscenteDashboard();
    return { success: true };
  } else {
    return { success: false, error: (data && data.error) ? data.error : 'Falha na autenticação com o SIGAA.' };
  }
}

function setupDiscenteDashboard() {
  const navCardapioBtn = document.getElementById('navCardapioBtn');
  const navAulasBtn = document.getElementById('navAulasBtn');
  const navDiscenteBtn = document.getElementById('navDiscenteBtn');
  const viewCardapio = document.getElementById('viewCardapio');
  const viewAulas = document.getElementById('viewAulas');
  const viewDiscente = document.getElementById('viewDiscente');

  function switchGlobalTab(tab) {
    [navCardapioBtn, navAulasBtn, navDiscenteBtn].forEach(b => b && b.classList.remove('active'));
    [viewCardapio, viewAulas, viewDiscente].forEach(v => {
      if (v) {
        v.classList.remove('active');
        v.style.display = 'none';
      }
    });

    if (tab === 'cardapio' && navCardapioBtn && viewCardapio) {
      navCardapioBtn.classList.add('active');
      viewCardapio.classList.add('active');
      viewCardapio.style.display = 'block';
    } else if (tab === 'aulas' && navAulasBtn && viewAulas) {
      navAulasBtn.classList.add('active');
      viewAulas.classList.add('active');
      viewAulas.style.display = 'block';
      if (typeof renderAulasScreen === 'function') renderAulasScreen();
    } else if (tab === 'discente' && navDiscenteBtn && viewDiscente) {
      navDiscenteBtn.classList.add('active');
      viewDiscente.classList.add('active');
      viewDiscente.style.display = 'block';
      renderDiscenteDashboard();
    }
  }

  function bindInstantTab(btn, tab) {
    if (!btn) return;
    btn.addEventListener('pointerdown', (e) => {
      e.preventDefault();
      switchGlobalTab(tab);
    });
    btn.onclick = (e) => {
      e.preventDefault();
      switchGlobalTab(tab);
    };
  }

  bindInstantTab(navCardapioBtn, 'cardapio');
  bindInstantTab(navAulasBtn, 'aulas');
  bindInstantTab(navDiscenteBtn, 'discente');

  const ctaBtn = document.getElementById('openSigaaSyncCtaBtn');
  if (ctaBtn) {
    ctaBtn.onclick = openSigaaModal;
  }

  const syncBtn = document.getElementById('discenteSyncBtn');
  if (syncBtn) {
    syncBtn.onclick = async () => {
      const credsRaw = localStorage.getItem('ru_sigaa_creds');
      if (!credsRaw) {
        openSigaaModal();
        return;
      }
      const icon = document.getElementById('discenteSyncIcon');
      if (icon) icon.style.animation = 'spin 0.8s linear infinite';
      try {
        const creds = JSON.parse(credsRaw);
        await executeSigaaSync(creds.matricula, creds.senha);
      } catch (err) {
        console.error(err);
      } finally {
        if (icon) icon.style.animation = 'none';
      }
    };
  }

  const flipper = document.getElementById('discenteScreenFlipper') || document.getElementById('ruFlipInner');
  const flipToQrBtn = document.getElementById('ruFlipToQrBtn');
  const flipToFrontBtn = document.getElementById('ruFlipToFrontBtn');

  if (flipToQrBtn && flipper) {
    flipToQrBtn.onclick = (e) => {
      e.stopPropagation();
      flipper.classList.add('flipped');
      document.body.classList.add('discente-flipped');
    };
  }

  if (flipToFrontBtn && flipper) {
    flipToFrontBtn.onclick = (e) => {
      e.stopPropagation();
      flipper.classList.remove('flipped');
    };
  }

  const flipBack = document.getElementById('discenteScreenBack') || document.querySelector('.ru-flip-back');
  if (flipBack && flipper) {
    flipBack.onclick = (e) => {
      flipper.classList.remove('flipped');
      document.body.classList.remove('discente-flipped');
    };
  }

  const disconnectBtn = document.getElementById('discenteDisconnectBtn');
  if (disconnectBtn) {
    disconnectBtn.onclick = () => {
      if (confirm('Deseja realmente desconectar sua conta do SIGAA deste dispositivo?')) {
        appState.studentCard = { isConfigured: false };
        appState.statement = null;
        localStorage.removeItem('ru_student_card');
    localStorage.removeItem('ru_sigaa_disciplinas');
    appState.disciplinas = [];
        localStorage.removeItem('ru_statement');
        localStorage.removeItem('ru_sigaa_creds');
        renderDiscenteDashboard();
      }
    };
  }
}

function renderDiscenteDashboard() {
  const isConfigured = appState.studentCard && appState.studentCard.isConfigured;
  const disconnectedState = document.getElementById('discenteDisconnectedState');
  const connectedState = document.getElementById('discenteConnectedState');
  const lastSyncText = document.getElementById('discenteLastSyncText');

  if (!isConfigured) {
    if (disconnectedState) disconnectedState.style.display = 'block';
    if (connectedState) connectedState.style.display = 'none';
    if (lastSyncText) lastSyncText.textContent = 'Não conectado';
    return;
  }

  if (disconnectedState) disconnectedState.style.display = 'none';
  if (connectedState) connectedState.style.display = 'block';

  const card = appState.studentCard;
  const stmt = appState.statement;

  if (lastSyncText) {
    lastSyncText.textContent = 'Atualizado em ' + (stmt?.ultimaAtualizacao || 'Hoje');
  }

  // Card 1: Perfil
  const nameEl = document.getElementById('discenteNome');
  const matEl = document.getElementById('discenteMatricula');
  const cursoEl = document.getElementById('discenteCurso');
  const statusBadge = document.getElementById('discenteStatusBadge');
  const nivelBadge = document.getElementById('discenteNivelBadge');
  const semestreBadge = document.getElementById('discenteSemestreBadge');
  const photoImg = document.getElementById('discentePhotoImg');
  const initialsAvatar = document.getElementById('discenteInitialsAvatar');

  if (nameEl) nameEl.textContent = card.nome || 'Estudante UnB';
  if (matEl) matEl.textContent = 'Matrícula: ' + (card.matricula || '--');
  if (cursoEl) cursoEl.textContent = card.curso || 'Graduação';
  if (statusBadge) statusBadge.textContent = card.status || 'ATIVO';
  if (nivelBadge) nivelBadge.textContent = card.nivel || 'GRADUAÇÃO';
  if (semestreBadge) semestreBadge.textContent = card.semestreAtual || '2026.2';

  const names = (card.nome || 'UnB').trim().split(/\s+/);
  const initials = names.length > 1 ? (names[0][0] + names[names.length - 1][0]).toUpperCase() : names[0].substring(0, 2).toUpperCase();
  if (initialsAvatar) initialsAvatar.textContent = initials;

  if (photoImg && initialsAvatar) {
    if (card.fotoPerfilUrl) {
      photoImg.src = card.fotoPerfilUrl;
      photoImg.style.display = 'block';
      initialsAvatar.style.display = 'none';
      photoImg.onerror = () => {
        photoImg.style.display = 'none';
        initialsAvatar.style.display = 'flex';
      };
    } else {
      photoImg.style.display = 'none';
      initialsAvatar.style.display = 'flex';
    }
  }

  // Card 2: Rendimento
  const iraEl = document.getElementById('discenteIraVal');
  const mpEl = document.getElementById('discenteMpVal');
  const pctEl = document.getElementById('discentePctVal');
  const progressFill = document.getElementById('discenteProgressBar');
  const chConcluidaEl = document.getElementById('discenteHoursConcluidas');
  const chTotalEl = document.getElementById('discenteHoursTotal');
  const chObEl = document.getElementById('discenteChOb');
  const chOpEl = document.getElementById('discenteChOp');

  if (iraEl) iraEl.textContent = card.ira || '--';
  if (mpEl) mpEl.textContent = card.mediaPonderada || '--';

  const pct = card.percentualConcluido != null ? Number(card.percentualConcluido) : 0;
  if (pctEl) pctEl.textContent = pct.toFixed(1) + '%';
  if (progressFill) progressFill.style.width = Math.min(100, Math.max(0, pct)) + '%';

  const chConcluidas = card.chConcluida != null ? card.chConcluida : 0;
  const chTotal = card.chTotal != null ? card.chTotal : 0;
  if (chConcluidaEl) chConcluidaEl.textContent = chConcluidas + 'h';
  if (chTotalEl) chTotalEl.textContent = chTotal + 'h';

  if (chObEl) chObEl.textContent = (card.chObrigatoriaPendente || 0) + 'h';
  if (chOpEl) chOpEl.textContent = (card.chOptativaPendente || 0) + 'h';

  // Card 3: RU Flip Card
  const saldoEl = document.getElementById('discenteRuSaldoVal');
  if (saldoEl) {
    saldoEl.textContent = stmt?.saldoAtual || card.saldo || 'R$ 0,00';
  }

  const transContainer = document.getElementById('discenteMiniTransacoes');
  if (transContainer) {
    transContainer.innerHTML = '';
    const trans = stmt?.transacoes || [];
    if (trans.length === 0) {
      transContainer.innerHTML = '<div style="font-size:12px;color:var(--text-secondary);padding:6px 0;text-align:center;">Nenhuma transação recente registrada</div>';
    } else {
      trans.slice(0, 4).forEach(t => {
        const row = document.createElement('div');
        row.className = 'ru-statement-row';
        row.innerHTML = `
          <div>
            <div class="ru-statement-desc">${escapeHtml(decodeHtmlEntities(t.descricao))}</div>
            <div class="ru-statement-date">${escapeHtml(t.dataHora)}</div>
          </div>
          <div class="ru-statement-val">${escapeHtml(t.valor)}</div>
        `;
        transContainer.appendChild(row);
      });
    }
  }

  // QR Code Catraca (Back)
  const qrImg = document.getElementById('discenteQrImg');
  if (qrImg) {
    if (card.qrImageBase64) {
      qrImg.src = card.qrImageBase64;
      qrImg.style.display = 'block';
    } else if (card.codigoQr) {
      const tempDiv = document.createElement('div');
      renderZxingQRCode(tempDiv, card.codigoQr, 200);
      const canvas = tempDiv.querySelector('canvas');
      if (canvas) {
        qrImg.src = canvas.toDataURL('image/png');
        qrImg.style.display = 'block';
      }
    }
  }

  const backNome = document.getElementById('discenteBackNome');
  const backMat = document.getElementById('discenteBackMatricula');
  if (backNome) backNome.textContent = card.nome || 'Estudante UnB';
  if (backMat) backMat.textContent = 'Matrícula: ' + (card.matricula || '--');
}


function setupCarteirinhaModal() {
  const modal = document.getElementById('carteirinhaModal');
  const openBtn = document.getElementById('openCarteirinhaBtn');
  const closeBtn = document.getElementById('closeCarteirinhaBtn');

  if (openBtn) openBtn.onclick = () => {
    renderCarteirinhaModal();
    modal.classList.add('open');
  };

  closeBtn.onclick = () => {
    modal.classList.remove('open');
  };

  // Top action button in Carteirinha header
  const topActionBtn = document.getElementById('carteirinhaTopActionBtn');
  topActionBtn.onclick = () => {
    if (!appState.studentCard || !appState.studentCard.isConfigured) {
      openSigaaModal();
    } else {
      showCarteirinhaEditForm();
    }
  };

  // Inline edit button on the ID card
  document.getElementById('editCarteirinhaInlineBtn').onclick = () => {
    showCarteirinhaEditForm();
  };

  document.getElementById('btnPreencherManual').onclick = () => {
    showCarteirinhaEditForm();
  };

  document.getElementById('btnCancelEdit').onclick = () => {
    document.getElementById('carteirinhaEditForm').style.display = 'none';
    renderCarteirinhaModal();
  };

  document.getElementById('btnSaveCard').onclick = () => {
    const nome = document.getElementById('inputNome').value.trim();
    const matricula = document.getElementById('inputMatricula').value.trim();
    if (!matricula) {
      alert('Por favor, informe sua matrícula.');
      return;
    }
    const saldo = document.getElementById('inputSaldo').value.trim() || 'R$ 0,00';
    const prevQr = appState.studentCard ? appState.studentCard.codigoQr : '';
    const newQr = document.getElementById('inputQrCode').value.trim() || `${matricula.padEnd(16, '0')}`;
    const qrImg = (newQr === prevQr && appState.studentCard) ? (appState.studentCard.qrImageBase64 || '') : '';
    appState.studentCard = {
      isConfigured: true,
      nome: nome || 'Estudante UnB',
      matricula: matricula,
      curso: document.getElementById('inputCurso').value.trim() || 'GRADUAÇÃO',
      validade: document.getElementById('inputValidade').value.trim() || 'Março / 2027',
      codigoQr: newQr,
      qrImageBase64: qrImg,
      saldo: saldo
    };
    if (appState.statement) {
      appState.statement.saldoAtual = saldo;
    }
        if (data.disciplinas) {
      appState.disciplinas = data.disciplinas;
      localStorage.setItem('ru_sigaa_disciplinas', JSON.stringify(appState.disciplinas));
    }
    localStorage.setItem('ru_student_card', JSON.stringify(appState.studentCard));
    if (appState.statement) {
      localStorage.setItem('ru_statement', JSON.stringify(appState.statement));
    }
    document.getElementById('carteirinhaEditForm').style.display = 'none';
    renderCarteirinhaModal();
  };

  // Turnstile Fullscreen QR
  const qrBox = document.getElementById('idQrCodeContainer');
  const hintBtn = document.getElementById('openFullscreenQrHint');
  const turnstileOverlay = document.getElementById('turnstileQrOverlay');

  const openTurnstile = () => {
    renderTurnstileFullscreenQR();
    turnstileOverlay.classList.add('open');
  };

  qrBox.onclick = openTurnstile;
  hintBtn.onclick = openTurnstile;

  turnstileOverlay.onclick = () => {
    turnstileOverlay.classList.remove('open');
  };

  // Refresh Extrato
  const btnRefreshExtrato = document.getElementById('btnRefreshExtrato');
  btnRefreshExtrato.onclick = async () => {
    const credsRaw = localStorage.getItem('ru_sigaa_creds');
    if (!credsRaw) {
      openSigaaModal();
      return;
    }
    const icon = document.getElementById('extratoRefreshIcon');
    icon.style.animation = 'spin 0.8s linear infinite';
    try {
      const creds = JSON.parse(credsRaw);
      const isVercelHost = window.location.hostname.endsWith('vercel.app') || (window.location.hostname === 'localhost' && window.location.port !== '');
      const apiUrl = window.SIGAA_API_URL || (isVercelHost ? '/api' : 'https://ru-unb-cardapio-3zjv.vercel.app/api');
      const response = await fetch(apiUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ matricula: creds.matricula, senha: creds.senha })
      });
      const data = await response.json();
      if (response.ok && data && data.success) {
        if (data.saldo) {
          appState.studentCard.saldo = data.saldo;
          if (data.qrImageBase64) appState.studentCard.qrImageBase64 = data.qrImageBase64;
              if (data.disciplinas) {
      appState.disciplinas = data.disciplinas;
      localStorage.setItem('ru_sigaa_disciplinas', JSON.stringify(appState.disciplinas));
    }
    localStorage.setItem('ru_student_card', JSON.stringify(appState.studentCard));
        }
        const now = new Date();
        const dd = String(now.getDate()).padStart(2, '0');
        const mm = String(now.getMonth() + 1).padStart(2, '0');
        const yyyy = now.getFullYear();
        const hh = String(now.getHours()).padStart(2, '0');
        const min = String(now.getMinutes()).padStart(2, '0');
        const timeStr = `${dd}/${mm}/${yyyy} ${hh}:${min}`;

        const existingStmt = JSON.parse(localStorage.getItem('ru_statement') || '{}');
        const existingTrans = existingStmt.transacoes || [];
        const newTrans = data.transacoes || [];
        const mergedMap = new Map();
        existingTrans.forEach(t => {
          t.descricao = decodeHtmlEntities(t.descricao);
          mergedMap.set(`${t.dataHora}|${t.descricao}|${t.valor}`, t);
        });
        newTrans.forEach(t => {
          t.descricao = decodeHtmlEntities(t.descricao);
          mergedMap.set(`${t.dataHora}|${t.descricao}|${t.valor}`, t);
        });
        const mergedList = Array.from(mergedMap.values());

        appState.statement = {
          saldoAtual: data.saldo || appState.studentCard.saldo || 'R$ 0,00',
          saldoAnterior: data.saldoAnterior || data.saldo || 'R$ 0,00',
          ultimaAtualizacao: timeStr,
          transacoes: mergedList
        };
        localStorage.setItem('ru_statement', JSON.stringify(appState.statement));
        renderCarteirinhaModal();
      }
    } catch (e) {
      console.error(e);
    } finally {
      icon.style.animation = 'none';
    }
  };

  // Toggle RU Guide
  const btnToggleGuide = document.getElementById('btnToggleRuGuide');
  const guidePanel = document.getElementById('ruGuideContent');
  btnToggleGuide.onclick = () => {
    const isShown = guidePanel.style.display === 'flex';
    guidePanel.style.display = isShown ? 'none' : 'flex';
  };

  // Disconnect Account
  document.getElementById('btnDisconnectAccount').onclick = () => {
    if (confirm('Deseja realmente desconectar esta conta da carteirinha?')) {
      appState.studentCard = { isConfigured: false };
      appState.statement = null;
      localStorage.removeItem('ru_student_card');
    localStorage.removeItem('ru_sigaa_disciplinas');
    appState.disciplinas = [];
      localStorage.removeItem('ru_statement');
      localStorage.removeItem('ru_sigaa_creds');
      renderCarteirinhaModal();
    }
  };

  // SIGAA Dialog Setup
  setupSigaaDialog();
}

function setupSigaaDialog() {
  const overlay = document.getElementById('sigaaModalOverlay');
  document.getElementById('btnConectarSigaa').onclick = openSigaaModal;

  document.getElementById('btnCancelSigaa').onclick = () => {
    overlay.classList.remove('open');
  };

  overlay.onclick = (e) => {
    if (e.target === overlay) {
      overlay.classList.remove('open');
    }
  };

  const confirmBtn = document.getElementById('btnConfirmSigaa');

  confirmBtn.onclick = async () => {
    const mat = document.getElementById('sigaaMatricula').value.trim();
    const pass = document.getElementById('sigaaSenha').value.trim();
    const statusMsg = document.getElementById('sigaaStatusMsg');

    if (!mat) {
      statusMsg.className = 'sigaa-status-msg error';
      statusMsg.style.display = 'block';
      statusMsg.textContent = 'Por favor, informe sua matrícula da UnB.';
      return;
    }
    if (!pass) {
      statusMsg.className = 'sigaa-status-msg error';
      statusMsg.style.display = 'block';
      statusMsg.textContent = 'Por favor, informe sua senha do SIGAA / SSO UnB.';
      return;
    }

        statusMsg.className = 'sigaa-status-msg';
    statusMsg.style.display = 'block';
    statusMsg.textContent = 'Conectando ao SSO UnB...';
    confirmBtn.disabled = true;
    confirmBtn.style.opacity = '0.7';

    try {
      const res = await executeSigaaSync(mat, pass);
      if (res.success) {
        statusMsg.textContent = 'Carteirinha e dados sincronizados com sucesso!';
        setTimeout(() => {
          overlay.classList.remove('open');
          statusMsg.style.display = 'none';
          document.getElementById('sigaaMatricula').value = '';
          document.getElementById('sigaaSenha').value = '';
          confirmBtn.disabled = false;
          confirmBtn.style.opacity = '1';
          renderCarteirinhaModal();
          renderDiscenteDashboard();
        }, 600);
      } else {
        statusMsg.className = 'sigaa-status-msg error';
        statusMsg.textContent = res.error || 'Matrícula ou senha incorretas.';
        confirmBtn.disabled = false;
        confirmBtn.style.opacity = '1';
      }
    } catch (err) {
      statusMsg.className = 'sigaa-status-msg error';
      statusMsg.textContent = 'Serviço do SIGAA temporariamente indisponível. Tente novamente mais tarde.';
      confirmBtn.disabled = false;
      confirmBtn.style.opacity = '1';
    }
    return;

  };
}

function openSigaaModal() {
  const overlay = document.getElementById('sigaaModalOverlay');
  const statusMsg = document.getElementById('sigaaStatusMsg');
  statusMsg.style.display = 'none';
  statusMsg.className = 'sigaa-status-msg';
  statusMsg.textContent = '';
  const confirmBtn = document.getElementById('btnConfirmSigaa');
  confirmBtn.disabled = false;
  confirmBtn.style.opacity = '1';
  overlay.classList.add('open');
}

function showCarteirinhaEditForm() {
  const form = document.getElementById('carteirinhaEditForm');
  const configuredState = document.getElementById('carteirinhaConfiguredState');
  const emptyState = document.getElementById('carteirinhaEmptyState');

  configuredState.style.display = 'none';
  emptyState.style.display = 'none';
  form.style.display = 'block';

  document.getElementById('inputNome').value = appState.studentCard.nome || '';
  document.getElementById('inputMatricula').value = appState.studentCard.matricula || '';
  document.getElementById('inputCurso').value = appState.studentCard.curso || '';
  document.getElementById('inputValidade').value = appState.studentCard.validade || '';
  document.getElementById('inputQrCode').value = appState.studentCard.codigoQr || '';
  document.getElementById('inputSaldo').value = appState.studentCard.saldo || 'R$ 0,00';
}

function renderCarteirinhaModal() {
  const configuredState = document.getElementById('carteirinhaConfiguredState');
  const emptyState = document.getElementById('carteirinhaEmptyState');
  const form = document.getElementById('carteirinhaEditForm');
  const topActionIcon = document.getElementById('topActionIcon');

  form.style.display = 'none';

  if (!appState.studentCard || !appState.studentCard.isConfigured || !appState.studentCard.matricula) {
    emptyState.style.display = 'flex';
    configuredState.style.display = 'none';
    topActionIcon.innerHTML = '<path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle>';
    return;
  }

  emptyState.style.display = 'none';
  configuredState.style.display = 'block';
  topActionIcon.innerHTML = '<path d="M12 20h9"></path><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"></path>';

  document.getElementById('cardNomeTxt').textContent = appState.studentCard.nome || 'Estudante UnB';
  document.getElementById('cardMatriculaTxt').textContent = appState.studentCard.matricula;
  document.getElementById('cardCursoTxt').textContent = appState.studentCard.curso || 'GRADUAÇÃO';
  document.getElementById('cardValidadeTxt').textContent = appState.studentCard.validade || 'Março / 2027';
  document.getElementById('cardSaldoTxt').textContent = appState.studentCard.saldo || 'R$ 0,00';

  // Render Statement & Transactions
  const statement = appState.statement;
  if (statement) {
    if (statement.saldoAtual) {
      document.getElementById('cardSaldoTxt').textContent = statement.saldoAtual;
    }
    if (statement.ultimaAtualizacao) {
      document.getElementById('extratoLastUpdateTxt').textContent = `Atualizado em ${statement.ultimaAtualizacao}`;
    }
    const historyList = document.getElementById('extratoHistoricoList');
    if (statement.transacoes && statement.transacoes.length > 0) {
      historyList.innerHTML = statement.transacoes.map(t => `
        <div class="extrato-history-item">
          <div>
            <div class="history-item-desc">${escapeHtml(decodeHtmlEntities(t.descricao))}</div>
            <div class="history-item-time">${escapeHtml(t.dataHora)}</div>
          </div>
          <div class="history-item-val">${escapeHtml(t.valor)}</div>
        </div>
      `).join('');
    } else {
      historyList.innerHTML = `
        <div class="empty-history-notice" style="font-size: 12px; color: var(--text-secondary); padding: 8px 0;">
          Nenhuma transação registrada nesta conta.
        </div>
      `;
    }
  }

  const qrContainer = document.getElementById('idQrCodeContainer');
  const qrPayload = (appState.studentCard && appState.studentCard.codigoQr)
    ? appState.studentCard.codigoQr
    : (appState.studentCard ? appState.studentCard.matricula : '');
  renderZxingQRCode(qrContainer, qrPayload, 190);
}

function renderZxingQRCode(container, content, sizePx) {
  if (!container) return;
  container.innerHTML = '';
  const text = (content || '').trim();

  // 1. Prioritize raw official QR code image from SIGAA PDF if available
  if (appState.studentCard && appState.studentCard.qrImageBase64) {
    const img = document.createElement('img');
    img.src = appState.studentCard.qrImageBase64;
    img.alt = 'QR Code da Catraca';
    img.style.width = '100%';
    img.style.height = '100%';
    img.style.objectFit = 'contain';
    img.style.imageRendering = 'pixelated';
    img.style.display = 'block';
    container.appendChild(img);
    return;
  }

  if (!text) return;

  // 2. Official ZXing port (matches Android ru-unb-app 1:1, Level L, Margin 1)
  if (typeof ZXing !== 'undefined') {
    try {
      const hints = new Map();
      hints.set(ZXing.EncodeHintType.MARGIN, 1);
      hints.set(ZXing.EncodeHintType.ERROR_CORRECTION, 'L');
      hints.set(ZXing.EncodeHintType.CHARACTER_SET, 'UTF-8');

      if (ZXing.QRCodeWriter) {
        const writer = new ZXing.QRCodeWriter();
        const matrix = writer.encode(text, ZXing.BarcodeFormat.QR_CODE, sizePx, sizePx, hints);
        const width = matrix.getWidth();
        const height = matrix.getHeight();
        const canvas = document.createElement('canvas');
        canvas.width = width;
        canvas.height = height;
        canvas.style.width = '100%';
        canvas.style.height = '100%';
        canvas.style.display = 'block';
        canvas.style.imageRendering = 'pixelated';
        const ctx = canvas.getContext('2d');
        ctx.fillStyle = '#ffffff';
        ctx.fillRect(0, 0, width, height);
        ctx.fillStyle = '#000000';
        for (let y = 0; y < height; y++) {
          for (let x = 0; x < width; x++) {
            if (matrix.get(x, y)) {
              ctx.fillRect(x, y, 1, 1);
            }
          }
        }
        container.appendChild(canvas);
        return;
      }
    } catch (e) {
      console.warn('ZXing encode failed:', e);
    }
  }

  // 3. Fallback to existing base64 image if available
  if (appState.studentCard && appState.studentCard.qrImageBase64) {
    const img = document.createElement('img');
    img.src = appState.studentCard.qrImageBase64;
    img.alt = 'QR Code da Catraca';
    img.style.width = '100%';
    img.style.height = '100%';
    img.style.objectFit = 'contain';
    img.style.imageRendering = 'pixelated';
    img.style.display = 'block';
    container.appendChild(img);
    return;
  }

  // 4. Fallback to QRCode
  if (typeof QRCode !== 'undefined') {
    new QRCode(container, {
      text: text,
      width: sizePx,
      height: sizePx,
      colorDark: '#000000',
      colorLight: '#ffffff',
      correctLevel: QRCode.CorrectLevel.L
    });
  }
}

function renderTurnstileFullscreenQR() {
  const container = document.getElementById('turnstileQrCode');
  container.innerHTML = '';
  const nomeEl = document.getElementById('turnstileNomeTxt');
  const matEl = document.getElementById('turnstileMatriculaTxt');

  if (nomeEl) {
    nomeEl.textContent = appState.studentCard.nome || 'Estudante UnB';
  }
  if (matEl) {
    matEl.textContent = `Matrícula: ${appState.studentCard.matricula || '---'}`;
  }

  const qrPayload = (appState.studentCard && appState.studentCard.codigoQr)
    ? appState.studentCard.codigoQr
    : (appState.studentCard ? appState.studentCard.matricula : '');

  renderZxingQRCode(container, qrPayload, 320);
}

// Navegação de abas exclusivamente pelos botões inferiores
function setupSwipeGestures() {
  // Swipe lateral desativado conforme alinhado
}

// Service Worker Registration with Immediate Update Check
function registerServiceWorker() {
  if ('serviceWorker' in navigator) {
    window.addEventListener('load', () => {
      navigator.serviceWorker.register('sw.js?v=15').then((reg) => {
        reg.update();
      }).catch(() => {});
    });
  }
}

// Initialize Application
document.addEventListener('DOMContentLoaded', () => {
  if (navigator.storage && navigator.storage.persist) {
    navigator.storage.persist().catch(() => {});
  }

  
  // Pull-to-refresh on Discente Tab
  const discenteContainer = document.getElementById('viewDiscente');
  if (discenteContainer) {
    let dStartY = 0;
    let dDistY = 0;
    discenteContainer.addEventListener('touchstart', (e) => {
      if (discenteContainer.scrollTop <= 2) {
        dStartY = e.touches[0].clientY;
      } else {
        dStartY = 0;
      }
      dDistY = 0;
    }, { passive: true });

    discenteContainer.addEventListener('touchmove', (e) => {
      if (!dStartY) return;
      dDistY = e.touches[0].clientY - dStartY;
    }, { passive: true });

    discenteContainer.addEventListener('touchend', async () => {
      if (dDistY > 70) {
        const credsRaw = localStorage.getItem('ru_sigaa_creds');
        if (credsRaw) {
          try {
            const creds = JSON.parse(credsRaw);
            await executeSigaaSync(creds.matricula, creds.senha);
          } catch (_) {}
        }
      }
      dStartY = 0;
      dDistY = 0;
    });
  }

  initTheme();
  document.getElementById('themeToggleBtn').onclick = toggleTheme;
  document.getElementById('refreshBtn').onclick = () => {
    const isNotToday = appState.selectedDayIndex !== appState.todayIndex && appState.todayIndex !== -1;
    if (isNotToday) {
      appState.selectedDayIndex = appState.todayIndex;
      renderHomeScreen();
    } else {
      loadMenu(appState.campus);
    }
  };

  // Meal Selector Buttons (Top & Bottom)
  document.querySelectorAll('.meal-top-btn, .meal-control-btn').forEach(btn => {
    btn.onclick = () => {
      appState.meal = btn.getAttribute('data-meal');
      renderHomeScreen();
    };
  });

  setupCampusModal();
  setupNotificationsModal();
  setupCarteirinhaModal();
  setupDiscenteDashboard();
  renderDiscenteDashboard();
  setupSwipeGestures();

  loadMenu();
  registerServiceWorker();
});


let currentSelectedAulasDia = null;
let currentAulasSegment = 'dia';

function getTodayDiaNum() {
  const d = new Date().getDay(); // 0=Sun, 1=Mon, ..., 6=Sat
  return (d >= 1 && d <= 6) ? d : 1;
}

function setupAulasSegmentedControl() {
  const btnDia = document.getElementById('btnAulasTabDia');
  const btnTurmas = document.getElementById('btnAulasTabTurmas');
  const viewDia = document.getElementById('subviewAulasDia');
  const viewTurmas = document.getElementById('subviewAulasTurmas');
  if (!btnDia || !btnTurmas || !viewDia || !viewTurmas) return;

  const switchToSegment = (seg) => {
    currentAulasSegment = seg;
    if (seg === 'dia') {
      btnDia.classList.add('active');
      btnTurmas.classList.remove('active');
      viewDia.style.display = 'flex';
      viewTurmas.style.display = 'none';
      renderAulasDoDia();
    } else {
      btnTurmas.classList.add('active');
      btnDia.classList.remove('active');
      viewTurmas.style.display = 'flex';
      viewDia.style.display = 'none';
      renderTurmasList();
    }
  };

  btnDia.onpointerdown = (e) => {
    e.preventDefault();
    switchToSegment('dia');
  };
  btnTurmas.onpointerdown = (e) => {
    e.preventDefault();
    switchToSegment('turmas');
  };
}

function renderAulasScreen() {
  setupAulasSegmentedControl();

  const countBadge = document.getElementById('turmasCountBadge');
  const classes = appState.disciplinas || [];
  if (countBadge) {
    if (classes.length > 0) {
      countBadge.textContent = classes.length;
      countBadge.style.display = 'inline-block';
    } else {
      countBadge.style.display = 'none';
    }
  }

  if (currentAulasSegment === 'turmas') {
    renderTurmasList();
  } else {
    renderAulasDoDia();
  }
}

function renderAulasDoDia() {
  const dayRail = document.getElementById('aulasDayRail');
  const listEl = document.getElementById('aulasContentList');
  if (!dayRail || !listEl) return;

  if (currentSelectedAulasDia === null) {
    currentSelectedAulasDia = getTodayDiaNum();
  }

  const todayNum = getTodayDiaNum();
  const diasSemana = [
    { num: 1, label: 'SEG' },
    { num: 2, label: 'TER' },
    { num: 3, label: 'QUA' },
    { num: 4, label: 'QUI' },
    { num: 5, label: 'SEX' },
    { num: 6, label: 'SÁB' }
  ];

  dayRail.innerHTML = diasSemana.map(d => {
    const isActive = d.num === currentSelectedAulasDia;
    const isToday = d.num === todayNum;
    return `
      <div class="aulas-day-chip ${isActive ? 'active' : ''}" data-dia="${d.num}">
        <span class="chip-label">${d.label}</span>
        ${isToday ? '<span class="chip-dot"></span>' : ''}
      </div>
    `;
  }).join('');

  dayRail.querySelectorAll('.aulas-day-chip').forEach(chip => {
    chip.onpointerdown = (e) => {
      e.preventDefault();
      currentSelectedAulasDia = parseInt(chip.getAttribute('data-dia'), 10);
      renderAulasDoDia();
    };
  });

  if (!appState.studentCard || !appState.studentCard.isConfigured) {
    listEl.innerHTML = `
      <div class="discente-card not-connected-card" style="text-align: center; padding: 32px 20px;">
        <div class="not-connected-icon-box" style="margin: 0 auto 16px auto;">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="currentColor">
            <path d="M5 13.18v4L12 21l7-3.82v-4L12 17l-7-3.82zM12 3L1 9l11 6 9-4.91V17h2V9L12 3z"/>
          </svg>
        </div>
        <h2 style="font-size: 17px; font-weight: 700; color: var(--text-primary); margin-bottom: 6px;">Grade Horária e Aulas</h2>
        <p style="font-size: 13px; color: var(--text-secondary); line-height: 1.4; margin-bottom: 20px;">Conecte sua conta do SIGAA da UnB para carregar automaticamente suas disciplinas, salas e horários de aula.</p>
        <button id="aulasConnectBtn" class="sigaa-confirm-btn" type="button" style="width: 100%;">
          Sincronizar com SIGAA
        </button>
      </div>
    `;
    const btn = document.getElementById('aulasConnectBtn');
    if (btn) {
      btn.onclick = () => {
        const discenteTab = document.getElementById('navDiscenteBtn');
        if (discenteTab) discenteTab.click();
        const dlgBtn = document.getElementById('openSigaaDialogBtn');
        if (dlgBtn) dlgBtn.click();
      };
    }
    return;
  }

  const classes = appState.disciplinas || [];
  const aulasDoDia = [];
  classes.forEach(c => {
    const time = (c.horarios || []).find(h => h.dia === currentSelectedAulasDia);
    if (time) aulasDoDia.push({ disciplina: c, time: time });
  });

  aulasDoDia.sort((a, b) => (a.time.inicio || '').localeCompare(b.time.inicio || ''));

  if (aulasDoDia.length === 0) {
    const diaNome = ['Domingo', 'Segunda-feira', 'Terça-feira', 'Quarta-feira', 'Quinta-feira', 'Sexta-feira', 'Sábado'][currentSelectedAulasDia] || 'este dia';
    listEl.innerHTML = `
      <div class="discente-card" style="text-align: center; padding: 36px 20px;">
        <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" style="color: var(--text-secondary); opacity: 0.5; margin-bottom: 12px;">
          <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
          <polyline points="22 4 12 14.01 9 11.01"></polyline>
        </svg>
        <h3 style="font-size: 16px; font-weight: 700; color: var(--text-primary); margin-bottom: 4px;">Nenhuma aula neste dia</h3>
        <p style="font-size: 13px; color: var(--text-secondary);">Sem disciplinas agendadas para ${diaNome}.</p>
      </div>
    `;
    return;
  }

  listEl.innerHTML = aulasDoDia.map(({ disciplina, time }) => {
    const outros = (disciplina.horarios || []).filter(h => h.dia !== currentSelectedAulasDia);
    return `
      <div class="aula-card" data-local="${encodeURIComponent(disciplina.local || '')}">
        <div class="aula-card-header">
          <div class="aula-time-badge">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg>
            <span>${time.horarioFormatado || (time.inicio + ' - ' + time.fim)}</span>
          </div>
          <svg class="aula-chevron" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"></polyline></svg>
        </div>
        <div class="aula-name">${disciplina.nome}</div>
        <div class="aula-local-row">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path><circle cx="12" cy="10" r="3"></circle></svg>
          <span>${disciplina.local || 'Sala não informada'}</span>
        </div>
        <div class="aula-details-panel">
          ${(disciplina.codigo || disciplina.turma) ? `<div class="aula-detail-item"><strong>Turma:</strong> ${disciplina.codigo} • Turma ${disciplina.turma}</div>` : ''}
          ${disciplina.docente ? `<div class="aula-detail-item"><strong>Docente:</strong> ${disciplina.docente}</div>` : ''}
          ${disciplina.horarioRaw ? `<div class="aula-detail-item"><strong>Código Horário:</strong> ${disciplina.horarioRaw}</div>` : ''}
          ${outros.length > 0 ? `
            <div class="aula-detail-item">
              <strong>Outros horários na semana:</strong><br/>
              ${outros.map(o => `• ${o.diaNome}: ${o.horarioFormatado}`).join('<br/>')}
            </div>
          ` : ''}
        </div>
      </div>
    `;
  }).join('');

  listEl.querySelectorAll('.aula-card').forEach(card => {
    card.onclick = () => {
      card.classList.toggle('expanded');
    };
  });
}

function renderTurmasList() {
  const listEl = document.getElementById('turmasContentList');
  const countEl = document.getElementById('turmasSummaryCount');
  if (!listEl) return;

  const classes = appState.disciplinas || [];
  if (countEl) {
    countEl.textContent = `${classes.length} turmas matriculadas`;
  }

  if (classes.length === 0) {
    listEl.innerHTML = `
      <div class="discente-card" style="text-align: center; padding: 36px 20px;">
        <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" style="color: var(--text-secondary); opacity: 0.5; margin-bottom: 12px;">
          <path d="M5 13.18v4L12 21l7-3.82v-4L12 17l-7-3.82zM12 3L1 9l11 6 9-4.91V17h2V9L12 3z"/>
        </svg>
        <h3 style="font-size: 16px; font-weight: 700; color: var(--text-primary); margin-bottom: 4px;">Nenhuma turma encontrada</h3>
        <p style="font-size: 13px; color: var(--text-secondary);">Sincronize sua conta com o SIGAA para listar as disciplinas do semestre.</p>
      </div>
    `;
    return;
  }

  const allCourseNames = new Set(classes.map(cl => (cl.nome || '').trim().toLowerCase()));

  listEl.innerHTML = classes.map((c, idx) => {
    const pctMinistradas = typeof c.percentualAulasMinistradas === 'number' ? c.percentualAulasMinistradas : null;
    const textoMinistradas = c.aulasMinistradasTexto || (pctMinistradas !== null ? `${Math.round(pctMinistradas)}% ministradas` : 'Aulas em andamento');
    const pctFreq = typeof c.percentualFrequencia === 'number' ? c.percentualFrequencia : null;
    const faltas = typeof c.faltas === 'number' ? c.faltas : 0;
    const textoFreq = faltas > 0 ? `${Math.round(pctFreq)}% de presença • ${faltas} falta(s)` : `${Math.round(pctFreq)}% de presença`;
    const isAlertaFalta = pctFreq !== null && pctFreq < 75;

    const weeklySchedule = (c.horarios || []).map(h => `${(h.diaNome || '').slice(0, 3)} ${h.horarioFormatado}`).join(' • ');
    const rawNotices = c.noticias || [];
    const notices = rawNotices.filter(n => {
      const t = (n.titulo || '').trim().toLowerCase();
      return t && !allCourseNames.has(t);
    });

    return `
      <div class="turma-card" id="turmaCard_${idx}">
        <div class="turma-header-badges">
          <div class="turma-chips-row">
            ${(c.codigo || c.turma) ? `<span class="turma-code-pill">${c.codigo || 'UNB'} • T${c.turma || '01'}</span>` : ''}
            ${c.cargaHoraria ? `<span class="turma-ch-pill">${c.cargaHoraria}</span>` : ''}
          </div>
          ${c.horarioRaw ? `<span class="turma-horario-raw">${c.horarioRaw}</span>` : ''}
        </div>

        <div class="turma-name">${c.nome}</div>

        ${c.docente ? `
          <div class="turma-info-row">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>
            <span>${c.docente}</span>
          </div>
        ` : ''}

        <div class="turma-info-row">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path><circle cx="12" cy="10" r="3"></circle></svg>
          <span>${c.local || 'Sala não informada'}</span>
        </div>

        ${weeklySchedule ? `
          <div class="turma-info-row">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg>
            <span>${weeklySchedule}</span>
          </div>
        ` : ''}

        <!-- Bloco de Métricas: Aulas Ministradas e Frequência -->
        <div class="turma-metrics-box">
          <div class="metric-line">
            <div class="metric-line-header">
              <span class="metric-line-label">Aulas ministradas</span>
              <span class="metric-line-val">${textoMinistradas}</span>
            </div>
            <div class="metric-bar-track">
              <div class="metric-bar-fill" style="width: ${pctMinistradas !== null ? Math.min(100, Math.max(0, pctMinistradas)) : 0}%;"></div>
            </div>
          </div>
          ${pctFreq !== null ? `
            <div class="metric-line" style="margin-top: 10px;">
              <div class="metric-line-header">
                <span class="metric-line-label">Frequência</span>
                <span class="metric-line-val ${isAlertaFalta ? 'warning' : ''}">${textoFreq}</span>
              </div>
              <div class="metric-bar-track">
                <div class="metric-bar-fill ${isAlertaFalta ? 'warning' : ''}" style="width: ${Math.min(100, Math.max(0, pctFreq))}%;"></div>
              </div>
            </div>
          ` : ''}
        </div>

        <!-- Accordion: Avisos da Turma Virtual -->
        <button class="turma-accordion-toggle" type="button" onclick="toggleTurmaNotices(${idx})">
          <div class="turma-accordion-left">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path><path d="M13.73 21a2 2 0 0 1-3.46 0"></path></svg>
            <span>Avisos da Turma Virtual</span>
            ${notices.length > 0 ? `<span class="segment-counter-badge">${notices.length}</span>` : ''}
          </div>
          <svg class="turma-accordion-chevron" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"></polyline></svg>
        </button>

        <div class="turma-notices-panel">
          ${notices.length === 0 ? `
            <div style="font-size: 12px; color: var(--text-secondary); padding: 4px 0;">Nenhum aviso publicado pelo docente desta turma.</div>
          ` : notices.map((aviso, nIdx) => `
            <div class="turma-notice-item" id="noticeItem_${idx}_${nIdx}" onclick="toggleNoticeItem(${idx}, ${nIdx})">
              <div class="turma-notice-top">
                <div class="turma-notice-title">${aviso.titulo}</div>
                <div style="display: flex; align-items: center; gap: 6px;">
                  ${aviso.data ? `<span class="turma-notice-date">${aviso.data}</span>` : ''}
                  <svg class="turma-notice-chevron" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"></polyline></svg>
                </div>
              </div>
              <div class="turma-notice-body">
                ${aviso.conteudo ? `<div class="turma-notice-content">${aviso.conteudo}</div>` : `<div class="turma-notice-content" style="opacity: 0.5;">Sem descrição adicional neste aviso.</div>`}
                ${(aviso.anexos || []).length > 0 ? `
                  <div style="margin-top: 8px; display: flex; flex-direction: column; gap: 4px;">
                    ${aviso.anexos.map(anexo => `
                      <a class="turma-attachment-btn" href="${anexo.url}" target="_blank" rel="noopener noreferrer" download onclick="event.stopPropagation()">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="7 10 12 15 17 10"></polyline><line x1="12" y1="15" x2="12" y2="3"></line></svg>
                        <span>${anexo.nome}</span>
                      </a>
                    `).join('')}
                  </div>
                ` : ''}
              </div>
            </div>
          `).join('')}
        </div>
      </div>
    `;
  }).join('');
}

window.toggleTurmaNotices = function(idx) {
  const card = document.getElementById(`turmaCard_${idx}`);
  if (card) {
    card.classList.toggle('notices-expanded');
  }
};

window.toggleNoticeItem = function(cardIdx, noticeIdx) {
  const el = document.getElementById(`noticeItem_${cardIdx}_${noticeIdx}`);
  if (el) {
    el.classList.toggle('expanded');
  }
};


