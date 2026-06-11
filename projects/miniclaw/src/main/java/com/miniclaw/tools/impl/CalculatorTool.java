package com.miniclaw.tools.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniclaw.tools.Tool;
import com.miniclaw.tools.ToolDefinition;

import java.util.Map;

/**
 * 计算器工具 - 执行数学运算
 */
@ToolDefinition(
    name = "calculator",
    description = "执行数学计算，支持加减乘除和复杂表达式",
    parameters = """
        {
            "type": "object",
            "properties": {
                "expression": {
                    "type": "string",
                    "description": "数学表达式，如 '2+3*4' 或 'sqrt(16)'"
                }
            },
            "required": ["expression"]
        }
    """
)
public class CalculatorTool implements Tool {
    private static final ObjectMapper mapper = new ObjectMapper();
    
    @Override
    public String getName() {
        return "calculator";
    }
    
    @Override
    public String getDescription() {
        return "执行数学计算，支持加减乘除和复杂表达式";
    }
    
    @Override
    public String getParametersSchema() {
        return """
            {
                "type": "object",
                "properties": {
                    "expression": {
                        "type": "string",
                        "description": "数学表达式，如 '2+3*4' 或 'sqrt(16)'"
                    }
                },
                "required": ["expression"]
            }
        """;
    }
    
    @Override
    public ToolResult execute(String params) {
        try {
            Map<String, Object> paramMap = mapper.readValue(params, Map.class);
            String expression = (String) paramMap.get("expression");
            
            if (expression == null || expression.isEmpty()) {
                return ToolResult.failure("Missing expression parameter");
            }
            
            // 简化的表达式计算（仅支持基本运算）
            double result = evaluateExpression(expression);
            return ToolResult.success("计算结果: " + result);
            
        } catch (Exception e) {
            return ToolResult.failure("计算失败: " + e.getMessage());
        }
    }
    
    /**
     * 简化表达式计算（支持 + - * / 和括号）
     */
    private double evaluateExpression(String expr) {
        // 去除空格
        expr = expr.replaceAll("\\s+", "");
        
        // 先处理括号
        while (expr.contains("(")) {
            int start = expr.lastIndexOf("(");
            int end = expr.indexOf(")", start);
            if (end > start) {
                String subExpr = expr.substring(start + 1, end);
                double subResult = evaluateSimple(subExpr);
                expr = expr.substring(0, start) + subResult + expr.substring(end + 1);
            }
        }
        
        return evaluateSimple(expr);
    }
    
    /**
     * 计算无括号的简单表达式
     */
    private double evaluateSimple(String expr) {
        // 处理乘除
        while (expr.contains("*") || expr.contains("/")) {
            int mulIdx = expr.indexOf("*");
            int divIdx = expr.indexOf("/");
            int idx = -1;
            char op = ' ';
            
            if (mulIdx >= 0 && divIdx >= 0) {
                idx = Math.min(mulIdx, divIdx);
                op = idx == mulIdx ? '*' : '/';
            } else if (mulIdx >= 0) {
                idx = mulIdx;
                op = '*';
            } else if (divIdx >= 0) {
                idx = divIdx;
                op = '/';
            }
            
            if (idx >= 0) {
                String[] parts = expr.split("[*/]", 2);
                double left = Double.parseDouble(parts[0]);
                
                // 找右边的数字
                String rightPart = parts[1];
                String rightNum = rightPart.split("[+-]", 2)[0];
                double right = Double.parseDouble(rightNum);
                
                double result = (op == '*') ? left * right : left / right;
                
                // 替换表达式
                if (rightPart.contains("+") || rightPart.contains("-")) {
                    int signIdx = findSignIndex(rightPart);
                    expr = result + rightPart.substring(signIdx);
                } else {
                    expr = String.valueOf(result);
                }
            }
        }
        
        // 处理加减
        double result = 0;
        if (expr.contains("+") || expr.contains("-")) {
            result = parseAddSub(expr);
        } else {
            result = Double.parseDouble(expr);
        }
        
        return result;
    }
    
    private int findSignIndex(String str) {
        for (int i = 1; i < str.length(); i++) {
            if (str.charAt(i) == '+' || str.charAt(i) == '-') {
                return i;
            }
        }
        return str.length();
    }
    
    private double parseAddSub(String expr) {
        double result = 0;
        int start = 0;
        char op = '+';
        
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (c == '+' || c == '-') {
                String numStr = expr.substring(start, i);
                if (!numStr.isEmpty()) {
                    double num = Double.parseDouble(numStr);
                    result = (op == '+') ? result + num : result - num;
                }
                op = c;
                start = i + 1;
            }
        }
        
        // 处理最后一个数字
        if (start < expr.length()) {
            double num = Double.parseDouble(expr.substring(start));
            result = (op == '+') ? result + num : result - num;
        }
        
        return result;
    }
}