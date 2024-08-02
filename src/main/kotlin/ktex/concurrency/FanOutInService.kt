package ktex.concurrency

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow

class FanOutInService {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun doAction(
        scope: CoroutineScope,
        count: Int = 500,
        eventLog: EventLogCollector
    ): Flow<String> {
        val report_CH = Channel<String>(100)

        val producer = scope.produce {
            (0..<count).forEach {
                eventLog.channelSend("producer", it.toString())
                send(it)
            }
        }

        val workers = (0..<10).map { jobId ->
            scope.async {
                for (num in producer) {
                    eventLog.channelReceive("producer", num.toString(), workerId = jobId.toString(), taskId = num.toString())
                    val report = "Worker ${jobId} - num ${num}"
                    report_CH.send(report)
                    eventLog.channelSend("report", report, workerId = jobId.toString(), taskId = num.toString())
                }
            }
        }

        scope.launch {
            eventLog.workerStarted("closer")
            eventLog.taskStarted("await workers", workerId = "closer")
            workers.awaitAll()
            eventLog.taskCompleted("await workers", workerId = "closer")
            report_CH.close()
            eventLog.workerCompleted("closer")
        }

        return report_CH.consumeAsFlow()
    }
}
