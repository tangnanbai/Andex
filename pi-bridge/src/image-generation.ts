export interface ImageModelConfig {
  model_id: string;
  pi_api: string;
  pi_provider_id: string;
  base_url: string;
  api_key?: string;
  custom_headers?: Record<string, string>;
}
export interface GeneratedImage { bytes: Buffer; mimeType: string; extension: string }
export interface ImageInput { data: string; mimeType: string }
type ObjectValue = Record<string, any>;
const MAX_IMAGE_BYTES = 8 * 1024 * 1024;

export function isImageGenerationModel(id: string): boolean {
  return /gpt-image|dall-e|nano-banana|nanobanana|gemini.*image|^imagen-/i.test(id);
}

export function imageType(bytes: Buffer): Pick<GeneratedImage, "mimeType" | "extension"> {
  if (bytes.subarray(0, 8).equals(Buffer.from([137,80,78,71,13,10,26,10]))) return { mimeType: "image/png", extension: "png" };
  if (bytes[0] === 255 && bytes[1] === 216 && bytes[2] === 255) return { mimeType: "image/jpeg", extension: "jpg" };
  if (bytes.subarray(0, 4).toString() === "RIFF" && bytes.subarray(8, 12).toString() === "WEBP") return { mimeType: "image/webp", extension: "webp" };
  throw new Error("生图接口没有返回有效的 PNG、JPEG 或 WebP 图片。");
}

async function readBounded(response: Response, limit: number, signal: AbortSignal): Promise<Buffer> {
  if (!response.body) throw new Error("生图接口返回空响应。");
  const reader = response.body.getReader();
  const chunks: Uint8Array[] = [];
  let size = 0;
  try {
    while (true) {
      signal.throwIfAborted();
      const { done, value } = await reader.read();
      if (done) break;
      size += value.length;
      if (size > limit) throw new Error("图片响应过大，请选择较低分辨率后重试。");
      chunks.push(value);
    }
  } finally { await reader.cancel().catch(() => {}); }
  return Buffer.concat(chunks);
}

