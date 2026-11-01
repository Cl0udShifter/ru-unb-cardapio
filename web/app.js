// RU UnB - Web PWA (1:1 Android Port)
// Accurate port of Jetpack Compose UI to Web

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

// State Management
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
  studentCard: JSON.parse(localStorage.getItem('ru_student_card')) || {
    isConfigured: true,
    nome: 'Daniel Alexandro Contreira Junior',
    matricula: '190026308',
    curso: 'GRADUAÇÃO',
    validade: 'Março / 2027',
    codigoQr: '1900263080000000',
    saldo: 'R$ 16,65'
  }
};

function detectCurrentMeal() {
  const now = new Date();
  const mins = now.getHours() * 60 + now.getMinutes();
  if (mins < 9 * 60 + 30) return 'cafe';
  if (mins < 14 * 60 + 30) return 'almoco';
  return 'jantar';
}

function getEffectiveDiet() {
  const cfg = appState.notifications;
  const hasActiveAlerts = (cfg.cafeEnabled && cfg.cafeDays.length > 0) ||
                          (cfg.almocoEnabled && cfg.almocoDays.length > 0) ||
                          (cfg.jantarEnabled && cfg.jantarDays.length > 0);
  return hasActiveAlerts ? (cfg.dietPreference || 'padrao') : 'padrao';
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

// Fetch menu with caching & fallback
async function loadMenu(campusId = appState.campus) {
  const cacheKey = `ru_menu_json_${campusId}`;
  const cached = localStorage.getItem(cacheKey);
  if (cached) {
    try {
      appState.menu = JSON.parse(cached);
      calculateTodayIndex();
      renderHomeScreen();
    } catch (e) {}
  }

  // 1. Try raw github master
  try {
    const res = await fetch(`${RAW_GITHUB_BASE}/menu_${campusId}.json?t=${Date.now()}`);
    if (res.ok) {
      const data = await res.json();
      appState.menu = data;
      localStorage.setItem(cacheKey, JSON.stringify(data));
      calculateTodayIndex();
      renderHomeScreen();
      return;
    }
  } catch (err) {}

  // 2. Fallback to local web/data/
  try {
    const localRes = await fetch(`./data/menu_${campusId}.json`);
    if (localRes.ok) {
      const data = await localRes.json();
      appState.menu = data;
      localStorage.setItem(cacheKey, JSON.stringify(data));
      calculateTodayIndex();
      renderHomeScreen();
    }
  } catch (err) {}
}

function calculateTodayIndex() {
  if (!appState.menu || !appState.menu.dias || appState.menu.dias.length === 0) return;
  const now = new Date();
  const dayOfWeek = now.getDay(); // 0=Sun, 1=Mon..6=Sat
  const mappedIdx = dayOfWeek === 0 ? 6 : dayOfWeek - 1;
  appState.todayIndex = mappedIdx < appState.menu.dias.length ? mappedIdx : -1;
  
  if (appState.selectedDayIndex === 0 && appState.todayIndex !== -1) {
    appState.selectedDayIndex = appState.todayIndex;
  }
}

// Rendering Main UI
function renderHomeScreen() {
  // Update Header Campus Title
  const campusObj = CAMPUSES[appState.campus] || CAMPUSES.darcy;
  document.getElementById('currentCampusLabel').textContent = campusObj.name;

  renderDaySelectorRail();
  renderMealContent();
  updateMealBottomBar();
  updateNotificationBellIcon();
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
    
    // Extract day number (e.g. "26" from "26/10/2026")
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
    cardContainer.innerHTML = '<div style="text-align:center; padding: 24px; color: var(--text-secondary);">Cardápio indisponível para este dia.</div>';
    return;
  }

  const currentDay = appState.menu.dias[appState.selectedDayIndex];
  const dateStr = currentDay.data || '';
  const weekdayStr = currentDay.diaSemana || WEEKDAYS_LONG[appState.selectedDayIndex] || 'Dia';
  
  // Update Day Header: `${currentDay.diaSemana}, ${currentDay.data}`
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
  if (!menu || (!menu.pratoPrincipal && !menu.vegetariano && !menu.vegano)) {
    container.innerHTML = '<div style="text-align:center; padding: 24px; color: var(--text-secondary); font-size: 14px;">Refeitório fechado nesta refeição.</div>';
    return;
  }

  const rawDishes = [];
  if (menu.pratoPrincipal && menu.pratoPrincipal.trim()) {
    rawDishes.push({ type: 'padrao', label: 'Padrão', name: menu.pratoPrincipal });
  }
  if (menu.vegetariano && menu.vegetariano.trim()) {
    rawDishes.push({ type: 'ovolacto', label: 'Ovolacto', name: menu.vegetariano });
  }
  if (menu.vegano && menu.vegano.trim()) {
    rawDishes.push({ type: 'vegano', label: 'Vegano', name: menu.vegano });
  }

  // Sorting based on diet preference
  const sortedDishes = [...rawDishes].sort((a, b) => {
    if (dietPreference === 'ovolacto') {
      const order = { ovolacto: 0, vegano: 1, padrao: 2 };
      return (order[a.type] ?? 3) - (order[b.type] ?? 3);
    }
    if (dietPreference === 'vegano') {
      const order = { vegano: 0, ovolacto: 1, padrao: 2 };
      return (order[a.type] ?? 3) - (order[b.type] ?? 3);
    }
    return 0; // standard
  });

  let html = `<div class="section-label">PRATOS PRINCIPAIS</div>`;
  sortedDishes.forEach((dish, idx) => {
    const isHighlight = (idx === 0 && dish.type === dietPreference) ||
                        (dietPreference === 'padrao' && dish.type === 'padrao');
    html += `
      <div class="dish-item">
        <span class="diet-pill pill-${dish.type}">${dish.label}</span>
        <span class="dish-text ${isHighlight ? 'highlight' : ''}">${escapeHtml(dish.name)}</span>
      </div>
    `;
  });

  // Guarnição & Acompanhamentos
  if (menu.guarnicao || menu.acompanhamento) {
    html += `<div class="notion-divider"></div><div class="section-label">GUARNIÇÃO & ACOMPANHAMENTOS</div>`;
    if (menu.guarnicao) html += renderCleanRow('Guarnição', menu.guarnicao);
    if (menu.acompanhamento) html += renderCleanRow('Acomp.', menu.acompanhamento);
  }

  // Sopa do Dia
  if (menu.sopa || menu.torrada) {
    html += `<div class="notion-divider"></div><div class="section-label">SOPA DO DIA</div>`;
    if (menu.sopa) html += renderCleanRow('Sopa', menu.sopa);
    if (menu.torrada) html += renderCleanRow('Torrada', menu.torrada);
  }

  // Saladas
  if (menu.salada1 || menu.salada2 || menu.molho) {
    html += `<div class="notion-divider"></div><div class="section-label">SALADAS</div>`;
    if (menu.salada1) html += renderCleanRow('Salada 1', menu.salada1);
    if (menu.salada2) html += renderCleanRow('Salada 2', menu.salada2);
    if (menu.molho) html += renderCleanRow('Molho', menu.molho);
  }

  // Sobremesa & Bebida (Clean simple rows without colored chips!)
  if (menu.sobremesa || menu.bebida) {
    html += `<div class="notion-divider"></div><div class="section-label">SOBREMESA & BEBIDA</div>`;
    if (menu.sobremesa) html += renderCleanRow('Sobremesa', menu.sobremesa);
    if (menu.bebida) html += renderCleanRow('Refresco', menu.bebida);
  }

  container.innerHTML = html;
}

