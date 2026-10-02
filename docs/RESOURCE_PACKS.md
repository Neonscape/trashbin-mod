# 资源包兼容

所有方块、物品和界面文本通过语言键显示。资源包可以覆盖：

```text
assets/trashbin/blockstates/trash_bin.json
assets/trashbin/models/block/trash_bin_base.json
assets/trashbin/models/block/trash_bin_active_closed.json
assets/trashbin/models/block/trash_bin_active_open.json
assets/trashbin/models/block/trash_bin_inactive_closed.json
assets/trashbin/models/block/trash_bin_inactive_open.json
assets/trashbin/models/item/trash_bin.json
assets/trashbin/textures/block/body.png
assets/trashbin/textures/block/front.png
assets/trashbin/textures/block/lid.png
assets/trashbin/textures/block/rim.png
assets/trashbin/textures/block/handle.png
assets/trashbin/textures/block/active.png
assets/trashbin/textures/block/inactive.png
assets/trashbin/textures/gui/experience_bar.png
assets/trashbin/lang/zh_cn.json
assets/trashbin/lang/en_us.json
```

方块纹理默认为 16×16，模型引用与粒子材质均为标准 JSON 路径，支持更高分辨率覆盖。模型有水平朝向、启用和开盖三种状态属性，总共 16 个组合。`active` 表示红石门控后自动化是否启用，不表示经验缓存是否有空位。

经验条布局：逻辑纹理画布为 160×32；背景位于 `(0,0)`，尺寸 160×8；填充位于 `(0,8)`，使用前 158×6 像素。替换图可按相同比例提高分辨率。填充条的六行不含边框。

槽位、容器边框和中间空白面板取自资源包的原版箱子 GUI 纹理；按钮、字体、物品渲染和提示框使用原版实现。资源包如果彻底改变原版箱子纹理布局，仍需保持原版 UV 布局，或同时提供本 Mod 的 GUI 代码适配；通用资源包无法保证任意布局变更的兼容。

离线校样脚本 `scripts/render_preview.py` 需要 Pillow，并从本地 Minecraft 客户端 jar 读取 GUI 像素。预览中的 Minecraft GUI 归 Mojang 所有，方块材质为本项目原创；该脚本不会将原版纹理复制进 Mod 资源。
