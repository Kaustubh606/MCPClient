package com.aiwork.mcpclient.util;

import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpProgress;
import org.springframework.stereotype.Component;

@Component
public class HelpDeskToolProgressListener {
    private static final Logger log = LoggerFactory.getLogger(HelpDeskToolProgressListener.class);

    @McpProgress(clients = "helpdesk-remote")
    public void onProgressUpdate(McpSchema.ProgressNotification notification){
        log.info("Progress Update: {}% completed for request ID {}: Message: {}",
                notification.progress(),notification.progressToken(),notification.message());
    }
}
