# MoRuneSocketing 构建教程

## 插件信息
- **插件名称**：MoRuneSocketing
- **版本**：1.0.6-Release
- **作者**：Allen_Linong

## 环境要求

### Java 环境
- **JDK 版本**：21 或更高版本
- **下载地址**：https://adoptium.net/

### Maven 构建工具
- **Maven 版本**：3.6.0 或更高版本
- **下载地址**：https://maven.apache.org/download.cgi

### 开发工具（可选）
- **IDE**：IntelliJ IDEA 或 Eclipse
- **推荐使用 IntelliJ IDEA**

## 构建步骤

### 方法一：使用命令行构建

#### 1. 克隆或下载源码
```bash
git clone <仓库地址>
cd MoRuneSocketing
```

#### 2. 执行构建命令
在项目根目录下执行以下命令：
```bash
mvn clean package -DskipTests
```

#### 3. 获取构建产物
构建完成后，插件 jar 文件位于：
```
target/MoRuneSocketing-1.0.6-Release.jar
```

### 方法二：使用 IDE 构建

#### IntelliJ IDEA

1. **打开项目**
   - 选择 File -> Open
   - 选择项目文件夹
   - 点击 OK

2. **配置 Maven**
   - 打开右侧 Maven 面板
   - 点击刷新按钮同步项目

3. **执行构建**
   - 打开 Maven 面板
   - 选择 Lifecycle -> package
   - 双击 package 执行构建

4. **获取构建产物**
   - 右键点击 target 目录下的 jar 文件
   - 选择 Open In -> Explorer
   - 或直接在 `target/MoRuneSocketing-1.0.6-Release.jar` 获取

#### Eclipse

1. **导入项目**
   - 选择 File -> Import
   - 选择 Maven -> Existing Maven Projects
   - 选择项目文件夹

2. **执行构建**
   - 右键点击项目
   - 选择 Run As -> Maven build...
   - 在 Goals 中输入：`clean package -DskipTests`
   - 点击 Run

3. **获取构建产物**
   - 刷新项目
   - 在 target 目录下找到 jar 文件

## Maven 命令详解

### 常用命令

| 命令 | 说明 |
|------|------|
| `mvn clean` | 清理 target 目录 |
| `mvn compile` | 编译源码 |
| `mvn package` | 打包项目 |
| `mvn install` | 安装到本地仓库 |
| `mvn clean package -DskipTests` | 清理并打包，跳过测试 |

### 参数说明
- `-DskipTests`：跳过测试执行
- `-Dmaven.test.skip=true`：跳过测试编译和执行

## 项目结构

```
MoRuneSocketing/
├── pom.xml                    # Maven 配置文件
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── morunesocketing/
│       │           ├── MoRuneSocketing.java    # 主类
│       │           ├── RuneCommand.java         # 命令处理
│       │           ├── RuneListener.java        # 事件监听
│       │           ├── RuneManager.java         # 符文管理
│       │           ├── DataManager.java         # 数据管理
│       │           ├── LanguageManager.java     # 语言管理
│       │           ├── database/                # 数据库相关
│       │           ├── economy/                 # 经济系统
│       │           └── util/                    # 工具类（含SchedulerUtils）
│       │       └── tcoded/
│       │           └── folialib/                # FoliaLib 多核心调度器
│       └── resources/
│           ├── plugin.yml        # 插件描述文件
│           ├── config.yml        # 主配置文件
│           ├── runes.yml         # 符文配置文件
│           ├── menu-config.yml   # 菜单配置文件
│           └── lang/             # 语言文件目录
│               ├── zh_CN.yml     # 中文语言文件
│               └── en_US.yml     # 英文语言文件
└── target/                     # 构建输出目录
```

## 配置文件说明

### pom.xml
Maven 项目配置文件，定义了项目依赖和构建插件。

### src/main/resources/
插件资源配置目录，包含所有运行时需要的配置文件。

## 常见问题

### Q: 构建时报错 "Java version mismatch"
**A:** 请确保已安装 JDK 21 或更高版本，并正确配置 JAVA_HOME 环境变量。

### Q: 找不到依赖包
**A:** 执行 `mvn clean install -U` 强制更新依赖。

### Q: IDE 中无法识别项目
**A:** 确保已安装 Maven 插件，并在 IDE 中重新导入项目。

## 下一步

构建完成后，请参考以下文档：
- [API.md](API.md) - API 文档
- [Wiki.md](Wiki.md) - 使用教程
- [LICENSE.md](LICENSE.md) - 授权协议

## 联系方式
- 作者：Allen_Linong
- QQ：1422163791
