package ninja.candroid.backdrop

import android.animation.ObjectAnimator
import android.animation.StateListAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.Color
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
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.children
import androidx.navigation.NavController
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.ScrollingViewBehavior
import com.google.android.material.card.MaterialCardView
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.textview.MaterialTextView
import ninja.candroid.backdrop.extensions.toolbar
import ninja.candroid.backdrop.extensions.topLevelDestinations
import ninja.candroid.backdrop.utils.ViewFinderUtils

class MaterialBackdrop @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val viewFinderUtils = ViewFinderUtils()
    private var navController: NavController? = null
    private val screenHeight: Int
    private var backdropShown = false
    private var navigationMenuId: Int = View.generateViewId()
    private val scrim = MaterialCardView(context)
    private val navigationIcon = BackdropStateDrawable(context)
    private val backLayer = FrameLayout(context)
    private val frontLayer = MaterialCardView(context)
    private var subHeader = View(context)
    private var frontLayerShape: ShapeAppearanceModel

    var subHeaderIcon: Drawable? = context.getDrawable(R.drawable.backdrop_ic_up)
        set(value) {
            if (!showSubHeaderIcon) showSubHeaderIcon = true
            field = value
        }
    var showSubHeader: Boolean = true
        set(value) {
            subHeader.visibility = if (value) View.VISIBLE else View.GONE
            field = value
        }
    var showSubHeaderIcon: Boolean = true
        set(value) {
            subHeader.apply {
                findViewById<ImageView>(R.id.backdropSubHeaderIconImageView)?.visibility =
                    if (value) View.VISIBLE else View.GONE
            }
            field = value
        }

    // TODO: Get from ShapeAppearance.MaterialComponents.LargeComponent
    var cornerFamily: Int = CornerFamily.ROUNDED
    var cornerSize: Float = 44F

    var subHeaderTitle: String? = ""
        set(value) {
            if (!showSubHeader) showSubHeader = true
            subHeader.apply {
                findViewById<TextView>(R.id.backdropSubHeaderTitleTextView)?.text = value
            }
            field = value
        }

    private val topLevelDestination: Boolean
        get() = topLevelDestinations.isNotEmpty() &&
                !topLevelDestinations.contains(navController?.currentDestination?.id)

    init {
        val displayMetrics = DisplayMetrics()
        (context as Activity).windowManager.defaultDisplay.getMetrics(displayMetrics)
        screenHeight = displayMetrics.heightPixels
        frontLayerShape = ShapeAppearanceModel()
            .toBuilder()
            .setTopLeftCorner(cornerFamily, cornerSize)
            .setTopRightCorner(cornerFamily, cornerSize)
            .build()
        addFrontLayer()
        addBackLayer()
        addSubHeader()

        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.MaterialBackdrop, 0, 0)
            subHeaderTitle = typedArray.getString(R.styleable.MaterialBackdrop_sub_header_title)
            showSubHeader =
                typedArray.getBoolean(R.styleable.MaterialBackdrop_show_sub_header, true)
            showSubHeaderIcon =
                typedArray.getBoolean(R.styleable.MaterialBackdrop_show_sub_header_icon, true)
            subHeaderIcon = typedArray.getDrawable(R.styleable.MaterialBackdrop_sub_header_icon)
                ?: subHeaderIcon
            typedArray.recycle()
        }

    }

    @JvmOverloads
    fun open(view: View? = null, animation: Boolean = true, fullOpen: Boolean = false) {
        backdropShown = true
        addBackLayerChild(view) { backViewHeight ->
            slide(backViewHeight, animation, fullOpen)
        }
    }

    @JvmOverloads
    fun close(animation: Boolean = true) {
        if (backdropShown) {
            backdropShown = false
            slide(0, animation)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        populateFrontLayer()

        this.requestLayout()

        navController = viewFinderUtils.findNavController(this)

        toolbar = toolbar ?: viewFinderUtils.findToolbar(this)

        setAppBarElevationToZero()

        toolbar?.setNavigationOnClickListener {
            if (!backdropShown && topLevelDestination) {
                navController?.navigateUp()
            } else {
                backdropShown = !backdropShown

                if (backdropShown) {
                    addBackLayerChild { backViewHeight ->
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

            frontLayer.shapeAppearanceModel = frontLayerShape

            this.addView(frontLayer, 0)
            frontLayer.requestLayout()
        }
    }

    private fun populateFrontLayer() {
        children.forEach {
            if (it != backLayer && it != frontLayer) {
                this.removeView(it)
                frontLayer.addView(it)
            }
        }
    }

    private fun addSubHeader() {
        subHeader = View.inflate(context, R.layout.backdrop_sub_header, null)
        frontLayer.addView(subHeader)
    }

    private fun slide(position: Int, animation: Boolean = true, fullOpen: Boolean = false) {
        val toolbarPosition = IntArray(2)
        val toolBarHeight = toolbar?.layoutParams?.height ?: 0
        applyScrim(fullOpen)
        updateSubHeaderIcon()
        toolbar?.getLocationOnScreen(toolbarPosition)
        val subHeaderHeight =
            context.resources.getDimensionPixelSize(R.dimen.backdrop_subheader_height)
        ObjectAnimator.ofFloat(
            frontLayer,
            "translationY",
            if (!fullOpen)
                position.toFloat()
            else (screenHeight - (subHeaderHeight + toolBarHeight + toolbarPosition[1])).toFloat()
        )
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

    private fun updateSubHeaderIcon() {
        val subHeaderImageView =
            subHeader.findViewById<ImageView>(R.id.backdropSubHeaderIconImageView)
        subHeaderImageView.setImageDrawable(subHeaderIcon)
        subHeaderImageView?.visibility =
            if (showSubHeaderIcon && backdropShown) View.VISIBLE else View.GONE
    }

    private fun applyScrim(fullOpen: Boolean) {
        frontLayer.removeView(scrim)

        if (backdropShown.not()) return

        scrim.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        scrim.shapeAppearanceModel = frontLayerShape

        scrim.setOnClickListener {
            if (backdropShown) {
                backdropShown = false
                slide(0)
            }
        }
        if (fullOpen.not()) {
            scrim.setCardBackgroundColor(context.getColor(R.color.backdrop_scrim_color))
        } else {
            scrim.setCardBackgroundColor(Color.TRANSPARENT)
        }
        scrim.elevation = 0f
        frontLayer.addView(scrim)
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

    private fun addBackLayerChild(view: View? = null, onHeightMeasured: (height: Int) -> Unit) {
        backLayer.clearAnimation()
        backLayer.removeAllViews()
        // TODO: Add the navigation menu here as the default back layer child
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

    companion object {
        private const val ANIMATION_SPEED = 150L
        private const val NAV_ICON_ANIMATION_SPEED = 300L
    }

    private enum class FadeType { IN, OUT }
}
