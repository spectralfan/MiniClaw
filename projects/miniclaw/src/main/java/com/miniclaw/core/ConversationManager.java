package com.miniclaw.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniclaw.tools.Tool;
import com.miniclaw.tools.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * AI 对话管理器 - 管理多轮对话和工具调用
 */
public class ConversationManager {
    private static final Logger logger = LoggerFactory.getLogger(ConversationManager.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    
    private final LLMProvider llmProvider;
    private final ToolRegistry toolRegistry;
    private final List<Message> conversationHistory;
    private final int maxHistoryLength;
    
    public ConversationManager(LLMProvider llmProvider, ToolRegistry toolRegistry) {
        this.llmProvider = llmProvider;
        this.toolRegistry = toolRegistry;
        this.conversationHistory = new ArrayList<>();
        this.maxHistoryLength = 20;
    }
    
    /**
     * 发送用户消息并处理响应
     */
    public String sendMessage(String userMessage) {
        // 添加用户消息到历史
        conversationHistory.add(Message.user(userMessage));
        
        // 获取可用工具
        List<Tool> availableTools = toolRegistry.getAllTools();
        
        // 调用 LLM
        LLMProvider.ChatResponse response = llmProvider.chatWithTools(
            truncateHistory(),
            availableTools
        );
        
        // 处理工具调用循环
        return handleResponse(response);
    }
    
    /**
     * 处理 LLM 响应（包括工具调用）
     */
    private String handleResponse(LLMProvider.ChatResponse response) {
        int maxIterations = 5; // 防止无限循环
        int iteration = 0;
        
        while (response.needsToolExecution() && iteration < maxIterations) {
            iteration++;
            logger.info("Tool execution iteration {}", iteration);
            
            // 创建包含 tool_calls 的 assistant 消息
            List<Message.ToolCallInfo> toolCallInfos = new ArrayList<>();
            for (LLMProvider.ToolCall call : response.getToolCalls()) {
                toolCallInfos.add(new Message.ToolCallInfo(
                    call.getId(), call.getName(), call.getArguments()
                ));
            }
            Message assistantMsg = Message.assistantWithToolCalls(toolCallInfos);
            conversationHistory.add(assistantMsg);
            
            // 执行所有工具调用
            List<Message> toolResults = executeToolCalls(response.getToolCalls());
            
            // 添加工具结果
            conversationHistory.addAll(toolResults);
            
            // 再次调用 LLM 处理工具结果
            response = llmProvider.chatWithTools(
                truncateHistory(),
                toolRegistry.getAllTools()
            );
        }
        
        // 添加最终助手响应
        conversationHistory.add(Message.assistant(response.getContent()));
        
        return response.getContent();
    }
    
    /**
     * 执行工具调用列表
     */
    private List<Message> executeToolCalls(List<LLMProvider.ToolCall> toolCalls) {
        List<Message> results = new ArrayList<>();
        
        for (LLMProvider.ToolCall call : toolCalls) {
            Tool tool = toolRegistry.getTool(call.getName());
            
            if (tool == null) {
                logger.warn("Tool not found: {}", call.getName());
                results.add(Message.tool(call.getId(), "Error: Tool '" + call.getName() + "' not found"));
                continue;
            }
            
            logger.info("Executing tool: {} with args: {}", call.getName(), call.getArguments());
            
            Tool.ToolResult toolResult = tool.execute(call.getArguments());
            
            String resultContent = toolResult.isSuccess() ? toolResult.getResult() : toolResult.getError();
            results.add(Message.tool(call.getId(), resultContent));
            
            logger.info("Tool {} result: {}", call.getName(), resultContent);
        }
        
        return results;
    }
    
    /**
     * 截断历史消息以保持长度限制
     */
    private List<Message> truncateHistory() {
        if (conversationHistory.size() <= maxHistoryLength) {
            return new ArrayList<>(conversationHistory);
        }
        
        // 保留系统消息（如果有）和最近的对话
        List<Message> truncated = new ArrayList<>();
        
        // 检查是否有系统消息
        if (!conversationHistory.isEmpty() && "system".equals(conversationHistory.get(0).getRole())) {
            truncated.add(conversationHistory.get(0));
            truncated.addAll(conversationHistory.subList(
                conversationHistory.size() - maxHistoryLength + 1,
                conversationHistory.size()
            ));
        } else {
            truncated.addAll(conversationHistory.subList(
                conversationHistory.size() - maxHistoryLength,
                conversationHistory.size()
            ));
        }
        
        return truncated;
    }
    
    /**
     * 清空对话历史
     */
    public void clearHistory() {
        conversationHistory.clear();
    }
    
    /**
     * 获取对话历史
     */
    public List<Message> getHistory() {
        return new ArrayList<>(conversationHistory);
    }
    
    /**
     * 设置系统提示
     */
    public void setSystemPrompt(String prompt) {
        // 移除旧的系统消息
        conversationHistory.removeIf(msg -> "system".equals(msg.getRole()));
        // 在开头添加新的系统消息
        conversationHistory.add(0, Message.system(prompt));
    }
}