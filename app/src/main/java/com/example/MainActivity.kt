package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.RestaurantMenu
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.auth.GoogleAuthHelper
import com.example.data.model.OrderStatus
import com.example.data.repository.CafeteriaRepository
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CredencialSetupScreen
import com.example.ui.screens.CredentialProfileScreen
import com.example.ui.screens.MenuScreen
import com.example.ui.screens.OrderQrTicketScreen
import com.example.ui.screens.OrdersScreen
import com.example.ui.theme.Cafeteria134Theme
import com.example.ui.viewmodel.CafeteriaViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Cafeteria134Theme {
                CafeteriaAppRoot()
            }
        }
    }
}

private sealed class BottomTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    data object Menu : BottomTab(
        route = "menu",
        label = "Menú",
        selectedIcon = Icons.Rounded.RestaurantMenu,
        unselectedIcon = Icons.Outlined.RestaurantMenu,
        testTag = "nav_menu"
    )

    data object Orders : BottomTab(
        route = "orders",
        label = "Pedidos QR",
        selectedIcon = Icons.Rounded.QrCode2,
        unselectedIcon = Icons.Outlined.QrCode2,
        testTag = "nav_orders"
    )

    data object Credential : BottomTab(
        route = "credential",
        label = "Credencial",
        selectedIcon = Icons.Rounded.Badge,
        unselectedIcon = Icons.Outlined.Badge,
        testTag = "nav_credential"
    )
}

@Composable
fun CafeteriaAppRoot(auth: FirebaseAuth = Firebase.auth) {
    var currentUser by remember { mutableStateOf(auth.currentUser) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = { currentUser = auth.currentUser }
        )
    } else {
        key(user.uid) {
            AuthenticatedCafeteriaFlow(currentUser = user)
        }
    }
}

