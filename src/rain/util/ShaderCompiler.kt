package rain.util

import rain.log
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader

// Helper class which will look in ./data/shaders and compile every .vert/.frag into .vert.spv/.frag.spv
class ShaderCompiler {

    fun findAndCompile() {
        val directory = File("./data/shaders")
        if (directory.isDirectory) {
            log("Compiling shaders in ${directory.absolutePath}")
            val files = directory.listFiles()
            for (file in files) {
                val name = file.name
                if (name.endsWith(".spv")) {
                    continue
                }

                log("Compiling ${file.absoluteFile} -> ${file.absoluteFile}.spv")

                try {
                    val processBuilder = ProcessBuilder("/Users/christophersjoblom/vulkansdk-macos-1.1.85.0/macOS/bin/glslangValidator", "-V", "${file.absoluteFile}", "-o", "${file.absoluteFile}.spv")
                    val process = processBuilder.start()
                    val reader = BufferedReader(InputStreamReader(process.inputStream))

                    val output = ArrayList<String>()
                    for (line in reader.lines()) {
                        output.add(line)
                    }

                    if (output.stream().anyMatch { s -> s.contains("ERROR")}) {
                        log("Error compiling shader: ${file.absoluteFile}")
                        for (i in 1..output.size-1) {
                            log(output[i])
                        }
                    }
                }
                catch (e: IOException) {
                    log("Unable to compile shaders: ${e.message}")
                    return
                }
            }
        }
        else {
            log("No ./data/shaders directory found!")
        }
    }
}