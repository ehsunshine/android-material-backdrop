package ninja.candroid.backdrop

import android.animation.ObjectAnimator
import android.animation.StateListAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.animation.AccelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.ColorInt
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

    private var navController: NavController? = null
    private val animateHeight: Int
    private var backdropShown = false
    private var navigationMenuId: Int = View.generateViewId()
    private val navigationIcon = BackdropStateDrawable(context)
    private val backLayer = FrameLayout(context)
    private val frontLayer = MaterialCardView(context)
    private var subHeader = View(context)

    var showSubHeader: Boolean = true

    private val topLevelDestination: Boolean
        get() = topLevelDestinations.isNotEmpty() &&
                !topLevelDestinations.contains(navController?.currentDestination?.id)

    init {
        val displayMetrics = DisplayMetrics()
        (context as Activity).windowManager.defaultDisplay.getMetrics(displayMetrics)
        animateHeight = displayMetrics.heightPixels
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        addFrontLayer()
        addBackLayer()

        this.requestLayout()

        navController = findNavController(this)

        toolbar = toolbar ?: findToolbar()

        setAppBarElevationToZero()

        toolbar?.setNavigationOnClickListener {
            if (!backdropShown && topLevelDestination) {
                navController?.navigateUp()
            } else {
                backdropShown = !backdropShown

                if (backdropShown) {
                    addBackLayer { backViewHeight ->
                        slide(backViewHeight)
                    }
                } else {
                    slide(0)
                }

                fadeBackLayer(if (backdropShown) FadeType.IN else FadeType.OUT)
            }
        }
    }

    override fun onViewRemoved(child: View?) {
        super.onViewRemoved(child)
        toolbar = null
    }

    @JvmOverloads
    fun open(view: View? = null, animation: Boolean = true) {
        backdropShown = true
        addBackLayer(view) { backViewHeight ->
            slide(backViewHeight, animation)
        }
    }

    private fun addBackLayer() {
        if (backLayer.parent == null) {

            backLayer.layoutParams = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            backLayer.id = View.generateViewId()

            (backLayer.layoutParams as? CoordinatorLayout.LayoutParams)?.behavior =
                ScrollingViewBehavior()

            this.addView(backLayer, 0)
        }
    }

    private fun addFrontLayer() {
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

            if (showSubHeader) addSubHeader()

            children.forEach {
                this.removeView(it)
                frontLayer.addView(it)
            }

            this.addView(frontLayer, 0)
            frontLayer.requestLayout()
        }
    }

    private fun addSubHeader() {
        if (backLayer.parent == null) {
            subHeader = View.inflate(context, R.layout.backdrop_sub_header, null)
            frontLayer.addView(subHeader)
        }
    }


    private fun slide(position: Int, animation: Boolean = true) {
        ObjectAnimator.ofFloat(frontLayer, "translationY", position.toFloat())
            .apply {
                removeAllListeners()
                end()
                cancel()
                duration = ANIMATION_SPEED
                addListener(onSlideAnimationEnded())
                if (animation) start()
            }
        updateIcon(animation)
    }

    private fun setAppBarElevationToZero() {
        val stateListAnimator = StateListAnimator()
        stateListAnimator.addState(IntArray(0), ObjectAnimator.ofFloat(toolbar, "elevation", 0f))
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

    private fun updateIcon(animation: Boolean) {
        toolbar?.navigationIcon = navigationIcon
        if (navigationIcon.progress == 1f && backdropShown) return
        navigationIcon.shape = if (topLevelDestination) {
            BackdropStateDrawable.Shape.ARROW_CLOSE
        } else {
           BackdropStateDrawable.Shape.HAMBURGER_CLOSE
        }

        val (start, end) = if (backdropShown) 0F to 1F else 1F to 0F
        ValueAnimator.ofFloat(start, end).apply {
            duration = NAV_ICON_ANIMATION_SPEED
            addUpdateListener {
                navigationIcon.progress = it.animatedValue.toString().toFloat()
            }
            if (animation) start() else navigationIcon.progress = end
        }
    }

    private fun addBackLayer(view: View? = null, onHeightMeasured: (height: Int) -> Unit) {
        backLayer.clearAnimation()
        backLayer.removeAllViews()
        val backLayerChild = view
            ?: MaterialTextView(context).apply {
                id = navigationMenuId
                text = "Navigation menu items goes here!"
                val params = LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    400
                )
                val margin = 44
                params.setMargins(margin, margin, margin, margin)
                layoutParams = params
            }

        backLayer.addView(backLayerChild, 0)
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
        private const val NAV_ICON_ANIMATION_SPEED = 300L
    }

    private enum class FadeType { IN, OUT }

    private val Int.dp: Int
        get() = (this / Resources.getSystem().displayMetrics.density).toInt()
    private val Int.px: Int
        get() = (this * Resources.getSystem().displayMetrics.density).toInt()
}
