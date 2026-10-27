import { _electron as electron } from 'playwright-core';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { mkdir } from 'node:fs/promises';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const artifacts = path.join(root, 'tests', 'artifacts');
await mkdir(artifacts, { recursive: true });

const env = Object.fromEntries(
  Object.entries(process.env).filter(([key]) => key !== 'ELECTRON_RUN_AS_NODE'),
);
env.ELECTRON_ENABLE_LOGGING = '1';

const app = await electron.launch({
  executablePath: path.join(root, 'node_modules', 'electron', 'dist', 'electron.exe'),
  args: ['.'],
  cwd: root,
  env,
  timeout: 45_000,
});

const processHandle = app.process();
processHandle.stderr?.on('data', (chunk) => {
  const text = chunk.toString();
  if (/ERROR|Error|error/.test(text)) process.stdout.write(`[main] ${text}`);
});

const page = await app.firstWindow();
await page.evaluate(() => {
  localStorage.setItem(
    'luma.settings.v1',
    JSON.stringify({
      position: 'top',
      theme: 'system',
      seed: '#5B5BD6',
      engine: 'bing',
      shortcuts: true,
      transparency: true,
    }),
  );
});
await page.reload();
await page.waitForTimeout(2200);

const logs = [];
page.on('console', (message) => {
  if (message.type() === 'error' || message.type() === 'warning') {
    logs.push(`${message.type()}: ${message.text()}`);
  }
});
page.on('pageerror', (error) => logs.push(`pageerror: ${error.stack || error.message}`));

await page.waitForTimeout(2800);
await page.screenshot({ path: path.join(artifacts, '01-newtab.png') });

const snapshot = await page.evaluate(() => ({
  title: document.title,
  tabs: document.querySelectorAll('.tab').length,
  webviews: document.querySelectorAll('webview').length,
  toolbar: document.documentElement.dataset.toolbar,
  theme: document.documentElement.dataset.theme,
  chromeHeight: getComputedStyle(document.documentElement).getPropertyValue('--chrome-h').trim(),
  dialogDefined: Boolean(customElements.get('md-dialog')),
  segmentedDefined: Boolean(customElements.get('md-segmented-button')),
  bodyText: document.body.innerText.slice(0, 180),
}));

console.log(JSON.stringify({ snapshot, logs }, null, 2));

await page.click('#menu-button');
await page.waitForTimeout(350);
await page.screenshot({ path: path.join(artifacts, '02-menu.png') });
await page.click('[data-command="settings"]');
await page.waitForTimeout(800);
await page.screenshot({ path: path.join(artifacts, '03-settings.png') });

const settingsState = await page.evaluate(() => {
  const dialog = document.querySelector('#settings-dialog');
  return {
    open: dialog.open,
    position: document.documentElement.dataset.toolbar,
    rect: dialog.getBoundingClientRect().toJSON(),
  };
});
console.log('settings', JSON.stringify(settingsState));

await page.click('.position-option[data-position="bottom"]');
await page.waitForTimeout(900);
await page.screenshot({ path: path.join(artifacts, '04-settings-bottom.png') });
await page.click('#settings-done');
await page.waitForTimeout(800);
await page.screenshot({ path: path.join(artifacts, '05-bottom.png') });

const bottomState = await page.evaluate(() => ({
  toolbar: document.documentElement.dataset.toolbar,
  chrome: document.querySelector('#chrome').getBoundingClientRect().toJSON(),
  stage: document.querySelector('#page-stage').getBoundingClientRect().toJSON(),
  setting: JSON.parse(localStorage.getItem('luma.settings.v1') || '{}'),
}));
console.log('bottom', JSON.stringify(bottomState, null, 2));

await app.close();
console.log('UI_SMOKE_DONE');
