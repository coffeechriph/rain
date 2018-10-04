package rain

class Timer {
    var framesPerSecond = -1
        private set
    private var frameCounter = 0
    private var lastTimeInMilliseconds = System.currentTimeMillis()

    fun update() {
        if (System.currentTimeMillis() - lastTimeInMilliseconds >= 1000L) {
            framesPerSecond = frameCounter
            frameCounter = 0
            lastTimeInMilliseconds = System.currentTimeMillis()
        }

        frameCounter++
    }
}
