(() => {
  const KEY = 'luma.settings.v1';
  const DEFAULTS = Object.freeze({
    position: 'top',
    theme: 'system',
    seed: '#5B5BD6',
    engine: 'bing',
    shortcuts: true,
    transparency: true,
  });

  let stored = {};
  try {
    stored = JSON.parse(localStorage.getItem(KEY) || '{}') || {};
  } catch {
    stored = {};
  }

  const settings = { ...DEFAULTS, ...stored };
  if (!['top', 'bottom'].includes(settings.position)) settings.position = DEFAULTS.position;
  if (!['system', 'light', 'dark'].includes(settings.theme)) settings.theme = DEFAULTS.theme;
  if (!['bing', 'google', 'duckduckgo'].includes(settings.engine)) settings.engine = DEFAULTS.engine;
  if (!/^#[0-9a-f]{6}$/i.test(settings.seed || '')) settings.seed = DEFAULTS.seed;
  settings.shortcuts = Boolean(settings.shortcuts);
  settings.transparency = Boolean(settings.transparency);

  const systemDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
  const dark = settings.theme === 'dark' || (settings.theme === 'system' && systemDark);

  const root = document.documentElement;
  root.dataset.theme = dark ? 'dark' : 'light';
  root.dataset.toolbar = settings.position;
  root.dataset.transparency = settings.transparency ? 'on' : 'off';
  root.dataset.platform = navigator.platform.toLowerCase().includes('mac') ? 'mac' : 'win';
  root.style.colorScheme = dark ? 'dark' : 'light';
  root.style.setProperty('--luma-seed', settings.seed);

  window.__LUMA_BOOT__ = { settings, dark, systemDark };
})();
