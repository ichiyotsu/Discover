import { applyDynamicTheme } from './vendor/md3e/md3-expressive.esm.js';

/* ==========================================================================
   Constants and small helpers
   ========================================================================== */

const SETTINGS_KEY = 'luma.settings.v1';
const HISTORY_KEY = 'luma.history.v1';
const BOOKMARKS_KEY = 'luma.bookmarks.v1';

const DEFAULTS = Object.freeze({
  position: 'top',
  theme: 'system',
  seed: '#5B5BD6',
  engine: 'bing',
  shortcuts: true,
  transparency: true,
});

const ENGINES = Object.freeze({
  bing: {
    label: 'Bing',
    search: (query) => `https://www.bing.com/search?q=${encodeURIComponent(query)}`,
  },
  google: {
    label: 'Google',
    search: (query) => `https://www.google.com/search?q=${encodeURIComponent(query)}`,
  },
  duckduckgo: {
    label: 'DuckDuckGo',
    search: (query) => `https://duckduckgo.com/?q=${encodeURIComponent(query)}`,
  },
});

const SYSTEM_DARK = window.matchMedia('(prefers-color-scheme: dark)');
const NEWTAB_BASE = new URL('newtab.html', window.location.href).href;
const IS_MAC = (window.luma?.platform || '') === 'darwin';

const $ = (selector) => document.querySelector(selector);
const $$ = (selector) => Array.from(document.querySelectorAll(selector));

const elements = {
  root: document.documentElement,
  chrome: $('#chrome'),
  tabRow: $('#tab-row'),
  tabsViewport: $('#tabs-viewport'),
  tabs: $('#tabs'),
  newTab: $('#new-tab-button'),
  back: $('#back-button'),
  forward: $('#forward-button'),
  reload: $('#reload-button'),
  reloadUse: $('#reload-use'),
  omnibox: $('#omnibox'),
  siteChip: $('#site-chip'),
  siteIcon: $('#site-icon'),
  urlDisplay: $('#url-display'),
  urlInput: $('#url-input'),
  star: $('#star-button'),
  menuButton: $('#menu-button'),
  pageStage: $('#page-stage'),
  pageFrame: $('#page-frame'),
  webviews: $('#webviews'),
  progressContainer: $('#page-progress-container'),
  progress: $('#page-progress'),
  errorOverlay: $('#error-overlay'),
  errorTitle: $('#error-title'),
  errorDetail: $('#error-detail'),
  errorUrl: $('#error-url'),
  errorCode: $('#error-code'),
  errorRetry: $('#error-retry'),
  errorBack: $('#error-back'),
  suggestions: $('#suggestions'),
  suggestionsList: $('#suggestions-list'),
  sitePopover: $('#site-popover'),
  sitePopoverIcon: $('#site-popover-icon'),
  sitePopoverTitle: $('#site-popover-title'),
  sitePopoverOrigin: $('#site-popover-origin'),
  sitePopoverText: $('#site-popover-text'),
  siteCopy: $('#site-copy'),
  appMenu: $('#app-menu'),
  panelScrim: $('#panel-scrim'),
  sidePanel: $('#side-panel'),
  panelTitle: $('#panel-title'),
  panelFilter: $('#panel-filter'),
  panelList: $('#panel-list'),
  panelCount: $('#panel-count'),
  panelClear: $('#panel-clear'),
  panelClose: $('#panel-close'),
  settingsDialog: $('#settings-dialog'),
  settingsDone: $('#settings-done'),
  settingsReset: $('#settings-reset'),
  themeSegmented: $('#theme-segmented'),
  engineSegmented: $('#engine-segmented'),
  shortcutsSwitch: $('#shortcuts-switch'),
  transparencySwitch: $('#transparency-switch'),
  seedGrid: $('#seed-grid'),
  aboutDialog: $('#about-dialog'),
  aboutClose: $('#about-close'),
  aboutVersion: $('#about-version'),
  aboutChrome: $('#about-chrome'),
  aboutElectron: $('#about-electron'),
  aboutProject: $('#about-project'),
  shortcutsDialog: $('#shortcuts-dialog'),
  shortcutsClose: $('#shortcuts-close'),
  toasts: $('#toasts'),
  windowMinimize: $('#window-minimize'),
  windowMaximize: $('#window-maximize'),
  windowClose: $('#window-close'),
  windowMaximizeUse: $('#window-maximize use'),
};

function readJSON(key, fallback) {
  try {
    const parsed = JSON.parse(localStorage.getItem(key) || 'null');
    return parsed ?? fallback;
  } catch {
    return fallback;
  }
}

function writeJSON(key, value) {
  try {
    localStorage.setItem(key, JSON.stringify(value));
  } catch {
    // Storage can be unavailable in unusual deployments; browsing still works.
  }
}

function clamp(value, min, max) {
  return Math.min(max, Math.max(min, value));
}

function escapeHTML(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');
}

function svgIcon(name) {
  return `<svg class="icon" aria-hidden="true"><use href="#i-${name}"></use></svg>`;
}

function debounce(fn, wait = 180) {
  let timer = null;
  return (...args) => {
    clearTimeout(timer);
    timer = setTimeout(() => fn(...args), wait);
  };
}

/* ==========================================================================
   Persistent settings
   ========================================================================== */

function loadSettings() {
  const boot = window.__LUMA_BOOT__?.settings;
  const stored = boot || readJSON(SETTINGS_KEY, {});
  const merged = { ...DEFAULTS, ...stored };
  if (!['top', 'bottom'].includes(merged.position)) merged.position = DEFAULTS.position;
  if (!['system', 'light', 'dark'].includes(merged.theme)) merged.theme = DEFAULTS.theme;
  if (!ENGINES[merged.engine]) merged.engine = DEFAULTS.engine;
  if (!/^#[0-9a-f]{6}$/i.test(merged.seed || '')) merged.seed = DEFAULTS.seed;
  merged.shortcuts = Boolean(merged.shortcuts);
  merged.transparency = Boolean(merged.transparency);
  return merged;
}

let settings = loadSettings();

function saveSettings() {
  writeJSON(SETTINGS_KEY, settings);
}

function resolvedDarkMode() {
  if (settings.theme === 'dark') return true;
  if (settings.theme === 'light') return false;
  return SYSTEM_DARK.matches;
}

function applyTheme({ syncTabs = true } = {}) {
  const dark = resolvedDarkMode();
  const root = elements.root;
  root.dataset.theme = dark ? 'dark' : 'light';
  root.dataset.themeScheme = 'expressive';
  root.dataset.motionScheme = 'expressive';
  root.dataset.paletteVariant = 'expressive';
  root.style.colorScheme = dark ? 'dark' : 'light';
  root.style.setProperty('--luma-seed', settings.seed);

  try {
    applyDynamicTheme(settings.seed, dark, 'expressive');
  } catch (error) {
    console.warn('md3e dynamic theme failed', error);
  }

  if (syncTabs) syncInternalTabs();
}

function applyChromeSettings() {
  elements.root.dataset.toolbar = settings.position;
  elements.root.dataset.transparency = settings.transparency ? 'on' : 'off';
  applyTheme();
  updateChromeHeight();
  syncSettingsControls();
}

/* ==========================================================================
   Theme + chrome sizing
   ========================================================================== */

const chromeObserver = new ResizeObserver(() => updateChromeHeight());
let chromeHeightRaf = 0;

function updateChromeHeight() {
  cancelAnimationFrame(chromeHeightRaf);
  chromeHeightRaf = requestAnimationFrame(() => {
    const height = Math.ceil(elements.chrome.getBoundingClientRect().height);
    if (height > 0) {
      elements.root.style.setProperty('--chrome-h', `${height}px`);
    }
    repositionFloatingUI();
  });
}

/* ==========================================================================
   Browser state
   ========================================================================== */

const state = {
  tabs: new Map(),
  tabOrder: [],
  activeId: null,
  nextId: 1,
  history: readJSON(HISTORY_KEY, []),
  bookmarks: readJSON(BOOKMARKS_KEY, []),
  historyOpen: false,
  bookmarksOpen: false,
  panelOpen: false,
  panelMode: 'history',
  panelFilter: '',
  panelItems: [],
  editing: false,
  suggestions: [],
  suggestionIndex: -1,
  closedTabs: [],
  ignoreBlur: false,
  openPopover: null,
  menuOpen: false,
  fullscreen: false,
  aboutInfoLoaded: false,
};

if (!Array.isArray(state.history)) state.history = [];
if (!Array.isArray(state.bookmarks)) state.bookmarks = [];

function activeTab() {
  return state.activeId ? state.tabs.get(state.activeId) : null;
}

function tabIdFor(tab) {
  return tab.id;
}

function createTab({ url = newTabUrl(), active = true, focusAddress = false } = {}) {
  const id = `tab-${state.nextId++}`;
  const webview = document.createElement('webview');
  webview.setAttribute('partition', 'persist:luma');
  webview.setAttribute('webpreferences', 'contextIsolation=yes,nodeIntegration=no,sandbox=yes');
  webview.setAttribute('allowpopups', '');
  webview.setAttribute('src', url);
  webview.className = 'inactive';

  const tabElement = document.createElement('div');
  tabElement.className = 'tab enter';
  tabElement.dataset.id = id;
  tabElement.setAttribute('role', 'tab');
  tabElement.setAttribute('aria-selected', 'false');
  tabElement.tabIndex = -1;

  const favicon = document.createElement('span');
  favicon.className = 'tab-favicon';
  favicon.appendChild(makeFavicon(null, 'Luna', isNewTabUrl(url)));

  const title = document.createElement('span');
  title.className = 'tab-title';
  title.textContent = isNewTabUrl(url) ? '新标签页' : '正在加载…';

  const close = document.createElement('button');
  close.className = 'tab-close';
  close.type = 'button';
  close.tabIndex = -1;
  close.setAttribute('aria-label', '关闭标签页');
  close.innerHTML = svgIcon('close');

  tabElement.append(favicon, title, close);

  const tab = {
    id,
    url,
    title: isNewTabUrl(url) ? '新标签页' : '正在加载…',
    favicon: null,
    loading: true,
    canGoBack: false,
    canGoForward: false,
    error: null,
    zoom: 1,
    webview,
    element: tabElement,
    faviconElement: favicon,
    titleElement: title,
    closeElement: close,
    recordedUrl: null,
    ready: false,
  };

  state.tabs.set(id, tab);
  state.tabOrder.push(id);

  attachWebviewEvents(tab);
  elements.webviews.appendChild(webview);
  elements.tabs.appendChild(tabElement);

  requestAnimationFrame(() => tabElement.classList.remove('enter'));

  tabElement.addEventListener('click', (event) => {
    if (event.target.closest('.tab-close')) return;
    selectTab(id, { focusPage: true });
  });
  tabElement.addEventListener('auxclick', (event) => {
    if (event.button === 1) {
      event.preventDefault();
      closeTab(id);
    }
  });
  tabElement.addEventListener('keydown', (event) => {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      selectTab(id, { focusPage: true });
    } else if (event.key === 'Delete' || event.key === 'Backspace') {
      event.preventDefault();
      closeTab(id);
    } else if (event.key === 'ArrowRight' || event.key === 'ArrowLeft') {
      event.preventDefault();
      focusAdjacentTab(id, event.key === 'ArrowRight' ? 1 : -1);
    }
  });
  close.addEventListener('click', (event) => {
    event.stopPropagation();
    closeTab(id);
  });

  if (active) {
    selectTab(id, { focusPage: false });
    if (focusAddress) focusAddress();
  } else {
    updateTabElement(tab);
  }

  return tab;
}

