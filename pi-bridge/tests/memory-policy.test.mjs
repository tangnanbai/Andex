import { test, after } from "node:test";
import assert from "node:assert/strict";
import { mkdtemp, rm, readFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { build } from "esbuild";
import { pathToFileURL } from "node:url";

const directory = await mkdtemp(join(tmpdir(), "andex-memory-test-"));
await build({ entryPoints: ["src/context-policy.ts", "../extensions/pi-long-memory/store.ts"],
  outdir: directory, outbase: "..", platform: "node", format: "esm", outExtension: { ".js": ".mjs" } });
const { automaticCompactionSettings } = await import(pathToFileURL(join(directory, "pi-bridge/src/context-policy.mjs")));
const { PersistentMemoryStore } = await import(pathToFileURL(join(directory, "extensions/pi-long-memory/store.mjs")));
after(() => rm(directory, { recursive: true, force: true }));

test("manual edits persist, deduplicate and reject stale concurrent overwrites", async () => {
  const store = new PersistentMemoryStore(join(directory, "manual-editor"));
  await store.editDocument("user", "Use Chinese.\n\n---\n\nUse Chinese!", "");
  assert.equal(await new PersistentMemoryStore(store.directory).read("user"), "Use Chinese.");
  await store.update("user", "add", "New preference");
  await assert.rejects(store.editDocument("user", "stale edit", "Use Chinese."), /已被更新/);
  assert.ok((await store.read("user")).includes("New preference"));
  await assert.rejects(store.editDocument("memory", "token=sk-abcdefghijklmnopqrstuv", ""), /API Key/);
  await store.editDocument("user", "a".repeat(4001), await store.read("user"));
  assert.equal((await store.read("user")).length, 4001);
});

test("native Pi strict threshold triggers at 80% for every configured window", () => {
  for (const window of [8_000, 128_000, 256_000, 512_000, 1_024_000]) {
    const settings = automaticCompactionSettings(window);
    const boundary = Math.floor(window * .8);
    assert.equal(boundary - 1 > window - settings.reserveTokens, false);
    assert.equal(boundary > window - settings.reserveTokens, true);
    assert.ok(settings.keepRecentTokens < boundary);
  }
});
test("memory persists across store instances and separates user profile", async () => {
  const store = new PersistentMemoryStore(join(directory, "persist"));
  assert.equal(await store.enabled(), true);
  await store.update("user", "add", "用户喜欢简体中文。");
  await store.update("memory", "add", "项目包名 com.android.andex");
  const restarted = new PersistentMemoryStore(store.directory);
  assert.equal(await restarted.read("user"), "用户喜欢简体中文。");
  assert.equal(await readFile(join(store.directory, "MEMORY.md"), "utf8"), "项目包名 com.android.andex");
  await restarted.setEnabled(false);
  assert.equal(await store.enabled(), false);
  assert.ok((await store.snapshot()).includes("简体中文"));
});
test("deduplication, unique replacement, removal, limits and credential rejection", async () => {
  const store = new PersistentMemoryStore(join(directory, "edit"));
  await store.update("memory", "add", "lesson one");
  await store.update("memory", "add", "lesson one");
  assert.equal(await store.read("memory"), "lesson one");
  await store.update("memory", "add", "lesson two");
  await assert.rejects(store.update("memory", "replace", "bad", "lesson"), /exactly one/);
  await store.update("memory", "replace", "updated one", "lesson one");
  await store.update("memory", "remove", "", "lesson two");
  await store.update("user", "add", "a".repeat(4001));
  await assert.rejects(store.update("memory", "add", "api_key=sk-abcdefghijklmnopqrstuv"), /Credentials/);
  assert.equal(await store.read("memory"), "updated one");
  await store.clear();
  assert.equal(await store.read("memory"), "");
});
test("simultaneous writers retain every entry", async () => {
  const store = new PersistentMemoryStore(join(directory, "concurrent"));
  await Promise.all(Array.from({ length: 10 }, (_, i) => store.update("memory", "add", `fact ${i}`)));
  for (let i = 0; i < 10; i++) assert.ok((await store.read("memory")).includes(`fact ${i}`));
});

test("memory settings import refreshes and delete requires confirmation", async () => {
  const home = join(directory, "settings-home");
  const previousHome = process.env.USERPROFILE;
  process.env.USERPROFILE = home;
  try {
    const { createJiti } = await import("jiti");
    const jiti = createJiti(import.meta.url);
    const { default: activate } = await jiti.import("../../extensions/pi-long-memory/andex.ts");
    const actions = new Map();
    const storage = new Map();
    let settings;
    let confirmation = false;
    let imported = "imported note";
    const notices = [];
    await activate({
      storage: { set: (key, value) => storage.set(key, value) },
      registerSettings: value => { settings = value; },
      registerAction: (id, handler) => { actions.set(id, handler); return () => actions.delete(id); },
      host: { invoke: async (method, args) => {
        if (method === "app.importMarkdown") return { text: imported };
        if (method === "pi_extension_confirm") return { value: confirmation };
        if (method === "app.notify") notices.push(args.message);
        return {};
      } },
    });
    assert.equal(settings.title, "长效记忆");
    assert.equal(settings.sections[0].settings[0].default, true);
    const store = new PersistentMemoryStore(join(home, ".andex", "memory"));
    await store.update("user", "add", "saved preference");
    await actions.get("settings:long-memory:enabled")({ value: false });
    assert.equal(await store.enabled(), false);
    assert.equal(await store.read("user"), "saved preference");
    await actions.get("import-memory")({});
    assert.ok(settings.sections[1].settings.some(s => s.text === imported));
    const importedCard = settings.sections[1].settings.find(s => s.text === imported);
    await actions.get(importedCard.deleteAction)({});
    assert.equal(await store.read("memory"), imported);
    confirmation = true;
    await actions.get(importedCard.deleteAction)({});
    assert.equal(await store.read("memory"), "");
    imported = Array.from({ length: 150 }, (_, i) => `entry ${i}`).join("\n\n---\n\n");
    await actions.get("import-memory")({});
    assert.equal(settings.sections[1].settings.filter(s => s.deleteAction && s.id.startsWith("memory-")).length, 150);
    confirmation = false;
    imported = " ";
    await actions.get("import-memory")({});
    assert.ok(notices.some(n => n.startsWith("导入失败")));
    await actions.get("request-clear")({});
    assert.equal(await store.read("user"), "saved preference");
    confirmation = true;
    await actions.get("request-clear")({});
    assert.equal(await store.read("user"), "");
  } finally {
    if (previousHome === undefined) delete process.env.USERPROFILE;
    else process.env.USERPROFILE = previousHome;
  }
});


test("Markdown round trip merges deduplicates and retains compaction summaries", async () => {
  const store = new PersistentMemoryStore(join(directory, "markdown"));
  await store.update("user", "add", "Use Chinese.");
  await store.update("user", "add", "Use  Chinese!");
  assert.equal(await store.read("user"), "Use Chinese.");
  await store.recordCompaction("chat", "First summary");
  await store.recordCompaction("chat", "Updated summary");
  await store.recordCompaction("other", "Updated summary!");
  assert.equal((await store.compactions()).length, 1);
  const markdown = await store.exportMarkdown();
  const restored = new PersistentMemoryStore(join(directory, "markdown-restored"));
  await restored.importMarkdown(markdown);
  await restored.importMarkdown(markdown);
  assert.equal(await restored.read("user"), "Use Chinese.");
  assert.equal((await restored.compactions()).length, 1);
  assert.ok((await restored.snapshot()).includes("Updated summary"));
  await assert.rejects(restored.importMarkdown("api_key=sk-abcdefghijklmnopqrstuv"), /Credentials/);
  assert.equal(await restored.read("user"), "Use Chinese.");
});

test("memory reloads every turn and successful compaction persists its actual summary", async () => {
  const previousHome = process.env.USERPROFILE;
  const home = join(directory, "turn-home");
  process.env.USERPROFILE = home;
  try {
    const { createJiti } = await import("jiti");
    const { persistentMemoryExtension } = await createJiti(import.meta.url).import("../src/persistent-memory.ts");
    const events = new Map();
    persistentMemoryExtension({ on: (name, handler) => events.set(name, handler), registerTool() {} });
    const store = new PersistentMemoryStore(join(home, ".andex", "memory"));
    const first = await events.get("before_agent_start")({ systemPrompt: "base" });
    await store.update("user", "add", "newly saved preference");
    const next = await events.get("before_agent_start")({ systemPrompt: "base" });
    assert.equal(first.systemPrompt.includes("newly saved preference"), false);
    assert.equal(next.systemPrompt.includes("newly saved preference"), true);
    await events.get("session_compact")({ compactionEntry: { summary: "compressed conversation facts" } },
      { sessionManager: { getSessionId: () => "chat" }, ui: { notify: assert.fail } });
    assert.ok((await store.snapshot()).includes("compressed conversation facts"));
    await store.setEnabled(false);
    assert.equal(await events.get("before_agent_start")({ systemPrompt: "base" }), undefined);
  } finally {
    if (previousHome === undefined) delete process.env.USERPROFILE; else process.env.USERPROFILE = previousHome;
  }
});


test("imports above 2 MB persist in full and snapshot stays bounded", async () => {
  const store = new PersistentMemoryStore(join(directory, "large-import"));
  const large = "大文件记忆内容".repeat(300_000);
  await store.importMarkdown(large);
  assert.equal(await store.read("memory"), large);
  assert.ok((await store.snapshot()).length < 24_000);
  assert.ok((await store.exportMarkdown()).includes(large));
  await store.importMarkdown("\ufeff## USER.md\r\n\r\n中文偏好\r\n\r\n## MEMORY.md\r\n\r\n新的事实");
  assert.equal(await store.read("user"), "中文偏好");
  assert.ok((await store.read("memory")).includes("新的事实"));
  await store.editEntry("memory", large, "edited");
  assert.equal(await store.read("memory"), "edited\n\n---\n\n新的事实");
  await assert.rejects(store.editEntry("memory", large, "stale"), /已被更新/);
  await store.editEntry("memory", "edited", "");
  assert.equal(await store.read("memory"), "新的事实");
});
