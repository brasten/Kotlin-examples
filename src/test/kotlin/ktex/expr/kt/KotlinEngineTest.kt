package ktex.expr.kt

import io.kotest.matchers.collections.shouldContainExactly
import ktex.plugins.kt.PluginExecutor
import org.junit.jupiter.api.Test

class KotlinEngineTest {
    val executor = PluginExecutor()

    @Test
    fun `test execution`() {
        val res = executor.doExecutor(listOf(), "listOf(1L, 2L, 10L)")

        res.shouldContainExactly(1L, 2L, 10L)
    }
}