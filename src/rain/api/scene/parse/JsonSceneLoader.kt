package rain.api.scene.parse

import kotlinx.serialization.json.Json
import java.io.File
import java.nio.charset.StandardCharsets

class JsonSceneLoader : SceneLoader {
    override fun load(file: String): SceneDefinition {
        val content = File(file).readText(StandardCharsets.UTF_8)
        return Json.parse(SceneDefinition.serializer(), content)
    }

    override fun save(file: String, sceneDefinition: SceneDefinition) {
        val json = Json.stringify(SceneDefinition.serializer(), sceneDefinition)
        File(file).writeText(json, StandardCharsets.UTF_8)
    }
}
