package ktex.concurrency

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.toList
import me.brasten.ktex.concurrency.Event
import me.brasten.ktex.concurrency.EventLogCollector
import me.brasten.ktex.concurrency.ResultsWithLog

class ConcurrencyService {
    /**
     * This demonstrates map/async pattern where the actual parallel execution depends entirely
     * on the coroutine scope the function was called in.
     *
     * When called on the main dispatcher, this effectively executes one at a time.
     *
     */
    suspend fun doConcurrentOperationsWithCallingContext(
        opCount: Int = 100,
        delayMillis: Long = 100,
    ): ResultsWithLog<String> = coroutineScope {
        EventLogCollector().run { log ->
            log.operationStarted()

            val deferred = (0 until opCount).map { idx ->
                log.taskCreated(idx)
                async {
                    log.taskStarted(idx)
                    Thread.sleep(delayMillis)
                    log.taskCompleted(idx)
                    "IDX: $idx"
                }
            }
            log.operationInfo("awaiting")
            val result = deferred.awaitAll()
            log.operationCompleted()

            result
        }
    }

    /**
     * This demonstrates map/async pattern where the context is explicitly set.
     *
     * This causes concurrency to run in parallel.
     */
    suspend fun doConcurrentOperationsWithSelfDefinedContext(
        opCount: Int = 100,
        delayMillis: Long = 100,
    ): ResultsWithLog<String> = withContext(Dispatchers.Default) {
        doConcurrentOperationsWithCallingContext(opCount, delayMillis)
    }

    /**
     *
     */
    suspend fun doConcurrentOperationsThatBlock(
        opCount: Int = 100,
        delayMillis: Long = 100,
    ) = coroutineScope {
        val dispatcher = newFixedThreadPoolContext(5, "blocking")

        (0 until opCount).map { idx ->
            println("[$idx] MAP | ")
            async(dispatcher) {
                println("[$idx] ASYNC start | ")
                launch(dispatcher) { Thread.sleep(delayMillis) }.join()
                println("[$idx] ASYNC end | ")
                "IDX: $idx"
            }
        }.awaitAll()
    }

}