/** One generation request per user action. Never retry a possibly billed request automatically. */
export async function generateImages(config: ImageModelConfig, prompt: string, inputs: ImageInput[], signal: AbortSignal,
  fetcher: typeof fetch = fetch): Promise<{ images: GeneratedImage[]; text: string; usage?: ObjectValue }> {
  const router = config.pi_provider_id === "openrouter";
  // Gemini image models use generateContent even on gateways configured for OpenAI chat.
  // Verified against the gateway's supported_endpoint_types and a real Nano Banana request.
  const google = config.pi_api.startsWith("google-") || (!router && /gemini.*(?:banana|image)/i.test(config.model_id));
  let base = config.base_url.replace(/\/+$/, "");
  if (!base) base = google ? "https://generativelanguage.googleapis.com/v1beta" : router ? "https://openrouter.ai/api/v1" : "https://api.openai.com/v1";
  base = base.replace(/\/(?:images\/(?:generations|edits)|chat\/completions|responses)$/, "");
  if (google) base = /\/v1$/.test(base) ? base.replace(/\/v1$/, "/v1beta")
    : /\/v1(?:beta)?$/.test(base) ? base : `${base}/v1beta`;
  const parsedBase = new URL(base);
  if (!["https:", "http:"].includes(parsedBase.protocol)) throw new Error("生图请求地址必须使用 HTTP 或 HTTPS。");
  const headers = new Headers(config.custom_headers ?? {});
  if (config.api_key) headers.set(google ? "x-goog-api-key" : "Authorization", google ? config.api_key : `Bearer ${config.api_key}`);
  let endpoint: string;
  let body: BodyInit;
  if (google) {
    endpoint = `${base}/models/${encodeURIComponent(config.model_id)}:generateContent`;
    body = JSON.stringify({ contents: [{ role: "user", parts: [{ text: prompt }, ...inputs.map(i => ({ inlineData: i }))] }],
      generationConfig: { responseModalities: ["TEXT", "IMAGE"] } });
    headers.set("Content-Type", "application/json");
  } else if (router) {
    endpoint = `${base}/chat/completions`;
    body = JSON.stringify({ model: config.model_id, messages: [{ role: "user", content: [
      { type: "text", text: prompt }, ...inputs.map(i => ({ type: "image_url", image_url: { url: `data:${i.mimeType};base64,${i.data}` } })),
    ] }], modalities: ["image", "text"], stream: false });
    headers.set("Content-Type", "application/json");
  } else if (inputs.length) {
    endpoint = `${base}/images/edits`;
    const form = new FormData();
    form.set("model", config.model_id); form.set("prompt", prompt); form.set("n", "1");
    inputs.forEach((input, index) => {
      const bytes = Buffer.from(input.data, "base64");
      const type = imageType(bytes);
      form.append(inputs.length === 1 ? "image" : "image[]", new Blob([new Uint8Array(bytes)], { type: type.mimeType }), `reference-${index}.${type.extension}`);
    });
    body = form;
    headers.delete("Content-Type");
  } else {
    endpoint = `${base}/images/generations`;
    body = JSON.stringify({ model: config.model_id, prompt, n: 1, size: "1024x1024" });
    headers.set("Content-Type", "application/json");
  }
  const response = await fetcher(endpoint, { method: "POST", headers, body, signal, redirect: "error" });
  if (!response.ok) {
    const raw = (await readBounded(response, 64 * 1024, signal)).toString("utf8");
    let detail = `HTTP ${response.status}`;
    try { const parsed = JSON.parse(raw); detail = String(parsed.error?.message ?? parsed.message ?? detail); } catch { /* Do not render gateway HTML. */ }
    if (config.api_key) detail = detail.split(config.api_key).join("[已隐藏]");
    throw new Error(`生图请求失败（HTTP ${response.status}）：${detail.slice(0, 500)}`);
  }
  const payload = JSON.parse((await readBounded(response, MAX_IMAGE_BYTES * 2, signal)).toString("utf8")) as ObjectValue;
  const assets: { data?: string; url?: string }[] = [];
  const texts: string[] = [];
  for (const item of payload.data ?? []) assets.push({ data: item.b64_json, url: item.url });
  for (const candidate of payload.candidates ?? []) for (const part of candidate.content?.parts ?? []) {
    const data = part.inlineData?.data ?? part.inline_data?.data;
    if (data) assets.push({ data });
    if (part.text) texts.push(part.text);
  }
  for (const choice of payload.choices ?? []) {
    if (typeof choice.message?.content === "string") texts.push(choice.message.content);
    for (const image of choice.message?.images ?? []) assets.push({ url: image.image_url?.url ?? image.url });
  }
  const images: GeneratedImage[] = [];
  for (const asset of assets) {
    signal.throwIfAborted();
    let bytes: Buffer;
    if (asset.data) bytes = Buffer.from(asset.data, "base64");
    else if (asset.url?.startsWith("data:image/")) bytes = Buffer.from(asset.url.substring(asset.url.indexOf(",") + 1), "base64");
    else if (asset.url) {
      const url = new URL(asset.url);
      if (!["https:", "http:"].includes(url.protocol)) throw new Error("图片下载地址不是 HTTP 或 HTTPS。");
      // Generated asset hosts never receive provider credentials or custom headers.
      const download = await fetcher(url, { signal });
      if (!download.ok) throw new Error(`图片下载失败（HTTP ${download.status}）。`);
      bytes = await readBounded(download, MAX_IMAGE_BYTES, signal);
    } else continue;
    if (bytes.length > MAX_IMAGE_BYTES) throw new Error("生成图片超过 8 MB，请降低分辨率。");
    images.push({ bytes, ...imageType(bytes) });
  }
  if (!images.length) throw new Error(texts.join("\n").slice(0, 500) || "接口未返回图片，可能被服务端拒绝或该模型不支持生图。");
  return { images, text: texts.join("\n"), usage: payload.usage ?? payload.usageMetadata };
}
