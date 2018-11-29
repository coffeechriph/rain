package rain

class ExceptionHandler: Thread.UncaughtExceptionHandler {
    override fun uncaughtException(t: Thread?, e: Throwable?) {
        endLog()

        if (t != null && e != null) {
            assertion("Uncaught exception in ${t.name}: ${e.message!!}")
        }
    }
}
