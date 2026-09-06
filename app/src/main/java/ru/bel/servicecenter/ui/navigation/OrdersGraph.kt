package ru.bel.servicecenter.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import ru.bel.servicecenter.ui.orders.OrderEditScreen
import ru.bel.servicecenter.ui.orders.OrderItemEditScreen
import ru.bel.servicecenter.ui.orders.OrdersListScreen
import ru.bel.servicecenter.viewmodels.OrderItemViewModel
import ru.bel.servicecenter.viewmodels.OrderViewModel

fun NavGraphBuilder.ordersGraph(
    navController: NavController,
    orderViewModel: OrderViewModel,
    orderItemViewModel: OrderItemViewModel
) {
    composable("orders") {
        OrdersListScreen(
            orderViewModel = orderViewModel,
            onEditOrder = { order ->
                orderViewModel.prepareForEdit(order)
                navController.navigate("order_edit")
            },
            onDeleteOrder = { order ->
                orderViewModel.deleteOrder(order)
            },
            onAddNewOrder = {
                orderViewModel.prepareForNewOrder()
                navController.navigate("order_new")
            },
            onBack = { navController.popBackStack() }
        )
    }

    composable("order_new") {
        OrderEditScreen(
            orderViewModel = orderViewModel,
            orderItemViewModel = orderItemViewModel,
            onAddItem = {
                orderItemViewModel.currentOrderId = orderViewModel.currentOrderWithItems.value?.id ?: ""
                orderItemViewModel.startNewItem()
                navController.navigate("order_item_new")
            },
            onEditItem = { item ->
                orderItemViewModel.currentOrderId = item.order_id
                orderItemViewModel.startEditing(item)
                navController.navigate("order_item_edit")
            },
            onSaved = { navController.popBackStack() },
            onCancel = { navController.popBackStack() }
        )
    }

    composable("order_edit") {
        OrderEditScreen(
            orderViewModel = orderViewModel,
            orderItemViewModel = orderItemViewModel,
            onAddItem = {
                orderItemViewModel.currentOrderId = orderViewModel.currentOrderWithItems.value?.id ?: ""
                orderItemViewModel.startNewItem()
                navController.navigate("order_item_new")
            },
            onEditItem = { item ->
                orderItemViewModel.currentOrderId = item.order_id
                orderItemViewModel.startEditing(item)
                navController.navigate("order_item_edit")
            },
            onSaved = { navController.popBackStack() },
            onCancel = { navController.popBackStack() }
        )
    }

    composable("order_item_new") {
        OrderItemEditScreen(
            orderItemViewModel = orderItemViewModel,
            onSaved = { navController.popBackStack() },
            onCancel = { navController.popBackStack() }
        )
    }

    composable("order_item_edit") {
        OrderItemEditScreen(
            orderItemViewModel = orderItemViewModel,
            onSaved = { navController.popBackStack() },
            onCancel = { navController.popBackStack() }
        )
    }
}