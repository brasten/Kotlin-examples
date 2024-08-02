package ktex.pagination

/**
 * Converts a function for retrieving pages of data into an Iterable.
 */
class PaginatedIterable<T>(
    val pageFetcher: Fetcher<T>,
) {

    fun toSequence(): Sequence<T> {
        var pageToken: Object? = null

        return sequence {
            do {
                val fetchedPage = pageFetcher.fetch(pageToken)
                if (pageToken == fetchedPage.nextPageToken) {
                    pageToken = null
                } else {
                    pageToken = fetchedPage.nextPageToken
                    yieldAll(fetchedPage.page)
                }
            } while (pageToken != null)
        }
    }

    fun interface Fetcher<T> {
        fun fetch(pageToken: Object?): Page<T>
    }

    data class Page<T>(
        val page: Iterable<T>,
        val nextPageToken: Object? = null,
    )
}