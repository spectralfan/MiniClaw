package com.miniclaw.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

/**
 * 工具接口 - 所有插件工具必须实现此接口
 */
public interface Tool {
    
    /**
     * 获取工具名称
     */
    String getName();
    
    /**
     * 获取工具描述
     */
    String getDescription();
    
    /**
     * 获取工具参数 schema (JSON Schema 格式)
     */
    String getParametersSchema();
    
    /**
     * 执行工具
     * @param params 参数 JSON 字符串
     * @return 执行结果
     */
    ToolResult execute(String params);
    
    /**
     * 工具执行结果
     */
    class ToolResult {
        private boolean success;
        private String result;
        private String error;
        
        public static ToolResult success(String result) {
            ToolResult r = new ToolResult();
            r.success = true;
            r.result = result;
            return r;
        }
        
        public static ToolResult failure(String error) {
            ToolResult r = new ToolResult();
            r.success = false;
            r.error = error;
            return r;
        }
        
        public boolean isSuccess() { return success; }
        public String getResult() { return result; }
        public String getError() { return error; }
        
        @Override
        public String toString() {
            if (success) {
                return result;
            }
            return "Error: " + error;
        }
    }
    
    /**
     * 工具定义 Schema (用于 LLM 调用)
     */
    default Map<String, Object> toFunctionSchema() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            Map<String, Object> schema = mapper.readValue(getParametersSchema(), Map.class);
            return Map.of(
                "type", "function",
                "function", Map.of(
                    "name", getName(),
                    "description", getDescription(),
                    "parameters", schema
                )
            );
        } catch (Exception e) {
            return Map.of(
                "type", "function",
                "function", Map.of(
                    "name", getName(),
                    "description", getDescription(),
                    "parameters", Map.of("type", "object", "properties", Map.of())
                )
            );
        }
    }
}