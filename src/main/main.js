'use strict';

const {
  app,
  BrowserWindow,
  Menu,
  clipboard,
  dialog,
  ipcMain,
  screen,
  session,
  shell,
} = require('electron');
const path = require('node:path');
const fs = require('node:fs');
const { fileURLToPath } = require('node:url');

const ROOT = path.resolve(__dirname, '..', '..');
const RENDERER_DIR = path.join(ROOT, 'src', 'renderer');
const PRELOAD = path.join(__dirname, 'preload.js');
const IS_DEV = process.argv.includes('--dev');

app.setName('Luma');
app.setAppUserModelId('app.luma.browser');
Menu.setApplicationMenu(null);

app.userAgentFallback =
  'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 ' +
  `(KHTML, like Gecko) Chrome/${process.versions.chrome} Safari/537.36`;

const gotLock = app.requestSingleInstanceLock();
if (!gotLock) {
  app.quit();
}

let mainWindow = null;
let saveTimer = null;

const ALLOWED_PERMISSIONS = new Set([
  'fullscreen',
  'pointerLock',
  'keyboardLock',
  'clipboard-sanitized-write',
]);

const permissionPrompts = new Map();
const permissionLabels = {
  media: '使用摄像头或麦克风',
  geolocation: '获取你的位置',
  notifications: '发送通知',
  'clipboard-read': '读取剪贴板',
  'display-capture': '录制屏幕',
  midi: '使用 MIDI 设备',
  'midiSysex': '使用 MIDI 设备',
  'openExternal': '打开外部应用',
  'idle-detection': '检测空闲状态',
  'window-management': '管理窗口',
  'speaker-selection': '选择扬声器',
};

function statePath() {
  return path.join(app.getPath('userData'), 'window-state.json');
}

function readWindowState() {
  try {
    const parsed = JSON.parse(fs.readFileSync(statePath(), 'utf8'));
    if (!parsed || typeof parsed !== 'object') return {};
    return parsed;
  } catch {
    return {};
  }
}

function normalizeWindowState(state) {
  const fallback = { width: 1380, height: 900 };
  const width = Number.isFinite(state.width)
    ? Math.max(900, Math.min(state.width, 3840))
    : fallback.width;
  const height = Number.isFinite(state.height)
    ? Math.max(620, Math.min(state.height, 2400))
    : fallback.height;

  let x;
  let y;
  if (Number.isFinite(state.x) && Number.isFinite(state.y)) {
    const visible = screen.getAllDisplays().some((display) => {
      const area = display.workArea;
      return (
        state.x + width > area.x + 80 &&
        state.x < area.x + area.width - 80 &&
        state.y + height > area.y + 40 &&
        state.y < area.y + area.height - 40
      );
    });
    if (visible) {
      x = state.x;
      y = state.y;
    }
  }
  return { width, height, x, y, maximized: Boolean(state.maximized) };
}

function writeWindowState() {
  if (!mainWindow || mainWindow.isDestroyed()) return;
  try {
    const bounds = mainWindow.getNormalBounds();
    fs.writeFileSync(
      statePath(),
      JSON.stringify(
        {
          ...bounds,
          maximized: mainWindow.isMaximized(),
        },
        null,
        2,
      ),
    );
  } catch {
    // Window state is a nicety; never let it break browsing.
  }
}

function scheduleWindowStateSave() {
  clearTimeout(saveTimer);
  saveTimer = setTimeout(writeWindowState, 350);
}

function sendCommand(name, detail = {}) {
  if (!mainWindow || mainWindow.isDestroyed()) return;
  mainWindow.webContents.send('browser:command', { name, ...detail });
}

function isInternalFile(url) {
  try {
    const parsed = new URL(url);
    if (parsed.protocol !== 'file:') return false;
    const file = path.resolve(fileURLToPath(parsed));
    const rel = path.relative(RENDERER_DIR, file);
    return rel === '' || (!rel.startsWith('..') && !path.isAbsolute(rel));
  } catch {
    return false;
  }
}

function isBrowserUrl(url) {
  if (!url) return false;
  if (/^https?:/i.test(url)) return true;
  if (/^about:blank$/i.test(url)) return true;
  return isInternalFile(url);
}

function openExternalSafely(url) {
  try {
    const parsed = new URL(url);
    if (['http:', 'https:', 'mailto:', 'tel:'].includes(parsed.protocol)) {
      void shell.openExternal(url);
      return true;
    }
  } catch {
    // Ignore malformed URLs.
  }
  return false;
}

