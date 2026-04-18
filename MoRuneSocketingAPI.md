# MoRuneSocketing API 文档

## 插件信息

- **插件名称**：MoRuneSocketing
- **版本**：1.0.5-Release
- **作者**：Allen\_Linong
- **联系方式**：QQ 1422163791

## 插件简介

MoRuneSocketing 是一款 Minecraft 服务器插件，提供符文镶嵌系统，支持附魔符文和属性符文（buff符文）的镶嵌与管理。

## 主要功能

- 符文镶嵌：玩家可以将符文镶嵌到装备上
- 符文拆卸：使用符文拆卸粉拆卸装备上的buff符文
- 成功率系统：可配置的镶嵌成功率
- 多语言支持：支持中文和英文

## API 概述

### 主类方法

#### `getInstance()`

获取插件单例实例。

```java
public static MoRuneSocketing getInstance()
```

#### `getPluginVersion()`

获取插件版本号。

```java
public static String getPluginVersion()
```

### 数据管理

#### `getDataManager()`

获取数据管理器实例。

```java
public DataManager getDataManager()
```

#### `getDatabaseManager()`

获取数据库管理器实例。

```java
public DatabaseManager getDatabaseManager()
```

### 经济系统

#### `getEconomyManager()`

获取经济管理器实例。

```java
public EconomyManager getEconomyManager()
```

### 语言系统

#### `getLanguageManager()`

获取语言管理器实例。

```java
public LanguageManager getLanguageManager()
```

### 配置管理

#### `reloadPlugin()`

重载插件配置。

```java
public void reloadPlugin()
```

#### `getCurrentSuccessRate()`

获取当前镶嵌成功率。

```java
public int getCurrentSuccessRate()
```

## 配置文件结构

### config.yml

```yaml
# 镶嵌成功率 (%)
success-rate: 100

# 镶嵌费用
socketing:
  economy:
    enabled: false
    cost: 1000

# 符文拆卸粉配置
rune-remover:
  item: "SUGAR"
  name: "§b符文拆卸粉"
  custom-model-data: 0
  lore:
    - "§7功能: §e拆卸已镶嵌BUFF符文的装备上的符文"
    - "§7效果: §a将符文从装备上拆下，装备不损坏"
  return-chance: 100
```

### runes.yml

```yaml
# 符文ID
符文ID:
  name: "符文名称"
  material: "物品材质"
  # 附魔类型（非buff符文使用）
  enchantment: "附魔名称"
  enchant-level: 1
  # Buff类型（buff符文使用）
  buff: "buff标识"
  buff-level: 1
  # 特定物品限制（可选，留空则使用socketable-items）
  runes-items:
```

## 命令列表

### `/mrs` 或 `/mrs help`

显示帮助信息。

### `/mrs menu`

打开符文镶嵌菜单。

### `/mrs give <玩家> <符文ID> [数量]`

给予玩家指定符文。

### `/mrs giveremover <玩家> [数量]`

给予玩家符文拆卸粉。

### `/mrs reload`

重载插件配置（需要管理员权限）。

### `/mrs setrate <成功率>`

设置随机成功率（需要管理员权限）。

### `/mrs setinterval <时间> [单位]`

设置随机成功率刷新间隔（需要管理员权限）。

## 权限列表

| 权限节点                    | 说明       | 默认值  |
| ----------------------- | -------- | ---- |
| `morunesocketing.use`   | 使用符文镶嵌功能 | true |
| `morunesocketing.admin` | 管理员权限    | op   |

## 事件监听

### RuneListener

- `CraftItemEvent`：阻止符文放入合成器
- `InventoryClickEvent`：处理菜单点击事件
- `InventoryMoveItemEvent`：阻止漏斗将符文放入合成器
- `PlayerSwapHandItemsEvent`：处理玩家切换主副手事件，检查buff切换

### RuneManager

- `handleInsertButtonAction`：处理符文镶嵌/拆卸逻辑
- `removeBuffsFromItem`：处理buff符文拆卸
- `createRuneFromBuff`：根据buff创建符文物品

## NBT 数据存储

插件使用 PersistentDataContainer 存储以下数据：

| NBT Key              | 说明         |
| -------------------- | ---------- |
| `custom_enchantment` | 自定义附魔数据    |
| `custom_buff`        | 自定义buff数据  |
| `buff_display_map`   | buff显示名称映射 |
| `rune_type`          | 符文类型       |
| `no_craft`           | 禁止合成标记     |
| `menu_button`        | 菜单按钮标识     |

## 开发依赖

在 Maven 项目中添加以下依赖：

```xml
<dependency>
    <groupId>com.morunesocketing</groupId>
    <artifactId>MoRuneSocketing</artifactId>
    <version>1.0.5-Release</version>
    <scope>provided</scope>
</dependency>
```

## 版本历史

- **1.0.5-Release**：新增符文拆卸粉功能
- **1.0.4-Release**：修复语言文件问题
- **1.0.3-Release**：新增符文特定物品镶嵌限制
- **1.0.2-Release**：修复buff切换消失问题
- **1.0.1-Release**：修复漏斗放入符文问题
- **1.0.0-Release**：初始版本

## 联系方式

- 作者：Allen\_Linong
- QQ：1422163791

