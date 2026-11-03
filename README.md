# MCPClient

# Spring AI MCP Client
### *** IMP NOTE: Original author of the concept and structure of the code is eazybytes, I have modified it as and when required for my study purpose of AI related concepts learning. ***

A Spring Boot application demonstrating how an AI-powered Java application can act as a **Model Context Protocol (MCP) Client** and interact with multiple MCP servers using both:

- **STDIO transport**
- **Streamable HTTP transport**

The project integrates **Spring AI**, **OpenAI**, and MCP to demonstrate tool discovery, dynamic tool selection, trusted request metadata, tool filtering, server-to-client progress and logging notifications, elicitation, and sampling.

---

# Overview

This application acts as an AI host and MCP Client.

The user sends a natural-language request to the Spring Boot application. An OpenAI model can then decide whether an MCP tool should be invoked.

The MCP Client is currently configured to work with:

```text
STDIO MCP Servers
├── Filesystem MCP Server
└── GitHub MCP Server

Streamable HTTP MCP Server
└── Remote HelpDesk MCP Server
```

The application also contains a backup configuration for connecting to a custom local HelpDesk MCP Server over STDIO.

---

# High-Level Architecture

```text
                         User / Postman
                              |
                              v
                     Spring Boot REST API
                              |
                              v
                         ChatClient
                              |
                              v
                           OpenAI
                              |
                    decides whether a
                    tool is required
                              |
                              v
                         MCP Client
                     /        |        \
                    /         |         \
                   v          v          v

             Filesystem    GitHub     HelpDesk
                MCP         MCP         MCP
               Server      Server      Server

               STDIO       STDIO    Streamable HTTP
```

The LLM does not directly communicate with the external system.

Instead:

```text
User Request
     |
     v
OpenAI LLM
     |
     | chooses MCP Tool
     v
Spring AI MCP Client
     |
     v
MCP Server
     |
     v
External Capability
```

---

# Technology Stack

- Java 25
- Spring Boot 4.0.9
- Spring AI 2.0.1
- OpenAI
- Model Context Protocol
- MCP STDIO Transport
- MCP Streamable HTTP Transport
- Spring MVC
- Maven
- Docker
- Node.js / NPX
- GitHub MCP Server
- Filesystem MCP Server

---

# Main Dependencies

The project uses the Spring AI MCP Client starter:

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-client</artifactId>
</dependency>
```

OpenAI integration:

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-openai</artifactId>
</dependency>
```

Spring Web MVC:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc</artifactId>
</dependency>
```

---

# Application Configuration

The MCP Client runs on:

```text
http://localhost:8081
```

Example configuration:

```yaml
server:
  port: 8081

spring:
  application:
    name: mcpclient

  ai:
    openai:
      api-key: ${OPENAI_API_KEY}

    mcp:
      client:
        enabled: true
        type: SYNC
        request-timeout: 60s

        stdio:
          servers-configuration: classpath:mcp-servers.json

        streamable-http:
          connections:
            helpdesk-remote:
              url: http://localhost:8082
              endpoint: /mcp
```

The application uses a synchronous MCP client:

```text
type = SYNC
```

with a request timeout of:

```text
60 seconds
```

---

# Supported MCP Transports

## 1. STDIO

STDIO MCP servers are configured using:

```text
src/main/resources/mcp-servers.json
```

The MCP Client starts the configured processes and communicates with them using:

```text
STDIN
STDOUT
```

Conceptually:

```text
Spring AI MCP Client
        |
        | starts process
        v
    MCP Server
        |
        | stdin/stdout
        v
    MCP Protocol
```

---

## 2. Streamable HTTP

The HelpDesk MCP Server runs independently as a remote Spring Boot application.

Connection:

```yaml
streamable-http:
  connections:
    helpdesk-remote:
      url: http://localhost:8082
      endpoint: /mcp
```

Architecture:

```text
MCP Client
    |
    | HTTP
    v
http://localhost:8082/mcp
    |
    v
