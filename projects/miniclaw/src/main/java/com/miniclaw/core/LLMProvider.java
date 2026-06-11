package com.miniclaw.core;

import com.miniclaw.tools.Tool;
import java.util.List;

/**
 * LLM 提供商统一抽象接口
 * 支持 OpenAI、Anthropic、Ollama 等多提供商
 */
public interface LLMProvider {
    
    /**
     * 简单对话（不带工具）
     * @param messages 消息列表
     * @return AI 回复内容
     */
    String chat(List<Message> messages);
    
    /**
     * 带工具的对话
     * @param messages 消息列表
     * @param tools 可用工具列表
     * @return AI 回复内容
     */
    ChatResponse chatWithTools(List<Message> messages, List<Tool> tools);
    
    /**
     * 设置 API Key
     */
    void setApiKey(String apiKey);
    
    /**
     * 设置 API 基础 URL
     */
    void setBaseUrl(String baseUrl);
    
    /**
     * 设置模型名称
     */
    void setModel(String model);
    
    /**
     * 获取提供商名称
     */
    String getProviderName();
    
    /**
     * 对话响应（包含工具调用信息）
     */
    class ChatResponse {
        private String content;
        private List<ToolCall> toolCalls;
        private boolean needsToolExecution;
        
        public ChatResponse(String content) {
            this.content = content;
            this.needsToolExecution = false;
        }
        
        public ChatResponse(List<ToolCall> toolCalls) {
            this.toolCalls = toolCalls;
            this.needsToolExecution = true;
        }
        
        public String getContent() { return content; }
        public List<ToolCall> getToolCalls() { return toolCalls; }
        public boolean needsToolExecution() { return needsToolExecution; }
    }
    
    /**
     * 工具调用请求
     */
    class ToolCall {
        private String id;
        private String name;
        private String arguments;
        
        public ToolCall(String id, String name, String arguments) {
            this.id = id;
            this.name = name;
            this.arguments = arguments;
        }
        
        public String getId() { return id; }
        public String getName() { return name; }
        public String getArguments() { return arguments; }
    }
}