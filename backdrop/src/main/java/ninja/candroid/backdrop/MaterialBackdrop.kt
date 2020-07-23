package ninja.candroid.backdrop

import android.animation.ObjectAnimator
import android.animation.StateListAnimator
import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.animation.AccelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.children
import androidx.fragment.app.FragmentContainerView
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.ScrollingViewBehavior
import com.google.android.material.card.MaterialCardView
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.textview.MaterialTextView
import ninja.candroid.backdrop.extensions.toolbar
import ninja.candroid.backdrop.extensions.topLevelDestinations


class MaterialBackdrop @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val animateHeight: Int
    private var backdropShown = false
    private val openIcon: Drawable?
    private val closeIcon: Drawable?
    private var navigationMenuId: Int = View.generateViewId()
    private val view = MaterialTextView(context)
    private val backLayer = FrameLayout(context)
    private val frontLayer = MaterialCardView(context)

    init {
        val displayMetrics = DisplayMetrics()
        (context as Activity).windowManager.defaultDisplay.getMetrics(displayMetrics)
        animateHeight = displayMetrics.heightPixels
        openIcon = context.getDrawable(R.drawable.ic_menu)
        closeIcon = context.getDrawable(R.drawable.ic_close)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (frontLayer.parent == null) {
            frontLayer.layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
            frontLayer.id = View.generateViewId()
            frontLayer.cardElevation = 1F

            // TODO: Get from ShapeAppearance.MaterialComponents.LargeComponent
            frontLayer.shapeAppearanceModel =
                ShapeAppearanceModel()
                    .toBuilder()
                    .setTopLeftCorner(CornerFamily.ROUNDED, 64F)
                    .setTopRightCorner(CornerFamily.ROUNDED, 64F)
                    .build()

            frontLayer.requestLayout()

            children.forEach {
                this.removeView(it)
                frontLayer.addView(it)
            }

            this.addView(frontLayer, 0)
            frontLayer.requestLayout()
        }
        if (backLayer.parent == null) {

            backLayer.layoutParams = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            backLayer.id = View.generateViewId()
            backLayer.setBackgroundColor(R.attr.colorError)

            (backLayer.layoutParams as? CoordinatorLayout.LayoutParams)?.behavior =
                ScrollingViewBehavior()

            this.addView(backLayer, 0)
        }

        this.requestLayout()

        val navController = findNavController(this)

        toolbar = toolbar ?: findToolbar()

        setAppBarElevationToZero()

        toolbar?.setNavigationOnClickListener {
            if (topLevelDestinations.isNotEmpty() && !topLevelDestinations.contains(navController?.currentDestination?.id)) {
                navController?.navigateUp()
            } else {
                backdropShown = !backdropShown

                updateIcon(it)

                if (backdropShown) {
                    addBackLayer { backViewHeight ->
                        ObjectAnimator.ofFloat(frontLayer, "translationY", backViewHeight.toFloat())
                            .apply {
                                removeAllListeners()
                                end()
                                cancel()
                                duration = ANIMATION_SPEED
                                addListener(onSlideAnimationEnded())
                                start()
                            }
                    }
                } else {
                    ObjectAnimator.ofFloat(frontLayer, "translationY", 0F)
                        .apply {
                            removeAllListeners()
                            end()
                            cancel()
                            duration = ANIMATION_SPEED
                            addListener(onSlideAnimationEnded())
                            start()
                        }
                }

                fadeBackLayer(if (backdropShown) FadeType.IN else FadeType.OUT)

            }
        }
    }

    private fun setAppBarElevationToZero() {
        val stateListAnimator = StateListAnimator()
        stateListAnimator.addState(IntArray(0), ObjectAnimator.ofFloat(view, "elevation", 0f))
        (toolbar?.parent as? AppBarLayout)?.stateListAnimator = stateListAnimator
    }

    private fun onSlideAnimationEnded(): AnimationEndListener {
        return AnimationEndListener {
            if (backdropShown.not())
                backLayer.apply {
                    clearAnimation()
                    removeAllViews()
                }
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

    private fun addBackLayer(onHeightMeasured: (height: Int) -> Unit) {
        view.id = navigationMenuId
        view.text = "Hello Backdrop!"
        view.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 400)
        backLayer.addView(view, 0)
        backLayer.viewTreeObserver
            .addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    backLayer.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    onHeightMeasured(backLayer.measuredHeight)
                }
            })
    }

    private fun findNavController(view: ViewGroup): NavController? {
        if (view.childCount != 0) {
            view.children.forEach {
                if (it is FragmentContainerView) {
                    try {
                        return it.findNavController()
                    } catch (ex: IllegalStateException) {
                        (it as? ViewGroup)?.let { viewGroup ->
                            val navController = findNavController(viewGroup)
                            if (navController == null) return@forEach else return navController
                        }
                    }

                } else {
                    (it as? ViewGroup)?.let { viewGroup ->
                        val navController = findNavController(viewGroup)
                        if (navController == null) return@forEach else return navController
                    }
                }
            }
        }
        return null
    }

    private fun fadeBackLayer(fadeType: FadeType) {
        backLayer.clearAnimation()
        val fade = when (fadeType) {
            FadeType.IN ->
                AlphaAnimation(0f, 1f).apply {
                    interpolator = DecelerateInterpolator()
                }
            FadeType.OUT ->
                AlphaAnimation(1f, 0f).apply {
                    interpolator = AccelerateInterpolator()
                }
        }
        fade.duration = ANIMATION_SPEED

        backLayer.animation = AnimationSet(false).apply {
            addAnimation(fade)
        }
    }

    private fun findToolbar(): Toolbar? {

        fun findToolbarInChildren(view: ViewGroup): Toolbar? {
            if (view.childCount != 0) {
                view.children.forEach {
                    if (it is Toolbar) {
                        return it
                    } else (it as? ViewGroup)?.let { viewGroup ->
                        val toolbar = findToolbarInChildren(viewGroup)
                        if (toolbar == null) return@forEach else return toolbar
                    }
                }
            }
            return null
        }

        var view = parent as? ViewGroup
        do {
            view?.children?.forEach {
                if (it is Toolbar) return it else {
                    val toolbar = (it as? ViewGroup)?.let { viewGroup ->
                        findToolbarInChildren(viewGroup)
                    }
                    if (toolbar != null) return toolbar
                }
            }
            view = view?.parent as? ViewGroup

        } while (view?.parent != null)
        return null
    }

    companion object {
        private const val ANIMATION_SPEED = 150L
    }

    private enum class FadeType { IN, OUT }
}
