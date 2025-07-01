package dev.lordyorden.as_no_phish_detector.models

data class Notification(
    var id: String,
    var title: String,
    var body: String?,
    var timestamp: Long? = 0L
){
    //for java bean
    constructor(): this("", "", "", 0L)
}
