# Claude Code Java

<p align="center">
  <img src="https://img.shields.io/badge/Java-21+-ED8B00?style=flat-square&logo=openjdk&logoColor=white" alt="Java">
  <img src="https://img.shields.io/badge/Maven-3.9+-C71A36?style=flat-square&logo=apache-maven&logoColor=white" alt="Maven">
  <img src="https://img.shields.io/badge/License-MIT-green?style=flat-square" alt="License">
</p>

> Claude Code 的 Java 版本实现，一比一复刻自 TypeScript/Bun 技术栈

## 项目概述

Claude Code Java 是 [Claude Code](https://docs.anthropic.com/zh-CN/docs/claude-code) 的纯 Java 实现，保持与原版功能完整性和行为一致性。该项目采用 Java 21 开发，充分利用现代 Java 特性（Virtual Threads、Records、Sealed Classes 等），通过 JLine3 构建终端 UI 层。

### 核心特性

- **完整的 AI 对话能力** — 与 Claude AI 进行多轮交互，支持工具调用、消息压缩、会话恢复
- **54 个内置工具** — 文件操作、Shell 命令、代码搜索、Web 访问、MCP 集成等
- **102 个斜杠命令** — 快速执行常用操作（commit、diff、review、compact 等）
- **MCP 协议支持** — 连接 MCP 服务器扩展 AI 能力
- **IDE 桥接** — 支持 VSCode、Neovim、JetBrains 等主流 IDE
- **LSP 集成** — Language Server Protocol 协议支持
- **终端 UI** — 完整的 Vim 模式、Markdown 渲染、代码高亮
- **会话持久化** — JSONL 格式，与 TypeScript 版本兼容

## 技术架构

```
┌─────────────────────────────────────────────────────────────┐
│                    claude-code-cli (Picocli)                │
│                      应用入口与 CLI 解析                      │
├─────────────────────────────────────────────────────────────┤
│                    claude-code-ui (JLine3)                  │
│           终端 UI：REPL、消息渲染、输入、Markdown              │
├──────────────────┬──────────────────┬───────────────────────┤
│  claude-code-    │  claude-code-    │  claude-code-         │
│  commands        │  tools           │  services             │
│  斜杠命令系统     │  工具系统         │  服务层                │
├──────────────────┴──────────────────┴───────────────────────┤
│                    claude-code-core                         │
│  QueryEngine │ Store │ Message │ Task │ Permission │ Config │
├──────────────────┬──────────────────┬───────────────────────┤
│  claude-code-api │  claude-code-mcp │  claude-code-bridge   │
│  Anthropic API   │  MCP 客户端       │  IDE 桥接             │
├──────────────────┴──────────────────┴───────────────────────┤
│  claude-code-session │ claude-code-permissions              │
│  会话持久化           │ 权限引擎                              │
├─────────────────────────────────────────────────────────────┤
│                    claude-code-utils                        │
│           通用工具：文件、Shell、Git、加密、格式化等            │
└─────────────────────────────────────────────────────────────┘
```

## 模块结构

| 模块 | 描述 | 文件数 |
|------|------|--------|
| `claude-code-utils` | 通用工具类：文件操作、Shell、Git、加密等 | ~50 |
| `claude-code-core` | 核心模型、QueryEngine、消息系统、状态管理 | ~80 |
| `claude-code-api` | Anthropic API 客户端（多 SDK 适配） | ~30 |
| `claude-code-permissions` | 权限系统（allow/deny/ask） | ~20 |
| `claude-code-tools` | 54 个内置工具实现 | ~150 |
| `claude-code-commands` | 102 个斜杠命令 | ~120 |
| `claude-code-mcp` | Model Context Protocol 集成 | ~50 |
| `claude-code-bridge` | IDE 桥接（VSCode、Neovim、JetBrains） | ~80 |
| `claude-code-session` | 会话管理（JSONL 持久化） | ~20 |
| `claude-code-services` | 服务层：compact、hooks、memory 等 | ~100 |
| `claude-code-ui` | 终端 UI：渲染器、对话框、菜单、Vim 模式 | ~200 |
| `claude-code-lsp` | Language Server Protocol 集成 | ~30 |
| `claude-code-cli` | CLI 入口（Picocli） | ~20 |
| `claude-code-app` | 应用打包与分发 | ~10 |

## 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | Java 21+ (Records, Sealed Classes, Pattern Matching, Virtual Threads) |
| 构建 | Maven 3.9+ |
| JSON | Jackson 2.17.2 |
| 终端 UI | JLine3 3.26.3 |
| CLI | Picocli 4.7.6 |
| Markdown | commonmark-java 0.22.0 |
| 日志 | SLF4J 2.0.13 + Logback 1.5.8 |
| 工具库 | Guava 33.2.1, Commons Lang3 3.14.0, Commons IO 2.16.1 |
| LSP | Eclipse LSP4J 0.24.0 |
| 测试 | JUnit 5.10.3, jqwik 1.9.0 (属性测试) |

## 快速开始

### 环境要求

- **Java**: 21 或更高版本
- **Maven**: 3.9 或更高版本
- **API Key**: Anthropic API Key（设置 `ANTHROPIC_API_KEY` 环境变量）

### 安装 Java 21（如果需要）

```bash
# macOS with SDKMAN
sdk install java 21.0.2-tem

# Ubuntu/Debian
sudo apt install openjdk-21-jdk

# Windows (Chocolatey)
choco install openjdk21
```

### 构建项目

```bash
# 克隆项目
git clone https://github.com/your-username/claude-code-java.git
cd claude-code-java

# 全量构建
mvn clean install

# 跳过测试快速构建
mvn clean install -DskipTests

# 单模块构建
mvn -pl claude-code-core clean install
```

### 运行应用

```bash
# 使用 Maven 运行
mvn -pl claude-code-cli exec:java

# 或者运行打包后的 JAR
java -jar claude-code-app/target/claude-code-java-*-jar-with-dependencies.jar
```

### Docker 运行

```bash
# 构建镜像
docker build -t claude-code-java .

# 运行容器
docker run -it \
  -e ANTHROPIC_API_KEY=$ANTHROPIC_API_KEY \
  -v $(pwd):/workspace \
  claude-code-java
```

## 配置

### 环境变量

| 变量 | 描述 | 必需 |
|------|------|------|
| `ANTHROPIC_API_KEY` | Anthropic API 密钥 | 是 |
| `ANTHROPIC_BASE_URL` | API 基础 URL（可选，用于代理） | 否 |
| `CLAUDE_CONFIG_DIR` | 配置目录（默认 `~/.claude`） | 否 |

### 配置文件

```json
// ~/.claude/settings.json
{
  "model": "claude-sonnet-4",
  "maxTokens": 8192,
  "permissionMode": "ask",
  "disableBrowserTools": false
}
```

### 项目级配置

在项目根目录创建 `CLAUDE.md` 文件，定义项目级指令：

```markdown
# CLAUDE.md

- 你是一个 Java 专家
- 使用 Maven 作为构建工具
- 遵循 Google Java Style Guide
- 每次提交前运行测试
```

## 使用指南

### 基本命令

| 命令 | 描述 |
|------|------|
| `/help` | 显示帮助信息 |
| `/exit` | 退出程序 |
| `/clear` | 清屏 |
| `/model` | 切换模型 |
| `/compact` | 压缩会话上下文 |
| `/cost` | 显示会话费用统计 |

### Git 命令

| 命令 | 描述 |
|------|------|
| `/commit` | 创建 Git 提交 |
| `/diff` | 显示工作区变更 |
| `/review` | 代码审查 |
| `/branch` | 管理分支 |

### 开发命令

| 命令 | 描述 |
|------|------|
| `/doctor` | 检查环境配置 |
| `/status` | 显示当前状态 |
| `/config` | 编辑配置 |
| `/mcp` | 管理 MCP 服务器 |

### 内置工具

#### 文件操作
- `BashTool` — 执行 Shell 命令
- `FileReadTool` — 读取文件内容
- `FileWriteTool` — 写入文件
- `FileEditTool` — 编辑文件
- `GlobTool` — 文件模式搜索
- `GrepTool` — 代码内容搜索

#### Web 工具
- `WebFetchTool` — 获取网页内容
- `WebSearchTool` — 网络搜索
- `WebBrowserTool` — 浏览器自动化

#### AI 工具
- `AgentTool` — 创建子代理
- `MCPTool` — 调用 MCP 工具
- `SkillTool` — 执行技能

### Vim 模式

终端 UI 支持完整的 Vim 输入模式：

| 模式 | 快捷键 | 描述 |
|------|--------|------|
| INSERT | `i` | 插入模式 |
| NORMAL | `Esc` | 普通模式 |
| VISUAL | `v` | 可视模式 |
| COMMAND | `:` | 命令模式 |

**常用命令：**

```
:w              保存
:q              退出
:wq             保存并退出
dd              删除行
yy              复制行
p               粘贴
u               撤销
Ctrl+r          重做
3j              向下移动3行
/widget         搜索 "widget"
n               下一个搜索结果
```

## 项目结构

```
claude-code-java/
├── CLAUDE.md                 # 开发指南
├── design.md                 # 技术设计文档
├── requirements.md           # 需求规格书
├── PERFORMANCE.md            # 性能优化文档
├── pom.xml                   # 父 POM
│
├── claude-code-utils/        # 通用工具
│   └── src/main/java/
│       └── com/claudecode/utils/
│
├── claude-code-core/         # 核心模块
│   └── src/main/java/
│       └── com/claudecode/core/
│           ├── engine/        # QueryEngine、流式客户端
│           ├── message/       # 消息类型体系
│           ├── state/        # 状态管理
│           └── task/         # 任务系统
│
├── claude-code-api/          # API 客户端
├── claude-code-permissions/  # 权限系统
├── claude-code-tools/       # 工具系统
│   └── src/main/java/
│       └── com/claudecode/tools/
│           └── impl/         # 工具实现
│
├── claude-code-commands/     # 命令系统
│   └── src/main/java/
│       └── com/claudecode/commands/
│           └── impl/         # 命令实现
│
├── claude-code-mcp/          # MCP 集成
├── claude-code-bridge/       # IDE 桥接
├── claude-code-session/      # 会话管理
├── claude-code-services/     # 服务层
├── claude-code-ui/           # 终端 UI
│   └── src/main/java/
│       └── com/claudecode/ui/
│           ├── renderer/     # 渲染器
│           ├── dialog/       # 对话框
│           ├── vim/          # Vim 模式
│           └── mcp/           # MCP 菜单
│
├── claude-code-lsp/          # LSP 集成
├── claude-code-cli/          # CLI 入口
└── claude-code-app/          # 打包配置
    └── src/main/resources/
        └── META-INF/native-image/  # GraalVM 配置
```

## 测试

### 运行所有测试

```bash
mvn test
```

### 运行特定模块测试

```bash
mvn -pl claude-code-core test
mvn -pl claude-code-session test
```

### 属性测试（Property-Based Testing）

项目使用 jqwik 进行属性测试，验证核心属性的正确性：

| ID | 属性 | 描述 |
|----|------|------|
| CP-1 | API 协议正确性 | 消息序列化/反序列化一致性 |
| CP-2 | 工具执行确定性 | 相同输入产生相同输出 |
| CP-3 | 权限决策一致性 | 相同上下文产生相同决策 |
| CP-4 | 消息兼容性 | 与 TypeScript 版本兼容 |
| CP-5 | 状态管理不变性 | 状态更新保持有效 |
| CP-6 | 任务生命周期正确性 | 任务状态转换正确 |
| CP-7 | 会话压缩安全性 | 压缩后信息完整 |

## 性能优化

### 目标

| 指标 | 目标值 |
|------|--------|
| JVM 启动时间 | < 2 秒 |
| GraalVM 启动时间 | < 500ms |
| 稳定状态内存 | < 256MB |
| 长会话（1000+ 轮） | 无内存增长 |

### 优化策略

- 使用 **AppCDS** 加速 JVM 启动
- 延迟加载服务模块（MCP、Voice、VCR）
- 使用 `WeakReference` 缓存工具结果
- 实现会话压缩防止内存泄漏
- 使用 `record` 类型减少开销
- GraalVM native-image 支持

### 构建 native-image

```bash
# 需要 GraalVM
mvn package -Pnative -DskipTests
```

## 开发指南

### 添加新工具

1. 创建工具类：`claude-code-tools/src/main/java/com/claudecode/tools/impl/YourTool.java`
2. 继承 `Tool<I, O>` 抽象类
3. 实现核心方法：

```java
public class YourTool extends Tool<YourInput, YourOutput> {

    @Override
    public String name() {
        return "your_tool";
    }

    @Override
    public String description() {
        return "描述工具功能";
    }

    @Override
    public JsonNode inputSchema() {
        // 返回 JSON Schema
    }

    @Override
    public ToolResult<YourOutput> call(YourInput input, ToolContext context) {
        // 实现工具逻辑
    }

    @Override
    public PermissionDecision checkPermissions(YourInput input, ToolPermissionContext ctx) {
        // 权限检查
    }
}
```

4. 在 `ToolRegistry` 中注册
5. 添加单元测试

### 添加新命令

1. 创建命令类：`claude-code-commands/src/main/java/com/claudecode/commands/impl/YourCommand.java`
2. 实现 `Command` 接口
3. 在 `CommandFactory` 中注册

### 代码风格

- 包级别私有（`package-private`）优先
- 数据类使用 `record`
- 联合类型使用 `sealed interface`
- 复杂对象使用 Builder 模式
- 使用 try-with-resources 管理资源

## TypeScript 到 Java 的类型映射

| TypeScript | Java |
|------------|------|
| `async function*` | `Iterator<T>` + Virtual Thread |
| `union type` | `sealed interface` + `permits` |
| `interface` | `record` 或 `class` |
| `Partial<T>` | `Optional<T>` 或 Builder |
| `Record<K,V>` | `Map<K,V>` |
| `Array<T>` | `List<T>` 或 `T[]` |
| `Promise<T>` | `CompletableFuture<T>` |
| `EventEmitter` | `Store<T>` + listeners |
| `Zod` schema | JSON Schema + Jackson |
| `AsyncGenerator` | `Iterator<T>` via `BlockingQueue` |

## 与 TypeScript 版本差异

| 特性 | TypeScript 版本 | Java 版本 |
|------|----------------|-----------|
| 运行时 | Bun | JVM / GraalVM |
| UI 框架 | React + Ink | JLine3 + ANSI |
| 包管理 | npm | Maven |
| 类型系统 | 原生 TypeScript | Java 强类型 |

## 贡献

欢迎提交 Issue 和 Pull Request！

### 开发环境设置

```bash
# 1. Fork 项目
# 2. 克隆你的 Fork
git clone https://github.com/your-username/claude-code-java.git

# 3. 创建特性分支
git checkout -b feature/your-feature

# 4. 进行开发
# ... 修改代码 ...

# 5. 运行测试
mvn test

# 6. 提交
git commit -m "feat: 添加新特性"

# 7. 推送并创建 PR
git push origin feature/your-feature
```

### 代码规范

- 遵循 [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- 所有新代码必须包含单元测试
- 运行 `mvn spotless:apply` 格式化代码

## 路线图

- [ ] Phase 6: 完成测试覆盖率（核心模块 80%+）
- [ ] 端到端集成测试
- [ ] 跨平台兼容性测试
- [ ] GraalVM native-image 优化
- [ ] 终端 UI 增强（Phase 6.1-6.6）

## 许可证

MIT License - 详见 [LICENSE](LICENSE) 文件

## 参考

- [Claude Code 文档](https://docs.anthropic.com/zh-CN/docs/claude-code)
- [Anthropic API 文档](https://docs.anthropic.com/zh-CN/docs/api-reference)
- [Model Context Protocol](https://modelcontextprotocol.io/)
- [Java 21 新特性](https://openjdk.org/projects/jdk/21/)

## 致谢

- [Anthropic](https://www.anthropic.com/) — Claude AI 与 Claude Code
- [JLine3](https://github.com/jline/jline3) — 终端库
- [Picocli](https://picocli.info/) — CLI 框架
- [Eclipse LSP4J](https://github.com/eclipse/lsp4j) — LSP 客户端

---

<p align="center">
  用 ❤️ 和 ☕ 由 Claude Code Java 团队构建
</p>
