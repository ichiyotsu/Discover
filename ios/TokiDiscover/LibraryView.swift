import SwiftUI
/// Library: segmented PillGroup-style switcher 书签 / 历史 / 下载.
/// Downloads on iOS: model + empty state only — WKWebView has no download-manager UI in 0.1.0-beta1 (honest limitation, see ARCHITECTURE.md).
struct LibraryView: View {
    @EnvironmentObject var model: AppModel
    @State private var section: Int = 0
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("书库").font(.title.bold())
            Picker("", selection: $section) { Text("书签").tag(0); Text("历史").tag(1); Text("下载").tag(2) }.pickerStyle(.segmented)
            switch section {
            case 0: if model.bookmarks.isEmpty { Text("暂无书签 — 默认存入「默认」文件夹").foregroundStyle(.secondary) }
                else { List { ForEach(model.bookmarks) { b in VStack(alignment: .leading) { Text(b.title); Text("\(b.folder) · \(b.url)").font(.caption).foregroundStyle(.secondary) } }
                    .onDelete { idx in idx.forEach { model.bookmarks.remove(at: $0) } } } }
            case 1: VStack(alignment: .leading) { if !model.history.isEmpty { Button("清空历史") { model.history = [] } }
                if model.history.isEmpty { Text("暂无历史 — 无痕标签页不会产生记录").foregroundStyle(.secondary) }
                else { List(model.history) { h in VStack(alignment: .leading) { Text(h.title).lineLimit(1); Text(h.url).font(.caption).foregroundStyle(.secondary).lineLimit(1) } } } }
            default: Text("暂无下载 — iOS 端暂为列表模型 + 空状态，WKWebView 下载管理为后续版本计划").foregroundStyle(.secondary)
            }
            Spacer()
        }.padding(20)
    }
}
