package ninja.candroid.backdrop.utils

import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.core.view.children
import androidx.fragment.app.FragmentContainerView
import androidx.navigation.NavController
import androidx.navigation.findNavController

internal class ViewFinderUtils {

    fun findNavController(view: ViewGroup): NavController? {
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

    fun findToolbar(view: View): Toolbar? {

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

        var parentView = view.parent as? ViewGroup
        do {
            parentView?.children?.forEach {
                if (it is Toolbar) return it else {
                    val toolbar = (it as? ViewGroup)?.let { viewGroup ->
                        findToolbarInChildren(viewGroup)
                    }
                    if (toolbar != null) return toolbar
                }
            }
            parentView = parentView?.parent as? ViewGroup

        } while (parentView?.parent != null)
        return null
    }
}