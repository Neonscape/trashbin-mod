# 维护与扩展

## 结构

| 目录 | 职责 |
| --- | --- |
| `core/src/main/java` | 无 Minecraft/Forge 依赖的整数经验账本、到达顺序、红石真值表 |
| `core/src/test/java` | JUnit 数值、精度、边界和持久化测试 |
| `common/src/main/java` | Forge 注册、方块、方块实体、标准能力、服务端菜单和公共 UI 内容 |
| `legacy-client/src/main/java` | 1.18.2/1.19.2 的 PoseStack GUI |
| `versions/forge-*/src/main/java` | 注册表、创造分类、文本、方块属性、菜单打开适配；1.20.1 GuiGraphics GUI |
| `versions/neoforge-1.21.1/src/main/java` | NeoForge 原生注册、能力、组件 NBT、菜单与 GUI；复用 core 与 ScreenContent |
| `common/src/main/resources` | 四个版本共用的模型、原创纹理、翻译、配方、战利品与工具标签 |
| `tests/gametest` | 显式启用的真实服务器集成测试及最小结构，不进入发行 jar |
| `tests/neoforge` | NeoForge 服务器回归测试，额外覆盖物品组件与 1.21.1 数据路径，不进入发行 jar |
| `scripts` | 本地构建、可重建资产、预览与发行检查 |

ForgeGradle、ModDevGradle 和 Gradle Wrapper 均固定版本；游戏使用 Mojang 官方映射。Forge 编译目标为 Java 17；NeoForge 为 Java 21。发行前执行四个版本构建和 GameTests，检查 `dist/` jar。Forge 的 `reobfJar` 已绑定到打包流程；NeoForge 1.21.1 使用原生开发映射，不执行 Forge 的重混淆任务。

## 状态与存档

方块状态只有 `facing`、`active`、`open`。菜单通过原版菜单协议同步经验点数、容量、模式、启用状态、输出类型、占用槽位和小数。点数/容量拆成两个 16 位字段，避免超过 32767 后显示异常。

方块实体 NBT schema 版本为 `DataVersion=1`：

| 字段 | 内容 |
| --- | --- |
| `Inventory` | 对应加载器的 ItemStackHandler，固定 27 槽 |
| `ArrivalOrder` | 每个槽位的单调到达序号；合并不更新 |
| `ArrivalGameTime` | 每组首次放入的世界游戏时间 |
| `ExperienceUnits` | 以 1/20 经验点为单位的缓存 |
| `ItemRemainder` | 尚未达到一整点的物品数量 |
| `FluidRemainder` | 尚未达到一整点的液体 mB 数 |
| `RedstoneMode` | 0 无、1 充能启用、2 未充能启用 |

缓存、余数、到达顺序、模式均随区块保存。所有真实改变调用 `setChanged`；槽位的原地堆叠修改也显式通知方块实体。开盖观众数不存档，服务器每秒按当前菜单重新核对，清理断线留下的开盖状态。

未来变更存档格式时读取 `DataVersion` 并进行显式迁移；保留旧版本备份世界用于迁移回归，不能单纯重命名已有 NBT 键。缓存容量降低采用丢弃超出部分的策略，不生成自动经验球。

## 自动化接口约定

GUI 访问普通 `ItemStackHandler`，自动化访问独立 `IItemHandler` 包装器。实际储物始终为 27 槽，自动化额外看到索引 27 的虚拟投入口，共 28 个能力槽位；模拟调用绝不改变库存、余数、时间或经验。

前 27 槽执行普通插入/提取；虚拟投入口读取始终为空，插入的物品即时移入实际库存，满仓时回收最早的一组，不在投入口中积存。该设计使 Forge 漏斗的满仓预检与物品类型预检均能通过，同时维持真实库存视图。标准遍历插入的管道会自然使用投入口；仅绑定指定槽位的设备可直接绑定虚拟投入口。投入口不能提取物品，也不会增加 GUI 容量。

流体 tank 0 为即时销毁输入，读取始终为空，容量 `Integer.MAX_VALUE`。检测到可选经验流体时，tank 1 为虚拟输出，容量和余量对应经验缓存。输出无可选 Mod 的编译依赖，不反射内部类。`fill` 与两种 `drain` 重载均实时检查红石；即使管道长期缓存 LazyOptional，也不能绕过门控。`invalidateCaps` / `reviveCaps` 正确管理能力生命周期。

手动动作使用原版按钮包，在服务端检查有效菜单、玩家距离、旁观者和动作编号后执行。客户端只发出意图，不提供经验数量或库存内容。

经验球按最多 32 个实体分组，经验总量精确且避免大容量提取生成数十万个实体。自动化溢出直接舍弃；方块移除只执行一次内容释放，不将内部数据写回物品。

## NeoForge 1.21.1

`gradle/neoforge.gradle` 使用 ModDevGradle 2.0.148 / NeoForge 21.1.265；旧 Forge 构建脚本独立保留。经验精度、FIFO 时序与红石真值表只在 `core` 定义；Loader 相关代码使用独立适配，避免用文本替换生成 Java 源码。修改交互规则时，两套适配的服务器测试必须同时通过。

能力由 `RegisterCapabilitiesEvent` 注册，返回稳定的物品/流体包装器，每次调用实时检查移除状态和红石。无需 LazyOptional；块被移除后的失效由 NeoForge 能力缓存处理，外部仍持有旧包装器时也不能继续操作。

物品保存使用带 `HolderLookup.Provider` 的 ItemStackHandler 序列化，比较堆叠使用 `isSameItemSameComponents`，保留自定义名称、最大堆叠数量等组件。逻辑字段仍使用 `DataVersion=1`；跨 Minecraft 大版本的整存档迁移尚未验证，当前测试验证同版本保存/加载。

资源构建时仅转换 `data/<命名空间>/` 下的类型目录：`recipe`、`loot_table`、`advancement` 和 `tags/block`，以及配方结果的 `id` 字段。嵌套资源 ID（如 `recipes/trash_bin`）保持原样；模型、材质和语言文件不复制维护。NeoForge 的 pack 元数据声明支持 34～48，涵盖 1.21.1 的资源包和数据包格式。

新增目标时，同时更新 `settings.gradle`、对应加载器构建配置、Windows JDK 选择、CI 矩阵和 `scripts/verify_jars.py`，再运行发布脚本的四附件回归测试。新增 tag 由用户决定；普通代码推送只生成构建 artifact。