@Composable
private fun AuthenticatedCafeteriaFlow(currentUser: FirebaseUser) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: CafeteriaViewModel = viewModel(
        key = currentUser.uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                CafeteriaViewModel(
                    repository = CafeteriaRepository(db),
                    currentUserId = currentUser.uid
                )
            }
        }
    )

    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val ordersState by viewModel.ordersState.collectAsStateWithLifecycle()
    val menuItems by viewModel.filteredMenu.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val cart by viewModel.cart.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()
    val feedbackMessage by viewModel.feedbackMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(feedbackMessage) {
        val msg = feedbackMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.consumeFeedbackMessage()
    }

    var isEditingCredential by remember { mutableStateOf(false) }

    val handleSignOut: () -> Unit = {
        GoogleAuthHelper.signOut(
            credentialManager = credentialManager,
            scope = scope
        )
    }

    when (val state = profileState) {
        UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is UiState.Error -> {
            CredencialSetupScreen(
                existingProfile = null,
                defaultName = currentUser.displayName.orEmpty(),
                userEmail = currentUser.email,
                isSubmitting = isSubmitting,
                canNavigateBack = false,
                onBack = {},
                onSaveCredential = { control, name, specialty, semester, shift ->
                    viewModel.saveStudentCredential(
                        numeroControl = control,
                        fullName = name,
                        specialty = specialty,
                        semester = semester,
                        shift = shift,
                        email = currentUser.email,
                        onSaved = { isEditingCredential = false }
                    )
                },
                onSignOut = handleSignOut
            )
        }
        is UiState.Success -> {
            val profile = state.data
            if (profile == null || isEditingCredential) {
                CredencialSetupScreen(
                    existingProfile = profile,
                    defaultName = currentUser.displayName.orEmpty(),
                    userEmail = currentUser.email,
                    isSubmitting = isSubmitting,
                    canNavigateBack = profile != null,
                    onBack = { isEditingCredential = false },
                    onSaveCredential = { control, name, specialty, semester, shift ->
                        viewModel.saveStudentCredential(
                            numeroControl = control,
                            fullName = name,
                            specialty = specialty,
                            semester = semester,
                            shift = shift,
                            email = currentUser.email,
                            onSaved = { isEditingCredential = false }
                        )
                    },
                    onSignOut = handleSignOut
                )
            } else {
                val navController = rememberNavController()
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route
                val tabs = listOf(BottomTab.Menu, BottomTab.Orders, BottomTab.Credential)
                val showTopLevelNav = tabs.any { it.route == currentRoute }

                val ordersList = (ordersState as? UiState.Success)?.data.orEmpty()
                val activeOrders = ordersList.filter {
                    val st = it.parsedStatus()
                    st == OrderStatus.RECIBIDO || st == OrderStatus.PREPARANDO || st == OrderStatus.LISTO
                }
                val activeOrder = activeOrders.firstOrNull()

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val isWideScreen = maxWidth >= 600.dp

                    Scaffold(
                        contentWindowInsets = WindowInsets.safeDrawing,
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        bottomBar = {
                            if (showTopLevelNav && !isWideScreen) {
                                NavigationBar {
                                    tabs.forEach { tab ->
                                        val selected = currentRoute == tab.route
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = {
                                                navController.navigate(tab.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                            icon = {
                                                TabIconWithBadge(
                                                    tab = tab,
                                                    selected = selected,
                                                    activeOrdersCount = activeOrders.size
                                                )
                                            },
                                            label = { Text(tab.label) },
                                            modifier = Modifier.testTag(tab.testTag)
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            if (showTopLevelNav && isWideScreen) {
                                NavigationRail {
                                    tabs.forEach { tab ->
                                        val selected = currentRoute == tab.route
                                        NavigationRailItem(
                                            selected = selected,
                                            onClick = {
                                                navController.navigate(tab.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                            icon = {
                                                TabIconWithBadge(
                                                    tab = tab,
                                                    selected = selected,
                                                    activeOrdersCount = activeOrders.size
                                                )
                                            },
                                            label = { Text(tab.label) },
                                            modifier = Modifier.testTag(tab.testTag)
                                        )
                                    }
                                }
                            }

                            NavHost(
                                navController = navController,
                                startDestination = BottomTab.Menu.route,
                                modifier = Modifier.weight(1f)
                            ) {
                                composable(BottomTab.Menu.route) {
                                    MenuScreen(
                                        profile = profile,
                                        menuItems = menuItems,
                                        selectedCategory = selectedCategory,
                                        searchQuery = searchQuery,
                                        cart = cart,
                                        activeOrder = activeOrder,
                                        isSubmitting = isSubmitting,
                                        onCategorySelected = viewModel::setSelectedCategory,
                                        onSearchQueryChange = viewModel::setSearchQuery,
                                        onAddToCart = viewModel::addToCart,
                                        onUpdateCartQuantity = viewModel::updateCartItemQuantity,
                                        onClearCart = viewModel::clearCart,
                                        onPlaceOrder = { pickupWindow, paymentMethod, notes ->
                                            viewModel.placeOrder(
                                                profile = profile,
                                                pickupWindow = pickupWindow,
                                                paymentMethod = paymentMethod,
                                                generalNotes = notes,
                                                onOrderCreated = { created ->
                                                    navController.navigate("ticket/${created.id}")
                                                }
                                            )
                                        },
                                        onOpenActiveOrderQr = { order ->
                                            navController.navigate("ticket/${order.id}")
                                        }
                                    )
                                }

                                composable(BottomTab.Orders.route) {
                                    OrdersScreen(
                                        ordersState = ordersState,
                                        onOpenOrderQr = { order ->
                                            navController.navigate("ticket/${order.id}")
                                        },
                                        onExploreMenu = {
                                            navController.navigate(BottomTab.Menu.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                }

                                composable(BottomTab.Credential.route) {
                                    CredentialProfileScreen(
                                        profile = profile,
                                        onEditCredential = { isEditingCredential = true },
                                        onSignOut = handleSignOut
                                    )
                                }

                                composable(
                                    route = "ticket/{orderId}",
                                    arguments = listOf(navArgument("orderId") { type = NavType.StringType })
                                ) { entry ->
                                    val orderId = entry.arguments?.getString("orderId").orEmpty()
                                    val order = ordersList.firstOrNull { it.id == orderId }
                                    OrderQrTicketScreen(
                                        order = order,
                                        onBack = { navController.popBackStack() },
                                        onAdvanceStatus = viewModel::advanceOrderStatus,
                                        onCancelOrder = viewModel::cancelOrder
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabIconWithBadge(
    tab: BottomTab,
    selected: Boolean,
    activeOrdersCount: Int
) {
    val icon = if (selected) tab.selectedIcon else tab.unselectedIcon
    if (tab == BottomTab.Orders && activeOrdersCount > 0) {
        BadgedBox(
            badge = {
                Badge {
                    Text("$activeOrdersCount")
                }
            }
        ) {
            Icon(icon, contentDescription = tab.label)
        }
    } else {
        Icon(icon, contentDescription = tab.label)
    }
}
