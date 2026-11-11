import SwiftUI
/// TokiPalette — same hex as Tokinote (Matcha), full 48-token table mirroring Android Color.kt.
/// Light primary #4C662B / background #F9FAEF; Dark primary #B1D18A / background #12140E.
enum TokiPalette {
    static func primary(dark: Bool) -> Color { dark ? Color(hex: 0xB1D18A) : Color(hex: 0x4C662B) }
    static func onPrimary(dark: Bool) -> Color { dark ? Color(hex: 0x1F3701) : .white }
    static func primaryContainer(dark: Bool) -> Color { dark ? Color(hex: 0x354E16) : Color(hex: 0xCDEDA3) }
    static func onPrimaryContainer(dark: Bool) -> Color { dark ? Color(hex: 0xCDEDA3) : Color(hex: 0x354E16) }
    static func secondary(dark: Bool) -> Color { dark ? Color(hex: 0xBFCBAD) : Color(hex: 0x586249) }
    static func secondaryContainer(dark: Bool) -> Color { dark ? Color(hex: 0x404A33) : Color(hex: 0xDCE7C8) }
    static func tertiary(dark: Bool) -> Color { dark ? Color(hex: 0xA0D0CB) : Color(hex: 0x386663) }
    static func tertiaryContainer(dark: Bool) -> Color { dark ? Color(hex: 0x1F4E4B) : Color(hex: 0xBCECE7) }
    static func error(dark: Bool) -> Color { dark ? Color(hex: 0xFFB4AB) : Color(hex: 0xBA1A1A) }
    static func background(dark: Bool) -> Color { dark ? Color(hex: 0x12140E) : Color(hex: 0xF9FAEF) }
    static func onBackground(dark: Bool) -> Color { dark ? Color(hex: 0xE2E3D8) : Color(hex: 0x1A1C16) }
    static func surface(dark: Bool) -> Color { background(dark: dark) }
    static func onSurface(dark: Bool) -> Color { onBackground(dark: dark) }
    static func surfaceVariant(dark: Bool) -> Color { dark ? Color(hex: 0x44483D) : Color(hex: 0xE1E4D5) }
    static func onSurfaceVariant(dark: Bool) -> Color { dark ? Color(hex: 0xC5C8BA) : Color(hex: 0x44483D) }
    static func outline(dark: Bool) -> Color { dark ? Color(hex: 0x8F9285) : Color(hex: 0x75796C) }
    static func outlineVariant(dark: Bool) -> Color { dark ? Color(hex: 0x44483D) : Color(hex: 0xC5C8BA) }
    static func inverseSurface(dark: Bool) -> Color { dark ? Color(hex: 0xE2E3D8) : Color(hex: 0x2F312A) }
    static func inversePrimary(dark: Bool) -> Color { dark ? Color(hex: 0x4C662B) : Color(hex: 0xB1D18A) }
    static func surfaceContainerLowest(dark: Bool) -> Color { dark ? Color(hex: 0x0C0F09) : .white }
    static func surfaceContainerLow(dark: Bool) -> Color { dark ? Color(hex: 0x1A1C16) : Color(hex: 0xF3F4E9) }
    static func surfaceContainer(dark: Bool) -> Color { dark ? Color(hex: 0x1E201A) : Color(hex: 0xEEEFE3) }
    static func surfaceContainerHigh(dark: Bool) -> Color { dark ? Color(hex: 0x282B24) : Color(hex: 0xE8E9DE) }
    static func surfaceContainerHighest(dark: Bool) -> Color { dark ? Color(hex: 0x33362E) : Color(hex: 0xE2E3D8) }
    static func surfaceDim(dark: Bool) -> Color { dark ? Color(hex: 0x12140E) : Color(hex: 0xDADBD0) }
    static func surfaceBright(dark: Bool) -> Color { dark ? Color(hex: 0x383A32) : Color(hex: 0xF9FAEF) }
}
extension Color { init(hex: UInt32) { self.init(red: Double((hex >> 16) & 0xFF) / 255, green: Double((hex >> 8) & 0xFF) / 255, blue: Double(hex & 0xFF) / 255) } }
/// TokiShapes (points): extraSmall 8 / small 12 / medium 18 / large 26 / extraLarge 36; card 34/22/34/22, chip 20/20/20/7, capsule 36.
let tokiCardCorners = UnevenRoundedRectangle(topLeadingRadius: 34, bottomLeadingRadius: 22, bottomTrailingRadius: 34, topTrailingRadius: 22)
let tokiChipCorners = UnevenRoundedRectangle(topLeadingRadius: 20, bottomLeadingRadius: 7, bottomTrailingRadius: 20, topTrailingRadius: 20)