function trustedSender(event) {
  return Boolean(mainWindow) && event.sender === mainWindow.webContents;
}

function attachPermissionHandlers(browserSession) {
  browserSession.setPermissionCheckHandler((_webContents, permission) => {
    return ALLOWED_PERMISSIONS.has(permission);
  });

  browserSession.setPermissionRequestHandler((webContents, permission, callback, details) => {
    if (ALLOWED_PERMISSIONS.has(permission)) {
      callback(true);
      return;
    }
    if (!permissionLabels[permission]) {
      callback(false);
      return;
    }

    let origin = '此网站';
    try {
      origin = new URL(details.requestingUrl || webContents.getURL()).hostname || origin;
    } catch {
      // Keep the generic origin.
    }

    const key = `${origin}:${permission}`;
    const cached = permissionPrompts.get(key);
    if (cached !== undefined) {
      callback(cached);
      return;
    }

    const options = {
      type: 'question',
      buttons: ['允许', '阻止'],
      defaultId: 1,
      cancelId: 1,
      noLink: true,
      title: '权限请求',
      message: `${origin} 想要${permissionLabels[permission]}`,
      detail: '只有在信任此网站时才允许。此决定会在本次运行期间记住。',
    };

    const prompt = mainWindow
      ? dialog.showMessageBox(mainWindow, options)
      : dialog.showMessageBox(options);

    prompt
      .then((result) => {
        const allowed = result.response === 0;
        permissionPrompts.set(key, allowed);
        callback(allowed);
      })
      .catch(() => callback(false));
  });
}

function attachDownloadHandlers(browserSession) {
  browserSession.on('will-download', (_event, item) => {
    let directory;
    try {
      directory = app.getPath('downloads');
      fs.mkdirSync(directory, { recursive: true });
    } catch {
      return;
    }

    const requested = item.getFilename() || 'download';
    const extension = path.extname(requested);
    const stem = path.basename(requested, extension) || 'download';
    let target = path.join(directory, requested);
    let counter = 1;
    while (fs.existsSync(target) && counter < 1000) {
      target = path.join(directory, `${stem} (${counter})${extension}`);
      counter += 1;
    }

    item.setSavePath(target);
    const payload = {
      filename: path.basename(target),
      path: target,
      totalBytes: item.getTotalBytes(),
    };
    mainWindow?.webContents.send('browser:download', { state: 'started', ...payload });

    item.once('done', (_downloadEvent, state) => {
      mainWindow?.webContents.send('browser:download', {
        state,
        ...payload,
        receivedBytes: item.getReceivedBytes(),
      });
    });
  });
}

function showGuestContextMenu(contents, params) {
  const template = [];

  if (params.linkURL) {
    template.push(
      {
        label: '在新标签页中打开链接',
        click: () => sendCommand('new-tab', { url: params.linkURL }),
      },
      {
        label: '复制链接地址',
        click: () => clipboard.writeText(params.linkURL),
      },
      { type: 'separator' },
    );
  }

  if (params.selectionText) {
    template.push(
      { label: '复制', role: 'copy', enabled: params.editFlags.canCopy },
      {
        label: `搜索 “${params.selectionText.slice(0, 24)}${params.selectionText.length > 24 ? '…' : ''}”`,
        click: () => sendCommand('search', { text: params.selectionText }),
      },
      { type: 'separator' },
    );
  } else if (params.isEditable) {
    template.push(
      { label: '撤销', role: 'undo', enabled: params.editFlags.canUndo },
      { label: '重做', role: 'redo', enabled: params.editFlags.canRedo },
      { type: 'separator' },
      { label: '剪切', role: 'cut', enabled: params.editFlags.canCut },
      { label: '复制', role: 'copy', enabled: params.editFlags.canCopy },
      { label: '粘贴', role: 'paste', enabled: params.editFlags.canPaste },
      { label: '全选', role: 'selectAll' },
      { type: 'separator' },
    );
  } else if (params.selectionText === '') {
    template.push(
      {
        label: '后退',
        enabled: contents.navigationHistory?.canGoBack() ?? contents.canGoBack(),
        click: () => (contents.navigationHistory?.goBack() ?? contents.goBack()),
      },
      {
        label: '前进',
        enabled: contents.navigationHistory?.canGoForward() ?? contents.canGoForward(),
        click: () => (contents.navigationHistory?.goForward() ?? contents.goForward()),
      },
      { label: '重新加载', click: () => contents.reload() },
      { type: 'separator' },
    );
  }

  template.push({
    label: '检查',
    visible: IS_DEV,
    click: () => contents.inspectElement(params.x, params.y),
  });

  if (!template.length) return;
  Menu.buildFromTemplate(template).popup({ window: mainWindow || undefined });
}

