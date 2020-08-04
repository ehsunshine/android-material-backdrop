package ninja.candroid.backdrop.extensions

import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI

internal var toolbar: Toolbar? = null

/**
 * Sets up a [Toolbar] for use with a [NavController].
 *
 * By calling this method, the title in the Toolbar will automatically be updated when
 * the destination changes (assuming there is a valid
 * [label][androidx.navigation.NavDestination.getLabel]).
 *
 * The start destination of your navigation graph is considered the only top level
 * destination. On the start destination of your navigation graph, the Toolbar will show
 * the drawer icon if the given `drawerLayout` is non null. On all other destinations,
 * the Toolbar will show the Up button.
 *
 * This method will call [NavController.navigateUp] when the navigation icon is clicked.
 *
 * @param navController The NavController whose navigation actions will be reflected
 *                      in the title of the Toolbar.
 * @param drawerLayout The DrawerLayout that should be toggled from the Navigation button
 */
fun Toolbar.setupWithNavController(
    navController: NavController,
    drawerLayout: DrawerLayout?
) {
    val configuration = AppBarConfiguration(navController.graph, drawerLayout)
    toolbar = this
    NavigationUI.setupWithNavController(this, navController, configuration)
    topLevelDestinations.addAll(configuration.topLevelDestinations)
}

/**
 * Sets up a [Toolbar] for use with a [NavController].
 *
 * By calling this method, the title in the Toolbar will automatically be updated when
 * the destination changes (assuming there is a valid
 * [label][androidx.navigation.NavDestination.getLabel]).
 *
 * The [AppBarConfiguration] you provide controls how the Navigation button is
 * displayed and what action is triggered when the Navigation button is tapped.
 *
 * This method will call [NavController.navigateUp] when the navigation icon is clicked.
 *
 * @param navController The NavController whose navigation actions will be reflected
 *                      in the title of the Toolbar.
 * @param configuration Additional configuration options for customizing the behavior of the
 *                      Toolbar
 */
fun Toolbar.setupWithNavController(
    navController: NavController,
    configuration: AppBarConfiguration = AppBarConfiguration(navController.graph)
) {
    toolbar = this
    topLevelDestinations.addAll(configuration.topLevelDestinations)
    NavigationUI.setupWithNavController(this, navController, configuration)
}