Remote HelpDesk MCP Server
```

Unlike STDIO, the MCP Client does not launch the remote HelpDesk server process.

The remote MCP Server must already be running.

---

# STDIO — Filesystem MCP Server

The application integrates the official MCP filesystem server using NPX.

Current configuration:

```json
{
  "filesystem": {
    "command": "cmd.exe",
    "args": [
      "/c",
      "npx.cmd",
      "-y",
      "@modelcontextprotocol/server-filesystem",
      "<ALLOWED_DIRECTORY>"
    ]
  }
}
```

Replace:

```text
<ALLOWED_DIRECTORY>
```

with a local directory that the MCP filesystem server is allowed to access.

Example:

```text
E:\Projects\MCPResource
```

The filesystem MCP server should only be granted access to directories that are safe for AI-assisted operations.

---

# STDIO — GitHub MCP Server

The application also integrates the GitHub MCP Server using Docker.

Configuration:

```json
{
  "github": {
    "command": "docker",
    "args": [
      "run",
      "-i",
      "--rm",
      "-e",
      "GITHUB_PERSONAL_ACCESS_TOKEN",
      "ghcr.io/github/github-mcp-server"
    ]
  }
}
```

The GitHub Personal Access Token is supplied using the environment variable:

```text
GITHUB_PERSONAL_ACCESS_TOKEN
```

The token itself is not stored inside the MCP configuration file.

---

# Custom HelpDesk STDIO Server

The project also contains:

```text
mcp-servers-stdio-backup-local.json
```

which demonstrates connecting to the custom Spring AI HelpDesk MCP Server over STDIO.

Example:

```json
{
  "mcp-server-stdio": {
    "command": "java",
    "args": [
      "-jar",
      "<PATH_TO_MCP_STDIO_SERVER_JAR>"
    ]
  }
}
```

This configuration is retained as a local STDIO example/backup.

The active `mcp-servers.json` currently contains:

```text
Filesystem MCP Server
GitHub MCP Server
```

while the HelpDesk application is currently connected using Streamable HTTP.

---

# Remote HelpDesk MCP Server

The MCP Client connects to the custom HelpDesk server using:

```text
Connection Name:
helpdesk-remote
```

URL:

```text
http://localhost:8082/mcp
```

The HelpDesk MCP Server exposes backend capabilities such as support-ticket operations.

Architecture:

```text
User
  |
  v
MCP Client REST API
  |
  v
OpenAI
  |
  | chooses HelpDesk tool
  v
MCP Client
  |
  | Streamable HTTP
  v
Remote HelpDesk MCP Server
  |
  v
HelpDesk Service
  |
  v
Database
```

---

# Client Connection Name vs Server Name

An important MCP distinction demonstrated by this project is the difference between:

```text
Client Connection Name
```

and:

```text
MCP Server Advertised Name
```

The Client connection is configured as:

```yaml
connections:
  helpdesk-remote:
```

Therefore client-side handlers use:

```java
clients = "helpdesk-remote"
```

Examples:

```java
@McpElicitation(clients = "helpdesk-remote")
```

```java
@McpSampling(clients = "helpdesk-remote")
```

```java
@McpProgress(clients = "helpdesk-remote")
```

```java
@McpLogging(clients = "helpdesk-remote")
```

The HelpDesk MCP Server itself advertises a different MCP server name:

```text
mcp-server-remote
```

That advertised server name is used by request-level tool selection.

---

# Basic OpenAI Chat

A basic endpoint is available without MCP tool selection:

```http
GET /api/openai/chat?message=Hello
```

Flow:

```text
User
  |
  v
Spring ChatClient
  |
  v
OpenAI
  |
  v
Response
```

---

# MCP Tool Calling

The application can expose selected MCP tools to the LLM.

Conceptually:

```text
User:
"Show my support tickets"

        |
        v

OpenAI
        |
        | determines that a tool is required
        v

MCP Tool Callback
        |
        v

Remote MCP Server
        |
        v

Backend Service
```

The controller never directly calls the HelpDesk backend.

The LLM chooses from the MCP tools that the application makes available for the request.

---

# Request-Based MCP Tool Selection

The project contains a custom:

```java
ToolsUtil
```

which dynamically selects tools based on:

```text
MCP Server Name
Tool Name
```

Example:

```java
ToolCallback[] toolCallbacks =
        ToolsUtil.selectToolsFor(
            mcpClientList,
            "mcp-server-remote",
            null
        );
