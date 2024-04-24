package tech.jamalam.util

import tech.jamalam.curseforge.models.CurseforgeModLoader
import tech.jamalam.pack.ModLoader

fun ModLoader.toCurseforge(): CurseforgeModLoader = when (this) {
    ModLoader.Neoforge -> CurseforgeModLoader.Neoforge
    ModLoader.Fabric -> CurseforgeModLoader.Fabric
    ModLoader.Forge -> CurseforgeModLoader.Forge
    ModLoader.Quilt -> CurseforgeModLoader.Quilt
}
