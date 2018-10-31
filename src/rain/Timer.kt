package rain

class Timer {
    var framesPerSecond = -1
        private set

    var deltaTime = 0.0f
        private set

    private var frameCounter = 0
    private var lastTimeInNanoSeconds = System.nanoTime()
    private var lastFrameInNanoSeconds = System.nanoTime()

    fun update() {
        val nanoTime = System.nanoTime()
        if (nanoTime - lastTimeInNanoSeconds >= 1_000_000_000L) {
            framesPerSecond = frameCounter
            frameCounter = 0
            lastTimeInNanoSeconds = nanoTime
        }

        frameCounter++
        deltaTime = (nanoTime - lastFrameInNanoSeconds) / 1_000_000_000.0f
        lastFrameInNanoSeconds = nanoTime
    }
}
