package dev.lordyorden.as_no_phish_detector.models

data class SmsMessage(
    var id: String,
    var phone_number: String,
    var body: String?,
    var timestamp: Long? = 0L
){
    //for java bean
    constructor(): this("", "", "", 0L)
}
