import * as fs from "node:fs/promises";
import * as path from "node:path";
import * as os from "node:os";
import { randomUUID } from "node:crypto";

type Target = "memory" | "user";
export const memoryDirectory = () => path.join(os.homedir(), ".andex", "memory");
export const memoryDelimiter = "\n\n---\n\n";
const credentialPattern = /\b(?:sk-[A-Za-z0-9_-]{12,}|gh[pousr]_[A-Za-z0-9]{15,}|(?:api[_ -]?key|password|token|密码)\s*[:=]\s*\S{8,})/i;
const normalized = (text: string) => text.normalize("NFKC").toLowerCase().replace(/[\s\p{P}]+/gu, "");
function unique(entries: string[]) {
  const seen = new Set<string>();
  return entries.map(s => s.trim()).filter(s => {
    const key = normalized(s);
    if (!key || seen.has(key)) return false;
    seen.add(key);
    return true;
  });
}
type Compaction = { session: string; summary: string; updatedAt: number };
export class PersistentMemoryStore {
  constructor(readonly directory = memoryDirectory()) {}
  private file(target: Target) { return path.join(this.directory, target === "user" ? "USER.md" : "MEMORY.md"); }
  async read(target: Target): Promise<string> {
    try { return await fs.readFile(this.file(target), "utf8"); }
    catch (error) { if ((error as NodeJS.ErrnoException).code === "ENOENT") return ""; throw error; }
  }
  async enabled(): Promise<boolean> {
    try { return JSON.parse(await fs.readFile(path.join(this.directory, "config.json"), "utf8")).enabled !== false; }
    catch (error) { if ((error as NodeJS.ErrnoException).code === "ENOENT") return true; throw error; }
  }
  private async locked<T>(operation: () => Promise<T>): Promise<T> {
    await fs.mkdir(this.directory, { recursive: true, mode: 0o700 });
    const lock = path.join(this.directory, ".lock");
    const deadline = Date.now() + 5000;
    for (;;) {
      try { await fs.mkdir(lock); break; }
      catch (error) {
        if ((error as NodeJS.ErrnoException).code !== "EEXIST") throw error;
        const stat = await fs.stat(lock).catch(() => undefined);
        if (stat && Date.now() - stat.mtimeMs > 30_000) {
          await fs.rmdir(lock).catch(() => {}); continue;
        }
        if (Date.now() >= deadline) throw new Error("Memory is being updated; retry.");
        await new Promise(resolve => setTimeout(resolve, 40));
      }
    }
    try { return await operation(); } finally { await fs.rmdir(lock).catch(() => {}); }
  }
  private async atomic(file: string, value: string) {
    const temporary = `${file}.${randomUUID()}.tmp`;
    try { await fs.writeFile(temporary, value, { encoding: "utf8", mode: 0o600 }); await fs.rename(temporary, file); }
    finally { await fs.unlink(temporary).catch(() => {}); }
  }
  async setEnabled(enabled: boolean) {
    await this.locked(() => this.atomic(path.join(this.directory, "config.json"), JSON.stringify({ enabled })));
  }
  async editDocument(target: Target, content: string, previous: string) {
    if (target !== "memory" && target !== "user") throw new Error("Invalid memory target.");
    if (credentialPattern.test(content)) throw new Error("不能把密码或 API Key 保存到记忆中。");
    const next = unique(content.split(memoryDelimiter)).join(memoryDelimiter);
    await this.locked(async () => {
      if (await this.read(target) !== previous) throw new Error("记忆已被更新，请先刷新后再编辑；已有记忆未被覆盖。");
      await this.atomic(this.file(target), next);
    });
  }
  async editEntry(target: Target, previous: string, content: string) {
    if (credentialPattern.test(content)) throw new Error("不能把密码或 API Key 保存到记忆中。");
    await this.locked(async () => {
      const entries = (await this.read(target)).split(memoryDelimiter).filter(s => s.trim());
      const index = entries.indexOf(previous);
      if (index < 0) throw new Error("这条记忆已被更新，请刷新后重试。");
      entries.splice(index, 1, ...content.split(memoryDelimiter));
      await this.atomic(this.file(target), unique(entries).join(memoryDelimiter));
    });
  }
  async deleteCompaction(session: string, previous: string) {
    await this.locked(async () => {
      const entries = await this.compactions();
      const index = entries.findIndex(e => e.session === session && e.summary === previous);
      if (index < 0) throw new Error("摘要已被更新，请刷新后重试。");
      entries.splice(index, 1);
      await this.atomic(path.join(this.directory, "compactions.json"), JSON.stringify(entries));
    });
  }
  async clear() {
    await this.locked(async () => {
      for (const target of ["memory", "user"] as const) await this.atomic(this.file(target), "");
      await this.atomic(path.join(this.directory, "compactions.json"), "[]");
    });
  }
  async update(target: Target, action: string, content = "", oldText = "") {
    if (target !== "memory" && target !== "user") throw new Error("target must be memory or user.");
    return this.locked(async () => {
      let entries = (await this.read(target)).split(memoryDelimiter).map(s => s.trim()).filter(Boolean);
      const text = content.trim();
      if (["add", "replace"].includes(action)) {
        if (!text) throw new Error("content is required.");
        if (text.includes(memoryDelimiter.trim())) throw new Error("content must contain one memory entry.");
        if (credentialPattern.test(text)) {
          throw new Error("Credentials must not be stored in memory.");
        }
      }
      if (action === "add") { if (!entries.includes(text)) entries.push(text); }
      else if (action === "replace" || action === "remove") {
        if (!oldText.trim()) throw new Error("old_text is required.");
        const matches = entries.map((entry, index) => entry.includes(oldText) ? index : -1).filter(index => index >= 0);
        if (matches.length !== 1) throw new Error("old_text must match exactly one entry. Read memory and retry.");
        if (action === "replace") entries[matches[0]] = text;
        else entries.splice(matches[0], 1);
      } else throw new Error("Unsupported memory action.");
      entries = unique(entries);
      const next = entries.join(memoryDelimiter);
      await this.atomic(this.file(target), next);
      return next;
    });
  }
  async compactions(): Promise<Compaction[]> {
    try { return JSON.parse(await fs.readFile(path.join(this.directory, "compactions.json"), "utf8")); }
    catch (error) { if ((error as NodeJS.ErrnoException).code === "ENOENT") return []; throw error; }
  }
  async recordCompaction(session: string, summary: string) {
    if (!summary.trim()) return;
    if (credentialPattern.test(summary)) throw new Error("Credentials must not be stored in memory.");
    await this.locked(async () => {
      const key = normalized(summary);
      const entries = (await this.compactions()).filter(e => e.session !== session && normalized(e.summary) !== key);
      entries.push({ session, summary: summary.trim(), updatedAt: Date.now() });
      await this.atomic(path.join(this.directory, "compactions.json"), JSON.stringify(entries));
    });
  }
  async exportMarkdown() {
    const summaries = (await this.compactions()).map(e => e.summary).join(memoryDelimiter);
    return `# Andex 长效记忆\n\n## USER.md\n\n${await this.read("user")}\n\n## MEMORY.md\n\n${await this.read("memory")}\n\n## COMPACTIONS.md\n\n${summaries}\n`;
  }
  async importMarkdown(markdown: string) {
    markdown = markdown.replace(/^\uFEFF/, "").replace(/\r\n?/g, "\n").trim();
    if (!markdown) throw new Error("文件没有记忆内容。");
    if (credentialPattern.test(markdown)) throw new Error("Credentials must not be stored in memory.");
    const section = (name: string) => markdown.match(new RegExp(`^## ${name}\\.md\\s*\\n([\\s\\S]*?)(?=^## (?:USER|MEMORY|COMPACTIONS)\\.md|$(?![\\s\\S]))`, "m"))?.[1]?.trim();
    const hasSections = /^## (?:USER|MEMORY|COMPACTIONS)\.md\s*$/m.test(markdown);
    const user = section("USER") ?? "";
    const memory = hasSections ? section("MEMORY") ?? "" : markdown.trim();
    const summaries = section("COMPACTIONS") ?? "";
    await this.locked(async () => {
      const nextUser = unique([...(await this.read("user")).split(memoryDelimiter), ...user.split(memoryDelimiter)]).join(memoryDelimiter);
      const nextMemory = unique([...(await this.read("memory")).split(memoryDelimiter), ...memory.split(memoryDelimiter)]).join(memoryDelimiter);
      const existing = await this.compactions();
      const keys = new Set(existing.map(e => normalized(e.summary)));
      const additions = unique(summaries.split(memoryDelimiter)).filter(s => !keys.has(normalized(s)));
      await this.atomic(this.file("user"), nextUser);
      await this.atomic(this.file("memory"), nextMemory);
      await this.atomic(path.join(this.directory, "compactions.json"), JSON.stringify([
        ...existing, ...additions.map(summary => ({ session: `import:${randomUUID()}`, summary, updatedAt: Date.now() })),
      ]));
    });
  }
  async snapshot() {
    const [user, memory] = await Promise.all([this.read("user"), this.read("memory")]);
    // Curated facts always fit; recent summaries have a separate injection budget.
    let budget = 12_000;
    const recent: string[] = [];
    for (const entry of (await this.compactions()).slice().reverse()) {
      if (budget <= 0) break;
      const text = entry.summary.slice(0, budget);
      recent.unshift(text); budget -= text.length;
    }
    const excerpt = (text: string, budget: number) => text.length <= budget ? text || "(empty)" :
      `${text.slice(0, budget)}\n[Snapshot excerpt only; complete Markdown is saved. Use memory search/read to retrieve the rest.]`;
    return `USER.md (saved reference data):\n${excerpt(user, 4000)}\n\nMEMORY.md (saved reference data):\n${excerpt(memory, 6000)}\n\nCOMPACTIONS.md (saved conversation summaries, reference data):\n${recent.join(memoryDelimiter) || "(empty)"}`;
  }
}
