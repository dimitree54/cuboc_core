package cuboc_core.cuboc.database.search

data class SearchRequest(
    val query: String, val type: SearchType
)