package cuboc_core.cuboc.database.search

class SmartStringSearch(searchQuery: String) {
    val searchBoundaries: Pair<String, String>
    val normalisedQuery: String = searchQuery.lowercase().trim()

    init {
        val lastChar = searchQuery.last()
        val trimmedQuery = normalisedQuery.dropLast(1)
        searchBoundaries = trimmedQuery + (lastChar - 1) to trimmedQuery + (lastChar + 1)
    }

    fun isRelevant(string: String): Boolean {
        return string in searchBoundaries.first..searchBoundaries.second
    }
}