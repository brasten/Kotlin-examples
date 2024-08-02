package ktex.expr.functions

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import ktex.plugins.gcf.PluginExecutor
import ktex.plugins.gcf.ReqBody
import org.junit.jupiter.api.Test
import kotlin.test.Ignore

class CloudFunctionsTest {
    val executor = PluginExecutor()

    @Test
    @Ignore("This test calls Google Cloud and costs money.")
    fun `testing using cloud functions`(): Unit = runTest {
        val numOfInvocations = 50
        val reqBody = ReqBody(name = "Brasten")

        val results = (0 until numOfInvocations).map { x ->
            async(Dispatchers.Default) {
                val resp = executor.executePlugin("test-function-2", reqBody)

                "$x :: RESP -> $resp"
            }
        }.awaitAll()

        results.size.shouldBe(numOfInvocations)
        results.shouldContain("0 :: RESP -> Hello, Brasten!")
        results.shouldContain("${numOfInvocations-1} :: RESP -> Hello, Brasten!")
    }
}

/**
 * Source of Cloud Function implementation used for testing:
 *
 * ```go
 *   package helloworld
 *
 *   import (
 *     "encoding/json"
 *     "fmt"
 *     "html"
 *     "net/http"
 *
 *     "github.com/GoogleCloudPlatform/functions-framework-go/functions"
 *   )
 *
 *   func init() {
 *      functions.HTTP("ProcessRule", processRule)
 *   }
 *
 *   // helloHTTP is an HTTP Cloud Function with a request parameter.
 *   func processRule(w http.ResponseWriter, r *http.Request) {
 *     var d struct {
 *       Name string `json:"name"`
 *     }
 *     if err := json.NewDecoder(r.Body).Decode(&d); err != nil {
 *       fmt.Fprint(w, "Hello, World!")
 *       return
 *     }
 *     if d.Name == "" {
 *       fmt.Fprint(w, "Hello, World!")
 *       return
 *     }
 *     fmt.Fprintf(w, "Hello, %s!", html.EscapeString(d.Name))
 *   }
 * ```
 */