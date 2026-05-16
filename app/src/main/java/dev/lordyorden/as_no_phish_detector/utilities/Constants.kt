package dev.lordyorden.as_no_phish_detector.utilities

class Constants {
    object RestAPI{
        const val BASE_URL = "http://localhost:9000"
    }

    object Perms {
        const val POST_NOTIFICATION_CODE = 42
        const val READ_NOTIFICATION_CODE = 43
        const val READ_SMS_CODE = 44
    }

    object Onboarding{
        const val ACTION_GENERATE = "GENERATE"
    }

    object OTP {
        const val OTP_LENGTH = 6
        const val OTP_SECRET = "test"
        const val AUTO_GENERATE_SECRET = true //false for better testing
        const val TEST_VALUE = "888888"
        const val OWNER_OTP = "999999"
    }

    object Circle {
        const val CIRCLE_TEMP_ID = "jn7e7gmjn4ztprcbff91grwznx84wkad"
        const val CIRCLE_CODE_KEY = "JoinCode"
        const val CIRCLE_ID_KEY = "circleId"
    }

    object UploadScheduler {
        const val TTL_MILLIS = 5 * 60 * 1000L
        const val RETRY_INTERVAL_MILLIS = 5_000L
    }

    object HistoryPagination {
        const val PAGE_SIZE = 10
    }
}