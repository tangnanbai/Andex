# Andex 内置悬浮药丸

药丸为 Andex 内置功能，只构建并安装 `:app` 一个 APK。

## 使用

设置 → Agent Mode → 悬浮药丸，授权「显示在其他应用上层」。
开关默认开启，授权前不会绘制。Android 13+ 可授予通知权限。
药丸严格只在 Agent 任务正在执行、且 Andex 不处于前台时出现。
完成✅、错误❌、已停止⏹ 显示 8 秒后自动收起，返回 Andex 或关闭开关立即隐藏。
后台任务继续使用原有任务前台服务，药丸不增加独立保活服务。

单击展开；点气泡外收回；点击气泡主体打开对应会话。
展开时透明遮罩消费第一次外部点击，窗口不夺输入焦点。
拖动药丸或气泡顶部调整位置，位置持久化，旋转后重新夹取边界。
隐藏本次任务仅隐藏当前轮次，新任务正常显示。

## 数据与实现

PillOverlayController 直接订阅 SessionExecutionManager 与 AppForegroundTracker。
没有跨 App HTTP、Binder 或额外 APK，也不暴露控制 token。
空闲不轮询；后台任务每秒刷新耗时及阶段，结束提示到期清理。
运行状态细化为搜索🔎、运行命令⚙️、浏览网页🌐、读取文件📄、
修改文件✏️、手机操作📱、处理压缩包📦 等，按实际正在执行的工具映射。
扩展确认/选择/输入请求关联会话，显示等待确认或输入。
任务总量未知时显示不定进度、耗时及工具次数，不编造百分比。
TYPE_APPLICATION_OVERLAY 窗口以 View 缩放淡入，不逐帧调整窗口尺寸。
权限撤回后清理窗口，重新授权后随下次后台任务恢复。

## 命名迁移

Android applicationId 和 namespace 为 `com.android.andex`。
本次 Debug 包也使用该 applicationId，生产发布须继续沿用相同签名证书。
iOS Bundle ID、共享框架、自有脚本及扩展协议迁移到 Andex。
更换包名后视为新应用，旧版数据须先导出再导入，系统权限重新授权。
Termux 使用 ANDEX_TERMUX_API_URL/TOKEN，自有文件在 Andex 私有目录。
官方 Termux 的 `/data/data/com.termux` 编译前缀属于第三方 ABI，
由 PRoot 映射到 Andex 私有目录，不能直接替换。
第三方源码与许可证保留原始归属。

iOS 不支持等价的跨 App 系统悬浮窗，本次同步命名，不改变聊天行为。

## 验证

OverlayPolicyTest 覆盖显示策略、轮次隐藏、边界夹取、未知进度及状态标签。
Debug 专用 OverlayCheckActivity 注入限时模拟执行状态以测试窗口，
仅持有系统 DUMP 权限的调试端可启动；不调用模型、不写聊天数据，
Release 不包含该 Activity。
