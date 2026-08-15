import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const CONFIG_PATH = path.resolve(__dirname, "..", "opencode.json");

const PROFILE_MAP = Object.freeze({
  opencode: Object.freeze({
    model: "opencode-go/deepseek-v4-pro",
    small_model: "opencode-go/deepseek-v4-flash",
    heavy_model: "opencode-go/deepseek-v4-pro",
  }),
  openai: Object.freeze({
    model: "openai/gpt-5.4",
    small_model: "openai/gpt-5.4-mini",
    heavy_model: "openai/gpt-5.4",
  }),
  google: Object.freeze({
    model: "google/gemini-3.1-pro-preview-customtools",
    small_model: "google/gemini-3.5-flash",
    heavy_model: "google/gemini-3.1-pro-preview-customtools",
  }),
  copilot: Object.freeze({
    model: "copilot/gpt-5.4",
    small_model: "copilot/gpt-5.4-mini",
    heavy_model: "copilot/gpt-5.4",
  }),
});

const SUPPORTED_PROFILES = Object.keys(PROFILE_MAP);
const HEAVY_AGENTS = ["spec-writer", "code-reviewer", "team-lead", "architect"];
const ALL_OVERRIDDEN_AGENTS = [...HEAVY_AGENTS, "verifier"];

function normalizeProfileValue(value) {
  return typeof value === "string" ? value.trim() : "";
}

function assertKnownProfile(value) {
  if (!SUPPORTED_PROFILES.includes(value)) {
    throw new Error(
      `Unknown provider profile "${value}". Must be one of: ${SUPPORTED_PROFILES.join(", ")}`,
    );
  }
}

function pickProfile(options = {}) {
  const optionProfile = normalizeProfileValue(options.profile);
  const envProfile = normalizeProfileValue(process.env.OPENCODE_PROVIDER_PROFILE);
  const activeProfile = optionProfile || envProfile || "opencode";

  assertKnownProfile(activeProfile);

  return activeProfile;
}

export default function resolveProfile(options = {}) {
  const activeProfile = pickProfile(options);
  const profile = PROFILE_MAP[activeProfile];

  return {
    model: profile.model,
    small_model: profile.small_model,
    agent_overrides: {
      "spec-writer": profile.heavy_model,
      verifier: profile.small_model,
      "code-reviewer": profile.heavy_model,
      "team-lead": profile.heavy_model,
      architect: profile.heavy_model,
    },
  };
}

export function buildProfileConfig(options = {}) {
  const resolved = resolveProfile(options);

  return {
    model: resolved.model,
    small_model: resolved.small_model,
    agent: Object.fromEntries(
      ALL_OVERRIDDEN_AGENTS.map((name) => [name, { model: resolved.agent_overrides[name] }]),
    ),
  };
}

function isProviderProfilePlugin(entry) {
  return typeof entry === "string" && entry.replaceAll("\\", "/").endsWith("provider-profile.mjs");
}

export function readPluginOptions(configPath = CONFIG_PATH) {
  try {
    const config = JSON.parse(fs.readFileSync(configPath, "utf8"));
    const plugins = Array.isArray(config.plugin) ? config.plugin : [];

    for (const plugin of plugins) {
      if (Array.isArray(plugin) && plugin.length === 2 && isProviderProfilePlugin(plugin[0])) {
        return plugin[1] && typeof plugin[1] === "object" ? plugin[1] : {};
      }
    }
  } catch {
    return {};
  }

  return {};
}

/**
 * ProviderProfilePlugin — runtime hook placeholder (no-op).
 *
 * This is a no-op runtime hook for opencode's plugin system.
 * The actual model-profile switching happens outside the runtime hook:
 *
 *   1) `bin/moc` (the shell launcher) or direct invocation sets
 *      `OPENCODE_CONFIG_CONTENT` by running this module directly:
 *      `node plugins/provider-profile.mjs`
 *
 *   2) The direct-execution path at the bottom of this file generates
 *      the JSON config override fragment that opencode merges at startup.
 *
 * This function exists so the module is loadable as an opencode plugin,
 * but no runtime transformation is needed because the config override is
 * already applied via `OPENCODE_CONFIG_CONTENT` at process launch.
 *
 * @returns {object} Empty object — no runtime hook behavior.
 */
export function ProviderProfilePlugin() {
  return {};
}

function isDirectExecution() {
  const scriptPath = process.argv[1];
  return scriptPath ? path.resolve(scriptPath) === __filename : false;
}

if (isDirectExecution()) {
  const options = readPluginOptions();
  process.stdout.write(`${JSON.stringify(buildProfileConfig(options))}\n`);
}
