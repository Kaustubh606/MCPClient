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
import java.util.UUID;

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
                        "username", username,
                        "progressToken", UUID.randomUUID().toString()
                ))
                .call()
                .content();
    }

    /**
     * Dedicated endpoint for the MCP sampling demo. We still let the chat LLM drive: it sees
     * the {@code summarizeTickets} tool and decides to call it. That tool runs on the server,
     * which then calls back into this client's {@code @McpSampling} handler to generate the
     * natural-language summary. The controller never invokes the tool directly.
     */
    @GetMapping("/openai/custom-mcp/summarize-tickets")
    public String summarizeTickets(@RequestHeader("username") String username) {
        ToolCallback[] toolCallbacks = ToolsUtil.selectToolsFor(mcpCLientList, "mcp-server-remote", null);
        return openAiChatClient.prompt()
                .system("""
                        You orchestrate the 'summarizeTickets' tool. The tool already returns a complete,
                        customer-ready summary that was generated for this exact request. Return that tool
                        output to the user EXACTLY as-is: do not rewrite, reformat, shorten, expand,
                        rephrase, or add any commentary of your own. Your reply must be the verbatim tool
                        response and nothing else.
                        """)
                .user("Summarize all of my support tickets. My username is " + username)
                .tools(toolCallbacks)
                .toolContext(Map.of(
                        "username", username,
                        "progressToken", UUID.randomUUID().toString()))
                .call()
                .content();
    }

}
