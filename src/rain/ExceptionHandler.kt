package rain

import rain.api.assertion
import rain.api.endLog

class ExceptionHandler: Thread.UncaughtExceptionHandler {
    override fun uncaughtException(t: Thread?, e: Throwable?) {
        if (t != null && e != null) {
            assertion("Uncaught exception in ${t.name}: ${e.message!!}")
        }

        endLog()
    }
}
