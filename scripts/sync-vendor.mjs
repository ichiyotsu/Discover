import { cp, mkdir, access } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const source = path.join(root, 'node_modules', '@materialwebunofficial', 'md3e-web', 'dist');
const target = path.join(root, 'src', 'renderer', 'vendor', 'md3e');

try {
  await access(source);
} catch {
  console.error('md3e-web is not installed. Run `pnpm install` first.');
  process.exit(1);
}

await mkdir(target, { recursive: true });
for (const file of ['md3-expressive.esm.js', 'tokens.css', 'LICENSE']) {
  try {
    await cp(path.join(source, file), path.join(target, file));
  } catch {
    // LICENSE lives at the package root, not inside dist.
  }
}
await cp(path.join(source, 'fonts'), path.join(target, 'fonts'), { recursive: true });
await cp(
  path.join(root, 'node_modules', '@materialwebunofficial', 'md3e-web', 'LICENSE'),
  path.join(target, 'LICENSE')
);

console.log('Synced md3e-web runtime assets to src/renderer/vendor/md3e');
