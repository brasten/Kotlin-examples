package ktex.pagination

data class SequencePage<T>(
    val page: Iterable<T>,
    val nextPageToken: Object? = null,
)

fun interface PageFetcher<T> {
    fun fetch(pageToken: Object?): SequencePage<T>
}

class Sequencer<T>(
    val pageFetcher: PageFetcher<T>,
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
}