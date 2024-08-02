package ktex.pagination

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.lang.IllegalStateException
import kotlin.test.Test
import kotlin.test.assertEquals

@Execution(ExecutionMode.CONCURRENT)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class PaginatedIterableTest {
    val paginatedService = PaginatedService()

    fun buildPageFetcher(svc: PaginatedService = paginatedService) = PaginatedIterable.Fetcher<Vehicle> { pageToken ->
        val res = svc.getVehicles(pageToken?.toString())

        PaginatedIterable.Page(
            res.data,
            nextPageToken = res.nextPageToken as? Object
        )
    }

    @Test
    fun `paginates as needed to collect values`() {
        val seq = PaginatedIterable<Vehicle>(buildPageFetcher(paginatedService)).toSequence()

        val vehicleChunks =
            seq.chunked(150)
                .take(3)
                .toList()

        assertEquals(3, vehicleChunks.size)
        assertEquals(150, vehicleChunks[0].size)
        assertEquals(150, vehicleChunks[1].size)
        assertEquals(150, vehicleChunks[2].size)

        assertEquals(paginatedService.dataStore[0].vehicleId, vehicleChunks[0][0].vehicleId)
        assertEquals(paginatedService.dataStore[149].vehicleId, vehicleChunks[0][149].vehicleId)
        assertEquals(paginatedService.dataStore[150].vehicleId, vehicleChunks[1][0].vehicleId)
        assertEquals(paginatedService.dataStore[299].vehicleId, vehicleChunks[1][149].vehicleId)
        assertEquals(paginatedService.dataStore[300].vehicleId, vehicleChunks[2][0].vehicleId)
        assertEquals(paginatedService.dataStore[449].vehicleId, vehicleChunks[2][149].vehicleId)
    }

    @Test
    fun `paginates to end if needed`() {
        val dataStore = dataStoreFromResource()
            .take(4000)
            .toList()
        val seq = PaginatedIterable<Vehicle>(buildPageFetcher(PaginatedService(dataStore))).toSequence()

        val vehicleChunks =
            seq.chunked(1500)
                .take(4)
                .toList()

        assertEquals(3, vehicleChunks.size)
        assertEquals(1500, vehicleChunks[0].size)
        assertEquals(1500, vehicleChunks[1].size)
        assertEquals(1000, vehicleChunks[2].size)
    }

    @Nested
    inner class AsIterable {

    }

    /**
     * [service] hides the sequencer from the caller so that the caller
     * appears to be working with any normal service/accessor/etc returning
     * an iterable or sequence.
     */
    class TestService(val poisonPillVin: String? = null) {
        val ext = PaginatedService(poisonPillVin = poisonPillVin)
        fun getVehicles(): Sequence<Vehicle> =
            PaginatedIterable<Vehicle>(PaginatedIterable.Fetcher<Vehicle> { pageToken ->
                val result = ext.getVehicles(pageToken?.toString())

                PaginatedIterable.Page(result.data, result.nextPageToken as Object)
            }).toSequence()
    }
    val service = TestService("JTMFB3FV6M")

    @Nested
    inner class UsageExampleWhenBehindServiceCall {
        val vehicles = service.getVehicles()

        @Test
        fun `hits poison pill if converted to list`() {
            assertThrows<IllegalStateException> {
                vehicles.toList()
            }
        }

        @Test
        fun `doesn't hit poison pill if we only take a few hundrend entries`() {
            assertDoesNotThrow {
                vehicles.take(300).toList()
            }
        }

        @Test
        fun `doesn't hit poison pill when map and filter are used as long as we don't take enough`() {
            val result = assertDoesNotThrow {
                vehicles
                    .filter { it.city != "Everett" }
                    .map { it.copy(city = "Kirkland") }
                    .take(100)
                    .toList()
            }

            assertEquals(100, result.size)
        }

    }

    data class GetVehiclesResponse(
        val data: Collection<Vehicle>,
        val nextPageToken: String? = null
    )

    // This is just addition an additional layer of complexity by replacing the [PaginatedService] with
    // an HTTP-based service. The intent is to demonstrate easily iterating over a paginated endpoint.
    //
    // NOTE: At the moment this is fairly messy, but the hope would be to create a utility library to
    // make the endpoint pagination more seamless.
    //
    @Nested
    inner class SequencerWithWebService {
        val objectMapper = ObjectMapper().registerKotlinModule()

        // Create a MockWebServer. These are lean enough that you can create a new
        // instance for every unit test.
        val routeDispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when (request.requestUrl?.encodedPath) {
                    "/v1/vehicles" -> {
                        val nextPageToken = request.requestUrl?.queryParameter("page_token")
                        val vehiclePage = paginatedService.getVehicles(nextPageToken)
                        val vehiclesResponseBody = GetVehiclesResponse(vehiclePage.data, vehiclePage.nextPageToken)

                        MockResponse()
                            .setResponseCode(200)
                            .setBody(
                                objectMapper
                                    .writerFor(GetVehiclesResponse::class.java)
                                    .writeValueAsString(vehiclesResponseBody)
                            )
                    }
                    else -> {
                        println("DISPATCH FAILED : ${request}")
                        MockResponse().setResponseCode(404)
                    }
                }
            }
        }
        val server = MockWebServer().apply {
            dispatcher = routeDispatcher
            start()
        }
        val baseUrl = server.url("/")
        val client = OkHttpClient()

        val webServiceFetcher = PaginatedIterable.Fetcher<Vehicle> { pageToken ->
            val urlBuilder = baseUrl.newBuilder()
                .addPathSegments("v1/vehicles")
            if (pageToken is String) {
                urlBuilder.addQueryParameter("page_token", pageToken)
            }
            val url = urlBuilder.build().toString()

            val req = Request.Builder()
                .url(url)
                .build();

            val resp = client.newCall(req).execute()
            val body = resp.body?.byteStream()?.readAllBytes()?.toString(Charsets.UTF_8) ?: "{}"

            val responseBody = objectMapper
                .readerFor(PaginatedService.GetVehicleResponse::class.java)
                .readValue<PaginatedService.GetVehicleResponse>(body)!!

            val nextPageToken = responseBody.nextPageToken?.toString() ?: null

            PaginatedIterable.Page<Vehicle>(
                page = responseBody.data,
                nextPageToken = nextPageToken as Object?,
            )
        }

        @AfterEach
        fun tearDown() {
            server.shutdown()
        }

        @Test
        fun `sample sequencer using web service`() {
            val vehicles = PaginatedIterable<Vehicle>(webServiceFetcher).toSequence()

            val vehicleList = vehicles.take(500).toList()
            assertEquals(500, vehicleList.size)
        }
    }
}