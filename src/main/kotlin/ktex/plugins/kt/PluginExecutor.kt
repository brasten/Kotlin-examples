package ktex.plugins.kt

import javax.script.ScriptEngineManager

class PluginExecutor {

    fun doExecutor(
        measures: List<Long>,
        expression: String,
    ): List<Long> {
        val engine = ScriptEngineManager().getEngineByName("kotlin")
//        val compiledExpression = engine.compile(expression)

//        val res1 = compiledExpression.eval()

        val res1 = engine.eval(expression)

        return res1 as List<Long>
    }
}