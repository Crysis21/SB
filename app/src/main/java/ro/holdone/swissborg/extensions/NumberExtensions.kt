package ro.holdone.swissborg.extensions

import kotlin.math.pow
import kotlin.math.roundToInt

fun Float.roundTo(numFractionDigits: Int): Float {
    val factor = 10.0.toFloat().pow(numFractionDigits.toFloat())
    return (this * factor).roundToInt() / factor
}