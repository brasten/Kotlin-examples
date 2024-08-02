package ktex.expr.cel

import dev.cel.common.types.CelTypes
import dev.cel.compiler.CelCompilerFactory
import dev.cel.runtime.CelRuntimeFactory
import org.junit.jupiter.api.Test

class CelSampleServiceTest {
    val runtime = CelRuntimeFactory.standardCelRuntimeBuilder().build()

    @Test
    fun `CEL samples`() {
        val compiler = CelCompilerFactory
            .standardCelCompilerBuilder()
            .addVar("my_var", CelTypes.STRING)
            .build()

//            CelCompilerFactory.standardCelCompilerBuilder().addVar("my_var", SimpleType.STRING).build();

        val ast = compiler.compile("'hello ' + my_var").ast
        val program = runtime.createProgram(ast)

        val result = program.eval(mapOf(
            "my_var" to "World",
        ))

        println("Result: $result")
    }
}