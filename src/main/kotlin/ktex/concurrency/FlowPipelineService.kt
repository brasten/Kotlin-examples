package ktex.concurrency

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow

fun delayedRequest(value: String): String {
    Thread.sleep(1000L)

    return value
}

class FlowPipelineService {
    val ioScope = Dispatchers.IO.limitedParallelism(5)

    /**
     * CAREFUL: We have to provide a scope from the caller in order for this function to return.
     * If we do something like `coroutineScope`, when it hits the end of the block it will wait (forever)
     * for child coroutines to complete before moving on (this is the point -- structured concurrency).
     *
     * We COULD just use something like `GlobalScope`, but that's not recommended as it could leak memory.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun doAction(
        scope: CoroutineScope,
        eventLog: EventLogCollector? = null,
    ): Flow<String> {
        val periods = scope.produce<Int> {
            for (x in (0..50)) {
                eventLog?.taskCreated(x)
                println("Emitting period: $x")
                send(x)
            }
        }

        val responses = scope.produce<String>(capacity = 1000) {
            repeat(5) { worker ->
                launch {
                    eventLog?.workerStarted(worker)
                    println("Start worker $worker")
                    for (period in periods) {
                        eventLog?.taskStarted(period, worker)
                        val result = async(ioScope) {
                            println("async for $worker / $period")
                            delayedRequest("Period $period")
                        }.await()
                        eventLog?.taskCompleted(period, worker)

                        send(result)
                        println("... end $worker")
                    }
                    eventLog?.workerCompleted(worker)
                }
            }
        }

        println("Responses to consume as flow ...")

        return responses.consumeAsFlow()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun CoroutineScope.mergeChannels(channels: List<ReceiveChannel<String>>): ReceiveChannel<String> =
        produce {
            channels.forEach {
                launch { it.consumeEach { send(it) } }
            }
        }
}