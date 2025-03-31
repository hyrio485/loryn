package top.loryn.support

data class PaginationParams(
    val currentPage: Int,
    val pageSize: Int,
) {
    init {
        require(currentPage > 0) { "currentPage must be greater than 0" }
        require(pageSize > 0) { "pageSize must be greater than 0" }
    }
}
