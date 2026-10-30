# Luna 设计说明

> 视觉与交互参考：[Material Design 3 Expressive — Built for the Web](https://materialwebunofficial.github.io/#home)  
> 官方实现：`@materialwebunofficial/md3e-web` 1.0.14（非 Google 官方项目，Apache-2.0）

## 1. 设计意图

Luna 把浏览器界面分成三个层次：

1. **环境层**：窗口边缘的柔和渐变与缓慢漂移的色块，暗示“这是一个容器”，而不是一张白纸。
2. **悬浮层**：地址栏 + 导航按钮 + 菜单组成一只玻璃胶囊，和标签栏一起停靠在顶部或底部。
3. **内容层**：网页被装进一张 22px 圆角的卡片，四周留出 10px 呼吸空间；切换工具栏位置时，卡片跟着让位。

这个分层让“浏览器”本身保持低存在感：用户看到的是网页，需要导航时，悬浮胶囊从边缘浮现。

## 2. 从 md3e 继承了什么

| md3e 能力 | Luna 的使用方式 |
| --- | --- |
| Design tokens | `tokens.css` 提供基础色、形状、字阶、状态层、动效曲线 |
| 动态色（HCT） | `applyDynamicTheme(seed, dark, 'expressive')` 从 8 个种子色生成整套角色 |
| Expressive 形状 | 胶囊、28–30px 大圆角、按下时的形变与回弹 |
| Spring motion | `cubic-bezier(0.34, 1.42, 0.64, 1)` 的统一弹簧曲线 |
| Web Components | `md-dialog`、`md-switch`、`md-segmented-button`、`md-progress-indicator`、`md-button` |
| 字体 | Roboto + 内置 Material Symbols Rounded 子集 |

## 3. 关键尺寸

| 元素 | 规格 |
| --- | --- |
| 窗口控制区 | 42px 高，自绘最小化 / 最大化 / 关闭 |
| 标签胶囊 | 36px 高，激活时 232px 基准宽，18px 全圆角 |
| 工具栏胶囊 | 58px 高，30px 圆角，最大宽 1180px |
| 地址栏 | 46px 高，23px 圆角，占满工具栏剩余宽度 |
| 页面卡片 | 左右 10px 边距，22px 圆角 |
| 顶部/底部 chrome 总高 | 125px（运行时由 `ResizeObserver` 实测并写入 `--chrome-h`） |
| 页面与 chrome 间距 | 4px |
| 悬浮建议层 | 26px 圆角，最大 430px 高，最大宽 760px |

## 4. 工具栏位置

设置改变 `html[data-toolbar]`：

- `top`：标签栏在上，工具栏在下；页面从 chrome 底部开始。
- `bottom`：工具栏在上，标签栏在最底部；页面在 chrome 上方结束。
- 两者通过 `top` / `bottom` 的 460ms 弹簧过渡交换空间。
- 切换时 chrome 本身做 380ms 的轻微位移 + 淡入，避免“瞬移”。

页面没有覆盖 chrome，因此不会出现点击穿透；悬浮感来自胶囊的透明度、背景模糊、双层阴影和 1px 高光边，而不是让控件压在网页上。

## 5. 颜色与层级

- 背景：`surface` + 两个 radial gradient（primary / tertiary container），外加大面积 blur 色块。
- 玻璃面：`surface-container` 72–82% 透明度 + `backdrop-filter: blur(30px) saturate(1.75)`。
- 高光：1px 内阴影 + 顶部白色渐变，模拟玻璃边缘。
- 深色模式：提高 surface 容器层级，让胶囊从近黑背景中浮起来；边框与高光同步降低。
- 主题色种子：鸢尾蓝、湖水青、玫瑰灰、琥珀金、苔藓绿、洋红、远山蓝、赤陶。

## 6. 动效

- 标签创建：`translateY(6px) scale(.92)` → 归位，420ms 弹簧。
- 标签切换：宽度 + 背景 + 阴影 220–320ms；激活标签底部有一条 18px 的主色小横线。
- 菜单 / 建议 / 站点信息：`scale(.94)` + 轻微位移，220ms 弹出。
- 侧边面板：从右侧 34px 滑入并缩放，420ms。
- 加载进度：md3e `md-progress-indicator` 的 wavy 线性不确定态，贴在页面卡片顶部。
- 设置弹窗：md3e dialog 自带的 top-layer 缩放淡入；位置切换与主题切换即时生效。
- `prefers-reduced-motion` 下全部压缩到 0.001ms。

## 7. 地址栏状态

| 状态 | 表现 |
| --- | --- |
| 新标签页 | 星形图标 + “新标签页”，站点图标为盾牌 |
| HTTPS | 锁图标，站点图标使用 primary |
| HTTP | 地球图标，站点信息提示“连接不安全” |
| 加载中 | 刷新按钮换成停止方块，顶部 wavy 进度条 |
| 错误 | 警告图标 + 自定义错误卡片；地址栏保留失败域名 |
| 编辑中 | 显示原生 input、全选、主色 focus ring、下拉建议 |
| 书签页 | 星星填充主色 |

输入判定顺序：

1. `luma://settings|history|bookmarks|newtab` 内部命令。
2. `javascript:` / `data:` / `file:` 拦截并提示。
3. 显式 `http(s)://` 直接打开。
4. 域名 / localhost / IP 自动补协议。
5. 其余文本交给当前搜索引擎。

## 8. 设置面板

使用 md3e `md-dialog` 的默认插槽承载内容，`actions` 插槽放“恢复默认 / 完成”。

- **工具栏位置**：两张带迷你浏览器示意的手绘卡片，而不是干巴巴的 radio。
- **外观**：md3e segmented button + 主题色种子色板。
- **搜索引擎**：Bing / Google / DuckDuckGo，和地址栏、新标签页共享。
- **行为开关**：新标签页快捷方式、悬浮玻璃效果。

设置即时写入 `localStorage`（`luma.settings.v1`），主题通过 `applyDynamicTheme` 立刻重建全部角色；打开的新标签页通过 `executeJavaScript` 同步主题，不需要刷新。

## 9. 新标签页

- 本地 `file://` 页面，不发起网络请求。
- 大字号问候 + 实时时钟 + 搜索 + 灵感 chip + 6 个快捷站点。
- 跟随设置里的主题、种子色、搜索引擎、快捷方式开关。
- 与浏览器 chrome 共享同一套 md3e tokens 和动态色引擎。

## 10. 可访问性

- 所有工具栏按钮有 `aria-label` 和原生 `title`。
- 标签使用 `role=tab`、`aria-selected`、方向键切换、Delete 关闭。
- 建议列表使用 `role=listbox` / `option` 和 `aria-selected`，支持 ↑↓、Enter、Esc。
- dialog 使用 md3e 的模态控制器，Esc / 遮罩关闭，焦点回到触发点。
- 键盘焦点使用 2px 主色 outline 或 md3e focus ring。
- 设置面板的开关有可读标签；颜色选择有 `aria-pressed`。

## 11. Android 适配方向

md3e 本身是 Web Components，不能直接跑在 Jetpack Compose 里。Android 端按以下方式“同源适配”：

- 用 Compose Material 3 复刻 tokens：surface 层级、primary / secondary / tertiary、28px 大圆角、pill 形状。
- 悬浮工具栏用 `Box` + `Surface` + `Modifier.shadow` + 半透明容器；顶部/底部通过 `Scaffold` 的 `contentWindowInsets` 或自算 padding 切换。
- 地址栏仍是 floating pill，和导航按钮同一行；标签页改为底部/顶部可滑动的胶囊行或 modal sheet。
- 网页内容用 Android `WebView`（Chromium 内核），通过 `WebViewClient` 拦截新窗口、`WebChromeClient` 处理权限和进度。
- 设置用 `ModalBottomSheet`，持久化 DataStore。
- 动效使用 `spring(dampingRatio = 0.75f, stiffness = 380f)`，与桌面的 0.34/1.42 曲线气质一致。
