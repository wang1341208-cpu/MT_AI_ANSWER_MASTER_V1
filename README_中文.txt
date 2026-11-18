MT AI ANSWER MASTER V1.0（可直接构建版）
=====================================

【项目说明】
- Android 原生 Kotlin + Jetpack Compose
- 无障碍服务读取当前窗口可访问的文字（非截图 OCR）
- 支持配置 OpenAI Chat Completions 兼容接口
- AI 可建议：精确文字点击、输入框填入、上下滚动
- FULL 自动操作开关 + 紧急停止
- Root 仅固定只读诊断（每次弹窗确认）

【重要限制】
1. 使用 AccessibilityService，游戏/Canvas/自绘界面可能读不到文字。
2. 自动点击只支持文字完全匹配的可见控件。
3. 开启 FULL 自动操作后请勿在支付、删除、发消息等高风险页面使用。
4. 屏幕文字会发送到你配置的 AI 服务，注意隐私。
5. 本项目不绕过登录、验证码、付费或安全限制。


=====================================
【手机端构建 APK 完整步骤】（推荐）
=====================================

A. 准备 GitHub 仓库
1. 手机浏览器打开 https://github.com 并登录。
2. 点击右上角 + → New repository。
3. Repository name 填写：MT-AI-ANSWER-MASTER（或任意名字）。
4. 保持 Public，不要勾选 Add a README file。
5. 点击 Create repository。

B. 上传源码文件（两种方式任选）

方式一：直接上传整个文件夹（推荐）
1. 在仓库页面点击 "uploading an existing file"。
2. 把解压后的所有文件和文件夹（包含 .github、app、build.gradle.kts 等）全部拖进去。
3. 注意：.github 文件夹必须一起上传。
4. 提交信息写：upload source，然后 Commit changes。

方式二：用 MT 管理器 + 浏览器（如果拖拽不方便）
1. 把整个源码文件夹压缩成 zip。
2. 在 GitHub 仓库用 "Upload files" 上传这个 zip。
3. 然后在仓库里新建文件 .github/workflows/build.yml，把下面的内容粘贴进去（或者直接用仓库里已有的）。

C. 触发构建
1. 打开仓库顶部的 Actions 标签。
2. 左侧选择 "Build MT AI ANSWER MASTER APK"。
3. 右侧点击 "Run workflow" → 绿色按钮再点一次。
4. 等待几分钟，状态变成绿色勾号。
5. 点击这次运行记录，往下找到 Artifacts。
6. 下载 "MT-AI-ANSWER-MASTER-V1-debug"。
7. 解压后得到 app-debug.apk。

D. 安装
1. 把 apk 传到手机。
2. 设置 → 允许安装未知来源应用。
3. 安装后打开。
4. 点「打开无障碍设置」→ 找到「MT AI 屏幕助手」并开启。
5. 返回软件配置 AI 地址、模型、API Key 后即可使用。


=====================================
【首次使用】
1. 打开无障碍服务。
2. 切换到题目页面，回到本软件点「读取当前屏幕文字」。
3. 填写并保存 AI 配置（默认是 OpenAI 地址，可改成其他兼容服务）。
4. 点「分析屏幕并生成操作建议」。
5. 确认风险后再打开 FULL 自动点击模式。
6. 随时可点「紧急停止自动操作」。


构建产物路径：
app/build/outputs/apk/debug/app-debug.apk
