- When running `sculk install` with an existing installed pack present, any existing mods that are
  up to date will not be redownloaded. Note that if a file is removed from the pack, it will
  currently remain present in the users install even if they update.
- Add `sculk export multimc` to export MultiMC-compatible (i.e. MultiMC and Prism) instances.
  Currently, the exported ZIPs have to bundle the entire Sculk JAR and all of its dependencies, but
  this will change in the future.
