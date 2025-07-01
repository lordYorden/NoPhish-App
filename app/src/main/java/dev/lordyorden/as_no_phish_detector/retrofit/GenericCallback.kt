package dev.lordyorden.as_no_phish_detector.retrofit

interface GenericCallback<T> {
    fun success(data: T?)

    fun error(error: String?)
}