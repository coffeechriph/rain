package rain

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.stream.Collectors

private var logBuffer = ByteArray(10240)
private var logIndex = 0
private lateinit var logFile: File
private lateinit var outputStream: BufferedOutputStream

fun startLog() {
    createLogDirectory()
    removeIfTooManyLogs()
    logFile = File("./log/${System.currentTimeMillis()+(Random().nextInt(10_000_00))}.txt")
    outputStream = BufferedOutputStream(FileOutputStream(logFile));
}

fun createLogDirectory() {
    val directory = File("./log")
    if (!directory.exists()) {
        if(!directory.mkdir()) {
            assertion("Unable to create log directory!")
        }
    }
}

fun removeIfTooManyLogs() {
    val directory = File("./log")
    if (directory.exists()) {
        if (directory.isDirectory) {
            if (directory.listFiles().size > 30) {
                val last = directory.listFiles().asList().stream()
                        .sorted (compareBy<File>({file -> file.lastModified()}, {file2 -> file2.lastModified()}))
                        .collect(Collectors.toList())

                for (i in 0 until 10) {
                    last[last.size-1-i].delete()
                }
            }
        }
        else {
            assertion("No directory named 'log' exists! It's a file.")
        }
    }
    else {
        assertion("Log directory does not exist!")
    }
}

fun endLog() {
    if (logIndex > 0) {
        outputStream.write(logBuffer, 0, logIndex)
    }
    outputStream.flush()
    outputStream.close()
}

fun log(text: String) {
    val stackWalker = StackWalker.getInstance()
    val frame = stackWalker.walk { stream ->
        val stack = stream
            .filter { s -> !s.className.contains("rain") }
            .collect(Collectors.toList())
        stack[0]
    }

    val finalString = frame.className + ".${frame.methodName}[@${frame.lineNumber}]: " + text + System.getProperty("line.separator")

    if (logIndex + finalString.length >= logBuffer.size) {
        outputStream.write(logBuffer)
        logIndex = 0;
    }

    for (c in finalString) {
        logBuffer[logIndex++] = c.toByte()
    }

    print(finalString)
}

fun assertion(text: String): Nothing {
    log(text)
    throw AssertionError(text)
}