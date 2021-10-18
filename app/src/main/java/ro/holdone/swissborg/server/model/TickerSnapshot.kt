package ro.holdone.swissborg.server.model

data class TickerSnapshot(
    val channelId: String,
    val bid: Float,
    val bidSize: Float,
    val ask: Float,
    val askSize: Float,
    val dailyChange: Float,
    val dailyChangePerc: Float,
    val lastPrice: Float,
    val volume: Float,
    val high: Float,
    val low: Float
)
