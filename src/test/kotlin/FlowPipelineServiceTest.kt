import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import me.brasten.FanOutInService
import me.brasten.FlowPipelineService
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class FlowPipelineServiceTest {

    @Test
    fun `testing parallel flows`() = runBlocking<Unit> {
        val service = FlowPipelineService()

        val results = service.doAction(this)
        println("Got results!")
        results.collect {
            println("  TEST COMPLETED -> $it")
        }
    }
}