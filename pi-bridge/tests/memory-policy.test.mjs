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
  await assert.rejects(store.update("user", "add", "a".repeat(4001)), /limit/);
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

test("memory settings are registered, disable preserves notes and double tap clears", async () => {
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
    await activate({
      storage: { set: (key, value) => storage.set(key, value) },
      registerSettings: value => { settings = value; },
      registerAction: (id, handler) => actions.set(id, handler),
      host: { invoke: async () => ({}) },
    });
    assert.equal(settings.title, "长效记忆");
    assert.equal(settings.sections[0].settings[0].default, true);
    const store = new PersistentMemoryStore(join(home, ".andex", "memory"));
    await store.update("user", "add", "saved preference");
    await actions.get("settings:long-memory:enabled")({ value: false });
    assert.equal(await store.enabled(), false);
    assert.equal(await store.read("user"), "saved preference");
    await actions.get("request-clear")({});
    assert.equal(await store.read("user"), "saved preference");
    await actions.get("request-clear")({});
    assert.equal(await store.read("user"), "");
  } finally {
    if (previousHome === undefined) delete process.env.USERPROFILE;
    else process.env.USERPROFILE = previousHome;
  }
});
