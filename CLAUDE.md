# Villager Trade Refresher — Fabric Mod

自动刷新村民交易的 Fabric Mod。玩家打开自定义 GUI 选择目标交易后，MOD 自动操控村民刷新交易列表直到目标出现。

- **Minecraft**：26.1.2
- **API**：Fabric（`~26.1`）
- **Java**：>= 25
- **Fabric Loader**：>= 0.19.0

**官方文档**：https://docs.fabricmc.net/develop/  
**示例 Mod**：https://github.com/FabricMC/fabric-docs/tree/main/reference/latest

---

## 项目结构

```
src/
├── main/
│   ├── java/com/aye10032/autotradefiltering/
│   │   ├── AutoTradeFiltering.java    # 主入口点（ModInitializer）
│   │   └── network/                   # 客户端→服务端通信包
│   └── resources/
│       ├── fabric.mod.json
│       └── assets/auto-trade-filtering/
│           └── lang/zh_cn.json        # 本地化文本
└── client/
    ├── java/com/aye10032/autotradefiltering/client/
    │   ├── AutoTradeFilteringClient.java         # 客户端入口点
    │   ├── gui/                       # 自定义 Screen / Widget
    │   └── mixin/                     # 客户端 Mixin
    └── resources/
```

---

## 核心功能对应文档

### GUI（自定义交易选择界面）
- 自定义 Screen：https://docs.fabricmc.net/develop/rendering/gui/custom-screens
- 自定义 Widget：https://docs.fabricmc.net/develop/rendering/gui/custom-widgets
- 文本与翻译（本地化）：https://docs.fabricmc.net/develop/text-and-translations

> GUI 代码必须位于 `src/client/java`，严禁在服务端路径中引用任何渲染/Screen 类。

### 网络通信（客户端→服务端触发刷新）
- 网络文档：https://docs.fabricmc.net/develop/networking

> 玩家在客户端 GUI 选定交易后，通过自定义网络包通知服务端执行刷新逻辑；服务端不能直接调用客户端 GUI。

### Mixin（修改村民行为 / 交易逻辑）
- Mixin 字节码：https://docs.fabricmc.net/develop/mixins/bytecode
- Access Widening（访问私有字段）：https://docs.fabricmc.net/develop/class-tweakers/access-widening

> 刷新村民交易通常需要 Mixin 注入 `MerchantEntity` 或 `VillagerEntity` 的交易更新逻辑，或通过 Access Widener 暴露内部字段。

### 按键绑定（可选，快捷键打开 GUI）
- 按键绑定：https://docs.fabricmc.net/develop/key-mappings

### 事件（监听玩家与村民交互）
- 事件系统：https://docs.fabricmc.net/develop/events

---

## 构建命令

```bash
./gradlew genSources   # 生成反编译源码（IDE 跳转用）
./gradlew runClient    # 启动开发客户端
./gradlew build        # 构建 JAR，产物在 build/libs/
```

---

## 注意事项

- 刷新交易逻辑运行在**服务端**；GUI 渲染运行在**客户端**——两者通过网络包通信，不可混用。
- 使用 Mojang 官方映射（unobfuscated），类名以官方名称为准。