```

Here:

```text
serverName = mcp-server-remote
toolName   = null
```

means:

> Select all tools belonging to the MCP server whose advertised name matches `mcp-server-remote`.

The resulting callbacks are attached only to the current AI request:

```java
.tools(toolCallbacks)
```

This enables request-specific control over which MCP tools are exposed to the LLM.

---

# ToolsUtil

The selection utility queries every connected MCP client:

```java
client.listTools()
```

and compares:

```text
client.getServerInfo().name()
```

and:

```text
tool.name()
```

against the supplied hints.

Conceptually:

```text
All Connected MCP Clients
          |
          v
      listTools()
          |
          v
Server-name filtering
          |
          v
Tool-name filtering
          |
          v
SyncMcpToolCallback[]
          |
          v
Current Chat Request
```

A `null` or blank filtering hint means:

```text
Match everything
```

for that field.

---

# Global MCP Tool Filtering

The project also implements:

```java
McpServerToolFilter
```

using:

```java
McpToolFilter
```

The filter evaluates discovered MCP tools and can globally reject tools.

Current filtering rules reject:

```text
Tools from GitHub MCP Server
Tools containing "write_"
Tools containing "file"
```

Example:

```java
if (serverName.toLowerCase().contains("github")) {
    return false;
}

if (toolName.toLowerCase().contains("write_")) {
    return false;
}

if (toolName.toLowerCase().contains("file")) {
    return false;
}
```

In an MCP tool filter:

```text
true  = tool allowed
false = tool rejected
```

This demonstrates a simple allow/deny governance mechanism around discovered MCP capabilities.

---

# Global Filtering vs Request Filtering

This project demonstrates two different filtering concepts.

## Global Filtering

Implemented using:

```text
McpToolFilter
```

It applies policy-oriented filtering to discovered MCP tools.

Example:

```text
Block GitHub tools
Block write tools
Block file-related tools
```

## Request-Specific Filtering

Implemented using:

```text
ToolsUtil.selectToolsFor(...)
```

The controller selects the tools required for one particular request.

Conceptually:

```text
All MCP Tools
      |
      +---- Global Governance Filter
      |
      v
Available MCP Capability Set
      |
      +---- Request-Level Selection
      |
      v
Tools given to LLM for this request
```

---

# Trusted Application Context

The HelpDesk endpoint receives the username through an HTTP header:

```text
username
```

Instead of adding trusted identity information as a normal LLM-generated tool parameter, the application passes it using:

```java
.toolContext(
    Map.of(
        "username", username
    )
)
```

For more advanced calls:

```java
.toolContext(
    Map.of(
        "username", username,
        "progressToken", UUID.randomUUID().toString()
    )
)
```

Architecture:

```text
HTTP Request Header
        |
        v
Spring MCP Client
        |
        | ToolContext
        v
MCP Request Metadata
        |
        v
Remote MCP Server
        |
        v
Server Tool
```

This keeps trusted application-owned information separate from model-generated arguments.

---

# Progress Token

The application creates a unique identifier:

```java
UUID.randomUUID().toString()
```

and sends it as:

```text
progressToken
```

inside the request context.

Example:

```java
.toolContext(Map.of(
    "username", username,
    "progressToken", UUID.randomUUID().toString()
))
```

The remote MCP Server can associate progress notifications with the originating request.

---

# MCP Progress Notifications

The Client handles server progress notifications using:

```java
@McpProgress(clients = "helpdesk-remote")
```

Example:

```java
public void onProgressUpdate(
        McpSchema.ProgressNotification notification) {

    log.info(
        "Progress Update: {}% completed for request ID {}: Message: {}",
        notification.progress(),
        notification.progressToken(),
        notification.message()
    );
}
```

Flow:

```text
MCP Server
    |
    | progress notification
    v
MCP Client
    |
    v
@McpProgress Handler
    |
    v
