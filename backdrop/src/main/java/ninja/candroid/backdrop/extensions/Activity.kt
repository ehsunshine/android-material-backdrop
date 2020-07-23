package ninja.candroid.backdrop.extensions

import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI

/**
 * Sets up the ActionBar returned by [AppCompatActivity.getSupportActionBar] for use
 * with a [NavController].
 *
 * By calling this method, the title in the action bar will automatically be updated when
 * the destination changes (assuming there is a valid
 * [label][androidx.navigation.NavDestination.getLabel]).
 *
 * The start destination of your navigation graph is considered the only top level
 * destination. On the start destination of your navigation graph, the ActionBar will show
 * the drawer icon if the given `drawerLayout` is non null. On all other destinations,
 * the ActionBar will show the Up button.
 *
 * You are responsible for calling [NavController.navigateUp] to handle the Navigation button.
 * Typically this is done in [AppCompatActivity.onSupportNavigateUp].
 *
 * @param navController The NavController whose navigation actions will be reflected
 *                      in the title of the action bar.
 * @param drawerLayout The DrawerLayout that should be toggled from the Navigation button
 */
fun AppCompatActivity.setupActionBarWithNavController(
    navController: NavController,
    drawerLayout: DrawerLayout?
) {
    val configuration = AppBarConfiguration(navController.graph, drawerLayout)
    NavigationUI.setupActionBarWithNavController(this, navController, configuration)
    topLevelDestinations.addAll(configuration.topLevelDestinations)
}

/**
 * Sets up the ActionBar returned by [AppCompatActivity.getSupportActionBar] for use
 * with a [NavController].
 *
 * By calling this method, the title in the action bar will automatically be updated when
 * the destination changes (assuming there is a valid
 * [label][androidx.navigation.NavDestination.getLabel]).
 *
 * The [AppBarConfiguration] you provide controls how the Navigation button is
 * displayed.
 *
 * You are responsible for calling [NavController.navigateUp] to handle the Navigation button.
 * Typically this is done in [AppCompatActivity.onSupportNavigateUp].
 *
 * @param navController The NavController whose navigation actions will be reflected
 *                      in the title of the action bar.
 * @param configuration Additional configuration options for customizing the behavior of the
 *                      ActionBar
 */
fun AppCompatActivity.setupActionBarWithNavController(
    navController: NavController,
    configuration: AppBarConfiguration = AppBarConfiguration(navController.graph)
) {
    topLevelDestinations.addAll(configuration.topLevelDestinations)
    NavigationUI.setupActionBarWithNavController(this, navController, configuration)
}
