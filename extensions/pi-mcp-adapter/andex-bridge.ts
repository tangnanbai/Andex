// Lightweight handoff between the Pi extension (index.ts) and the Andex
// Script Mod (andex.ts). Keep this module free of pi/MCP SDK imports so the
// two loaders can share it without pulling each other's dependency graphs.

export type McpAndexServerStatus =
  | "connected"
  | "cached"
  | "failed"
  | "needs-auth"
  | "not-connected"
  | "disabled";

export interface McpAndexServerSnapshot {
  name: string;
  status: McpAndexServerStatus;
  toolCount: number;
  resourceCount?: number;
  failedAgoSeconds?: number;
  disabled: boolean;
}

export interface McpAndexSnapshot {
  ready: boolean;
  configPath: string;
  servers: McpAndexServerSnapshot[];
  totalTools: number;
  totalResources: number;
  connectedCount: number;
  disabledCount: number;
  /** Public Pi tool names registered as direct MCP tools in the current session. */
  toolNames?: string[];
}

export interface McpAndexBridge {
  api?: McpAndexBridgeApi;
  onStatusChanged?: (bridge?: McpAndexBridge | undefined) => void;
  getSnapshot(): McpAndexSnapshot;
  reconnect(serverName: string): Promise<{ ok: boolean; message: string }>;
  reconnectAll(): Promise<{ ok: boolean; message: string }>;
  startAuth(serverName: string): Promise<{ ok: boolean; message: string; authorizationUrl?: string }>;
  completeAuth(serverName: string, input: string): Promise<{ ok: boolean; message: string }>;
  inspect(serverName: string, operation: "tools" | "resources" | "prompts"): Promise<{ ok: boolean; message: string; details?: string }>;
  logout(serverName: string): Promise<{ ok: boolean; message: string }>;
  reload(): Promise<{ ok: boolean; message: string }>;
}

interface McpAndexBridgeApi {
  invalidate?: () => void;
}

interface BridgeState {
  api?: McpAndexBridgeApi;
  onBridge?: (bridge: McpAndexBridge) => void;
}

const BRIDGE_KEY = Symbol.for("pi-mcp-adapter.andex-bridge");
const API_KEY = Symbol.for("pi-mcp-adapter.andex-api");

export function readMcpAndexBridge(): McpAndexBridge | undefined {
  return (globalThis as Record<PropertyKey, unknown>)[ BRIDGE_KEY] as McpAndexBridge | undefined;
}

function readBridgeState(): BridgeState | undefined {
  return (globalThis as Record<PropertyKey, unknown>)[API_KEY] as BridgeState | undefined;
}

/** Installed by the Pi extension. Re-registering replaces any previous activation. */
export function registerMcpAndexBridge(bridge: McpAndexBridge): void {
  (globalThis as Record<PropertyKey, unknown>)[BRIDGE_KEY] = bridge;
  const state = readBridgeState();
  if (!state) return;
  if (state.api !== undefined) bridge.api = state.api;
  if (state.onBridge !== undefined) bridge.onStatusChanged = () => state.onBridge?.(bridge);
  state.onBridge?.(bridge);
  state.api?.invalidate?.();
}

/** Remove a bridge installed by this Pi activation. */
export function unregisterMcpAndexBridge(bridge: McpAndexBridge): void {
  const current = readMcpAndexBridge();
  if (current !== bridge) return;
  delete (globalThis as Record<PropertyKey, unknown>)[BRIDGE_KEY];
}

/** Attach the Andex API half of the handoff when the Script Mod loads. */
export function attachMcpAndexApi(
  api: McpAndexBridgeApi,
  onBridge?: (bridge: McpAndexBridge) => void,
): void {
  (globalThis as Record<PropertyKey, unknown>)[API_KEY] = { api, onBridge };
  const bridge = readMcpAndexBridge();
  if (!bridge) return;
  bridge.api = api;
  if (onBridge !== undefined) bridge.onStatusChanged = () => onBridge?.(bridge);
  onBridge?.(bridge);
  api.invalidate?.();
}
