import AndexShared

import Charts

import Foundation

import SwiftUI

import UIKit

import PhotosUI



private let nativeSettingsPersianTranslations: [String: String] = [

    "API key": "کلید API",

    "About": "درباره",

    "Account authorized.": "حساب تأیید شد.",

    "Add": "افزودن",

    "Add Provider": "افزودن ارائه‌دهنده",

    "Add Skill": "افزودن مهارت",

    "Add a provider to choose models for Andex.": "برای انتخاب مدل‌های Andex یک ارائه‌دهنده اضافه کنید.",

    "Add provider": "افزودن ارائه‌دهنده",

    "Advanced settings": "تنظیمات پیشرفته",

    "Agent Skills": "مهارت‌های عامل",

    "Allowed tools": "ابزارهای مجاز",

    "Any": "همه",

    "App data": "داده‌های برنامه",

    "Authenticate": "احراز هویت",

    "Authentication": "احراز هویت",

    "Author": "نویسنده",

    "Automatic": "خودکار",

    "Automatically clean old history": "پاک‌سازی خودکار تاریخچه قدیمی",

    "Available Models": "مدل‌های موجود",

    "Average latency": "میانگین تأخیر",

    "Average speed": "میانگین سرعت",

    "Average turn": "میانگین هر نوبت",

    "Back": "بازگشت",

    "Back up or restore settings, providers, skills, and conversations.": "از تنظیمات، ارائه‌دهندگان، مهارت‌ها و گفتگوها پشتیبان بگیرید یا آن‌ها را بازیابی کنید.",

    "Base URL": "نشانی پایه",

    "Browser login": "ورود با مرورگر",

    "Cache tokens": "توکن‌های کش",

    "Cached read": "خواندن از کش",

    "Cached write": "نوشتن در کش",

    "Cancel": "لغو",

    "Check your connection and try again.": "اتصال خود را بررسی کرده و دوباره تلاش کنید.",

    "Choose Folder": "انتخاب پوشه",

    "Choose Zip": "انتخاب فایل Zip",

    "Choose a login method and finish authorization.": "یک روش ورود انتخاب کرده و مجوزدهی را کامل کنید.",

    "Choose a provider": "انتخاب ارائه‌دهنده",

    "Choose backup": "انتخاب نسخه پشتیبان",

    "Choose models": "انتخاب مدل‌ها",

    "Choose the language used throughout Andex.": "زبان مورد استفاده در سراسر Andex را انتخاب کنید.",

    "Command history": "تاریخچه فرمان‌ها",

    "Compatibility": "سازگاری",

    "Compatibility warning": "هشدار سازگاری",

    "Configure credentials provided by the runtime environment.": "اعتبارنامه‌های ارائه‌شده توسط محیط اجرا را پیکربندی کنید.",

    "Connect your provider": "اتصال ارائه‌دهنده",

    "Connect your subscription account to continue.": "برای ادامه، حساب اشتراک خود را متصل کنید.",

    "Connection": "اتصال",

    "Continue": "ادامه",

    "Continue authentication": "ادامه احراز هویت",

    "Copy": "کپی",

    "Credentials are read from the provider environment.": "اعتبارنامه‌ها از محیط ارائه‌دهنده خوانده می‌شوند.",

    "Custom headers": "سرآیندهای سفارشی",

    "Custom instructions": "دستورالعمل‌های سفارشی",

    "Dark": "تیره",

    "Data and diagnostics": "داده‌ها و عیب‌یابی",

    "Day": "روز",

    "Default Models": "مدل‌های پیش‌فرض",

    "Delete": "حذف",

    "Dependencies": "وابستگی‌ها",

    "Developer": "توسعه‌دهنده",

    "Developer message fallbacks": "جایگزین‌های پیام توسعه‌دهنده",

    "Device code login": "ورود با کد دستگاه",

    "Diagnostics": "عیب‌یابی",

    "Disconnect": "قطع اتصال",

    "Discover": "کاوش",

    "Done": "تمام",

    "Downloads": "دانلودها",

    "Edit": "ویرایش",

    "Edit Provider": "ویرایش ارائه‌دهنده",

    "Enabled": "فعال",

    "Enter an API key or use the provider's credential flow.": "یک کلید API وارد کنید یا از فرایند اعتبارنامه ارائه‌دهنده استفاده کنید.",

    "Environment": "محیط",

    "Environment Presets": "پیش‌تنظیم‌های محیط",

    "Environment variables": "متغیرهای محیطی",

    "Existing local data will be replaced by the selected backup.": "داده‌های محلی موجود با نسخه پشتیبان انتخاب‌شده جایگزین می‌شوند.",

    "Export": "خروجی گرفتن",

    "Export app data": "خروجی گرفتن از داده‌های برنامه",

    "Export app data?": "از داده‌های برنامه خروجی گرفته شود؟",

    "Export logs": "خروجی گرفتن از گزارش‌ها",

    "Failed": "ناموفق",

    "Fetch models": "دریافت مدل‌ها",

    "Files": "فایل‌ها",

    "General Settings": "تنظیمات عمومی",

    "Git & Search Tools": "ابزارهای Git و جستجو",

    "History": "تاریخچه",

    "How do you want to authenticate?": "چگونه می‌خواهید احراز هویت کنید؟",

    "Idle timeout": "مهلت بی‌کاری",

    "Import Extension": "وارد کردن افزونه",

    "Import app data": "وارد کردن داده‌های برنامه",

    "Import app data?": "داده‌های برنامه وارد شوند؟",

    "Initialize": "راه‌اندازی",

    "Input": "ورودی",

    "Install": "نصب",

    "Install common development tools into the Alpine environment.": "ابزارهای رایج توسعه را در محیط Alpine نصب کنید.",

    "Install from URL": "نصب از نشانی اینترنتی",

    "Installed": "نصب‌شده",

    "Installing...": "در حال نصب...",

    "Keep tasks running in background": "ادامه اجرای وظایف در پس‌زمینه",

    "Language": "زبان",

    "Largest turn": "بزرگ‌ترین نوبت",

    "License": "مجوز",

    "Light": "روشن",

    "Loading extensions...": "در حال بارگیری افزونه‌ها...",

    "Loading package...": "در حال بارگیری بسته...",

    "Local": "محلی",

    "Manual model IDs, one per line": "شناسه‌های مدل به‌صورت دستی، هر کدام در یک خط",

    "Method": "روش",

    "Model Providers": "ارائه‌دهندگان مدل",

    "Models": "مدل‌ها",

    "Multitasking": "چندوظیفگی",

    "Name": "نام",

    "No Pi extensions installed": "هیچ افزونه Pi نصب نشده است",

    "No extensions found": "افزونه‌ای یافت نشد",

    "No models were returned. Add a model ID above or try again.": "مدلی دریافت نشد. یک شناسه مدل در بالا اضافه کنید یا دوباره تلاش کنید.",

    "No providers": "ارائه‌دهنده‌ای وجود ندارد",

    "No skills installed": "هیچ مهارتی نصب نشده است",

    "Node Environment": "محیط Node",

    "Not installed": "نصب نشده",

    "Notify when tasks finish": "اعلان پس از پایان وظایف",

    "Open": "باز کردن",

    "Open authorization page": "باز کردن صفحه مجوزدهی",

    "Open verification page": "باز کردن صفحه تأیید",

    "Output": "خروجی",

    "Overview": "نمای کلی",

    "Package Information": "اطلاعات بسته",

    "Package details unavailable": "جزئیات بسته در دسترس نیست",

    "Path": "مسیر",

    "Peak day": "روز اوج",

    "Personalization": "شخصی‌سازی",

    "Pi Extensions": "افزونه‌های Pi",

    "Privacy Policy": "سیاست حفظ حریم خصوصی",

    "Prompt extensions may not work in this environment.": "ممکن است افزونه‌های پرامپت در این محیط کار نکنند.",

    "Provider": "ارائه‌دهنده",

    "Provider ID": "شناسه ارائه‌دهنده",

    "Provider ID is required.": "شناسه ارائه‌دهنده الزامی است.",

    "Published": "منتشرشده",

    "Python Environment": "محیط Python",

    "Ready": "آماده",

    "Reasoning": "استدلال",

    "Recent 7 days": "۷ روز اخیر",

    "Recent speed": "سرعت اخیر",

    "Reconnect": "اتصال مجدد",

    "Recorded turns": "نوبت‌های ثبت‌شده",

    "Refresh": "تازه‌سازی",

    "Reinstall": "نصب مجدد",

    "Release": "نسخه انتشار",

    "Reliability": "پایداری",

    "Remote": "راه دور",

    "Remove": "حذف",

    "Remove Skill": "حذف مهارت",

    "Replay Alpine setup preview": "نمایش دوباره پیش‌نمایش راه‌اندازی Alpine",

    "Replay follow-up onboarding": "نمایش دوباره راهنمای پیگیری",

    "Repository": "مخزن",

    "Reset": "بازنشانی",

    "Reset Alpine Data": "بازنشانی داده‌های Alpine",

    "Reset Alpine data?": "داده‌های Alpine بازنشانی شوند؟",

    "Retention": "مدت نگهداری",

    "Retry": "تلاش دوباره",

    "SSH Tools": "ابزارهای SSH",

    "Sample": "نمونه",

    "Save": "ذخیره",

    "Search providers": "جستجوی ارائه‌دهندگان",

    "Sessions": "جلسه‌ها",

    "Settings": "تنظیمات",

    "Setup previews": "پیش‌نمایش‌های راه‌اندازی",

    "Setup required": "نیازمند راه‌اندازی",

    "Sign in with an account or subscription.": "با یک حساب یا اشتراک وارد شوید.",

    "Sign in with browser": "ورود با مرورگر",

    "Sign in with device code": "ورود با کد دستگاه",

    "Size": "اندازه",

    "Source": "منبع",

    "Status": "وضعیت",

    "Step": "مرحله",

    "Submit": "ارسال",

    "Subscription": "اشتراک",

    "System": "سیستم",

    "Terminal": "پایانه",

    "The Alpine environment stays inside Andex's private app storage.": "محیط Alpine در فضای ذخیره‌سازی خصوصی Andex باقی می‌ماند.",

    "The backup may contain conversation content and API credentials. Store it securely.": "نسخه پشتیبان ممکن است شامل محتوای گفتگو و اعتبارنامه‌های API باشد. آن را در مکانی امن نگه دارید.",

    "The export redacts stored credentials and includes runtime diagnostics.": "خروجی، اعتبارنامه‌های ذخیره‌شده را حذف کرده و اطلاعات عیب‌یابی زمان اجرا را در بر می‌گیرد.",

    "Theme": "پوسته",

    "Theme extensions are not supported here.": "افزونه‌های پوسته در اینجا پشتیبانی نمی‌شوند.",

    "These instructions are included when Andex starts an agent turn.": "این دستورالعمل‌ها هنگام آغاز یک نوبت عامل توسط Andex اضافه می‌شوند.",

    "These providers rejected the developer message role, so Andex uses system messages for them. Turn a provider off to try developer messages again.": "این ارائه‌دهندگان نقش پیام توسعه‌دهنده را نپذیرفتند، بنابراین Andex برای آن‌ها از پیام‌های سیستم استفاده می‌کند. برای امتحان دوباره پیام‌های توسعه‌دهنده، ارائه‌دهنده را غیرفعال کنید.",

    "This extension may not be compatible with the current platform.": "ممکن است این افزونه با پلتفرم فعلی سازگار نباشد.",

    "This extension requires interactive terminal UI features.": "این افزونه به قابلیت‌های تعاملی رابط پایانه نیاز دارد.",

    "This provider ID is already in use.": "این شناسه ارائه‌دهنده قبلاً استفاده شده است.",

    "This removes the runtime and all files stored inside it.": "این کار محیط اجرا و همه فایل‌های ذخیره‌شده در آن را حذف می‌کند.",

    "Token and session activity": "فعالیت توکن‌ها و جلسه‌ها",

    "Token history": "تاریخچه توکن‌ها",

    "Token mix": "ترکیب توکن‌ها",

    "Total tokens": "مجموع توکن‌ها",

    "Type": "نوع",

    "Types": "انواع",

    "Unavailable": "در دسترس نیست",

    "Update": "به‌روزرسانی",

    "Updating skills...": "در حال به‌روزرسانی مهارت‌ها...",

    "Usage Statistics": "آمار استفاده",

    "Use a provider-issued API key.": "از کلید API صادرشده توسط ارائه‌دهنده استفاده کنید.",

    "Use as Default Runtime": "استفاده به‌عنوان محیط اجرای پیش‌فرض",

    "Use credentials already available in the runtime.": "از اعتبارنامه‌های موجود در محیط اجرا استفاده کنید.",

    "Use lowercase letters, numbers, and underscores only.": "فقط از حروف کوچک، اعداد و زیرخط استفاده کنید.",

    "Use the system appearance or choose a fixed theme.": "از ظاهر سیستم استفاده کنید یا یک پوسته ثابت برگزینید.",

    "User agent": "عامل کاربر",

    "Value": "مقدار",

    "Version": "نسخه",

    "Version ": "نسخه ",

    "View": "نمایش",

    "Website": "وب‌سایت",

    "Working...": "در حال انجام...",

]



@MainActor

final class NativeSettingsModel: NSObject, ObservableObject, @preconcurrency IosNativeSettingsListener {

    @Published var snapshot: [String: Any] = [:]

    @Published var isPresented = false



    override init() {

        super.init()

        IosNativeSettingsHost.shared.setListener(listener: self)

    }



    deinit {

        IosNativeSettingsHost.shared.setListener(listener: nil)

    }



    func onPresentSettings() {

        DispatchQueue.main.async { [weak self] in self?.isPresented = true }

    }



    func onSnapshotChanged(snapshotJson: String) {

        guard

            let data = snapshotJson.data(using: .utf8),

            let value = try? JSONSerialization.jsonObject(with: data) as? [String: Any]

        else { return }

        DispatchQueue.main.async { [weak self] in self?.snapshot = value }

    }



    var settings: [String: Any] { snapshot["settings"] as? [String: Any] ?? [:] }

    var providers: [[String: Any]] { snapshot["providers"] as? [[String: Any]] ?? [] }

    var providerCatalog: [[String: Any]] { snapshot["providerCatalog"] as? [[String: Any]] ?? [] }

    var modelOptions: [[String: Any]] { snapshot["modelOptions"] as? [[String: Any]] ?? [] }

    var providerAuth: [String: Any] { snapshot["providerAuth"] as? [String: Any] ?? [:] }

    var skills: [[String: Any]] { snapshot["skills"] as? [[String: Any]] ?? [] }

    var extensionSettings: [[String: Any]] { snapshot["extensionSettings"] as? [[String: Any]] ?? [] }

    var capabilities: [String: Any] { snapshot["capabilities"] as? [String: Any] ?? [:] }

    var statistics: [String: Any] { snapshot["statistics"] as? [String: Any] ?? [:] }

    var piExtensions: [String: Any] { snapshot["piExtensions"] as? [String: Any] ?? [:] }

    var alpine: [String: Any] { snapshot["alpine"] as? [String: Any] ?? [:] }



    var language: String { string(settings, "language", fallback: "en") }

    var themeMode: String { string(settings, "themeMode", fallback: "system") }



    func text(_ english: String, _ chinese: String, _ persian: String? = nil) -> String {

        switch language {

        case "zh-CN": chinese

        case "fa": persian ?? nativeSettingsPersianTranslations[english] ?? english

        default: english

        }

    }



    func settingString(_ key: String, fallback: String = "") -> String {

        string(settings, key, fallback: fallback)

    }



    func settingBool(_ key: String, fallback: Bool = false) -> Bool {

        bool(settings, key, fallback: fallback)

    }



    func settingInt(_ key: String, fallback: Int = 0) -> Int {

        int(settings, key, fallback: fallback)

    }



    func patch(_ values: [String: Any]) {

        perform("update_settings", values)

    }



    func perform(_ command: String, _ payload: [String: Any] = [:]) {

        guard

            JSONSerialization.isValidJSONObject(payload),

            let data = try? JSONSerialization.data(withJSONObject: payload),

            let json = String(data: data, encoding: .utf8)

        else { return }

        IosNativeSettingsHost.shared.perform(command: command, payloadJson: json)

    }



    func string(_ object: [String: Any], _ key: String, fallback: String = "") -> String {

        object[key] as? String ?? fallback

    }



    func bool(_ object: [String: Any], _ key: String, fallback: Bool = false) -> Bool {

        object[key] as? Bool ?? (object[key] as? NSNumber)?.boolValue ?? fallback

    }



    func int(_ object: [String: Any], _ key: String, fallback: Int = 0) -> Int {

        object[key] as? Int ?? (object[key] as? NSNumber)?.intValue ?? fallback

    }

}



private let nativeThemeAccents: [(String, String, UInt32, UInt32)] = [
        ("pink-mint", "桃绯 · 薄荷", 0xF35B83, 0x8AEED1),
        ("mint-pink", "薄荷 · 桃绯", 0x8AEED1, 0xF35B83),
        ("peach-moss", "裸桃 · 苔青", 0xFACBC6, 0x465C47),
        ("moss-peach", "苔青 · 裸桃", 0x465C47, 0xFACBC6),
        ("purple-sakura", "雾堇 · 樱粉", 0x615EDE, 0xFF7BBF),
        ("sakura-purple", "樱粉 · 雾堇", 0xFF7BBF, 0x615EDE),
        ("pink-black", "胭脂 · 墨黑", 0xFF7CA2, 0x000000),
        ("pink-white", "胭脂 · 纯白", 0xFF7CA2, 0xFFFFFF)
    ]


private let nativeThemeColors: [(String, String, UInt32)] = [
    ("black", "黑色", 0x000000), ("white", "白色", 0xFFFFFF),
    ("rose", "桃绯", 0xF35B83), ("mint", "薄荷", 0x8AEED1),
    ("peach", "裸桃", 0xFACBC6), ("moss", "苔青", 0x465C47),
    ("purple", "雾堇", 0x615EDE), ("sakura", "樱粉", 0xFF7BBF),
    ("pink", "胭脂", 0xFF7CA2)
]

private func nativeSelectedColors(_ model: NativeSettingsModel, scheme: ColorScheme) -> (UInt32, UInt32) {
    let accent = model.string(model.settings, "themeAccent", fallback: "auto")
    let background = model.string(model.settings, "themeBackground", fallback: "auto")
    let dark = model.themeMode == "dark" || (model.themeMode != "light" && scheme == .dark)
    let legacy = nativeThemeAccents.first { $0.0 == accent }
    let primary = nativeThemeColors.first { $0.0 == accent }?.2 ?? legacy?.2 ?? 0xFF7CA2
    let canvas = nativeThemeColors.first { $0.0 == background }?.2 ?? legacy?.3 ?? (dark ? 0x000000 : 0xFFFFFF)
    return (primary, canvas)
}

private struct NativeAndexColors {
    let accent: Color
    let background: Color
    let surface: Color
    let foreground: Color
    let dark: Bool

