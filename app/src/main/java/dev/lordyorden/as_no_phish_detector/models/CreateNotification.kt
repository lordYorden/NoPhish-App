package dev.lordyorden.as_no_phish_detector.models

data class CreateNotification(
    var title: String,
    var extraTitle: String?,
    var isGroup: Boolean?,
    var body: String?,
    var packageName: String?,
    var timestamp: Long? = 0L
){
    //for java bean
    constructor(): this("", "", false,"", "", 0L)
}
