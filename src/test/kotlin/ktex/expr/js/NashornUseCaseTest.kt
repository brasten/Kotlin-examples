package ktex.expr.js

import org.junit.jupiter.api.Test
import javax.script.ScriptEngineManager

class NashornUseCaseTest {
    var nashornEngine = ScriptEngineManager().getEngineByName("Nashorn")

    @Test
    fun `testing scripting`() {
        val result = nashornEngine.eval("""
            print("Hello!")
            
            "World"
        """.trimIndent())

        println("Result is: ${result}")
    }

}