    init(model: NativeSettingsModel, systemScheme: ColorScheme) {
        let choice = nativeSelectedColors(model, scheme: systemScheme)
        func rgb(_ hex: UInt32) -> [Double] { [Double((hex >> 16) & 255) / 255, Double((hex >> 8) & 255) / 255, Double(hex & 255) / 255] }
        func color(_ channels: [Double]) -> Color { Color(red: channels[0], green: channels[1], blue: channels[2]) }
        let channels = rgb(choice.1)
        let linear = channels.map { $0 <= 0.04045 ? $0 / 12.92 : pow(($0 + 0.055) / 1.055, 2.4) }
        dark = linear[0] * 0.2126 + linear[1] * 0.7152 + linear[2] * 0.0722 <= 0.179
        background = color(channels)
        let neutral = dark ? 1.0 : 0.0
        let primary = rgb(choice.0)
        let lift = choice.1 == 0 ? 1.0 : choice.1 == 0xFFFFFF ? 0.0 : dark ? 0.0 : 1.0
        let surfaces = [0.0, 0.025, 0.065, 0.10, 0.14].map { amount in
            channels.map { $0 * (1 - amount) + lift * amount }
        }
        let backgroundLuminance = linear[0] * 0.2126 + linear[1] * 0.7152 + linear[2] * 0.0722
        var amount = 0.0
        var ink = primary
        while true {
            ink = primary.map { $0 * (1 - amount) + neutral * amount }
            let channels = ink.map { $0 <= 0.04045 ? $0 / 12.92 : pow(($0 + 0.055) / 1.055, 2.4) }
            let luminance = channels[0] * 0.2126 + channels[1] * 0.7152 + channels[2] * 0.0722
            let ratio = (max(luminance, backgroundLuminance) + 0.05) / (min(luminance, backgroundLuminance) + 0.05)
            let readableOnSurfaces = surfaces.allSatisfy { surface in
                let linear = surface.map { $0 <= 0.04045 ? $0 / 12.92 : pow(($0 + 0.055) / 1.055, 2.4) }
                let value = linear[0] * 0.2126 + linear[1] * 0.7152 + linear[2] * 0.0722
                return (max(value, luminance) + 0.05) / (min(value, luminance) + 0.05) >= 4.5
            }
            if (ratio >= 5.8 && readableOnSurfaces) || amount >= 1 { break }
            amount = min(1, amount + 0.025)
        }
        foreground = color(ink)
        accent = foreground
        let amount = dark ? 0.065 : 0.025
        surface = color(channels.map { $0 * (1 - amount) + lift * amount })
    }
}

private struct NativeAndexForm<Content: View>: View {
    @ObservedObject var model: NativeSettingsModel
    @Environment(\.colorScheme) private var colorScheme
    private let content: Content
    init(model: NativeSettingsModel, @ViewBuilder content: () -> Content) {
        self.model = model
        self.content = content()
    }
    var body: some View {
        let colors = NativeAndexColors(model: model, systemScheme: colorScheme)
        List { content.listRowBackground(colors.background) }
            .listStyle(.plain)
            .scrollContentBackground(.hidden)
            .background(colors.background)
            .foregroundStyle(colors.foreground)
            .tint(colors.accent)
            .environment(\.colorScheme, colors.dark ? .dark : .light)
    }
}

struct NativeSettingsView: View {
    @Environment(\.colorScheme) private var systemScheme

    @ObservedObject var model: NativeSettingsModel

    @Environment(\.dismiss) private var dismiss



    var body: some View {

        NavigationStack {

            List {

                Section {

                    settingsLink("sparkles", model.text("General Settings", "通用设置"), generalSummary) {

                        NativeGeneralSettingsView(model: model)

                    }

                }

                Section {

                    settingsLink("cloud", model.text("Model Providers", "模型提供商"), providerSummary) {

                        NativeProviderListView(model: model)

                    }

                    settingsLink("person", model.text("Role definitions", "角色定义"), personalizationSummary) {

                        NativePersonalizationSettingsView(model: model)

                    }

                    settingsLink("arrow.clockwise", model.text("Reliability", "可靠性"), reliabilitySummary) {

                        NativeReliabilitySettingsView(model: model)

                    }

                }

                if !model.extensionSettings.isEmpty {

                    Section {

                        ForEach(model.extensionSettings, id: \.nativeID) { page in

                            settingsLink(

                                nativeSymbol(model.string(page, "icon"), fallback: "puzzlepiece.extension"),

                                model.string(page, "title"),

                                model.string(page, "subtitle", fallback: model.string(page, "extensionName"))

                            ) {

                                NativeExtensionSettingsPage(model: model, page: page)

                            }

                        }

                    }

                }

                Section {

                    settingsLink("puzzlepiece", model.text("Agent Skills", "Agent Skills"), "\(model.skills.count)") {

                        NativeSkillsSettingsView(model: model)

                    }

                    settingsLink("shippingbox", model.text("Pi Extensions", "Pi Extensions"), extensionSummary) {

                        NativeExtensionsOverview(model: model)

                    }

                    settingsLink("terminal", "Alpine", alpineSummary) {

                        NativeAlpineSettingsView(model: model)

                    }

                }

                Section {

                    settingsLink("chart.bar", model.text("Usage Statistics", "使用统计"), model.text("Token and session activity", "Token 与会话活动")) {

                        NativeStatisticsView(model: model)

                    }

                }

                Section {

                    settingsLink("hammer", model.text("Developer", "开发者"), model.text("Data and diagnostics", "数据与诊断")) {

                        NativeDeveloperSettingsView(model: model)

                    }

                    settingsLink("info.circle", model.text("About", "关于"), model.text("Version ", "版本 ") + model.string(model.snapshot, "appVersion")) {

                        NativeAboutSettingsView(model: model)

                    }

                }

            }

            .scrollContentBackground(.hidden)
            .background(NativeAndexColors(model: model, systemScheme: systemScheme).background)
            .foregroundStyle(NativeAndexColors(model: model, systemScheme: systemScheme).foreground)
            .navigationTitle(model.text("Settings", "设置"))

            .toolbar {

                ToolbarItem(placement: .confirmationAction) {

                    Button(model.text("Done", "完成")) { dismiss() }

                }

            }

        }

        .listStyle(.plain)
        .preferredColorScheme(preferredColorScheme)
        .tint(NativeAndexColors(model: model, systemScheme: systemScheme).accent)

        .environment(\.layoutDirection, model.language == "fa" ? .rightToLeft : .leftToRight)

    }



    private var preferredColorScheme: ColorScheme? {

        switch model.themeMode {

        case "light": .light

        case "dark": .dark

        default: nil

        }

    }



    private var generalSummary: String {

        languageName(model.language)

    }



    private var providerSummary: String {

        let enabled = model.providers.filter { model.bool($0, "isEnabled", fallback: true) }

        if enabled.count == 1 { return model.string(enabled[0], "name") }

        return model.text("\(enabled.count) enabled", "已启用 \(enabled.count) 个", "\(enabled.count) فعال")

    }



    private var personalizationSummary: String {

        let prompt = model.settingString("systemPrompt").trimmingCharacters(in: .whitespacesAndNewlines)

        return prompt.isEmpty ? model.text("Custom instructions", "自定义指令") : String(prompt.prefix(60))

    }



    private var reliabilitySummary: String {

        model.text(

            "Reconnect after \(model.settingInt("llmInactivityReconnectTimeoutSeconds", fallback: 360)) seconds",

            "闲置 \(model.settingInt("llmInactivityReconnectTimeoutSeconds", fallback: 360)) 秒后重连",

            "اتصال مجدد پس از \(model.settingInt("llmInactivityReconnectTimeoutSeconds", fallback: 360)) ثانیه"

        )

    }



    private var extensionSummary: String {

        let count = (model.piExtensions["installed"] as? [[String: Any]] ?? []).count

        return model.text("\(count) installed", "已安装 \(count) 个", "\(count) نصب‌شده")

    }



    private var alpineSummary: String {

        model.settingBool("alpineSetupCompleted")

            ? model.text("Ready", "已就绪")

            : model.text("Setup required", "需要设置")

    }



    @ViewBuilder

    private func settingsLink<Destination: View>(

        _ icon: String,

        _ title: String,

        _ subtitle: String,

        @ViewBuilder destination: () -> Destination

    ) -> some View {

        NavigationLink(destination: destination) {

            Label {

                VStack(alignment: .leading, spacing: 3) {

                    Text(title)

                    if !subtitle.isEmpty {

                        Text(subtitle).font(.caption).foregroundStyle(.secondary).lineLimit(2)

                    }

                }

            } icon: {

                Image(systemName: icon).foregroundStyle(.tint).frame(width: 24)

            }

        }

        .accessibilityElement(children: .combine)

        .accessibilityLabel("\(title), \(subtitle)")
        .listRowBackground(NativeAndexColors(model: model, systemScheme: systemScheme).background)

    }



    private func languageName(_ value: String) -> String {

        switch value {

        case "zh-CN": "简体中文"

        case "fa": "فارسی"

        default: "English"

        }

    }



    private func themeName(_ value: String) -> String {

        switch value {

        case "light": model.text("Light", "浅色")

        case "dark": model.text("Dark", "深色")

        default: model.text("System", "跟随系统")

        }

    }

}



private struct NativeGeneralSettingsView: View {

    @ObservedObject var model: NativeSettingsModel

    @Environment(\.colorScheme) private var colorScheme
    private func color(_ hex: UInt32) -> Color {
        Color(red: Double((hex >> 16) & 255) / 255, green: Double((hex >> 8) & 255) / 255, blue: Double(hex & 255) / 255)
    }
    private func colorRow(_ title: String, background: Bool) -> some View {
        let selected = nativeSelectedColors(model, scheme: colorScheme)
        let current = background ? selected.1 : selected.0
        let colors = NativeAndexColors(model: model, systemScheme: colorScheme)
        return VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text(title).font(.headline)
                Spacer()
                Text(nativeThemeColors.first { $0.2 == current }?.1 ?? "").font(.caption)
            }
            Text(background ? "页面背景 · 表面轻度区分" : "文字、图标、按钮、提示、高亮与描边").font(.caption)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 4) {
                    ForEach(nativeThemeColors, id: \.0) { item in
                        let active = current == item.2
                        Button {
                            let storedAccent = model.string(model.settings, "themeAccent", fallback: "auto")
                            let storedBackground = model.string(model.settings, "themeBackground", fallback: "auto")
                            if background {
                                let accent = nativeThemeColors.first { $0.2 == selected.0 }!.0
                                model.patch(["themeAccent": accent, "themeBackground": item.0])
                            } else {
                                let keep = storedBackground == "auto" && nativeThemeAccents.contains { $0.0 == storedAccent }
                                    ? nativeThemeColors.first { $0.2 == selected.1 }!.0 : storedBackground
                                model.patch(["themeAccent": item.0, "themeBackground": keep])
                            }
                        } label: {
                            Circle().fill(color(item.2)).frame(width: 26, height: 26)
                                .overlay {
                                    if active { Image(systemName: "checkmark").font(.system(size: 13, weight: .bold))
                                        .foregroundStyle(item.2 == 0 || item.2 == 0x465C47 || item.2 == 0x615EDE ? Color.white : Color.black) }
                                }
                                .padding(4).overlay(Circle().stroke(colors.foreground.opacity(active ? 1 : 0.45), lineWidth: active ? 2 : 1))
                                .frame(width: 40, height: 40)
                        }.buttonStyle(.plain).accessibilityLabel("\(title)：\(item.1)")
                            .accessibilityAddTraits(active ? .isSelected : [])
                    }
                }
            }
        }.padding(.vertical, 4)
    }

    var body: some View {
        let colors = NativeAndexColors(model: model, systemScheme: colorScheme)
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                VStack(alignment: .leading, spacing: 10) {
                    Text(model.text("Language", "语言")).font(.headline)
                    Text(model.text("Choose the language used throughout Andex.", "选择 Andex 使用的界面语言。")).font(.caption)
                    Picker(model.text("Language", "语言"), selection: languageBinding) {
                        Text("English").tag("en"); Text("简体中文").tag("zh-CN"); Text("فارسی").tag("fa")
                    }.pickerStyle(.menu).accessibilityLabel("\(languageLabel), Language")
                }
                Divider().overlay(colors.foreground.opacity(0.28))
                colorRow("主题色", background: false)
                colorRow("背景色", background: true)
                HStack { Spacer(); Button("恢复默认 · 粉白 / 粉黑") { model.patch(["themeAccent": "auto", "themeBackground": "auto"]) } }
                Divider().overlay(colors.foreground.opacity(0.28))
                NavigationLink { NativeVolcAsrSettingsView(model: model) } label: {
                    HStack {
                        VStack(alignment: .leading, spacing: 6) {
                            Text("语音输入").font(.headline)
                            Text(voiceConfigured ? "火山 ASR · 已配置" : "配置火山 ASR · 按住输入框说话").font(.caption)
                        }
                        Spacer(); Image(systemName: "chevron.right")
                    }.padding(.vertical, 8)
                }
            }.padding(20)
        }.background(colors.background).foregroundStyle(colors.foreground).tint(colors.accent)
            .navigationTitle(model.text("General Settings", "通用设置"))
            .navigationBarTitleDisplayMode(.inline)
    }

    private var voiceConfigured: Bool {

        let value = model.settings["voiceInput"] as? [String: Any] ?? [:]

        return !model.string(value, "apiKey").isEmpty ||

            (!model.string(value, "appId").isEmpty && !model.string(value, "accessKey").isEmpty)

    }



    private var languageBinding: Binding<String> {

        Binding(get: { model.language }, set: { model.patch(["language": $0]) })

    }



    private var themeBinding: Binding<String> {

        Binding(get: { model.themeMode }, set: { model.patch(["themeMode": $0]) })

    }



    private var languageLabel: String {

        switch model.language { case "zh-CN": "简体中文"; case "fa": "فارسی"; default: "English" }

    }



    private var themeLabel: String {

        switch model.themeMode {

        case "light": "Light"

        case "dark": "Dark"

        default: "System"

        }

    }

}



private struct NativeVolcAsrSettingsView: View {

    @ObservedObject var model: NativeSettingsModel

    @Environment(\.dismiss) private var dismiss

    @State private var apiKey = ""
    @State private var endpoint = "https://openspeech.bytedance.com/api/v3/auc/bigmodel/recognize/flash"
    @State private var checking = false
    @State private var checkResult: String?


    @State private var appId = ""

    @State private var accessKey = ""

    @State private var legacy = false

    var body: some View {

        NativeAndexForm(model: model) {

            Section {

                TextField("服务地址", text: $endpoint).textInputAutocapitalization(.never).autocorrectionDisabled()
                    .keyboardType(.URL)
                Toggle("使用旧版 App ID / Access Token", isOn: $legacy)

                if legacy {

                    TextField("App ID", text: $appId).textInputAutocapitalization(.never).autocorrectionDisabled()

                    SecureField("Access Token", text: $accessKey)

                } else {

                    SecureField("API Key", text: $apiKey)

                }

            } footer: {

                Text("使用大模型录音文件识别极速版。松开后将录音发送至火山引擎，识别文字填入草稿，不会自动发送消息。")

            }

            Section {

                LabeledContent("资源 ID", value: "volc.bigasr.auc_turbo").font(.caption)

                Text("首次使用需允许麦克风权限。单次最长录音 60 秒，上滑取消。")

                    .font(.footnote).foregroundStyle(.secondary)

                Button(checking ? "正在检测…" : "检测连接") { Task { await testConnection() } }.disabled(checking)
                if let checkResult { Text(checkResult).font(.footnote) }
                Text("检测会发送一段短静音以校验地址、鉴权和资源权限。").font(.caption)
                Button("清除配置并停用", role: .destructive) { save(clear: true) }

            }

        }

        .navigationTitle("火山 ASR").navigationBarTitleDisplayMode(.inline)

        .toolbar {

            ToolbarItem(placement: .confirmationAction) {

                Button("保存") { save(clear: false) }

                    .disabled(legacy ? appId.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ||

                              accessKey.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty :

                                apiKey.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)

            }

        }

        .onAppear {

            let config = model.settings["voiceInput"] as? [String: Any] ?? [:]

            apiKey = model.string(config, "apiKey"); appId = model.string(config, "appId"); accessKey = model.string(config, "accessKey")

            endpoint = model.string(config, "endpoint", fallback: "https://openspeech.bytedance.com/api/v3/auc/bigmodel/recognize/flash")
            legacy = apiKey.isEmpty && !appId.isEmpty

        }

    }

    @MainActor private func testConnection() async {
        guard let url = URL(string: endpoint.trimmingCharacters(in: .whitespacesAndNewlines)), url.scheme == "https" else {
            checkResult = "服务地址需使用 HTTPS。"; return
        }
        guard legacy ? !appId.isEmpty && !accessKey.isEmpty : !apiKey.isEmpty else {
            checkResult = "请填写完整的 ASR 凭据。"; return
        }
        checking = true; checkResult = nil
        defer { checking = false }
        do {
            var wav = Data(repeating: 0, count: 8044)
            func text(_ offset: Int, _ value: String) { wav.replaceSubrange(offset..<(offset + value.utf8.count), with: value.utf8) }
            func number(_ offset: Int, _ value: Int, _ count: Int) {
                for i in 0..<count { wav[offset + i] = UInt8((value >> (8 * i)) & 255) }
            }
            text(0, "RIFF"); number(4, 8036, 4); text(8, "WAVEfmt ")
            number(16, 16, 4); number(20, 1, 2); number(22, 1, 2); number(24, 16000, 4)
            number(28, 32000, 4); number(32, 2, 2); number(34, 16, 2); text(36, "data"); number(40, 8000, 4)
            var request = URLRequest(url: url, timeoutInterval: 30)
            request.httpMethod = "POST"
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
            if legacy {
                request.setValue(appId.trimmingCharacters(in: .whitespacesAndNewlines), forHTTPHeaderField: "X-Api-App-Key")
                request.setValue(accessKey.trimmingCharacters(in: .whitespacesAndNewlines), forHTTPHeaderField: "X-Api-Access-Key")
            } else { request.setValue(apiKey.trimmingCharacters(in: .whitespacesAndNewlines), forHTTPHeaderField: "X-Api-Key") }
            request.setValue("volc.bigasr.auc_turbo", forHTTPHeaderField: "X-Api-Resource-Id")
            request.setValue(UUID().uuidString, forHTTPHeaderField: "X-Api-Request-Id")
            request.setValue("-1", forHTTPHeaderField: "X-Api-Sequence")
            request.httpBody = try JSONSerialization.data(withJSONObject: [
                "user": ["uid": "andex-voice"], "audio": ["data": wav.base64EncodedString()],
                "request": ["model_name": "bigmodel", "enable_punc": true]
            ])
            let (_, response) = try await URLSession.shared.data(for: request)
            let http = response as? HTTPURLResponse
            let code = http?.value(forHTTPHeaderField: "X-Api-Status-Code") ?? "未知"
            if let status = http?.statusCode, (200..<300).contains(status), ["20000000", "20000003"].contains(code) {
                checkResult = "连接正常 · 鉴权和语音资源可用"
            } else { checkResult = "检测失败（HTTP \(http?.statusCode ?? 0)，状态 \(code)）。请检查地址、密钥和语音资源权限。" }
        } catch { checkResult = "检测失败，请检查网络连接。" }
    }