Application Log
```

A typical sequence could be:

```text
20%
40%
60%
80%
100%
```

with the same request correlation token.

---

# MCP Logging Notifications

The application listens for structured logs sent by the remote MCP Server.

Handler:

```java
@McpLogging(clients = "helpdesk-remote")
```

The callback receives:

```text
Logging Level
Source
Message
```

Architecture:

```text
Remote MCP Server
       |
       | structured MCP log notification
       v
MCP Client
       |
       v
HelpDeskLogBridge
       |
       v
Application Logs
```

This is different from normal server-side Java logging.

Normal logging:

```text
Server log file / console
```

MCP logging:

```text
Server
   |
   | MCP notification
   v
Client
```

---

# MCP Elicitation

The project demonstrates MCP Elicitation using:

```java
@McpElicitation(clients = "helpdesk-remote")
```

Elicitation allows the MCP Server to request additional information from the Client during tool execution.

Example use case:

```text
User:
"My VPN is not working.
Create a ticket."
```

The server may require:

```text
Priority
Contact Phone Number
```

Flow:

```text
User
  |
  v
LLM
  |
  v
createTicket MCP Tool
  |
  v
Remote MCP Server
  |
  | needs additional information
  v
Elicitation Request
  |
  v
MCP Client
  |
  v
@McpElicitation Handler
  |
  v
ElicitResult
  |
  v
Remote MCP Server
  |
  v
Continue original tool execution
```

---

# Current Elicitation Demo

The current implementation simulates a user completing a form.

Example:

```java
Map<String, Object> userResponse = Map.of(
    "priority", "HIGH",
    "contactPhone", "+1-202-555-0185"
);
```

The handler returns:

```java
McpSchema.ElicitResult
        .builder(McpSchema.ElicitResult.Action.ACCEPT)
        .content(userResponse)
        .build();
```

This is intentionally a learning/demo implementation.

In a real production application:

```text
Elicitation Request
       |
       v
Web / Mobile UI
       |
       v
User fills requested fields
       |
       v
Client submits answer
       |
       v
ElicitResult
       |
       v
MCP Server continues processing
```

The original MCP request remains pending until the elicitation interaction is completed or cancelled.

---

# MCP Sampling

The project demonstrates MCP Sampling using:

```java
@McpSampling(clients = "helpdesk-remote")
```

Sampling reverses the usual AI direction.

Normally:

```text
MCP Client
    |
    v
LLM
    |
    v
MCP Server Tool
```

With sampling:

```text
MCP Server
    |
    | requests AI generation
    v
MCP Client
    |
    v
LLM
    |
    v
MCP Client
    |
    v
MCP Server
```

This allows an MCP Server to request AI processing from the connected host without directly owning the application's LLM integration.

---

# Sampling Implementation

The sampling handler receives:

```java
McpSchema.CreateMessageRequest
```

It translates the MCP request into Spring AI messages:

```text
MCP System Prompt
       |
       v
SystemMessage

MCP User Messages
       |
       v
UserMessage
```

Then invokes:

```java
ChatModel.call(...)
```

directly.

```java
ChatResponse response =
        chatModel.call(
            new Prompt(messages)
        );
```

Using `ChatModel` directly is intentional.

It prevents the sampling request from entering another ChatClient flow that could expose MCP tools again and accidentally create recursive MCP tool calls.

---

# Sampling Response

After receiving the LLM response, the Client returns an MCP:

```java
CreateMessageResult
```

containing:

```text
Role = ASSISTANT
Generated Text
Model Name
```

Flow:

```text
HelpDesk MCP Server
       |
       | sampling request
       v
MCP Client
       |
       v
ChatModel
       |
       v
OpenAI
       |
       v
Generated Summary
       |
       v
CreateMessageResult
       |
       v
HelpDesk MCP Server
```

---

# Ticket Summarization Demo

The controller contains a dedicated endpoint:

```http
GET /api/openai/custom-mcp/summarize-tickets
```

Header:

```text
username: <username>
```

Flow:

```text
User
  |
  v
MCP Client Controller
  |
  v
OpenAI
  |
  | chooses summarize ticket tool
  v
Remote HelpDesk MCP Server
  |
  | fetches ticket data
  |
  | sends Sampling request
  v
MCP Client Sampling Handler
  |
  v
OpenAI ChatModel
  |
  v
