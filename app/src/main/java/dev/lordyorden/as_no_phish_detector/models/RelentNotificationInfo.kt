package dev.lordyorden.as_no_phish_detector.models

data class RelentNotificationInfo(
    var body: String?,
    var packageName: String?,
    var hash: String?,
    var urls: List<String> = listOf()
){
    //for java bean
    constructor(): this("", "", "")
}