    private func save(clear: Bool) {

        model.patch(["voiceInput": [

            "apiKey": clear || legacy ? "" : apiKey.trimmingCharacters(in: .whitespacesAndNewlines),

            "appId": clear || !legacy ? "" : appId.trimmingCharacters(in: .whitespacesAndNewlines),

            "accessKey": clear || !legacy ? "" : accessKey.trimmingCharacters(in: .whitespacesAndNewlines),

            "resourceId": "volc.bigasr.auc_turbo",
            "endpoint": endpoint.trimmingCharacters(in: .whitespacesAndNewlines)

        ]])

        dismiss()

    }

}



private struct NativeRole: Codable, Identifiable {

    var id: String

    var name: String

    var prompt: String

    var avatar: String

}



private struct NativePersonalizationSettingsView: View {

    @ObservedObject var model: NativeSettingsModel

    @State private var prompt = ""

    @State private var roles: [NativeRole] = []

    @State private var defaultId = ""

    @State private var selectedId = ""

    @State private var editing: NativeRole?



    private func save() {

        let data = (try? JSONEncoder().encode(roles)) ?? Data("[]".utf8)

        model.patch(["systemPrompt": prompt, "rolesJson": String(data: data, encoding: .utf8) ?? "[]",

                     "defaultRoleId": defaultId, "selectedRoleId": selectedId])

    }

    var body: some View {

        NativeAndexForm(model: model) {

            Section(model.text("Roles", "角色")) {

                ForEach(roles) { role in

                    VStack(alignment: .leading, spacing: 8) {

                        HStack {

                            NativeRoleAvatar(avatar: role.avatar)

                            Text(role.name)

                            Spacer()

                            Button(model.text("Edit", "编辑")) { editing = role }

                        }

                        Text(role.prompt).lineLimit(2).font(.caption)

                        HStack {

                            Button(defaultId == role.id ? "默认 ✓" : "设为默认") { defaultId = role.id; save() }

                            Button(selectedId == role.id ? "已选择 ✓" : "使用角色") { selectedId = role.id; save() }

                            Button("删除", role: .destructive) {

                                roles.removeAll { $0.id == role.id }

                                if defaultId == role.id { defaultId = roles.first?.id ?? "" }

                                if selectedId == role.id { selectedId = defaultId }

                                save()

                            }

                        }.font(.caption)

                    }

                }

                Button("＋ 添加角色") { editing = NativeRole(id: UUID().uuidString, name: "", prompt: "", avatar: "🤖") }

            }

            Section {

                TextEditor(text: $prompt).frame(minHeight: 160)

            } header: { Text(model.text("Shared instructions", "通用提示词 · 对所有角色生效")) }

              footer: { Text("每次 Agent 回答会载入所选角色的提示词，新对话使用默认角色。") }

        }

        .navigationTitle(model.text("Role definitions", "角色定义"))

        .navigationBarTitleDisplayMode(.inline)

        .onAppear {

            prompt = model.settingString("systemPrompt")

            roles = (try? JSONDecoder().decode([NativeRole].self, from: Data(model.settingString("rolesJson", fallback: "[]").utf8))) ?? []

            defaultId = model.settingString("defaultRoleId")

            selectedId = model.settingString("selectedRoleId")

        }

        .onDisappear { save() }

        .toolbar { ToolbarItem(placement: .confirmationAction) { Button("保存") { save() } } }

        .sheet(item: $editing) { role in

            NativeRoleEditor(model: model, role: role) { updated in

                roles.removeAll { $0.id == updated.id }; roles.append(updated)

                if defaultId.isEmpty { defaultId = updated.id }

                if selectedId.isEmpty { selectedId = updated.id }

                save(); editing = nil

            }

        }

    }

}



private struct NativeRoleAvatar: View {

    var avatar: String

    var body: some View {

        if avatar.hasPrefix("data:image/"), let encoded = avatar.split(separator: ",", maxSplits: 1).last,

           let data = Data(base64Encoded: String(encoded)), let image = UIImage(data: data) {

            Image(uiImage: image).resizable().scaledToFill().frame(width: 32, height: 32).clipShape(Circle())

        } else { Text(avatar.isEmpty ? "🤖" : avatar).font(.title2) }

    }

}



private struct NativeRoleEditor: View {

    @ObservedObject var model: NativeSettingsModel

    @State var role: NativeRole

    var onSave: (NativeRole) -> Void

    @Environment(\.dismiss) private var dismiss

    @State private var photo: PhotosPickerItem?

    var body: some View {

        NavigationStack {

            NativeAndexForm(model: model) {

                TextField("角色名称", text: $role.name)

                NativeRoleAvatar(avatar: role.avatar)

                PhotosPicker("选择头像", selection: $photo, matching: .images)

                TextEditor(text: $role.prompt).frame(minHeight: 220)

            }

            .navigationTitle("角色定义")

            .toolbar {

                ToolbarItem(placement: .cancellationAction) { Button("取消") { dismiss() } }

                ToolbarItem(placement: .confirmationAction) { Button("保存") { onSave(role) }.disabled(role.name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty) }

            }

            .onChange(of: photo) { value in

                Task {

                    if let data = try? await value?.loadTransferable(type: Data.self), let image = UIImage(data: data) {

                        let scale = min(1, 256 / max(image.size.width, image.size.height))

                        let size = CGSize(width: image.size.width * scale, height: image.size.height * scale)

                        let resized = UIGraphicsImageRenderer(size: size).image { _ in image.draw(in: CGRect(origin: .zero, size: size)) }

                        if let png = resized.pngData() { role.avatar = "data:image/png;base64," + png.base64EncodedString() }

                    }

                }

            }

        }

    }

}



private struct NativeReliabilitySettingsView: View {

    @ObservedObject var model: NativeSettingsModel



    var body: some View {

        NativeAndexForm(model: model) {

            if model.bool(model.capabilities, "persistentBackground") {

                Section(model.text("Multitasking", "多任务")) {

                    Toggle(model.text("Keep tasks running in background", "在后台继续运行任务"), isOn: boolBinding("keepTasksRunningInBackground"))

                    if model.bool(model.capabilities, "localNotifications") {

                        Toggle(model.text("Notify when tasks finish", "任务完成时通知"), isOn: boolBinding("notifyOnTaskCompletion"))

                    }

                }

            }

            Section(model.text("Reconnect", "重连")) {

                Stepper(value: intBinding("llmInactivityReconnectTimeoutSeconds", 30...3600), in: 30...3600, step: 30) {

                    LabeledContent(

                        model.text("Idle timeout", "闲置超时"),

                        value: model.text(

                            "\(model.settingInt("llmInactivityReconnectTimeoutSeconds", fallback: 360)) sec",

                            "\(model.settingInt("llmInactivityReconnectTimeoutSeconds", fallback: 360)) 秒",

                            "\(model.settingInt("llmInactivityReconnectTimeoutSeconds", fallback: 360)) ثانیه"

                        )

                    )

                }

            }

        }

        .navigationTitle(model.text("Reliability", "可靠性"))

        .navigationBarTitleDisplayMode(.inline)

    }



    private func boolBinding(_ key: String) -> Binding<Bool> {

        Binding(get: { model.settingBool(key) }, set: { model.patch([key: $0]) })

    }



    private func intBinding(_ key: String, _ range: ClosedRange<Int>) -> Binding<Int> {

        Binding(

            get: { model.settingInt(key, fallback: range.lowerBound) },

            set: { model.patch([key: min(max($0, range.lowerBound), range.upperBound)]) }

        )

    }

}



private struct NativeProviderListView: View {

    @ObservedObject var model: NativeSettingsModel

    @State private var addingProvider = false



    var body: some View {

        List {

            Section {

                NavigationLink {

                    NativeDefaultModelsView(model: model)

                } label: {

                    Label(model.text("Default Models", "默认模型"), systemImage: "slider.horizontal.3")

                }

            }

            if model.providers.isEmpty {

                ContentUnavailableView(

                    model.text("No providers", "尚无提供商"),

                    systemImage: "cloud",

                    description: Text(model.text("Add a provider to choose models for Andex.", "添加提供商以便为 Andex 选择模型。"))

                )

            }

            ForEach(model.providers, id: \.nativeID) { provider in

                HStack(spacing: 12) {

                    NavigationLink {

                        NativeProviderEditor(model: model, provider: provider)

                    } label: {

                        VStack(alignment: .leading, spacing: 4) {

                            Text(model.string(provider, "name", fallback: model.string(provider, "providerId")))

                            Text(modelSummary(provider)).font(.caption).foregroundStyle(.secondary)

                        }

                    }

                    Toggle("", isOn: Binding(

                        get: { model.bool(provider, "isEnabled", fallback: true) },

                        set: { model.perform("provider_enabled", ["id": model.string(provider, "id"), "enabled": $0]) }

                    )).labelsHidden()

                }

                .swipeActions { deleteButton(provider) }

            }

        }

        .navigationTitle(model.text("Model Providers", "模型提供商"))

        .navigationBarTitleDisplayMode(.inline)

        .toolbar {

            ToolbarItem(placement: .primaryAction) {

                Button { addingProvider = true } label: { Image(systemName: "plus") }

                    .accessibilityLabel(model.text("Add provider", "添加提供商"))

            }

        }

        .sheet(isPresented: $addingProvider) {

            NavigationStack { NativeProviderEditor(model: model, provider: nil) }

        }

    }



    private func modelSummary(_ provider: [String: Any]) -> String {

        let available = Set(((provider["cachedModels"] as? [String]) ?? []) + ((provider["manualModelIds"] as? [String]) ?? []))

        let enabled = (provider["enabledModelIds"] as? [String] ?? []).filter(available.contains).count

        return model.text(

            "\(enabled) of \(available.count) models enabled",

            "已启用 \(enabled)/\(available.count) 个模型",

            "\(enabled) مدل از \(available.count) مدل فعال است"

        )

    }



    private func deleteButton(_ provider: [String: Any]) -> some View {

        Button(role: .destructive) {

            model.perform("provider_remove", ["id": model.string(provider, "id")])

        } label: { Label(model.text("Delete", "删除"), systemImage: "trash") }

    }

}



private struct NativeDefaultModelsView: View {

    @ObservedObject var model: NativeSettingsModel

    private let rows = [

        ("defaultChatModelKey", "Chat", "对话", "chat"),

        ("defaultTitleModelKey", "Title generation", "标题生成", "title"),

        ("defaultNamingModelKey", "Naming", "命名", "naming"),

        ("defaultCompactingModelKey", "Compacting", "上下文压缩", "compacting"),

    ]



    var body: some View {

        List(rows, id: \.0) { row in

            NavigationLink {

                NativeModelSelector(model: model, settingKey: row.0, title: model.text(row.1, row.2), purpose: row.3)

            } label: {

                LabeledContent(model.text(row.1, row.2), value: selectedLabel(row.0, row.3))

            }

        }

        .navigationTitle(model.text("Default Models", "默认模型"))

        .navigationBarTitleDisplayMode(.inline)

    }



    private func selectedLabel(_ key: String, _ purpose: String) -> String {

        let selected = model.settingString(key)

        if selected.isEmpty {

            let automatic = model.snapshot["automaticModels"] as? [String: Any] ?? [:]

            let resolved = automatic[purpose] as? [String: Any] ?? [:]

            let label = model.string(resolved, "label")

            return label.isEmpty

                ? model.text("Automatic", "自动")

                : model.text("Automatic: \(label)", "自动：\(label)", "خودکار: \(label)")

        }

        return model.modelOptions.first { model.string($0, "key") == selected }.map { model.string($0, "fullLabel") }

            ?? model.text("Unavailable", "不可用")

    }

}



private struct NativeModelSelector: View {

    @ObservedObject var model: NativeSettingsModel

    let settingKey: String

    let title: String

    let purpose: String

    @State private var search = ""



    var body: some View {

        List {

            Button { model.patch([settingKey: ""]) } label: {

                modelRow(model.text("Automatic", "自动"), key: "")

            }

            ForEach(filtered, id: \.nativeID) { option in

                Button { model.patch([settingKey: model.string(option, "key")]) } label: {

                    modelRow(model.string(option, "fullLabel"), key: model.string(option, "key"))

                }

            }

        }

        .searchable(text: $search)

        .navigationTitle(title)

        .navigationBarTitleDisplayMode(.inline)

    }



    private var filtered: [[String: Any]] {

        let query = search.trimmingCharacters(in: .whitespacesAndNewlines)

        guard !query.isEmpty else { return model.modelOptions }

        return model.modelOptions.filter { model.string($0, "fullLabel").localizedCaseInsensitiveContains(query) }

    }



    private func modelRow(_ label: String, key: String) -> some View {

        HStack {

            Text(label).foregroundStyle(.primary)

            Spacer()

            if model.settingString(settingKey) == key { Image(systemName: "checkmark").foregroundStyle(.tint) }

        }

    }

}



struct NativeProviderPair: Identifiable {

    var id = UUID()

    var name: String

    var value: String

}



struct NativeProviderDraft {

    var id: String

    var providerId: String

    var name: String

    var piProviderId: String

    var authMethod: String

    var apiKey: String

    var oauthCredentialJson: String

    var environment: [NativeProviderPair]

    var baseURL: String

    var modelIDs: String

    var serviceTier: String

    var userAgent: String

    var headers: [NativeProviderPair]

    var compatibilityMode: Bool

    var cachedModels: [String]

    var enabledModels: Set<String>

    var isEnabled: Bool

    var createdAt: Int64



    var manualModels: [String] {

        Array(Set(modelIDs.split(whereSeparator: \.isNewline).map {

            $0.trimmingCharacters(in: .whitespacesAndNewlines)

        }.filter { !$0.isEmpty })).sorted()

    }



    var allModels: [String] { Array(Set(cachedModels + manualModels)).sorted() }



    mutating func applyFetchedModels(_ models: [String], error: String = "") {

        guard error.isEmpty || !models.isEmpty else { return }

        let previous = Set(allModels)

        cachedModels = Array(Set(models.map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }

            .filter { !$0.isEmpty })).sorted()

        enabledModels.formUnion(Set(cachedModels).subtracting(previous))

        enabledModels.formIntersection(allModels)

    }



    static func availableProviderID(_ base: String, existing: Set<String>) -> String {

        var candidate = base

        var suffix = 2

        while existing.contains(candidate) {

            candidate = "\(base)_\(suffix)"

            suffix += 1

        }

        return candidate

    }

}



private struct NativeProviderEditor: View {

    @ObservedObject var model: NativeSettingsModel

    @Environment(\.dismiss) private var dismiss

    private let original: [String: Any]?

    @State private var draft: NativeProviderDraft

    @State private var showingPrompt = false

    @State private var openedAuthorizationURL = ""

    @State private var wizardStage = 0

    @State private var providerSearch = ""

    @State private var showAdvanced = false

    @State private var waitingForWizardModels = false

    @State private var wizardAuthMethod = "api_key"

    @State private var fetchRequestID = ""

    @State private var fetchError = ""

    @State private var authSessionID = ""

    @State private var appliedCredential = ""

    @State private var presentedPromptID = ""



    init(model: NativeSettingsModel, provider: [String: Any]?) {

        self.model = model

        original = provider

        let now = Int64(Date().timeIntervalSince1970 * 1_000)

        let defaultDefinition = model.providerCatalog.first { model.string($0, "id") == "openai" } ?? [:]

        let modelIDs = provider?["manualModelIds"] as? [String] ?? []

        _draft = State(initialValue: NativeProviderDraft(

            id: provider?["id"] as? String ?? UUID().uuidString.lowercased(),

            providerId: provider?["providerId"] as? String ?? "openai",

            name: provider?["name"] as? String ?? model.string(defaultDefinition, "displayName", fallback: "OpenAI"),

            piProviderId: provider?["piProviderId"] as? String ?? "openai",

            authMethod: provider?["authMethod"] as? String ?? "api_key",

            apiKey: provider?["apiKey"] as? String ?? "",

            oauthCredentialJson: provider?["oauthCredentialJson"] as? String ?? "",

            environment: Self.pairs(provider?["providerEnvironmentVariables"]),

            baseURL: provider?["baseUrl"] as? String ?? model.string(defaultDefinition, "defaultBaseUrl", fallback: "https://api.openai.com/v1"),

            modelIDs: modelIDs.joined(separator: "\n"),

            serviceTier: provider?["serviceTier"] as? String ?? "",

            userAgent: provider?["userAgent"] as? String ?? "Andex/1.0",

            headers: Self.pairs(provider?["customHeaders"]),

            compatibilityMode: provider?["compatibilityMode"] as? Bool ?? false,

            cachedModels: provider?["cachedModels"] as? [String] ?? [],

            enabledModels: Set(provider?["enabledModelIds"] as? [String] ?? modelIDs),

            isEnabled: provider?["isEnabled"] as? Bool ?? true,

            createdAt: (provider?["createdAtMillis"] as? NSNumber)?.int64Value ?? now

        ))

    }



    var body: some View {

        Group {

            if original == nil { wizard } else { editorForm }

        }

        .navigationTitle(original == nil ? model.text("Add Provider", "添加提供商") : model.text("Edit Provider", "编辑提供商"))

        .navigationBarTitleDisplayMode(.inline)

        .toolbar {

            if original == nil {

                ToolbarItem(placement: .cancellationAction) { Button(model.text("Cancel", "取消")) { dismiss() } }

            } else {

                ToolbarItem(placement: .confirmationAction) { Button(model.text("Save", "保存"), action: save).disabled(!isValid) }

            }

        }

        .onReceive(model.$snapshot.receive(on: RunLoop.main)) { snapshot in applyResults(snapshot) }

        .onChange(of: draft.authMethod) { _, _ in clearAuth() }

        .onChange(of: draft.modelIDs) { old, _ in

            let previous = Set(old.split(whereSeparator: \.isNewline).map { $0.trimmingCharacters(in: .whitespaces) })

            draft.enabledModels.formUnion(Set(manualModels).subtracting(previous))

            draft.enabledModels.formIntersection(allModels)

        }

        .onDisappear { clearAuth() }

        .sheet(isPresented: $showingPrompt) { NativeProviderAuthPromptView(model: model) }

    }