Natural Language Summary
  |
  v
Remote MCP Server
  |
  v
Tool Result
  |
  v
Original Chat Flow
```

This demonstrates a complete server-to-client-to-LLM callback flow.

---

# Preventing Unnecessary LLM Rewriting

The summarization endpoint contains a strict system prompt instructing the outer LLM to return the MCP tool output exactly as received.

The goal is to reduce situations where an LLM takes a correct backend result and adds unsupported information.

Example undesirable behavior:

```text
Tool:
Ticket #62 created.

LLM invents:
A technician will contact you shortly.
Your ticket has been escalated.
Please restart your VPN.
```

For business applications:

```text
MCP Tool Result
```

should be treated as authoritative data.

The model should not invent:

```text
Ticket status
ETA
Escalation
Technician actions
Contact methods
Troubleshooting procedures
```

unless those facts were actually returned by the backend system.

---

# Hallucination Reduction Principle

A key learning from this project is:

> Do not rely only on prompt engineering to reduce hallucination. Reduce how much authoritative information the LLM is allowed to invent.

Architecture:

```text
User Intent
    |
    v
LLM
    |
    | determines required capability
    v
MCP Tool
    |
    v
Authoritative Backend
    |
    v
Verified Tool Result
```

The LLM can orchestrate the interaction, while actual business facts come from deterministic systems.

---

# REST Endpoints

## Basic OpenAI Chat

```http
GET /api/openai/chat
```

Example:

```text
/api/openai/chat?message=Hello
```

---

## Custom MCP Server Endpoint

```http
GET /api/openai/custom-mcp-server
```

Parameters:

```text
message=<message>
```

Header:

```text
username=<username>
```

The current source keeps automatic/default MCP tool registration commented out for experimentation.

Therefore this endpoint should be considered part of the learning evolution of the project rather than the primary request-filtered MCP flow.

---

## Request-Filtered Remote MCP Call

```http
GET /api/openai/custom-mcp/tool-filter
```

Parameters:

```text
message=<message>
```

Header:

```text
username=<username>
```

This endpoint dynamically selects tools from:

```text
mcp-server-remote
```

and adds:

```text
username
progressToken
```

to the request context.

This is the primary example of controlled MCP tool exposure.

---

## Ticket Sampling Demo

```http
GET /api/openai/custom-mcp/summarize-tickets
```

Header:

```text
username=<username>
```

This endpoint demonstrates the complete MCP Sampling workflow.

---

# Simple Logger Advisor

The ChatClient is configured with:

```java
new SimpleLoggerAdvisor()
```

This provides logging around ChatClient interactions and is useful while learning and troubleshooting tool-selection behavior.

The corresponding logging level is configured as:

```yaml
logging:
  level:
    org.springframework.ai.chat.client.advisor: DEBUG
```

---

# Project Structure

```text
src/main/java/com/aiwork/mcpclient
│
├── McpclientApplication.java
│
├── controller
│   └── MCPClientController.java
│
└── util
    ├── HelpDeskElicitationProvider.java
    ├── HelpDeskLogBridge.java
    ├── HelpDeskSamplingProvider.java
    ├── HelpDeskToolProgressListener.java
    ├── McpServerToolFilter.java
    └── ToolsUtil.java
