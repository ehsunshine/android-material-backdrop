package ninja.candroid.backdrop

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable

class BackdropStateDrawable(context: Context) : DrawerArrowDrawable(context) {

    var shape = Shape.HAMBURGER_ARROW

    private val path = Path()

    override fun draw(canvas: Canvas) {
        when (shape) {
            Shape.HAMBURGER_ARROW -> super.draw(canvas)
            Shape.HAMBURGER_CLOSE -> {
                if (progress < 0.5) {
                    drawHamburger(canvas)
                } else {
                    drawCross(canvas)
                }
            }
            Shape.ARROW_CLOSE -> if (progress < 0.5) {
                drawArrow(canvas)
            } else {
                drawCross(canvas)
            }
        }
    }

    private fun drawHamburger(canvas: Canvas) {
        val centerY = bounds.exactCenterY()
        val centerX = bounds.exactCenterX()
        val left = bounds.left.toFloat() + barThickness
        val right = bounds.right.toFloat() - barThickness
        val offsetY = (gapSize + barThickness) * (2 * (0.5f - progress))

        path.rewind()

        path.moveTo(lerp(left, centerX, progress * 2), centerY)
        path.lineTo(lerp(right, centerX, progress * 2), centerY)

        path.moveTo(left, centerY - offsetY)
        path.lineTo(right, centerY - offsetY)

        path.moveTo(left, centerY + offsetY)
        path.lineTo(right, centerY + offsetY)

        drawOnCanvas(canvas)
    }

    private fun drawCross(canvas: Canvas) {
        val centerX = bounds.exactCenterX()
        val centerY = bounds.exactCenterY()
        val crossHeight = gapSize * 2 + barThickness * 3

        val distanceY = (crossHeight / 2) * (2 * (progress - 0.5f))
        val top = centerY - distanceY
        val bottom = centerY + distanceY
        val left = centerX - crossHeight / 2
        val right = centerX + crossHeight / 2

        path.rewind()

        path.moveTo(left, top)
        path.lineTo(right, bottom)

        path.moveTo(left, bottom)
        path.lineTo(right, top)

        drawOnCanvas(canvas)
    }

    private fun drawArrow(canvas: Canvas) {
        val centerX = bounds.exactCenterX()
        val centerY = bounds.exactCenterY()
        val crossHeight = gapSize * 2 + barThickness * 3

        val distanceY = (crossHeight / 2) * (2 * (progress - 0.5f))
        val top = centerY - distanceY
        val bottom = centerY + distanceY
        val left = centerX - crossHeight / 2
        val right = centerX + crossHeight / 2

        path.rewind()

        path.moveTo(left, centerY)
        path.lineTo(lerp(right, centerX, progress * 2), centerY)


        path.moveTo(left, centerY)
        path.lineTo(lerp(centerX, right, progress * 2), top)

        path.moveTo(left, centerY)
        path.lineTo(lerp(centerX, right, progress * 2), bottom)

        drawOnCanvas(canvas)
    }

    private fun drawOnCanvas(canvas: Canvas) {
        path.close()
        canvas.save()

        canvas.drawPath(path, paint)
        canvas.restore()
    }

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

    enum class Shape {
        HAMBURGER_ARROW, HAMBURGER_CLOSE, ARROW_CLOSE
    }
}