function selectTab(id, { focusPage = false } = {}) {
  const tab = state.tabs.get(id);
  if (!tab) return;
  if (state.activeId === id) {
    if (focusPage) tab.webview.focus();
    return;
  }

  state.activeId = id;
  for (const [tabId, candidate] of state.tabs) {
    const selected = tabId === id;
    candidate.element.classList.toggle('active', selected);
    candidate.element.setAttribute('aria-selected', String(selected));
    candidate.element.tabIndex = selected ? 0 : -1;
    candidate.webview.classList.toggle('inactive', !selected);
    if (selected) {
      candidate.webview.style.visibility = '';
    }
  }

  closePopovers();
  closeSuggestions();
  finishEditing({ restoreDisplay: true });

  updateToolbar();
  updateErrorOverlay();
  updateProgress();
  scrollActiveTabIntoView();

  if (focusPage) {
    requestAnimationFrame(() => tab.webview.focus());
  }
}

function focusAdjacentTab(fromId, direction) {
  const index = state.tabOrder.indexOf(fromId);
  if (index === -1) return;
  const nextIndex = (index + direction + state.tabOrder.length) % state.tabOrder.length;
  const nextId = state.tabOrder[nextIndex];
  selectTab(nextId, { focusPage: false });
  const next = state.tabs.get(nextId);
  next?.element.focus();
}

function nextTab(direction) {
  const index = state.tabOrder.indexOf(state.activeId);
  if (index === -1) return;
  const nextIndex = (index + direction + state.tabOrder.length) % state.tabOrder.length;
  selectTab(state.tabOrder[nextIndex], { focusPage: true });
}

function selectTabByIndex(index) {
  if (index === 8) {
    const last = state.tabOrder.at(-1);
    if (last) selectTab(last, { focusPage: true });
    return;
  }
  const id = state.tabOrder[index];
  if (id) selectTab(id, { focusPage: true });
}

function closeTab(id) {
  const tab = state.tabs.get(id);
  if (!tab) return;

  const index = state.tabOrder.indexOf(id);
  const neighbour =
    state.tabOrder[index + 1] || state.tabOrder[index - 1] || null;
  const wasActive = state.activeId === id;

  try {
    if (tab.url && !isInternalUrl(tab.url)) {
      state.closedTabs.push({ url: tab.url, title: tab.title });
      if (state.closedTabs.length > 15) state.closedTabs.shift();
    }
  } catch {
    // Ignore.
  }

  tab.element.classList.add('leaving');
  tab.loading = false;
  try {
    tab.webview.stop();
  } catch {
    // Ignore.
  }

  state.tabs.delete(id);
  state.tabOrder = state.tabOrder.filter((tabId) => tabId !== id);

  setTimeout(() => {
    tab.element.remove();
    tab.webview.remove();
  }, 180);

  if (wasActive) {
    state.activeId = null;
    if (neighbour && state.tabs.has(neighbour)) {
      selectTab(neighbour, { focusPage: true });
    } else if (state.tabs.size === 0) {
      createTab({ url: newTabUrl(), active: true });
    }
  } else {
    updateToolbar();
  }
}

function reopenClosedTab() {
  const last = state.closedTabs.pop();
  if (!last) {
    createTab({ url: newTabUrl(), active: true });
    return;
  }
  createTab({ url: last.url, active: true });
}

function scrollActiveTabIntoView() {
  const tab = activeTab();
  if (!tab) return;
  tab.element.scrollIntoView({ block: 'nearest', inline: 'nearest' });
}

function updateTabElement(tab) {
  tab.titleElement.textContent = tab.title || defaultTitle(tab.url);
  const nextFavicon = makeFavicon(tab.favicon, tab.title, isNewTabUrl(tab.url));
  tab.faviconElement.replaceChildren(nextFavicon);
  tab.element.classList.toggle('loading', tab.loading);
  tab.element.title = tab.title || tab.url;
}

function makeFavicon(url, title, internal = false) {
  if (internal) {
    const span = document.createElement('span');
    span.className = 'internal-spark';
    span.innerHTML = svgIcon('sparkle');
    return span;
  }

  if (url) {
    const image = document.createElement('img');
    image.src = url;
    image.alt = '';
    image.referrerPolicy = 'no-referrer';
    image.addEventListener('error', () => {
      image.replaceWith(makeLetterFavicon(title, internal));
    });
    return image;
  }

  return makeLetterFavicon(title, internal);
}

function makeLetterFavicon(title, internal = false) {
  if (internal) {
    const span = document.createElement('span');
    span.className = 'internal-spark';
    span.innerHTML = svgIcon('sparkle');
    return span;
  }
  const span = document.createElement('span');
  span.className = 'fallback';
  span.textContent = firstLetter(title);
  return span;
}

function firstLetter(title) {
  const cleaned = String(title || '?').replace(/^https?:\/\/(www\.)?/i, '').trim();
  return (cleaned[0] || '?').toUpperCase();
}

function defaultTitle(url) {
  if (isNewTabUrl(url)) return '新标签页';
  try {
    return new URL(url).hostname.replace(/^www\./, '') || url;
  } catch {
    return url || '新标签页';
  }
}

/* ==========================================================================
   Webview events
   ========================================================================== */

