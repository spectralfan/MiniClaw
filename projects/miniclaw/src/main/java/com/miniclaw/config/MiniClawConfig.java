package com.miniclaw.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * MiniClaw 配置管理
 */
public class MiniClawConfig {
    private Properties properties;
    
    public MiniClawConfig() {
        this.properties = new Properties();
        loadDefaults();
    }
    
    public MiniClawConfig(String configFile) {
        this.properties = new Properties();
        loadDefaults();
        loadFromFile(configFile);
    }
    
    /**
     * 加载默认配置
     */
    private void loadDefaults() {
        properties.setProperty("llm.provider", "deepseek");
        properties.setProperty("llm.model", "deepseek-chat");
        properties.setProperty("llm.baseUrl", "https://api.deepseek.com");
        properties.setProperty("task.maxConcurrent", "5");
        properties.setProperty("tool.scanPackage", "com.miniclaw.tools.impl");
    }
    
    /**
     * 从文件加载配置
     */
    private void loadFromFile(String configFile) {
        try (FileInputStream fis = new FileInputStream(configFile)) {
            properties.load(fis);
        } catch (IOException e) {
            System.err.println("Warning: Could not load config file: " + configFile);
        }
    }
    
    /**
     * 从环境变量加载 API Key
     */
    public String getApiKey() {
        // 优先使用环境变量
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        if (apiKey == null) {
            apiKey = properties.getProperty("llm.apiKey");
        }
        return apiKey;
    }
    
    public String getProvider() {
        return properties.getProperty("llm.provider");
    }
    
    public String getModel() {
        return properties.getProperty("llm.model");
    }
    
    public String getBaseUrl() {
        return properties.getProperty("llm.baseUrl");
    }
    
    public int getMaxConcurrentTasks() {
        return Integer.parseInt(properties.getProperty("task.maxConcurrent"));
    }
    
    public String getToolScanPackage() {
        return properties.getProperty("tool.scanPackage");
    }
    
    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }
    
    public String getProperty(String key) {
        return properties.getProperty(key);
    }
}