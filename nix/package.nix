{
  stdenv,
  lib,
  fetchurl,
  makeWrapper,
  jre_headless,
}:
stdenv.mkDerivation rec {
  pname = "sculk";
  version = "0.1.0+beta.18";

  src = fetchurl {
    url = "https://github.com/sculk-cli/${pname}/releases/download/${version}/${pname}-${version}.jar";
    hash = "sha256-qQkinfAF4sQqz9Xs2Nv5PNGAQp+HYV5u+cls/1nIPSs=";
  };

  dontUnpack = true;

  nativeBuildInputs = [makeWrapper];

  installPhase = ''
    mkdir -p $out/lib
    cp $src $out/lib/sculk

    makeWrapper ${jre_headless}/bin/java $out/bin/sculk \
      --add-flags "-jar $out/lib/sculk"
  '';

  meta = with lib; {
    description = "A program for creating Minecraft modpacks";
    homepage = "https://github.com/sculk-cli/sculk";
    sourceProvenance = with sourceTypes; [binaryBytecode];
    license = licenses.mit;
    inherit (jre_headless.meta) platforms;
    mainProgram = "sculk";
  };
}