function registerGuest(contents) {
  contents.setWindowOpenHandler(({ url }) => {
    if (/^https?:/i.test(url)) {
      sendCommand('new-tab', { url, background: false });
    } else if (/^(mailto|tel):/i.test(url)) {
      openExternalSafely(url);
    }
    return { action: 'deny' };
  });

  contents.on('will-navigate', (event, url) => {
    if (isBrowserUrl(url)) return;
    event.preventDefault();
    openExternalSafely(url);
  });

  contents.on('before-input-event', handleBrowserShortcut);

  contents.on('context-menu', (_event, params) => {
    showGuestContextMenu(contents, params);
  });

  contents.on('will-prevent-unload', (event) => {
    const options = {
      type: 'question',
      buttons: ['离开', '留下'],
      defaultId: 1,
      cancelId: 1,
      noLink: true,
      title: '离开此网站？',
      message: '此页面可能不会保存你的更改。',
    };
    const choice = mainWindow
      ? dialog.showMessageBoxSync(mainWindow, options)
      : dialog.showMessageBoxSync(options);
    if (choice === 0) event.preventDefault();
  });
}

function handleBrowserShortcut(event, input) {
  if (input.type !== 'keyDown' || input.isAutoRepeat) return;
  const ctrl = input.control || input.meta;
  const shift = input.shift;
  const alt = input.alt;
  const code = input.code;

  const run = (name, detail) => {
    event.preventDefault();
    sendCommand(name, detail);
  };

  if (ctrl && !alt && (code === 'KeyL' || code === 'KeyK')) {
    run('focus-address');
    return;
  }
  if (ctrl && !alt && code === 'KeyT') {
    run(shift ? 'reopen-tab' : 'new-tab');
    return;
  }
  if (ctrl && !alt && code === 'KeyW') {
    run('close-tab');
    return;
  }
  if (ctrl && code === 'Tab') {
    run(shift ? 'previous-tab' : 'next-tab');
    return;
  }
  if (ctrl && (code === 'PageDown' || code === 'PageUp')) {
    run(code === 'PageDown' ? 'next-tab' : 'previous-tab');
    return;
  }
  if (ctrl && /^Digit[1-9]$/.test(code)) {
    run('select-tab', { index: Number(code.slice(-1)) - 1 });
    return;
  }
  if (ctrl && !alt && code === 'KeyR') {
    run(shift ? 'hard-reload' : 'reload');
    return;
  }
  if (!ctrl && !alt && code === 'F5') {
    run(shift ? 'hard-reload' : 'reload');
    return;
  }
  if (alt && !ctrl && (code === 'ArrowLeft' || code === 'ArrowRight')) {
    run(code === 'ArrowLeft' ? 'back' : 'forward');
    return;
  }
  if (ctrl && !alt && code === 'Comma') {
    run('settings');
    return;
  }
  if (ctrl && !alt && code === 'KeyH') {
    run('library', { mode: 'history' });
    return;
  }
  if (ctrl && !alt && code === 'KeyJ') {
    run('downloads');
    return;
  }
  if (ctrl && !alt && (code === 'Equal' || code === 'NumpadAdd')) {
    run('zoom', { delta: 1 });
    return;
  }
  if (ctrl && !alt && (code === 'Minus' || code === 'NumpadSubtract')) {
    run('zoom', { delta: -1 });
    return;
  }
  if (ctrl && !alt && code === 'Digit0') {
    run('zoom', { reset: true });
    return;
  }
  if (code === 'F11') {
    event.preventDefault();
    if (!mainWindow) return;
    mainWindow.setFullScreen(!mainWindow.isFullScreen());
    return;
  }
  if (!ctrl && !alt && code === 'Escape') {
    run('stop');
  }
}

