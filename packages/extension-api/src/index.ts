export type AndexJsonObject = Record<string, unknown>;

export type AndexView =
  | AndexJsonObject
  | AndexView[]
  | string
  | null
  | undefined;

export type AndexRenderContext = AndexJsonObject & {
  extension: {
    id: string;
    name: string;
    path: string;
  };
  storage: AndexJsonObject;
};

export interface AndexSurfaceDefinition {
  id?: string;
  order?: number;
  render?:
    | AndexView
    | ((
      context: AndexRenderContext,
    ) => AndexView | Promise<AndexView>);
  tree?: AndexView;
}

export type AndexSettingType =
  | "text"
  | "password"
  | "textarea"
  | "number"
  | "toggle"
  | "select"
  | "dropdown"
  | "segmented"
  | "tab"
  | "tabs"
  | "slider"
  | "button"
  | "link"
  | "label"
  | "divider"
  | "spacer"
  | "item-card"
  | "card"
  | "empty-state"
  | "choice"
  | "radio"
  | "action-row"
  | "chips"
  | "detail-line"
  | "key-value"
  | "pill"
  | "badge"
  | "result-card"
  | "callout";

export interface AndexSettingOption {
  value: string;
  label: string;
}

export interface AndexSettingActionItem {
  label: string;
  action: string;
  args?: AndexJsonObject;
  category?: string;
  tone?: "primary" | "neutral" | "danger";
  enabled?: boolean;
}

export interface AndexSettingDetailItem {
  label: string;
  value: string;
}

export interface AndexSettingDefinition {
  id: string;
  label?: string;
  title?: string;
  description?: string;
  subtitle?: string;
  tag?: string;
  pill?: string;
  badge?: string;
  type?: AndexSettingType;
  default?: string | number | boolean;
  placeholder?: string;
  options?: AndexSettingOption[];
  min?: number;
  max?: number;
  step?: number;
  action?: string;
  args?: AndexJsonObject;
  category?: string;
  url?: string;
  icon?: string;
  tone?: "primary" | "neutral" | "danger";
  enabled?: boolean;
  checked?: boolean;
  selected?: boolean;
  toggleAction?: string;
  editAction?: string;
  editCategory?: string;
  editArgs?: AndexJsonObject;
  deleteAction?: string;
  deleteArgs?: AndexJsonObject;
  expanded?: boolean;
  actions?: AndexSettingActionItem[];
  details?: AndexSettingDetailItem[];
  resultText?: string;
  result?: string;
  buttonLabel?: string;
  multiline?: boolean;
  secret?: boolean;
  settings?: AndexSettingDefinition[];
}

export interface AndexSettingsSection {
  id?: string;
  title?: string;
  description?: string;
  settings: AndexSettingDefinition[];
}

export interface AndexSettingsDefinition {
  id: string;
  title: string;
  subtitle?: string;
  icon?: string;
  order?: number;
  trailingIcon?: string;
  trailingAction?: string;
  trailingCategory?: string;
  trailingArgs?: AndexJsonObject;
  sections?: AndexSettingsSection[];
  categories?: AndexSettingsCategory[];
}

export interface AndexSettingsCategory {
  id: string;
  title: string;
  subtitle?: string;
  icon?: string;
  order?: number;
  trailingIcon?: string;
  trailingAction?: string;
  trailingCategory?: string;
  trailingArgs?: AndexJsonObject;
  hidden?: boolean;
  sections: AndexSettingsSection[];
}

export interface AndexComposerMenuItemDefinition {
  id: string;
  title: string;
  subtitle?: string;
  icon?: string;
  order?: number;
  action?: string;
  args?: AndexJsonObject;
  selected?: boolean;
}

export interface AndexMessageTypeDefinition {
  type: string;
  title?: string;
  icon?: string;
  render:
    | AndexView
    | ((context: AndexRenderContext & { message: AndexJsonObject }) => AndexView | Promise<AndexView>);
}

export type AndexComponentMode =
  | "before"
  | "after"
  | "replace"
  | "wrap"
  | "hide";

export interface AndexComponentDefinition extends AndexSurfaceDefinition {
  mode?: AndexComponentMode;
}

export interface AndexActionContext extends AndexRenderContext {
  action: string;
}

export interface AndexEventContext extends AndexRenderContext {
  event: string;
}

