package ro.holdone.swissborg.server.model

sealed class BookEvent(val channelId: String) {
    class Update(channelId: String, val entry: BookEntry): BookEvent(channelId) {
        override fun toString(): String {
            return "Update(channelId=${channelId} entry=$entry)"
        }
    }
    class Snapshot(channelId: String, val updates: List<BookEntry>): BookEvent(channelId)
}