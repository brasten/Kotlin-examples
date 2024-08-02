package ktex.pagination

class PaginatedIterable<T>(
    val pageFetcher: Fetcher<T>,
): Iterable<T> {
    override fun iterator(): Iterator<T> = iterator<T> {
        var pageToken: Object? = null

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

    fun interface Fetcher<T> {
        fun fetch(pageToken: Object?): Page<T>
    }

    data class Page<T>(
        val page: Iterable<T>,
        val nextPageToken: Object? = null,
    )
}