package com.aiwork.mcpclient.util;

import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.McpConnectionInfo;
import org.springframework.ai.mcp.McpToolFilter;
import org.springframework.stereotype.Component;

@Component
public class McpServerToolFilter implements McpToolFilter {
    private static final Logger logger = LoggerFactory.getLogger(McpServerToolFilter.class);
    @Override
    public boolean test(McpConnectionInfo mcpConnectionInfo, McpSchema.Tool tool) {
        String serverName = mcpConnectionInfo.initializeResult()
                .serverInfo()
                .name();
        String toolName = tool.name();
        logger.info("Evaluating tool '{}' from MCP server '{}'", toolName, serverName);
        if (serverName.toLowerCase().contains("github")) {
            logger.warn("Tool '{}' rejected because it belongs to the blocked MCP server '{}'",
                    toolName, serverName);
            return false;
        }
        if (toolName.toLowerCase().contains("write_")) {
            logger.warn("Tool '{}' rejected because it belongs to the blocked MCP server '{}'",
                    toolName, serverName);
            return false;
        }
        if (toolName.toLowerCase().contains("file")) {
            logger.warn("Tool '{}' rejected because it belongs to the blocked MCP server '{}'",
                    toolName, serverName);
            return false;
        }
        logger.info("Tool '{}' approved from MCP server '{}'",toolName, serverName );
        return true;
    }
}
