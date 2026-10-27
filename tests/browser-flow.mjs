import { _electron as electron } from 'playwright-core';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { createServer } from 'node:http';
import { readFile } from 'node:fs/promises';
import { mkdir } from 'node:fs/promises';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const artifacts = path.join(root, 'tests', 'artifacts');
await mkdir(artifacts, { recursive: true });

const fixture = await readFile(path.join(root, 'tests', 'fixture', 'index.html'));
const server = createServer((request, response) => {
  response.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
  response.end(fixture);
});
await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve));
const port = server.address().port;
const fixtureUrl = `http://127.0.0.1:${port}/`;

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

const page = await app.firstWindow();
const logs = [];
page.on('console', (message) => {
  if (message.type() === 'error' || message.type() === 'warning') {
    logs.push(`${message.type()}: ${message.text()}`);
  }
});
page.on('pageerror', (error) => logs.push(`pageerror: ${error.stack || error.message}`));

await page.evaluate(() => {
  localStorage.clear();
  localStorage.setItem(
    'luma.settings.v1',
    JSON.stringify({
      position: 'top',
      theme: 'light',
      seed: '#5B5BD6',
      engine: 'bing',
      shortcuts: true,
      transparency: true,
    }),
  );
});
await page.reload();
await page.waitForTimeout(2000);

async function focusAddressAndType(value) {
  await page.click('#url-display');
  await page.fill('#url-input', value);
}

await focusAddressAndType(fixtureUrl);
await page.press('#url-input', 'Enter');
await page.waitForTimeout(2400);
await page.screenshot({ path: path.join(artifacts, '10-page-top.png') });

const pageState = await page.evaluate(() => ({
  title: document.title,
  tabTitle: document.querySelector('.tab.active .tab-title')?.textContent,
  host: document.querySelector('.url-host')?.textContent,
  starDisabled: document.querySelector('#star-button').disabled,
  backDisabled: document.querySelector('#back-button').disabled,
  webviewCount: document.querySelectorAll('webview').length,
  loading: !document.querySelector('#page-progress-container').hidden,
}));
console.log('PAGE_STATE', JSON.stringify(pageState));

await page.click('#star-button');
await page.waitForTimeout(450);
await page.click('#menu-button');
await page.click('[data-command="library"][data-library="bookmarks"]');
await page.waitForTimeout(650);
await page.screenshot({ path: path.join(artifacts, '11-bookmarks.png') });
const bookmarkState = await page.evaluate(() => ({
  saved: document.querySelector('#star-button').classList.contains('saved'),
  items: document.querySelectorAll('.panel-item').length,
  title: document.querySelector('.panel-item-title')?.textContent,
}));
console.log('BOOKMARK_STATE', JSON.stringify(bookmarkState));
await page.click('#panel-close');
await page.waitForTimeout(350);

await page.click('#new-tab-button');
await page.waitForTimeout(550);
await page.fill('#url-input', 'field notes');
await page.waitForTimeout(450);
await page.screenshot({ path: path.join(artifacts, '12-suggestions.png') });
const suggestionState = await page.evaluate(() => ({
  visible: !document.querySelector('#suggestions').hidden,
  count: document.querySelectorAll('.suggestion').length,
  selected: document.querySelector('.suggestion[aria-selected="true"] .suggestion-title')?.textContent,
  tabs: document.querySelectorAll('.tab').length,
}));
console.log('SUGGESTION_STATE', JSON.stringify(suggestionState));
await page.press('#url-input', 'Escape');

await focusAddressAndType('https://this-domain-should-not-exist.invalid/');
await page.press('#url-input', 'Enter');
await page.waitForTimeout(2300);
const errorState = await page.evaluate(() => ({
  visible: !document.querySelector('#error-overlay').hidden,
  title: document.querySelector('#error-title').textContent,
  detail: document.querySelector('#error-detail').textContent,
}));
console.log('ERROR_STATE', JSON.stringify(errorState));
await page.screenshot({ path: path.join(artifacts, '13-error.png') });

await page.click('#menu-button');
await page.click('[data-command="settings"]');
await page.waitForTimeout(600);
await page.click('.position-option[data-position="bottom"]');
await page.locator('#theme-segmented').getByRole('radio', { name: '深色', exact: true }).click();
await page.waitForTimeout(850);
await page.screenshot({ path: path.join(artifacts, '14-dark-settings-bottom.png') });
await page.click('#settings-done');
await page.waitForTimeout(750);
await page.screenshot({ path: path.join(artifacts, '15-dark-bottom.png') });

await page.click('#new-tab-button');
await page.waitForTimeout(900);
await page.screenshot({ path: path.join(artifacts, '16-dark-newtab.png') });
const darkNewTab = await page.evaluate(async () => {
  const webview = document.querySelector('webview:not(.inactive)');
  let guest = null;
  try {
    guest = await webview.executeJavaScript(`(() => {
      const root = getComputedStyle(document.documentElement);
      const clock = document.querySelector('.clock-time');
      const clockStyle = getComputedStyle(clock);
      return {
        theme: document.documentElement.dataset.theme,
        onPrimaryContainer: root.getPropertyValue('--md-sys-color-on-primary-container').trim(),
        primaryContainer: root.getPropertyValue('--md-sys-color-primary-container').trim(),
        clockColor: clockStyle.color,
        clockBackground: getComputedStyle(document.querySelector('.clock-card')).backgroundColor,
      };
    })()`);
  } catch (error) {
    guest = { error: String(error) };
  }
  return {
    theme: document.documentElement.dataset.theme,
    tabs: document.querySelectorAll('.tab').length,
    activeTitle: document.querySelector('.tab.active .tab-title')?.textContent,
    guest,
  };
});
console.log('DARK_NEWTAB', JSON.stringify(darkNewTab));

const finalState = await page.evaluate(() => ({
  toolbar: document.documentElement.dataset.toolbar,
  theme: document.documentElement.dataset.theme,
  settings: JSON.parse(localStorage.getItem('luma.settings.v1') || '{}'),
  history: JSON.parse(localStorage.getItem('luma.history.v1') || '[]').length,
  chromeRect: document.querySelector('#chrome').getBoundingClientRect().toJSON(),
  stageRect: document.querySelector('#page-stage').getBoundingClientRect().toJSON(),
}));
console.log('FINAL_STATE', JSON.stringify(finalState, null, 2));
console.log('LOGS', JSON.stringify(logs, null, 2));

await page.click('#window-maximize');
await page.waitForTimeout(700);
const windowState = await page.evaluate(() => window.luma.getWindowState());
console.log('WINDOW_STATE', JSON.stringify(windowState));

await app.close();
server.close();
console.log('BROWSER_FLOW_DONE');
