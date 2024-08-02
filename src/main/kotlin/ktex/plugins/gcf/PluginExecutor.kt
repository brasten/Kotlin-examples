package ktex.plugins.gcf

import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.GenericUrl
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import com.google.auth.oauth2.IdTokenCredentials
import com.google.auth.oauth2.IdTokenProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

const val pluginServiceBase = "https://us-west1-new-workflow-project.cloudfunctions.net"

class PluginExecutor(
    val baseUrl: String = pluginServiceBase,
) {

    suspend fun executePlugin(
        pluginName: String,
        reqBody: ReqBody,
    ): String = coroutineScope {
        val serviceUrl = "$baseUrl/$pluginName"
        val audience = serviceUrl

        val credential = GoogleCredentials.getApplicationDefault()
        if (credential !is IdTokenProvider) { throw AssertionError("Expected a IdTokenProvider") }

        val tokenCredential = IdTokenCredentials
            .newBuilder()
            .setIdTokenProvider(credential)
            .setTargetAudience(audience)
            .build()

        val genericUrl = GenericUrl(serviceUrl)
        val adapter = HttpCredentialsAdapter(tokenCredential)
        val transport: HttpTransport = NetHttpTransport()

        val request = transport
            .createRequestFactory(adapter)
            .buildPostRequest(
                genericUrl,
                ByteArrayContent.fromString("application/json", Json.encodeToString(reqBody)),
            )

        async(Dispatchers.IO) { request.execute() }
            .await()
            .content
            .readAllBytes()
            .toString(Charsets.UTF_8)
    }

}

@Serializable
data class ReqBody(val name: String)