    private var editorForm: some View {

        NativeAndexForm(model: model) {

            Section(model.text("Provider", "提供商")) {

                Picker(model.text("Type", "类型"), selection: $draft.piProviderId) {

                    ForEach(model.providerCatalog, id: \.nativeID) { item in

                        Text(model.string(item, "displayName")).tag(model.string(item, "id"))

                    }

                }

                .onChange(of: draft.piProviderId) { _, value in applyDefaults(value) }

                TextField(model.text("Name", "名称"), text: $draft.name)

                TextField(model.text("Provider ID", "提供商 ID"), text: $draft.providerId)

                    .textInputAutocapitalization(.never).autocorrectionDisabled()

                if !providerIDError.isEmpty { Text(providerIDError).font(.caption).foregroundStyle(.red) }

            }

            Section(model.text("Authentication", "认证")) {

                Picker(model.text("Method", "方式"), selection: $draft.authMethod) {

                    ForEach(authMethods, id: \.self) { method in Text(authLabel(method)).tag(method) }

                }

                if draft.authMethod == "api_key" {

                    apiKeyContent

                } else if draft.authMethod == "oauth" {

                    Text("\(model.string(definition, "displayName")) OAuth")

                        .font(.caption).foregroundStyle(.secondary)

                    Text(oauthConnectionStatus)

                        .foregroundStyle(model.string(relevantAuth, "errorMessage").isEmpty ? Color.primary : Color.red)

                    oauthDeviceCodeRow

                    oauthActionRows

                    if !draft.oauthCredentialJson.isEmpty {

                        wizardActionRow("trash", model.text("Disconnect", "断开连接"), destructive: true) {

                            draft.oauthCredentialJson = ""

                            clearAuth()

                        }

                    }

                } else {

                    Text(model.text("Credentials are read from the provider environment.", "凭证从提供商环境变量读取。"))

                        .font(.caption).foregroundStyle(.secondary)

                }

                authStatus

            }

            Section(model.text("Connection", "连接")) {

                TextField(model.text("Base URL", "基础 URL"), text: $draft.baseURL)

                    .textInputAutocapitalization(.never).keyboardType(.URL)

                TextField(model.text("Service tier (e.g. fast / ultrafast)", "速度档位（如 fast / ultrafast）"), text: $draft.serviceTier)

                    .textInputAutocapitalization(.never).autocorrectionDisabled()

                Text(model.text("Leave blank to use the provider default.", "留空使用提供商默认档位。"))

                    .font(.caption).foregroundStyle(.secondary)

                TextField(model.text("User agent", "User Agent"), text: $draft.userAgent)

                pairEditor(model.text("Custom headers", "自定义请求头"), pairs: $draft.headers)

                pairEditor(model.text("Environment variables", "环境变量"), pairs: $draft.environment)

            }

            if draft.piProviderId == "openai-compatible" {

                compatibilityModeToggle

            }

            Section(model.text("Models", "模型")) {

                VStack(alignment: .leading, spacing: 6) {

                    Text(model.text("Manual model IDs, one per line", "手动模型 ID，每行一个")).font(.caption).foregroundStyle(.secondary)

                    TextEditor(text: $draft.modelIDs).frame(minHeight: 80)

                        .textInputAutocapitalization(.never).autocorrectionDisabled()

                }

                Button { fetchModels() } label: {

                    if isFetching { ProgressView() } else { Label(model.text("Fetch models", "获取模型"), systemImage: "arrow.clockwise") }

                }.disabled(isFetching || !connectionIsValid)

                if !fetchError.isEmpty {

                    Text(fetchError).foregroundStyle(.red).font(.caption)

                }

                ForEach(allModels, id: \.self) { modelID in

                    Toggle(modelID, isOn: Binding(

                        get: { draft.enabledModels.contains(modelID) },

                        set: { enabled in if enabled { draft.enabledModels.insert(modelID) } else { draft.enabledModels.remove(modelID) } }

                    ))

                }

            }

        }

    }



    private var wizard: some View {

        Group {

            switch wizardStage {

            case 0: authenticationChoices

            case 1: providerChoices

            case 2: credentialsStep

            default: modelsStep

            }

        }

        .background(Color(uiColor: .systemGroupedBackground).ignoresSafeArea())

    }



    private var authenticationChoices: some View {

        List {

            Section {

                wizardChoice("checkmark.shield", model.text("Subscription", "订阅账号"), model.text("Sign in with an account or subscription.", "使用账号或订阅登录。")) { selectAuth("oauth") }

                wizardChoice("key", model.text("API key", "API 密钥"), model.text("Use a provider-issued API key.", "使用提供商签发的 API 密钥。")) { selectAuth("api_key") }

                wizardChoice("cloud", model.text("Environment", "环境"), model.text("Use credentials already available in the runtime.", "使用运行环境中已有的凭证。")) { selectAuth("ambient") }

            } header: { wizardHeader(0) }

        }

        .listStyle(.plain)

    }



    private var providerChoices: some View {

        List {

            Section {

                HStack(spacing: 10) {

                    Image(systemName: "magnifyingglass").foregroundStyle(.secondary)

                    TextField(model.text("Search providers", "搜索提供商"), text: $providerSearch)

                        .textInputAutocapitalization(.never)

                        .autocorrectionDisabled()

                }

                ForEach(filteredProviders, id: \.nativeID) { item in

                    wizardChoice(

                        "cloud",

                        model.string(item, "displayName"),

                        model.string(item, "category")

                    ) {

                        draft.piProviderId = model.string(item, "id")

                        applyDefaults(draft.piProviderId)

                        draft.authMethod = wizardAuthMethod

                        navigateWizard(to: 2)

                    }

                }

                if filteredProviders.isEmpty {

                    Text(model.text("No providers found", "未找到提供商", "ارائه‌دهنده‌ای یافت نشد"))

                        .foregroundStyle(.secondary)

                }

            } header: { wizardHeader(1) }

            Section {

                Button { navigateWizard(to: 0) } label: { Label(model.text("Back", "返回"), systemImage: "chevron.left") }

            }

        }

        .listStyle(.plain)

    }



    private var credentialsStep: some View {

        List {

            Section {

                Text(model.string(definition, "displayName")).font(.headline)

                if draft.authMethod == "api_key" {

                    apiKeyContent

                    authStatus

                } else if draft.authMethod == "oauth" {

                    oauthContent

                } else {

                    Text(model.text("Credentials are read from the provider environment.", "凭证从提供商环境变量读取。"))

                        .foregroundStyle(.secondary)

                    pairEditor(model.text("Environment variables", "环境变量"), pairs: $draft.environment)

                }

            } header: { wizardHeader(2) }

            if definitionBool("requiresBaseUrl") || definitionBool("supportsCustomBaseUrl") || !definitionBool("isBuiltIn") {

                Section(model.text("Connection", "连接")) {

                    TextField(model.text("Base URL", "基础 URL"), text: $draft.baseURL)

                        .textInputAutocapitalization(.never).keyboardType(.URL)

                }

            }

            if draft.piProviderId == "openai-compatible" {

                compatibilityModeToggle

            }

            Section {

                HStack {

                    Button { clearAuth(); waitingForWizardModels = false; fetchRequestID = ""; navigateWizard(to: 1) } label: { Text(model.text("Back", "返回")) }

                        .buttonStyle(.borderless)

                    Spacer()

                    Button {

                        waitingForWizardModels = true

                        fetchModels()

                    } label: {

                        if isFetching { ProgressView() } else { Text(model.text("Continue", "继续")) }

                    }

                    .buttonStyle(.borderedProminent)

                    .disabled(!connectionIsValid || isFetching || waitingForWizardModels || model.bool(relevantAuth, "isRunning"))

                }

            }

        }

        .listStyle(.plain)

    }



    private var modelsStep: some View {

        List {

            Section {

                VStack(alignment: .leading, spacing: 8) {

                    Text(model.text("Manual model IDs, one per line", "手动模型 ID，每行一个")).font(.caption).foregroundStyle(.secondary)

                    TextEditor(text: $draft.modelIDs).frame(minHeight: 80)

                        .textInputAutocapitalization(.never).autocorrectionDisabled()

                }

            } header: { wizardHeader(3) } footer: {

                Text(model.text(

                    "Select the models Andex can use with \(model.string(definition, "displayName")).",

                    "选择 Andex 可通过 \(model.string(definition, "displayName")) 使用的模型。",

                    "مدل‌هایی را انتخاب کنید که Andex می‌تواند با \(model.string(definition, "displayName")) استفاده کند."

                ))

            }

            Section(model.text("Available Models", "可用模型")) {

                Button { fetchModels() } label: {

                    if isFetching { ProgressView() } else { Label(model.text("Fetch models", "获取模型"), systemImage: "arrow.clockwise") }

                }.disabled(isFetching || !connectionIsValid)

                if !fetchError.isEmpty {

                    Text(fetchError).foregroundStyle(.red).font(.caption)

                }

                if allModels.isEmpty {

                    Text(model.text("No models were returned. Add a model ID above or try again.", "未获取到模型。请在上方添加模型 ID，或重试。"))

                        .font(.callout).foregroundStyle(.secondary)

                }

                ForEach(allModels, id: \.self) { modelID in

                    Toggle(modelID, isOn: Binding(

                        get: { draft.enabledModels.contains(modelID) },

                        set: { enabled in if enabled { draft.enabledModels.insert(modelID) } else { draft.enabledModels.remove(modelID) } }

                    ))

                }

            }

            Section {

                DisclosureGroup(model.text("Advanced settings", "高级设置"), isExpanded: $showAdvanced) {

                    TextField(model.text("Name", "名称"), text: $draft.name)

                    TextField(model.text("Provider ID", "提供商 ID"), text: $draft.providerId)

                        .textInputAutocapitalization(.never).autocorrectionDisabled()

                    if !providerIDError.isEmpty { Text(providerIDError).font(.caption).foregroundStyle(.red) }

                    TextField(model.text("Base URL", "基础 URL"), text: $draft.baseURL)

                        .textInputAutocapitalization(.never).autocorrectionDisabled().keyboardType(.URL)

                    TextField(model.text("Service tier (e.g. fast / ultrafast)", "速度档位（如 fast / ultrafast）"), text: $draft.serviceTier)

                        .textInputAutocapitalization(.never).autocorrectionDisabled()

                    Text(model.text("Leave blank to use the provider default.", "留空使用提供商默认档位。"))

                        .font(.caption).foregroundStyle(.secondary)

                    TextField(model.text("User agent", "User Agent"), text: $draft.userAgent)

                    pairEditor(model.text("Environment variables", "环境变量"), pairs: $draft.environment)

                    pairEditor(model.text("Custom headers", "自定义请求头"), pairs: $draft.headers)

                }

            }

            Section {

                HStack {

                    Button { navigateWizard(to: 2) } label: { Text(model.text("Back", "返回")) }

                        .buttonStyle(.borderless)

                    Spacer()

                    Button(model.text("Save", "保存"), action: save).buttonStyle(.borderedProminent).disabled(!isValid)

                }

            }

        }

        .listStyle(.plain)

    }



    private var filteredProviders: [[String: Any]] {

        let query = providerSearch.trimmingCharacters(in: .whitespacesAndNewlines)

        return model.providerCatalog.filter { item in

            let supported = wizardAuthMethod == "oauth" ? model.bool(item, "supportsOAuth")

                : wizardAuthMethod == "ambient" ? model.bool(item, "supportsAmbientAuth")

                : model.bool(item, "supportsInteractiveApiKey")

            return supported && (query.isEmpty || model.string(item, "displayName").localizedCaseInsensitiveContains(query)

                || model.string(item, "id").localizedCaseInsensitiveContains(query)

                || model.string(item, "category").localizedCaseInsensitiveContains(query))

        }

    }

    private var compatibilityModeToggle: some View {

        Section {

            Toggle(isOn: $draft.compatibilityMode) {

                VStack(alignment: .leading, spacing: 4) {

                    Text(model.text("Compatibility mode", "兼容模式", "حالت سازگاری"))

                    Text(model.text(

                        "Sends system and developer instructions as user messages for endpoints with limited role support.",

                        "将 System 和 Developer 指令作为 User 消息发送，以兼容角色支持有限的端点。",

                        "دستورهای system و developer را برای نقاط پایانی با پشتیبانی محدود از نقش‌ها به‌صورت پیام user ارسال می‌کند."

                    ))

                    .font(.caption)

                    .foregroundStyle(.secondary)

                }

            }

        }

    }

    private func selectAuth(_ auth: String) {

        wizardAuthMethod = auth

        draft.authMethod = auth

        providerSearch = ""

        navigateWizard(to: 1)

    }

    private func navigateWizard(to stage: Int) {

        guard stage != wizardStage else { return }

        withAnimation(.snappy(duration: 0.38, extraBounce: 0)) {

            wizardStage = stage

        }

    }



    private func wizardHeader(_ stage: Int) -> some View {

        VStack(alignment: .leading, spacing: 5) {

            Text(model.text("Step \(stage + 1) of 4", "第 \(stage + 1) 步，共 4 步", "مرحله \(stage + 1) از ۴"))

                .font(.subheadline)

                .foregroundStyle(.secondary)

                .textCase(nil)

            Text(wizardTitle(for: stage))

                .font(.title2.bold())

                .foregroundStyle(.primary)

                .textCase(nil)

        }

        .padding(.bottom, 8)

    }



    private func wizardTitle(for stage: Int) -> String {

        switch stage {

        case 0: model.text("How do you want to authenticate?", "选择认证方式")

        case 1: model.text("Choose a provider", "选择提供商")

        case 2: model.text("Connect your provider", "连接提供商")

        default: model.text("Choose models", "选择模型")

        }

    }

    private func wizardChoice(_ icon: String, _ title: String, _ subtitle: String, action: @escaping () -> Void) -> some View {

        Button(action: action) {

            HStack(spacing: 14) {

                Image(systemName: icon).font(.title3).frame(width: 28).foregroundStyle(.tint)

                VStack(alignment: .leading, spacing: 4) {

                    Text(title).font(.headline).foregroundStyle(.primary)

                    Text(subtitle).font(.subheadline).foregroundStyle(.secondary).multilineTextAlignment(.leading)

                }

                Spacer()

                Image(systemName: "chevron.right").foregroundStyle(.tertiary)

            }

            .padding(.vertical, 4)

        }.buttonStyle(.plain)

    }



    private var definition: [String: Any] { model.providerCatalog.first { model.string($0, "id") == draft.piProviderId } ?? [:] }

    private var authMethods: [String] {

        var methods: [String] = []

        if definitionBool("supportsInteractiveApiKey") { methods.append("api_key") }

        if definitionBool("supportsOAuth") { methods.append("oauth") }

        if definitionBool("supportsAmbientAuth") { methods.append("ambient") }

        return methods.isEmpty ? ["api_key"] : methods

    }

    private var manualModels: [String] { draft.manualModels }

    private var allModels: [String] { draft.allModels }

    private var isFetching: Bool { !fetchRequestID.isEmpty }

    private var connectionIsValid: Bool {

        authenticationIsValid && (!(definitionBool("requiresBaseUrl") || !definitionBool("isBuiltIn")) ||

            !draft.baseURL.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)

    }

    private var isValid: Bool {

        let providerID = draft.providerId.trimmingCharacters(in: .whitespacesAndNewlines)

        let validProviderID = providerID.range(of: "^[a-z0-9_]+$", options: .regularExpression) != nil

        let duplicateProviderID = model.providers.contains { provider in

            model.string(provider, "providerId") == providerID && model.string(provider, "id") != draft.id

        }

        return !draft.name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&

        validProviderID && !duplicateProviderID &&

        (!definitionBool("requiresBaseUrl") || !draft.baseURL.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty) &&

        connectionIsValid && !isFetching && !model.bool(relevantAuth, "isRunning")

    }

    private var authenticationIsValid: Bool {

        if draft.authMethod == "oauth" { return definitionBool("supportsOAuth") && !draft.oauthCredentialJson.isEmpty }

        if draft.authMethod == "ambient" { return definitionBool("supportsAmbientAuth") }

        return !definitionBool("supportsApiKey") || !definitionBool("isBuiltIn") || !draft.apiKey.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty

    }

    private var providerIDError: String {

        let providerID = draft.providerId.trimmingCharacters(in: .whitespacesAndNewlines)

        if providerID.isEmpty { return model.text("Provider ID is required.", "必须填写提供商 ID。") }

        if providerID.range(of: "^[a-z0-9_]+$", options: .regularExpression) == nil {

            return model.text("Use lowercase letters, numbers, and underscores only.", "只能使用小写字母、数字和下划线。")

        }

        if model.providers.contains(where: { model.string($0, "providerId") == providerID && model.string($0, "id") != draft.id }) {

            return model.text("This provider ID is already in use.", "该提供商 ID 已被使用。")

        }

        return ""

    }

    private func definitionBool(_ key: String) -> Bool { model.bool(definition, key) }

    private func authLabel(_ method: String) -> String { method == "oauth" ? "OAuth" : method == "ambient" ? model.text("Environment", "环境") : model.text("API key", "API 密钥") }



    private var relevantAuth: [String: Any] {

        let auth = model.providerAuth

        guard !authSessionID.isEmpty, model.string(auth, "sessionId") == authSessionID,

              model.string(auth, "providerId") == draft.piProviderId,

              model.string(auth, "authMethod") == draft.authMethod else { return [:] }

        return auth

    }



    private var oauthConnectionStatus: String {

        if !model.string(relevantAuth, "errorMessage").isEmpty {

            return model.string(relevantAuth, "errorMessage")

        }

        if model.bool(relevantAuth, "isRunning"), !model.string(relevantAuth, "statusMessage").isEmpty {

            return model.string(relevantAuth, "statusMessage")

        }

        if let account = oauthAccountLabel {

            return model.text("Connected as \(account)", "已授权账号：\(account)", "متصل به‌عنوان \(account)")

        }

        return draft.oauthCredentialJson.isEmpty

            ? model.text("Connect your subscription account to continue.", "连接订阅账户后继续。")

            : model.text("Account authorized.", "账户授权成功。")

    }



    private var oauthAccountLabel: String? {

        guard let data = draft.oauthCredentialJson.data(using: .utf8),

              let value = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else { return nil }

        for key in ["email", "accountName", "username", "login", "accountId"] {

            if let label = value[key] as? String, !label.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {

                return label

            }

        }

        return nil

    }



    private var oauthContent: some View {

        Group {

            Text("\(model.string(definition, "displayName")) OAuth")

                .font(.caption)

                .foregroundStyle(.secondary)

            Text(oauthConnectionStatus)

                .font(.body)

                .foregroundStyle(model.string(relevantAuth, "errorMessage").isEmpty ? Color.primary : Color.red)

            oauthDeviceCodeRow

            oauthActionRows

            authStatus

            if !draft.oauthCredentialJson.isEmpty {

                wizardActionRow("trash", model.text("Disconnect", "断开连接"), destructive: true) {

                    draft.oauthCredentialJson = ""

                    clearAuth()

                }

            }

        }

    }



    @ViewBuilder private var oauthDeviceCodeRow: some View {

        if model.bool(relevantAuth, "isRunning"), !model.string(relevantAuth, "deviceCode").isEmpty {

            Button {

                UIPasteboard.general.string = model.string(relevantAuth, "deviceCode")

            } label: {

                HStack {

                    Text(model.string(relevantAuth, "deviceCode")).font(.headline.monospaced())

                    Spacer()

                    Text(model.text("Copy", "复制")).font(.subheadline).foregroundStyle(.tint)

                }

                .padding(12)

                .background { providerCardBackground(Color(uiColor: .tertiarySystemGroupedBackground), minimumRadius: 10) }

            }

            .buttonStyle(.plain)

        }

    }



    @ViewBuilder private var oauthActionRows: some View {

        if model.bool(relevantAuth, "isRunning") {

            ProgressView()

            Button(model.text("Cancel", "取消")) { clearAuth() }

        }

        if draft.piProviderId == "openai-codex" {

            wizardActionRow("safari", model.text("Browser login", "浏览器登录")) { authenticate(flow: "browser") }

            Divider()

            wizardActionRow("number", model.text("Device code login", "设备代码登录")) { authenticate(flow: "device_code") }

        } else if draft.piProviderId == "github-copilot" {

            wizardActionRow("number", model.text("Device code login", "设备代码登录")) { authenticate(flow: "device_code") }

        } else {

            wizardActionRow("safari", model.text("Browser login", "浏览器登录")) { authenticate(flow: "browser") }

        }

    }



    @ViewBuilder

