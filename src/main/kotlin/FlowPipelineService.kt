package me.brasten

import kotlinx.coroutines.*
import kotlinx.coroutines.GlobalScope.coroutineContext
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.consumeAsFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.coroutines.coroutineContext

fun delayedRequest(value: String): String {
    Thread.sleep(5000L)

    return value
}

class FlowPipelineService {

    val ioScope = Dispatchers.IO.limitedParallelism(5)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    suspend fun doAction(scope: CoroutineScope): Flow<String> {
        val periods = scope.produce<Int> {
            for (x in (0..50)) {
                println("Emitting period: $x")
                send(x)
            }
        }

        val responses = scope.produce<String>(capacity = 1000) {
            repeat(5) { worker ->
                launch {
                    println("Start worker $worker")
                    for (period in periods) {
                        val result = async(ioScope) {
                            println("async for $worker / $period")
                            delayedRequest("Period $period")
                        }.await()

                        send(result)
                        println("... end $worker")
                    }
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