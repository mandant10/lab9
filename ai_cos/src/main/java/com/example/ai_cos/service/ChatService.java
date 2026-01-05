package com.example.ai_cos.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class ChatService {

    private final ToolCallbackProvider toolCallbackProvider;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final Map<String, ToolCallback> toolCallbacks;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.model:gemini-2.0-flash}")
    private String model;

    @Value("${gemini.temperature:0.7}")
    private double temperature;

    @Value("${gemini.instructions}")
    private String systemInstructions;

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    public ChatService(ToolCallbackProvider toolCallbackProvider) {
        this.toolCallbackProvider = toolCallbackProvider;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.toolCallbacks = new HashMap<>();
        
        for (ToolCallback callback : toolCallbackProvider.getToolCallbacks()) {
            toolCallbacks.put(callback.getToolDefinition().name(), callback);
        }
    }

    public String chat(String userMessage) {
        try {
            if (apiKey == null || apiKey.isBlank()) {
                return "Brak GEMINI_API_KEY. Ustaw zmienną środowiskową GEMINI_API_KEY na hoście (np. Render).";
            }

            ObjectNode requestBody = buildRequestWithTools(userMessage);
            String response = callGeminiApi(requestBody);
            
            JsonNode responseJson = objectMapper.readTree(response);
            JsonNode functionCall = extractFunctionCall(responseJson);
            
            if (functionCall != null) {
                String functionName = functionCall.get("name").asText();
                JsonNode args = functionCall.get("args");
                String toolResult = executeToolCallback(functionName, args);
                return callWithFunctionResult(userMessage, functionName, args, toolResult);
            }
            
            return extractTextResponse(responseJson);
            
        } catch (Exception e) {
            e.printStackTrace();
            return "Błąd: " + e.getMessage();
        }
    }

    private ObjectNode buildRequestWithTools(String userMessage) {
        ObjectNode request = objectMapper.createObjectNode();
        
        ArrayNode contents = request.putArray("contents");
        ObjectNode userContent = contents.addObject();
        userContent.put("role", "user");
        ArrayNode parts = userContent.putArray("parts");
        parts.addObject().put("text", userMessage);
        
        ObjectNode systemInstruction = request.putObject("system_instruction");
        ArrayNode sysParts = systemInstruction.putArray("parts");
        sysParts.addObject().put("text", systemInstructions);
        
        ArrayNode tools = request.putArray("tools");
        ObjectNode toolObj = tools.addObject();
        ArrayNode functionDeclarations = toolObj.putArray("function_declarations");
        
        for (ToolCallback callback : toolCallbackProvider.getToolCallbacks()) {
            var toolDef = callback.getToolDefinition();
            ObjectNode func = functionDeclarations.addObject();
            func.put("name", toolDef.name());
            func.put("description", toolDef.description());
            
            String inputSchema = toolDef.inputSchema();
            try {
                JsonNode schemaNode = objectMapper.readTree(inputSchema);
                ObjectNode cleanedParams = cleanSchemaForGemini((ObjectNode) schemaNode);
                func.set("parameters", cleanedParams);
            } catch (Exception e) {
                ObjectNode params = func.putObject("parameters");
                params.put("type", "object");
                params.putObject("properties");
            }
        }
        
        ObjectNode toolConfig = request.putObject("tool_config");
        ObjectNode functionCallingConfig = toolConfig.putObject("function_calling_config");
        functionCallingConfig.put("mode", "AUTO");
        
        ObjectNode generationConfig = request.putObject("generationConfig");
        generationConfig.put("temperature", temperature);
        
        return request;
    }
    
    private ObjectNode cleanSchemaForGemini(ObjectNode schema) {
        ObjectNode cleaned = objectMapper.createObjectNode();
        
        schema.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            JsonNode value = entry.getValue();
            
            if (key.equals("$schema") || key.equals("additionalProperties")) {
                return;
            }
            
            if (value.isObject()) {
                cleaned.set(key, cleanSchemaForGemini((ObjectNode) value));
            } else {
                cleaned.set(key, value);
            }
        });
        
        return cleaned;
    }

    private String callGeminiApi(ObjectNode requestBody) throws Exception {
        String url = String.format(GEMINI_API_URL, model, apiKey);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        
        return response.getBody();
    }

    private JsonNode extractFunctionCall(JsonNode response) {
        try {
            JsonNode candidates = response.get("candidates");
            if (candidates != null && candidates.isArray() && candidates.size() > 0) {
                JsonNode content = candidates.get(0).get("content");
                if (content != null) {
                    JsonNode parts = content.get("parts");
                    if (parts != null && parts.isArray()) {
                        for (JsonNode part : parts) {
                            if (part.has("functionCall")) {
                                return part.get("functionCall");
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String extractTextResponse(JsonNode response) {
        try {
            JsonNode candidates = response.get("candidates");
            if (candidates != null && candidates.isArray() && candidates.size() > 0) {
                JsonNode content = candidates.get(0).get("content");
                if (content != null) {
                    JsonNode parts = content.get("parts");
                    if (parts != null && parts.isArray() && parts.size() > 0) {
                        JsonNode text = parts.get(0).get("text");
                        if (text != null) {
                            return text.asText();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Nie udało się uzyskać odpowiedzi.";
    }

    private String executeToolCallback(String functionName, JsonNode args) {
        try {
            ToolCallback callback = toolCallbacks.get(functionName);
            if (callback != null) {
                String argsJson = args != null ? objectMapper.writeValueAsString(args) : "{}";
                return callback.call(argsJson);
            }
            return "Nieznane narzędzie: " + functionName;
        } catch (Exception e) {
            e.printStackTrace();
            return "Błąd wykonania narzędzia: " + e.getMessage();
        }
    }

    private String callWithFunctionResult(String originalMessage, String functionName, JsonNode args, String result) throws Exception {
        ObjectNode request = objectMapper.createObjectNode();
        
        ArrayNode contents = request.putArray("contents");
        
        ObjectNode userContent = contents.addObject();
        userContent.put("role", "user");
        userContent.putArray("parts").addObject().put("text", originalMessage);
        
        ObjectNode modelContent = contents.addObject();
        modelContent.put("role", "model");
        ObjectNode functionCallPart = modelContent.putArray("parts").addObject();
        ObjectNode functionCall = functionCallPart.putObject("functionCall");
        functionCall.put("name", functionName);
        if (args != null) {
            functionCall.set("args", args);
        } else {
            functionCall.putObject("args");
        }
        
        ObjectNode functionResponseContent = contents.addObject();
        functionResponseContent.put("role", "user");
        ObjectNode functionResponsePart = functionResponseContent.putArray("parts").addObject();
        ObjectNode functionResponse = functionResponsePart.putObject("functionResponse");
        functionResponse.put("name", functionName);
        ObjectNode responseObj = functionResponse.putObject("response");
        responseObj.put("result", result);
        
        ObjectNode systemInstruction = request.putObject("system_instruction");
        ArrayNode sysParts = systemInstruction.putArray("parts");
        sysParts.addObject().put("text", systemInstructions);
        
        String response = callGeminiApi(request);
        JsonNode responseJson = objectMapper.readTree(response);
        return extractTextResponse(responseJson);
    }
}