```

Resources:

```text
src/main/resources
│
├── application.yaml
├── mcp-servers.json
└── mcp-servers-stdio-backup-local.json
```

---

# Responsibilities

## MCPClientController

Responsible for:

```text
REST endpoints
ChatClient interaction
Per-request MCP tool selection
ToolContext metadata
Progress correlation token
Sampling demonstration orchestration
```

---

## ToolsUtil

Responsible for:

```text
Dynamic MCP server selection
Dynamic tool selection
Creation of SyncMcpToolCallback instances
```

---

## McpServerToolFilter

Responsible for:

```text
Global MCP tool filtering
Allow/deny decisions
Tool governance
```

---

## HelpDeskToolProgressListener

Handles:

```text
MCP Progress Notifications
```

---

## HelpDeskLogBridge

Handles:

```text
MCP Logging Notifications
```

---

## HelpDeskElicitationProvider

Handles:

```text
MCP Elicitation Requests
```

The current implementation simulates user input.

---

## HelpDeskSamplingProvider

Handles:

```text
MCP Sampling Requests
```

and invokes the configured OpenAI `ChatModel`.

---

# Running the Application

## Prerequisites

Install:

```text
Java 25
Maven
Docker Desktop
Node.js / NPX
```

You also need:

```text
OpenAI API Key
GitHub Personal Access Token
```

For the HelpDesk integration, run the Remote MCP Server separately on:

```text
http://localhost:8082
```

---

# Environment Variables

Configure:

```text
OPENAI_API_KEY
GITHUB_PERSONAL_ACCESS_TOKEN
```

Do not commit their actual values.

---

# Local Path Configuration

The supplied STDIO configuration contains machine-specific Windows paths.

Before running the project, replace paths such as:

```text
E:\Study\AI\...
```

with paths valid for your machine.

This applies particularly to:

```text
Filesystem MCP allowed directory
Custom STDIO MCP Server JAR path
Java executable path
```

Avoid committing personal or sensitive filesystem locations when preparing a public portfolio repository.

---

# Build

```bash
mvn clean package
```

---

# Run

```bash
mvn spring-boot:run
```

or:

```bash
java -jar target/mcpclient-0.0.1-SNAPSHOT.jar
```

The application starts on:

```text
http://localhost:8081
```

---

# Starting the Remote HelpDesk MCP Server

Before testing Streamable HTTP features, start the HelpDesk Remote MCP Server.

Expected endpoint:

```text
http://localhost:8082/mcp
```

The MCP Client will establish the connection using:

```yaml
helpdesk-remote:
  url: http://localhost:8082
  endpoint: /mcp
```

---

# Running the GitHub MCP Server

The MCP Client launches the GitHub MCP Server using Docker:

```text
docker run -i --rm \
  -e GITHUB_PERSONAL_ACCESS_TOKEN \
  ghcr.io/github/github-mcp-server
```

Ensure:

```text
Docker Desktop is running
```

and:

```text
GITHUB_PERSONAL_ACCESS_TOKEN
```

is available to the MCP Client process.

---

# Running the Filesystem MCP Server

The MCP Client launches the filesystem MCP Server using:

```text
npx
```

Ensure Node.js and NPX are installed.

The configured directory acts as the server's allowed filesystem root.

---

# MCP Client Feature Matrix

| Capability | Transport | Implemented |
|---|---|---:|
| Filesystem MCP Server | STDIO | ✅ |
| GitHub MCP Server | STDIO | ✅ |
| Custom HelpDesk STDIO configuration | STDIO | ✅ Backup/Example |
| Remote HelpDesk MCP Server | Streamable HTTP | ✅ |
| MCP Tool Discovery | Both | ✅ |
| Dynamic Tool Selection | MCP Client | ✅ |
| Global Tool Filtering | MCP Client | ✅ |
| Trusted Metadata | Remote HelpDesk | ✅ |
| Progress Notifications | Remote HelpDesk | ✅ |
| Logging Notifications | Remote HelpDesk | ✅ |
| Elicitation | Remote HelpDesk | ✅ |
| Sampling | Remote HelpDesk | ✅ |
| OpenAI Integration | Client Host | ✅ |
| Request Correlation | Remote HelpDesk | ✅ |

---

# MCP Interaction Summary

This project demonstrates several MCP interaction directions.

## Normal Tool Calling

```text
Client → Server
```

```text
LLM
 |
 v
MCP Tool
 |
 v
MCP Server
```

---

## Progress

```text
Server → Client
```

```text
MCP Server
 |
 v
Progress Notification
 |
 v
MCP Client
```

---

## Logging

```text
Server → Client
```

```text
MCP Server
 |
 v
Structured Log
 |
 v
MCP Client
```

---

## Elicitation

```text
Server → Client → User → Server
```

```text
MCP Server
 |
 | asks for additional information
 v
MCP Client
 |
 v
User / UI
 |
 v
ElicitResult
 |
 v
MCP Server
```

---

## Sampling

```text
Server → Client → LLM → Server
```

```text
MCP Server
 |
 | asks client to generate content
 v
