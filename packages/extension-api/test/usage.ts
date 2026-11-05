import {
  defineAndexExtension,
  ui,
  type AndexExtensionAPI,
} from "@tangnanbai/andex-extension-api";

const factory = defineAndexExtension((andex) => {
  andex.registerSurface("chat.composer.top", {
    render: ({ storage }) =>
      ui.card([
        ui.text(String(storage.count ?? 0)),
        ui.button("Increment", "increment"),
      ]),
  });
  andex.registerAction("increment", () => {
    const count = andex.storage.get("count", 0) + 1;
    andex.storage.set("count", count);
  });
  andex.registerSettings({
    id: "preferences",
    title: "Preferences",
    categories: [{ id: "general", title: "General", sections: [{ settings: [{ id: "enabled", label: "Enabled", type: "toggle", default: true }] }] }],
  });
  andex.registerComposerMenuItem({ id: "run", title: "Run", action: "run" });
  andex.registerMessageType({ type: "demo", render: ({ message }) => ui.text(String(message.text ?? "")) });
  andex.registerToolTitle("demo_search", "Searching demos", "Searched demos", 200);
  andex.registerAction("message", () => andex.messages.append("demo", { text: "hello" }));
});

const acceptsApi = (_api: AndexExtensionAPI) => factory;
void acceptsApi;
