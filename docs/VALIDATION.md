# 本地验证记录

日期：2026-10-02（Asia/Shanghai）。Java 为 Zulu OpenJDK 17.0.15；Gradle 8.8，ForgeGradle 6.0.54，官方映射，Windows 64 位。所有依赖缓存在项目 `.tools/`，构建脚本不改变系统 Java 设置。

## 已完成

| 验证 | 结果 |
| --- | --- |
| JUnit 核心逻辑 | 16 项全部通过 |
| Forge 1.18.2 / 40.3.12 | 编译、重混淆打包成功；12 个注册 GameTests 通过，可选输出测试在无流体模式跳过内部断言 |
| Forge 1.19.2 / 43.5.2 | 编译、重混淆打包成功；12 个 GameTests 全部通过，启用 both 测试流体 |
| Forge 1.20.1 / 47.4.23 | 编译、重混淆打包成功；12 个 GameTests 全部通过，启用 sophisticated 测试流体 |
| 发行包检查 | 三个 jar 的元数据、语言键、16 个方块状态、8 张纹理、配方及战利品、客户端及服务器类完整；不包含测试类、测试结构或原版美术/音效文件 |
| MIT 发行检查 | 三个 jar 的 `mods.toml` 标记 MIT，并包含与仓库相同的 LICENSE 文本；重新构建成功 |
| CI 与发布脚本 | actionlint 1.7.12 和 Bash 语法检查通过；7 项发布测试通过，覆盖新建、草稿恢复、公开版本重跑、上传失败、校验失败和缺少凭据 |
| 美术检查 | 已离线渲染检查关闭/开启、启用/停用模型和 GUI 排版，预览位于 `docs/preview.png` |

核心测试覆盖物品及液体余数跨次累计、持久化、非整除比例、缓存溢出、配置缩容、整数溢出、原生液态经验单位、模拟流体输出不改变缓存、1 mB 小额输出、整点提取保留小数、FIFO 时序与全部红石状态。

真实 Forge 测试服务器覆盖：

1. 手动满仓插入拒绝，不销毁物品。
2. 多次模拟满仓插入不产生副作用，真实插入回收最早组。
3. 部分堆叠优先合并，不回收无关物品。
4. **原版漏斗**确实向满仓垃圾桶输入新物品，并回收最早组。
5. 自动化经验溢出不产生经验球。
6. 不同液体输入、模拟填充和转换余数累计。
7. 测试流体的输出优先级、原生倍率、1 mB 精确扣除、模拟无副作用和红石门控。
8. 红石切换会立即约束已经缓存的物品/液体能力，手动操作可用。
9. 保存/重载保持库存、FIFO 顺序、经验、余数和模式。
10. 999999 点经验菜单数据不被 16 位字段截断，未知/远程按钮操作被拒绝。
11. 手动溢出与整点提取的经验球总值守恒，重复提取不会复制经验。
12. 销毁方块掉落内部物品、垃圾桶及经验，重复移除不重复释放。

实际漏斗测试发现 Forge 会在满仓时提前拒绝插入；最终实现增加不保存物品的虚拟自动化投入口，解决该兼容问题。实际库存和 GUI 仍为 27 格。自动化能力多一个入口槽位的约定见维护文档。

## 验证边界

可选流体测试使用本项目原创的测试专用流体，分别注册用户指定的原生流体 ID，验证注册表检测和标准 Forge `IFluidHandler` 行为。原生单位另外核对了两个 Mod 官方仓库中的目标版本代码/兼容配方。没有把第三方源码或美术复制到项目中，也没有安装并运行完整 Create、CEI、Sophisticated Core、AE2、Mekanism 整合包；各自管道/总线的实机组合测试仍需在目标整合包进行。

美术预览是模型和 GUI UV 的离线校样；未启动图形客户端进行操作测试，也未穷举第三方资源包。NeoForge 的新增服务器验证记录见下文；未验证旧 Forge 世界直接升级到 1.21.1 的整存档迁移。

