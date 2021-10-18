package ro.holdone.swissborg.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import ro.holdone.swissborg.R

class VolumeIndicatorView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class Origin(val value: Int) {
        start(0),
        end(1);

        companion object {
            fun fromValue(value: Int): Origin {
                return values().first { it.value == value }
            }
        }
    }

    private val paint = Paint().also {
        it.isAntiAlias = true
        it.color = Color.RED
        it.style = Paint.Style.FILL
    }

    private var drawingRect = RectF()

    var progress: Float = 0.5f
        set(value) {
            field = value
            invalidate()
        }

    var progressColor: Int = Color.RED
        set(value) {
            field = value
            paint.color = value
            invalidate()
        }

    var origin = Origin.start
        set(value) {
            field = value
            invalidate()
        }

    init {
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.VolumeIndicatorView)
        progress = attributes.getFloat(R.styleable.VolumeIndicatorView_progress, progress)
        progressColor =
            attributes.getColor(R.styleable.VolumeIndicatorView_progressColor, Color.LTGRAY)
        origin = Origin.fromValue(
            attributes.getInt(
                R.styleable.VolumeIndicatorView_origin,
                Origin.start.value
            )
        )
        attributes.recycle()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        drawingRect.set(
            if (origin == Origin.start) 0F else w - progress * w,
            0F,
            if (origin == Origin.start) progress * w else w.toFloat(),
            h.toFloat()
        )
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.drawRoundRect(drawingRect, 10f, 10f, paint)
    }
}