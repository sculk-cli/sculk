# inspo: https://github.com/Infinidoge/nix-minecraft/blob/master/pkgs/tools/fetchPackwizModpack/default.nix
{
  stdenvNoCC,
  sculk,
  jre_headless,
}: let
  fetchSculkModpack = {
    pname ? "sculk-modpack",
    version ? "",
    url,
    hash,
    ...
  } @ args:
    stdenvNoCC.mkDerivation (finalAttrs:
      {
        inherit pname version;

        dontUnpack = true;
        dontFixup = true;

        buildInputs = [];

        buildPhase = ''
          runHook preBuild
          ${jre_headless}/bin/java -jar ${sculk}/lib/sculk install "${url}" .
          runHook postBuild
        '';

        installPhase = ''
          runHook preInstall
          rm install.sculk.json
          mkdir -p $out
          cp -r * $out/
          runHook postInstallsculk
        '';

        outputHashAlgo = "sha256";
        outputHashMode = "recursive";
        outputHash = hash;
      }
      // args);
in
  fetchSculkModpack
