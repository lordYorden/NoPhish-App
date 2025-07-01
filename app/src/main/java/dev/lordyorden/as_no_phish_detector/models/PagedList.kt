package dev.lordyorden.as_no_phish_detector.models

data class PagedList<T>(
    var items: List<T>,
    var total: Int,
    var page: Int,
    var size: Int,
    var pages: Int
){
    //for java bean
    constructor(): this(listOf(), 0,0,0,0,)
}
