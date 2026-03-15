# 113's Camera Lib

A NeoForge 1.21.1 client-side library that provides a complete **RTS-style camera system** — including camera movement, entity box-selection, right-click command zones, ray casting, and highlight rendering.

Designed to be consumed by other mods (e.g., *Better Mine Team*) via a thin API layer.

---

## Requirements

| Item | Version |
|------|---------|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.209+ |
| Java | 21 |
| Side | Client-only |

---

## Importing the Library

**Local JAR** (place the JAR in your mod's `libs/` directory):

```groovy
dependencies {
    implementation files('libs/i113w_camera_lib-0.0.1.jar')
}
```

**Declare the dependency** in `META-INF/neoforge.mods.toml`:

```toml
[[dependencies."your_mod_id"]]
    modId = "i113w_camera_lib"
    type = "required"
    versionRange = "[0.0.1,)"
    ordering = "BEFORE"
    side = "CLIENT"
```

---

## Key Interfaces & Classes

### `CameraLibAPI` — singleton entry point

```java
CameraLibAPI api = CameraLibAPI.get();

// Register your delegate
api.setInteractionDelegate(new MyDelegate());

// Sync selected entity IDs to the library (drives outline rendering)
api.setSelectedEntities(Set<Integer> ids);

// Query hovered entity (-1 if none)
int id = api.getHoveredEntityId();

// Clear selection and hover state
api.clearSelection();
```

---

### `IRTSInteractionDelegate` — inject business logic

```java
public interface IRTSInteractionDelegate {
    boolean isSelectable(Entity entity);
    ResourceLocation getCursorIcon(@Nullable Entity hoveredEntity, boolean isAttackDragging);
}
```

Register via `CameraLibAPI.get().setInteractionDelegate(impl)`.

---

### Events (fired on `NeoForge.EVENT_BUS`)

**`RTSBoxSelectEvent`** — left-click or drag-box completed

```java
@SubscribeEvent
public static void onSelect(RTSBoxSelectEvent event) {
    List<Entity> candidates = event.getCandidates(); // pre-filtered by isSelectable()
}
```

**`RTSRightClickEvent`** — right-click or right-drag completed

```java
@SubscribeEvent
public static void onRightClick(RTSRightClickEvent event) {
    if (event.isDrag()) {
        List<Entity> targets = event.getDragTargets();
    } else {
        HitResult hit = event.getSingleHitResult(); // BlockHitResult or EntityHitResult
    }
}
```

---

### `RTSCameraController` — camera state

```java
RTSCameraController cam = RTSCameraController.get();

cam.toggleRTSMode();      // activate / deactivate
cam.toggleCameraStyle();  // switch RTS <-> FREE (while active)
cam.isActive();           // boolean
cam.reset();              // force-exit, use on logout / level unload
```

---

## Camera Modes

| Mode | FOV | Zoom | Yaw Rotation |
|------|-----|------|--------------|
| RTS | 25° (fixed) | Scroll wheel | Hold `Left Ctrl` + drag (snaps by `rtsSnapAngle`) |
| FREE | Standard | Scroll wheel (moves forward) | Hold `Left Ctrl` + drag (continuous) |

---

## Highlight Rendering

The library automatically draws entity outlines when the camera is active. No rendering code is needed in the consumer mod — just keep `CameraLibAPI.setSelectedEntities(...)` in sync.

| Color | Condition |
|-------|-----------|
| White | Entity ID is in the selected set |
| Yellow | Hovered entity (not in selected set) |

---

## Configuration

Player-adjustable values in `.minecraft/config/i113w_camera_lib-client.toml`:

| Key | Default | Description |
|-----|---------|-------------|
| `rtsPitchMin / Max` | 35 / 45 | Pitch clamp range in RTS mode |
| `freePitchMin / Max` | 10 / 90 | Pitch clamp range in FREE mode |
| `rtsZoomMin / Max` | 10 / 80 | Zoom distance clamp in RTS mode |
| `rtsZoomSpeedMultiplier` | 3.5 | Scroll wheel zoom sensitivity |
| `rtsSnapAngle` | 90 | Degrees per yaw snap step |
| `freeRotationSpeed` | 5.0 | Mouse yaw sensitivity in FREE mode |
| `thresholdPx` | 20 | Edge-pan trigger distance (pixels from screen edge) |
| `baseSpeed` | 1.0 | WASD movement speed |
| `sprintMultiplier` | 2.0 | Speed multiplier when sprint key is held |

---

## License

MIT — see [LICENSE](LICENSE)

**Author:** i113w
