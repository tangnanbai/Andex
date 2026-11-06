# 豆包输入法非官方 ASR

0.99 默认启用在线语音输入，不需要最终用户安装 Python，不集成输入法 UI，也不使用火山付费录音文件接口。旧火山 ASR 地址、App ID、API Key 和 Access Token 会在读取设置时迁移清除；模型提供方配置不受影响。

## 使用

通用设置 → 语音输入可检测连接或关闭。键盘收起时按住对话输入框说话；按住期间流式上传，松手发送结束信号，等待最终完整文本后一次性填入草稿。中间文本只在提示中预览。上滑取消、切到后台或页面销毁会取消请求和录音，丢弃迟到回调。键盘打开后长按保留系统粘贴行为。单次上限 60 秒。

录音直接从设备发送到在线服务，没有中转后端，不默认长期保存原始音频。非官方接入，可用性和额度由服务端决定。访问拒绝或限流时显示错误并冷却，不循环重试，不重新注册或轮换设备身份绕过限制。

## 参考来源与许可

2026-11-06 核对：

| 仓库 | 分支 | 提交 |
| --- | --- | --- |
| yangmoling/doubaoime-asr | main | 267972f815f519fd7c6149f85a8b7cc99daf61a5 |
| melody0709/VoxType | cpp-integration | 3c5ca9c497b7cac814d794c846f83fbed03e5472 |

主要参考的 README 安装命令仍指向 starccy/doubaoime-asr；实际访问该旧地址返回 Repository not found，验证使用的是上述 yangmoling 仓库。参考了实际 ASRConfig、transcribe、transcribe_stream、transcribe_realtime、实时麦克风示例，以及 VoxType 的 doubao_ime 后端，与其火山付费后端区分。

检查时两个参考仓库根目录均未找到明确 LICENSE，GitHub 元数据为 license:null。公开可访问不代表可任意复用或商用。本项目没有复制其客户端实现，使用独立编写的 Kotlin 协议客户端；参考项目的许可和相关服务使用条件仍需由分发者核对。

Android Opus 编码依赖 Maven `io.github.jaredmdobson:concentus:1.0.2`，为纯 Java 实现，不新增 Python 或 native ABI。其 BSD 式许可原文随共享资源 `files/licenses/concentus.txt` 分发。iOS 使用系统 AVAudioConverter 和 AVAudioEngine。

## 已核对的协议

具体字段来自参考源码，不套用火山官方付费协议：

| 项目 | 实现 |
| --- | --- |
| 注册 | POST `https://log.snssdk.com/service/2/device_register/`；先保存固定身份，成功后立即保存 device_id |
| 认证 | POST `https://is.snssdk.com/service/settings/v3/`；body=`body=null`；x-ss-stub 为该 body 的 MD5 大写；读取 data.settings.asr_config.app_key |
| 应用参数 | aid=401734，app_name=oime，version_code=100102018，version_name=1.1.2 |
| 连接 | `wss://frontier-audio-ime-ws.doubao.com/ocean/api/v1/ws?aid=401734&device_id=...` |
| 请求头 | 参考客户端 User-Agent、proto-version:v2、x-custom-keepalive:true |
| 采集 | PCM signed16 little endian、16000Hz、单声道 |
| 网络音频 | Opus；20ms 一帧，即 320 个样本、640 字节 PCM 编为一个 Opus 包 |
| Protobuf 请求 | 2=token，3=service_name，5=method_name，6=payload JSON，7=audio_data，8=request_id，9=frame_state |
| Protobuf 响应 | 4=message_type，5=status_code，6=status_message，7=result_json；不向日志输出原始认证响应 |
| 会话 | StartTask → TaskStarted → StartSession → SessionStarted → TaskRequest 音频 → LAST → FinishSession → SessionFinished |
| 音频状态 | FIRST=1、MIDDLE=3、LAST=9；最后残帧补零 |
| 最终结果 | results.extra.nonstream_result，或 is_interim=false 且 is_vad_finished=true；必须等 SessionFinished 后提交 |

StartSession 的 audio_info 为 channel=1、format=speech_opus、sample_rate=16000；启用标点，extra 包含固定设备 ID、app_name=com.android.andex、enable_asr_threepass、enable_asr_twopass、input_mode=tool。中间结果是当前分句替换，不直接追加；多个确定分句累计并去除前后缀重叠，重复最终事件不重复写入。