function attachWebviewEvents(tab) {
  const webview = tab.webview;

  webview.addEventListener('dom-ready', () => {
    tab.ready = true;
    updateNavigationState(tab);
    if (isNewTabUrl(tab.url) || isNewTabUrl(getWebviewUrl(tab))) {
      syncSingleNewTab(tab);
      if (state.activeId === tab.id && !state.editing) {
        requestAnimationFrame(() => {
          webview
            .executeJavaScript(
              "document.querySelector('.search-input')?.focus({ preventScroll: true })",
              true,
            )
            .catch(() => {});
        });
      }
    }
    if (state.activeId === tab.id) updateToolbar();
  });

  webview.addEventListener('did-start-loading', () => {
    tab.loading = true;
    if (state.activeId === tab.id) {
      updateToolbar();
      updateProgress(true);
    }
  });

  webview.addEventListener('did-stop-loading', () => {
    tab.loading = false;
    updateNavigationState(tab);
    if (state.activeId === tab.id) {
      updateToolbar();
      updateProgress(false);
    }
  });

  webview.addEventListener('did-start-navigation', (event) => {
    if (!event.isMainFrame) return;
    if (tab.error && state.activeId === tab.id) {
      tab.error = null;
      elements.errorOverlay.hidden = true;
    }
  });

  webview.addEventListener('did-navigate', (event) => {
    if (!event.isMainFrame) return;
    tab.url = event.url;
    tab.error = null;
    tab.favicon = null;
    updateNavigationState(tab);
    if (state.activeId === tab.id) {
      updateToolbar();
      if (elements.errorOverlay) elements.errorOverlay.hidden = true;
    }
    updateTabElement(tab);
  });

  webview.addEventListener('did-navigate-in-page', (event) => {
    if (!event.isMainFrame) return;
    tab.url = event.url;
    updateNavigationState(tab);
    if (state.activeId === tab.id) updateToolbar();
    if (/^https?:/i.test(event.url)) recordVisit(tab);
  });

  webview.addEventListener('did-finish-load', () => {
    tab.loading = false;
    updateNavigationState(tab);
    recordVisit(tab);
    if (state.activeId === tab.id) {
      updateToolbar();
      updateProgress(false);
    }
  });

  webview.addEventListener('page-title-updated', (event) => {
    if (isInternalUrl(tab.url) || isInternalUrl(getWebviewUrl(tab))) {
      tab.title = isNewTabUrl(tab.url) ? '新标签页' : 'Luna';
    } else {
      tab.title = event.title?.trim() || defaultTitle(tab.url);
    }
    updateTabElement(tab);
    if (state.activeId === tab.id) {
      document.title = `${tab.title} — Luna`;
      updateHistoryTitle(tab.url, tab.title);
    }
  });

  webview.addEventListener('page-favicon-updated', (event) => {
    const icons = Array.isArray(event.favicons) ? event.favicons : [];
    tab.favicon = icons.at(-1) || null;
    updateTabElement(tab);
  });

  webview.addEventListener('did-fail-load', (event) => {
    if (!event.isMainFrame) return;
    if (event.errorCode === -3) return;
    if (isInternalUrl(event.validatedURL)) return;

    tab.loading = false;
    tab.error = {
      url: event.validatedURL || tab.url,
      code: event.errorCode,
      description: event.errorDescription || '',
    };
    tab.url = tab.error.url;
    tab.title = defaultTitle(tab.error.url);
    updateTabElement(tab);
    if (state.activeId === tab.id) {
      updateToolbar();
      updateErrorOverlay();
      updateProgress(false);
    }
  });

  webview.addEventListener('render-process-gone', () => {
    tab.loading = false;
    showToast('页面进程意外退出，可以重新加载重试。', { icon: 'warning' });
    if (state.activeId === tab.id) updateToolbar();
  });

  webview.addEventListener('enter-html-full-screen', () => {
    elements.root.dataset.pageFullscreen = 'on';
  });
  webview.addEventListener('leave-html-full-screen', () => {
    elements.root.dataset.pageFullscreen = 'off';
  });
}

function getWebviewUrl(tab) {
  try {
    return tab.webview.getURL() || tab.url;
  } catch {
    return tab.url;
  }
}

function updateNavigationState(tab) {
  try {
    tab.canGoBack = tab.webview.canGoBack();
    tab.canGoForward = tab.webview.canGoForward();
  } catch {
    tab.canGoBack = false;
    tab.canGoForward = false;
  }
}

/* ==========================================================================
   Toolbar
   ========================================================================== */

function updateToolbar() {
  const tab = activeTab();
  const loading = Boolean(tab?.loading);
  const canGoBack = Boolean(tab?.canGoBack);
  const canGoForward = Boolean(tab?.canGoForward);

  elements.back.disabled = !canGoBack;
  elements.forward.disabled = !canGoForward;
  elements.reload.setAttribute('aria-label', loading ? '停止加载' : '重新加载');
  elements.reload.title = loading ? '停止加载' : '重新加载 (Ctrl+R)';
  elements.reloadUse.setAttribute('href', loading ? '#i-stop' : '#i-reload');

  if (!tab) {
    elements.urlDisplay.replaceChildren();
    elements.urlInput.value = '';
    elements.star.disabled = true;
    return;
  }

  renderUrlDisplay(tab);
  if (!state.editing) {
    elements.urlInput.value = isNewTabUrl(tab.url) ? '' : tab.url;
  }

  const bookmarked = isBookmarked(tab.url);
  elements.star.classList.toggle('saved', bookmarked);
  elements.star.setAttribute('aria-label', bookmarked ? '移除此书签' : '添加书签');
  elements.star.title = bookmarked ? '移除此书签' : '添加书签';
  elements.star.disabled = isNewTabUrl(tab.url) || isInternalUrl(tab.url);

  document.title = `${tab.title || defaultTitle(tab.url)} — Luna`;
}

function renderUrlDisplay(tab) {
  const container = elements.urlDisplay;
  container.replaceChildren();

  if (isNewTabUrl(tab.url)) {
    const internal = document.createElement('span');
    internal.className = 'url-internal';
    internal.innerHTML = `${svgIcon('sparkle')}<span>新标签页</span>`;
    container.appendChild(internal);
    elements.siteChip.classList.add('secure');
    setSiteIcon('shield');
    return;
  }

  if (isInternalUrl(tab.url)) {
    const internal = document.createElement('span');
    internal.className = 'url-internal';
    internal.innerHTML = `${svgIcon('shield')}<span>Luna 内部页面</span>`;
    container.appendChild(internal);
    elements.siteChip.classList.add('secure');
    setSiteIcon('shield');
    return;
  }

  let parsed = null;
  try {
    parsed = new URL(tab.url);
  } catch {
    // Keep the raw URL below.
  }

  if (!parsed) {
    const raw = document.createElement('span');
    raw.className = 'url-host';
    raw.textContent = tab.url || '';
    container.appendChild(raw);
    setSiteIcon('globe');
    return;
  }

  const secure = parsed.protocol === 'https:';
  elements.siteChip.classList.toggle('secure', secure);
  setSiteIcon(tab.error ? 'warning' : secure ? 'lock' : 'globe');

  const host = document.createElement('span');
  host.className = 'url-host';
  host.textContent = parsed.host || parsed.pathname;
  container.appendChild(host);

  let rest = '';
  if (parsed.pathname && parsed.pathname !== '/') rest += parsed.pathname;
  if (parsed.search) rest += parsed.search;
  if (parsed.hash) rest += parsed.hash;

  if (rest) {
    const restElement = document.createElement('span');
    restElement.className = 'url-rest';
    restElement.textContent = rest;
    container.appendChild(restElement);
  }
}

function setSiteIcon(name) {
  elements.siteIcon.innerHTML = `<use href="#i-${name}"></use>`;
}

function isBookmarked(url) {
  return state.bookmarks.some((item) => item.url === url);
}

function toggleBookmark() {
  const tab = activeTab();
  if (!tab || isNewTabUrl(tab.url) || isInternalUrl(tab.url)) return;
  const existing = state.bookmarks.findIndex((item) => item.url === tab.url);
  if (existing >= 0) {
    state.bookmarks.splice(existing, 1);
    writeJSON(BOOKMARKS_KEY, state.bookmarks);
    showToast('已移除书签');
  } else {
    state.bookmarks.unshift({
      url: tab.url,
      title: tab.title || defaultTitle(tab.url),
      timestamp: Date.now(),
    });
    state.bookmarks = state.bookmarks.slice(0, 300);
    writeJSON(BOOKMARKS_KEY, state.bookmarks);
    showToast('已添加到书签');
  }
  updateToolbar();
  if (state.panelOpen && state.panelMode === 'bookmarks') renderPanel();
}

