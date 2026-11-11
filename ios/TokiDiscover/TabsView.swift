import SwiftUI
/// Tab overview grid: card per tab (title + URL + close), active highlighted, private badged.
struct TabsView: View {
    @EnvironmentObject var model: AppModel
    @Environment(\.colorScheme) private var scheme
    var onPick: () -> Void
    private var dark: Bool { scheme == .dark }
    var body: some View {
        ScrollView { VStack(alignment: .leading, spacing: 16) {
            Text("标签页").font(.title.bold())
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                ForEach(model.tabs) { tab in
                    VStack(alignment: .leading, spacing: 6) {
                        HStack { Text(tab.title).font(.subheadline.bold()).lineLimit(2); Spacer(); Button { model.closeTab(tab.id) } label: { Image(systemName: "xmark") } }
                        Text(tab.url == "about:blank" ? "新标签页" : tab.url).font(.caption).foregroundStyle(.secondary).lineLimit(2)
                        if tab.isPrivate { Text("无痕").font(.caption2).padding(4).background(TokiPalette.primaryContainer(dark: dark), in: tokiChipCorners) }
                        Button(tab.id == model.activeTabID ? "当前标签" : "切换到此标签") { model.activeTabID = tab.id; onPick() }.font(.caption)
                    }.padding(14).background(tab.id == model.activeTabID ? TokiPalette.primaryContainer(dark: dark) : TokiPalette.surfaceContainer(dark: dark), in: tokiCardCorners)
                }
            }
            HStack { Button("新建标签") { model.newTab(); onPick() }; Button("新建无痕标签") { model.newTab(isPrivate: true); onPick() } }
        }.padding(20) }
    }
}
