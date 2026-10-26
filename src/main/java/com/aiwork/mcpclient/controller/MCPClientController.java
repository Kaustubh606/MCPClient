package com.aiwork.mcpclient.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api")
public class MCPClientController {

    private final ChatClient openAiChatClient;
    private final ToolCallbackProvider toolCallbackProvider;
    public MCPClientController(ChatClient.Builder chatClientBuilder, ToolCallbackProvider toolCallbackProvider) {
        this.openAiChatClient = chatClientBuilder.defaultAdvisors(new SimpleLoggerAdvisor())
                .defaultTools(toolCallbackProvider)
                .build();
        this.toolCallbackProvider = toolCallbackProvider;
    }
    @GetMapping("/openai/chat")
    public String callAiChat(@RequestParam("message") String message) {
        return openAiChatClient.prompt(message).call().content();
    }

    @GetMapping("/mcp/tools")
    public List<String> getMcpTools() {
        return Arrays.stream(toolCallbackProvider.getToolCallbacks())
                .map(tool -> tool.getToolDefinition().name())
                .toList();
    }
}
