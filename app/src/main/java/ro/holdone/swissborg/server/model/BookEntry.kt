package ro.holdone.swissborg.server.model

data class BookEntry(val price: Float, val count: Int, val amount: Float) {
    override fun toString(): String {
        return "BookEntry(price=$price, count=$count, amount=$amount)"
    }
}