    private func providerCardBackground(_ color: Color, minimumRadius: CGFloat) -> some View {

        if #available(iOS 26.0, *) {

            ConcentricRectangle(

                corners: .concentric(minimum: .fixed(minimumRadius)),

                isUniform: true

            )

            .fill(color)

        } else {

            RoundedRectangle(cornerRadius: minimumRadius, style: .continuous)

                .fill(color)

        }

    }



    private func wizardActionRow(

        _ icon: String,

        _ title: String,

        destructive: Bool = false,

        action: @escaping () -> Void

    ) -> some View {

        Button(action: action) {

            HStack(spacing: 12) {

                Image(systemName: icon).frame(width: 24)

                Text(title).font(.body.weight(.medium))

                Spacer()

                if !destructive { Image(systemName: "chevron.right").font(.caption).foregroundStyle(.tertiary) }

            }

            .foregroundStyle(destructive ? Color.red : Color.primary)

            .frame(minHeight: 44)

            .contentShape(Rectangle())

        }

        .buttonStyle(.plain)

        .disabled(model.bool(relevantAuth, "isRunning"))

    }



    @ViewBuilder private var apiKeyContent: some View {

        if ["cloudflare-ai-gateway", "cloudflare-workers-ai"].contains(draft.piProviderId) {

            if model.bool(relevantAuth, "isRunning") {

                ProgressView()

                Button(model.text("Cancel", "取消")) { clearAuth() }

            } else if !draft.apiKey.isEmpty {

                Label(model.text("Credentials configured", "凭证已配置", "اطلاعات ورود تنظیم شد"), systemImage: "checkmark.shield")

                Button(model.text("Disconnect", "断开连接"), role: .destructive) {

                    clearAuth()

                    draft.apiKey = ""

                    draft.environment = []

                }

            } else {

                Button { authenticate(flow: "") } label: {

                    Label(model.text("Configure credentials", "配置凭证", "تنظیم اطلاعات ورود"), systemImage: "key")

                }

            }

        } else {

            SecureField(model.text("API key", "API 密钥"), text: $draft.apiKey)

                .textInputAutocapitalization(.never).autocorrectionDisabled()

        }

    }



    private func authenticate(flow: String) {

        clearAuth()

        authSessionID = UUID().uuidString

        model.perform("provider_login", ["id": draft.id, "providerId": draft.piProviderId, "authMethod": draft.authMethod, "oauthFlow": flow, "sessionId": authSessionID])

    }



    private func clearAuth() {

        if !authSessionID.isEmpty { model.perform("provider_clear_auth", ["sessionId": authSessionID]) }

        authSessionID = ""

        appliedCredential = ""

        openedAuthorizationURL = ""

        presentedPromptID = ""

        showingPrompt = false

    }



    @ViewBuilder private var authStatus: some View {

        let auth = relevantAuth

        if draft.authMethod != "oauth", !model.string(auth, "statusMessage").isEmpty {

            Text(model.string(auth, "statusMessage")).font(.caption).foregroundStyle(.secondary)

        }

        if model.bool(auth, "isRunning"), !model.string(auth, "authorizationUrl").isEmpty {

            Button { model.perform("provider_open_auth_url") } label: {

                Label(model.text("Open authorization page", "打开认证页面"), systemImage: "safari")

            }

        }

        if model.bool(auth, "isRunning"), let url = URL(string: model.string(auth, "verificationUrl")), !url.absoluteString.isEmpty {

            Link(model.text("Open verification page", "打开验证页面"), destination: url)

        }

        if auth["prompt"] is [String: Any] { Button(model.text("Continue authentication", "继续认证")) { showingPrompt = true } }

        if draft.authMethod != "oauth", !model.string(auth, "errorMessage").isEmpty {

            Text(model.string(auth, "errorMessage")).foregroundStyle(.red).font(.caption)

        }

    }



    private func pairEditor(_ title: String, pairs: Binding<[NativeProviderPair]>) -> some View {

        DisclosureGroup(title) {

            ForEach(pairs) { pair in

                HStack {

                    TextField(model.text("Name", "名称"), text: pair.name)

                    SecureField(model.text("Value", "值"), text: pair.value)

                    Button(role: .destructive) { pairs.wrappedValue.removeAll { $0.id == pair.wrappedValue.id } } label: { Image(systemName: "minus.circle") }

                        .buttonStyle(.borderless)

                }

                .textInputAutocapitalization(.never).autocorrectionDisabled()

            }

            Button { pairs.wrappedValue.append(.init(name: "", value: "")) } label: { Label(model.text("Add", "添加"), systemImage: "plus") }

        }

    }



    private func applyDefaults(_ id: String) {

        guard let item = model.providerCatalog.first(where: { model.string($0, "id") == id }) else { return }

        clearAuth()

        fetchRequestID = ""

        fetchError = ""

        waitingForWizardModels = false

        draft.name = model.string(item, "displayName")

        draft.providerId = NativeProviderDraft.availableProviderID(id.replacingOccurrences(of: "-", with: "_"),

            existing: Set(model.providers.filter { model.string($0, "id") != draft.id }.map { model.string($0, "providerId") }))

        draft.baseURL = model.string(item, "defaultBaseUrl")

        draft.modelIDs = ""

        draft.authMethod = model.bool(item, "supportsInteractiveApiKey") ? "api_key" : model.bool(item, "supportsOAuth") ? "oauth" : "ambient"

        draft.apiKey = ""; draft.oauthCredentialJson = ""; draft.compatibilityMode = false; draft.cachedModels = []; draft.enabledModels = Set(manualModels)

        draft.serviceTier = ""; draft.environment = []; draft.headers = []; draft.userAgent = "Andex/1.0"

    }



    private func fetchModels() {

        guard connectionIsValid, !isFetching else { return }

        fetchRequestID = UUID().uuidString

        fetchError = ""

        var request = payload()

        request["requestId"] = fetchRequestID

        model.perform("provider_fetch_models", request)

    }



    private func applyResults(_ snapshot: [String: Any]) {

        // Model results belong to a fetch request, not to an authentication session.

        if !fetchRequestID.isEmpty, model.string(snapshot, "providerCompletedRequestId") == fetchRequestID {

            let fetched = (snapshot["providerModels"] as? [String: Any])?[draft.id] as? [String] ?? []

            fetchError = model.string(snapshot, "providerError")

            draft.applyFetchedModels(fetched, error: fetchError)

            fetchRequestID = ""

            if waitingForWizardModels {

                waitingForWizardModels = false

                navigateWizard(to: 3)

            }

        }

        let auth = snapshot["providerAuth"] as? [String: Any] ?? [:]

        guard !authSessionID.isEmpty, model.string(auth, "sessionId") == authSessionID,

              model.string(auth, "providerId") == draft.piProviderId,

              model.string(auth, "authMethod") == draft.authMethod else { return }

        let authorizationURL = model.string(auth, "authorizationUrl")

        if model.bool(auth, "isRunning"), !authorizationURL.isEmpty && authorizationURL != openedAuthorizationURL {

            openedAuthorizationURL = authorizationURL

            model.perform("provider_open_auth_url")

        }

        let promptID = model.string(auth["prompt"] as? [String: Any] ?? [:], "id")

        if promptID.isEmpty {

            showingPrompt = false

        } else if promptID != presentedPromptID {

            presentedPromptID = promptID

            showingPrompt = true

        }

        let apiKey = model.string(auth, "apiKey")

        let credential = model.string(auth, "oauthCredentialJson")

        let result = apiKey + credential

        guard !result.isEmpty, result != appliedCredential else { return }

        appliedCredential = result

        if !apiKey.isEmpty { draft.apiKey = apiKey }

        if !credential.isEmpty { draft.oauthCredentialJson = credential }

        if let values = auth["providerEnvironmentVariables"] as? [[String: Any]], !values.isEmpty { draft.environment = Self.pairs(values) }

    }



    private func payload() -> [String: Any] {

        let orderedEnabledModels = allModels.filter(draft.enabledModels.contains)

        let originalModel = original?["modelId"] as? String ?? ""

        let primaryModel = orderedEnabledModels.contains(originalModel) ? originalModel : orderedEnabledModels.first ?? ""

        return ["id": draft.id, "providerId": draft.providerId.trimmingCharacters(in: .whitespacesAndNewlines), "name": draft.name.trimmingCharacters(in: .whitespacesAndNewlines),

         "piProviderId": draft.piProviderId, "authMethod": draft.authMethod, "apiKey": draft.apiKey.trimmingCharacters(in: .whitespacesAndNewlines), "oauthCredentialJson": draft.oauthCredentialJson,

         "providerEnvironmentVariables": draft.environment.filter { !$0.name.isEmpty }.map { ["name": $0.name, "value": $0.value] }, "baseUrl": draft.baseURL,

         "serviceTier": draft.serviceTier.trimmingCharacters(in: .whitespacesAndNewlines),

         "modelId": primaryModel, "manualModelIds": manualModels, "userAgent": draft.userAgent,

         "customHeaders": draft.headers.filter { !$0.name.isEmpty }.map { ["name": $0.name, "value": $0.value] },

         "compatibilityMode": draft.piProviderId == "openai-compatible" && draft.compatibilityMode, "cachedModels": draft.cachedModels,

         "enabledModelIds": orderedEnabledModels, "isEnabled": draft.isEnabled, "createdAtMillis": draft.createdAt, "updatedAtMillis": Int64(Date().timeIntervalSince1970 * 1_000)]

    }

    private func save() {

        guard isValid else { return }

        model.perform("provider_upsert", payload())

        dismiss()

    }



    private static func pairs(_ value: Any?) -> [NativeProviderPair] {

        (value as? [[String: Any]] ?? []).map { .init(name: $0["name"] as? String ?? "", value: $0["value"] as? String ?? "") }

    }

}



private struct NativeProviderAuthPromptView: View {

    @ObservedObject var model: NativeSettingsModel

    @Environment(\.dismiss) private var dismiss

    @State private var value = ""

    var body: some View {

        NavigationStack {

            NativeAndexForm(model: model) {

                let prompt = model.providerAuth["prompt"] as? [String: Any] ?? [:]

                Section { Text(model.string(prompt, "message")) }

                if let options = prompt["options"] as? [[String: Any]], !options.isEmpty {

                    Section { ForEach(options, id: \.nativeID) { option in Button(model.string(option, "label")) { submit(model.string(option, "id")) } } }

                } else {

                    if ["password", "secret"].contains(model.string(prompt, "type")) {

                        SecureField(model.string(prompt, "placeholder"), text: $value)

                            .textInputAutocapitalization(.never).autocorrectionDisabled()

                    } else {

                        TextField(model.string(prompt, "placeholder"), text: $value)

                            .textInputAutocapitalization(.never).autocorrectionDisabled()

                    }

                    Button(model.text("Submit", "提交")) { submit(value) }

                }

            }

            .navigationTitle(model.text("Authentication", "认证"))

            .toolbar { ToolbarItem(placement: .cancellationAction) { Button(model.text("Cancel", "取消")) { cancel() } } }

            .interactiveDismissDisabled()

            .onChange(of: model.string(model.providerAuth["prompt"] as? [String: Any] ?? [:], "id")) { _, _ in value = "" }

        }

    }

    private func submit(_ answer: String) { send(answer, false) }

    private func cancel() { send("", true) }

    private func send(_ answer: String, _ cancelled: Bool) {

        let prompt = model.providerAuth["prompt"] as? [String: Any] ?? [:]

        model.perform("provider_auth_prompt", ["promptId": model.string(prompt, "id"), "sessionId": model.string(model.providerAuth, "sessionId"), "value": answer, "cancelled": cancelled])

        dismiss()

    }

}



private struct NativeSkillsSettingsView: View {

    @ObservedObject var model: NativeSettingsModel



    var body: some View {

        List {

            operationStatus

            if model.skills.isEmpty {

                ContentUnavailableView(model.text("No skills installed", "尚未安装 Skill"), systemImage: "puzzlepiece")

            }

            ForEach(model.skills, id: \.nativeID) { skill in

                DisclosureGroup {

                    LabeledContent("ID", value: model.string(skill, "id"))

                    LabeledContent(model.text("Files", "文件"), value: "\(model.int(skill, "resourceCount"))")

                    let tools = skill["allowedTools"] as? [String] ?? []

                    LabeledContent(model.text("Allowed tools", "允许的工具"), value: tools.isEmpty ? model.text("Any", "任意") : tools.joined(separator: ", "))

                    if !model.string(skill, "compatibility").isEmpty {

                        LabeledContent(model.text("Compatibility", "兼容性"), value: model.string(skill, "compatibility"))

                    }

                    if !model.string(skill, "source").isEmpty {

                        LabeledContent(model.text("Source", "来源"), value: model.string(skill, "source"))

                    }

                    LabeledContent(model.text("Path", "路径"), value: model.string(skill, "guestPath"))

                    Button(role: .destructive) {

                        model.perform("skill_remove", ["id": model.string(skill, "id")])

                    } label: { Label(model.text("Remove Skill", "移除 Skill"), systemImage: "trash") }

                } label: {

                    HStack(spacing: 10) {

                        VStack(alignment: .leading, spacing: 4) {

                            Text(model.string(skill, "name"))

                            Text(model.string(skill, "description"))

                                .font(.caption).foregroundStyle(.secondary).lineLimit(2)

                        }

                        Spacer()

                        Toggle("", isOn: Binding(

                            get: { model.bool(skill, "enabled") },

                            set: { model.perform("skill_enabled", ["id": model.string(skill, "id"), "enabled": $0]) }

                        )).labelsHidden()

                    }

                }

            }

        }

        .navigationTitle(model.text("Agent Skills", "Agent Skills"))

        .navigationBarTitleDisplayMode(.inline)

        .task { model.perform("clear_operation_status") }

        .toolbar {

            ToolbarItem(placement: .primaryAction) {

                NavigationLink { NativeSkillInstaller(model: model) } label: { Image(systemName: "plus") }

                    .accessibilityLabel(model.text("Add Skill", "添加 Skill"))

            }

        }

    }



    @ViewBuilder private var operationStatus: some View {

        if model.string(model.snapshot, "operation").hasPrefix("skill_") {

            HStack { ProgressView(); Text(model.text("Updating skills...", "正在更新 Skills...")) }

        }

        if !model.string(model.snapshot, "operationError").isEmpty {

            Text(model.string(model.snapshot, "operationError")).foregroundStyle(.red)

        } else if !model.string(model.snapshot, "operationMessage").isEmpty {

            Text(model.string(model.snapshot, "operationMessage")).foregroundStyle(.secondary)

        }

    }

}



private struct NativeSkillInstaller: View {

    @ObservedObject var model: NativeSettingsModel

    @Environment(\.dismiss) private var dismiss

    @State private var remoteURL = ""



    var body: some View {

        NativeAndexForm(model: model) {

            Section(model.text("Local", "本地")) {

                Button {

                    model.perform("skill_install_directory")

                } label: {

                    Label(model.text("Choose Folder", "选择文件夹"), systemImage: "folder")

                }

                Button {

                    model.perform("skill_install_zip")

                } label: {

                    Label(model.text("Choose Zip", "选择 Zip"), systemImage: "doc.zipper")

                }

            }

            Section(model.text("Remote", "远程")) {

                TextField("URL", text: $remoteURL)

                    .keyboardType(.URL)

                    .textInputAutocapitalization(.never)

                Button(model.text("Install from URL", "从 URL 安装")) {

                    let url = remoteURL.trimmingCharacters(in: .whitespacesAndNewlines)

                    model.perform("skill_install_url", ["url": url])

                }

                .disabled(remoteURL.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)

            }

            if model.string(model.snapshot, "operation") == "skill_install" {

                Section { HStack { ProgressView(); Text(model.text("Installing...", "正在安装...")) } }

            }

            if !model.string(model.snapshot, "operationError").isEmpty {

                Section { Text(model.string(model.snapshot, "operationError")).foregroundStyle(.red) }

            }

        }

        .navigationTitle(model.text("Add Skill", "添加 Skill"))

        .navigationBarTitleDisplayMode(.inline)

    }

}



private struct NativeExtensionsOverview: View {

    @ObservedObject var model: NativeSettingsModel

    @State private var tab = 0

    @State private var search = ""

    @State private var pendingEnabled: [String: Bool] = [:]



    var body: some View {

        List {

            Section {

                Picker(model.text("View", "视图"), selection: $tab) {

                    Text(model.text("Installed", "已安装")).tag(0)

                    Text(model.text("Discover", "发现")).tag(1)

                }.pickerStyle(.segmented)

            }

            statusSection

            if tab == 0 {

                if installed.isEmpty && operation.isEmpty {

                    ContentUnavailableView(model.text("No Pi extensions installed", "尚未安装 Pi Extension"), systemImage: "shippingbox")

                }

                ForEach(installed, id: \.nativeID) { item in

                    Section {

                        HStack {

                            VStack(alignment: .leading, spacing: 4) {

                                Text(model.string(item, "name"))

                                if !model.string(item, "version").isEmpty { Text("v\(model.string(item, "version"))").font(.caption).foregroundStyle(.secondary) }

                            }

                            Spacer()

                            let extensionID = model.string(item, "id")

                            Toggle("", isOn: Binding(

                                get: { pendingEnabled[extensionID] ?? model.bool(item, "isEnabled") },

                                set: { enabled in

                                    pendingEnabled[extensionID] = enabled

                                    model.perform("pi_extension_enabled", ["id": extensionID, "enabled": enabled])

                                }

                            )).labelsHidden().disabled(!operation.isEmpty)

                        }

                        if !model.string(item, "description").isEmpty { Text(model.string(item, "description")).font(.caption).foregroundStyle(.secondary) }

                        Text(resources(item)).font(.caption).foregroundStyle(.secondary)

                        HStack {

                            if model.string(item, "kind") == "package" {

                                Button { model.perform("pi_extension_update", ["id": model.string(item, "id")]) } label: { Label(model.text("Update", "更新"), systemImage: "arrow.clockwise") }

                            }

                            Spacer()

                            Button(role: .destructive) { model.perform("pi_extension_remove", ["id": model.string(item, "id")]) } label: { Label(model.text("Remove", "移除"), systemImage: "trash") }

                        }.disabled(!operation.isEmpty)

                    }

                }

            } else {

                if catalog.isEmpty && catalogError.isEmpty {

                    HStack { Spacer(); ProgressView(); Text(model.text("Loading extensions...", "正在加载 Extensions...")); Spacer() }

                } else if !catalogError.isEmpty && catalog.isEmpty {

                    Section {

                        Text(catalogError).foregroundStyle(.secondary)

                        Button(model.text("Retry", "重试")) { model.perform("pi_extensions_discover") }

                    }

                } else if filteredCatalog.isEmpty {

                    ContentUnavailableView(model.text("No extensions found", "未找到 Extension"), systemImage: "magnifyingglass")

                } else {

                    ForEach(Array(filteredCatalog.prefix(40)), id: \.nativeID) { item in

                        NavigationLink {

                            NativePiPackageDetailsView(model: model, entry: item)

                        } label: {

                            VStack(alignment: .leading, spacing: 5) {

                                Text(model.string(item, "name"))

                                Text(model.string(item, "description")).font(.caption).foregroundStyle(.secondary).lineLimit(3)

                                HStack { Text(model.string(item, "author")); Spacer(); Text(downloads(item)) }.font(.caption2).foregroundStyle(.secondary)

                                if !model.string(item, "compatibilityIssue").isEmpty { Label(model.text("Compatibility warning", "兼容性警告"), systemImage: "exclamationmark.triangle").font(.caption).foregroundStyle(.orange) }

                            }

                        }

                    }

                }

            }

        }

        .searchable(text: $search)

        .navigationTitle(model.text("Pi Extensions", "Pi Extensions"))

        .navigationBarTitleDisplayMode(.inline)

        .toolbar {

            ToolbarItemGroup(placement: .primaryAction) {

                Button { model.perform("pi_extension_import") } label: { Image(systemName: "square.and.arrow.down") }

                    .accessibilityLabel(model.text("Import Extension", "导入 Extension"))

                Button { model.perform("pi_extensions_refresh") } label: { Image(systemName: "arrow.clockwise") }

                    .accessibilityLabel(model.text("Refresh", "刷新"))

            }

        }

        .task(id: tab) {

            if tab == 1 && catalog.isEmpty && operation.isEmpty {

                model.perform("pi_extensions_discover")

            }

        }

        .onReceive(model.$snapshot) { snapshot in

            let state = snapshot["piExtensions"] as? [String: Any] ?? [:]

            if model.string(state, "operation").isEmpty {

                pendingEnabled.removeAll()

            }

        }

    }



