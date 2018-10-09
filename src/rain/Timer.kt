package rain

class Timer {
    var framesPerSecond = -1
        private set

    var deltaTime = 0.0f
        private set

    private var frameCounter = 0
    private var lastTimeInMilliseconds = System.currentTimeMillis()
    private var lastFrameInMilliseconds = System.currentTimeMillis()

    fun update() {
        if (System.currentTimeMillis() - lastTimeInMilliseconds >= 1000L) {
            framesPerSecond = frameCounter
            frameCounter = 0
            lastTimeInMilliseconds = System.currentTimeMillis()
        }

        frameCounter++
        deltaTime = (System.currentTimeMillis() - lastFrameInMilliseconds) / 1000.0f
        lastFrameInMilliseconds = System.currentTimeMillis()
    }
}
