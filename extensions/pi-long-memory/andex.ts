import type { AndexExtensionAPI } from "../../packages/extension-api/src/index.js";
import { createHash } from "node:crypto";
import { PersistentMemoryStore, memoryDelimiter } from "./store.ts";

export default async function activate(andex: AndexExtensionAPI) {
  const store = new PersistentMemoryStore();
  let disposeEntryActions: (() => void)[] = [];
  const notify = (message: string) => andex.host.invoke("app.notify", { message });
  const confirmDelete = async (title: string) => (await andex.host.invoke("pi_extension_confirm", {
    title, message: "删除后无法恢复，确定继续吗？",
  })).value === true;
  andex.storage.set("settings:long-memory:enabled", await store.enabled());
  async function refresh() {
    const [user, memory, summaries] = await Promise.all([store.read("user"), store.read("memory"), store.compactions()]);
    disposeEntryActions.forEach(dispose => dispose());
    disposeEntryActions = [];
    const cards = (["user", "memory"] as const).flatMap(target => {
      const entries = (target === "user" ? user : memory).split(memoryDelimiter).filter(s => s.trim());
      const label = target === "user" ? "用户资料" : "长期记忆";
      if (!entries.length) return [{ id: `${target}-empty`, type: "result-card" as const, title: label,
        expanded: false, editAction: `edit-${target}`, text: "" }];
      return entries.map((text, index) => {
        const id = `${target}-${createHash("sha256").update(text).digest("hex").slice(0, 24)}`;
        disposeEntryActions.push(andex.registerAction(`edit-${id}`, async payload => {
          if (typeof payload.content !== "string" || payload.previous !== text) throw new Error("请刷新记忆后重试。");
          if (!payload.content.trim() && !await confirmDelete(`删除这条${label}？`)) return { cancelled: true };
          await store.editEntry(target, text, payload.content);
          await refresh(); await notify("记忆已保存，下次对话自动载入。");
          return { saved: true };
        }));
        disposeEntryActions.push(andex.registerAction(`delete-${id}`, async () => {
          if (!await confirmDelete(`删除这条${label}？`)) return { cancelled: true };
          await store.editEntry(target, text, "");
          await refresh(); await notify("记忆已删除。"); return { deleted: true };
        }));
        return { id, type: "result-card" as const, title: `${label} · ${index + 1}`, expanded: false,
          editAction: `edit-${id}`, deleteAction: `delete-${id}`, text };
      });
    });
    const summaryCards = summaries.map((entry, index) => {
      const id = `summary-${createHash("sha256").update(entry.session).digest("hex").slice(0, 24)}`;
      disposeEntryActions.push(andex.registerAction(`delete-${id}`, async () => {
        if (!await confirmDelete("删除这条对话摘要？")) return { cancelled: true };
        await store.deleteCompaction(entry.session, entry.summary);
        await refresh(); await notify("摘要已删除。"); return { deleted: true };
      }));
      return { id, type: "result-card" as const, title: `对话摘要 · ${index + 1}`, expanded: false,
        deleteAction: `delete-${id}`, text: entry.summary };
    });
    andex.registerSettings({
      id: "long-memory", title: "长效记忆", subtitle: "跨会话保存用户资料和经验笔记", icon: "brain", order: 10,
      trailingIcon: "trash", trailingAction: "request-clear",
      sections: [{
        title: "持久记忆", description: "每次对话载入最新记忆；上下文达到 80% 后自动压缩并保存摘要。导入文件完整保存，重复内容自动去重。",
        settings: [{ id: "enabled", label: "启用长效记忆", type: "toggle", default: true,
          description: "关闭后停止读写，已有记忆保留。" }],
      }, {
        title: "已保存的记忆",
        settings: [{ id: "memory-actions", type: "action-row", actions: [
          { label: "刷新", action: "refresh-memory" }, { label: "导入", action: "import-memory" }, { label: "导出", action: "export-memory" },
        ] }, ...cards, ...summaryCards],
      }],
    });
  }
  await refresh();
  for (const target of ["user", "memory"] as const) {
    andex.registerAction(`edit-${target}`, async payload => {
      if (typeof payload.content !== "string" || typeof payload.previous !== "string") throw new Error("缺少记忆内容，请刷新后重试。");
      await store.editDocument(target, payload.content, payload.previous);
      await refresh(); await notify("记忆已保存，下次对话自动载入。"); return { saved: true };
    });
  }
  andex.registerAction("refresh-memory", async () => { await refresh(); return { refreshed: true }; });
  andex.registerAction("import-memory", async () => {
    try {
      const result = await andex.host.invoke("app.importMarkdown");
      if (result.cancelled) return result;
      if (typeof result.text !== "string") throw new Error("无法读取 Markdown 文件");
      await store.importMarkdown(result.text);
      await refresh(); await notify("记忆导入成功，已刷新列表。"); return { imported: true };
    } catch (error) {
      await notify(`导入失败：${error instanceof Error ? error.message : String(error)}`);
      return { imported: false };
    }
  });
  andex.registerAction("export-memory", async () => andex.host.invoke("app.exportMarkdown", {
    name: "Andex-memory.md", text: await store.exportMarkdown(),
  }));
  andex.registerAction("settings:long-memory:enabled", async payload => {
    const enabled = (payload.value ?? payload.checked) === true;
    await store.setEnabled(enabled); andex.storage.set("settings:long-memory:enabled", enabled); return { enabled };
  });
  andex.registerAction("request-clear", async () => {
    if (!await confirmDelete("清空全部长效记忆？")) return { cancelled: true };
    await store.clear(); await refresh(); await notify("长效记忆已清空。"); return { cleared: true };
  });
}