    private var installed: [[String: Any]] { model.piExtensions["installed"] as? [[String: Any]] ?? [] }

    private var catalog: [[String: Any]] { model.piExtensions["catalog"] as? [[String: Any]] ?? [] }

    private var operation: String { model.string(model.piExtensions, "operation") }

    private var catalogError: String { model.string(model.piExtensions, "catalogError") }

    private var filteredCatalog: [[String: Any]] {

        let query = search.trimmingCharacters(in: .whitespacesAndNewlines)

        guard !query.isEmpty else { return catalog }

        return catalog.filter { item in

            ["name", "description", "author", "source"].contains { key in

                model.string(item, key).localizedCaseInsensitiveContains(query)

            }

        }

    }

    @ViewBuilder private var statusSection: some View {

        if !operation.isEmpty { Section { HStack { ProgressView(); Text(model.text("Working...", "正在处理...")) } } }

        if tab == 0 && !model.string(model.piExtensions, "installedError").isEmpty { Section { Text(model.string(model.piExtensions, "installedError")).foregroundStyle(.red) } }

        else if !model.string(model.piExtensions, "error").isEmpty { Section { Text(model.string(model.piExtensions, "error")).foregroundStyle(.red) } }

        else if !model.string(model.piExtensions, "message").isEmpty { Section { Text(model.string(model.piExtensions, "message")).foregroundStyle(.secondary) } }

    }

    private func resources(_ item: [String: Any]) -> String {

        [model.text("\(model.int(item, "extensionCount")) extensions", "\(model.int(item, "extensionCount")) 个 Extension", "\(model.int(item, "extensionCount")) افزونه"),

         model.text("\(model.int(item, "skillCount")) skills", "\(model.int(item, "skillCount")) 个 Skill", "\(model.int(item, "skillCount")) مهارت"),

         model.text("\(model.int(item, "promptCount")) prompts", "\(model.int(item, "promptCount")) 个 Prompt", "\(model.int(item, "promptCount")) پرامپت")].joined(separator: " · ")

    }

    private func downloads(_ item: [String: Any]) -> String {

        let value = (item["monthlyDownloads"] as? NSNumber)?.int64Value ?? 0

        return model.text("\(value) monthly", "每月 \(value) 次", "ماهانه \(value)")

    }

}



private struct NativePiPackageDetailsView: View {

    @ObservedObject var model: NativeSettingsModel

    let entry: [String: Any]

    @State private var didRequestDetails = false

    @State private var requestPending = false



    var body: some View {

        List {

            Section {

                Text(model.string(entry, "name"))

                    .font(.title3.weight(.semibold))

                if !summaryDescription.isEmpty {

                    Text(summaryDescription).foregroundStyle(.secondary)

                }

                LabeledContent(model.text("Source", "来源"), value: model.string(entry, "source"))

                if !model.string(entry, "author").isEmpty {

                    LabeledContent(model.text("Author", "作者"), value: model.string(entry, "author"))

                }

                if !compatibilityIssue.isEmpty {

                    Label(compatibilityMessage, systemImage: "exclamationmark.triangle")

                        .foregroundStyle(.orange)

                }

            }



            if isLoading {

                Section {

                    HStack {

                        Spacer()

                        ProgressView()

                        Text(model.text("Loading package...", "正在加载软件包..."))

                        Spacer()

                    }

                }

            } else if details.isEmpty {

                Section {

                    ContentUnavailableView(

                        model.text("Package details unavailable", "无法获取软件包详情"),

                        systemImage: "doc.text.magnifyingglass",

                        description: Text(detailsError)

                    )

                    Button(model.text("Retry", "重试"), action: requestDetails)

                }

            } else {

                Section(model.text("Package Information", "软件包信息")) {

                    detail(model.text("Version", "版本"), "version")

                    detail(model.text("Published", "发布时间"), "published")

                    detail(model.text("Downloads", "下载量"), "downloads")

                    detail(model.text("Author", "作者"), "author")

                    detail(model.text("License", "许可证"), "license")

                    detail(model.text("Types", "类型"), value: detailTypes)

                    detail(model.text("Size", "大小"), "size")

                    detail(model.text("Dependencies", "依赖"), "dependencies")

                }

                Section {

                    Button { model.perform("pi_extension_install", ["source": model.string(entry, "source")]) } label: {

                        if isInstalling {

                            HStack { ProgressView(); Text(model.text("Installing...", "正在安装...")) }

                        } else {

                            Label(installed ? model.text("Reinstall", "重新安装") : model.text("Install", "安装"), systemImage: "square.and.arrow.down")

                        }

                    }

                    .disabled(isInstalling)

                    if let url = URL(string: model.string(details, "npmUrl")), !url.absoluteString.isEmpty { Link("npm", destination: url) }

                    if let url = URL(string: model.string(details, "repositoryUrl")), !url.absoluteString.isEmpty { Link(model.text("Repository", "代码仓库"), destination: url) }

                }

                if !model.string(details, "readmeMarkdown").isEmpty {

                    Section("README") {

                        NativeMarkdownView(markdown: model.string(details, "readmeMarkdown"))

                            .textSelection(.enabled)

                    }

                }

            }

        }

        .navigationTitle(model.string(entry, "name"))

        .navigationBarTitleDisplayMode(.inline)

        .task { requestDetails() }

        .onReceive(model.$snapshot) { snapshot in

            guard didRequestDetails else { return }

            let state = snapshot["piExtensions"] as? [String: Any] ?? [:]

            let operation = model.string(state, "operation")

            let response = state["details"] as? [String: Any] ?? [:]

            let matches = model.string(response, "source") == model.string(entry, "source")

            if operation != "details" && (matches || !model.string(state, "error").isEmpty) {

                requestPending = false

            }

        }

    }



    private var details: [String: Any] {

        let value = model.piExtensions["details"] as? [String: Any] ?? [:]

        return model.string(value, "source") == model.string(entry, "source") ? value : [:]

    }

    private var installed: Bool { (model.piExtensions["installed"] as? [[String: Any]] ?? []).contains { model.string($0, "source") == model.string(entry, "source") } }

    private var isLoading: Bool {

        !didRequestDetails || requestPending || model.string(model.piExtensions, "operation") == "details"

    }

    private var isInstalling: Bool { model.string(model.piExtensions, "operation") == "install" }

    private var detailsError: String {

        model.string(model.piExtensions, "error").isEmpty

            ? model.text("Check your connection and try again.", "请检查网络连接后重试。")

            : model.string(model.piExtensions, "error")

    }

    private var summaryDescription: String {

        model.string(details, "description", fallback: model.string(entry, "description"))

    }

    private var compatibilityIssue: String {

        model.string(details, "compatibilityIssue", fallback: model.string(entry, "compatibilityIssue"))

    }

    private var compatibilityMessage: String {

        switch compatibilityIssue {

        case "interactiveui", "interactive_ui": model.text("This extension requires interactive terminal UI features.", "此 Extension 依赖交互式终端 UI。")

        case "theme": model.text("Theme extensions are not supported here.", "此处不支持主题 Extension。")

        case "prompt": model.text("Prompt extensions may not work in this environment.", "提示词 Extension 可能无法在此环境中工作。")

        default: model.text("This extension may not be compatible with the current platform.", "此 Extension 可能与当前平台不兼容。")

        }

    }

    private var detailTypes: String {

        (details["types"] as? [String] ?? []).joined(separator: ", ")

    }



    private func requestDetails() {

        didRequestDetails = true

        requestPending = true

        model.perform("pi_extension_details", ["source": model.string(entry, "source")])

    }



    @ViewBuilder private func detail(_ label: String, _ key: String) -> some View {

        detail(label, value: model.string(details, key))

    }



    @ViewBuilder private func detail(_ label: String, value: String) -> some View {

        if !value.isEmpty { LabeledContent(label, value: value) }

    }

}



private struct NativeMarkdownView: View {

    let markdown: String



    var body: some View {

        VStack(alignment: .leading, spacing: 12) {

            ForEach(Array(blocks.enumerated()), id: \.offset) { _, block in

                blockView(block)

            }

        }

        .frame(maxWidth: .infinity, alignment: .leading)

    }



    @ViewBuilder

    private func blockView(_ block: Block) -> some View {

        switch block {

        case let .heading(level, text):

            inlineText(text)

                .font(headingFont(level))

                .fontWeight(.semibold)

        case let .paragraph(text):

            inlineText(text).font(.body)

        case let .bullet(text):

            HStack(alignment: .firstTextBaseline, spacing: 8) {

                Text("\u{2022}")

                inlineText(text).frame(maxWidth: .infinity, alignment: .leading)

            }

        case let .numbered(number, text):

            HStack(alignment: .firstTextBaseline, spacing: 8) {

                Text("\(number).")

                inlineText(text).frame(maxWidth: .infinity, alignment: .leading)

            }

        case let .quote(text):

            HStack(alignment: .top, spacing: 10) {

                Rectangle().fill(.tint).frame(width: 3)

                inlineText(text).foregroundStyle(.secondary)

            }

        case let .code(text):

            ScrollView(.horizontal) {

                Text(text)

                    .font(.system(.callout, design: .monospaced))

                    .fixedSize(horizontal: true, vertical: false)

                    .padding(12)

            }

            .background(Color(uiColor: .tertiarySystemGroupedBackground))

            .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))

        case let .image(url, description):

            AsyncImage(url: url) { phase in

                switch phase {

                case let .success(image): image.resizable().scaledToFit()

                case .failure: Label(description, systemImage: "photo.badge.exclamationmark").foregroundStyle(.secondary)

                default: ProgressView().frame(maxWidth: .infinity)

                }

            }

            .frame(maxWidth: .infinity)

        case .rule:

            Divider()

        }

    }



    private func inlineText(_ source: String) -> Text {

        let options = AttributedString.MarkdownParsingOptions(interpretedSyntax: .inlineOnlyPreservingWhitespace)

        return (try? AttributedString(markdown: source, options: options)).map(Text.init) ?? Text(source)

    }



    private func headingFont(_ level: Int) -> Font {

        switch level {

        case 1: .title2

        case 2: .title3

        default: .headline

        }

    }



    private var blocks: [Block] { Block.parse(markdown) }



    private enum Block {

        case heading(Int, String)

        case paragraph(String)

        case bullet(String)

        case numbered(Int, String)

        case quote(String)

        case code(String)

        case image(URL, String)

        case rule



        static func parse(_ markdown: String) -> [Block] {

            let lines = markdown.replacingOccurrences(of: "\r\n", with: "\n").components(separatedBy: "\n")

            var result: [Block] = []

            var paragraph: [String] = []

            var code: [String] = []

            var insideCode = false



            func flushParagraph() {

                guard !paragraph.isEmpty else { return }

                result.append(.paragraph(paragraph.joined(separator: " ")))

                paragraph.removeAll()

            }



            for line in lines {

                let trimmed = line.trimmingCharacters(in: .whitespaces)

                if trimmed.hasPrefix("```") {

                    flushParagraph()

                    if insideCode {

                        result.append(.code(code.joined(separator: "\n")))

                        code.removeAll()

                    }

                    insideCode.toggle()

                    continue

                }

                if insideCode {

                    code.append(line)

                    continue

                }

                if trimmed.isEmpty {

                    flushParagraph()

                    continue

                }

                if let image = markdownImage(trimmed) {

                    flushParagraph()

                    result.append(.image(image.url, image.description))

                    continue

                }

                let headingLevel = trimmed.prefix { $0 == "#" }.count

                if (1...6).contains(headingLevel), trimmed.dropFirst(headingLevel).first == " " {

                    flushParagraph()

                    result.append(.heading(headingLevel, String(trimmed.dropFirst(headingLevel + 1))))

                } else if trimmed == "---" || trimmed == "***" || trimmed == "___" {

                    flushParagraph()

                    result.append(.rule)

                } else if ["- ", "* ", "+ "].contains(where: trimmed.hasPrefix) {

                    flushParagraph()

                    result.append(.bullet(String(trimmed.dropFirst(2))))

                } else if let numbered = numberedItem(trimmed) {

                    flushParagraph()

                    result.append(.numbered(numbered.number, numbered.text))

                } else if trimmed.hasPrefix("> ") {

                    flushParagraph()

                    result.append(.quote(String(trimmed.dropFirst(2))))

                } else {

                    paragraph.append(trimmed)

                }

            }

            flushParagraph()

            if !code.isEmpty { result.append(.code(code.joined(separator: "\n"))) }

            return result

        }



        private static func numberedItem(_ line: String) -> (number: Int, text: String)? {

            guard let marker = line.firstIndex(of: "."), marker < line.endIndex else { return nil }

            let numberText = String(line[..<marker])

            let contentStart = line.index(after: marker)

            guard let number = Int(numberText), contentStart < line.endIndex, line[contentStart] == " " else { return nil }

            return (number, String(line[line.index(after: contentStart)...]))

        }



        private static func markdownImage(_ line: String) -> (url: URL, description: String)? {

            guard line.hasPrefix("!["), let separator = line.range(of: "](") , line.hasSuffix(")") else { return nil }

            let description = String(line[line.index(line.startIndex, offsetBy: 2)..<separator.lowerBound])

            let urlText = String(line[separator.upperBound..<line.index(before: line.endIndex)])

            guard let url = URL(string: urlText) else { return nil }

            return (url, description)

        }

    }

}



private struct NativeAlpineSettingsView: View {

    @ObservedObject var model: NativeSettingsModel

    @State private var showsFiles = false

    @State private var showsTerminal = false

    @State private var confirmsReset = false



    var body: some View {

        List {

            Section {

                LabeledContent(model.text("Status", "状态"), value: statusLabel)

                if !model.string(model.alpine, "detail").isEmpty {

                    Text(model.string(model.alpine, "detail")).font(.caption).foregroundStyle(.secondary)

                }

                HStack {

                    Button {

                        model.perform(ready ? "alpine_refresh" : "alpine_initialize")

                    } label: {

                        if busy { ProgressView() } else { Text(ready ? model.text("Ready", "已就绪") : model.text("Initialize", "初始化")) }

                    }

                    .disabled(busy || ready)

                    Spacer()

                    Button { model.perform("alpine_refresh") } label: { Label(model.text("Refresh", "刷新"), systemImage: "arrow.clockwise") }

                        .disabled(busy)

                }

                .buttonStyle(.borderless)

                .listRowSeparator(.hidden, edges: .bottom)

                Button(role: .destructive) { confirmsReset = true } label: { Label(model.text("Reset Alpine Data", "重置 Alpine 数据"), systemImage: "trash") }

                    .disabled(busy)

                if ready && !model.bool(model.alpine, "isDefault") {

                    Button { model.perform("alpine_set_default") } label: { Label(model.text("Use as Default Runtime", "设为默认运行时"), systemImage: "checkmark.circle") }

                }

            } footer: {

                Text(model.text("The Alpine environment stays inside Andex's private app storage.", "Alpine 环境保存在 Andex 的私有应用存储中。"))

            }

            Section(model.text("Open", "打开")) {

                Button { showsTerminal = true } label: { Label(model.text("Terminal", "终端"), systemImage: "terminal") }.disabled(!ready)

                Button { showsFiles = true } label: { Label(model.text("Files", "文件"), systemImage: "folder") }.disabled(!ready)

            }

            Section {

                ForEach(profiles, id: \.nativeID) { profile in

                    HStack(spacing: 12) {

                        VStack(alignment: .leading, spacing: 3) {

                            Text(profileTitle(model.string(profile, "id")))

                            Text(profileSubtitle(model.string(profile, "id"))).font(.caption).foregroundStyle(.secondary)

                            if !model.string(profile, "error").isEmpty {

                                Text(model.string(profile, "error")).font(.caption2).foregroundStyle(.red).lineLimit(2)

                            } else if installing(model.string(profile, "id")) && !model.string(model.alpine, "progress").isEmpty {

                                Text(model.string(model.alpine, "progress")).font(.caption2).foregroundStyle(.tint).lineLimit(1)

                            }

                        }

                        Spacer()

                        Button {

                            model.perform("alpine_install_profile", ["profileId": model.string(profile, "id")])

                        } label: {

                            if installing(model.string(profile, "id")) { ProgressView() }

                            else { Text(model.bool(profile, "installed") ? model.text("Installed", "已安装") : model.text("Install", "安装")) }

                        }

                        .buttonStyle(.bordered)

                        .disabled(!ready || busy || model.bool(profile, "installed"))

                    }

                }

            } header: {

                Text(model.text("Environment Presets", "环境预设"))

            } footer: {

                Text(model.text("Install common development tools into the Alpine environment.", "将常用开发工具安装到 Alpine 环境。"))

            }

        }

        .navigationTitle("Alpine")

        .navigationBarTitleDisplayMode(.inline)

        .sheet(isPresented: $showsFiles) { AlpineFileManagerView(host: AndexRuntimeHost.shared) }

        .fullScreenCover(isPresented: $showsTerminal) { NativeAlpineTerminalScreen() }

        .confirmationDialog(model.text("Reset Alpine data?", "重置 Alpine 数据？"), isPresented: $confirmsReset, titleVisibility: .visible) {

            Button(model.text("Reset", "重置"), role: .destructive) { model.perform("alpine_reset") }

        } message: {

            Text(model.text("This removes the runtime and all files stored inside it.", "这会移除运行时及其中保存的全部文件。"))

        }

    }



    private var ready: Bool { model.bool(model.alpine, "ready") }

    private var busy: Bool { !model.string(model.alpine, "operation").isEmpty }

    private var profiles: [[String: Any]] { model.alpine["profiles"] as? [[String: Any]] ?? [] }

    private var statusLabel: String {

        switch model.string(model.alpine, "issue") {

        case "ready": model.text("Ready", "已就绪")

        case "failed": model.text("Failed", "失败")

        default: model.text("Not installed", "未安装")

        }

    }

    private func installing(_ id: String) -> Bool { model.string(model.alpine, "operation") == "install:\(id)" }

