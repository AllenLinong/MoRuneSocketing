# MoRuneSocketing Wiki 文档

## 目录

1. [插件介绍](#插件介绍)
2. [安装指南](#安装指南)
3. [快速开始](#快速开始)
4. [命令系统](#命令系统)
5. [权限配置](#权限配置)
6. [配置文件说明](#配置文件说明)
7. [符文系统](#符文系统)
8. [符文拆卸粉](#符文拆卸粉)
9. [常见问题](#常见问题)

***

## 插件介绍

### 插件简介

MoRuneSocketing 是一款 Minecraft 服务器插件，提供完整的符文镶嵌系统。玩家可以将符文镶嵌到武器、盔甲和工具上，获得强大的属性加成。

### 主要特性

- 支持附魔符文和属性符文（buff符文）
- 可配置的镶嵌成功率系统
- 内置 FoliaLib，原生支持 Paper / Folia / Spigot 多核心
- 支持第三方附魔插件（FotiaEnchantment、EcoEnchants 等）
- 符文拆卸粉功能，可拆卸已镶嵌的buff符文
- 丰富的经济系统集成
- 多语言支持（中文、英文）
- 防止符文被放入合成器和漏斗

### 支持的服务器版本

- **Paper 1.21.1+** ✅
- **Folia 1.21.1+** ✅（通过 FoliaLib 原生支持）
- **Spigot 1.21.1+** ✅
- **PurPur 及其他 Paper 分支 1.21.1+** ✅
- **Java 要求：JDK 21+**

> ⚠️ 1.20.x 及以下版本不再兼容，如需旧版本支持请联系作者。

***

## 安装指南

### 安装步骤

1. **下载插件**
   - 从构建产物获取 `MoRuneSocketing-1.0.6-Release.jar`
2. **放置插件**
   - 将 jar 文件放入服务器的 `plugins` 目录
3. **启动服务器**
   - 启动服务器，插件将自动生成配置文件
4. **配置插件**
   - 根据需要修改 `config.yml`、`runes.yml` 等配置文件
5. **重载配置**
   - 使用 `/mrs reload` 命令重载配置（需要管理员权限）

***

## 快速开始

### 基础使用流程

1. **打开镶嵌菜单**
   ```
   /mrs menu
   ```
2. **放入装备**
   - 将要镶嵌符文的装备（如武器、盔甲）放入装备槽
3. **放入符文**
   - 将符文放入符文槽
4. **点击镶嵌**
   - 点击"确定"按钮开始镶嵌
5. **完成**
   - 镶嵌成功后，装备上会显示符文属性

***

## 命令系统

### 玩家命令

| 命令                           | 说明        | 权限                      |
| ---------------------------- | --------- | ----------------------- |
| `/mrs`                       | 显示帮助信息    | -                       |
| `/mrs menu`                  | 打开符文镶嵌菜单  | `morunesocketing.use`   |
| `/mrs analyze`               | 分析手中附魔物品  | -                       |
| `/mrs give <玩家> <符文ID> [数量]` | 给予玩家符文    | `morunesocketing.admin` |
| `/mrs giveremover <玩家> [数量]` | 给予玩家符文拆卸粉 | `morunesocketing.admin` |

### 管理员命令

| 命令                           | 说明        | 权限                      |
| ---------------------------- | --------- | ----------------------- |
| `/mrs reload`                | 重载插件配置    | `morunesocketing.admin` |
| `/mrs setrate <成功率>`         | 设置随机成功率   | `morunesocketing.admin` |
| `/mrs setinterval <时间> [单位]` | 设置成功率刷新间隔 | `morunesocketing.admin` |

### 命令示例

```
/mrs menu                    # 打开菜单
/mrs analyze                 # 分析手中附魔物品的命名空间和ID
/mrs give Steve sharpness 3  # 给 Steve 3个锋利符文
/mrs giveremover Steve 5     # 给 Steve 5个符文拆卸粉
/mrs reload                  # 重载配置
/mrs setrate 80              # 设置成功率为80%
/mrs setinterval 30m         # 设置刷新间隔为30分钟
```

***

## 权限配置

### 权限节点

| 权限节点                    | 说明       | 默认值  |
| ----------------------- | -------- | ---- |
| `morunesocketing.use`   | 使用符文镶嵌功能 | true |
| `morunesocketing.admin` | 管理员权限    | op   |

### 权限设置示例

**允许所有玩家使用镶嵌功能：**

```yaml
permissions:
  morunesocketing.use:
    description: 使用符文镶嵌功能
    default: true
```

**仅管理员可用：**

```yaml
permissions:
  morunesocketing.admin:
    description: 管理员权限
    default: op
```

***

## 配置文件说明

### config.yml

主配置文件，控制插件的全局设置。

```yaml
# 镶嵌成功率 (%)(0-100)
success-rate: 100

# 镶嵌费用
socketing:
  economy:
    enabled: false
    cost: 1000

# 随机成功率设置
features:
  random-success-rate:
    enabled: false
    min-rate: 50
    max-rate: 100
    refresh-interval: "1h"

# 可镶嵌物品列表
socketable-items:
  - "DIAMOND_SWORD"
  - "DIAMOND_HELMET"
  - "NETHERITE_*"
  - "*_SWORD"
  - "*_AXE"

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

符文配置文件，定义所有可用的符文。

```yaml
# 锋利符文（附魔类型）
sharpness:
  name: "§6锋利符文"
  material: "PAPER"
  enchantment: "DAMAGE_ALL"
  enchant-level: 1
  description:
    - "§7镶嵌到武器上"
    - "§7增加锋利附魔"

# 幸运符文（Buff类型）
fortune:
  name: "§a幸运符文"
  material: "PAPER"
  buff: "LUCK"
  buff-level: 1
  description:
    - "§7镶嵌到装备上"
    - "§7增加幸运属性"
  runes-items:  # 可选，特定物品限制
```

### menu-config.yml

菜单配置文件，控制镶嵌菜单的外观和布局。

```yaml
Title: "§6§l符文镶嵌系统"
Shape:
  - 'XXXXXXXXX'
  - 'XEXXRXXIX'
  - 'XXXXXXXXX'

Buttons:
  'X':  # 边框
    material: "BLACK_STAINED_GLASS_PANE"
    name: " "
    can-place: false
    can-take: false
    is-static: true

  'E':  # 装备槽
    material: "AIR"
    name: "§6§l装备槽"
    lore:
      - "§7将需要镶嵌符文的装备放入此槽"
    can-place: true
    can-take: true
    is-equipment-slot: true

  'R':  # 符文槽
    material: "AIR"
    name: "§6§l符文槽"
    lore:
      - "§7将符文放入此槽"
    can-place: true
    can-take: true
    is-rune-slot: true

  'I':  # 镶嵌按钮
    material: "ANVIL"
    name: "§a§l确定"
    lore:
      - "§7点击开始镶嵌/拆卸符文"
      - "§7成功率: §f%success-rate%"
      - "§7镶嵌费用: §e%cost% 金币"
      - "§7拆卸成功率百分百"
      - "§7拆卸符文返还概率：§f%return-chance%%"
    can-place: false
    can-take: false
    glow: true
    is-insert-button: true
```

### 语言文件

#### zh\_CN.yml（中文）

插件的中文语言文件。

#### en\_US.yml（英文）

插件的英文语言文件。

***

## 符文系统

### 符文类型

#### 附魔符文

- 需要目标装备已有相应附魔
- 直接增强已有的附魔等级
- 支持原版附魔和第三方插件附魔
- 示例：锋利、耐久、效率等

#### Buff符文

- 直接为装备添加属性
- 提供额外的能力加成
- 示例：力量、幸运、速度、抗火等

### 附魔命名空间支持（v1.0.6+）

插件支持 `命名空间:附魔ID` 格式，可以兼容第三方附魔插件：

| 示例 | 来源 |
|------|------|
| `sharpness` | 原版附魔（默认 `minecraft:` 命名空间） |
| `minecraft:sharpness` | 等价于 `sharpness` |
| `fotia:water_mastery` | FotiaEnchantment 插件附魔 |
| `eco:telekinesis` | EcoEnchants 插件附魔 |

#### 如何获取第三方附魔的完整ID

使用 `/mrs analyze` 命令，手持附魔物品执行即可看到所有附魔的完整 `namespace:key`：

```
/mrs analyze
```

输出示例：
```
§6===== 手中物品附魔分析 =====
§afotia:water_mastery §7→ §e等级: §f1
```

直接将 `fotia:water_mastery` 填入 `runes.yml` 的 `enchantment:` 配置即可。

### 镶嵌规则

1. 装备必须在 `socketable-items` 列表中
2. 附魔符文需要装备已有对应附魔
3. Buff符文可以直接镶嵌
4. 镶嵌成功率受配置的成功率影响
5. 每次镶嵌会消耗相应金币（如果启用经济系统）

### 可镶嵌物品通配符

| 通配符           | 说明          | 示例                |
| ------------- | ----------- | ----------------- |
| `*`           | 匹配任意字符      | `*_SWORD` 匹配所有剑   |
| `NETHERITE_*` | 匹配下界合金开头的物品 | NETHERITE\_HELMET |

***

## 符文拆卸粉

### 功能说明

符文拆卸粉是一种特殊物品，可以拆卸装备上的Buff类型符文，并有可能返还同类型的符文。

### 获取方式

```
/mrs giveremover <玩家> [数量]
```

### 使用方法

1. 打开镶嵌菜单：`/mrs menu`
2. 将带有Buff符文的装备放入装备槽
3. 将符文拆卸粉放入符文槽
4. 点击"确定"按钮
5. 系统会自动拆卸装备上的Buff符文

### 配置参数

| 参数                  | 说明      | 默认值     |
| ------------------- | ------- | ------- |
| `item`              | 拆卸粉物品ID | SUGAR   |
| `name`              | 拆卸粉名称   | §b符文拆卸粉 |
| `custom-model-data` | 自定义模型数据 | 0       |
| `lore`              | 拆卸粉描述   | -       |
| `return-chance`     | 返还概率(%) | 100     |

### 返还规则

- 拆卸时会根据 `return-chance` 概率返还符文
- 返还的符文类型和等级与原装备上的相同
- 例如：装备有"力量1级"和"幸运2级"，拆卸后会返还对应符文

***

## 常见问题

### Q: 符文无法放入合成器

**A:** 这是插件的正常功能，符文被设计为不能通过合成器合成。

### Q: 漏斗可以放入符文吗

**A:** 不可以，插件会阻止漏斗将符文放入合成器。

### Q: 切换装备后Buff消失

**A:** 请确保在服务器的plugins目录中只保留一个版本的MoRuneSocketing插件，多个版本可能导致冲突。

### Q: 如何自定义符文

**A:** 编辑 `runes.yml` 配置文件，添加新的符文定义。

### Q: 镶嵌成功率如何设置

**A:** 在 `config.yml` 中修改 `success-rate` 值（0-100）。

### Q: 支持哪些语言

**A:** 目前支持中文（zh\_CN）和英文（en\_US）。

### Q: 如何添加第三方插件的附魔作为符文

**A:** 使用 `/mrs analyze` 命令分析手中附魔物品，获取完整的 `namespace:key`（如 `fotia:water_mastery`），然后填入 `runes.yml` 的 `enchantment:` 配置即可。

### Q: 支持 Folia 核心吗

**A:** 完全支持。插件内置 FoliaLib，运行时会自动检测服务端类型并使用对应的调度器，在 Folia 环境下走区域调度，不会拖全局 tick。

### Q: 为什么不支持 1.13 了

**A:** v1.0.6 升级到了 Paper API 1.21.1，旧版本 API 中的附魔常量已被移除，无法向下兼容。如需 1.13 支持请联系作者获取旧版本。

<br />

***

## 更新记录

### v1.0.6-Release

- 🚀 升级 Paper API 至 1.21.1，最低要求 JDK 21
- 🚀 集成 FoliaLib，原生支持 Folia 核心
- ✨ 附魔解析支持 `命名空间:附魔ID` 格式，兼容第三方附魔插件
- ✨ 新增 `/mrs analyze` 命令，分析手中附魔物品的命名空间和ID
- 🔧 修复所有过时的附魔 API 常量名称

### v1.0.5-Release

- ✨ 新增符文拆卸粉功能
- ✨ 新增 `/mrs giveremover` 命令
- 🔧 修复 Buff 切换消失问题
- 🔧 修复拆卸后装备残留 Lore 问题
- 🔧 修复语言文件未匹配问题

### v1.0.4 及更早

- 初始符文镶嵌系统
- 菜单系统与成功率配置
- 经济系统集成
- 多语言支持

## 联系方式

- 作者：Allen\_Linong
- QQ：1422163791

