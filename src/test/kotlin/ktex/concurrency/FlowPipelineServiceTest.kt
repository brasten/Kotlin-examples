package ktex.concurrency

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.newCoroutineContext
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class FlowPipelineServiceTest {

    @Test
    fun `testing parallel flows`(): Unit = runTest {
        val eventLog = EventLogCollector()
        eventLog.startCollector()

        val service = FlowPipelineService()

        val results = service.doChannelsConsumedAsFlow(this, eventLog)
        println("Got results!")
        val data = mutableListOf<String>()
        results.collect {
            println("  TEST COMPLETED -> $it")
            data.add(it)
        }

        ResultsWithLog(data, eventLog.receiveEvents())
            .printRunDiagram()
    }

    @Nested
    inner class DoFlowThings {
        @Test
        fun `basic flow`(): Unit = runTest {
            val eventLog = EventLogCollector()
            eventLog.startCollector()

            val ch = Channel<String>()
            val chReceiver = async {
                for (msg in ch) {
                    println("MSG: $msg")
                }
            }

            println("FlowPipelineService()")
            val service = FlowPipelineService()
            val flow = service.doFlowThings(eventLog)
            println("got flow")
            flow
                .flowOn(Dispatchers.Default)
                .collect { println("ch.send(${it})"); ch.send(it) }
            println("collected.")
            ch.close()

//            chReceiver.await()
        }
    }
}