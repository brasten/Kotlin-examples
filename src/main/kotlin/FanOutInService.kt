package me.brasten

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow

class FanOutInService {
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun doAction(scope: CoroutineScope, count: Int = 500): Flow<String> {
        val report_CH = Channel<String>(100)

        val producer = scope.produce {
            (0..<count).forEach {
                println("Produce ${it}")
                send(it)
            }
        }

        val jobs = (0..<10).map {
            scope.async {
                for (num in producer) {
                    val report = "Worker ${it} - num ${num}"
                    report_CH.send(report)
                    println("Report -> ${report}")
                }
            }
        }

        scope.launch {
            println("CLOSE WATCHER")
            jobs.awaitAll()
            report_CH.close()
        }

        return report_CH.consumeAsFlow()
    }
}
