import SwiftUI
/// New-tab Home: centered capsule search (upper area) + Speed Dial grid (card corners 34/22/34/22) + recent history.
struct HomeView: View {
    @EnvironmentObject var model: AppModel
    @Environment(\.colorScheme) private var scheme
    @State private var query: String = ""
    private var dark: Bool { scheme == .dark }
    var body: some View {
        ScrollView { VStack(alignment: .leading, spacing: 16) {
            Text("TokiDiscover").font(.largeTitle.bold()).padding(.top, 56)
            if model.activeTab.isPrivate { Text("无痕标签页 — 不会记录历史").foregroundStyle(.secondary) }
            TextField("搜索或输入网址", text: $query).textFieldStyle(.plain)
                .padding(16).background(TokiPalette.surfaceContainer(dark: dark), in: Capsule()).onSubmit { model.submitOmnibox(query) }
            Text("常用站点").font(.headline)
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                ForEach(defaultSpeedDial, id: \.url) { site in
                    Button { model.navigate(site.url) } label: { VStack(alignment: .leading) { Text(site.title).font(.subheadline.bold()); Text(site.url).font(.caption2).foregroundStyle(.secondary).lineLimit(1) }
                        .padding(12).frame(maxWidth: .infinity, alignment: .leading).background(TokiPalette.surfaceContainer(dark: dark), in: tokiCardCorners) }.buttonStyle(.plain)
                }
            }
            if !model.history.isEmpty && !model.activeTab.isPrivate { Text("最近访问").font(.headline)
                ForEach(model.history.prefix(3)) { h in Button { model.navigate(h.url) } label: { VStack(alignment: .leading) { Text(h.title).lineLimit(1); Text(h.url).font(.caption).foregroundStyle(.secondary).lineLimit(1) } }.buttonStyle(.plain) } }
        }.padding(20) }
    }
}
