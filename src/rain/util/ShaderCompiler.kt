package rain.util

import rain.log
import java.io.File

// Helper class which will look in ./data/shaders and compile every .vert/.frag into .vert.spv/.frag.spv
class ShaderCompiler {

    fun findAndCompile() {
        val directory = File("./data/shaders")
        if (directory.isDirectory) {
            val files = directory.listFiles()
            for (file in files) {
                val name = file.name
                Runtime.getRuntime().exec("glslangValidator -V $name -o $name.spv")
            }
        }
        else {
            log("No ./data/shaders directory found!")
        }
    }
}