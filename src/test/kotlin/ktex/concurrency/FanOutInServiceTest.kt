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

        val flow = service.doAction(this)
        val results = flow.toList()

        assertEquals(500, results.size)
    }

    @Test
    fun `testing parallel flows with test scope`(): Unit = runTest {
        val service = FanOutInService()

        coroutineScope {
            val flow = service.doAction(this)
            println("Flow received!")
            val results = flow.toList()
            println("Got results!")

            assertEquals(500, results.size)
        }
    }
}