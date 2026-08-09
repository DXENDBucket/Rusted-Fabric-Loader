# `class: CustomProjectile` 设计草案

状态：第二阶段已实现。独立定义、Action 发射、原版炮塔精确织入以及
`single` / `fan` / `ring` / `line` 同帧展开已经可用；定时 sequence 和自定义逐 tick
轨迹仍在后续阶段。

## 要解决的问题

原版 `[projectile_*]` 适合“单位炮塔朝一个目标发射一发弹”的模型，但很难自然表达：

- 没有目标单位或目标点的固定方向平射；
- 一次生成扇形、环形、线形等弹幕；
- 按固定间隔完成一组 burst；
- 让方向、数量、间距和速度读取 memory、resource 与运行时数学表达式。

这些需求目前常由一个弹体不断生成下一层弹体实现，即社区所说的“刷弹”。它增加了无意义的中间弹体、递归深度和配置复杂度，也很难保证大弹幕的性能与联机一致性。

`CustomProjectile` 的目标是提供独立、可复用的弹体定义，以及无需刷弹的原生弹幕发射流程。它不是把任意 Java 类名暴露给 INI。

## 文件和引用模型

独立定义文件：

```ini
[core]
class: CustomProjectile
name: example:plasma_fan
schemaVersion: 1

[projectile]
directDamage: 20
life: 180
speed: 5

[pattern_main]
type: fan
aimMode: direction
count: 7
centerDirection: self.dir
sweepAngle: 60
```

普通单位在 `[action_*]` 或 `[hiddenAction_*]` 中通过有命名空间的引用使用它：

```ini
emitProjectilePattern: example:plasma_fan/main
```

`[projectile]` 复用原版 `[projectile_NAME]` 的字段语法；`[pattern_*]` 的数值字段在动作
执行时以发射单位为 `self` 求值，因此可读取该单位的 memory、resources 和数学表达式。

原则：

- `class: CustomUnitMetadata` 和现有 `[projectile_*]` 的含义完全不变；
- 只有显式引用 `CustomProjectile` 的单位会启用新流程；
- 定义在加载后不可变，运行时状态属于发射任务或实际弹体；
- 跨文件引用必须包含模组命名空间，避免不同 INI 模组同名覆盖；
- 第一版不支持从 INI 指定 Java 实现类。

## 发射模型

### 瞄准模式

- `unit`：跟踪目标单位，保留原版常规语义；
- `point`：只使用确定的世界坐标，不要求目标单位；
- `direction`：只使用发射方向，创建固定目标向量或初始速度，适合平射；
- `velocity`：显式给出世界坐标速度分量，适合完全自定义轨迹起点。

所有坐标、方向和速度值应优先采用运行时表达式。发射原点应支持发射单位、炮塔、任意 UnitReference 和相对偏移。

### 一次性弹幕（第一阶段）

首批 pattern：

- `single`：单发；
- `fan`：以中心方向和总夹角均匀排布；
- `ring`：按整圆或指定角度区间排布；
- `line`：沿横向或纵向间隔排布发射原点；
- `sequence`：计划按列表组合其他 pattern，不通过弹体递归，第一阶段尚未开放。

当前共同参数包含 `count`、`centerDirection`、`startAngle`、`sweepAngle`、
`originSpacing`、`lineAngleOffset`、三个原点偏移和 `directionDistance`。
`count: 1` 时扇形的方向固定为中心方向，避免除零和左右偏置歧义。
单次展开硬上限为 1024 发；越界会明确报错而不是截断。

`interval: 0` 在同一模拟帧直接生成全部实际弹体；大于零时由轻量的发射任务保存剩余数量和下次触发时间。发射任务不是可碰撞、可绘制或可继续刷弹的 Projectile。

随机散布只允许显式的确定性种子。默认种子由发射单位运行时 ID、同步 tick、炮塔/动作索引和 pattern 内序号组合；禁止依赖系统时间或客户端本地随机数。

## API 分层

通用能力进入 Rusted Fabric API，INI Essentials 只负责语法：

