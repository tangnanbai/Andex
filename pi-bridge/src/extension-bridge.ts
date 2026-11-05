import { AsyncLocalStorage } from "node:async_hooks";
import { flushCompileCache } from "node:module";
import { createInterface } from "node:readline";
import { stdin as input, stderr } from "node:process";
import {
  andexAppExtensionSnapshot,
  configureAndexExtensionTransport,
  dispatchAndexAppExtensionEvent,
  invokeAndexAppExtensionAction,
  loadAndexAppExtensions,
} from "./andex-extensions.js";
import {
  installExtensionPackage,
  listDiscoveredSkills,
  listExtensionPackages,
  removeExtensionPackage,
  updateExtensionPackage,
} from "./extension-packages.js";
import { reserveProtocolStdout, writeProtocolFrame } from "./protocol-output.js";
reserveProtocolStdout();
const BRIDGE_VERSION = "2.0.0-alpha.0";
const PI_AI_VERSION = "1.3.0";
const PI_AGENT_CORE_VERSION = "1.3.0";
const PI_CODING_AGENT_VERSION = "1.3.0";
type JsonObject = Record<string, unknown>;
interface BridgeRequest {
  id?: string;
  type?: string;
  payload?: JsonObject;
}
interface PendingHostCall {
  resolve: (result: JsonObject) => void;
  reject: (error: Error) => void;
  timeout: NodeJS.Timeout;
}
const subscriberRequestIds = new Set<string>();
const operationContext = new AsyncLocalStorage<string>();
const activeOperationRequestIds = new Set<string>();
const pendingHostCalls = new Map<string, PendingHostCall>();
let hostCallCounter = 0;
let compileCacheFlushed = false;
let currentLoadOptions = {
  disabledExtensionPaths: [] as string[],
  disabledPackageSources: [] as string[],
};
function asObject(value: unknown): JsonObject {
  return value && typeof value === "object" && !Array.isArray(value)
    ? value as JsonObject
    : {};
}
function asString(value: unknown, fallback = ""): string {
  return typeof value === "string" ? value : fallback;
}
function asBoolean(value: unknown, fallback = false): boolean {
  return typeof value === "boolean" ? value : fallback;
}
function stringArray(value: unknown): string[] {
  return Array.isArray(value)
    ? value.filter((entry): entry is string => typeof entry === "string")
    : [];
}
function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : String(error);
}
function writeFrame(frame: JsonObject): void {
  writeProtocolFrame(frame);
}
function writeEvent(id: string, event: string, payload: JsonObject = {}): void {
  writeFrame({ type: "event", id, event, payload });
}
function writeResponse(id: string, payload: JsonObject = {}): void {
  writeFrame({ type: "response", id, ok: true, payload });
}
function writeError(id: string | undefined, error: unknown, code = "bridge_error"): void {
  writeFrame({
    type: "error",
    id: id ?? "",
    ok: false,
    error: { code, message: errorMessage(error) },
  });
}
function flushStartupCompileCache(): void {
  if (compileCacheFlushed) return;
  compileCacheFlushed = true;
  try {
    flushCompileCache();
  } catch {
    // Compile caching is an optimization and must not affect bridge requests.
  }
}
function emitSubscriberEvent(event: string, payload: JsonObject = {}): void {
  for (const requestId of subscriberRequestIds) writeEvent(requestId, event, payload);
}
function requestHost(method: string, args: JsonObject): Promise<JsonObject> {
  const operationRequestId = operationContext.getStore();
  const requestId = operationRequestId && activeOperationRequestIds.has(operationRequestId)
    ? operationRequestId
    : subscriberRequestIds.values().next().value;
  if (!requestId) throw new Error("The Andex app host is not subscribed.");
  const callId = `andex-host-${Date.now()}-${++hostCallCounter}`;
  writeEvent(requestId, "andex_host_call", { call_id: callId, method, args });
  return new Promise<JsonObject>((resolve, reject) => {
    const timeout = setTimeout(() => {
      pendingHostCalls.delete(callId);
      reject(new Error(`Andex host call timed out: ${method}`));
    }, 2 * 60 * 1000);
    pendingHostCalls.set(callId, { resolve, reject, timeout });
  });
}
async function runOperation<T>(requestId: string, operation: () => Promise<T>): Promise<T> {
  activeOperationRequestIds.add(requestId);
  try {
    return await operationContext.run(requestId, operation);
  } finally {
    activeOperationRequestIds.delete(requestId);
  }
}
function resolveHostCall(payload: JsonObject): boolean {
  const callId = asString(payload.call_id).trim();
  const pending = pendingHostCalls.get(callId);
  if (!pending) return false;
  pendingHostCalls.delete(callId);
  clearTimeout(pending.timeout);
  if (asBoolean(payload.ok, true)) pending.resolve(asObject(payload.result));
  else pending.reject(new Error(asString(payload.error, "Andex host call failed.")));
  return true;
}
configureAndexExtensionTransport({
  requestHost,
  invalidate(version) {
    emitSubscriberEvent("andex_invalidated", { version });
  },
  notify(message, level) {
    emitSubscriberEvent("andex_notification", { message, level });
  },
});
function loadOptions(payload: JsonObject): typeof currentLoadOptions {
  const hasOptions = Object.prototype.hasOwnProperty.call(payload, "disabled_extension_paths") ||
    Object.prototype.hasOwnProperty.call(payload, "disabled_package_sources");
  if (hasOptions) {
    currentLoadOptions = {
      disabledExtensionPaths: stringArray(payload.disabled_extension_paths),
      disabledPackageSources: stringArray(payload.disabled_package_sources),
    };
  }
  return currentLoadOptions;
}
async function installedPackagesPayload(): Promise<JsonObject> {
  return {
    packages: (await listExtensionPackages(process.cwd())).map((installedPackage) => ({
      source: installedPackage.source,
      scope: installedPackage.scope,
      filtered: installedPackage.filtered,
      installed_path: installedPackage.installedPath ?? "",
      name: installedPackage.name,
      version: installedPackage.version,
      description: installedPackage.description,
      extension_count: installedPackage.extensionCount,
      andex_extension_count: installedPackage.andexExtensionCount,
      native_entrypoint_count: installedPackage.nativeEntrypointCount,
      skill_count: installedPackage.skillCount,
      prompt_count: installedPackage.promptCount,
      theme_count: installedPackage.themeCount,
      skill_paths: installedPackage.skillPaths,
    })),
  };
}
async function reloadAndex(id: string, payload: JsonObject): Promise<JsonObject> {
  return runOperation(id, async () => {
    const result = await loadAndexAppExtensions(process.cwd(), loadOptions(payload));
    return {
      ...result,
      snapshot: await andexAppExtensionSnapshot(asObject(payload.context)),
    };
  });
}
async function packageOperation(
  id: string,
  payload: JsonObject,
  operation: (cwd: string, source: string, onProgress: (phase: string) => void) => Promise<unknown>,
  result: JsonObject,
): Promise<JsonObject> {
  const source = asString(payload.source).trim();
  const progress = (phase: string) => writeEvent(id, "extension_package_progress", { phase, source });
  await operation(process.cwd(), source, progress);
  progress("reloading");
  const andexReload = await runOperation(id, () =>
    loadAndexAppExtensions(process.cwd(), loadOptions(payload))
  );
  progress("refreshing");
  return {
    ...result,
    source,
    ...(await installedPackagesPayload()),
    reload: {
      succeeded: true,
      session_count: 0,
      sessions: [],
      andex_reload: andexReload,
      andex: await andexAppExtensionSnapshot(),
    },
  };
}
async function handleRequest(request: BridgeRequest): Promise<void> {
  const id = asString(request.id);
  const type = asString(request.type);
  const payload = asObject(request.payload);
  if (!id) throw new Error("Request id is required.");
  switch (type) {
    case "ping":
      writeResponse(id, {
        bridge_version: BRIDGE_VERSION,
        pi_ai_version: PI_AI_VERSION,
        pi_agent_core_version: PI_AGENT_CORE_VERSION,
        pi_coding_agent_version: PI_CODING_AGENT_VERSION,
        node_version: process.version,
      });
      return;
    case "list_extension_packages":
      writeResponse(id, await installedPackagesPayload());
      return;
    case "list_discovered_skills": {
      const workspaceDirectory = asString(payload.workspace_directory, process.cwd()) || process.cwd();
      const agentDirectory = asString(
        payload.agent_directory,
        `${process.env.HOME ?? "/root"}/.pi/agent`,
      );
      const skills = await listDiscoveredSkills(
        workspaceDirectory,
        agentDirectory,
        stringArray(payload.skill_paths),
      );
      writeResponse(id, {
        skills: skills.map((skill) => ({
          name: skill.name,
          description: skill.description,
          file_path: skill.filePath,
          base_dir: skill.baseDir,
          source: skill.source,
          scope: skill.scope,
          origin: skill.origin,
        })),
      });
      return;
    }
    case "install_extension_package":
      writeResponse(id, await packageOperation(
        id,
        payload,
        installExtensionPackage,
        { installed: true },
      ));
      return;
    case "remove_extension_package": {
      const source = asString(payload.source).trim();
      const removed = await removeExtensionPackage(process.cwd(), source);
      writeResponse(id, await packageOperation(
        id,
        payload,
        async () => undefined,
        { removed },
      ));
      return;
    }
    case "update_extension_package":
      writeResponse(id, await packageOperation(
        id,
        payload,
        updateExtensionPackage,
        { updated: true },
      ));
      return;
    case "reload_all_extensions": {
      const andexReload = await runOperation(id, () =>
        loadAndexAppExtensions(process.cwd(), loadOptions(payload))
      );
      writeResponse(id, {
        succeeded: true,
        session_count: 0,
        sessions: [],
        andex_reload: andexReload,
        andex: await andexAppExtensionSnapshot(),
      });
      return;
    }
    case "reload_andex_extensions":
      writeResponse(id, await reloadAndex(id, payload));
      return;
    case "get_andex_extensions":
      writeResponse(id, await runOperation(id, async () => ({
        snapshot: await andexAppExtensionSnapshot(asObject(payload.context)),
      })));
      return;
    case "invoke_andex_extension_action":
      writeResponse(id, await runOperation(id, async () => ({
        ...(await invokeAndexAppExtensionAction(
          asString(payload.extension_id),
          asString(payload.action),
          asObject(payload.args),
          asObject(payload.context),
        )),
        snapshot: await andexAppExtensionSnapshot(asObject(payload.context)),
      })));
      return;
    case "dispatch_andex_extension_event":
      writeResponse(id, await runOperation(id, async () => ({
        ...(await dispatchAndexAppExtensionEvent(
          asString(payload.event),
          asObject(payload.data),
          asObject(payload.context),
        )),
        snapshot: await andexAppExtensionSnapshot(asObject(payload.context)),
      })));
      return;
    case "subscribe_andex_extensions":
      subscriberRequestIds.add(id);
      writeEvent(id, "andex_invalidated", { subscribed: true });
      return;
    case "unsubscribe_andex_extensions":
      subscriberRequestIds.delete(asString(payload.request_id, id));
      writeResponse(id, { unsubscribed: true });
      return;
    case "andex_host_result":
      writeResponse(id, { accepted: resolveHostCall(payload) });
      return;
    default:
      throw new Error(`Unsupported request type in Extension Bridge: ${type}`);
  }
}
async function main(): Promise<void> {
  if (process.argv.includes("--ping")) {
    writeResponse("ping", {
      bridge_version: BRIDGE_VERSION,
      pi_ai_version: PI_AI_VERSION,
      pi_agent_core_version: PI_AGENT_CORE_VERSION,
      pi_coding_agent_version: PI_CODING_AGENT_VERSION,
      node_version: process.version,
    });
    return;
  }
  const reader = createInterface({ input, crlfDelay: Infinity });
  for await (const line of reader) {
    if (!line.trim()) continue;
    let request: BridgeRequest;
    try {
      request = JSON.parse(line) as BridgeRequest;
    } catch (error) {
      writeError(undefined, error, "invalid_json");
      continue;
    }
    handleRequest(request).then(
      flushStartupCompileCache,
      (error) => {
        flushStartupCompileCache();
        writeError(request.id, error);
      },
    );
  }
}
main().catch((error) => {
  stderr.write(`extension-bridge fatal: ${error instanceof Error ? error.stack ?? error.message : String(error)}\n`);
  process.exitCode = 1;
});
