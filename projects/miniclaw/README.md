# MiniClaw - AI 助手框架

一个简化的 AI 助手框架，支持多 LLM 提供商、插件化工具系统和任务管理。

## 功能特性

### 1. 多 LLM 提供商支持
- **OpenAI-Compatible**: 支持 OpenAI、DeepSeek 等兼容 API
- **Anthropic**: 支持 Claude 模型
- **Ollama**: 支持本地部署模型

### 2. 插件化工具系统
- 基于 `@ToolDefinition` 注解的工具定义
- 动态工具发现机制（自动扫描包下所有工具）
- 工具注册中心统一管理

### 3. 任务管理系统
- 任务状态管理（PENDING → RUNNING → COMPLETED/FAILED）
- 支持异步和同步执行
- 任务队列管理

## 项目结构

```
miniclaw/
├── src/main/java/com/miniclaw/
│   ├── MiniClawApplication.java      # 主入口
│   ├── core/
│   │   ├── LLMProvider.java          # LLM 接口
│   │   ├── Message.java              # 消息类
│   │   └── ConversationManager.java  # 对话管理
│   ├── providers/
│   │   ├── OpenAICompatibleProvider.java
│   │   ├── AnthropicProvider.java
│   │   └── OllamaProvider.java
│   ├── tools/
│   │   ├── Tool.java                 # 工具接口
│   │   ├── ToolDefinition.java       # 工具注解
│   │   ├── ToolRegistry.java         # 工具注册中心
│   │   └── impl/
│   │       ├── CalculatorTool.java   # 计算器工具
│   │       ├── WeatherTool.java      # 天气工具
│   │       └── TimeTool.java         # 时间工具
│   ├── task/
│   │   └── TaskManager.java          # 任务管理器
│   └── config/
│       └── MiniClawConfig.java       # 配置类
└── src/main/resources/
    ├── application.properties        # 配置文件
    └── logback.xml                   # 日志配置
```

## 快速开始

### 1. 编译项目

```bash
cd miniclaw
mvn clean package -DskipTests
```

### 2. 设置 API Key

```bash
# Linux/Mac
export DEEPSEEK_API_KEY=your_api_key

# Windows CMD
set DEEPSEEK_API_KEY=your_api_key
```

### 3. 运行

**单次执行模式：**
```bash
java -cp target/miniclaw-1.0.0.jar com.miniclaw.MiniClawApplication "你好"
```

**交互模式：**
```bash
java -cp target/miniclaw-1.0.0.jar com.miniclaw.MiniClawApplication
```

## 示例使用

### 工具调用示例

```
User: 帮我计算 (2+3)*4 等于多少
MiniClaw: (2+3)*4 的计算结果是 20。计算过程：先算括号里的 2+3=5，再乘以 4，得到 20。

User: 北京今天天气怎么样
MiniClaw: 北京的天气情况如下：天气多云，温度 23°C...

User: 现在几点了
MiniClaw: 现在是 2026年6月11日 17:26（下午5点26分）。
```

## 添加新工具

1. 创建工具类并实现 `Tool` 接口
2. 使用 `@ToolDefinition` 注解定义工具信息

```java
@ToolDefinition(
    name = "my_tool",
    description = "工具描述",
    parameters = """
        {
            "type": "object",
            "properties": {
                "param": {"type": "string", "description": "参数描述"}
            },
            "required": ["param"]
        }
    """
)
public class MyTool implements Tool {
    @Override
    public ToolResult execute(String params) {
        // 实现逻辑
        return ToolResult.success("结果");
    }
}
```

工具会自动被发现并注册（扫描 `com.miniclaw.tools.impl` 包）。

## 配置说明

在 `application.properties` 中配置：

```properties
llm.provider=deepseek
llm.model=deepseek-chat
llm.baseUrl=https://api.deepseek.com
task.maxConcurrent=5
tool.scanPackage=com.miniclaw.tools.impl
```

## 技术栈

- Java 17
- OkHttp (HTTP 客户端)
- Jackson (JSON 处理)
- Reflections (动态扫描)
- SLF4J + Logback (日志)