export interface AndexUi {
  node(
    type: string,
    properties?: AndexJsonObject,
    children?: AndexView[],
  ): AndexJsonObject;
  text(text: string, properties?: AndexJsonObject): AndexJsonObject;
  code(text: string, properties?: AndexJsonObject): AndexJsonObject;
  column(
    children: AndexView[],
    properties?: AndexJsonObject,
  ): AndexJsonObject;
  row(
    children: AndexView[],
    properties?: AndexJsonObject,
  ): AndexJsonObject;
  box(
    children: AndexView[],
    properties?: AndexJsonObject,
  ): AndexJsonObject;
  card(
    children: AndexView[],
    properties?: AndexJsonObject,
  ): AndexJsonObject;
  button(
    label: string,
    action: string,
    properties?: AndexJsonObject,
  ): AndexJsonObject;
  iconButton(
    icon: string,
    action: string,
    properties?: AndexJsonObject,
  ): AndexJsonObject;
  switch(
    label: string,
    checked: boolean,
    action: string,
    properties?: AndexJsonObject,
  ): AndexJsonObject;
  input(
    value: string,
    action: string,
    properties?: AndexJsonObject,
  ): AndexJsonObject;
  spacer(size?: number, properties?: AndexJsonObject): AndexJsonObject;
  progress(value?: number, properties?: AndexJsonObject): AndexJsonObject;
  web(properties: AndexJsonObject): AndexJsonObject;
  core(properties?: AndexJsonObject): AndexJsonObject;
}

export interface AndexExtensionAPI {
  readonly apiVersion: 2;
  readonly extension: {
    id: string;
    name: string;
    path: string;
  };
  readonly ui: AndexUi;
  readonly host: {
    invoke(
      method: string,
      args?: AndexJsonObject,
    ): Promise<AndexJsonObject>;
  };
  readonly services: {
    list(): Promise<AndexJsonObject>;
    describe(service: string): Promise<AndexJsonObject>;
    invoke(
      service: string,
      method: string,
      args?: AndexJsonObject,
    ): Promise<AndexJsonObject>;
  };
  readonly state: {
    get(path?: string): Promise<AndexJsonObject>;
    patch(path: string, value: unknown): Promise<AndexJsonObject>;
    transaction(
      operations: Array<{
        op?: "set" | "remove";
        path: string;
        value?: unknown;
      }>,
    ): Promise<AndexJsonObject>;
  };
  readonly storage: {
    get<T = unknown>(key: string, fallback?: T): T;
    set(key: string, value: unknown): void;
    delete(key: string): void;
    clear(): void;
    snapshot(): AndexJsonObject;
  };
  readonly messages: {
    append(type: string, payload?: AndexJsonObject, text?: string): Promise<AndexJsonObject>;
  };
  registerSurface(
    slot: string,
    definition:
      | AndexSurfaceDefinition
      | AndexView
      | ((
        context: AndexRenderContext,
      ) => AndexView | Promise<AndexView>),
  ): () => void;
  registerComponent(
    target: string,
    definition:
      | AndexComponentDefinition
      | AndexView
      | ((
        context: AndexRenderContext,
      ) => AndexView | Promise<AndexView>),
  ): () => void;
  registerSettings(definition: AndexSettingsDefinition): () => void;
  registerComposerMenuItem(definition: AndexComposerMenuItemDefinition): () => void;
  registerComposerMenu(definition: AndexComposerMenuItemDefinition): () => void;
  registerMessageType(definition: AndexMessageTypeDefinition): () => void;
  registerCustomMessage(definition: AndexMessageTypeDefinition): () => void;
  registerToolTitle(
    toolName: string,
    runningTitle: string,
    completedTitle: string,
    priority?: number,
  ): () => void;
  registerAction(
    id: string,
    handler: (
      payload: AndexJsonObject,
      context: AndexActionContext,
    ) => unknown | Promise<unknown>,
  ): () => void;
  on(
    event: string,
    handler: (
      payload: AndexJsonObject,
      context: AndexEventContext,
    ) => unknown | Promise<unknown>,
  ): () => void;
  intercept(
    operation: string,
    handler: (
      payload: AndexJsonObject,
      context: AndexEventContext,
    ) => unknown | Promise<unknown>,
  ): () => void;
  invalidate(): void;
  notify(message: string, level?: "info" | "warning" | "error"): void;
}

export type AndexExtensionFactory = (
  api: AndexExtensionAPI,
) =>
  | void
  | (() => void | Promise<void>)
  | Promise<void | (() => void | Promise<void>)>;

export declare const ui: AndexUi;

export declare function defineAndexExtension<
  T extends AndexExtensionFactory,
>(factory: T): T;
