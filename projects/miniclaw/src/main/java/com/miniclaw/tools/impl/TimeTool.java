package com.miniclaw.tools.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniclaw.tools.Tool;
import com.miniclaw.tools.ToolDefinition;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 时间工具 - 获取当前时间、日期格式化
 */
@ToolDefinition(
    name = "time",
    description = "获取当前时间和日期信息，支持自定义格式",
    parameters = """
        {
            "type": "object",
            "properties": {
                "format": {
                    "type": "string",
                    "description": "时间格式，如 'yyyy-MM-dd HH:mm:ss'，默认为 ISO 格式"
                }
            },
            "required": []
        }
    """
)
public class TimeTool implements Tool {
    private static final ObjectMapper mapper = new ObjectMapper();
    
    @Override
    public String getName() {
        return "time";
    }
    
    @Override
    public String getDescription() {
        return "获取当前时间和日期信息，支持自定义格式";
    }
    
    @Override
    public String getParametersSchema() {
        return """
            {
                "type": "object",
                "properties": {
                    "format": {
                        "type": "string",
                        "description": "时间格式，如 'yyyy-MM-dd HH:mm:ss'，默认为 ISO 格式"
                    }
                },
                "required": []
            }
        """;
    }
    
    @Override
    public ToolResult execute(String params) {
        try {
            LocalDateTime now = LocalDateTime.now();
            
            if (params != null && !params.isEmpty()) {
                Map<String, Object> paramMap = mapper.readValue(params, Map.class);
                String format = (String) paramMap.get("format");
                
                if (format != null && !format.isEmpty()) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
                    return ToolResult.success("当前时间: " + now.format(formatter));
                }
            }
            
            return ToolResult.success("当前时间: " + now.toString());
            
        } catch (Exception e) {
            return ToolResult.failure("时间获取失败: " + e.getMessage());
        }
    }
}