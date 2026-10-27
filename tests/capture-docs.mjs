import { _electron as electron } from 'playwright-core';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { createServer } from 'node:http';
import { readFile, mkdir } from 'node:fs/promises';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const docs = path.join(root, 'docs');
await mkdir(docs, { recursive: true });

const fixture = await readFile(path.join(root, 'tests', 'fixture', 'index.html'));
const server = createServer((_request, response) => {
  response.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
  response.end(fixture);
});
await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve));
const fixtureUrl = `http://127.0.0.1:${server.address().port}/`;

const env = Object.fromEntries(
  Object.entries(process.env).filter(([key]) => key !== 'ELECTRON_RUN_AS_NODE'),
);
const app = await electron.launch({
  executablePath: path.join(root, 'node_modules', 'electron', 'dist', 'electron.exe'),
  args: ['.'],
  cwd: root,
  env,
});

const page = await app.firstWindow();
await app.evaluate(({ BrowserWindow }) => {
  const win = BrowserWindow.getAllWindows()[0];
  win.unmaximize();
  win.setSize(1440, 900);
  win.center();
});
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
await page.waitForTimeout(2400);
await page.screenshot({ path: path.join(docs, 'luma-newtab.png') });

await page.click('#url-display');
await page.fill('#url-input', fixtureUrl);
await page.press('#url-input', 'Enter');
await page.waitForTimeout(2200);
await page.screenshot({ path: path.join(docs, 'luma-browsing.png') });

await page.click('#menu-button');
await page.click('[data-command="settings"]');
await page.waitForTimeout(850);
await page.screenshot({ path: path.join(docs, 'luma-settings.png') });

await page.click('.position-option[data-position="bottom"]');
await page.locator('#theme-segmented').getByRole('radio', { name: '深色', exact: true }).click();
await page.waitForTimeout(850);
await page.click('#settings-done');
await page.waitForTimeout(800);
await page.screenshot({ path: path.join(docs, 'luma-bottom-dark.png') });

await app.close();
server.close();
console.log('DOCS_CAPTURED');
