package ro.holdone.swissborg.server.model

enum class Precision {
    P0,
    P1,
    P2,
    P3;

    val precisionDecimals: String
        get() = when (this) {
            P0 -> "$0.01"
            P1 -> "$0.10"
            P2 -> "$1"
            P3 -> "$10"
        }
}