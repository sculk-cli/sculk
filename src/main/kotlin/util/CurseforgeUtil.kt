package tech.jamalam.util

import tech.jamalam.curseforge.models.CurseforgeModLoader
import tech.jamalam.curseforge.models.CurseforgeSide
import tech.jamalam.pack.ModLoader
import tech.jamalam.pack.Side

fun ModLoader.toCurseforge(): CurseforgeModLoader = when (this) {
    ModLoader.Neoforge -> CurseforgeModLoader.Neoforge
    ModLoader.Fabric -> CurseforgeModLoader.Fabric
    ModLoader.Forge -> CurseforgeModLoader.Forge
    ModLoader.Quilt -> CurseforgeModLoader.Quilt
}

fun CurseforgeSide.toSide(): Side = when (this) {
    CurseforgeSide.Client -> Side.ClientOnly
    CurseforgeSide.Server -> Side.ServerOnly
    CurseforgeSide.Both -> Side.Both
}
