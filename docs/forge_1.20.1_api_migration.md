# Forge 1.20.1 分支差异与迁移指南

本文档按 [i113w/113CameraLib](https://github.com/i113w/113CameraLib) 的当前源码核对，用于说明 `forge-1.20.1` 分支与 `main` 分支之间需要不同处理的地方。

不要把 NeoForge 主分支的内部实现直接覆盖到 Forge 1.20.1 分支。两个分支的公开 API 名称尽量统一，但底层事件、构建、Mixin、矩阵和虚拟实体实现不同。

## 分支版本

| 项目 | NeoForge 主分支 | Forge 1.20.1 分支 |
| --- | --- | --- |
| 分支 | `main` | `forge-1.20.1` |
| Minecraft | `1.21.1` | `1.20.1` |
| 加载器 | NeoForge `21.1.209` | Forge `47.4.10` |
| Java | `21` | `17` |
| Gradle 插件 | `net.neoforged.moddev` | `net.minecraftforge.gradle`、`org.parchmentmc.librarian.forgegradle`、`org.spongepowered.mixin` |
| Mod 版本 | `0.0.3` | `1.20.1-0.0.3` |

## 已统一的公开调用

两个分支都提供以下主要 API：

```java
CameraLibAPI api = CameraLibAPI.get();
api.setInteractionDelegate(delegate);
api.setSelectedEntities(selectedIds);
Set<Integer> selected = api.getSelectedEntities();
api.setHoveredEntityId(entityId);
int hoveredId = api.getHoveredEntityId();
api.clearSelection();
```

```java
RTSCameraController camera = RTSCameraController.get();
camera.enterMode(RTSCameraController.CameraStyle.ORTHOGRAPHIC);
camera.exitMode();
camera.toggleRTSMode();
camera.toggleCameraStyle();
camera.handleZoom(scrollDelta);
camera.handleInput(moveX, moveZ, rotateYaw, 0.0f, moveY, sprintDown);
boolean active = camera.isActive();
```

```java
public final class MyDelegate implements IRTSInteractionDelegate {
    @Override
    public boolean isSelectable(Entity entity) {
        return true;
    }

    @Override
    public @Nullable ResourceLocation getCursorIcon(@Nullable Entity hoveredEntity, boolean isAttackDragging) {
        return null;
    }
}
```

事件类名和 getter 也统一：

```java
RTSBoxSelectEvent#getCandidates()

RTSRightClickEvent#isDrag()
RTSRightClickEvent#getSingleHitResult()
RTSRightClickEvent#getDragTargets()
```

## 平台导入替换

| 用途 | NeoForge 主分支 | Forge 1.20.1 分支 |
| --- | --- | --- |
| 事件基类 | `net.neoforged.bus.api.Event` | `net.minecraftforge.eventbus.api.Event` |
| 订阅注解 | `net.neoforged.bus.api.SubscribeEvent` | `net.minecraftforge.eventbus.api.SubscribeEvent` |
| Dist | `net.neoforged.api.distmarker.Dist` | `net.minecraftforge.api.distmarker.Dist` |
| 游戏事件总线 | `net.neoforged.neoforge.common.NeoForge.EVENT_BUS` | `net.minecraftforge.common.MinecraftForge.EVENT_BUS` |
| 事件订阅类注解 | `net.neoforged.fml.common.EventBusSubscriber` | `net.minecraftforge.fml.common.Mod.EventBusSubscriber` |
| Mod 事件总线 | 构造函数注入 `IEventBus` | `FMLJavaModLoadingContext.get().getModEventBus()` |
| 配置注册 | `ModContainer#registerConfig` | `FMLJavaModLoadingContext.get().registerConfig` |
| Config spec | `net.neoforged.neoforge.common.ModConfigSpec` | `net.minecraftforge.common.ForgeConfigSpec` |
| KeyMapping 注册事件 | `net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent` | `net.minecraftforge.client.event.RegisterKeyMappingsEvent` |
| 客户端 tick | `net.neoforged.neoforge.client.event.ClientTickEvent.Post` | `net.minecraftforge.event.TickEvent.ClientTickEvent` + `Phase.END` |
| 鼠标滚轮 delta | `InputEvent.MouseScrollingEvent#getScrollDeltaY()` | `InputEvent.MouseScrollingEvent#getScrollDelta()` |
| HUD 过滤事件 | `RenderGuiLayerEvent.Pre` + `VanillaGuiLayers` | `RenderGuiOverlayEvent.Pre` + `VanillaGuiOverlay` |
| 客户端命令事件 | `net.neoforged.neoforge.client.event.RegisterClientCommandsEvent` | `net.minecraftforge.client.event.RegisterClientCommandsEvent` |
| 实体 DeferredRegister | `DeferredRegister.create(Registries.ENTITY_TYPE, modid)` | `DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, modid)` |
| 注册对象 | `DeferredHolder<EntityType<?>, EntityType<T>>` | `RegistryObject<EntityType<T>>` |

## Forge 1.20.1 订阅示例

Forge 侧事件订阅使用 `@Mod.EventBusSubscriber` 和 `MinecraftForge.EVENT_BUS`：

```java
package com.example.client;

import com.example.ExampleMod;
import com.i113w.camera_lib.api.CameraLibAPI;
import com.i113w.camera_lib.api.event.RTSBoxSelectEvent;
import com.i113w.camera_lib.api.event.RTSRightClickEvent;
import com.i113w.camera_lib.camera.RTSCameraController;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public final class ExampleForgeCameraEvents {
    @SubscribeEvent
    public static void onBoxSelect(RTSBoxSelectEvent event) {
        CameraLibAPI.get().setSelectedEntities(ExampleSelection.toIds(event.getCandidates()));
    }

    @SubscribeEvent
    public static void onRightClick(RTSRightClickEvent event) {
        ExampleOrders.handleRightClick(event);
    }

    public static void openRtsCamera() {
        RTSCameraController.get().enterMode(RTSCameraController.CameraStyle.RTS);
    }
}
```

`ResourceLocation` 写法也要按 1.20.1：

```java
private static final ResourceLocation CURSOR =
        new ResourceLocation(ExampleMod.MODID, "textures/gui/cursor.png");
```

## 构建与 metadata

Forge 1.20.1 分支使用 Java 17：

```groovy
java.toolchain.languageVersion = JavaLanguageVersion.of(17)
```

依赖 Forge：

```groovy
dependencies {
    minecraft "net.minecraftforge:forge:${minecraft_version}-${forge_version}"
    implementation "org.spongepowered:mixin:0.8.5"
    annotationProcessor "org.spongepowered:mixin:0.8.5:processor"
}
```

`META-INF/mods.toml` 的依赖格式是 Forge 格式：

```toml
[[dependencies."your_mod_id"]]
    modId="i113w_camera_lib"
    mandatory=true
    versionRange="[1.20.1-0.0.3,)"
    ordering="NONE"
    side="CLIENT"
```

如果公共代码会直接引用本库类，服务端也需要安装库，并把 `side` 改成 `BOTH`。如果只在客户端平台层引用相机库，推荐 `CLIENT`。

Forge 分支的 mixin 配置包含 refmap：

```json
{
  "compatibilityLevel": "JAVA_17",
  "refmap": "i113w_camera_lib.refmap.json"
}
```

构建脚本里保留了 Mixin AP 的 refmap 输出配置。不要删除这部分，否则 1.20.1 运行环境可能无法正确重映射 mixin。

## 运行行为差异

### `toggleRTSMode()`

NeoForge 主分支：

```java
public void toggleRTSMode() {
    if (isActive) exitRTS();
    else enterRTS();
}
```

主分支在未激活时会使用当前 `currentStyle` 进入相机。如果上一次退出时是 `FREE` 或 `ORTHOGRAPHIC`，再次 `toggleRTSMode()` 会按该模式进入。

Forge 1.20.1 分支：

```java
public void toggleRTSMode() {
    if (isActive) {
        exitRTS();
    } else {
        this.currentStyle = CameraStyle.RTS;
        enterRTS();
    }
}
```

Forge 分支未激活时会先把模式设为 `RTS`。

跨分支业务代码如果需要确定模式，统一用：

```java
RTSCameraController.get().enterMode(RTSCameraController.CameraStyle.RTS);
```

### sprint 移动速度

NeoForge 主分支当前实现：

```java
float moveSpeed = sprintDown ? CameraLibConfig.moveSprintMultiplier : CameraLibConfig.moveBaseSpeed;
```

Forge 1.20.1 分支当前实现：

```java
float moveSpeed = CameraLibConfig.moveBaseSpeed;
if (sprintDown) {
    moveSpeed *= CameraLibConfig.moveSprintMultiplier;
}
```

因此相同配置下，Forge 的 sprint 是“基础速度乘倍率”，主分支是“直接使用 sprintMultiplier 字段作为速度”。文档和配置 UI 不要把两个分支的数值效果写成完全一致。

### null 处理

NeoForge 主分支：

- `CameraLibAPI#setInteractionDelegate(null)` 会把 delegate 设为 `null`，后续输入或渲染调用可能 NPE。
- `CameraLibAPI#setSelectedEntities(null)` 会 NPE。

Forge 1.20.1 分支：

- `setInteractionDelegate(null)` 会被忽略。
- `setSelectedEntities(null)` 会先清空，再忽略 null。

跨分支代码不要传 null。需要清空选区时用：

```java
CameraLibAPI.get().clearSelection();
```

或传空集合：

```java
CameraLibAPI.get().setSelectedEntities(Set.of());
```

### yaw 吸附角为 0

NeoForge 主分支在初始化和模式切换时使用：

```java
float snap = Math.max(1.0f, CameraLibConfig.rtsSnapAngle);
```

Forge 1.20.1 分支使用：

```java
if (snap <= 0) return rawYaw;
```

也就是说 Forge 中 `rtsSnapAngle <= 0` 会禁用进入相机时的 yaw 吸附；NeoForge 主分支会按至少 `1` 度处理。

### 虚拟相机实体

NeoForge 主分支进入相机时：

```java
this.cameraEntity = new RTSCameraEntity(CameraLibEntities.RTS_CAMERA.get(), mc.level);
mc.level.addEntity(this.cameraEntity);
mc.setCameraEntity(this.cameraEntity);
```

Forge 1.20.1 分支进入相机时：

```java
this.cameraEntity = new RTSCameraEntity(CameraLibEntities.RTS_CAMERA.get(), mc.level);
mc.setCameraEntity(this.cameraEntity);
```

Forge 分支不把虚拟相机加入 level。不要把 NeoForge 的 `level.addEntity` 行复制到 Forge 分支，Forge 代码已经按“仅客户端 camera entity”处理。

实体类型注册也不同：

| 项目 | NeoForge 主分支 | Forge 1.20.1 分支 |
| --- | --- | --- |
| 注册 holder | `DeferredHolder<EntityType<?>, EntityType<RTSCameraEntity>>` | `RegistryObject<EntityType<RTSCameraEntity>>` |
| size | `0.1f, 0.1f` | `0.0f, 0.0f` |
| summon | 未调用 `noSummon()` | 调用 `noSummon()` |
| tracking range | `4` | `0` |
| update interval | `20` | `Integer.MAX_VALUE` |

`RTSCameraEntity` 方法签名也不同：

| 方法 | NeoForge 1.21.1 | Forge 1.20.1 |
| --- | --- | --- |
| `defineSynchedData` | `defineSynchedData(SynchedEntityData.Builder builder)` | `defineSynchedData()` |
| `getAddEntityPacket` | `getAddEntityPacket(ServerEntity entity)` | `getAddEntityPacket()` |
| 保存控制 | `noSave()` 注册，未 override `shouldBeSaved()` | 注册 `noSave()`，且 `shouldBeSaved()` 返回 false |

### MatrixCache

对外方法一致：

```java
MatrixCache.getModelViewMatrix();
MatrixCache.getProjectionMatrix();
MatrixCache.isValid();
MatrixCache.clear();
```

内部缓存来源不同：

| 分支 | model-view 来源 | projection 来源 |
| --- | --- | --- |
| NeoForge 主分支 | `event.getModelViewMatrix()` | `event.getProjectionMatrix()` |
| Forge 1.20.1 | `event.getPoseStack().last().pose()` | `event.getProjectionMatrix()` |

Forge 1.20.1 的矩阵/投影实现已经按该版本渲染事件验证，不要用 NeoForge 的实现覆盖。

### 渲染事件拆分

NeoForge 主分支中，FOV、手、HUD 层过滤和自定义光标部分位于 `RTSInputHandler`，选框和 3D 高亮位于 `RTSRenderHandler`。

Forge 1.20.1 分支中，FOV、手、HUD 过滤、选框、高亮和光标渲染主要集中在 `RTSRenderHandler`。

对业务模组来说结果一致：相机激活后会隐藏手、过滤 HUD、渲染选框、高亮和自定义光标。但如果你维护库源码，不要按类名机械搬运实现。

### Mixin 目标签名

`GameRendererMixin` 的 `getProjectionMatrix(D)Lorg/joml/Matrix4f;` 注入在两个分支一致。

`LevelRendererMixin` 的云渲染方法签名不同：

NeoForge 主分支：

```java
renderClouds(
    PoseStack poseStack,
    Matrix4f modelViewMatrix,
    Matrix4f projectionMatrix,
    float partialTick,
    double cameraX,
    double cameraY,
    double cameraZ
)
```

Forge 1.20.1 分支：

```java
renderClouds(
    PoseStack poseStack,
    Matrix4f projectionMatrix,
    float partialTick,
    double cameraX,
    double cameraY,
    double cameraZ
)
```

如果改动 Mixin，必须按目标 Minecraft 版本确认签名。

## 从旧 Forge API 迁移

如果附属模组还在使用更早的 Forge 1.20.1 接口，按下面替换。

### 相机控制类

旧：

```java
RTSCameraManager.get()
```

新：

```java
RTSCameraController.get()
```

替换：

| 旧 | 新 |
| --- | --- |
| `RTSCameraManager` | `RTSCameraController` |
| `RTSCameraManager.CameraStyle` | `RTSCameraController.CameraStyle` |
| `RTSCameraManager.get().toggleRTSMode()` | `RTSCameraController.get().toggleRTSMode()` |
| `RTSCameraManager.get().toggleCameraStyle()` | `RTSCameraController.get().toggleCameraStyle()` |
| `RTSCameraManager.get().isActive()` | `RTSCameraController.get().isActive()` |
| `RTSCameraManager.get().reset()` | `RTSCameraController.get().reset()` |

`handleInput` 新签名：

```java
handleInput(moveX, moveZ, rotateYaw, zoomDelta, moveY, sprintDown);
```

如果没有独立 `zoomDelta`，第四个参数传 `0.0f`。

### 删除旧 facade

旧 `CameraController` facade 不再使用：

| 旧 | 新 |
| --- | --- |
| `CameraController.toggleRTSMode()` | `RTSCameraController.get().toggleRTSMode()` |
| `CameraController.isRTSActive()` | `RTSCameraController.get().isActive()` |
| `CameraController.setSelectedEntityIds(ids)` | `CameraLibAPI.get().setSelectedEntities(ids)` |
| `CameraController.getSelectedEntityIds()` | `CameraLibAPI.get().getSelectedEntities()` |
| `CameraController.clearSelection()` | `CameraLibAPI.get().clearSelection()` |

### `CameraLibAPI`

| 旧 | 新 |
| --- | --- |
| `registerDelegate(delegate)` | `setInteractionDelegate(delegate)` |
| `delegate()` | `getDelegate()` |

选中和悬停状态统一放在 `CameraLibAPI`：

```java
CameraLibAPI.get().setSelectedEntities(ids);
CameraLibAPI.get().getSelectedEntities();
CameraLibAPI.get().setHoveredEntityId(id);
CameraLibAPI.get().getHoveredEntityId();
CameraLibAPI.get().clearSelection();
```

不要再通过 `RTSSelectionManager` 保存已选实体或悬停实体。

### `IRTSInteractionDelegate`

旧接口：

```java
boolean isSelectable(Entity entity);
boolean isEnemy(Entity entity);
@Nullable ResourceLocation getCursorTexture(CursorType type);
```

新接口：

```java
boolean isSelectable(Entity entity);
@Nullable ResourceLocation getCursorIcon(@Nullable Entity hoveredEntity, boolean isAttackDragging);
```

旧的 `CursorType`、`isEnemy`、`getCursorTexture` 已移除。把光标判断集中到 `getCursorIcon`：

```java
@Override
public @Nullable ResourceLocation getCursorIcon(@Nullable Entity hoveredEntity, boolean isAttackDragging) {
    if (isAttackDragging) {
        return ATTACK_CURSOR;
    }
    if (hoveredEntity != null && isEnemyLike(hoveredEntity)) {
        return ATTACK_CURSOR;
    }
    if (hoveredEntity != null && isSelectable(hoveredEntity)) {
        return ALLY_CURSOR;
    }
    return DEFAULT_CURSOR;
}
```

### `RTSBoxSelectEvent`

旧：

```java
new RTSBoxSelectEvent(player, candidates);
event.getPlayer();
event.getCandidates();
```

新：

```java
new RTSBoxSelectEvent(candidates);
event.getCandidates();
```

事件不再携带 player。客户端需要玩家时使用：

```java
Minecraft.getInstance().player
```

### `RTSRightClickEvent`

旧：

```java
new RTSRightClickEvent(player, false, null, hit);
new RTSRightClickEvent(player, true, draggedTargets, null);
event.getPlayer();
event.getDraggedTargets();
event.getSingleHitResult();
event.isDrag();
```

新：

```java
new RTSRightClickEvent(hit);
new RTSRightClickEvent(dragTargets);
event.getDragTargets();
event.getSingleHitResult();
event.isDrag();
```

替换：

| 旧 | 新 |
| --- | --- |
| `getDraggedTargets()` | `getDragTargets()` |
| `getPlayer()` | 移除 |
| `new RTSRightClickEvent(player, false, null, hit)` | `new RTSRightClickEvent(hit)` |
| `new RTSRightClickEvent(player, true, targets, null)` | `new RTSRightClickEvent(targets)` |

### `RTSSelectionManager`

左键拖拽：

| 旧 | 新 |
| --- | --- |
| `startLeftDrag(x, y)` | `startDrag(x, y)` |
| `updateLeftDrag(x, y)` | `updateDrag(x, y)` |
| `endLeftDrag()` | `endDrag()` |
| `isLeftDragging()` | `isDragging()` |
| `getLeftRect()` | `getSelectionRect()` |

右键矩形：

| 旧 | 新 |
| --- | --- |
| `getRightRect()` | `getRightDragRect()` |

选中和悬停迁移：

| 旧 | 新 |
| --- | --- |
| `RTSSelectionManager.get().setSelectedIds(ids)` | `CameraLibAPI.get().setSelectedEntities(ids)` |
| `RTSSelectionManager.get().getSelectedIds()` | `CameraLibAPI.get().getSelectedEntities()` |
| `RTSSelectionManager.get().setHoveredEntityId(id)` | `CameraLibAPI.get().setHoveredEntityId(id)` |
| `RTSSelectionManager.get().getHoveredEntityId()` | `CameraLibAPI.get().getHoveredEntityId()` |
| `RTSSelectionManager.get().clearSelection()` | `CameraLibAPI.get().clearSelection()` |

### `ScreenProjector`

旧矩形类型：

```java
RTSSelectionManager.SelectionRect
```

新矩形类型：

```java
ScreenProjector.ScreenRect
```

调用：

```java
ScreenProjector.ScreenRect rect = RTSSelectionManager.get().getSelectionRect();
boolean hit = ScreenProjector.isAABBInScreenRect(entity.getBoundingBox(), rect, camPos);
```

### `MatrixCache`

旧：

```java
MatrixCache.VIEW_MATRIX
MatrixCache.PROJ_MATRIX
```

新：

```java
if (!MatrixCache.isValid()) {
    return;
}

MatrixCache.getModelViewMatrix();
MatrixCache.getProjectionMatrix();
```

### KeyMapping

旧：

```java
CameraLibKeyMappings.RTS_CAMERA_ROTATE
```

新：

```java
CameraLibKeyMappings.CAMERA_ROTATE
```

翻译键：

```text
key.camera_lib.rotate
key.categories.camera_lib
```

## 旧接口残留搜索

在附属模组仓库运行：

```powershell
rg -n "\bRTSCameraManager\b|\bCameraController\b|CameraLibAPI\.get\(\)\.delegate\(|\bregisterDelegate\(|\bisEnemy\(|\bgetCursorTexture\(|\bCursorType\b|\bgetDraggedTargets\(|\bgetPlayer\(|\bstartLeftDrag\(|\bupdateLeftDrag\(|\bendLeftDrag\(|\bisLeftDragging\(|\bgetLeftRect\(|\bgetRightRect\(|\bSelectionRect\b|\bRTS_CAMERA_ROTATE\b|\bVIEW_MATRIX\b|\bPROJ_MATRIX\b" src
```

迁移完成后，这个命令不应再命中需要替换的旧 API。

## 验证

库分支编译：

```powershell
.\gradlew.bat compileJava
```

附属模组迁移后，也应运行自身编译任务：

```powershell
.\gradlew.bat compileJava
```

运行时检查：

- `getCursorIcon` 返回的贴图是否存在，路径是否使用 Forge 1.20.1 的 `new ResourceLocation(namespace, path)`。
- `RTSBoxSelectEvent` 回调后是否调用 `CameraLibAPI.get().setSelectedEntities(ids)`。
- 右键拖拽是否在业务侧过滤敌我，因为库不会调用 `isSelectable`。
- 是否误把 NeoForge 的 `MatrixCache`、`LevelRendererMixin`、实体注册或 `level.addEntity` 行复制到 Forge 分支。
- 是否把 NeoForge 的 `net.neoforged.*` 包导入到了 Forge 1.20.1 代码中。
