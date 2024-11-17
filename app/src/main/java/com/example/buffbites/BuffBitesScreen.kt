@OptIn(ExperimentalMaterial3Api::class)

package com.example.buffbites

import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.example.buffbites.ui.OrderViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.buffbites.data.Datasource
import com.example.buffbites.ui.ChooseDeliveryTimePreview
import com.example.buffbites.ui.ChooseDeliveryTimeScreen
import com.example.buffbites.ui.ChooseMenuScreen
import com.example.buffbites.ui.OrderSummaryScreen
import com.example.buffbites.ui.StartOrderScreen
import javax.sql.DataSource


enum class BuffBitesScreen(@StringRes val title: Int) {
    Start(title = R.string.app_name),
    Restaurant(title = R.string.choose_meal),
    Delivery(title = R.string.choose_delivery_time),
    Summary(title = R.string.order_summary)
}

@ExperimentalMaterial3Api
@Composable
fun BuffBitesAppBar(
    currentScreen: BuffBitesScreen,
    canNavigateBack: Boolean,
    navigateUp: () -> Unit,
    onCancelButtonClicked: () -> Unit,
    onNextButtonClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = { Text(stringResource(currentScreen.title)) },
        modifier = modifier,
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = navigateUp) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back_button)
                    )
                }
            }
        }
    )
}
@ExperimentalMaterial3Api
@Composable
fun BuffBitesApp(
    viewModel: OrderViewModel = viewModel(),
    navController: NavHostController = rememberNavController()
) {
    val uiState by viewModel.uiState.collectAsState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentScreen = BuffBitesScreen.valueOf(
        backStackEntry?.destination?.route ?: BuffBitesScreen.Start.name
    )
    Scaffold(
        topBar = {
            BuffBitesAppBar(
                currentScreen = currentScreen,
                canNavigateBack = navController.previousBackStackEntry != null,
                navigateUp = { navController.navigateUp() }
            )
        }
    ) { innerPadding ->




        NavHost(
            navController = navController,
            startDestination = BuffBitesScreen.Start.name,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(route = BuffBitesScreen.Start.name) {
                StartOrderScreen(
                    restaurantOptions = Datasource.restaurants,
                    onNextButtonClicked = {
                        navController.navigate(BuffBitesScreen.Restaurant.name)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }

            composable(route = BuffBitesScreen.Restaurant.name) {
                val context = LocalContext.current
                ChooseMenuScreen(
                    options = viewModel.uiState.value.selectedVendor?.menuItems ?: emptyList(),
                    onNextButtonClicked = {
                        navController.navigate(BuffBitesScreen.Delivery.name)
                    },
                    onCancelButtonClicked = {
                        cancelOrderAndNavigateToStart(
                            viewModel,
                            navController
                        )
                    },
                    onSelectionChanged = { selectedMenuItem -> viewModel.updateMeal(selectedMenuItem) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            composable(route = BuffBitesScreen.Delivery.name) {
                ChooseDeliveryTimeScreen(
                    subtotal = uiState.orderSubtotal,
                    options = uiState.availableDeliveryTimes,
                    onCancelButtonClicked = {
                        cancelOrderAndNavigateToStart(
                            viewModel,
                            navController
                        )
                    },
                    onNextButtonClicked = { navController.navigate(BuffBitesScreen.Summary.name) },
                    onSelectionChanged = { viewModel.updateDeliveryTime(it) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            composable(route = BuffBitesScreen.Summary.name) {
                val context = LocalContext.current
                OrderSummaryScreen(
                    orderUiState = uiState,
                    onCancelButtonClicked = {
                        cancelOrderAndNavigateToStart(viewModel, navController)
                    },
                    onSendButtonClicked = { subject: String, summary: String -> shareOrder(context, subject = subject, summary)}
                )

            }

        }
    }
}



private fun cancelOrderAndNavigateToStart(
    viewModel: OrderViewModel,
    navController: NavHostController
){
    viewModel.resetOrder()
    navController.popBackStack(BuffBitesScreen.Start.name, inclusive = false)

}
private fun shareOrder(context: Context, subject: String, summary: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, summary)
    }
    context.startActivity(
        Intent.createChooser(
            intent,
            context.getString(R.string.new_buffbites_order)
        )
    )
}