/* ==========================================================================
   Address editing, navigation, suggestions
   ========================================================================== */

function focusAddress(selectAll = true) {
  if (elements.settingsDialog.open || elements.aboutDialog.open || elements.shortcutsDialog.open) {
    return;
  }
  const tab = activeTab();
  if (!tab) return;

  state.editing = true;
  elements.omnibox.classList.add('editing');
  const value = isNewTabUrl(tab.url) ? '' : tab.url;
  elements.urlInput.value = value;
  elements.urlInput.focus({ preventScroll: true });
  if (selectAll) elements.urlInput.select();
  buildSuggestions(value);
  setTimeout(() => {
    if (state.editing) buildSuggestions(elements.urlInput.value);
  }, 0);
}

function finishEditing({ restoreDisplay = true } = {}) {
  if (!state.editing) return;
  state.editing = false;
  elements.omnibox.classList.remove('editing');
  elements.urlInput.setAttribute('aria-expanded', 'false');
  if (restoreDisplay) {
    const tab = activeTab();
    elements.urlInput.value = tab && !isNewTabUrl(tab.url) ? tab.url : '';
  }
  closeSuggestions();
}

function buildSuggestions(rawQuery) {
  const query = String(rawQuery || '').trim();
  const items = [];
  const seen = new Set();

  const add = (item) => {
    if (!item?.url || seen.has(item.url)) return;
    seen.add(item.url);
    items.push(item);
  };

  if (query) {
    const likelyUrl = looksLikeUrl(query);
    if (likelyUrl) {
      add({
        type: 'url',
        title: query,
        subtitle: '打开网址',
        url: likelyUrl,
        icon: 'globe',
        primary: true,
      });
    }

    const engine = ENGINES[settings.engine];
    add({
      type: 'search',
      title: query,
      subtitle: `使用 ${engine.label} 搜索`,
      url: engine.search(query),
      icon: 'search',
      primary: !likelyUrl,
    });
  } else {
    for (const entry of state.history.slice(0, 5)) {
      add({
        type: 'history',
        title: entry.title || defaultTitle(entry.url),
        subtitle: entry.url,
        url: entry.url,
        icon: 'history',
      });
    }
    for (const entry of state.bookmarks.slice(0, 3)) {
      add({
        type: 'bookmark',
        title: entry.title || defaultTitle(entry.url),
        subtitle: entry.url,
        url: entry.url,
        icon: 'bookmark',
      });
    }
  }

  if (query) {
    const needle = query.toLowerCase();
    const matches = [];
    for (const entry of state.history) {
      if (matches.length >= 4) break;
      if (
        (entry.title || '').toLowerCase().includes(needle) ||
        entry.url.toLowerCase().includes(needle)
      ) {
        matches.push(entry);
      }
    }
    for (const entry of matches) {
      add({
        type: 'history',
        title: entry.title || defaultTitle(entry.url),
        subtitle: entry.url,
        url: entry.url,
        icon: 'history',
      });
    }
  }

  state.suggestions = items;
  state.suggestionIndex = items.length ? 0 : -1;
  renderSuggestions();
}