`.github/workflows/build.yml` 在分支推送和 Pull Request 上执行四个版本的构建、核心测试和三种流体环境下的服务器测试，汇总 jar 并检查资源和 MIT 许可证。tag 推送检查通过后发布 GitHub Release。发布异常与重跑行为在本地通过模拟 GitHub CLI 验证，不会创建真实 Release。

本文记录本地验证结果；云端 CI 结果以 [GitHub Actions](https://github.com/Neonscape/trashbin-mod/actions) 的对应运行记录为准。

## NeoForge 1.21.1 新增验证（2026-10-31）

使用 Zulu OpenJDK 21.0.8、Gradle 8.8、ModDevGradle 2.0.148 与 NeoForge 21.1.265。无需额外前置 Mod，业务规则复用原有 core。

- 无可选经验流体、仅精妙核心流体、精妙核心与 CEI 流体同时存在，三种服务器环境各 16 项 GameTests 全部通过。无流体环境跳过可选输出测试的内部断言；另两种环境实际断言输出优先级、原生倍率、小额精度、模拟操作和红石门控。
- 原有 12 项行为回归保持通过；新增 4 项验证：物品数据组件保存/加载及防止错误合并、替换组件后的 FIFO 年龄、实际配方与镐标签加载、方块移除后拒绝旧能力引用的输入。
- 发布脚本 7 项测试通过，附件场景扩展为三个 Forge jar 加一个 NeoForge jar；actionlint 1.7.12 静态检查通过。
- 在同一工程以 Java 21 启动 Gradle、显式选择 Java 17/21 toolchain，四个 `0.2.0` jar 统一构建成功；发行检查通过，确认版本号、Java 目标、MIT 许可证、现代数据路径、模型/语言/纹理完整，并且 NeoForge jar 不引用 Forge API、不包含测试代码。
- 客户端 GUI 已编译，并核对 1.21.1 父类的背景绘制调用，避免重复调用背景/模糊。尚未进行图形客户端操作测试和完整第三方整合包联调。

## NeoForge 最低版本下调验证（2026-11-01）

构建与最低依赖基线下调至 NeoForge `21.1.249`，仍使用 Java 21 与 Minecraft 1.21.1。无可选流体、仅精妙核心流体、精妙核心与 CEI 流体同时存在，三个真实服务器环境各 16 项 GameTests 全部通过。

构建版本和 `neoforge.mods.toml` 的最低版本统一来自 `gradle.properties` 的 `neoforge_version`，发行检查会拒绝两者不一致的 jar。原有 21.1.265 的测试记录保留在上一节。

## 复现

```powershell
.\scripts\build.ps1
python scripts/verify_jars.py
.\scripts\build.ps1 -Minecraft 1.18.2 -Task :forge-1.18.2:runGameTestServer -GameTests
.\scripts\build.ps1 -Minecraft 1.19.2 -Task :forge-1.19.2:runGameTestServer -GameTests -Compatibility both
.\scripts\build.ps1 -Minecraft 1.20.1 -Task :forge-1.20.1:runGameTestServer -GameTests -Compatibility sophisticated
.\scripts\build.ps1 -Minecraft 1.21.1 -Task :neoforge-1.21.1:runGameTestServer -GameTests
.\scripts\build.ps1 -Minecraft 1.21.1 -Task :neoforge-1.21.1:runGameTestServer -GameTests -Compatibility sophisticated
.\scripts\build.ps1 -Minecraft 1.21.1 -Task :neoforge-1.21.1:runGameTestServer -GameTests -Compatibility both
```

测试日志位于各版本的 `run/gametest-<环境>/logs/latest.log`，JUnit 报告位于 `core/build/reports/tests/test/index.html`。最终安装包位于 `dist/`，其 SHA-256 摘要由 `scripts/verify_jars.py` 写入 `dist/SHA256SUMS.txt`。
