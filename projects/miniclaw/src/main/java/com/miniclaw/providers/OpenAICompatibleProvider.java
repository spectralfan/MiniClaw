package com.miniclaw.providers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniclaw.core.LLMProvider;
import com.miniclaw.core.Message;
import com.miniclaw.tools.Tool;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * OpenAI 兼容的 LLM 提供商
 * 支持 OpenAI、DeepSeek、Ollama 等兼容 API
 */
public class OpenAICompatibleProvider implements LLMProvider {
    private static final Logger logger = LoggerFactory.getLogger(OpenAICompatibleProvider.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    
    private String apiKey;
    private String baseUrl = "https://api.deepseek.com"; // 默认 DeepSeek
    private String model = "deepseek-chat";
    private final OkHttpClient httpClient;
    
    public OpenAICompatibleProvider() {
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
    }
    
    @Override
    public String chat(List<Message> messages) {
        ChatResponse response = chatWithTools(messages, Collections.emptyList());
        return response.getContent();
    }
    
    @Override
    public ChatResponse chatWithTools(List<Message> messages, List<Tool> tools) {
        try {
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", messages);
            
            // 添加工具定义
            if (!tools.isEmpty()) {
                requestBody.put("tools", tools.stream()
                    .map(Tool::toFunctionSchema)
                    .toList());
            }
            
            String jsonBody = mapper.writeValueAsString(requestBody);
            logger.debug("Request body: {}", jsonBody);
            
            // 发送请求
            Request request = new Request.Builder()
                .url(baseUrl + "/v1/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .build();
            
            Response response = httpClient.newCall(request).execute();
            
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                throw new IOException("API request failed: " + response.code() + " - " + errorBody);
            }
            
            String responseBody = response.body().string();
            logger.debug("Response body: {}", responseBody);
            
            return parseResponse(responseBody);
            
        } catch (Exception e) {
            logger.error("Chat request failed", e);
            throw new RuntimeException("Failed to chat with LLM: " + e.getMessage(), e);
        }
    }
    
    /**
     * 解析 API 响应
     */
    private ChatResponse parseResponse(String responseBody) throws IOException {
        JsonNode root = mapper.readTree(responseBody);
        JsonNode choices = root.path("choices");
        
        if (choices.isEmpty()) {
            return new ChatResponse("No response from model");
        }
        
        JsonNode firstChoice = choices.get(0);
        JsonNode message = firstChoice.path("message");
        
        // 检查是否有工具调用
        JsonNode toolCalls = message.path("tool_calls");
        if (!toolCalls.isMissingNode() && toolCalls.isArray() && toolCalls.size() > 0) {
            List<LLMProvider.ToolCall> calls = new ArrayList<>();
            for (JsonNode tc : toolCalls) {
                String id = tc.path("id").asText();
                String name = tc.path("function").path("name").asText();
                String args = tc.path("function").path("arguments").asText();
                calls.add(new ToolCall(id, name, args));
            }
            return new ChatResponse(calls);
        }
        
        // 返回文本内容
        String content = message.path("content").asText();
        return new ChatResponse(content);
    }
    
    @Override
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
    
    @Override
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    @Override
    public void setModel(String model) {
        this.model = model;
    }
    
    @Override
    public String getProviderName() {
        return "OpenAI-Compatible";
    }
}