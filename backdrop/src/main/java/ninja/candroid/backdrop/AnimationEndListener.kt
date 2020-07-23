package ninja.candroid.backdrop

import android.animation.Animator

class AnimationEndListener(private val onAnimationEnded: ()-> Unit) : Animator.AnimatorListener {

    override fun onAnimationRepeat(animation: Animator?) {}

    override fun onAnimationEnd(animation: Animator?) {
        onAnimationEnded()
    }

    override fun onAnimationCancel(animation: Animator?) {}

    override fun onAnimationStart(animation: Animator?) {}
}