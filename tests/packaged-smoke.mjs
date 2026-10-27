import { _electron as electron } from 'playwright-core';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { mkdir } from 'node:fs/promises';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const packageDir = path.join(root, 'dist', 'Luma-win32-x64');
const artifacts = path.join(root, 'tests', 'artifacts');
await mkdir(artifacts, { recursive: true });

const env = Object.fromEntries(
  Object.entries(process.env).filter(([key]) => key !== 'ELECTRON_RUN_AS_NODE'),
);

const app = await electron.launch({
  executablePath: path.join(packageDir, 'Luma.exe'),
  args: [],
  cwd: packageDir,
  env,
  timeout: 60_000,
});

const page = await app.firstWindow();
const errors = [];
page.on('console', (message) => {
  if (message.type() === 'error') errors.push(message.text());
});
page.on('pageerror', (error) => errors.push(error.message));
await page.evaluate(() => {
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
await page.waitForTimeout(2400);
await page.screenshot({ path: path.join(artifacts, '20-packaged.png') });

const state = await page.evaluate(() => ({
  title: document.title,
  tabs: document.querySelectorAll('.tab').length,
  webviews: document.querySelectorAll('webview').length,
  activeTab: document.querySelector('.tab.active .tab-title')?.textContent,
  settings: JSON.parse(localStorage.getItem('luma.settings.v1') || '{}'),
}));
console.log('PACKAGED_STATE', JSON.stringify(state, null, 2));
console.log('PACKAGED_ERRORS', JSON.stringify(errors));

await app.close();
console.log('PACKAGED_SMOKE_DONE');
