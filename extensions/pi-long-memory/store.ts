import * as fs from "node:fs/promises";
import * as path from "node:path";
import * as os from "node:os";
import { randomUUID } from "node:crypto";

const limits = { memory: 6000, user: 4000 };
type Target = keyof typeof limits;
export const memoryDirectory = () => path.join(os.homedir(), ".andex", "memory");
export const memoryDelimiter = "\n\n---\n\n";
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
  async clear() {
    await this.locked(async () => {
      for (const target of ["memory", "user"] as const) await this.atomic(this.file(target), "");
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
        if (/\b(?:sk-[A-Za-z0-9_-]{12,}|gh[pousr]_[A-Za-z0-9]{15,}|(?:api[_ -]?key|password|token|密码)\s*[:=]\s*\S{8,})/i.test(text)) {
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
      entries = [...new Set(entries)];
      const next = entries.join(memoryDelimiter);
      if (next.length > limits[target]) throw new Error(`Memory limit is ${limits[target]} characters. Replace or remove obsolete entries first.`);
      await this.atomic(this.file(target), next);
      return next;
    });
  }
  async snapshot() {
    const [user, memory] = await Promise.all([this.read("user"), this.read("memory")]);
    return `USER.md (saved reference data):\n${user || "(empty)"}\n\nMEMORY.md (saved reference data):\n${memory || "(empty)"}`;
  }
}
