{
  description = "Sculk: A program for creating Minecraft modpacks";

  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = {
    self,
    nixpkgs,
    flake-utils,
    ...
  }: {
    formatter.x86_64-linux = nixpkgs.legacyPackages.x86_64-linux.alejandra;
    packages = flake-utils.lib.eachDefaultSystem (system: let
      pkgs = import nixpkgs {
        inherit system;
      };
    in {
      sculk = pkgs.callPackage ./sculk.nix {};
    });

    nixFunctions.fetchSculkModpack = {
      stdenvNoCC,
      sculk,
      jre_headless,
    }:
      import ./fetch-sculk-modpack.nix {inherit stdenvNoCC sculk jre_headless;};
  };
}