服务器的 TaskFailed/SessionFailed、无语音、连接中断、15 秒响应超时均有明确错误。错误保留 StartTask / StartSession / Recognition 阶段、业务码及脱敏后的 status_message，不只显示“被拒绝”。401/403 认证失败会保留设备身份并清除旧 token；后续手动重试仅用同一设备获取 token。不会把 partial 结果当作超时成功。

设置检测与对话录音共用进程级会话锁，同时只允许一个在线请求；拒绝后的 60 秒冷却也由所有入口共享。取消后在不可取消的清理区内尝试 FinishSession 和 WebSocket 关闭，最多等待 1.5 秒后强制终止连接。快速再次录音会等待上一条请求释放资源；上一次的回调不会回填新草稿。

## 凭据

首次注册前持久化身份；注册和认证串行执行，多个入口不会同时注册。Android 使用 Keystore AES/GCM 加密和应用私有 SharedPreferences，并排除云备份/设备迁移；iOS 使用 ThisDeviceOnly 钥匙串。解密或存储失败会报错，避免因读取失败重新生成设备。

## 实际验证

短中文音频为公开仓库 Chinese-speech-to-text 的 `1.wav` 前 8 秒，转换为 16kHz 单声道 16bit PCM。测试音频、凭据及本地测试结果保存在工程之外，不进 Git。

- 参考 Python 客户端：首次注册、认证、识别成功，耗时 14,140ms；新进程复用缓存成功，12,960ms。均包含按实时速率发送音频的时间。
- 独立 Kotlin 客户端 + Concentus：真实流式发送，一次成功记录为 16,439ms，12 个预览事件，复用同一缓存凭据，没有重新注册。
- 实际最终文本：`1993 年 2 月 23 日上午，四川省安岳县安源乡五村彭家姑嫂 5 人进城购置衣服。`
- 连接检测发送静音，只确认连接与鉴权，不等同于完成真实语音识别。
- Android 麦克风和 UI 验证结果见 0.99 交付说明；iOS 在 Windows 环境仅能静态检查，未经 Xcode 构建或真机验证。

### 拒绝问题排查

2026-11-06 的一次 Kotlin 联网回归在 StartSession 阶段收到 `40200011`，在开始上传音频之前失败。当时解析器没有保留 protobuf 字段 6，无法从该历史响应确认完整服务端原因；不能仅凭这个业务码宣称是密钥失效或额度用完。

随后使用同一份缓存凭据、相同 Andex 会话参数做了一次参考客户端诊断，StartTask、StartSession、SessionFinished 均返回 `20000000 / OK`，13,542ms 后获得相同完整中文。没有注册新设备、刷新或更换 token。这确认当次诊断中旧凭据仍可用，但不能反向证明历史拒绝原因。

对照辅助项目并审查客户端后，修复了可能造成会话占用的取消清理缺口、不同入口可同时连接和错误详情丢失。辅助项目中“被拒后换身份”的方案未采用。修复后的取消再录测试结果见 0.99 交付说明。

修复后的 Kotlin 联网回归已通过：同一客户端启动会话后取消，随后用原设备和原 token 重新识别 8 秒短音频；收到 12 个中间事件并返回上述完整中文，含取消清理在内共 10,829ms。协议、互斥、冷却、取消清理、整句回填与真实网络共 14 项定向测试全部通过。这是客户端直连验证，不能代替手机麦克风录音和 UI 实测。

## 开发验证

```powershell
.\gradlew.bat :shared:testDebugUnitTest :app:testDebugUnitTest :app:assembleOptimized
```

可选真实网络测试需设置 `ANDEX_ASR_SMOKE_DIR`，目录内提供外部 `zh.pcm` 和私有 `private/credentials.json`。测试 `DoubaoImeLiveTest` 不在仓库保存凭据，未设置时跳过。常规测试覆盖整句回填、停止与取消分离、迟到回调、重复启动、权限拒绝、失败资源释放、配置迁移和协议解析。

另设 `ANDEX_ASR_CANCEL_SMOKE=1` 时，同一测试会先启动一次会话并取消，等待资源释放，再用同一设备与 token 识别短音频。遇到拒绝立即失败并保留原因，不轮换身份或循环重试。
