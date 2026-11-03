package com.avioiva.financiiea.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.avioiva.financiiea.AppContainer
import com.avioiva.financiiea.R
import com.avioiva.financiiea.ui.screens.AboutScreen
import com.avioiva.financiiea.ui.screens.AddEditScreen
import com.avioiva.financiiea.ui.screens.HomeScreen
import com.avioiva.financiiea.ui.screens.PinLockScreen
import com.avioiva.financiiea.ui.screens.RecordsScreen
import com.avioiva.financiiea.ui.screens.SettingsScreen
import com.avioiva.financiiea.ui.screens.SplashContent
import com.avioiva.financiiea.ui.theme.BrandGold
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object Routes {
    const val HOME = "home"
    const val ADD_EDIT = "addEdit?txId={txId}"
    const val RECORDS = "records"
    const val SETTINGS = "settings"
    const val ABOUT = "about"

    fun addEdit(txId: Long = -1L) = "addEdit?txId=$txId"
}

@Composable
fun AppRoot() {
    val settingsState by AppContainer.settings.state.collectAsState(initial = null)
    var splashDone by remember { mutableStateOf(false) }
    var unlocked by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(1500)
        splashDone = true
    }

    when {
        settingsState == null || !splashDone -> SplashContent()
        settingsState!!.pinHash != null && !unlocked ->
            PinLockScreen(onUnlocked = { unlocked = true })
        else -> MainShell()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainShell() {
    val nav = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStackEntry by nav.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route ?: Routes.HOME
    val isHome = route == Routes.HOME

    val titleRes = when (route) {
        Routes.RECORDS -> R.string.menu_history_reports
        Routes.SETTINGS -> R.string.title_settings
        Routes.ABOUT -> R.string.title_about
        else -> R.string.title_add_transaction
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (isHome) {
                HomeTopBar(onMenu = { scope.launch { drawerState.open() } })
            } else {
                SimpleTopBar(stringResource(titleRes), onBack = { nav.popBackStack() })
            }
        },
        floatingActionButton = {
            if (isHome) {
                FloatingActionButton(
                    onClick = { nav.navigate(Routes.addEdit()) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.menu_add_transaction))
                }
            }
        }
    ) { innerPadding ->
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                AppDrawer(
                    currentRoute = route,
                    onNavigate = { target ->
                        scope.launch { drawerState.close() }
                        if (target != route) {
                            nav.navigate(target) {
                                popUpTo(Routes.HOME) { inclusive = false }
                                launchSingleTop = true
                            }
                        }
                    },
                    onAddTransaction = {
                        scope.launch { drawerState.close() }
                        nav.navigate(Routes.addEdit())
                    }
                )
            }
        ) {
            NavHost(
                navController = nav,
                startDestination = Routes.HOME,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                composable(Routes.HOME) {
                    HomeScreen(
                        onOpenTransaction = { id -> nav.navigate(Routes.addEdit(id)) }
                    )
                }
                composable(
                    Routes.ADD_EDIT,
                    arguments = listOf(navArgument("txId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    })
                ) { entry ->
                    AddEditScreen(
                        txId = entry.arguments?.getLong("txId") ?: -1L,
                        onDone = { nav.popBackStack() }
                    )
                }
                composable(Routes.RECORDS) {
                    RecordsScreen(
                        onEditTransaction = { id -> nav.navigate(Routes.addEdit(id)) }
                    )
                }
                composable(Routes.SETTINGS) { SettingsScreen() }
                composable(Routes.ABOUT) { AboutScreen() }
            }
        }
    }
}

/** Narrow brand title bar: ☰  FINANCIIEA  *Your hassle-free money diary* */
@Composable
private fun HomeTopBar(onMenu: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 3.dp) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .height(54.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onMenu) {
                Icon(
                    Icons.Rounded.Menu,
                    contentDescription = stringResource(R.string.open_menu),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                "FINANCIIEA",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(Modifier.width(10.dp))
            Text(
                stringResource(R.string.tagline),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Medium
                ),
                color = BrandGold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SimpleTopBar(title: String, onBack: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 3.dp) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .height(54.dp)
                .padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                title,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppDrawer(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onAddTransaction: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Brand header with the shield logo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        RoundedCornerShape(18.dp)
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(R.drawable.splash_logo),
                        contentDescription = "FINANCIIEA logo",
                        modifier = Modifier.size(84.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "FINANCIIEA",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Text(
                        stringResource(R.string.menu_header_slogan),
                        style = MaterialTheme.typography.labelSmall,
                        fontStyle = FontStyle.Italic,
                        color = BrandGold
                    )
                }
            }
            Spacer(Modifier.height(18.dp))

            val items = listOf(
                Triple("add", stringResource(R.string.menu_add_transaction), Icons.Rounded.Add),
                Triple(Routes.RECORDS, stringResource(R.string.menu_history_reports), Icons.Rounded.History),
                Triple(Routes.SETTINGS, stringResource(R.string.menu_settings), Icons.Rounded.Settings),
                Triple(Routes.ABOUT, stringResource(R.string.menu_about), Icons.Rounded.Info)
            )
            items.forEach { (target, label, icon) ->
                NavigationDrawerItem(
                    label = { Text(label, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(icon, contentDescription = null) },
                    selected = target == currentRoute && target != "add",
                    onClick = { if (target == "add") onAddTransaction() else onNavigate(target) },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.about_made),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}
