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
 * Anthropic Claude 提供商
 */
public class AnthropicProvider implements LLMProvider {
    private static final Logger logger = LoggerFactory.getLogger(AnthropicProvider.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    
    private String apiKey;
    private String baseUrl = "https://api.anthropic.com";
    private String model = "claude-3-sonnet-20240229";
    private final OkHttpClient httpClient;
    
    public AnthropicProvider() {
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
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("max_tokens", 4096);
            
            // 分离 system message
            List<Message> filteredMessages = new ArrayList<>();
            String systemPrompt = "";
            for (Message msg : messages) {
                if ("system".equals(msg.getRole())) {
                    systemPrompt = msg.getContent();
                } else {
                    filteredMessages.add(msg);
                }
            }
            
            if (!systemPrompt.isEmpty()) {
                requestBody.put("system", systemPrompt);
            }
            
            // 格式化消息
            List<Map<String, Object>> formattedMessages = new ArrayList<>();
            for (Message msg : filteredMessages) {
                formattedMessages.add(Map.of(
                    "role", msg.getRole(),
                    "content", msg.getContent()
                ));
            }
            requestBody.put("messages", formattedMessages);
            
            // 添加工具
            if (!tools.isEmpty()) {
                requestBody.put("tools", tools.stream()
                    .map(t -> Map.of(
                        "name", t.getName(),
                        "description", t.getDescription(),
                        "input_schema", parseSchema(t.getParametersSchema())
                    ))
                    .toList());
            }
            
            String jsonBody = mapper.writeValueAsString(requestBody);
            logger.debug("Request body: {}", jsonBody);
            
            Request request = new Request.Builder()
                .url(baseUrl + "/v1/messages")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
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
            throw new RuntimeException("Failed to chat with Anthropic: " + e.getMessage(), e);
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
        JsonNode content = root.path("content");
        
        if (content.isArray()) {
            StringBuilder textContent = new StringBuilder();
            List<ToolCall> toolCalls = new ArrayList<>();
            
            for (JsonNode block : content) {
                String type = block.path("type").asText();
                
                if ("text".equals(type)) {
                    textContent.append(block.path("text").asText());
                } else if ("tool_use".equals(type)) {
                    String id = block.path("id").asText();
                    String name = block.path("name").asText();
                    String args = block.path("input").toString();
                    toolCalls.add(new ToolCall(id, name, args));
                }
            }
            
            if (!toolCalls.isEmpty()) {
                return new ChatResponse(toolCalls);
            }
            return new ChatResponse(textContent.toString());
        }
        
        return new ChatResponse("No response from model");
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
        return "Anthropic";
    }
}