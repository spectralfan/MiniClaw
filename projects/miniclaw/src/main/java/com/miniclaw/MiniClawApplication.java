package com.miniclaw;

import com.miniclaw.config.MiniClawConfig;
import com.miniclaw.core.ConversationManager;
import com.miniclaw.core.LLMProvider;
import com.miniclaw.core.Message;
import com.miniclaw.providers.OpenAICompatibleProvider;
import com.miniclaw.task.TaskManager;
import com.miniclaw.task.TaskManager.*;
import com.miniclaw.tools.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;
import java.util.concurrent.Future;

/**
 * MiniClaw AI 助手框架主程序
 * 
 * 功能：
 * - 多 LLM 提供商支持（OpenAI、Anthropic、Ollama）
 * - 插件化工具系统
 * - 动态工具发现
 * - 任务管理系统
 */
public class MiniClawApplication {
    private static final Logger logger = LoggerFactory.getLogger(MiniClawApplication.class);
    
    private final MiniClawConfig config;
    private final LLMProvider llmProvider;
    private final ToolRegistry toolRegistry;
    private final ConversationManager conversationManager;
    private final TaskManager taskManager;
    
    public MiniClawApplication() {
        this.config = new MiniClawConfig();
        this.llmProvider = createLLMProvider();
        this.toolRegistry = new ToolRegistry();
        this.conversationManager = new ConversationManager(llmProvider, toolRegistry);
        this.taskManager = new TaskManager(config.getMaxConcurrentTasks());
        
        initialize();
    }
    
    /**
     * 初始化系统
     */
    private void initialize() {
        logger.info("Initializing MiniClaw...");
        
        // 设置 API Key
        String apiKey = config.getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            logger.warn("No API Key found. Please set DEEP_SEEK_API_KEY environment variable.");
        } else {
            llmProvider.setApiKey(apiKey);
            logger.info("API Key configured");
        }
        
        // 设置其他 LLM 配置
        llmProvider.setBaseUrl(config.getBaseUrl());
        llmProvider.setModel(config.getModel());
        
        // 动态发现并注册工具
        toolRegistry.discoverTools(config.getToolScanPackage());
        
        // 设置系统提示
        conversationManager.setSystemPrompt(buildSystemPrompt());
        
        logger.info("MiniClaw initialized successfully");
        logger.info("Provider: {}, Model: {}", llmProvider.getProviderName(), config.getModel());
        logger.info("Available tools: {}", toolRegistry.getAllTools().size());
    }
    
    /**
     * 创建 LLM 提供商
     */
    private LLMProvider createLLMProvider() {
        String provider = config.getProvider();
        
        switch (provider.toLowerCase()) {
            case "openai":
            case "deepseek":
                return new OpenAICompatibleProvider();
            case "anthropic":
                // return new AnthropicProvider();
                logger.warn("Anthropic provider requires valid API key, using OpenAI-Compatible instead");
                return new OpenAICompatibleProvider();
            case "ollama":
                // return new OllamaProvider();
                logger.warn("Ollama requires local installation, using OpenAI-Compatible instead");
                return new OpenAICompatibleProvider();
            default:
                logger.info("Using default provider: OpenAI-Compatible");
                return new OpenAICompatibleProvider();
        }
    }
    
    /**
     * 构建系统提示
     */
    private String buildSystemPrompt() {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是 MiniClaw AI 助手，一个强大的智能助手。\n");
        prompt.append("你可以使用以下工具来帮助用户完成任务：\n\n");
        
        for (var tool : toolRegistry.getAllTools()) {
            prompt.append("- ").append(tool.getName()).append(": ").append(tool.getDescription()).append("\n");
        }
        
        prompt.append("\n当用户的问题需要使用工具时，请调用相应的工具。\n");
        prompt.append("请用简洁、友好的方式回复用户。\n");
        
        return prompt.toString();
    }
    
    /**
     * 处理单次对话请求
     */
    public String chat(String userInput) {
        return conversationManager.sendMessage(userInput);
    }
    
    /**
     * 创建任务
     */
    public Task createTask(String description, String userInput) {
        return taskManager.createTask(description, (task) -> {
            String result = chat(userInput);
            return TaskResult.success(result);
        });
    }
    
    /**
     * 执行任务（异步）
     */
    public Future<TaskResult> executeTaskAsync(String taskId) {
        return taskManager.executeAsync(taskId);
    }
    
    /**
     * 执行任务（同步）
     */
    public TaskResult executeTaskSync(String taskId) {
        return taskManager.executeSync(taskId);
    }
    
    /**
     * 运行交互式命令行界面
     */
    public void runInteractive() {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("\n========================================");
        System.out.println("   MiniClaw AI 助手 - 交互模式");
        System.out.println("========================================");
        System.out.println("可用工具:");
        for (var tool : toolRegistry.getAllTools()) {
            System.out.println("  - " + tool.getName() + ": " + tool.getDescription());
        }
        System.out.println("\n输入 'exit' 退出, 'clear' 清空历史, 'history' 查看历史");
        System.out.println("========================================\n");
        
        while (true) {
            System.out.print("You: ");
            String input = scanner.nextLine().trim();
            
            if (input.isEmpty()) {
                continue;
            }
            
            if ("exit".equalsIgnoreCase(input) || "quit".equalsIgnoreCase(input)) {
                System.out.println("\n再见！感谢使用 MiniClaw AI 助手。");
                break;
            }
            
            if ("clear".equalsIgnoreCase(input)) {
                conversationManager.clearHistory();
                conversationManager.setSystemPrompt(buildSystemPrompt());
                System.out.println("对话历史已清空。\n");
                continue;
            }
            
            if ("history".equalsIgnoreCase(input)) {
                System.out.println("\n对话历史:");
                for (Message msg : conversationManager.getHistory()) {
                    System.out.println("  [" + msg.getRole() + "] " + msg.getContent());
                }
                System.out.println();
                continue;
            }
            
            // 发送消息并获取响应
            System.out.print("MiniClaw: ");
            try {
                String response = chat(input);
                System.out.println(response + "\n");
            } catch (Exception e) {
                logger.error("Error processing message", e);
                System.out.println("抱歉，处理您的请求时出现错误: " + e.getMessage() + "\n");
            }
        }
        
        scanner.close();
        taskManager.shutdown();
    }
    
    /**
     * 主入口
     */
    public static void main(String[] args) {
        // 检查 API Key
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("Error: DEEPSEEK_API_KEY environment variable not set");
            System.err.println("Please set the API key: set DEEPSEEK_API_KEY=your_api_key (Windows)");
            System.err.println("Or: export DEEPSEEK_API_KEY=your_api_key (Linux/Mac)");
            System.exit(1);
        }
        
        MiniClawApplication app = new MiniClawApplication();
        
        // 检查是否有命令行参数（单次执行模式）
        if (args.length > 0) {
            String input = String.join(" ", args);
            System.out.println("User: " + input);
            System.out.print("MiniClaw: ");
            String response = app.chat(input);
            System.out.println(response);
            app.taskManager.shutdown();
        } else {
            // 交互模式
            app.runInteractive();
        }
    }
}