import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import me.brasten.FanOutInService
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals


class FanOutInServiceTest {

    @Test
    fun `testing parallel flows`() = runBlocking<Unit> {
        val service = FanOutInService()

        val flow = service.doAction(this)
        val results = flow.toList()

        assertEquals(500, results.size)
    }

    @Test
    fun `testing parallel flows with test scope`() = runBlocking<Unit> {
        val service = FanOutInService()

        val scope = TestScope()
        val flow = service.doAction(scope)
        println("Flow received!")
        scope.advanceUntilIdle()
        println("Idle'd scope.")
        val results = flow.toList()
        println("Got results!")

        assertEquals(500, results.size)
    }
}