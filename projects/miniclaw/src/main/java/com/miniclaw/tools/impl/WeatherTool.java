package com.miniclaw.tools.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniclaw.tools.Tool;
import com.miniclaw.tools.ToolDefinition;

import java.util.Map;
import java.util.Random;

/**
 * 天气工具 - 查询天气信息（模拟数据）
 */
@ToolDefinition(
    name = "weather",
    description = "查询指定城市的天气信息",
    parameters = """
        {
            "type": "object",
            "properties": {
                "city": {
                    "type": "string",
                    "description": "城市名称，如 '北京', '上海', '广州'"
                }
            },
            "required": ["city"]
        }
    """
)
public class WeatherTool implements Tool {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Random random = new Random();
    
    private static final String[] WEATHER_CONDITIONS = {
        "晴", "多云", "阴", "小雨", "中雨", "大雨", "雷阵雨", "小雪", "中雪"
    };
    
    @Override
    public String getName() {
        return "weather";
    }
    
    @Override
    public String getDescription() {
        return "查询指定城市的天气信息";
    }
    
    @Override
    public String getParametersSchema() {
        return """
            {
                "type": "object",
                "properties": {
                    "city": {
                        "type": "string",
                        "description": "城市名称，如 '北京', '上海', '广州'"
                    }
                },
                "required": ["city"]
            }
        """;
    }
    
    @Override
    public ToolResult execute(String params) {
        try {
            Map<String, Object> paramMap = mapper.readValue(params, Map.class);
            String city = (String) paramMap.get("city");
            
            if (city == null || city.isEmpty()) {
                return ToolResult.failure("缺少城市参数");
            }
            
            // 模拟天气数据
            String condition = WEATHER_CONDITIONS[random.nextInt(WEATHER_CONDITIONS.length)];
            int temperature = random.nextInt(35) - 5; // -5 到 30 度
            int humidity = random.nextInt(60) + 30; // 30% 到 90%
            int windSpeed = random.nextInt(30); // 0 到 30 km/h
            
            String weatherInfo = String.format(
                "%s天气：%s，温度 %d°C，湿度 %d%%，风速 %d km/h",
                city, condition, temperature, humidity, windSpeed
            );
            
            return ToolResult.success(weatherInfo);
            
        } catch (Exception e) {
            return ToolResult.failure("天气查询失败: " + e.getMessage());
        }
    }
}