function createWindow() {
  const state = normalizeWindowState(readWindowState());

  mainWindow = new BrowserWindow({
    width: state.width,
    height: state.height,
    x: state.x,
    y: state.y,
    minWidth: 860,
    minHeight: 620,
    show: false,
    frame: false,
    backgroundColor: '#141218',
    title: 'Luma',
    roundedCorners: true,
    hasShadow: true,
    icon: path.join(ROOT, 'assets', 'icon.png'),
    webPreferences: {
      preload: PRELOAD,
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true,
      webviewTag: true,
      spellcheck: true,
      backgroundThrottling: false,
    },
  });

  mainWindow.webContents.on('will-attach-webview', (event, webPreferences, params) => {
    const src = params.src || '';
    if (src && !isBrowserUrl(src)) {
      event.preventDefault();
      return;
    }

    delete webPreferences.preload;
    webPreferences.nodeIntegration = false;
    webPreferences.nodeIntegrationInSubFrames = false;
    webPreferences.contextIsolation = true;
    webPreferences.sandbox = true;
    webPreferences.webSecurity = true;
    webPreferences.allowRunningInsecureContent = false;
    webPreferences.webviewTag = false;
    params.partition = 'persist:luma';
  });

  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    openExternalSafely(url);
    return { action: 'deny' };
  });

  mainWindow.webContents.on('will-navigate', (event, url) => {
    if (!url.startsWith('file://')) event.preventDefault();
  });

  mainWindow.webContents.on('before-input-event', handleBrowserShortcut);
  mainWindow.webContents.on('render-process-gone', () => {
    if (!mainWindow || mainWindow.isDestroyed()) return;
    mainWindow.reload();
  });

  mainWindow.on('maximize', () => mainWindow?.webContents.send('window:maximized', true));
  mainWindow.on('unmaximize', () => mainWindow?.webContents.send('window:maximized', false));
  mainWindow.on('enter-full-screen', () =>
    mainWindow?.webContents.send('window:fullscreen', true),
  );
  mainWindow.on('leave-full-screen', () =>
    mainWindow?.webContents.send('window:fullscreen', false),
  );
  mainWindow.on('resize', scheduleWindowStateSave);
  mainWindow.on('move', scheduleWindowStateSave);
  mainWindow.on('close', writeWindowState);
  mainWindow.on('closed', () => {
    mainWindow = null;
  });

  mainWindow.loadFile(path.join(RENDERER_DIR, 'index.html'));

  mainWindow.once('ready-to-show', () => {
    if (!mainWindow) return;
    if (state.maximized) mainWindow.maximize();
    mainWindow.show();
    if (IS_DEV) mainWindow.webContents.openDevTools({ mode: 'detach' });
  });
}

app.on('second-instance', () => {
  if (!mainWindow) return;
  if (mainWindow.isMinimized()) mainWindow.restore();
  mainWindow.focus();
});

app.on('web-contents-created', (_event, contents) => {
  if (contents.getType() === 'webview') registerGuest(contents);
});

ipcMain.on('window:minimize', (event) => {
  if (trustedSender(event)) mainWindow?.minimize();
});

ipcMain.on('window:toggle-maximize', (event) => {
  if (!trustedSender(event) || !mainWindow) return;
  if (mainWindow.isMaximized()) mainWindow.unmaximize();
  else mainWindow.maximize();
});

ipcMain.on('window:close', (event) => {
  if (trustedSender(event)) mainWindow?.close();
});

ipcMain.handle('window:state', (event) => {
  if (!trustedSender(event) || !mainWindow) return {};
  return {
    maximized: mainWindow.isMaximized(),
    fullscreen: mainWindow.isFullScreen(),
  };
});

ipcMain.handle('app:info', (event) => {
  if (!trustedSender(event)) return {};
  return {
    version: app.getVersion(),
    electron: process.versions.electron,
    chrome: process.versions.chrome,
    platform: process.platform,
  };
});

ipcMain.handle('app:open-external', async (event, url) => {
  if (!trustedSender(event) || typeof url !== 'string') return false;
  return openExternalSafely(url);
});

ipcMain.handle('app:open-downloads', async (event) => {
  if (!trustedSender(event)) return false;
  const error = await shell.openPath(app.getPath('downloads'));
  return error === '';
});

ipcMain.on('app:quit', (event) => {
  if (trustedSender(event)) app.quit();
});

app.whenReady().then(() => {
  const browserSession = session.fromPartition('persist:luma');
  attachPermissionHandlers(browserSession);
  attachDownloadHandlers(browserSession);
  try {
    browserSession.setSpellCheckerLanguages(['en-US']);
  } catch {
    // Spellchecking is optional and the available dictionaries differ by build.
  }

  createWindow();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});