function looksLikeUrl(value) {
  const input = value.trim();
  if (!input || /\s/.test(input)) return null;
  if (/^(https?|file):\/\//i.test(input)) return input;
  if (/^(javascript|data|vbscript|blob|about):/i.test(input)) return null;
  if (/^(localhost|127\.0\.0\.1|\[::1\])(?::\d+)?(?:[/?#].*)?$/i.test(input)) {
    return `http://${input}`;
  }
  if (/^(?:\d{1,3}\.){3}\d{1,3}(?::\d+)?(?:[/?#].*)?$/.test(input)) {
    return `http://${input}`;
  }
  if (/^(?:[\w-]+\.)+[a-z]{2,}(?::\d+)?(?:[/?#].*)?$/i.test(input)) {
    return `https://${input}`;
  }
  if (/^[\w-]+(?:\.[\w-]+)+(?::\d+)?(?:[/?#].*)?$/.test(input) && input.includes('/')) {
    return `https://${input}`;
  }
  return null;
}

function renderSuggestions() {
  const list = elements.suggestionsList;
  list.replaceChildren();
  if (!state.editing || state.suggestions.length === 0) {
    closeSuggestions();
    return;
  }

  const fragment = document.createDocumentFragment();
  state.suggestions.forEach((item, index) => {
    const row = document.createElement('button');
    row.type = 'button';
    row.className = 'suggestion';
    row.dataset.index = String(index);
    row.setAttribute('role', 'option');
    row.setAttribute('aria-selected', String(index === state.suggestionIndex));
    row.innerHTML = `
      <span class="suggestion-icon ${item.primary ? 'primary' : ''}">${svgIcon(item.icon || 'globe')}</span>
      <span class="suggestion-text">
        <span class="suggestion-title">${escapeHTML(item.title)}</span>
        <span class="suggestion-subtitle">${escapeHTML(item.subtitle)}</span>
      </span>
      ${item.type === 'search' ? '<span class="suggestion-badge">搜索</span>' : item.type === 'bookmark' ? '<span class="suggestion-badge">书签</span>' : ''}
    `;
    fragment.appendChild(row);
  });
  list.appendChild(fragment);

  elements.suggestions.hidden = false;
  elements.urlInput.setAttribute('aria-expanded', 'true');
  positionSuggestions();
}

function closeSuggestions() {
  elements.suggestions.hidden = true;
  elements.urlInput.setAttribute('aria-expanded', 'false');
  state.suggestionIndex = -1;
}

function positionSuggestions() {
  if (elements.suggestions.hidden) return;
  const rect = elements.omnibox.getBoundingClientRect();
  const maxWidth = Math.min(760, window.innerWidth - 24);
  const width = clamp(rect.width, 300, maxWidth);
  const left = clamp(rect.left + Math.min(8, rect.width * 0.02), 12, window.innerWidth - width - 12);
  const openUp = settings.position === 'bottom';
  const estimatedHeight = Math.min(430, state.suggestions.length * 54 + 48);

  elements.suggestions.style.width = `${width}px`;
  elements.suggestions.style.left = `${left}px`;
  elements.suggestions.dataset.placement = openUp ? 'top' : 'bottom';
  if (openUp) {
    elements.suggestions.style.top = 'auto';
    elements.suggestions.style.bottom = `${window.innerHeight - rect.top + 8}px`;
  } else {
    elements.suggestions.style.bottom = 'auto';
    elements.suggestions.style.top = `${Math.min(rect.bottom + 8, window.innerHeight - estimatedHeight - 12)}px`;
  }
}

function updateSuggestionSelection(nextIndex) {
  if (!state.suggestions.length) return;
  state.suggestionIndex =
    (nextIndex + state.suggestions.length) % state.suggestions.length;
  $$('.suggestion').forEach((row) => {
    row.setAttribute(
      'aria-selected',
      String(Number(row.dataset.index) === state.suggestionIndex),
    );
  });
  const selected = $(`.suggestion[data-index="${state.suggestionIndex}"]`);
  selected?.scrollIntoView({ block: 'nearest' });
}

function commitSuggestion(index) {
  const item = state.suggestions[index];
  if (!item) return;
  navigateTo(item.url);
}

function navigateTo(rawInput) {
  const tab = activeTab();
  if (!tab) return;

  let input = String(rawInput || '').trim();
  if (!input) return;

  if (/^luma:\/\//i.test(input)) {
    const command = input.slice(7).replace(/\/+$/, '').toLowerCase();
    finishEditing();
    if (command === 'settings') openSettings();
    else if (command === 'history') openPanel('history');
    else if (command === 'bookmarks') openPanel('bookmarks');
    else if (command === 'newtab') loadTabUrl(tab, newTabUrl());
    else showToast('没有这个 Luna 内部页面。', { icon: 'info' });
    return;
  }

  if (/^(javascript|data|vbscript):/i.test(input)) {
    showToast('出于安全考虑，地址栏不允许运行脚本。', { icon: 'warning' });
    return;
  }

  if (/^file:/i.test(input)) {
    showToast('出于安全考虑，地址栏只能打开网页地址。', { icon: 'warning' });
    return;
  }

  const direct = looksLikeUrl(input);
  if (direct) {
    input = direct;
  } else if (!/^[a-z][a-z0-9+.-]*:\/\//i.test(input)) {
    input = ENGINES[settings.engine].search(input);
  }

  finishEditing();
  loadTabUrl(tab, input);
  requestAnimationFrame(() => tab.webview.focus());
}

function loadTabUrl(tab, url) {
  tab.error = null;
  tab.url = url;
  tab.loading = true;
  tab.favicon = null;
  updateTabElement(tab);
  if (state.activeId === tab.id) {
    elements.errorOverlay.hidden = true;
    updateToolbar();
    updateProgress(true);
  }
  try {
    const result = tab.webview.loadURL(url);
    if (result && typeof result.catch === 'function') result.catch(() => {});
  } catch {
    tab.webview.src = url;
  }
}

/* ==========================================================================
   Error overlay and progress
   ========================================================================== */

function errorHeadline(code) {
  const map = {
    '-106': '网络连接已断开',
    '-105': '找不到这个网站',
    '-102': '连接被拒绝',
    '-100': '连接被关闭',
    '-118': '连接超时',
    '-7': '连接超时',
    '-21': '网络连接已中断',
    '-201': '网站的安全证书有问题',
    '-202': '网站的身份无法验证',
    '-300': '页面跳转次数过多',
  };
  return map[String(code)] || '无法访问此网站';
}

function errorDescription(code, description = '') {
  const map = {
    '-106': '请检查 Wi-Fi 或网线连接，然后重试。',
    '-105': '请检查网址是否拼写正确，或尝试搜索这个网站。',
    '-102': '目标网站拒绝了连接。可能是网站暂时无法访问，或端口没有开放。',
    '-100': '连接被意外关闭。网站可能暂时不可用，或网络代理中断了请求。',
    '-118': '网站响应时间过长。你的网络可能较慢，或网站暂时不可用。',
    '-7': '网站响应时间过长。请稍后重试。',
    '-21': '网络连接在加载过程中被中断。',
    '-201': '这个网站的证书不受信任。为了安全，Luna 没有继续加载它。',
    '-202': '无法确认这个网站的身份。为了安全，Luna 没有继续加载它。',
    '-300': '页面重定向次数过多，可能需要清除此网站的 Cookie。',
  };
  return map[String(code)] || (description ? `错误详情：${description}` : '页面暂时无法加载，请稍后重试。');
}

function updateErrorOverlay() {
  const tab = activeTab();
  if (!tab?.error) {
    elements.errorOverlay.hidden = true;
    return;
  }
  elements.errorOverlay.hidden = false;
  elements.errorTitle.textContent = errorHeadline(tab.error.code);
  elements.errorDetail.textContent = errorDescription(tab.error.code, tab.error.description);
  elements.errorUrl.textContent = tab.error.url;
  elements.errorCode.textContent = `NET ${tab.error.code}${tab.error.description ? ` · ${tab.error.description}` : ''}`;
}

function updateProgress(show) {
  const tab = activeTab();
  const visible = Boolean(show && tab?.loading);
  elements.progressContainer.hidden = !visible;

  if (visible) {
    elements.progress.indeterminate = true;
    elements.progress.removeAttribute('value');
  } else {
    elements.progress.indeterminate = false;
    elements.progress.value = 100;
    setTimeout(() => {
      if (!activeTab()?.loading) elements.progressContainer.hidden = true;
    }, 220);
  }
}

/* ==========================================================================
   History and bookmarks
   ========================================================================== */

function recordVisit(tab) {
  if (!tab.url || tab.error || isInternalUrl(tab.url) || !/^https?:/i.test(tab.url)) return;
  if (tab.recordedUrl === tab.url) return;
  tab.recordedUrl = tab.url;

  const now = Date.now();
  const existing = state.history.findIndex((item) => item.url === tab.url);
  if (existing >= 0 && now - state.history[existing].timestamp < 60_000) {
    state.history[existing].timestamp = now;
    state.history[existing].title = tab.title || state.history[existing].title;
  } else {
    state.history.unshift({
      url: tab.url,
      title: tab.title || defaultTitle(tab.url),
      timestamp: now,
    });
  }

  state.history = state.history
    .sort((a, b) => b.timestamp - a.timestamp)
    .slice(0, 500);
  writeJSON(HISTORY_KEY, state.history);
  if (state.panelOpen && state.panelMode === 'history') renderPanel();
}

function updateHistoryTitle(url, title) {
  if (!url || !title) return;
  const entry = state.history.find((item) => item.url === url);
  if (entry && entry.title !== title) {
    entry.title = title;
    writeJSON(HISTORY_KEY, state.history);
  }
}

function removeHistoryItem(url) {
  state.history = state.history.filter((item) => item.url !== url);
  writeJSON(HISTORY_KEY, state.history);
  renderPanel();
}

function removeBookmarkItem(url) {
  state.bookmarks = state.bookmarks.filter((item) => item.url !== url);
  writeJSON(BOOKMARKS_KEY, state.bookmarks);
  if (activeTab()?.url === url) updateToolbar();
  renderPanel();
}

function formatPanelTime(timestamp) {
  const date = new Date(timestamp);
  const now = new Date();
  const time = date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
  if (date.toDateString() === now.toDateString()) return time;
  const yesterday = new Date(now);
  yesterday.setDate(now.getDate() - 1);
  if (date.toDateString() === yesterday.toDateString()) return `昨天 ${time}`;
  if (date.getFullYear() === now.getFullYear()) {
    return `${date.getMonth() + 1}月${date.getDate()}日`;
  }
  return `${date.getFullYear()}/${date.getMonth() + 1}/${date.getDate()}`;
}

function openPanel(mode = 'history') {
  closePopovers();
  closeSuggestions();
  state.panelMode = mode;
  state.panelFilter = '';
  elements.panelFilter.value = '';
  elements.sidePanel.classList.remove('closing');
  elements.sidePanel.hidden = false;
  elements.panelScrim.hidden = false;
  state.panelOpen = true;
  renderPanel();
  setTimeout(() => elements.panelClose.focus({ preventScroll: true }), 30);
}

function closePanel() {
  if (!state.panelOpen) return;
  state.panelOpen = false;
  elements.sidePanel.classList.add('closing');
  elements.panelScrim.hidden = true;
  setTimeout(() => {
    elements.sidePanel.hidden = true;
    elements.sidePanel.classList.remove('closing');
  }, 200);
}

function setPanelMode(mode) {
  state.panelMode = mode;
  state.panelFilter = '';
  elements.panelFilter.value = '';
  renderPanel();
}

function renderPanel() {
  const isHistory = state.panelMode === 'history';
  elements.panelTitle.textContent = isHistory ? '历史记录' : '书签';
  elements.panelFilter.placeholder = isHistory ? '搜索历史记录' : '搜索书签';
  elements.panelClear.textContent = isHistory ? '清空历史' : '清空书签';

  $$('.panel-tab').forEach((tab) => {
    const selected = tab.dataset.panel === state.panelMode;
    tab.classList.toggle('active', selected);
    tab.setAttribute('aria-selected', String(selected));
  });

  const filter = state.panelFilter.trim().toLowerCase();
  const source = isHistory ? state.history : state.bookmarks;
  const items = source.filter((item) => {
    if (!filter) return true;
    return (
      (item.title || '').toLowerCase().includes(filter) ||
      item.url.toLowerCase().includes(filter)
    );
  });

  state.panelItems = items;
  elements.panelList.replaceChildren();
  elements.panelCount.textContent = `${items.length} 条${isHistory ? '记录' : '书签'}`;
  elements.panelClear.disabled = items.length === 0 && source.length === 0;

  if (!items.length) {
    const empty = document.createElement('div');
    empty.className = 'panel-empty';
    empty.innerHTML = `
      <span class="panel-empty-icon">${svgIcon(isHistory ? 'history' : 'bookmark')}</span>
      <strong>${filter ? '没有匹配的内容' : isHistory ? '还没有浏览记录' : '还没有书签'}</strong>
      <span>${filter ? '换个关键词试试。' : isHistory ? '你访问过的网页会出现在这里。' : '点击地址栏右侧的星星即可收藏网页。'}</span>
    `;
    elements.panelList.appendChild(empty);
    return;
  }

  const fragment = document.createDocumentFragment();
  items.forEach((item, index) => {
    const row = document.createElement('div');
    row.className = 'panel-item';
    row.dataset.index = String(index);
    row.setAttribute('role', 'button');
    row.tabIndex = 0;

    const favicon = document.createElement('span');
    favicon.className = 'panel-item-favicon';
    favicon.textContent = firstLetter(item.title || item.url);

    const main = document.createElement('span');
    main.className = 'panel-item-main';
    const title = document.createElement('span');
    title.className = 'panel-item-title';
    title.textContent = item.title || defaultTitle(item.url);
    const url = document.createElement('span');
    url.className = 'panel-item-url';
    url.textContent = item.url;
    main.append(title, url);

    const time = document.createElement('span');
    time.className = 'panel-item-time';
    time.textContent = formatPanelTime(item.timestamp || Date.now());

    const remove = document.createElement('button');
    remove.className = 'panel-item-remove';
    remove.type = 'button';
    remove.dataset.remove = String(index);
    remove.setAttribute('aria-label', isHistory ? '删除这条记录' : '删除这个书签');
    remove.innerHTML = svgIcon('close');

    row.append(favicon, main, time, remove);
    fragment.appendChild(row);

    row.addEventListener('click', (event) => {
      if (event.target.closest('.panel-item-remove')) return;
      openInActiveTab(item.url);
      closePanel();
    });
    row.addEventListener('keydown', (event) => {
      if (event.key === 'Enter' || event.key === ' ') {
        event.preventDefault();
        openInActiveTab(item.url);
        closePanel();
      }
    });
    remove.addEventListener('click', (event) => {
      event.stopPropagation();
      if (isHistory) removeHistoryItem(item.url);
      else removeBookmarkItem(item.url);
    });
  });
  elements.panelList.appendChild(fragment);
}

function openInActiveTab(url) {
  const tab = activeTab();
  if (!tab) {
    createTab({ url, active: true });
    return;
  }
  loadTabUrl(tab, url);
}

function clearPanelItems() {
  const isHistory = state.panelMode === 'history';
  const source = isHistory ? state.history : state.bookmarks;
  if (!source.length) return;
  const backup = source.slice();

  if (isHistory) {
    state.history = [];
    writeJSON(HISTORY_KEY, state.history);
  } else {
    state.bookmarks = [];
    writeJSON(BOOKMARKS_KEY, state.bookmarks);
    updateToolbar();
  }
  renderPanel();
  showToast(isHistory ? '历史记录已清空' : '书签已清空', {
    icon: isHistory ? 'history' : 'bookmark',
    actionLabel: '撤销',
    onAction: () => {
      if (isHistory) {
        state.history = backup;
        writeJSON(HISTORY_KEY, state.history);
      } else {
        state.bookmarks = backup;
        writeJSON(BOOKMARKS_KEY, state.bookmarks);
        updateToolbar();
      }
      renderPanel();
      showToast('已恢复');
    },
  });
}

/* ==========================================================================
   Floating UI: site popover, app menu
   ========================================================================== */

function closePopovers() {
  elements.sitePopover.hidden = true;
  elements.appMenu.hidden = true;
  elements.menuButton.setAttribute('aria-expanded', 'false');
  state.openPopover = null;
  state.menuOpen = false;
}

function toggleSitePopover() {
  if (!elements.sitePopover.hidden) {
    elements.sitePopover.hidden = true;
    state.openPopover = null;
    return;
  }
  closePopovers();
  const tab = activeTab();
  if (!tab) return;

  const isNewTab = isNewTabUrl(tab.url);
  const isSecure = !isNewTab && /^https:/i.test(tab.url);
  const origin = isNewTab ? '由 Luna 本地生成' : (() => {
    try {
      return new URL(tab.url).origin;
    } catch {
      return tab.url;
    }
  })();

  elements.sitePopoverTitle.textContent = isNewTab
    ? 'Luna 新标签页'
    : isSecure
      ? '连接安全'
      : '连接不安全';
  elements.sitePopoverOrigin.textContent = origin;
  elements.sitePopoverIcon.classList.toggle('warning', !isNewTab && !isSecure);
  elements.sitePopoverIcon.innerHTML = svgIcon(isNewTab ? 'shield' : isSecure ? 'lock' : 'warning');
  elements.sitePopoverText.textContent = isNewTab
    ? '这个页面在本地生成，搜索只会在你提交后发送到所选的搜索引擎。'
    : isSecure
      ? '此网站使用 HTTPS 加密连接。Chromium 已校验它的安全证书。'
      : '此网站没有使用 HTTPS。不要在这里输入密码或支付信息。';

  const rect = elements.siteChip.getBoundingClientRect();
  const width = 304;
  const left = clamp(rect.left - 10, 12, window.innerWidth - width - 12);
  elements.sitePopover.style.left = `${left}px`;
  elements.sitePopover.style.width = `${width}px`;
  if (settings.position === 'bottom') {
    elements.sitePopover.style.top = 'auto';
    elements.sitePopover.style.bottom = `${window.innerHeight - rect.top + 10}px`;
  } else {
    elements.sitePopover.style.bottom = 'auto';
    elements.sitePopover.style.top = `${rect.bottom + 10}px`;
  }
  elements.sitePopover.hidden = false;
  state.openPopover = 'site';
}

function toggleAppMenu() {
  if (!elements.appMenu.hidden) {
    closePopovers();
    return;
  }
  closePopovers();
  const rect = elements.menuButton.getBoundingClientRect();
  const width = 282;
  const left = clamp(rect.right - width, 12, window.innerWidth - width - 12);
  elements.appMenu.style.left = `${left}px`;
  elements.appMenu.style.width = `${width}px`;
  if (settings.position === 'bottom') {
    elements.appMenu.style.top = 'auto';
    elements.appMenu.style.bottom = `${window.innerHeight - rect.top + 10}px`;
  } else {
    elements.appMenu.style.bottom = 'auto';
    elements.appMenu.style.top = `${rect.bottom + 10}px`;
  }
  elements.appMenu.hidden = false;
  elements.menuButton.setAttribute('aria-expanded', 'true');
  state.openPopover = 'menu';
  state.menuOpen = true;
}

function repositionFloatingUI() {
  if (!elements.suggestions.hidden) positionSuggestions();
  if (!elements.sitePopover.hidden) {
    elements.sitePopover.hidden = true;
    toggleSitePopover();
  }
  if (!elements.appMenu.hidden) {
    elements.appMenu.hidden = true;
    toggleAppMenu();
  }
}

/* ==========================================================================
   Settings dialog
   ========================================================================== */

function syncSettingsControls() {
  if (!elements.themeSegmented) return;
  const themeIndex = ['system', 'light', 'dark'].indexOf(settings.theme);
  elements.themeSegmented.selectedIndex = themeIndex >= 0 ? themeIndex : 0;
  const engineIndex = ['bing', 'google', 'duckduckgo'].indexOf(settings.engine);
  elements.engineSegmented.selectedIndex = engineIndex >= 0 ? engineIndex : 0;
  elements.shortcutsSwitch.checked = settings.shortcuts;
  elements.transparencySwitch.checked = settings.transparency;

  $$('.position-option').forEach((option) => {
    const checked = option.dataset.position === settings.position;
    option.setAttribute('aria-checked', String(checked));
  });

  $$('.seed-chip').forEach((chip) => {
    chip.setAttribute('aria-pressed', String(chip.dataset.seed.toUpperCase() === settings.seed.toUpperCase()));
  });
}

function openSettings() {
  closePopovers();
  closeSuggestions();
  finishEditing({ restoreDisplay: true });
  syncSettingsControls();
  document.querySelector('.settings')?.scrollTo({ top: 0 });
  if (!elements.settingsDialog.open) elements.settingsDialog.show();
}

function setPosition(position, { animate = true } = {}) {
  if (!['top', 'bottom'].includes(position) || settings.position === position) return;
  const previous = settings.position;
  settings.position = position;
  saveSettings();
  elements.root.dataset.toolbar = position;
  syncSettingsControls();
  updateChromeHeight();

  if (animate) {
    elements.chrome.animate(
      [
        {
          opacity: 0.55,
          transform: `translateY(${previous === 'top' ? '-14px' : '14px'})`,
        },
        { opacity: 1, transform: 'translateY(0)' },
      ],
      { duration: 380, easing: 'cubic-bezier(0.16, 1, 0.3, 1)' },
    );
  }
}

function setTheme(theme) {
  if (!['system', 'light', 'dark'].includes(theme)) return;
  settings.theme = theme;
  saveSettings();
  applyTheme();
  syncSettingsControls();
}

function setSeed(seed) {
  if (!/^#[0-9a-f]{6}$/i.test(seed)) return;
  settings.seed = seed.toUpperCase();
  saveSettings();
  applyTheme();
  syncSettingsControls();
}

function setEngine(engine) {
  if (!ENGINES[engine]) return;
  settings.engine = engine;
  saveSettings();
  syncSettingsControls();
  syncInternalTabs();
}

function resetSettings() {
  settings = { ...DEFAULTS };
  saveSettings();
  applyChromeSettings();
  syncInternalTabs();
  showToast('已恢复默认设置', { icon: 'reset' });
}

/* ==========================================================================
   Internal pages
   ========================================================================== */

function newTabUrl() {
  const url = new URL(NEWTAB_BASE);
  url.searchParams.set('theme', resolvedDarkMode() ? 'dark' : 'light');
  url.searchParams.set('seed', settings.seed);
  url.searchParams.set('engine', settings.engine);
  url.searchParams.set('links', settings.shortcuts ? '1' : '0');
  return url.href;
}

function isNewTabUrl(url) {
  if (!url) return false;
  try {
    const parsed = new URL(url);
    return parsed.protocol === 'file:' && /\/newtab\.html$/i.test(parsed.pathname);
  } catch {
    return false;
  }
}

function isInternalUrl(url) {
  if (!url) return false;
  try {
    const parsed = new URL(url);
    return parsed.protocol === 'file:' && /\/(newtab|error)\.html$/i.test(parsed.pathname);
  } catch {
    return false;
  }
}

function syncSingleNewTab(tab) {
  if (!tab.ready) return;
  const payload = {
    theme: resolvedDarkMode() ? 'dark' : 'light',
    seed: settings.seed,
    engine: settings.engine,
    links: settings.shortcuts,
  };
  tab.webview
    .executeJavaScript(
      `window.__lumaApplySettings && window.__lumaApplySettings(${JSON.stringify(payload)})`,
      true,
    )
    .catch(() => {});
}

function syncInternalTabs() {
  for (const tab of state.tabs.values()) {
    if (!isNewTabUrl(tab.url)) continue;
    if (tab.ready) syncSingleNewTab(tab);
  }
}

/* ==========================================================================
   Toast
   ========================================================================== */

function showToast(message, { icon = 'check', duration = 3000, actionLabel = '', onAction = null } = {}) {
  const toast = document.createElement('div');
  toast.className = 'toast';
  toast.innerHTML = `
    <span class="toast-icon">${svgIcon(icon)}</span>
    <span class="toast-text">${escapeHTML(message)}</span>
  `;

  let timeout = null;
  const dismiss = () => {
    clearTimeout(timeout);
    if (!toast.isConnected || toast.classList.contains('leaving')) return;
    toast.classList.add('leaving');
    setTimeout(() => toast.remove(), 240);
  };

  if (actionLabel) {
    const action = document.createElement('button');
    action.type = 'button';
    action.textContent = actionLabel;
    action.addEventListener('click', () => {
      onAction?.();
      dismiss();
    });
    toast.appendChild(action);
  }

  toast.addEventListener('click', (event) => {
    if (event.target.closest('button')) return;
    dismiss();
  });

  elements.toasts.appendChild(toast);
  while (elements.toasts.children.length > 3) elements.toasts.firstElementChild?.remove();
  timeout = setTimeout(dismiss, duration);
}

/* ==========================================================================
   Window controls and app menu actions
   ========================================================================== */

function updateWindowControls(maximized) {
  elements.windowMaximize.setAttribute('aria-label', maximized ? '还原' : '最大化');
  elements.windowMaximize.title = maximized ? '还原' : '最大化';
  elements.windowMaximizeUse.setAttribute('href', maximized ? '#i-restore' : '#i-maximize');
}

async function loadAboutInfo() {
  if (state.aboutInfoLoaded) return;
  try {
    const info = await window.luma.getAppInfo();
    elements.aboutVersion.textContent = info.version || '0.1.0';
    elements.aboutChrome.textContent = info.chrome || '—';
    elements.aboutElectron.textContent = info.electron || '—';
    state.aboutInfoLoaded = true;
  } catch {
    // Keep the placeholders.
  }
}

function handleMenuCommand(command, item) {
  closePopovers();
  switch (command) {
    case 'new-tab':
      createTab({ url: newTabUrl(), active: true, focusAddress: true });
      break;
    case 'library':
      openPanel(item?.dataset.library || 'history');
      break;
    case 'downloads':
      window.luma.openDownloads().then((ok) => {
        if (!ok) showToast('无法打开下载文件夹', { icon: 'warning' });
      });
      break;
    case 'settings':
      openSettings();
      break;
    case 'shortcuts':
      elements.shortcutsDialog.show();
      break;
    case 'about':
      loadAboutInfo();
      elements.aboutDialog.show();
      break;
    case 'quit':
      window.luma.quit();
      break;
    default:
      break;
  }
}

/* ==========================================================================
   Event wiring
   ========================================================================== */

function wireEvents() {
  elements.newTab.addEventListener('click', () => {
    createTab({ url: newTabUrl(), active: true });
    requestAnimationFrame(() => focusAddress());
  });

  elements.back.addEventListener('click', () => {
    const tab = activeTab();
    if (!tab) return;
    try {
      if (tab.webview.canGoBack()) tab.webview.goBack();
    } catch {
      // Ignore.
    }
  });

  elements.forward.addEventListener('click', () => {
    const tab = activeTab();
    if (!tab) return;
    try {
      if (tab.webview.canGoForward()) tab.webview.goForward();
    } catch {
      // Ignore.
    }
  });

  elements.reload.addEventListener('click', () => {
    const tab = activeTab();
    if (!tab) return;
    try {
      if (tab.loading) tab.webview.stop();
      else tab.webview.reload();
    } catch {
      // Ignore.
    }
  });

  elements.star.addEventListener('click', toggleBookmark);
  elements.siteChip.addEventListener('click', toggleSitePopover);
  elements.menuButton.addEventListener('click', toggleAppMenu);

  elements.urlDisplay.addEventListener('click', () => focusAddress(true));
  elements.omnibox.addEventListener('mousedown', (event) => {
    if (event.target.closest('.url-display') && !state.editing) {
      // Let the click handler focus the input.
    }
  });

  elements.urlInput.addEventListener('focus', () => {
    if (state.editing) return;
    state.editing = true;
    elements.omnibox.classList.add('editing');
    buildSuggestions(elements.urlInput.value);
  });

  elements.urlInput.addEventListener('input', (event) => {
    if (event.isComposing) return;
    buildSuggestions(elements.urlInput.value);
  });

  elements.urlInput.addEventListener('compositionend', () => {
    buildSuggestions(elements.urlInput.value);
  });

  elements.urlInput.addEventListener('keydown', (event) => {
    if (event.isComposing) return;
    if (event.key === 'ArrowDown') {
      event.preventDefault();
      updateSuggestionSelection(state.suggestionIndex + 1);
    } else if (event.key === 'ArrowUp') {
      event.preventDefault();
      updateSuggestionSelection(state.suggestionIndex - 1);
    } else if (event.key === 'Enter') {
      event.preventDefault();
      if (state.suggestionIndex >= 0 && state.suggestions[state.suggestionIndex]) {
        commitSuggestion(state.suggestionIndex);
      } else {
        navigateTo(elements.urlInput.value);
      }
    } else if (event.key === 'Escape') {
      event.preventDefault();
      if (!elements.suggestions.hidden) closeSuggestions();
      else {
        finishEditing({ restoreDisplay: true });
        elements.urlInput.blur();
      }
    } else if (event.key === 'Tab') {
      closeSuggestions();
    }
  });

  elements.urlInput.addEventListener('blur', () => {
    if (state.ignoreBlur) return;
    setTimeout(() => {
      if (!state.editing) return;
      if (document.activeElement === elements.urlInput) return;
      if (elements.suggestions.contains(document.activeElement)) return;
      finishEditing({ restoreDisplay: true });
    }, 90);
  });

  elements.suggestionsList.addEventListener('mousedown', (event) => {
    event.preventDefault();
    state.ignoreBlur = true;
    setTimeout(() => {
      state.ignoreBlur = false;
    }, 0);
  });

  elements.suggestionsList.addEventListener('click', (event) => {
    const row = event.target.closest('.suggestion');
    if (!row) return;
    commitSuggestion(Number(row.dataset.index));
  });

  elements.siteCopy.addEventListener('click', async () => {
    const tab = activeTab();
    if (!tab) return;
    try {
      await navigator.clipboard.writeText(tab.url);
      showToast('网址已复制');
    } catch {
      showToast('无法复制网址', { icon: 'warning' });
    }
    closePopovers();
  });

  elements.appMenu.addEventListener('click', (event) => {
    const item = event.target.closest('.menu-item');
    if (!item) return;
    handleMenuCommand(item.dataset.command, item);
  });

  elements.windowMinimize.addEventListener('click', () => window.luma.minimize());
  elements.windowMaximize.addEventListener('click', () => window.luma.toggleMaximize());
  elements.windowClose.addEventListener('click', () => window.luma.close());

  elements.settingsDone.addEventListener('click', () => elements.settingsDialog.close('done'));
  elements.settingsReset.addEventListener('click', resetSettings);
  elements.aboutClose.addEventListener('click', () => elements.aboutDialog.close('done'));
  elements.shortcutsClose.addEventListener('click', () => elements.shortcutsDialog.close('done'));
  elements.aboutProject.addEventListener('click', () => {
    window.luma.openExternal('https://materialwebunofficial.github.io/#home');
  });

  elements.themeSegmented.addEventListener('change', (event) => {
    const index = event.detail?.selectedIndex;
    const theme = ['system', 'light', 'dark'][index];
    if (theme) setTheme(theme);
  });

  elements.engineSegmented.addEventListener('change', (event) => {
    const index = event.detail?.selectedIndex;
    const engine = ['bing', 'google', 'duckduckgo'][index];
    if (engine) setEngine(engine);
  });

  elements.shortcutsSwitch.addEventListener('change', () => {
    settings.shortcuts = Boolean(elements.shortcutsSwitch.checked);
    saveSettings();
    syncInternalTabs();
  });

  elements.transparencySwitch.addEventListener('change', () => {
    settings.transparency = Boolean(elements.transparencySwitch.checked);
    saveSettings();
    elements.root.dataset.transparency = settings.transparency ? 'on' : 'off';
  });

  $$('.position-option').forEach((option) => {
    option.addEventListener('click', () => setPosition(option.dataset.position));
  });

  elements.seedGrid.addEventListener('click', (event) => {
    const chip = event.target.closest('.seed-chip');
    if (!chip) return;
    setSeed(chip.dataset.seed);
  });

  elements.panelClose.addEventListener('click', closePanel);
  elements.panelScrim.addEventListener('click', closePanel);
  elements.panelClear.addEventListener('click', clearPanelItems);
  elements.panelFilter.addEventListener('input', () => {
    state.panelFilter = elements.panelFilter.value;
    renderPanel();
  });
  $$('.panel-tab').forEach((tab) => {
    tab.addEventListener('click', () => setPanelMode(tab.dataset.panel));
  });

  elements.errorRetry.addEventListener('click', () => {
    const tab = activeTab();
    if (!tab?.error) return;
    const url = tab.error.url;
    tab.error = null;
    elements.errorOverlay.hidden = true;
    loadTabUrl(tab, url);
  });

  elements.errorBack.addEventListener('click', () => {
    const tab = activeTab();
    if (!tab) return;
    tab.error = null;
    elements.errorOverlay.hidden = true;
    try {
      if (tab.webview.canGoBack()) tab.webview.goBack();
      else loadTabUrl(tab, newTabUrl());
    } catch {
      loadTabUrl(tab, newTabUrl());
    }
  });

  elements.tabsViewport.addEventListener(
    'wheel',
    (event) => {
      if (Math.abs(event.deltaY) <= Math.abs(event.deltaX)) return;
      const target = elements.tabs;
      if (target.scrollWidth <= target.clientWidth) return;
      target.scrollLeft += event.deltaY;
      event.preventDefault();
    },
    { passive: false },
  );

  document.addEventListener('pointerdown', (event) => {
    if (state.openPopover === 'site' && !event.target.closest('#site-popover, #site-chip')) {
      closePopovers();
    }
    if (state.menuOpen && !event.target.closest('#app-menu, #menu-button')) {
      closePopovers();
    }
    if (
      state.editing &&
      !event.target.closest('#omnibox, #suggestions') &&
      !event.target.closest('md-dialog')
    ) {
      finishEditing({ restoreDisplay: true });
    }
  });

  document.addEventListener('keydown', (event) => {
    if (event.key === 'Escape') {
      if (state.openPopover) {
        closePopovers();
        return;
      }
      if (state.panelOpen) {
        closePanel();
        return;
      }
      if (state.editing) {
        finishEditing({ restoreDisplay: true });
        elements.urlInput.blur();
      }
    }
  });

  SYSTEM_DARK.addEventListener('change', () => {
    if (settings.theme === 'system') applyTheme();
  });

  window.addEventListener('resize', debounce(updateChromeHeight, 60));
  window.addEventListener('resize', debounce(positionSuggestions, 80));

  chromeObserver.observe(elements.chrome);

  window.luma.onMaximized(updateWindowControls);
  window.luma.onFullscreen((fullscreen) => {
    state.fullscreen = fullscreen;
    elements.root.dataset.fullscreen = fullscreen ? 'on' : 'off';
    updateChromeHeight();
  });
  window.luma.onCommand(handleAppCommand);
  window.luma.onDownload(handleDownload);

  window.luma
    .getWindowState()
    .then((value) => updateWindowControls(Boolean(value?.maximized)))
    .catch(() => {});

  elements.settingsDialog.addEventListener('close', () => {
    syncSettingsControls();
  });
}

function handleAppCommand(command) {
  const tab = activeTab();
  switch (command?.name) {
    case 'focus-address':
      focusAddress(true);
      break;
    case 'new-tab':
      if (command.url) createTab({ url: command.url, active: !command.background });
      else {
        createTab({ url: newTabUrl(), active: true });
        requestAnimationFrame(() => focusAddress());
      }
      break;
    case 'reopen-tab':
      reopenClosedTab();
      break;
    case 'close-tab':
      if (state.activeId) closeTab(state.activeId);
      break;
    case 'next-tab':
      nextTab(1);
      break;
    case 'previous-tab':
      nextTab(-1);
      break;
    case 'select-tab':
      selectTabByIndex(command.index);
      break;
    case 'reload':
      try {
        tab?.webview.reload();
      } catch {
        // Ignore.
      }
      break;
    case 'hard-reload':
      try {
        tab?.webview.reloadIgnoringCache();
      } catch {
        // Ignore.
      }
      break;
    case 'back':
      try {
        if (tab?.webview.canGoBack()) tab.webview.goBack();
      } catch {
        // Ignore.
      }
      break;
    case 'forward':
      try {
        if (tab?.webview.canGoForward()) tab.webview.goForward();
      } catch {
        // Ignore.
      }
      break;
    case 'settings':
      openSettings();
      break;
    case 'zoom':
      changeZoom(command);
      break;
    case 'stop':
      if (tab?.loading) {
        try {
          tab.webview.stop();
        } catch {
          // Ignore.
        }
      } else if (state.openPopover) {
        closePopovers();
      } else if (state.panelOpen) {
        closePanel();
      }
      break;
    case 'library':
      openPanel(command.mode === 'bookmarks' ? 'bookmarks' : 'history');
      break;
    case 'downloads':
      window.luma.openDownloads();
      break;
    case 'search':
      if (command.text) {
        createTab({ url: ENGINES[settings.engine].search(command.text), active: true });
      }
      break;
    default:
      break;
  }
}

function changeZoom(command) {
  const tab = activeTab();
  if (!tab) return;
  if (command.reset) tab.zoom = 1;
  else tab.zoom = clamp(Number((tab.zoom + (command.delta || 0) * 0.1).toFixed(2)), 0.3, 3);
  try {
    tab.webview.setZoomFactor(tab.zoom);
    showToast(`缩放 ${Math.round(tab.zoom * 100)}%`);
  } catch {
    // Ignore.
  }
}

function handleDownload(payload) {
  if (!payload) return;
  if (payload.state === 'started') {
    showToast(`正在下载 ${payload.filename}`, { icon: 'download', duration: 3800 });
  } else if (payload.state === 'completed') {
    showToast(`已下载 ${payload.filename}`, {
      icon: 'download',
      duration: 5000,
      actionLabel: '打开文件夹',
      onAction: () => window.luma.openDownloads(),
    });
  } else if (payload.state === 'interrupted' || payload.state === 'cancelled') {
    showToast(`下载失败：${payload.filename}`, { icon: 'warning', duration: 5000 });
  }
}

/* ==========================================================================
   Boot
   ========================================================================== */

function init() {
  if (IS_MAC) elements.root.dataset.platform = 'mac';
  applyChromeSettings();
  wireEvents();
  createTab({ url: newTabUrl(), active: true });
  updateWindowControls(false);

  setTimeout(() => {
    if (activeTab() && isNewTabUrl(activeTab().url)) {
      // Let the new-tab page own focus; Ctrl+L is always one keystroke away.
    }
  }, 0);
}

init();
