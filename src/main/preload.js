'use strict';

const { contextBridge, ipcRenderer } = require('electron');

function subscribe(channel, callback) {
  const listener = (_event, payload) => callback(payload);
  ipcRenderer.on(channel, listener);
  return () => ipcRenderer.removeListener(channel, listener);
}

contextBridge.exposeInMainWorld('luma', {
  platform: process.platform,
  minimize: () => ipcRenderer.send('window:minimize'),
  toggleMaximize: () => ipcRenderer.send('window:toggle-maximize'),
  close: () => ipcRenderer.send('window:close'),
  getWindowState: () => ipcRenderer.invoke('window:state'),
  getAppInfo: () => ipcRenderer.invoke('app:info'),
  openExternal: (url) => ipcRenderer.invoke('app:open-external', url),
  openDownloads: () => ipcRenderer.invoke('app:open-downloads'),
  quit: () => ipcRenderer.send('app:quit'),
  onMaximized: (callback) => subscribe('window:maximized', callback),
  onFullscreen: (callback) => subscribe('window:fullscreen', callback),
  onCommand: (callback) => subscribe('browser:command', callback),
  onOpenUrl: (callback) => subscribe('browser:open-url', callback),
  onDownload: (callback) => subscribe('browser:download', callback),
});
