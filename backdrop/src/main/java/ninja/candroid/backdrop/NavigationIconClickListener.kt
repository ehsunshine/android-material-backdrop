package ninja.candroid.backdrop

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.animation.Interpolator
import android.widget.ImageView
import androidx.core.animation.addListener
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.navigation.NavController
import ninja.candroid.backdrop.extensions.topLevelDestinations

class NavigationIconClickListener @JvmOverloads internal constructor(
    context: Context,
    private val navController: NavController,
    private val sheet: View,
    private val interpolator: Interpolator? = null,
    private val openIcon: Drawable? = null,
    private val closeIcon: Drawable? = null
) : View.OnClickListener {

    private val animatorSet = AnimatorSet()
    private val height: Int
    private var backdropShown = false

    init {
        val displayMetrics = DisplayMetrics()
        (context as Activity).windowManager.defaultDisplay.getMetrics(displayMetrics)
        height = displayMetrics.heightPixels
    }

    override fun onClick(view: View) {
        if (!topLevelDestinations.contains(navController.currentDestination?.id)) {
            navController.navigateUp()
        } else {
            backdropShown = !backdropShown

            // Cancel the existing animations
            animatorSet.removeAllListeners()
            animatorSet.end()
            animatorSet.cancel()

            updateIcon(view)

            val translateY = height - 200

            val animator = ObjectAnimator.ofFloat(
                sheet,
                "translationY",
                (if (backdropShown) translateY else 0).toFloat()
            )
            animator.addListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {


                }

                override fun onAnimationEnd(animation: Animator?) {

                }

                override fun onAnimationCancel(animation: Animator?) {

                }

                override fun onAnimationStart(animation: Animator?) {

                }

            })
            animator.duration = 500
            if (interpolator != null) {
                animator.interpolator = interpolator
            }
            animatorSet.play(animator)
            animator.start()

        }
    }

    private fun updateIcon(view: View) {
        if (openIcon != null && closeIcon != null) {
            if (view !is ImageView) {
                throw IllegalArgumentException("updateIcon() must be called on an ImageView")
            }
            if (backdropShown) {
                view.setImageDrawable(closeIcon)
            } else {
                view.setImageDrawable(openIcon)
            }
        }
    }
}