    private func profileTitle(_ id: String) -> String {

        switch id {

        case "python": model.text("Python Environment", "Python 环境")

        case "node": model.text("Node Environment", "Node 环境")

        case "git_search": model.text("Git & Search Tools", "Git 与搜索工具")

        default: model.text("SSH Tools", "SSH 工具")

        }

    }

    private func profileSubtitle(_ id: String) -> String {

        switch id { case "python": "python3, pip, virtualenv"; case "node": "nodejs, npm"; case "git_search": "git, ripgrep"; default: "openssh-client" }

    }

}



private struct NativeStatisticsView: View {

    @ObservedObject var model: NativeSettingsModel



    var body: some View {

        List {

            Section(model.text("Overview", "概览")) {

                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {

                    metricTile(model.text("Total tokens", "Token 总量"), formatTokens(long("totalTokens")), "number")

                    metricTile(model.text("Sessions", "会话"), "\(model.int(model.statistics, "sessionCount"))", "bubble.left.and.bubble.right")

                    metricTile(model.text("Average speed", "平均速度"), averageSpeed, "speedometer")

                    metricTile(model.text("Average latency", "平均延迟"), averageLatency, "timer")

                }

                .padding(.vertical, 4)

                .listRowBackground(Color(uiColor: .secondarySystemGroupedBackground))

            }

            if dailyUsage.contains(where: { tokens($0) > 0 }) {

                Section(model.text("Recent 7 days", "最近 7 天")) {

                    Chart(Array(dailyUsage.suffix(7)), id: \.nativeID) { day in

                        BarMark(

                            x: .value(model.text("Day", "日期"), model.string(day, "shortLabel")),

                            y: .value("Tokens", tokens(day))

                        )

                        .foregroundStyle(.tint)

                    }

                    .frame(height: 190)

                    .chartYAxis { AxisMarks(position: .leading) }

                }

            }

            if allDailyUsage.contains(where: { tokens($0) > 0 }) {

                Section(model.text("Token history", "Token 历史")) {

                    Chart(Array(allDailyUsage.suffix(14)), id: \.nativeID) { day in

                        LineMark(

                            x: .value(model.text("Day", "日期"), model.string(day, "shortLabel")),

                            y: .value("Tokens", tokens(day))

                        ).interpolationMethod(.catmullRom).foregroundStyle(.tint)

                        PointMark(

                            x: .value(model.text("Day", "日期"), model.string(day, "shortLabel")),

                            y: .value("Tokens", tokens(day))

                        ).foregroundStyle(.tint)

                    }

                    .frame(height: 190)

                    .chartYAxis { AxisMarks(position: .leading) }

                }

            }

            Section(model.text("Token mix", "Token 构成")) {

                if tokenMixTotal > 0 {

                    Chart(tokenMix, id: \.0) { item in

                        SectorMark(angle: .value("Tokens", item.1), innerRadius: .ratio(0.58), angularInset: 1.5)

                            .foregroundStyle(by: .value(model.text("Type", "类型"), item.0))

                    }

                    .frame(height: 210)

                    .chartLegend(position: .bottom, spacing: 12)

                }

                tokenLine(model.text("Input", "输入"), value: long("inputTokens"), color: .blue)

                tokenLine(model.text("Output", "输出"), value: long("outputTokens"), color: .green)

                tokenLine(model.text("Reasoning", "推理"), value: long("reasoningTokens"), color: .orange)

            }

            Section(model.text("Cache tokens", "缓存 token")) {

                LabeledContent(model.text("Cached read", "缓存读取"), value: optionalTokens("cachedInputTokens"))

                LabeledContent(model.text("Cached write", "缓存写入"), value: optionalTokens("cacheWriteTokens"))

            }

            Section(model.text("History", "历史")) {

                LabeledContent(model.text("Peak day", "峰值日期"), value: peakDay)

                LabeledContent(model.text("Largest turn", "最大单轮"), value: optionalTokens("largestTurnTokens"))

                LabeledContent(model.text("Average turn", "平均单轮"), value: optionalTokens("averageTurnTokens"))

                LabeledContent(model.text("Recorded turns", "已记录轮次"), value: "\(model.int(model.statistics, "turnCount"))")

            }

            if !speedSamples.isEmpty {

                Section(model.text("Recent speed", "近期速度")) {

                    Chart(speedSamples, id: \.nativeID) { sample in

                        BarMark(

                            x: .value(model.text("Sample", "样本"), model.string(sample, "shortLabel")),

                            y: .value("tok/s", (sample["tokensPerSecond"] as? NSNumber)?.doubleValue ?? 0)

                        ).foregroundStyle(.green)

                    }

                    .frame(height: 190)

                    .chartYAxis { AxisMarks(position: .leading) }

                }

            }

        }

        .navigationTitle(model.text("Usage Statistics", "使用统计"))

        .navigationBarTitleDisplayMode(.inline)

        .task { model.perform("refresh_statistics") }

    }



    private var dailyUsage: [[String: Any]] {

        model.statistics["recentDailyTokenUsage"] as? [[String: Any]] ?? []

    }

    private var allDailyUsage: [[String: Any]] {

        model.statistics["allDailyTokenUsage"] as? [[String: Any]] ?? dailyUsage

    }

    private var speedSamples: [[String: Any]] {

        model.statistics["recentSpeedSamples"] as? [[String: Any]] ?? []

    }

    private var tokenMix: [(String, Int64)] {

        [(model.text("Input", "输入"), long("inputTokens")),

         (model.text("Output", "输出"), long("outputTokens")),

         (model.text("Reasoning", "推理"), long("reasoningTokens"))].filter { $0.1 > 0 }

    }

    private var tokenMixTotal: Int64 { tokenMix.reduce(0) { $0 + $1.1 } }



    private func long(_ key: String) -> Int64 {

        (model.statistics[key] as? NSNumber)?.int64Value ?? 0

    }



    private func tokens(_ day: [String: Any]) -> Int64 {

        (day["tokens"] as? NSNumber)?.int64Value ?? 0

    }



    private var averageSpeed: String {

        guard let value = (model.statistics["averageOutputTokensPerSecond"] as? NSNumber)?.doubleValue else {

            return model.text("Unavailable", "暂无")

        }

        return String(format: "%.1f tok/s", value)

    }



    private var averageLatency: String {

        guard let value = (model.statistics["averageFirstTokenLatencyMillis"] as? NSNumber)?.int64Value else {

            return model.text("Unavailable", "暂无")

        }

        return value >= 1_000 ? String(format: "%.1f s", Double(value) / 1_000) : "\(value) ms"

    }



    private var peakDay: String {

        guard let peak = model.statistics["peakDay"] as? [String: Any] else {

            return model.text("Unavailable", "暂无")

        }

        return "\(model.string(peak, "label")) · \(formatTokens((peak["tokens"] as? NSNumber)?.int64Value ?? 0))"

    }



    private func optionalTokens(_ key: String) -> String {

        guard let value = (model.statistics[key] as? NSNumber)?.int64Value else {

            return model.text("Unavailable", "暂无")

        }

        return formatTokens(value)

    }



    private func formatTokens(_ value: Int64) -> String {

        if value >= 1_000_000 { return String(format: "%.1fM", Double(value) / 1_000_000) }

        if value >= 1_000 { return String(format: "%.1fK", Double(value) / 1_000) }

        return "\(value)"

    }



    private func tokenLine(_ label: String, value: Int64, color: Color) -> some View {

        HStack {

            Circle().fill(color).frame(width: 9, height: 9)

            Text(label)

            Spacer()

            Text(formatTokens(value)).foregroundStyle(.secondary)

        }

    }



    private func metricTile(_ title: String, _ value: String, _ icon: String) -> some View {

        VStack(alignment: .leading, spacing: 8) {

            Image(systemName: icon).foregroundStyle(.tint)

            Text(value).font(.title3.bold()).lineLimit(1).minimumScaleFactor(0.75)

            Text(title).font(.caption).foregroundStyle(.secondary)

        }

        .frame(maxWidth: .infinity, minHeight: 78, alignment: .leading)

        .padding(12)

    }

}



@MainActor

private final class NativeAlpineTerminalModel: NSObject, ObservableObject, NativeRuntimeProcessListener {

    @Published var title = "Alpine"

    @Published var status = "Starting Alpine..."

    let terminal = AndexTerminalView(frame: .zero)

    private var processID: Int64 = 0



    override init() {

        super.init()

        terminal.onInput = { [weak self] data in

            guard let self, self.processID > 0 else { return }

            _ = AndexRuntimeHost.shared.writeStdin(processId: self.processID, bytes: data.nativeKotlinBytes)

        }

        terminal.onResize = { [weak self] columns, rows in

            guard let self, self.processID > 0 else { return }

            AndexRuntimeHost.shared.resizeTerminal(processId: self.processID, columns: Int32(columns), rows: Int32(rows))

        }

        terminal.onTitleChanged = { [weak self] title in self?.title = title.isEmpty ? "Alpine" : title }

    }



    func start() {

        guard processID == 0 else { return }

        processID = AndexRuntimeHost.shared.startProcess(

            executable: "/bin/sh",

            arguments: ["-l"],

            environment: ["HOME": "/root", "TERM": "xterm-256color", "ANDEX_WORKSPACE": "/workspace"],

            workingDirectory: "/workspace",

            redirectErrorStream: true,

            interactiveTerminal: true,

            remoteDebuggingPipe: false,

            listener: self

        )

        status = processID > 0 ? "" : "Unable to start Alpine shell."

        DispatchQueue.main.async { [weak self] in self?.terminal.focus() }

    }



    func stop() {

        if processID > 0 { AndexRuntimeHost.shared.signal(processId: processID, signal: 15) }

        processID = 0

        terminal.cleanup()

    }



    nonisolated func onStdout(bytes: KotlinByteArray) {

        let data = bytes.nativeData

        DispatchQueue.main.async { [weak self] in self?.terminal.feed(data) }

    }

    nonisolated func onStderr(bytes: KotlinByteArray) { onStdout(bytes: bytes) }

    nonisolated func onExit(exitCode: Int32, signal: Int32) {

        DispatchQueue.main.async { [weak self] in

            self?.processID = 0

            self?.status = "Exited (\(exitCode))"

        }

    }

}



private struct NativeAlpineTerminalScreen: View {

    @Environment(\.dismiss) private var dismiss

    @StateObject private var terminal = NativeAlpineTerminalModel()



    var body: some View {

        NavigationStack {

            ZStack {

                NativeAlpineTerminalSurface(view: terminal.terminal).background(Color.black)

                if !terminal.status.isEmpty {

                    Text(terminal.status).foregroundStyle(.secondary).padding()

                }

            }

            .ignoresSafeArea(.keyboard, edges: .bottom)

            .navigationTitle(terminal.title)

            .navigationBarTitleDisplayMode(.inline)

            .toolbar { ToolbarItem(placement: .cancellationAction) { Button("Done") { dismiss() } } }

        }

        .preferredColorScheme(.dark)

        .onAppear { terminal.start() }

        .onDisappear { terminal.stop() }

    }

}



private struct NativeAlpineTerminalSurface: UIViewRepresentable {

    let view: AndexTerminalView

    func makeUIView(context: Context) -> AndexTerminalView { view }

    func updateUIView(_ uiView: AndexTerminalView, context: Context) { uiView.setDarkTheme(true) }

}



private extension Data {

    var nativeKotlinBytes: KotlinByteArray {

        let result = KotlinByteArray(size: Int32(count))

        for (index, byte) in enumerated() { result.set(index: Int32(index), value: Int8(bitPattern: byte)) }

        return result

    }

}



private extension KotlinByteArray {

    var nativeData: Data {

        var result = Data(count: Int(size))

        result.withUnsafeMutableBytes { buffer in

            guard let base = buffer.baseAddress?.assumingMemoryBound(to: UInt8.self) else { return }

            for index in 0..<Int(size) { base[index] = UInt8(bitPattern: get(index: Int32(index))) }

        }

        return result

    }

}



private struct NativeDeveloperSettingsView: View {

    @ObservedObject var model: NativeSettingsModel

    @State private var confirmExport = false

    @State private var confirmImport = false

    private var developerRoleFallbackProviders: [[String: Any]] {

        model.providers.filter { model.bool($0, "developerRoleUnsupported") }

    }

    var body: some View {

        NativeAndexForm(model: model) {

            if !model.string(model.snapshot, "operationError").isEmpty {

                Section { Text(model.string(model.snapshot, "operationError")).foregroundStyle(.red) }

            } else if !model.string(model.snapshot, "operationMessage").isEmpty {

                Section { Text(model.string(model.snapshot, "operationMessage")).foregroundStyle(.secondary) }

            }

            Section {

                Button { confirmImport = true } label: { Label(model.text("Import app data", "导入应用数据"), systemImage: "square.and.arrow.down") }

                Button { confirmExport = true } label: { Label(model.text("Export app data", "导出应用数据"), systemImage: "square.and.arrow.up") }

            } header: {

                Text(model.text("App data", "应用数据"))

            } footer: {

                Text(model.text("Back up or restore settings, providers, skills, and conversations.", "备份或恢复设置、提供商、Skills 与对话。"))

            }

            Section(model.text("Setup previews", "设置预览")) {

                Button {

                    model.isPresented = false

                    model.perform("developer_replay_alpine")

                } label: { Label(model.text("Replay Alpine setup preview", "重放 Alpine 设置预览"), systemImage: "play") }

                Button {

                    model.isPresented = false

                    model.perform("developer_replay_follow_up")

                } label: { Label(model.text("Replay follow-up onboarding", "重放后续引导"), systemImage: "arrow.counterclockwise") }

            }

            Section {

                Button { model.perform("developer_export_logs") } label: { Label(model.text("Export logs", "导出日志"), systemImage: "doc.text") }

            } header: {

                Text(model.text("Diagnostics", "诊断"))

            } footer: {

                Text(model.text("The export redacts stored credentials and includes runtime diagnostics.", "导出内容会隐藏已存储的凭证，并包含运行时诊断信息。"))

            }

            if !developerRoleFallbackProviders.isEmpty {

                Section {

                    ForEach(developerRoleFallbackProviders, id: \.nativeID) { provider in

                        Toggle(

                            model.string(provider, "name"),

                            isOn: Binding(

                                get: { true },

                                set: { enabled in

                                    if !enabled {

                                        model.perform(

                                            "developer_clear_developer_role_fallback",

                                            ["providerConfigId": model.string(provider, "id")]

                                        )

                                    }

                                }

                            )

                        )

                    }

                } header: {

                    Text(model.text("Developer message fallbacks", "Developer 消息回退"))

                } footer: {

                    Text(model.text(

                        "These providers rejected the developer message role, so Andex uses system messages for them. Turn a provider off to try developer messages again.",

                        "这些提供商拒绝了 Developer 消息角色，因此 Andex 会对它们使用 System 消息。关闭提供商标记即可重新尝试 Developer 消息。"

                    ))

                }

            }

            Section(model.text("Command history", "命令历史")) {

                Toggle(model.text("Automatically clean old history", "自动清理旧历史"), isOn: Binding(

                    get: { model.settingBool("autoCleanOldCommandHistory", fallback: true) },

                    set: { model.patch(["autoCleanOldCommandHistory": $0]) }

                ))

                Stepper(value: Binding(

                    get: { model.settingInt("oldCommandHistoryRetentionHours", fallback: 6) },

                    set: { model.patch(["oldCommandHistoryRetentionHours": $0]) }

                ), in: 1...168) {

                    LabeledContent(

                        model.text("Retention", "保留时间"),

                        value: model.text(

                            "\(model.settingInt("oldCommandHistoryRetentionHours", fallback: 6)) hours",

                            "\(model.settingInt("oldCommandHistoryRetentionHours", fallback: 6)) 小时",

                            "\(model.settingInt("oldCommandHistoryRetentionHours", fallback: 6)) ساعت"

                        )

                    )

                }

            }

        }

        .navigationTitle(model.text("Developer", "开发者"))

        .navigationBarTitleDisplayMode(.inline)

        .task { model.perform("clear_operation_status") }

        .confirmationDialog(

            model.text("Import app data?", "导入应用数据？"),

            isPresented: $confirmImport,

            titleVisibility: .visible

        ) {

            Button(model.text("Choose backup", "选择备份")) { model.perform("developer_import_data") }

        } message: {

            Text(model.text("Existing local data will be replaced by the selected backup.", "现有本地数据将被所选备份替换。"))

        }

        .confirmationDialog(

            model.text("Export app data?", "导出应用数据？"),

            isPresented: $confirmExport,

            titleVisibility: .visible

        ) {

            Button(model.text("Export", "导出")) { model.perform("developer_export_data") }

        } message: {

            Text(model.text("The backup may contain conversation content and API credentials. Store it securely.", "备份可能包含对话内容和 API 凭证，请妥善保管。"))

        }

    }

}



private struct NativeAboutSettingsView: View {

    @ObservedObject var model: NativeSettingsModel

    var body: some View {

        ScrollView {

            VStack(spacing: 16) {

                applicationIconView

                    .frame(width: 96, height: 96)

                    .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))

                    .padding(.top, 24)

                Text("Andex").font(.largeTitle.bold())

                Text(model.text("Release \(version)", "正式版 \(version)", "نسخه \(version)"))

                    .font(.subheadline).foregroundStyle(.secondary)

                VStack(spacing: 0) {

                    aboutRow(model.text("Author", "作者"), "Yukie")

                    Divider().padding(.leading, 16)

                    aboutRow(model.text("Version", "版本"), version)

                    Divider().padding(.leading, 16)

                    Link(destination: URL(string: "https://andex.baimoqilin.com")!) {

                        linkRow(model.text("Website", "网站"), "globe")

                    }

                    Divider().padding(.leading, 16)

                    Link(destination: URL(string: "https://github.com/tangnanbai/Andex")!) {

                        linkRow("GitHub", "chevron.left.forwardslash.chevron.right")

                    }

                    Divider().padding(.leading, 16)

                    Link(destination: URL(string: "https://github.com/tangnanbai/Andex/wiki/Privacy-Policy")!) {

                        linkRow(model.text("Privacy Policy", "隐私政策"), "hand.raised")

                    }

                }

                .background(

                    Color(uiColor: .secondarySystemGroupedBackground),

                    in: RoundedRectangle(cornerRadius: 12, style: .continuous)

                )

                Text("Copyright © Yukie")

                    .font(.footnote).foregroundStyle(.tertiary).padding(.top, 4)

            }