1. `ProjectileSpawnContext`：发射者、可空目标单位、可空目标点、炮塔/动作来源和同步 tick。
2. `ProjectileSpawnSpec`：模板、原点、方向/目标、初始速度、延迟和序号的不可变描述。
3. `ProjectileSpawner`：把一个 spec 转成正常的原版 `Projectile`，统一调用模板应用和原版 created effects。
4. `ProjectilePatternSpec` 与 `ProjectilePatternEmitter`：把 pattern 确定性展开为一组 spawn spec 或一个发射任务。
5. `ProjectileSpawnEvents.BEFORE_SPAWN/AFTER_SPAWN`：前者可修改或取消一个实际弹体，后者拿到创建结果；事件顺序固定。
6. `ProjectileRuntimeData`：仅在确有需要时提供注册过、可序列化的每弹体数据键，不能直接挂任意对象。

现有 `ProjectileEvents`、`ProjectileCombatEvents` 和原版 `CustomProjectileTemplate` 继续作为底层生命周期与伤害入口，不重复建立另一套命中系统。`[collision]` 直接使用原版单位接触与 `hover` 寻路阻挡判定，因此撞击、伤害、爆炸和移除仍走原版流程。

## 原版接入点和当前缺口

现有映射已经覆盖：

- `CustomProjectileTemplate` 与按名称查找模板；
- `CustomUnit.createProjectileFromTemplate` 和模板应用完成事件；
- `Projectile` 的目标点、固定目标、初始无制导速度、生命周期、碰撞、伤害、序列化与移除；
- projectile 创建、更新、命中、爆炸、移除事件。

当前仍需补齐并验证：

- 游戏内覆盖原版炮塔/动作发射入口中“目标为空”的全部组合；
- 模板 created effects 对空目标和方向目标的假设；
- 固定目标位置、初始速度与弹道/激光/instant 组合的行为矩阵；
- 发射任务的存档和联机重放位置；
- 原版同步 tick、发射计数器和递归限制的稳定映射；
- 大量同帧创建时 active projectiles 列表的安全插入顺序。

## 联机与兼容规则

- 弹体伤害、轨迹或数量发生变化时属于 `gameplay_synced`，所有参与模拟的客户端必须使用相同 API、INI Essentials 协议和定义哈希；
- 只有纹理、颜色和纯客户端特效变化时才可标记为 `client_only`；
- 同一 pattern 的展开顺序固定为 INI 声明顺序与递增序号；
- 数值表达式在发射时求值一次，除非字段明确声明为逐 tick 动态；
- 数量、递归、单帧生成量和活动弹体数必须有硬上限，并给出清楚的 INI 错误，而不是静默截断；
- 使用 gameplay `CustomProjectile` 的房间不能允许完全原版客户端加入，这与仅服务端逻辑不同，因为每个客户端都要进行弹体模拟。

## 实施进度

1. 已完成：API 单发 `ProjectileSpawner`，支持 `unit`、`point` 与 `direction`。
2. 已完成：纯数据且有契约测试的 `fan`、`ring`、`line` 展开器。
3. 已完成：独立 `class: CustomProjectile` 解析、命名空间注册表和完整 pattern 引用校验。
4. 已完成代码路径：Action 同帧 pattern 直接创建真实弹体，不生成中间母弹；待游戏内验收。
5. 已完成代码路径：Action 与炮塔生成的主弹/额外弹统一应用动态 `[collision]`，包括地形转换爆炸和按层、移动类型、高度、标签的单位过滤；待游戏内碰撞矩阵验收。
6. 已完成代码路径：原版 `[effect_NAME]` 引用和以弹体实时坐标为锚点的 `[decal_NAME]` 绘制；Decal 逻辑上下文仍为发射单位。
7. 加入带 interval 的发射任务及其存档/联机状态。
8. 已完成代码路径：原版炮塔精确发射织入，支持有序条件 Pattern 切换，且写有无条件 `projectilePattern` 时可省略原版 projectile；保留炮口效果、声音、后坐力、计数器和 onShoot Action。待游戏内覆盖验证。
9. 最后再评估自定义逐 tick 运动、碰撞过滤和高级生命周期回调。

第一轮验收基准：用一个动作或炮塔直接发射 30 发固定方向扇形弹幕，场上只出现 30 个实际弹体，不存在用于刷弹的母弹；PC 与 Android 的弹体顺序、角度、命中和回放结果一致。
