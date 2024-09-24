package io.github.sculk_cli.multimc

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private const val MMC_PACK_JSON_FORMAT_VERSION: Int = 1

@OptIn(ExperimentalSerializationApi::class)
private val json = Json {
	explicitNulls = false
	coerceInputValues = true
}

public sealed class MultiMcPackComponent(public val version: String) {
	public class Minecraft(version: String) : MultiMcPackComponent(version)
	public class FabricLoader(version: String) : MultiMcPackComponent(version)
	public class Neoforge(version: String) : MultiMcPackComponent(version)
	public class MinecraftForge(version: String) : MultiMcPackComponent(version)
	public class QuiltLoader(version: String) : MultiMcPackComponent(version)
}

@Serializable
private data class MmcPackJson(
	val formatVersion: Int,
	val components: List<MmcPackJsonComponent>,
)

@Serializable
private data class MmcPackJsonComponent(
	val important: Boolean? = null,
	val uid: String,
	val version: String,
)

public fun createMultiMcCompatiblePack(
	path: Path,
	name: String,
	components: List<MultiMcPackComponent>,
	files: Map<String, ByteArray>,
	packUrl: String? = null,
	sculkJarLocation: String? = null,
) {
	val outputStream = ZipOutputStream(FileOutputStream(path.toFile()))
	val packComponents = mutableListOf<MmcPackJsonComponent>()
	outputStream.writeEntry("instance.cfg", buildInstanceCfg(name, packUrl))

	for (component in components) {
		val uid = when (component) {
			is MultiMcPackComponent.Minecraft -> "net.minecraft"
			is MultiMcPackComponent.FabricLoader -> "net.fabricmc.fabric-loader"
			is MultiMcPackComponent.Neoforge -> "net.neoforged"
			is MultiMcPackComponent.MinecraftForge -> "net.minecraftforge"
			is MultiMcPackComponent.QuiltLoader -> "org.quiltmc.quilt-loader"
		}

		packComponents.add(MmcPackJsonComponent(uid = uid, version = component.version))
	}

	val mmcPackJson = MmcPackJson(
		formatVersion = MMC_PACK_JSON_FORMAT_VERSION,
		components = packComponents,
	)

	val mmcPackJsonBytes = json.encodeToString(mmcPackJson)
	outputStream.writeEntry("mmc-pack.json", mmcPackJsonBytes.encodeToByteArray())

	for ((filePath, bytes) in files) {
		outputStream.writeEntry(".minecraft/$filePath", bytes)
	}

	if (packUrl != null) {
		if (sculkJarLocation != null) {
			outputStream.writeEntry(".minecraft/_sculk.jar", File(sculkJarLocation).readBytes())
		} else {
			val sculkJar = findSculkJar()
			if (sculkJar != null) {
				outputStream.writeEntry(".minecraft/_sculk.jar", sculkJar.readBytes())
			} else {
				error("Could not find sculk.jar automatically and location was not manually specified")
			}
		}
	}

	outputStream.close()
}

private fun buildInstanceCfg(name: String, packUrl: String?): ByteArray {
	var cfg = """
        [General]
        ConfigVersion=1.2
        InstanceType=OneSix
        iconKey=default
        name=$name
    """.trimIndent()

	if (packUrl != null) {
		cfg += "\nOverrideCommands=true"
		cfg += "\nPreLaunchCommand=\\\"\$INST_JAVA\\\" -jar \\\"\$INST_DIR/.minecraft/_sculk.jar\\\" install \\\"$packUrl\\\" \\\"\$INST_MC_DIR\\\" --side client"
	}

	return cfg.toByteArray()
}

private fun findSculkJar(): File? {
	val sculkDir = File(System.getProperty("user.home")).resolve(".sculk")
	for (file in sculkDir.walkTopDown()) {
		if (file.isFile && file.extension == "jar") {
			return file
		}
	}

	val path = Paths.get(MultiMcPackComponent::class.java.getProtectionDomain().getCodeSource().getLocation().toURI())
	
	if (path.toFile().isFile && path.toFile().extension == "jar") {
		return path.toFile()
	}

	return null
}

private fun ZipOutputStream.writeEntry(path: String, data: ByteArray) {
	val entry = ZipEntry(path)
	putNextEntry(entry)
	write(data)
	closeEntry()
}
