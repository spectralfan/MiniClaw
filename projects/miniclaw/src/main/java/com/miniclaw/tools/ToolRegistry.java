package com.miniclaw.tools;

import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工具注册中心 - 管理所有可用工具
 */
public class ToolRegistry {
    private static final Logger logger = LoggerFactory.getLogger(ToolRegistry.class);
    
    private final Map<String, Tool> tools = new ConcurrentHashMap<>();
    
    /**
     * 注册工具
     */
    public void register(Tool tool) {
        tools.put(tool.getName(), tool);
        logger.info("Registered tool: {}", tool.getName());
    }
    
    /**
     * 获取工具
     */
    public Tool getTool(String name) {
        return tools.get(name);
    }
    
    /**
     * 获取所有工具
     */
    public List<Tool> getAllTools() {
        return new ArrayList<>(tools.values());
    }
    
    /**
     * 检查工具是否存在
     */
    public boolean hasTool(String name) {
        return tools.containsKey(name);
    }
    
    /**
     * 动态发现并注册工具（基于注解扫描）
     * @param packageName 要扫描的包名
     */
    public void discoverTools(String packageName) {
        logger.info("Discovering tools in package: {}", packageName);
        
        Reflections reflections = new Reflections(packageName);
        Set<Class<?>> toolClasses = reflections.getTypesAnnotatedWith(ToolDefinition.class);
        
        for (Class<?> clazz : toolClasses) {
            try {
                if (Tool.class.isAssignableFrom(clazz)) {
                    Tool tool = (Tool) clazz.getDeclaredConstructor().newInstance();
                    
                    // 使用注解信息增强工具定义（如果类本身没有实现）
                    ToolDefinition annotation = clazz.getAnnotation(ToolDefinition.class);
                    register(new AnnotatedToolWrapper(tool, annotation));
                    
                    logger.debug("Discovered and registered tool: {} from class {}", 
                        annotation.name(), clazz.getSimpleName());
                }
            } catch (Exception e) {
                logger.warn("Failed to instantiate tool class: {}", clazz.getName(), e);
            }
        }
        
        logger.info("Total tools registered: {}", tools.size());
    }
    
    /**
     * 获取工具列表的 Function Schema（用于 LLM 调用）
     */
    public List<Map<String, Object>> getToolSchemas() {
        return tools.values().stream()
            .map(Tool::toFunctionSchema)
            .toList();
    }
    
    /**
     * 注解工具包装器 - 用于将注解信息应用到工具
     */
    private static class AnnotatedToolWrapper implements Tool {
        private final Tool delegate;
        private final ToolDefinition annotation;
        
        AnnotatedToolWrapper(Tool delegate, ToolDefinition annotation) {
            this.delegate = delegate;
            this.annotation = annotation;
        }
        
        @Override
        public String getName() {
            return annotation.name();
        }
        
        @Override
        public String getDescription() {
            return annotation.description();
        }
        
        @Override
        public String getParametersSchema() {
            if (annotation.parameters().isEmpty()) {
                return delegate.getParametersSchema();
            }
            return annotation.parameters();
        }
        
        @Override
        public ToolResult execute(String params) {
            return delegate.execute(params);
        }
    }
}