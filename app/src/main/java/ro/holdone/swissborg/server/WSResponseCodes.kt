package ro.holdone.swissborg.server

object WSResponseCodes {
    //ERROR
    const val UNKNOWN_EVENT = 10000
    const val UNKNOWN_PAIR = 10001
    const val SUBSCRIPTION_FAILED = 10300
    const val ALREADY_SUBSCRIBED = 10301
    const val UNSUBSCRIPTION_FAILED = 10400
    const val NOT_SUBSCRIBED = 10401

    //INFO
    const val RESTART_SERVER = 20051
    const val DATA_REFRESH_IN_PROGRESS = 20060
    const val DATA_REFRESH_COMPLETE = 20061


}