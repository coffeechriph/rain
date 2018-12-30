package rain.api

class Timer {
    var framesPerSecond = -1
        private set

    private var frameCounter = 0
    private var lastTimeInMilliSeconds = System.currentTimeMillis()

    fun update() {
        val milliTime = System.currentTimeMillis()
        if (milliTime - lastTimeInMilliSeconds >= 1_000L) {
            framesPerSecond = frameCounter
            frameCounter = 0
            lastTimeInMilliSeconds = milliTime
        }

        frameCounter++
    }
}
