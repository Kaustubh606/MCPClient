package com.aiwork.mcpclient.controller;

import com.aiwork.mcpclient.util.ToolsUtil;
import io.modelcontextprotocol.client.McpSyncClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class MCPClientController {

    private final ChatClient openAiChatClient;
    /*private final ToolCallbackProvider toolCallbackProvider;*/
    private final List<McpSyncClient> mcpCLientList;

    public MCPClientController(ChatClient.Builder chatClientBuilder,
            /*ToolCallbackProvider toolCallbackProvider*/List<McpSyncClient> mcpCLientList) {
        this.openAiChatClient = chatClientBuilder
                .defaultAdvisors(new SimpleLoggerAdvisor())
               /* .defaultTools(toolCallbackProvider)*/
                .build();
        /*this.toolCallbackProvider = toolCallbackProvider;*/
        this.mcpCLientList = mcpCLientList;
    }
    @GetMapping("/openai/chat")
    public String callAiChat(@RequestParam("message") String message) {
        return openAiChatClient.prompt(message).call().content();
    }

   /* @GetMapping("/mcp/tools")
    public List<String> getMcpTools() {
        return Arrays.stream(toolCallbackProvider.getToolCallbacks())
                .map(tool -> tool.getToolDefinition().name())
                .toList();
    }*/

    @GetMapping("/openai/custom-mcp-server")
    public String callCustomMCPServer(@RequestParam("message") String message, @RequestHeader("username") String username) {
        return openAiChatClient.prompt()
                .user(message)
                .toolContext(Map.of(
                        "username", username
                ))
                .call()
                .content();
    }

    @GetMapping("/openai/custom-mcp/tool-filter")
    public String chat(
            @RequestHeader("username") String username,
            @RequestParam("message") String message) {

        ToolCallback[] toolCallbacks =
                ToolsUtil.selectToolsFor(
                        mcpCLientList,
                        "mcp-server-remote",
                        null
                );

        return openAiChatClient.prompt()
                .user(message)
                .tools(toolCallbacks)
                .toolContext(Map.of(
                        "username", username
                ))
                .call()
                .content();
    }
}
