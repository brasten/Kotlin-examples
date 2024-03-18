package me.brasten

import kotlinx.coroutines.*

class ConcurrencyService {


    /**
     * This demonstrates map/async pattern where the actual parallel execution depends entirely
     * on the coroutine scope the function was called in.
     *
     * When called on the main dispatcher, this effectively executes one at a time.
     *
     */
    suspend fun doConcurrentOperationsWithCallingContext(opCount: Int = 100, delayMillis: Long = 100): Collection<String> = coroutineScope {
        val deferred = (0 until opCount).map { idx ->
            print("[$idx] MAP | ")
            async {
                print("[$idx] ASYNC start | ")
                Thread.sleep(delayMillis)
                print("[$idx] ASYNC end | ")
                "IDX: $idx"
            }
        }
        print("Finished scheduling ... | ")
        deferred.awaitAll().also { println() }
    }

    /**
     * This demonstrates map/async pattern where the context is explicitly set.
     *
     * This causes concurrency to run in parallel.
     */
    suspend fun doConcurrentOperationsWithSelfDefinedContext(
        opCount: Int = 100,
        delayMillis: Long = 100,
    ): Collection<String> = withContext(Dispatchers.Default) {
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