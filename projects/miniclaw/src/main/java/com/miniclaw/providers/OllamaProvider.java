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
 * Ollama 本地模型提供商
 */
public class OllamaProvider implements LLMProvider {
    private static final Logger logger = LoggerFactory.getLogger(OllamaProvider.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    
    private String baseUrl = "http://localhost:11434";
    private String model = "llama3";
    private String apiKey; // Ollama 通常不需要 API Key
    private final OkHttpClient httpClient;
    
    public OllamaProvider() {
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS) // Ollama 可能较慢
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
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("stream", false);
            
            // 格式化消息
            List<Map<String, String>> formattedMessages = new ArrayList<>();
            for (Message msg : messages) {
                formattedMessages.add(Map.of(
                    "role", msg.getRole(),
                    "content", msg.getContent()
                ));
            }
            requestBody.put("messages", formattedMessages);
            
            // 添加工具
            if (!tools.isEmpty()) {
                List<Map<String, Object>> toolDefs = new ArrayList<>();
                for (Tool tool : tools) {
                    toolDefs.add(Map.of(
                        "type", "function",
                        "function", Map.of(
                            "name", tool.getName(),
                            "description", tool.getDescription(),
                            "parameters", parseSchema(tool.getParametersSchema())
                        )
                    ));
                }
                requestBody.put("tools", toolDefs);
            }
            
            String jsonBody = mapper.writeValueAsString(requestBody);
            logger.debug("Request body: {}", jsonBody);
            
            Request request = new Request.Builder()
                .url(baseUrl + "/api/chat")
                .header("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .build();
            
            Response response = httpClient.newCall(request).execute();
            
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                throw new IOException("API request failed: " + response.code() + " - " + errorBody);
            }
            
            String responseBody = response.body().string();
            return parseResponse(responseBody);
            
        } catch (Exception e) {
            logger.error("Chat request failed", e);
            throw new RuntimeException("Failed to chat with Ollama: " + e.getMessage(), e);
        }
    }
    
    private Object parseSchema(String schemaJson) {
        try {
            return mapper.readValue(schemaJson, Map.class);
        } catch (Exception e) {
            return Map.of("type", "object", "properties", Map.of());
        }
    }
    
    private ChatResponse parseResponse(String responseBody) throws IOException {
        JsonNode root = mapper.readTree(responseBody);
        JsonNode message = root.path("message");
        
        // 检查工具调用
        JsonNode toolCalls = message.path("tool_calls");
        if (!toolCalls.isMissingNode() && toolCalls.isArray() && toolCalls.size() > 0) {
            List<ToolCall> calls = new ArrayList<>();
            for (JsonNode tc : toolCalls) {
                JsonNode func = tc.path("function");
                String id = UUID.randomUUID().toString(); // Ollama 可能不返回 ID
                String name = func.path("name").asText();
                String args = func.path("arguments").toString();
                calls.add(new ToolCall(id, name, args));
            }
            return new ChatResponse(calls);
        }
        
        String content = message.path("content").asText();
        return new ChatResponse(content);
    }
    
    @Override
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey; // Ollama 通常不需要
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
        return "Ollama";
    }
}