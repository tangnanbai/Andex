import type { ExtensionAPI } from "@earendil-works/pi-coding-agent";
import {
  defineAndexExtension,
  ui,
} from "@tangnanbai/andex-extension-api";

export default function activatePi(pi: ExtensionAPI) {
  pi.registerCommand("andex-example", {
    description: "Show that the package is also active in Pi",
    handler: async (_args, context) => {
      context.ui.notify("The combined extension is active.", "info");
    },
  });
}

export const activateAndex = defineAndexExtension((andex) => {
  andex.registerAction("increment", async () => {
    const count = andex.storage.get("count", 0) + 1;
    andex.storage.set("count", count);
    await andex.host.invoke("app.notify", {
      message: `Extension count: ${count}`,
    });
  });

  andex.registerAction("prefill", async () => {
    await andex.host.invoke("app.setDraftInput", {
      text: "Summarize the current workspace and suggest the next three tasks.",
    });
  });

  andex.registerSurface("chat.composer.top", {
    id: "quick-tools",
    order: 10,
    render: ({ storage, is_running }) =>
      ui.card([
        ui.row([
          ui.text("Combined extension", {
            style: "label",
            weight: "semibold",
          }),
          ui.text(is_running ? "Agent running" : "Ready", {
            color: "muted",
          }),
        ], {
          arrangement: "space-between",
          verticalAlignment: "center",
        }),
        ui.row([
          ui.button(`Count ${storage.count ?? 0}`, "increment", {
            tone: "neutral",
          }),
          ui.button("Prefill", "prefill"),
        ], {
          wrap: true,
          rowSpacing: 8,
        }),
      ], {
        radius: 22,
      }),
  });

  /* Full-screen page registration is intentionally unsupported. */
  andex.registerSettings({
    id: "dashboard",
    title: "Extension dashboard",
    subtitle: "Native Compose and trusted TypeScript",
    icon: "code",
    categories: [{ id: "dashboard", title: "Dashboard", sections: [{ settings: [] }] }],
  });

  andex.on("before_send", ({ text }) => {
    if (String(text).startsWith("!raw ")) {
      return { text: String(text).slice(5) };
    }
    return undefined;
  });
});
