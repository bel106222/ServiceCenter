package ru.bel.servicecenter.controllers
import androidx.lifecycle.ViewModel
/**
 * Общая ViewModel для экранов работы с клиентами.
 * Содержит ClientController и может быть дополнена справочниками в будущем.
 */
class ClientManagementViewModel : ViewModel() {
    val clientController = ClientController()
}