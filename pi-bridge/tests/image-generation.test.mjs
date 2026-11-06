import { test, after } from "node:test";
import assert from "node:assert/strict";
import { mkdtemp, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { pathToFileURL } from "node:url";
import { build } from "esbuild";

const directory = await mkdtemp(join(tmpdir(), "andex-image-test-"));
await build({ entryPoints: ["src/image-generation.ts"], outfile: join(directory, "image.mjs"), platform: "node", format: "esm" });
const { generateImages, isImageGenerationModel } = await import(pathToFileURL(join(directory, "image.mjs")));
after(() => rm(directory, { recursive: true, force: true }));
const config = { model_id: "gpt-image-2", base_url: "https://api.example.test/v1/responses", pi_api: "openai-responses", pi_provider_id: "openai-responses-custom", api_key: "private-test-key" };
const png = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jQ1sAAAAASUVORK5CYII=";
const signal = () => new AbortController().signal;

test("OpenAI image request uses generation endpoint and returns real binary without chat tools", async () => {
    const result = await generateImages(config, "draw", [], signal(), async (url, request) => {
        assert.equal(url, "https://api.example.test/v1/images/generations");
        assert.equal(request.headers.get("Authorization"), "Bearer private-test-key");
        assert.deepEqual(JSON.parse(request.body), { model: "gpt-image-2", prompt: "draw", n: 1, size: "1024x1024" });
        return Response.json({ data: [{ b64_json: png }], usage: { input_tokens: 4 } });
    });
    assert.equal(result.images[0].extension, "png");
    assert.equal(result.images[0].bytes.toString("base64"), png);
});

test("asset download never forwards credentials or custom headers", async () => {
    let count = 0;
    await generateImages(config, "draw", [], signal(), async (url, request) => {
        if (++count === 1) return Response.json({ data: [{ url: "https://cdn.example.test/test.png" }] });
        assert.equal(String(url), "https://cdn.example.test/test.png");
        assert.equal(request.headers, undefined);
        return new Response(Buffer.from(png, "base64"));
    });
    assert.equal(count, 2);
});

test("provider failure is surfaced once, redacted, without billable retries", async () => {
    let count = 0;
    await assert.rejects(generateImages(config, "draw", [], signal(), async () => {
        count++;
        return Response.json({ error: { message: "get_channel_failed private-test-key" } }, { status: 500 });
    }), error => error.message.includes("get_channel_failed") && !error.message.includes("private-test-key"));
    assert.equal(count, 1);
});

test("Google inline image and OpenRouter data URL responses are decoded", async () => {
    for (const [pi_api, pi_provider_id, payload] of [
        ["google-generative-ai", "google", { candidates: [{ content: { parts: [{ inlineData: { data: png, mimeType: "image/png" } }] } }] }],
        ["openai-completions", "openrouter", { choices: [{ message: { images: [{ image_url: { url: "data:" + "image/png;base64," + png } }] } }] }],
    ]) {
        const result = await generateImages({ ...config, pi_api, pi_provider_id }, "draw", [], signal(), async () => Response.json(payload));
        assert.equal(result.images[0].bytes.toString("base64"), png);
    }
});

test("reference images use multipart edits rather than silently dropping the image", async () => {
    await generateImages(config, "edit", [{ data: png, mimeType: "image/png" }], signal(), async (url, request) => {
        assert.ok(url.endsWith("/images/edits"));
        assert.equal(request.headers.has("Content-Type"), false);
        assert.equal(request.body.get("image").type, "image/png");
        return Response.json({ data: [{ b64_json: png }] });
    });
});

test("cancellation and empty image results never report success", async () => {
    const controller = new AbortController(); controller.abort();
    await assert.rejects(generateImages(config, "draw", [], controller.signal, async () => Response.json({ data: [{ b64_json: png }] })));
    await assert.rejects(generateImages(config, "draw", [], signal(), async () => Response.json({ data: [] })), /未返回图片/);
    assert.equal(isImageGenerationModel("gpt-image-2.5-flare"), true);
    assert.equal(isImageGenerationModel("gemini-2.5-pro"), false);
});


test("Nano Banana on OpenAI gateway uses declared Gemini endpoint and key header", async () => {
  await generateImages({ ...config, model_id: "gemini-nano-banana-2.1" }, "draw", [], signal(), async (url, request) => {
    assert.equal(url, "https://api.example.test/v1beta/models/gemini-nano-banana-2.1:generateContent");
    assert.equal(request.headers.get("x-goog-api-key"), config.api_key);
    assert.deepEqual(JSON.parse(request.body).generationConfig.responseModalities, ["TEXT", "IMAGE"]);
    return Response.json({ candidates: [{ content: { parts: [{ inlineData: { data: png } }] } }] });
  });
});
