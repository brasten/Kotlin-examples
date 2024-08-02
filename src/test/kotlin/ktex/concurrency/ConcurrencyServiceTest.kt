package ktex.concurrency

import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.longs.shouldBeLessThan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.measureTimedValue

@Execution(ExecutionMode.CONCURRENT)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class ConcurrencyServiceTest {
    val service = ConcurrencyService()

    @Nested
    inner class BasicCoroutineAsyncMap {
        @Test
        fun `when called on Main Thread dispatcher - essentially has no parallelism`(): Unit = runTest {
            // Run diagram shows all tasks are scheduled, then proceed to execute one at a time:
            //
            //   0: #[]
            //   1: #  []
            //   2: #    []
            //   3: #      []
            //   4: #        []
            //   5: #          []
            //   6: #            []
            //   7: #              []
            //   8: #                []
            //   9: #                  []
            //  10: #                    []
            //  11: #                      []
            //
            val (result, duration) = measureTimedValue {
                service.doConcurrentOperationsWithCallingContext(
                    opCount = 50,
                    delayMillis = 100,
                )
            }

            result.printRunDiagram()

            assertEquals(50, result.results.size)
            duration.inWholeMilliseconds shouldBeGreaterThan 4500
        }

        @Test
        fun `when called from scope with backing thread pool - runs in parallel`(): Unit = runTest {
            // Run diagram looks something like:
            //
            //  11:        #            [................]
            //  12:        #             [................]
            //  13:        #              [....]
            //  14:        #                [...]
            //  15:        #                       [...............]
            //  16:        #                        [...............]
            //  17:        #                      [............]
            //  18:        #                     [............]
            //  19:        #                           [.....]
            //  20:        #                            [............]
            //  21:        #                               [...............]
            //
            // Demonstrating that multiple async tasks are executing in parallel.
            //

            val (result, duration) = withContext(Dispatchers.Default) {
                measureTimedValue {
                    service.doConcurrentOperationsWithCallingContext(
                        opCount = 50,
                        delayMillis = 100,
                    )
                }
            }

            result.printRunDiagram()

            assertEquals(50, result.results.size)
            duration.inWholeMilliseconds shouldBeLessThan 4000
        }
    }

    @Nested
    inner class BasicCoroutineAsyncMapWithSelfDefinedContext {
        @Test
        fun `when called on Main Thread dispatcher - is in parallel because it sets own context`(): Unit = runTest {
            val (result, duration) = measureTimedValue {
                service.doConcurrentOperationsWithSelfDefinedContext(
                    opCount = 50,
                    delayMillis = 100,
                )
            }

            assertEquals(50, result.results.size)
            duration.inWholeMilliseconds shouldBeLessThan 3000
        }
    }

    @Nested
    inner class CoroutingAsyncsThatBlock {
        @Test
        @Ignore
        fun `when a dispatcher cannot be used to process results`(): Unit = runTest {
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