# Overview

The Model Context Protocol allows applications to provide context for LLMs in a standardized way, separating the concerns of providing context from the actual LLM interaction.

## Key Features of this Python SDK

- Build MCP clients that can connect to any MCP server
- Create MCP servers that expose resources, prompts, and tools
- Use standard transports like stdio and SSE
- Handle all MCP protocol messages and lifecycle events
## MCP Primitives

The MCP protocol defines three core primitives that servers can implement: