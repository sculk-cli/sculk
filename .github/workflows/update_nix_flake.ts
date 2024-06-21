const REGEX_VERSION = `version\\s*=\\s*"(.+)"`;
const REGEX_HASH = `hash\\s*=\\s*"(.+)"`;

async function gh<T>(url: string): Promise<T> {
  const headers = new Headers();
  headers.set("Accept", "application/vnd.github.v3+json");
  const response = await fetch(url, { headers });
  return await response.json();
}

const config = await Deno.readTextFile("./nix/package.nix");
const version = new RegExp(REGEX_VERSION).exec(config)![1];
const hash = new RegExp(REGEX_HASH).exec(config)![1];

const releases = await gh<unknown[]>(
  `https://api.github.com/repos/sculk-cli/sculk/releases`,
);
const newVersion =  releases[0]["tag_name"];

const prefetchData = await new Deno.Command("nix", {
  args: ["store", "prefetch-file", `https://github.com/sculk-cli/sculk/releases/download/${newVersion}/sculk-${newVersion}.jar`],
	stderr: "piped",
}).output();
const newHash = new TextDecoder().decode(prefetchData.stderr).trim().split("sha256-")[1].split("'")[0];

const newConfig = config
	.replace(version, newVersion)
  .replace(hash, `sha256-${newHash}`);
await Deno.writeTextFile("./nix/package.nix", newConfig);
