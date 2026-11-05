import type { AndexExtensionAPI } from "../../packages/extension-api/src/index.js";
import { PersistentMemoryStore } from "./store.ts";

export default async function activate(andex: AndexExtensionAPI) {
  const store = new PersistentMemoryStore();
  let clearRequestedAt = 0;
  andex.storage.set("settings:long-memory:enabled", await store.enabled());
  andex.registerSettings({
    id: "long-memory", title: "长效记忆", subtitle: "跨会话保存用户资料和经验笔记",
    icon: "brain", order: 10,
    trailingIcon: "trash", trailingAction: "request-clear",
    sections: [{
      title: "持久记忆",
      description: "参考 Hermes 的 MEMORY.md / USER.md 分层设计。默认启用，新会话载入；压缩前提取重要事实。记忆保存在应用内，清除应用数据或卸载后会删除。",
      settings: [{ id: "enabled", label: "启用长效记忆", type: "toggle", default: true,
        description: "Agent 主动保存长期偏好、项目决策和经验。关闭后停止读写，已有记忆保留。" }],
    }],
  });
  andex.registerAction("settings:long-memory:enabled", async payload => {
    const enabled = (payload.value ?? payload.checked) === true;
    await store.setEnabled(enabled);
    andex.storage.set("settings:long-memory:enabled", enabled);
    return { enabled };
  });
  andex.registerAction("request-clear", async () => {
    if (clearRequestedAt && Date.now() - clearRequestedAt < 10_000) {
      await store.clear();
      clearRequestedAt = 0;
      await andex.host.invoke("app.notify", { message: "长效记忆已清空。新会话将不再载入旧记忆。" });
      return { cleared: true };
    }
    clearRequestedAt = Date.now();
    await andex.host.invoke("app.notify", { message: "10 秒内再次点击右上角清空按钮将删除全部长效记忆。" });
    return { confirmationRequired: true };
  });
}
