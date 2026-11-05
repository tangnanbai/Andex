<p align="center">
  <img src="app/src/main/res/drawable-nodpi/andex_mark.png" width="128" height="128" alt="Andex 图标">
</p>

<h1 align="center">Andex</h1>

<p align="center">在安卓手机上运行的 AI 助手，集成终端、手机控制和扩展能力。</p>

<p align="center">
  <a href="https://github.com/tangnanbai/Andex/releases">下载 APK</a> ·
  <a href="https://github.com/tangnanbai/Andex/issues">反馈问题</a>
</p>

## 项目介绍

Andex 基于 Aether 二次开发，将内置 Termux 环境、无障碍和 ADB 控制整合进手机端 AI 助手。你可以在对话中处理文件、运行命令、检索网页，并让 Agent 使用已授权的通道操作手机。

维护者：**Yukie**。应用包名：`com.android.andex`。

## 主要功能

- **多模型对话**：支持 OpenAI、Anthropic、Google、OpenRouter、DeepSeek、Xiaomi，以及自定义请求地址和模型 ID。
- **内置终端**：无需另外打开 Termux；支持运行命令、处理文件，集成 ADB、7zip 等工具。
- **手机控制**：结合内置终端、无障碍、ADB 通道操作手机；无障碍需要在系统设置中授权，ADB 需要先连接可用设备。
- **网页与 GitHub**：内置搜索、网页读取和 GitHub 相关技能，支持在获取的网页内容中查找信息。
- **角色定义**：支持多角色、上传头像、默认角色及提示词；在对话右上角点击头像切换。
- **上下文与记忆**：显示上下文使用情况，达到阈值时自动压缩，并提供长效记忆能力。
- **任务轨迹**：从右侧打开轨迹面板，查看 Agent 的操作步骤及工具调用详情。
- **后台进度**：任务运行时离开应用，可通过悬浮进度查看当前工作状态。
- **独立配色**：主题色与背景色分开选择，包含黑、白及 7 种参考色，点击立即生效；默认浅色粉白、深色粉黑。文字、图标和描边随主题更新，并自动调整对比度。按钮与弹窗使用轻磨砂半透明效果。
- **语音输入**：在通用设置中配置火山 ASR 后，按住输入框录音，松开识别，上滑取消；识别结果填入草稿。
- **扩展能力**：支持 Pi 扩展、技能及工具调用。

模型服务和语音识别服务需要配置自己的 API 凭据。功能实际可用范围取决于设备系统、已授予的权限和服务配置。

## 开始使用

1. 从本仓库的发布页面下载 APK，安装并打开 Andex。
2. 根据首次启动引导初始化内置环境，并授予需要使用的权限。
3. 在模型设置中选择预设服务或新增自定义地址，填写 API Key 并选择模型。
4. 如需操作手机，前往 Agent 模式开启无障碍或配置 ADB。
5. 如需语音输入，在“设置 → 通用设置 → 语音输入”中配置火山 ASR。

## 本地构建

使用 JDK 17 和 Android SDK，在项目目录执行：

```powershell
.\gradlew.bat :app:assembleDebug
```

APK 输出到 `app/build/outputs/apk/debug/app-debug.apk`。具体依赖和 SDK 版本以 Gradle 配置为准。

## 参与开发

欢迎通过本仓库提交问题或合并请求。应用更新检查指向本仓库的 Releases。

## 原项目与致谢

本项目基于 **[Zhou-Shilin/Aether](https://github.com/Zhou-Shilin/Aether)** 二次开发，感谢原作者及贡献者。

内置终端相关能力使用 [Termux](https://github.com/termux/termux-app) 及其生态项目。其他第三方组件及许可请参阅 [第三方声明](THIRD_PARTY_NOTICES.md) 和各组件许可证。

本项目许可证见 [LICENSE](LICENSE)；原项目及第三方组件的版权与许可声明予以保留。