// Breakfast Card (BreakfastCard 1:1)
function renderBreakfastCard(container, menu, dietPreference) {
  if (!menu || (!menu.pratoPrincipal && !menu.vegetariano && !menu.vegano)) {
    container.innerHTML = '<div style="text-align:center; padding: 24px; color: var(--text-secondary); font-size: 14px;">Refeitório fechado nesta refeição.</div>';
    return;
  }

  const rawComplements = [];
  if (menu.pratoPrincipal && menu.pratoPrincipal.trim()) {
    rawComplements.push({ type: 'padrao', label: 'Padrão', name: menu.pratoPrincipal });
  }
  if (menu.vegetariano && menu.vegetariano.trim()) {
    rawComplements.push({ type: 'ovolacto', label: 'Ovolacto', name: menu.vegetariano });
  }
  if (menu.vegano && menu.vegano.trim()) {
    rawComplements.push({ type: 'vegano', label: 'Vegano', name: menu.vegano });
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
        <span class="dish-text ${isHighlight ? 'highlight' : ''}">${escapeHtml(dish.name)}</span>
      </div>
    `;
  });

  if (menu.opcaoExtra) {
    html += renderCleanRow('Extra', menu.opcaoExtra);
  }

  if (menu.bebidasQuentes) {
    html += `<div class="notion-divider"></div><div class="section-label">BEBIDAS QUENTES</div>`;
    html += renderCleanRow('Bebidas', menu.bebidasQuentes);
  }

  if (menu.paes) {
    html += `<div class="notion-divider"></div><div class="section-label">PADARIA</div>`;
    html += renderCleanRow('Pães', menu.paes);
  }

  if (menu.fruta) {
    html += `<div class="notion-divider"></div><div class="section-label">FRUTA DO DIA</div>`;
    html += renderCleanRow('Fruta', menu.fruta);
  }

  container.innerHTML = html;
}

function renderCleanRow(label, val) {
  return `
    <div class="clean-item-row">
      <span class="clean-item-label">${label}</span>
      <span class="clean-item-val">${escapeHtml(val)}</span>
    </div>
  `;
}

function escapeHtml(str) {
  if (!str) return '';
  return str.replace(/[&<>"']/g, m => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
  })[m]);
}

function updateMealBottomBar() {
  document.querySelectorAll('.meal-control-btn').forEach(btn => {
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

// Campus Selector Sheet
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

// Notifications & Diet Preference Bottom Sheet
function setupNotificationsModal() {
  const overlay = document.getElementById('notificationsSheetOverlay');
  const openBtn = document.getElementById('openNotificationsBtn');

  openBtn.onclick = () => {
    populateNotificationSettingsUI();
    overlay.classList.add('open');
  };

  overlay.onclick = (e) => {
    if (e.target === overlay || e.target.classList.contains('sheet-drag-handle')) {
      overlay.classList.remove('open');
    }
  };

  // Wire Switches
  document.getElementById('switchCafe').onchange = (e) => {
    appState.notifications.cafeEnabled = e.target.checked;
    saveNotificationsConfig();
  };
  document.getElementById('switchAlmoco').onchange = (e) => {
    appState.notifications.almocoEnabled = e.target.checked;
    saveNotificationsConfig();
  };
  document.getElementById('switchJantar').onchange = (e) => {
    appState.notifications.jantarEnabled = e.target.checked;
    saveNotificationsConfig();
  };

  // Wire Day Dots
  setupDayDotGroup('daysRowCafe', 'cafeDays');
  setupDayDotGroup('daysRowAlmoco', 'almocoDays');
  setupDayDotGroup('daysRowJantar', 'jantarDays');

  // Wire Diet Choices
  document.querySelectorAll('.diet-choice-btn').forEach(btn => {
    btn.onclick = () => {
      const d = btn.getAttribute('data-diet');
      appState.notifications.dietPreference = d;
      saveNotificationsConfig();
      updateDietButtonsUI();
      renderHomeScreen(); // Reorder main card dishes immediately!
    };
  });
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

function populateNotificationSettingsUI() {
  const cfg = appState.notifications;
  document.getElementById('switchCafe').checked = cfg.cafeEnabled;
  document.getElementById('switchAlmoco').checked = cfg.almocoEnabled;
  document.getElementById('switchJantar').checked = cfg.jantarEnabled;

  syncDayDots('daysRowCafe', cfg.cafeDays);
  syncDayDots('daysRowAlmoco', cfg.almocoDays);
  syncDayDots('daysRowJantar', cfg.jantarDays);

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

// Carteirinha Digital View & Logic (StudentCardScreen.kt 1:1)
let qrCodeInstance = null;
let turnstileQrInstance = null;

function setupCarteirinhaModal() {
  const modal = document.getElementById('carteirinhaModal');
  const openBtn = document.getElementById('openCarteirinhaBtn');
  const closeBtn = document.getElementById('closeCarteirinhaBtn');

  openBtn.onclick = () => {
    renderCarteirinhaModal();
    modal.classList.add('open');
  };

  closeBtn.onclick = () => {
    modal.classList.remove('open');
  };

  // Toggle Edit Form
  const editBtn = document.getElementById('editCarteirinhaToggleBtn');
  const editForm = document.getElementById('carteirinhaEditForm');
  const configuredState = document.getElementById('carteirinhaConfiguredState');
  const emptyState = document.getElementById('carteirinhaEmptyState');

  editBtn.onclick = () => {
    const isEditing = editForm.style.display === 'block';
    if (isEditing) {
      editForm.style.display = 'none';
      renderCarteirinhaModal();
    } else {
      showCarteirinhaEditForm();
    }
  };

  document.getElementById('btnPreencherManual').onclick = () => {
    showCarteirinhaEditForm();
  };

  document.getElementById('btnCancelEdit').onclick = () => {
    editForm.style.display = 'none';
    renderCarteirinhaModal();
  };

  document.getElementById('btnSaveCard').onclick = () => {
    appState.studentCard = {
      isConfigured: true,
      nome: document.getElementById('inputNome').value.trim() || 'Estudante UnB',
      matricula: document.getElementById('inputMatricula').value.trim() || '190026308',
      curso: document.getElementById('inputCurso').value.trim() || 'GRADUAÇÃO',
      validade: document.getElementById('inputValidade').value.trim() || 'Março / 2027',
      codigoQr: document.getElementById('inputQrCode').value.trim() || '1900263080000000',
      saldo: appState.studentCard.saldo || 'R$ 16,65'
    };
    localStorage.setItem('ru_student_card', JSON.stringify(appState.studentCard));
    editForm.style.display = 'none';
    renderCarteirinhaModal();
  };

  // Turnstile Fullscreen QR
  const qrBox = document.getElementById('idQrCodeContainer');
  const turnstileOverlay = document.getElementById('turnstileQrOverlay');
  const closeTurnstileBtn = document.getElementById('closeTurnstileQrBtn');

  qrBox.onclick = () => {
    renderTurnstileFullscreenQR();
    turnstileOverlay.classList.add('open');
  };

  closeTurnstileBtn.onclick = () => {
    turnstileOverlay.classList.remove('open');
  };
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
}

function renderCarteirinhaModal() {
  const configuredState = document.getElementById('carteirinhaConfiguredState');
  const emptyState = document.getElementById('carteirinhaEmptyState');
  const form = document.getElementById('carteirinhaEditForm');
  form.style.display = 'none';

  if (!appState.studentCard || !appState.studentCard.isConfigured) {
    emptyState.style.display = 'block';
    configuredState.style.display = 'none';
    return;
  }

  emptyState.style.display = 'none';
  configuredState.style.display = 'block';

  document.getElementById('cardNomeTxt').textContent = appState.studentCard.nome;
  document.getElementById('cardMatriculaTxt').textContent = appState.studentCard.matricula;
  document.getElementById('cardCursoTxt').textContent = appState.studentCard.curso;
  document.getElementById('cardValidadeTxt').textContent = appState.studentCard.validade;
  document.getElementById('cardSaldoTxt').textContent = appState.studentCard.saldo || 'R$ 16,65';

  const qrContainer = document.getElementById('idQrCodeContainer');
  qrContainer.innerHTML = '';
  if (typeof QRCode !== 'undefined') {
    qrCodeInstance = new QRCode(qrContainer, {
      text: appState.studentCard.codigoQr || appState.studentCard.matricula,
      width: 170,
      height: 170,
      colorDark: '#000000',
      colorLight: '#ffffff',
      correctLevel: QRCode.CorrectLevel.M
    });
  }
}

function renderTurnstileFullscreenQR() {
  const container = document.getElementById('turnstileQrCode');
  container.innerHTML = '';
  document.getElementById('turnstileMatriculaTxt').textContent = appState.studentCard.matricula;
  if (typeof QRCode !== 'undefined') {
    turnstileQrInstance = new QRCode(container, {
      text: appState.studentCard.codigoQr || appState.studentCard.matricula,
      width: 270,
      height: 270,
      colorDark: '#000000',
      colorLight: '#ffffff',
      correctLevel: QRCode.CorrectLevel.H
    });
  }
}

// Service Worker Registration
function registerServiceWorker() {
  if ('serviceWorker' in navigator) {
    window.addEventListener('load', () => {
      navigator.serviceWorker.register('sw.js').catch(() => {});
    });
  }
}

// Initialize Application
document.addEventListener('DOMContentLoaded', () => {
  initTheme();
  document.getElementById('themeToggleBtn').onclick = toggleTheme;
  document.getElementById('refreshBtn').onclick = () => loadMenu(appState.campus);

  // Bottom Floating Tabs
  document.querySelectorAll('.meal-control-btn').forEach(btn => {
    btn.onclick = () => {
      appState.meal = btn.getAttribute('data-meal');
      renderHomeScreen();
    };
  });

  setupCampusModal();
  setupNotificationsModal();
  setupCarteirinhaModal();

  loadMenu();
  registerServiceWorker();
});
