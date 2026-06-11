package com.miniclaw.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * 聊天消息
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Message {
    private String role; // system, user, assistant, tool
    private String content;
    private String name; // for tool calls
    
    @JsonProperty("tool_call_id")
    private String toolCallId; // for tool responses
    
    @JsonProperty("tool_calls")
    private List<ToolCallInfo> toolCalls; // for assistant messages with tool calls
    
    public Message() {}
    
    public Message(String role, String content) {
        this.role = role;
        this.content = content;
    }
    
    public static Message system(String content) {
        return new Message("system", content);
    }
    
    public static Message user(String content) {
        return new Message("user", content);
    }
    
    public static Message assistant(String content) {
        return new Message("assistant", content);
    }
    
    public static Message assistantWithToolCalls(List<ToolCallInfo> toolCalls) {
        Message msg = new Message("assistant", null);
        msg.setToolCalls(toolCalls);
        return msg;
    }
    
    public static Message tool(String toolCallId, String content) {
        Message msg = new Message("tool", content);
        msg.setToolCallId(toolCallId);
        return msg;
    }
    
    // Getters and Setters
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getToolCallId() { return toolCallId; }
    public void setToolCallId(String toolCallId) { this.toolCallId = toolCallId; }
    public List<ToolCallInfo> getToolCalls() { return toolCalls; }
    public void setToolCalls(List<ToolCallInfo> toolCalls) { this.toolCalls = toolCalls; }
    
    @Override
    public String toString() {
        return String.format("Message{role='%s', content='%s'}", role, content);
    }
    
    /**
     * 工具调用信息（用于 assistant 消息中的 tool_calls）
     */
    public static class ToolCallInfo {
        private String id;
        private String type = "function";
        private FunctionCall function;
        
        public ToolCallInfo() {}
        
        public ToolCallInfo(String id, String name, String arguments) {
            this.id = id;
            this.function = new FunctionCall(name, arguments);
        }
        
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public FunctionCall getFunction() { return function; }
        public void setFunction(FunctionCall function) { this.function = function; }
    }
    
    /**
     * 函数调用详情
     */
    public static class FunctionCall {
        private String name;
        private String arguments;
        
        public FunctionCall() {}
        
        public FunctionCall(String name, String arguments) {
            this.name = name;
            this.arguments = arguments;
        }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getArguments() { return arguments; }
        public void setArguments(String arguments) { this.arguments = arguments; }
    }
}