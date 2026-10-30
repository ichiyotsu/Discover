import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { packager } from '@electron/packager';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');

const output = await packager({
  dir: root,
  name: 'Luna',
  platform: 'win32',
  arch: 'x64',
  out: path.join(root, 'dist'),
  overwrite: true,
  asar: true,
  prune: true,
  icon: path.join(root, 'assets', 'icon.ico'),
  appVersion: '0.1.0',
  appCopyright: 'Luna',
  win32metadata: {
    CompanyName: 'Luna',
    FileDescription: 'Luna — a Material 3 Expressive browser',
    ProductName: 'Luna',
    InternalName: 'Luna',
    OriginalFilename: 'Luna.exe',
  },
  ignore: [
    /^\/node_modules($|\/)/,
    /^\/tests($|\/)/,
    /^\/dist($|\/)/,
    /^\/scripts($|\/)/,
    /^\/\.git($|\/)/,
    /^\/docs($|\/)/,
    /^\/README\.md$/,
    /^\/DESIGN\.md$/,
    /^\/THIRD_PARTY_NOTICES\.md$/,
    /^\/pnpm-lock\.yaml$/,
    /^\/pnpm-workspace\.yaml$/,
    /^\/assets\/icon-512\.png$/,
  ],
});

console.log(output.join('\n'));
