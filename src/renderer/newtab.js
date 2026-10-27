import { applyDynamicTheme } from './vendor/md3e/md3-expressive.esm.js';

const ENGINES = {
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
};

const params = new URLSearchParams(window.location.search);
const media = window.matchMedia('(prefers-color-scheme: dark)');

const config = {
  theme: params.get('theme') === 'dark' || params.get('theme') === 'light'
    ? params.get('theme')
    : media.matches ? 'dark' : 'light',
  seed: normalizeSeed(params.get('seed')),
  engine: ENGINES[params.get('engine')] ? params.get('engine') : 'bing',
  links: params.get('links') !== '0',
};

const searchForm = document.querySelector('#search-form');
const searchInput = document.querySelector('#search-input');
const searchEngine = document.querySelector('#search-engine');
const linksPanel = document.querySelector('#links-panel');
const side = document.querySelector('.side');
const clockTime = document.querySelector('#clock-time');
const clockDate = document.querySelector('#clock-date');
const clockDay = document.querySelector('#clock-day');
const topbarDate = document.querySelector('#topbar-date');
const greeting = document.querySelector('#greeting');
const greetingLine = document.querySelector('#greeting-line');

function normalizeSeed(value) {
  if (value && /^#[0-9a-f]{6}$/i.test(`#${value.replace(/^#/, '')}`)) {
    return `#${value.replace(/^#/, '').toUpperCase()}`;
  }
  return '#5B5BD6';
}

function applyTheme(nextConfig) {
  const dark = nextConfig.theme === 'dark';
  document.documentElement.dataset.theme = dark ? 'dark' : 'light';
  document.documentElement.style.colorScheme = dark ? 'dark' : 'light';
  document.documentElement.style.setProperty('--luma-seed', nextConfig.seed);
  try {
    applyDynamicTheme(nextConfig.seed, dark, 'expressive');
  } catch {
    // The baseline md3e tokens remain usable if the color engine fails.
  }
}

function applyConfig(next) {
  Object.assign(config, next);
  if (!ENGINES[config.engine]) config.engine = 'bing';
  config.links = Boolean(config.links);
  config.theme = config.theme === 'dark' ? 'dark' : 'light';

  applyTheme(config);
  searchEngine.textContent = ENGINES[config.engine].label;
  searchInput.placeholder = `用 ${ENGINES[config.engine].label} 搜索，或输入网址`;
  linksPanel.hidden = !config.links;
  side.classList.toggle('single', !config.links);
}

function looksLikeUrl(value) {
  const input = value.trim();
  if (!input || /\s/.test(input)) return null;
  if (/^https?:\/\//i.test(input)) return input;
  if (/^(javascript|data|vbscript|file|blob):/i.test(input)) return null;
  if (/^(localhost|127\.0\.0\.1|\[::1\])(?::\d+)?(?:[/?#].*)?$/i.test(input)) {
    return `http://${input}`;
  }
  if (/^(?:[\w-]+\.)+[a-z]{2,}(?::\d+)?(?:[/?#].*)?$/i.test(input)) {
    return `https://${input}`;
  }
  return null;
}

function destination(value) {
  const direct = looksLikeUrl(value);
  if (direct) return direct;
  return ENGINES[config.engine].search(value.trim());
}

function submitSearch(value) {
  const query = String(value || '').trim();
  if (!query) {
    searchForm.classList.remove('nudge');
    void searchForm.offsetWidth;
    searchForm.classList.add('nudge');
    searchInput.focus();
    return;
  }
  window.location.assign(destination(query));
}

searchForm.addEventListener('submit', (event) => {
  event.preventDefault();
  submitSearch(searchInput.value);
});

document.querySelectorAll('.idea-chip').forEach((chip) => {
  chip.addEventListener('click', () => {
    searchInput.value = chip.dataset.query || chip.textContent.trim();
    submitSearch(searchInput.value);
  });
});

searchInput.addEventListener('keydown', (event) => {
  if (event.key === 'Escape') {
    searchInput.value = '';
    searchInput.blur();
  }
});

document.addEventListener('keydown', (event) => {
  if (event.key === '/' && document.activeElement !== searchInput) {
    event.preventDefault();
    searchInput.focus();
  }
});

function greetingForHour(hour) {
  if (hour >= 5 && hour < 11) return '早上好';
  if (hour >= 11 && hour < 13) return '中午好';
  if (hour >= 13 && hour < 18) return '下午好';
  if (hour >= 18 && hour < 23) return '晚上好';
  return '夜深了';
}

function updateClock() {
  const now = new Date();
  const hh = String(now.getHours()).padStart(2, '0');
  const mm = String(now.getMinutes()).padStart(2, '0');
  const greetingText = greetingForHour(now.getHours());

  clockTime.textContent = `${hh}:${mm}`;
  clockDate.textContent = `${now.getMonth() + 1}月${now.getDate()}日`;
  clockDay.textContent = new Intl.DateTimeFormat('zh-CN', { weekday: 'long' }).format(now);
  topbarDate.textContent = new Intl.DateTimeFormat('zh-CN', {
    month: 'long',
    day: 'numeric',
    weekday: 'short',
  }).format(now);

  if (greeting.textContent !== `${greetingText}。`) {
    greeting.textContent = `${greetingText}。`;
    greetingLine.textContent = greetingText;
  }
  document.title = '新标签页 · Luma';
}

window.__lumaApplySettings = (next = {}) => {
  applyConfig({
    theme: next.theme === 'dark' ? 'dark' : next.theme === 'light' ? 'light' : config.theme,
    seed: normalizeSeed(next.seed || config.seed),
    engine: ENGINES[next.engine] ? next.engine : config.engine,
    links: next.links === undefined ? config.links : Boolean(next.links),
  });
};

media.addEventListener('change', () => {
  const nextTheme = params.get('theme');
  if (nextTheme === 'dark' || nextTheme === 'light') return;
  applyConfig({ theme: media.matches ? 'dark' : 'light' });
});

applyConfig(config);
updateClock();
setInterval(updateClock, 1000);
