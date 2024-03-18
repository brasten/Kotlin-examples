import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.longs.shouldBeLessThan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import me.brasten.ConcurrencyService
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.time.measureTimedValue

class ConcurrencyServiceTest {
    val service = ConcurrencyService()

    @Nested
    inner class BasicCoroutineAsyncMap {
        @Test
        fun `when called on Main Thread dispatcher - essentially has no parallelism`() = runBlocking<Unit> {
            val (result, duration) = measureTimedValue {
                service.doConcurrentOperationsWithCallingContext(
                    opCount = 50,
                    delayMillis = 100,
                )
            }

            assertEquals(50, result.size)
            duration.inWholeMilliseconds shouldBeGreaterThan 4500
        }

        @Test
        fun `when called from scope with backing thread pool - runs in parallel`() = runBlocking<Unit> {
            withContext(Dispatchers.Default) {
                val (result, duration) = measureTimedValue {
                    service.doConcurrentOperationsWithCallingContext(
                        opCount = 50,
                        delayMillis = 100,
                    )
                }

                assertEquals(50, result.size)
                duration.inWholeMilliseconds shouldBeLessThan 1000
            }
        }
    }

    @Nested
    inner class BasicCoroutineAsyncMapWithSelfDefinedContext {
        @Test
        fun `when called on Main Thread dispatcher - is in parallel because it sets own context`() = runBlocking<Unit> {
            val (result, duration) = measureTimedValue {
                service.doConcurrentOperationsWithSelfDefinedContext(
                    opCount = 50,
                    delayMillis = 100,
                )
            }

            assertEquals(50, result.size)
            duration.inWholeMilliseconds shouldBeLessThan 1000
        }
    }

    @Nested
    inner class CoroutingAsyncsThatBlock {
        @Test
        fun `when a dispatcher cannot be used to process results`() = runBlocking<Unit> {
            val (result, duration) = measureTimedValue {
                service.doConcurrentOperationsThatBlock(
                    opCount = 50,
                    delayMillis = 100,
                )
            }

//            assertEquals(50, result.size)
            duration.inWholeMilliseconds shouldBeLessThan 1000
        }
    }
}