package ktex.pagination

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.csv.CsvSchema

@JsonIgnoreProperties(ignoreUnknown = true)
data class Vehicle(
    @JsonProperty("DOL Vehicle ID") val vehicleId: Long,
    @JsonProperty("VIN (1-10)") val vin: String,
    @JsonProperty("County") val county: String,
    @JsonProperty("City") val city: String,
    @JsonProperty("State") val state: String,
    @JsonProperty("Postal Code") val postalCode: String,
    @JsonProperty("Model Year") val modelYear: Int,
    @JsonProperty("Make") val make: String,
    @JsonProperty("Model") val model: String,
)
// VIN (1-10),County,City,State,Postal Code,Model Year,Make,Model,Electric Vehicle Type,Clean Alternative Fuel Vehicle (CAFV) Eligibility,Electric Range,Base MSRP,Legislative District,DOL Vehicle ID,Vehicle Location,Electric Utility,2020 Census Tract

const val PAGE_SIZE = 1000

fun dataStoreFromResource(): List<Vehicle> {
    val csvUri = PaginatedService::class.java.getResource("/Electric_Vehicle_Population_Data.csv")
    val mapper = CsvMapper()

    return mapper
        .readerFor(Vehicle::class.java)
        .with(CsvSchema.emptySchema().withHeader())
        .readValues<Vehicle>(csvUri)
        .asSequence()
        .toList()
}

val defaultDataStore = dataStoreFromResource()

/**
 * Represents a low level accessor or web client with a paginated
 * interface. We're going to want to hide this pagination complexity
 * from callers.
 */
class PaginatedService(
    val dataStore: List<Vehicle> = defaultDataStore,
    val pageSize: Int = PAGE_SIZE,
    val poisonPillVin: String? = null,
) {
    fun getVehicles(
        pageToken: String? = null
    ): GetVehicleResponse {
        val index = dataStore.indexOfFirst { it.vehicleId == pageToken?.toLong()  }
        val cursor = index + 1

        val page = dataStore.subList(cursor, minOf(dataStore.size, cursor + pageSize))
        println("Cursor: ${cursor}")
        println("getVehicles() Page Size: ${page.size}")

        // NOTE: We're putting a poison pill in here about 660 records into the data. This should demonstrate
        // if callers iterate further than we're expecting.
        //
        if (poisonPillVin != null) {
            page.forEach { check(it.vin != poisonPillVin) }
        }

        return GetVehicleResponse(
            data = page,
            nextPageToken = page.lastOrNull()?.vehicleId?.toString() ?: null,
        )
    }

    data class GetVehicleResponse(
        val data: Collection<Vehicle>,
        val nextPageToken: String? = null,
    )
}