MCP Client
 |
 v
OpenAI
 |
 v
MCP Client
 |
 v
MCP Server
```

---

# Important Enterprise Design Principle

The project separates:

```text
LLM Decision Making
```

from:

```text
Authoritative Business Operations
```

The AI layer can decide:

```text
Which tool should be called?
```

but business capabilities remain behind MCP Servers.

```text
User
 |
 v
LLM
 |
 | orchestration
 v
MCP
 |
 v
Backend Capability
```

This helps preserve traditional enterprise architecture while enabling agentic AI workflows.

---

# Security Considerations

Before using this project outside a learning environment:

- Never commit API keys or PAT tokens
- Restrict GitHub token scopes
- Restrict filesystem MCP access to safe directories
- Authenticate remote MCP endpoints
- Use TLS for remote MCP communication
- Validate metadata supplied to MCP servers
- Implement user/tenant authorization
- Apply tool-level access-control policies
- Audit sensitive tool invocations
- Avoid exposing unrestricted write/delete tools to an LLM
- Use proper secret-management solutions

The included `McpServerToolFilter` is a simple demonstration of tool governance and is not intended to replace a complete authorization system.

---

# Current Learning Coverage

This repository demonstrates:

- Spring AI MCP Client
- MCP Tool Calling
- Multiple MCP servers
- STDIO transport
- Streamable HTTP transport
- OpenAI + MCP integration
- Synchronous MCP client
- MCP Tool discovery
- `SyncMcpToolCallback`
- Dynamic request-specific tool selection
- `McpToolFilter`
- Tool governance
- Trusted `ToolContext`
- MCP metadata propagation
- Progress tokens
- MCP Progress notifications
- MCP Logging notifications
- MCP Elicitation
- Simulated human-in-the-loop input
- MCP Sampling
- Spring AI `ChatModel`
- Avoiding recursive MCP tool invocation during sampling
- Multiple MCP connection naming concepts
- Server-name vs connection-name distinction
- Hallucination reduction around authoritative tool responses
- Filesystem MCP integration
- GitHub MCP integration
- Custom Spring AI HelpDesk MCP integration

---

# Relationship with Companion Projects

This MCP Client is designed to work alongside two custom learning projects.

```text
1. MCP Server — STDIO
2. MCP Server — Streamable HTTP
3. MCP Client
```

Together they demonstrate the progression:

```text
Local MCP Server
      |
      v
STDIO Transport
      |
      v
Remote MCP Server
      |
      v
Streamable HTTP
      |
      v
Multi-Server MCP Client
      |
      v
Advanced MCP Interactions
```

including:

```text
Tool Calling
Metadata
Filtering
Progress
Logging
Elicitation
Sampling
```

---

# Possible Future Enhancements

Potential next steps include:

- Real UI-based elicitation instead of simulated responses
- WebSocket/SSE bridge for human-in-the-loop requests
- Per-user MCP session isolation
- OAuth/JWT authentication for remote MCP servers
- Fine-grained tool authorization
- Role-based MCP tool visibility
- Persistent audit trail
- Tool-call metrics
- Prometheus/Grafana monitoring
- Retry and timeout policies
- Circuit breakers
- Multiple LLM providers
- Model fallback
- Agent orchestration
- MCP Resources
- MCP Prompts
- Production-grade approval workflows
- Automated integration tests

---

# Project Purpose

This repository is a hands-on learning project focused on understanding how **Model Context Protocol can connect AI applications with existing tools and enterprise backend systems using Java and Spring AI**.

The goal is not simply to call an LLM.

The project explores how an AI host can:

```text
Discover capabilities
Select tools
Control tool exposure
Carry trusted context
Receive server notifications
Request human input
Delegate AI generation
Connect multiple MCP servers
```

while preserving a normal backend engineering architecture.

The core idea is:

```text
LLM = reasoning and orchestration

MCP = standardized capability integration

Java Services / External Systems = authoritative execution
```

---

## Disclaimer

This repository is intended for learning and demonstration purposes.

Production deployments would require additional controls around authentication, authorization, secrets, network security, session isolation, auditability, observability, resilience, and human approval for sensitive actions.
