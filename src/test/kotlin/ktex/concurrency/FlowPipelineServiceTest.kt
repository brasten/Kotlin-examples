package ktex.concurrency

import kotlinx.coroutines.test.runTest
import me.brasten.ktex.concurrency.EventLogCollector
import me.brasten.ktex.concurrency.ResultsWithLog
import org.junit.jupiter.api.Test
import kotlin.test.Ignore

class FlowPipelineServiceTest {

    @Test
//    @Ignore
    fun `testing parallel flows`(): Unit = runTest {
        println("Building Event Log Collector")
        val eventLog = EventLogCollector()
        println("... starting")
        eventLog.startCollector()
        println("... doAction")

        val service = FlowPipelineService()

        val results = service.doAction(this, eventLog)
        println("Got results!")
        val data = mutableListOf<String>()
        results.collect {
            println("  TEST COMPLETED -> $it")
            data.add(it)
        }

        ResultsWithLog(data, eventLog.receiveEvents())
            .printRunDiagram()
    }
}