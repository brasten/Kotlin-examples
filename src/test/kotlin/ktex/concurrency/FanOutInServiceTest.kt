package ktex.concurrency

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals


class FanOutInServiceTest {

    @Test
    fun `testing parallel flows`(): Unit = runTest {
        val service = FanOutInService()
        val eventLog = EventLogCollector().apply{ startCollector() }
        val flow = service.doAction(this, eventLog = eventLog)
        val results = flow.toList()

        eventLog.receiveEvents().printLog()

        assertEquals(500, results.size)
    }

    @Test
    fun `testing parallel flows with test scope`(): Unit = runTest {
        val service = FanOutInService()
        val eventLog = EventLogCollector().apply{ startCollector() }

        val results = coroutineScope {
            eventLog.operationStarted()
            val flow = service.doAction(this, eventLog = eventLog)
            eventLog.operationInfo("received flow")
            val results = flow.toList()
            eventLog.operationInfo("received flow contents")
            eventLog.operationCompleted()
            results
        }
        assertEquals(500, results.size)
        eventLog.receiveEvents().printLog()
    }
}