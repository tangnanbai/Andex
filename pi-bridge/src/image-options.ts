export interface ImageOptions {
  size?: string;
  output_format?: "png" | "jpeg" | "webp";
  background?: "transparent" | "opaque" | "auto";
  quality?: "auto" | "low" | "medium" | "high";
  resolution?: "1K" | "2K" | "4K";
  aspect_ratio?: string;
}

/** Explicit tool arguments override recognizable requirements in the original prompt. */
export function imageOptions(prompt: string, explicit: ImageOptions = {}): ImageOptions {
  const options: ImageOptions = {};
  const dimensions = [...prompt.matchAll(/(?<!\d)(\d{2,5})\s*[x×*]\s*(\d{2,5})(?!\d)/gi)].at(-1);
  if (dimensions) options.size = `${dimensions[1]}x${dimensions[2]}`;
  const ratio = [...prompt.matchAll(/(?<!\d)(\d{1,2})\s*[:：]\s*(\d{1,2})(?!\d)/g)].at(-1);
  if (ratio) options.aspect_ratio = `${ratio[1]}:${ratio[2]}`;
  const resolution = [...prompt.matchAll(/\b([124])\s*k\b/gi)].at(-1);
  if (resolution) options.resolution = `${resolution[1]}K` as ImageOptions["resolution"];
  const format = [...prompt.matchAll(/\b(png|jpe?g|webp)\b/gi)].at(-1)?.[1]?.toLowerCase();
  if (format) options.output_format = format === "jpg" ? "jpeg" : format as ImageOptions["output_format"];
  if (/(透明(?:背景|底)|背景透明|无背景|transparent\s*(?:background)?|alpha\s*channel)/i.test(prompt) &&
      !/(不(?:要|用)透明|非透明|不透明|opaque\s*background|no\s*transparen)/i.test(prompt)) {
    options.background = "transparent";
  }
  for (const [key, value] of Object.entries(explicit)) if (value !== undefined) (options as any)[key] = value;
  if (options.size && !/^[1-9]\d{1,4}x[1-9]\d{1,4}$/.test(options.size))
    throw new Error("图片尺寸请使用宽x高像素，例如 1024x1024。");
  if (options.aspect_ratio && !/^[1-9]\d?:[1-9]\d?$/.test(options.aspect_ratio))
    throw new Error("图片比例请使用宽:高，例如 16:9。");
  if (options.output_format && !["png", "jpeg", "webp"].includes(options.output_format))
    throw new Error("图片格式仅支持 PNG、JPEG 或 WebP。");
  if (options.resolution && !["1K", "2K", "4K"].includes(options.resolution))
    throw new Error("图片分辨率请选择 1K、2K 或 4K。");
  if (options.background && !["transparent", "opaque", "auto"].includes(options.background))
    throw new Error("图片背景选项无效。");
  if (options.quality && !["auto", "low", "medium", "high"].includes(options.quality))
    throw new Error("图片质量选项无效。");
  if (options.background === "transparent") {
    if (options.output_format === "jpeg") throw new Error("JPEG 不支持透明背景，请改用 PNG 或 WebP。");
    options.output_format ??= "png";
  }
  return options;
}

export function imageSize(options: ImageOptions): string {
  if (options.size) return options.size;
  const edge = options.resolution ? Number.parseInt(options.resolution) * 1024 : 1024;
  if (!options.aspect_ratio) return `${edge}x${edge}`;
  const [width, height] = options.aspect_ratio.split(":").map(Number);
  const scale = edge / Math.max(width, height);
  return `${Math.round(width * scale / 16) * 16}x${Math.round(height * scale / 16) * 16}`;
}

export function imageConfig(options: ImageOptions): Record<string, string> {
  let aspect = options.aspect_ratio;
  if (!aspect && options.size) {
    const [width, height] = options.size.split("x").map(Number);
    const gcd = (a: number, b: number): number => b ? gcd(b, a % b) : a;
    const divisor = gcd(width, height);
    aspect = `${width / divisor}:${height / divisor}`;
  }
  return {
    ...(aspect ? { aspectRatio: aspect } : {}),
    ...(options.resolution ? { imageSize: options.resolution } : {}),
  };
}
