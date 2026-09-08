# Codex MCP Client Configuration Example

This directory contains a sample client configuration for using a running
Textus CBD Support MCP server from Codex. It is not loaded automatically by
this server repository.

Copy or merge `config.toml` into the `.codex/config.toml` associated with the
Codex user's own workspace. Keep any existing client configuration when
merging it.

The sample endpoint is:

```text
http://127.0.0.1:19534/mcp
```

`127.0.0.1` means the MCP server runs on the same machine as Codex. Replace the
host and port with the reachable CBD Support server endpoint when Codex and the
server run in different environments. Remote exposure requires an appropriate
transport-security, authentication, authorization, and network policy; the
loopback sample does not define that deployment policy.

The sample enables only the public read-only `CbdRetrieval` tools. Tool
approval is a client-owned policy. Change `default_tools_approval_mode` or the
`enabled_tools` list to match the user's local policy.

## Optional Manual Probe

To call one public tool without changing Codex configuration, run the
repository-owned helper from the repository root:

```sh
scripts/run-mcp-tool.sh CbdSupport.CbdRetrieval.status '{}'
```

Set `CBD_SUPPORT_MCP_ENDPOINT` to override the default loopback endpoint. The
helper sends one MCP `tools/call` request; it does not install client
configuration, start the server, or broaden the server's authorization policy.
It limits connection setup to 5 seconds, the complete call to 60 seconds, and
the response to 16 MiB. Bounded overrides are available through
`CBD_SUPPORT_MCP_CONNECT_TIMEOUT_SECONDS`,
`CBD_SUPPORT_MCP_TOOL_TIMEOUT_SECONDS`, and
`CBD_SUPPORT_MCP_MAX_RESPONSE_BYTES`.