            .padding(.horizontal, 20).padding(.bottom, 28)

        }

        .background(Color(uiColor: .systemGroupedBackground).ignoresSafeArea())

        .navigationTitle(model.text("About", "关于"))

        .navigationBarTitleDisplayMode(.inline)

    }

    private var version: String { model.string(model.snapshot, "appVersion") }

    @ViewBuilder private var applicationIconView: some View {

        if let icon = applicationIcon {

            Image(uiImage: icon).resizable().scaledToFit()

        } else {

            Image(systemName: "app.fill").resizable().scaledToFit().foregroundStyle(.tint)

        }

    }

    private var applicationIcon: UIImage? {

        let iconDictionaries = [

            Bundle.main.object(forInfoDictionaryKey: "CFBundleIcons") as? [String: Any],

            Bundle.main.object(forInfoDictionaryKey: "CFBundleIcons~ipad") as? [String: Any],

        ].compactMap { $0 }

        for icons in iconDictionaries {

            guard let primary = icons["CFBundlePrimaryIcon"] as? [String: Any],

                  let files = primary["CFBundleIconFiles"] as? [String] else { continue }

            for name in files.reversed() {

                if let image = UIImage(named: name) { return image }

                if let path = Bundle.main.path(forResource: name, ofType: "png"),

                   let image = UIImage(contentsOfFile: path) { return image }

            }

        }

        for path in Bundle.main.paths(forResourcesOfType: "png", inDirectory: nil)

        where URL(fileURLWithPath: path).lastPathComponent.hasPrefix("AppIcon") {

            if let image = UIImage(contentsOfFile: path) { return image }

        }

        return nil

    }

    private func aboutRow(_ label: String, _ value: String) -> some View {

        HStack { Text(label); Spacer(); Text(value).foregroundStyle(.secondary) }.padding(16)

    }

    private func linkRow(_ label: String, _ icon: String) -> some View {

        HStack(spacing: 12) {

            Image(systemName: icon).frame(width: 22).foregroundStyle(.tint)

            Text(label).foregroundStyle(.primary)

            Spacer()

            Image(systemName: "arrow.up.right").font(.caption).foregroundStyle(.tertiary)

        }.padding(16)

    }

}



private struct NativeExtensionSettingsPage: View {

    @ObservedObject var model: NativeSettingsModel

    let page: [String: Any]



    var body: some View {

        Group {

            let categories = page["categories"] as? [[String: Any]] ?? []

            let visibleCategories = categories.filter { !model.bool($0, "hidden") }

            if categories.isEmpty {

                NativeExtensionForm(

                    model: model,

                    page: page,

                    sections: page["sections"] as? [[String: Any]] ?? [],

                    categories: categories

                )

            } else if !(page["sections"] as? [[String: Any]] ?? []).isEmpty {

                NativeExtensionForm(

                    model: model,

                    page: page,

                    sections: page["sections"] as? [[String: Any]] ?? [],

                    categories: categories,

                    showsCategoryNavigation: true

                )

            } else {

                List(visibleCategories, id: \.nativeID) { category in

                    NavigationLink {

                        NativeExtensionCategoryPage(model: model, page: page, category: category)

                    } label: {

                        VStack(alignment: .leading, spacing: 3) {

                            Text(model.string(category, "title"))

                            Text(model.string(category, "subtitle")).font(.caption).foregroundStyle(.secondary)

                        }

                    }

                }

            }

        }

        .navigationTitle(model.string(page, "title"))

        .navigationBarTitleDisplayMode(.inline)

        .toolbar {

            if let category = extensionCategory(page, id: model.string(page, "trailingCategory")) {

                ToolbarItem(placement: .primaryAction) {

                    NavigationLink {

                        NativeExtensionCategoryPage(model: model, page: page, category: category)

                    } label: {

                        Image(systemName: nativeSymbol(model.string(page, "trailingIcon"), fallback: "chevron.right"))

                    }

                }

            } else if !model.string(page, "trailingAction").isEmpty {

                ToolbarItem(placement: .primaryAction) {

                    Button {

                        model.perform("extension_action", [

                            "extension_id": model.string(page, "extensionId"),

                            "action": model.string(page, "trailingAction"),

                            "args": page["trailingArgs"] as? [String: Any] ?? [:],

                        ])

                    } label: { Image(systemName: nativeSymbol(model.string(page, "trailingIcon"), fallback: "checkmark")) }

                }

            }

        }

    }

}



private struct NativeExtensionCategoryPage: View {

    @ObservedObject var model: NativeSettingsModel

    let page: [String: Any]

    let category: [String: Any]



    var body: some View {

        NativeExtensionForm(

            model: model,

            page: page,

            sections: category["sections"] as? [[String: Any]] ?? [],

            categories: page["categories"] as? [[String: Any]] ?? []

        )

        .navigationTitle(model.string(category, "title"))

        .navigationBarTitleDisplayMode(.inline)

        .toolbar {

            let trailingCategory = model.string(category, "trailingCategory")

            let trailingAction = model.string(category, "trailingAction")

            if let destination = extensionCategory(page, id: trailingCategory) {

                ToolbarItem(placement: .primaryAction) {

                    NavigationLink {

                        NativeExtensionCategoryPage(model: model, page: page, category: destination)

                    } label: {

                        Image(systemName: nativeSymbol(model.string(category, "trailingIcon"), fallback: "chevron.right"))

                    }

                }

            } else if !trailingAction.isEmpty {

                ToolbarItem(placement: .primaryAction) {

                    Button {

                        model.perform("extension_action", [

                            "extension_id": model.string(page, "extensionId"),

                            "action": trailingAction,

                            "args": category["trailingArgs"] as? [String: Any] ?? [:],

                        ])

                    } label: {

                        Image(systemName: nativeSymbol(model.string(category, "trailingIcon"), fallback: "checkmark"))

                    }

                }

            }

        }

    }

}



private struct NativeExtensionForm: View {

    @ObservedObject var model: NativeSettingsModel

    let page: [String: Any]

    let sections: [[String: Any]]

    let categories: [[String: Any]]

    var showsCategoryNavigation = false



    var body: some View {

        NativeAndexForm(model: model) {

            ForEach(sections, id: \.nativeID) { section in

                Section {

                    ForEach(section["settings"] as? [[String: Any]] ?? [], id: \.nativeID) { setting in

                        NativeExtensionControl(

                            model: model,

                            page: page,

                            setting: setting,

                            categories: categories

                        )

                    }

                } header: {

                    Text(model.string(section, "title"))

                } footer: {

                    Text(model.string(section, "description"))

                }

            }

            if showsCategoryNavigation {

                Section {

                    ForEach(categories.filter { !model.bool($0, "hidden") }, id: \.nativeID) { category in

                        NavigationLink {

                            NativeExtensionCategoryPage(model: model, page: page, category: category)

                        } label: {

                            VStack(alignment: .leading, spacing: 3) {

                                Text(model.string(category, "title"))

                                let subtitle = model.string(category, "subtitle")

                                if !subtitle.isEmpty {

                                    Text(subtitle).font(.caption).foregroundStyle(.secondary)

                                }

                            }

                        }

                    }

                }

            }

        }

    }

}



private struct NativeExtensionControl: View {

    @ObservedObject var model: NativeSettingsModel

    let page: [String: Any]

    let setting: [String: Any]

    let categories: [[String: Any]]

    @State private var textValue = ""

    @State private var numberValue = 0.0



    var body: some View {

        let type = model.string(setting, "type", fallback: "text")

        switch type {

        case "toggle":

            Toggle(label, isOn: Binding(

                get: { model.bool(setting, "value") },

                set: { update($0) }

            ))

            .disabled(!model.bool(setting, "enabled", fallback: true))

        case "select", "dropdown":

            Picker(label, selection: Binding(

                get: { model.string(setting, "value") },

                set: { update($0) }

            )) {

                ForEach(options, id: \.nativeID) { option in

                    Text(model.string(option, "label", fallback: model.string(option, "value")))

                        .tag(model.string(option, "value"))

                }

            }

        case "segmented", "tab", "tabs":

            Picker(label, selection: Binding(

                get: { model.string(setting, "value") },

                set: { update($0) }

            )) {

                ForEach(options, id: \.nativeID) { option in

                    Text(model.string(option, "label", fallback: model.string(option, "value")))

                        .tag(model.string(option, "value"))

                }

            }.pickerStyle(.segmented)

        case "choice", "radio":

            Button {

                let selected = model.bool(setting, "selected") || model.bool(setting, "value")

                performAction(

                    setting,

                    fallback: "settings:\(model.string(page, "settingsId")):\(model.string(setting, "id"))",

                    fallbackArgs: ["setting": model.string(setting, "id"), "value": !selected]

                )

            } label: {

                HStack {

                    VStack(alignment: .leading, spacing: 3) {

                        Text(label).foregroundStyle(.primary)

                        if !subtitle.isEmpty { Text(subtitle).font(.caption).foregroundStyle(.secondary) }

                    }

                    Spacer()

                    if model.bool(setting, "selected") || model.bool(setting, "value") {

                        Image(systemName: "checkmark.circle.fill").foregroundStyle(.tint)

                    } else {

                        Image(systemName: "circle").foregroundStyle(.secondary)

                    }

                }

            }

            .disabled(!model.bool(setting, "enabled", fallback: true))

        case "slider":

            VStack(alignment: .leading) {

                LabeledContent(label, value: numberValue.formatted())

                Slider(value: $numberValue, in: minimum...maximum, step: step) { editing in

                    if !editing { update(numberValue) }

                }

            }

            .onAppear { numberValue = (setting["value"] as? NSNumber)?.doubleValue ?? minimum }

        case "button":

            categoryLinkOrButton(setting, label: label)

        case "link":

            if let category = category(for: setting) {

                NavigationLink(label) {

                    NativeExtensionCategoryPage(model: model, page: page, category: category)

                }

                .disabled(!model.bool(setting, "enabled", fallback: true))

            } else if let url = URL(string: model.string(setting, "url")), !model.string(setting, "url").isEmpty {

                Link(destination: url) { Label(label, systemImage: "link") }

                    .disabled(!model.bool(setting, "enabled", fallback: true))

            } else {

                Button(label) { action(setting) }

                    .disabled(!model.bool(setting, "enabled", fallback: true))

            }

        case "action-row", "chips":

            ForEach(setting["actions"] as? [[String: Any]] ?? [], id: \.nativeID) { item in

                categoryLinkOrButton(item, label: model.string(item, "label"))

            }

        case "item-card", "card":

            NativeExtensionItemCard(

                model: model,

                page: page,

                setting: setting,

                categories: categories

            )

        case "empty-state":

            VStack(alignment: .center, spacing: 10) {

                Image(systemName: nativeSymbol(model.string(setting, "icon"), fallback: "tray"))

                    .font(.title2)

                    .foregroundStyle(.secondary)

                if !label.isEmpty { Text(label).font(.headline) }

                if !subtitle.isEmpty {

                    Text(subtitle).font(.subheadline).foregroundStyle(.secondary).multilineTextAlignment(.center)

                }

                categoryLinkOrButton(

                    setting,

                    label: model.string(setting, "buttonLabel", fallback: model.text("Add", "添加"))

                )

                .buttonStyle(.borderedProminent)

            }

            .frame(maxWidth: .infinity)

            .padding(.vertical, 12)

        case "detail-line", "key-value":

            LabeledContent(label, value: model.string(setting, "value", fallback: subtitle))

        case "result-card", "callout":

            VStack(alignment: .leading, spacing: 4) {

                if !label.isEmpty { Text(label).font(.headline) }

                Text(model.string(setting, "text", fallback: model.string(setting, "value"))).foregroundStyle(.secondary)

            }

        case "divider":

            Divider()

        case "spacer":

            Color.clear.frame(height: CGFloat(model.int(setting, "size", fallback: 8)))

        case "label", "pill", "badge":

            Text(label).foregroundStyle(.secondary)

        default:

            VStack(alignment: .leading, spacing: 6) {

                if type == "password" || model.bool(setting, "secret") {

                    SecureField(label, text: $textValue)

                } else if type == "textarea" || model.bool(setting, "multiline") {

                    Text(label).font(.caption).foregroundStyle(.secondary)

                    TextEditor(text: $textValue).frame(minHeight: 110)

                } else {

                    TextField(label, text: $textValue)

                        .keyboardType(type == "number" ? .numberPad : .default)

                }

                if !subtitle.isEmpty { Text(subtitle).font(.caption).foregroundStyle(.secondary) }

            }

            .onAppear { textValue = model.string(setting, "value") }

            .onDisappear { update(type == "number" ? (Double(textValue) ?? 0) : textValue) }

        }

    }



    private var label: String { model.string(setting, "label", fallback: model.string(setting, "title")) }

    private var subtitle: String { model.string(setting, "description", fallback: model.string(setting, "subtitle")) }

    private var options: [[String: Any]] { setting["options"] as? [[String: Any]] ?? [] }

    private var minimum: Double { (setting["min"] as? NSNumber)?.doubleValue ?? 0 }

    private var maximum: Double { max((setting["max"] as? NSNumber)?.doubleValue ?? 1, minimum + 0.0001) }

    private var step: Double { max((setting["step"] as? NSNumber)?.doubleValue ?? 0.01, 0.0001) }



    private func update(_ value: Any) {

        model.perform("extension_setting", [

            "extension_id": model.string(page, "extensionId"),

            "settings_id": model.string(page, "settingsId"),

            "setting_id": model.string(setting, "id"),

            "value": value,

        ])

    }



    private func action(_ item: [String: Any]) {

        performAction(

            item,

            fallback: "settings:\(model.string(page, "settingsId")):\(model.string(setting, "id"))"

        )

    }



    private func performAction(

        _ item: [String: Any],

        fallback: String,

        fallbackArgs: [String: Any] = [:]

    ) {

        model.perform("extension_action", [

            "extension_id": model.string(page, "extensionId"),

            "action": model.string(item, "action", fallback: fallback),

            "args": item["args"] as? [String: Any] ?? fallbackArgs,

        ])

    }



    private func category(for item: [String: Any]) -> [String: Any]? {

        extensionCategory(page, id: model.string(item, "category"))

    }



    @ViewBuilder

    private func categoryLinkOrButton(_ item: [String: Any], label: String) -> some View {

        if let category = category(for: item) {

            NavigationLink(label) {

                NativeExtensionCategoryPage(model: model, page: page, category: category)

            }

            .disabled(!model.bool(item, "enabled", fallback: true))

        } else {

            Button(label) { action(item) }

                .disabled(!model.bool(item, "enabled", fallback: true))

        }

    }

}



private struct NativeExtensionItemCard: View {

    @ObservedObject var model: NativeSettingsModel

    let page: [String: Any]

    let setting: [String: Any]

    let categories: [[String: Any]]

    @State private var expanded: Bool



    init(

        model: NativeSettingsModel,

        page: [String: Any],

        setting: [String: Any],

        categories: [[String: Any]]

    ) {

        self.model = model

        self.page = page

        self.setting = setting

        self.categories = categories

        _expanded = State(initialValue: model.bool(setting, "expanded"))

    }



    var body: some View {

        DisclosureGroup(isExpanded: $expanded) {

            if hasToggle {

                Toggle(model.text("Enabled", "已启用"), isOn: Binding(

                    get: { model.bool(setting, "checked", fallback: model.bool(setting, "value", fallback: true)) },

                    set: { enabled in

                        perform(

                            model.string(setting, "toggleAction", fallback: defaultAction),

                            ["setting": settingID, "value": enabled, "checked": enabled]

                        )

                    }

                ))

            }

            ForEach(setting["actions"] as? [[String: Any]] ?? [], id: \.nativeID) { item in

                actionLinkOrButton(item)

            }

            ForEach(setting["details"] as? [[String: Any]] ?? [], id: \.nativeID) { detail in

                LabeledContent(model.string(detail, "label"), value: model.string(detail, "value"))

            }

            let result = model.string(

                setting,

                "resultText",

                fallback: model.string(setting, "result", fallback: model.string(setting, "status"))

            )

            if !result.isEmpty {

                Text(result).font(.caption).foregroundStyle(.secondary).textSelection(.enabled)

            }

            ForEach(setting["settings"] as? [[String: Any]] ?? [], id: \.nativeID) { child in

                NativeExtensionControl(model: model, page: page, setting: child, categories: categories)

            }

        } label: {

            VStack(alignment: .leading, spacing: 3) {

                Text(model.string(setting, "title", fallback: model.string(setting, "label")))

                let subtitle = model.string(setting, "subtitle", fallback: model.string(setting, "tag"))

                if !subtitle.isEmpty { Text(subtitle).font(.caption).foregroundStyle(.secondary) }

                let pill = model.string(setting, "pill", fallback: model.string(setting, "badge"))

                if !pill.isEmpty { Text(pill).font(.caption2).foregroundStyle(.secondary) }

            }

        }

        .swipeActions {

            if !model.string(setting, "deleteAction").isEmpty {

                Button(role: .destructive) {

                    perform(

                        model.string(setting, "deleteAction"),

                        setting["deleteArgs"] as? [String: Any] ?? [:]

                    )

                } label: {

                    Label(model.text("Delete", "删除"), systemImage: "trash")

                }

            }

            if let category = extensionCategory(page, id: model.string(setting, "editCategory")) {

                NavigationLink {

                    NativeExtensionCategoryPage(model: model, page: page, category: category)

                } label: {

                    Label(model.text("Edit", "编辑"), systemImage: "pencil")

                }

                .tint(.blue)

            } else if !model.string(setting, "editAction").isEmpty {

                Button {

                    perform(

                        model.string(setting, "editAction"),

                        setting["editArgs"] as? [String: Any] ?? [:]

                    )

                } label: {

                    Label(model.text("Edit", "编辑"), systemImage: "pencil")

                }

                .tint(.blue)

            }

        }

    }



    private var settingID: String { model.string(setting, "id") }

    private var defaultAction: String { "settings:\(model.string(page, "settingsId")):\(settingID)" }

    private var hasToggle: Bool { setting["checked"] != nil || !model.string(setting, "toggleAction").isEmpty }



    @ViewBuilder

    private func actionLinkOrButton(_ item: [String: Any]) -> some View {

        let label = model.string(item, "label")

        if let category = extensionCategory(page, id: model.string(item, "category")) {

            NavigationLink(label) {

                NativeExtensionCategoryPage(model: model, page: page, category: category)

            }

            .disabled(!model.bool(item, "enabled", fallback: true))

        } else {

            Button(label) {

                perform(

                    model.string(item, "action"),

                    item["args"] as? [String: Any] ?? [:]

                )

            }

            .disabled(!model.bool(item, "enabled", fallback: true))

        }

    }



    private func perform(_ action: String, _ args: [String: Any]) {

        guard !action.isEmpty else { return }

        model.perform("extension_action", [

            "extension_id": model.string(page, "extensionId"),

            "action": action,

            "args": args,

        ])

    }

}



private extension Dictionary where Key == String, Value == Any {

    var nativeID: String {

        (self["id"] as? String)

            ?? (self["value"] as? String)

            ?? (self["title"] as? String)

            ?? (self["label"] as? String)

            ?? ((try? JSONSerialization.data(withJSONObject: self, options: [.sortedKeys]))

                .flatMap { String(data: $0, encoding: .utf8) })

            ?? "item"

    }

}



private func extensionCategory(_ page: [String: Any], id: String) -> [String: Any]? {

    guard !id.isEmpty else { return nil }

    return (page["categories"] as? [[String: Any]] ?? []).first {

        ($0["id"] as? String) == id

    }

}



private func nativeSymbol(_ value: String, fallback: String) -> String {

    switch value.lowercased() {

    case "settings", "tune": "slider.horizontal.3"

    case "search", "globe", "public": "globe"

    case "key", "lock": "key"

    case "database", "storage": "externaldrive"

    case "terminal", "code": "terminal"

    case "refresh", "reload": "arrow.clockwise"

    case "add", "plus", "new": "plus"

    case "delete", "remove", "trash": "trash"

    case "edit": "pencil"

    case "check", "save", "done": "checkmark"

    default: fallback

    }

}
