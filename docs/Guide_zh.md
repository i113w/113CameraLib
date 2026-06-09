# 113's Camera Lib 使用手册

本文档按 [i113w/113CameraLib](https://github.com/i113w/113CameraLib) 当前源码重写，适用于 `main` 分支。

主分支是 NeoForge 1.21.1 版本。Forge 1.20.1 版本位于同一仓库的 `forge-1.20.1` 分支，它的对外 API 尽量与主分支保持一致，但事件包名、构建配置、部分底层实现和少量行为存在差异。Forge 细节见 [forge_1.20.1_api_migration.md](forge_1.20.1_api_migration.md)。

## 版本与定位

| 项目 | 主分支 |
| --- | --- |
| Minecraft | `1.21.1` |
| NeoForge | `21.1.209` |
| Java | `21` |
| Mod ID | `i113w_camera_lib` |
| Maven group | `com.i113w` |
| 当前版本 | `0.0.3` |
| 功能端 | 客户端相机与输入逻辑 |

这个库提供 RTS 风格相机、正交相机、自由相机、鼠标框选、右键动作框、鼠标射线、屏幕投影、选中/悬停高亮和自定义光标渲染。业务模组负责决定哪些实体可选、如何响应框选和右键事件，以及如何把命令发给服务端。

## 接入方式

### Gradle 依赖

如果使用项目内 jar：

```groovy
dependencies {
    implementation files("libs/i113w_camera_lib-0.0.3.jar")
}
```

如果通过本项目的 `publish` 输出到 Maven 仓库：

```groovy
repositories {
    mavenLocal()
}

dependencies {
    implementation "com.i113w:i113w_camera_lib:0.0.3"
}
```

### `neoforge.mods.toml` 依赖

只在客户端调用本库时，建议把依赖限制在客户端：

```toml
[[dependencies."your_mod_id"]]
modId="i113w_camera_lib"
type="required"
versionRange="[0.0.3,)"
ordering="NONE"
side="CLIENT"
```

如果你的公共代码或服务端代码会直接类加载本库类型，则服务端也必须安装本库，并把 `side` 改成 `BOTH`。更推荐的做法是把 `RTSCameraController`、输入事件和渲染相关调用隔离在客户端类中。

## 最小集成流程

1. 客户端初始化时注册 `IRTSInteractionDelegate`。
2. 自己注册一个按键或命令，调用 `RTSCameraController.get().enterMode(...)` 开启相机。
3. 监听 `RTSBoxSelectEvent`，更新业务模组选区，并调用 `CameraLibAPI.get().setSelectedEntities(...)` 同步给库渲染高亮。
4. 监听 `RTSRightClickEvent`，把移动、攻击或交互命令发包给服务端。
5. 登出或清理状态时调用 `RTSCameraController.get().reset()` 和 `CameraLibAPI.get().clearSelection()`。

```java
package com.example.client;

import com.example.ExampleMod;
import com.i113w.camera_lib.api.CameraLibAPI;
import com.i113w.camera_lib.api.IRTSInteractionDelegate;
import com.i113w.camera_lib.api.event.RTSBoxSelectEvent;
import com.i113w.camera_lib.api.event.RTSRightClickEvent;
import com.i113w.camera_lib.camera.RTSCameraController;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;

public final class CameraIntegration {
    private static final Set<Integer> SELECTED_IDS = new HashSet<>();
    private static final ResourceLocation CURSOR_DEFAULT =
            ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "textures/gui/cursor_default.png");
    private static final ResourceLocation CURSOR_ATTACK =
            ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "textures/gui/cursor_attack.png");

    @EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            CameraLibAPI.get().setInteractionDelegate(new Delegate());
        }
    }

    @EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
    public static final class GameBus {
        @SubscribeEvent
        public static void onKey(InputEvent.Key event) {
            if (event.getKey() == GLFW.GLFW_KEY_G && event.getAction() == GLFW.GLFW_PRESS) {
                RTSCameraController.get().enterMode(RTSCameraController.CameraStyle.ORTHOGRAPHIC);
            }
        }

        @SubscribeEvent
        public static void onBoxSelect(RTSBoxSelectEvent event) {
            SELECTED_IDS.clear();
            for (Entity entity : event.getCandidates()) {
                SELECTED_IDS.add(entity.getId());
            }
            CameraLibAPI.get().setSelectedEntities(SELECTED_IDS);
        }

        @SubscribeEvent
        public static void onRightClick(RTSRightClickEvent event) {
            if (SELECTED_IDS.isEmpty()) return;

            if (event.isDrag()) {
                // 右键红框，业务模组自行过滤敌我并发包。
                ExamplePackets.sendAttackOrder(SELECTED_IDS, event.getDragTargets());
                return;
            }

            HitResult hit = event.getSingleHitResult();
            if (hit instanceof BlockHitResult blockHit) {
                ExamplePackets.sendMoveOrder(SELECTED_IDS, blockHit.getBlockPos());
            } else if (hit instanceof EntityHitResult entityHit) {
                ExamplePackets.sendAttackOrder(SELECTED_IDS, entityHit.getEntity());
            }
        }
    }

    private static final class Delegate implements IRTSInteractionDelegate {
        @Override
        public boolean isSelectable(Entity entity) {
            return ExampleClientUnits.isOwnedByLocalPlayer(entity);
        }

        @Override
        public @Nullable ResourceLocation getCursorIcon(@Nullable Entity hoveredEntity, boolean isAttackDragging) {
            if (isAttackDragging || ExampleClientUnits.isEnemy(hoveredEntity)) {
                return CURSOR_ATTACK;
            }
            return CURSOR_DEFAULT;
        }
    }

    public static void clearClientState() {
        SELECTED_IDS.clear();
        CameraLibAPI.get().clearSelection();
        RTSCameraController.get().reset();
        Minecraft.getInstance().setScreen(null);
    }
}
```

## 运行模型

相机激活后，库会接管一部分客户端输入与渲染：

| 行为 | 说明 |
| --- | --- |
| 玩家移动输入 | `forwardImpulse`、`leftImpulse`、跳跃、潜行会被清零，避免玩家本体移动。 |
| 鼠标抓取 | 每 tick 释放 Minecraft 鼠标抓取，并隐藏系统光标。 |
| 相机更新 | `RTSCameraController.tick(...)` 每个客户端 tick 调用，位置和旋转带插值。 |
| HUD | 相机激活时隐藏大部分原版 HUD，保留聊天、调试层、Tab 列表、标题和字幕等白名单层。 |
| 第一人称手 | 相机激活时取消手部渲染。 |
| ESC | 相机激活时按 ESC 会退出相机、清空库侧选中和悬停状态，并关闭当前 screen。 |
| 左键 | 单点选择或绿色框选，释放时发布 `RTSBoxSelectEvent`。 |
| 右键 | 单点命中或红色动作框，释放时发布 `RTSRightClickEvent`。 |
| 滚轮 | 地面聚焦模式调整 zoom，自由模式沿视线前后移动。 |
| `Left Ctrl` | 默认相机旋转键。地面聚焦模式按吸附角旋转，自由模式连续旋转。 |
| 鼠标上下边缘 | 未按旋转键时，靠近屏幕上/下边缘会调整俯仰角。 |

事件和输入处理都在客户端发生。业务模组需要自行把最终命令通过网络包发给服务端执行。

## 公开 API 总览

推荐业务模组直接使用这些类型：

| 类型 | 用途 |
| --- | --- |
| `CameraLibAPI` | 注册交互委托，同步选中实体 ID，读取悬停实体 ID。 |
| `IRTSInteractionDelegate` | 注入实体可选性和光标贴图决策。 |
| `RTSBoxSelectEvent` | 左键单点或框选完成事件。 |
| `RTSRightClickEvent` | 右键单点或右键拖拽完成事件。 |
| `RTSCameraController` | 开关相机、切换模式、手动移动/缩放/旋转。 |
| `MouseRayCaster` | 从当前鼠标位置做客户端方块/实体命中检测。 |
| `ScreenProjector` | 判断实体 AABB 是否投影到 GUI 选框内。 |
| `MatrixCache` | 读取当前帧 model-view 和 projection 矩阵缓存。 |
| `OrthographicProjection` | 创建库使用的正交投影矩阵，通常只供高级渲染或调试使用。 |
| `CameraLibConfig` | 读取已经 bake 到静态字段的客户端配置。 |
| `CameraLibKeyMappings` | 读取库内置旋转键 `CAMERA_ROTATE`。 |

下面这些类是 `public`，但主要是库内部实现。业务模组通常不应直接依赖它们：

| 类型 | 说明 |
| --- | --- |
| `RTSSelectionManager` | 维护当前拖拽矩形，不保存业务选区。 |
| `RTSCameraEntity` | 纯客户端虚拟相机实体。 |
| `CameraLibEntities` | 注册虚拟相机实体类型。 |
| `RTSInputHandler` | 库的客户端输入接管逻辑。 |
| `RTSRenderHandler` | 库的选框、高亮和 GUI 渲染逻辑。 |
| `RTSCameraRenderer` | 虚拟相机实体渲染器，实体本身不渲染。 |
| `ClientEventHandler` | 登出时重置库状态。 |
| `NativeCursorController` | 隐藏和恢复 GLFW 系统光标。 |
| `CameraLibTestCommands` | `/cameralibtest` 调试命令。 |

## `CameraLibAPI`

入口：

```java
CameraLibAPI api = CameraLibAPI.get();
```

方法：

| 方法 | 说明 |
| --- | --- |
| `static CameraLibAPI get()` | 获取单例。 |
| `void setInteractionDelegate(IRTSInteractionDelegate delegate)` | 注册业务委托。主分支没有 null 保护，不要传 `null`。 |
| `IRTSInteractionDelegate getDelegate()` | 获取当前委托。未注册时为 `IRTSInteractionDelegate.DEFAULT`。 |
| `void setSelectedEntities(Set<Integer> entityIds)` | 同步业务模组选中的实体 ID。主分支没有 null 保护，不要传 `null`。 |
| `Set<Integer> getSelectedEntities()` | 返回选中 ID 的拷贝，修改返回值不会改库内部状态。 |
| `void setHoveredEntityId(int id)` | 设置悬停实体 ID。通常由库内部射线检测写入。 |
| `int getHoveredEntityId()` | 获取当前悬停实体 ID，`-1` 表示没有悬停实体。 |
| `void clearSelection()` | 清空选中 ID，并把悬停 ID 设为 `-1`。 |

典型用法：

```java
private static final Set<Integer> selectedIds = new HashSet<>();

public static void replaceSelection(List<Entity> entities) {
    selectedIds.clear();
    for (Entity entity : entities) {
        selectedIds.add(entity.getId());
    }
    CameraLibAPI.get().setSelectedEntities(selectedIds);
}

public static void clearSelection() {
    selectedIds.clear();
    CameraLibAPI.get().clearSelection();
}
```

注意事项：

- 库只保存实体 ID，不保存业务对象。实体死亡、卸载或换世界后，业务模组应清理自己的选区。
- 选中高亮完全由 `setSelectedEntities` 驱动。只处理 `RTSBoxSelectEvent` 但不同步 ID，不会显示白色高亮框。
- 悬停 ID 由库每 3 tick 更新一次，命中范围使用 `MouseRayCaster.pickFromMouse(..., 1024.0)`。

## `IRTSInteractionDelegate`

接口：

```java
public interface IRTSInteractionDelegate {
    boolean isSelectable(Entity entity);

    @Nullable
    ResourceLocation getCursorIcon(@Nullable Entity hoveredEntity, boolean isAttackDragging);
}
```

`isSelectable(Entity entity)` 只用于左键选择：

- 单点选择命中实体后调用。
- 框选时对候选实体逐个调用。
- 返回 `false` 的实体不会进入 `RTSBoxSelectEvent.getCandidates()`。
- 右键单点、右键红框和悬停检测不会调用这个方法。

`getCursorIcon(@Nullable Entity hoveredEntity, boolean isAttackDragging)` 用于每帧 GUI 后渲染自定义光标：

- `hoveredEntity` 来自库侧当前悬停 ID，可能为 `null`。
- `isAttackDragging` 表示玩家正在右键拖红框。
- 返回 `ResourceLocation` 时，库按 16x16 像素绘制整张贴图。
- 返回 `null` 时，不绘制库自定义光标。因为相机激活时系统光标会被隐藏，返回 `null` 通常意味着画面上没有相机光标。
- 不要在这个方法里做 I/O、查网络状态或构造大量对象。建议把常用 `ResourceLocation` 缓存在静态字段里。

默认委托行为：

| 方法 | 默认行为 |
| --- | --- |
| `isSelectable` | 所有实体都返回 `true`。 |
| `getCursorIcon` | 返回 `null`，即不绘制自定义光标。 |

NeoForge 1.21.1 推荐这样创建贴图路径：

```java
private static final ResourceLocation CURSOR_MOVE =
        ResourceLocation.fromNamespaceAndPath("your_mod_id", "textures/gui/cursor_move.png");
```

Forge 1.20.1 分支通常使用：

```java
private static final ResourceLocation CURSOR_MOVE =
        new ResourceLocation("your_mod_id", "textures/gui/cursor_move.png");
```

## 事件

两个事件都发布在游戏事件总线 `NeoForge.EVENT_BUS`，对应订阅注解包名为 `net.neoforged.bus.api.SubscribeEvent`。事件是客户端事件，不包含服务端玩家对象。

```java
@EventBusSubscriber(modid = YourMod.MODID, value = Dist.CLIENT)
public final class YourClientEvents {
    @SubscribeEvent
    public static void onBoxSelect(RTSBoxSelectEvent event) {
    }

    @SubscribeEvent
    public static void onRightClick(RTSRightClickEvent event) {
    }
}
```

### `RTSBoxSelectEvent`

触发时机：左键释放时。库会根据拖拽矩形大小决定是单点还是框选。

构造和读取：

```java
public RTSBoxSelectEvent(List<Entity> candidates)

public List<Entity> getCandidates()
```

候选实体规则：

| 操作 | 候选规则 |
| --- | --- |
| 左键单点 | 鼠标射线命中的实体必须是存活 `LivingEntity`，不是本地玩家，在相机有效范围内，并通过 `delegate.isSelectable(entity)`。 |
| 左键框选 | `level.entitiesForRendering()` 中每个实体必须是存活 `LivingEntity`，不是本地玩家，在相机有效范围内，AABB 至少一个角投影进选框，并通过 `delegate.isSelectable(entity)`。 |

相机有效范围：

- 实体 Y 与选择中心 Y 的距离不超过 `250.0`。
- 实体与选择中心的水平距离不超过 `256` 格。
- 实体 Y 在 `[-64, 320]` 内。
- RTS 和 ORTHOGRAPHIC 使用 `RTSCameraController.get().getFocusPosition()` 作为选择中心。
- FREE 使用当前主相机位置作为选择中心。

事件处理示例：

```java
@SubscribeEvent
public static void onBoxSelect(RTSBoxSelectEvent event) {
    Set<Integer> selected = new HashSet<>();
    for (Entity entity : event.getCandidates()) {
        selected.add(entity.getId());
    }
    CameraLibAPI.get().setSelectedEntities(selected);
}
```

### `RTSRightClickEvent`

触发时机：右键释放时。库会根据右键拖拽矩形大小决定是单点还是红框。

构造和读取：

```java
public RTSRightClickEvent(HitResult singleHitResult)
public RTSRightClickEvent(List<Entity> dragTargets)

public boolean isDrag()
public HitResult getSingleHitResult()
public List<Entity> getDragTargets()
```

| 方法 | 单点右键 | 右键拖拽 |
| --- | --- | --- |
| `isDrag()` | `false` | `true` |
| `getSingleHitResult()` | 返回射线命中的 `HitResult` | 返回 `null` |
| `getDragTargets()` | 返回空列表 | 返回框内目标列表 |

右键拖拽目标规则：

- 只收集 `LivingEntity`。
- 实体必须存活。
- 排除本地玩家。
- AABB 至少一个角投影进红框。
- 不调用 `IRTSInteractionDelegate.isSelectable`，业务模组需要自行过滤敌我、阵营、权限等。

示例：

```java
@SubscribeEvent
public static void onRightClick(RTSRightClickEvent event) {
    if (event.isDrag()) {
        List<Entity> targets = event.getDragTargets();
        // 发包：对目标列表执行攻击、集火或区域命令。
        return;
    }

    HitResult hit = event.getSingleHitResult();
    if (hit instanceof BlockHitResult blockHit) {
        // 发包：移动到 blockHit.getBlockPos()
    } else if (hit instanceof EntityHitResult entityHit) {
        // 发包：攻击或交互 entityHit.getEntity()
    }
}
```

## `RTSCameraController`

入口：

```java
RTSCameraController camera = RTSCameraController.get();
```

相机模式：

```java
public enum CameraStyle {
    FREE,
    RTS,
    ORTHOGRAPHIC
}
```

| 模式 | 行为 |
| --- | --- |
| `RTS` | 地面聚焦，透视投影，FOV 被压到 `25.0`。滚轮修改 zoom，摄像机位于焦点反方向 `zoomLevel * 4.0` 距离。 |
| `ORTHOGRAPHIC` | 地面聚焦，Mixin 覆盖投影矩阵为正交投影。可见宽度是 `zoomLevel * CameraLibConfig.orthographicZoomMultiplier`。 |
| `FREE` | 自由相机，`targetPos` 是相机位置本身。滚轮沿视线移动，按旋转键时连续偏航。 |

主要方法：

| 方法 | 说明 |
| --- | --- |
| `static RTSCameraController get()` | 获取单例。 |
| `void enterMode(CameraStyle style)` | 退出当前相机后，以指定模式进入相机。推荐业务模组用这个方法精确启动。 |
| `void exitMode()` | 如果相机激活，则退出相机。 |
| `void toggleRTSMode()` | 切换激活状态。主分支进入时使用当前 `currentStyle`，不强制切回 `RTS`。 |
| `void toggleCameraStyle()` | 只在激活时生效，循环顺序为 `RTS -> FREE -> ORTHOGRAPHIC -> RTS`。 |
| `void reset()` | 退出相机，清理虚拟相机、原视角实体和基础状态，并恢复系统光标。不会清理 `CameraLibAPI` 选区。 |
| `CameraStyle getCameraStyle()` | 返回当前模式。 |
| `boolean isActive()` | 返回相机是否激活。 |
| `boolean isGroundFocusedStyle()` | `RTS` 和 `ORTHOGRAPHIC` 返回 `true`，`FREE` 返回 `false`。 |
| `float getZoomLevel()` | 返回当前 zoom。 |
| `float getOrthographicVisibleWidth()` | 返回正交可见宽度。 |
| `Vec3 getFocusPosition()` | 返回当前地面聚焦点或自由模式目标位置。 |
| `boolean shouldUseOrthographicProjection()` | 激活且模式为 `ORTHOGRAPHIC` 时返回 `true`。 |
| `void adjustPitch(float delta)` | 修改目标俯仰角。正交锁俯仰开启时会固定到 `35.264389...`。 |
| `void snapYaw(float step)` | 地面聚焦模式下把目标 yaw 增加 `step`。 |
| `void adjustYaw(float delta)` | 直接增加目标 yaw，任意模式可用。 |
| `void handleZoom(float scrollDelta)` | 地面聚焦模式修改 zoom，自由模式沿视线移动。 |
| `void handleInput(float moveX, float moveZ, float rotateYaw, float zoomDelta, float moveY, boolean sprintDown)` | 手动输入入口。库内部每 tick 根据 WASD 等按键调用。 |
| `void tick(float partialTick)` | 插值更新虚拟相机位置和旋转。库内部已自动调用。 |

推荐启动方式：

```java
RTSCameraController.get().enterMode(RTSCameraController.CameraStyle.ORTHOGRAPHIC);
```

如果你希望热键总是打开 RTS 模式，不要依赖主分支的 `toggleRTSMode()`：

```java
RTSCameraController camera = RTSCameraController.get();
if (camera.isActive()) {
    camera.exitMode();
} else {
    camera.enterMode(RTSCameraController.CameraStyle.RTS);
}
```

`handleInput` 参数含义：

| 参数 | 含义 |
| --- | --- |
| `moveX` | 相对相机左右移动输入。库内置映射中，按左键为正，按右键为负。 |
| `moveZ` | 相对相机前后移动输入。库内置映射中，按前进为正，按后退为负。 |
| `rotateYaw` | 自由模式连续 yaw 输入，会乘以 `CameraLibConfig.freeRotationSpeed`。地面聚焦模式不使用它。 |
| `zoomDelta` | 目前只在自由模式中作为额外 Y 方向偏移使用。库内置输入传 `0`，滚轮走 `handleZoom`。 |
| `moveY` | 上下移动输入。跳跃为正，潜行为负。 |
| `sprintDown` | 是否按住 sprint。主分支当前实现中为 `true` 时使用 `moveSprintMultiplier` 作为移动速度，为 `false` 时使用 `moveBaseSpeed`。 |

## 选择与拖拽矩形

`RTSSelectionManager` 维护当前左键和右键拖拽状态：

```java
RTSSelectionManager manager = RTSSelectionManager.get();

boolean leftDragging = manager.isDragging();
ScreenProjector.ScreenRect leftRect = manager.getSelectionRect();

boolean rightDragging = manager.isRightDragging();
ScreenProjector.ScreenRect rightRect = manager.getRightDragRect();
```

方法：

| 方法 | 说明 |
| --- | --- |
| `reset()` | 清空左右拖拽状态和矩形。 |
| `startDrag(float x, float y)` | 开始左键拖拽，坐标为 GUI 缩放坐标。 |
| `updateDrag(float x, float y)` | 更新左键拖拽终点。 |
| `endDrag()` | 结束左键拖拽。 |
| `isDragging()` | 是否正在左键拖拽。 |
| `startRightDrag(float x, float y)` | 开始右键拖拽。 |
| `updateRightDrag(float x, float y)` | 更新右键拖拽终点。 |
| `endRightDrag()` | 结束右键拖拽。 |
| `isRightDragging()` | 是否正在右键拖拽。 |
| `getSelectionRect()` | 返回左键矩形。 |
| `getRightDragRect()` | 返回右键矩形。 |

业务模组通常只需要读 `isDragging()`、`isRightDragging()` 或矩形做调试。不要把业务选区状态写进 `RTSSelectionManager`，选中实体 ID 应统一通过 `CameraLibAPI` 同步。

## 射线与屏幕投影

### `MouseRayCaster`

```java
HitResult hit = MouseRayCaster.pickFromMouse(mouseX, mouseY, 1024.0);
```

参数：

| 参数 | 说明 |
| --- | --- |
| `mouseX` | 原始窗口像素坐标，不是 GUI 缩放坐标。 |
| `mouseY` | 原始窗口像素坐标，不是 GUI 缩放坐标。 |
| `pickRange` | 射线长度。库内置使用 `1024.0`。 |

行为：

- 依赖 `MatrixCache.isValid()`。
- 如果无相机、无世界或矩阵无效，返回 `BlockHitResult.miss(...)`。
- 同时检测方块和实体。
- 如果实体命中距离比方块命中更近，返回 `EntityHitResult`，否则返回方块命中。
- 实体过滤条件为不是当前相机实体、不是旁观者、可拾取。

### `ScreenProjector`

矩形类型：

```java
public record ScreenRect(float x, float y, float width, float height) {
    public boolean contains(float px, float py)
}
```

AABB 投影判断：

```java
boolean inside = ScreenProjector.isAABBInScreenRect(
        entity.getBoundingBox(),
        rect,
        Minecraft.getInstance().gameRenderer.getMainCamera().getPosition()
);
```

说明：

- `ScreenRect` 使用 GUI 缩放坐标，原点在左上角。
- `camPos` 必须是当前渲染相机位置。
- 方法会投影 AABB 的 8 个角，只要有一个角在矩形内且 NDC 深度在 `[-1, 1]`，就返回 `true`。
- 矩阵无效时直接返回 `false`。

### `MatrixCache`

```java
if (!MatrixCache.isValid()) {
    return;
}

Matrix4f view = MatrixCache.getModelViewMatrix();
Matrix4f projection = MatrixCache.getProjectionMatrix();
```

主分支在 `RenderLevelStageEvent.Stage.AFTER_ENTITIES` 缓存：

- `event.getModelViewMatrix()`
- `event.getProjectionMatrix()`

矩阵只保证在已经渲染过一帧后有效。登出时库会通过 `ClientEventHandler` 调用 `MatrixCache.clear()`。

### `OrthographicProjection`

```java
Matrix4f projection = OrthographicProjection.create(visibleWidth, aspectRatio);
```

常量：

| 常量 | 值 |
| --- | --- |
| `NEAR_PLANE` | `0.05f` |
| `FAR_PLANE` | `1024.0f` |

`create` 会把 `visibleWidth` 限制到至少 `1.0`，把 `aspectRatio` 限制到至少 `0.01`，然后创建以屏幕中心为原点的正交投影。通常业务模组不需要直接调用它。

## 渲染与光标

相机激活且 GUI 未隐藏时，库会自动渲染：

| 元素 | 位置/阶段 | 颜色或规则 |
| --- | --- | --- |
| 选中实体线框 | `RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS` | 白色，实体 ID 来自 `CameraLibAPI.getSelectedEntities()`。 |
| 悬停实体线框 | 同上 | 黄色，实体 ID 来自 `CameraLibAPI.getHoveredEntityId()`，且不在选中集合内。 |
| 左键选框 | `RenderGuiEvent.Post` | 绿色填充 `0x8000FF00`，绿色边框 `0xFF00FF00`。 |
| 右键动作框 | `RenderGuiEvent.Post` | 红色填充 `0x80FF0000`，红色边框 `0xFFFF0000`。 |
| 自定义光标 | `RenderGuiEvent.Post` | 调用 `IRTSInteractionDelegate.getCursorIcon(...)`，返回非 null 时按 16x16 绘制。 |

虚拟相机实体本身不会渲染。`RTSCameraRenderer.shouldRender(...)` 始终返回 `false`。

正交模式相关 Mixin：

| Mixin | 行为 |
| --- | --- |
| `GameRendererMixin` | 当 `shouldUseOrthographicProjection()` 为 true 时，用 `OrthographicProjection.create(...)` 替换投影矩阵。 |
| `LevelRendererMixin` | 正交模式跳过云渲染。 |
| `LevelRendererFrustumMixin` | 正交模式跳过原版 `offsetFrustum`。如果检测到 `embeddium`、`rubidium` 或 `sodium`，该 mixin 不应用。 |

## 配置

配置文件是客户端配置：`.minecraft/config/i113w_camera_lib-client.toml`。

源码会在配置加载和重载时调用 `CameraLibConfig.bake()`，把 `ModConfigSpec` 值写入静态字段。业务模组如果要读取配置，直接读静态字段即可。

| TOML 路径 | 静态字段 | 默认值 | 范围 | 说明 |
| --- | --- | --- | --- | --- |
| `camera_settings.rts_mode.rtsPitchMin` | `rtsPitchMin` | `35.26439` | `[-90, 90]` | RTS/ORTHOGRAPHIC 俯仰最小值。 |
| `camera_settings.rts_mode.rtsPitchMax` | `rtsPitchMax` | `45.0` | `[-90, 90]` | RTS/ORTHOGRAPHIC 俯仰最大值。 |
| `camera_settings.rts_mode.rtsZoomMin` | `rtsZoomMin` | `10.0` | `[1, 200]` | 地面聚焦模式 zoom 最小值。 |
| `camera_settings.rts_mode.rtsZoomMax` | `rtsZoomMax` | `80.0` | `[1, 200]` | 地面聚焦模式 zoom 最大值。 |
| `camera_settings.rts_mode.rtsZoomSpeedMultiplier` | `rtsZoomSpeed` | `3.5` | `[0.1, 20]` | 滚轮缩放速度。 |
| `camera_settings.rts_mode.orthographicZoomMultiplier` | `orthographicZoomMultiplier` | `2.0` | `[0.1, 20]` | 只影响正交模式可见宽度。 |
| `camera_settings.rts_mode.lockOrthographicPitch` | `lockOrthographicPitch` | `true` | boolean | 正交模式是否锁定到 `35.264389...` 度。 |
| `camera_settings.rts_mode.rtsSnapAngle` | `rtsSnapAngle` | `90.0` | `[0, 360]` | 地面聚焦模式 Ctrl 旋转步进角。主分支初始化和模式切换时会按至少 `1.0` 度做 yaw 吸附。 |
| `camera_settings.free_mode.freePitchMin` | `freePitchMin` | `10.0` | `[-90, 90]` | FREE 俯仰最小值。 |
| `camera_settings.free_mode.freePitchMax` | `freePitchMax` | `90.0` | `[-90, 90]` | FREE 俯仰最大值。 |
| `camera_settings.free_mode.freeRotationSpeed` | `freeRotationSpeed` | `5.0` | `[0.1, 50]` | FREE 模式连续 yaw 速度。 |
| `camera_settings.edge_panning.thresholdPx` | `edgePanThreshold` | `20.0` | `[0, 200]` | 鼠标离屏幕上下边缘多少像素时触发俯仰调整。 |
| `camera_settings.edge_panning.pitchAdjustSpeed` | `edgePanPitchSpeed` | `2.0` | `[0, 20]` | 边缘俯仰调整速度。 |
| `camera_settings.movement.baseSpeed` | `moveBaseSpeed` | `1.0` | `[0.01, 50]` | 普通移动速度。 |
| `camera_settings.movement.sprintMultiplier` | `moveSprintMultiplier` | `2.0` | `[1, 20]` | 主分支当前实现中 sprint 按下时作为移动速度使用。Forge 1.20.1 分支中作为 `baseSpeed` 的倍率。 |

## 内置按键

库只注册一个按键：

| 字段 | 默认键 | 翻译键 | 用途 |
| --- | --- | --- | --- |
| `CameraLibKeyMappings.CAMERA_ROTATE` | Left Ctrl | `key.camera_lib.rotate` | 相机旋转键。 |

库没有内置“开启相机”的玩家按键。业务模组应自行注册热键，然后调用 `RTSCameraController.get().enterMode(...)` 或 `toggleRTSMode()`。

## 调试命令

主分支注册了客户端命令 `/cameralibtest`：

| 命令 | 说明 |
| --- | --- |
| `/cameralibtest` | 启动 ORTHOGRAPHIC 模式。 |
| `/cameralibtest ortho` | 启动 ORTHOGRAPHIC 模式。 |
| `/cameralibtest rts` | 启动 RTS 模式。 |
| `/cameralibtest free` | 启动 FREE 模式。 |
| `/cameralibtest stop` | 退出相机。 |
| `/cameralibtest move forward [amount]` | 按相机输入向前移动，默认 `amount=1`，内部乘 `4.0`。 |
| `/cameralibtest move back [amount]` | 向后移动。 |
| `/cameralibtest move left [amount]` | 向左移动。 |
| `/cameralibtest move right [amount]` | 向右移动。 |
| `/cameralibtest move up [amount]` | 向上移动。 |
| `/cameralibtest move down [amount]` | 向下移动。 |
| `/cameralibtest yaw <degrees>` | 调整 yaw。 |
| `/cameralibtest pitch <degrees>` | 调整 pitch。 |
| `/cameralibtest zoom <delta>` | 调用 `handleZoom(delta)`。 |

这些命令适合验证库本身，不建议作为业务模组的正式控制入口。

## 虚拟相机实体

`RTSCameraEntity` 是纯客户端虚拟实体，用作 Minecraft 的 camera entity。

主分支行为：

- 进入相机时创建 `new RTSCameraEntity(CameraLibEntities.RTS_CAMERA.get(), mc.level)`。
- 设置位置、yaw、pitch。
- 调用 `mc.level.addEntity(this.cameraEntity)` 加入客户端世界。
- 调用 `mc.setCameraEntity(this.cameraEntity)` 切换视角。
- 退出时恢复原 camera entity，并 `remove(Entity.RemovalReason.DISCARDED)`。
- `getAddEntityPacket(ServerEntity entity)` 会抛出 `UnsupportedOperationException`。

业务模组不要手动创建、保存、同步或召唤这个实体。

## Forge 1.20.1 需要不同处理的地方

完整说明见 [forge_1.20.1_api_migration.md](forge_1.20.1_api_migration.md)。这里列最容易影响业务模组的差异：

| 项目 | NeoForge 主分支 | Forge 1.20.1 分支 |
| --- | --- | --- |
| Java | 21 | 17 |
| MC/加载器 | Minecraft `1.21.1`，NeoForge `21.1.209` | Minecraft `1.20.1`，Forge `47.4.10` |
| 事件基类 | `net.neoforged.bus.api.Event` | `net.minecraftforge.eventbus.api.Event` |
| 订阅注解 | `net.neoforged.bus.api.SubscribeEvent` | `net.minecraftforge.eventbus.api.SubscribeEvent` |
| 游戏事件总线 | `NeoForge.EVENT_BUS` | `MinecraftForge.EVENT_BUS` |
| `EventBusSubscriber` | `net.neoforged.fml.common.EventBusSubscriber` | `net.minecraftforge.fml.common.Mod.EventBusSubscriber` |
| 配置 spec | `ModConfigSpec` | `ForgeConfigSpec` |
| `ResourceLocation` | 推荐 `ResourceLocation.fromNamespaceAndPath(...)` | 常用 `new ResourceLocation(namespace, path)` |
| `toggleRTSMode()` 进入行为 | 使用当前 `currentStyle` | 未激活时先重置为 `RTS` |
| sprint 移动速度 | `sprintDown ? moveSprintMultiplier : moveBaseSpeed` | `baseSpeed * sprintMultiplier` |
| `setInteractionDelegate(null)` | 会把 delegate 设成 null，后续可能 NPE | 忽略 null |
| `setSelectedEntities(null)` | 会 NPE | 清空后忽略 null |
| 虚拟相机实体 | 进入时 `level.addEntity(cameraEntity)` | 不加入 level，只 `setCameraEntity(cameraEntity)` |
| `MatrixCache` | `event.getModelViewMatrix()` | `event.getPoseStack().last().pose()` |
| Mixin refmap | 无显式 refmap | `i113w_camera_lib.refmap.json` |

如果你要写一套兼容两个分支的附属模组代码，建议建立平台层，只把以下内容放在平台层中：

- 事件包名和事件总线注册。
- `ResourceLocation` 构造方式。
- loader/mod metadata 依赖声明。
- 任何直接使用 Forge/NeoForge 专属事件类的代码。

业务层尽量只依赖这些统一类型：

```java
CameraLibAPI
IRTSInteractionDelegate
RTSBoxSelectEvent
RTSRightClickEvent
RTSCameraController
MouseRayCaster
ScreenProjector
MatrixCache
```

## 集成检查清单

- 已在客户端初始化阶段注册 `IRTSInteractionDelegate`。
- `getCursorIcon` 返回的贴图存在，大小按 16x16 设计。
- 业务选区变化后调用 `CameraLibAPI.get().setSelectedEntities(ids)`。
- 离开世界、断线或关闭玩法时清空业务选区，并调用 `CameraLibAPI.get().clearSelection()`。
- 开启相机时使用 `enterMode(...)`，避免 `toggleRTSMode()` 的当前模式语义造成误判。
- 右键事件只在客户端触发，服务端逻辑通过业务模组自己的网络包执行。
- 没有在服务端类中直接引用 `RTSCameraController`、`MouseRayCaster`、渲染事件或 `Minecraft.getInstance()`。
- 如果直接使用 `MouseRayCaster` 或 `ScreenProjector`，调用前检查 `MatrixCache.isValid()`。
- Forge 1.20.1 分支没有直接复制 NeoForge 的 Mixin、矩阵缓存或实体注册实现。
