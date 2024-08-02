package ktex.pagination

import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@Execution(ExecutionMode.CONCURRENT)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class PaginatedServiceTest {
    val service = PaginatedService()

    @Test
    fun `without pageToken - returns first page`() {
        val result = service.getVehicles()

        assertEquals(PAGE_SIZE, result.data.size)
        assertNotNull(result.nextPageToken)
    }

    @Test
    fun `using pageToken - can paginate entire dataset`() {
        var pageToken: String? = null
        val vehicles = mutableListOf<Vehicle>()

        // We're intentionally using a more verbose method of manually paginating so we can compare
        // with using the Paginator in Paginator tests.
        //
        do {
            val page = service.getVehicles(pageToken)

            if (page.nextPageToken == pageToken) {
                pageToken = null
            } else {
                pageToken = page.nextPageToken
                vehicles.addAll(page.data)
            }
        } while (pageToken != null)

        assertEquals(194232, vehicles.size)
    }
}