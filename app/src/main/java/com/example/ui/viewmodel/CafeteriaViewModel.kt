package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CafeteriaOrder
import com.example.data.model.CartEntry
import com.example.data.model.CbtisCatalog
import com.example.data.model.MenuCategory
import com.example.data.model.MenuItem
import com.example.data.model.OrderLineItem
import com.example.data.model.OrderStatus
import com.example.data.model.StudentProfile
import com.example.data.repository.CafeteriaRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class CafeteriaViewModel(
    private val repository: CafeteriaRepository,
    private val currentUserId: String
) : ViewModel() {

    val profileState: StateFlow<UiState<StudentProfile?>> =
        repository.observeStudentProfile(currentUserId)
            .map<StudentProfile?, UiState<StudentProfile?>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing student profile", rootError)
                emit(UiState.Error(rootError.message ?: "No se pudo cargar tu credencial escolar."))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val ordersState: StateFlow<UiState<List<CafeteriaOrder>>> =
        repository.observeOrders(currentUserId)
            .map<List<CafeteriaOrder>, UiState<List<CafeteriaOrder>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing orders", rootError)
                emit(UiState.Error(rootError.message ?: "No se pudieron cargar tus pedidos QR."))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    private val _selectedCategory = MutableStateFlow(MenuCategory.ALL)
    val selectedCategory: StateFlow<MenuCategory> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredMenu: StateFlow<List<MenuItem>> =
        combine(_selectedCategory, _searchQuery) { category, query ->
            val cleanQuery = query.trim().lowercase()
            CbtisCatalog.menuItems.filter { item ->
                val matchesCategory = category == MenuCategory.ALL || item.category == category
                val matchesQuery = cleanQuery.isEmpty() ||
                    item.name.lowercase().contains(cleanQuery) ||
                    item.description.lowercase().contains(cleanQuery)
                matchesCategory && matchesQuery
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = CbtisCatalog.menuItems
        )

    private val _cart = MutableStateFlow<List<CartEntry>>(emptyList())
    val cart: StateFlow<List<CartEntry>> = _cart.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    fun setSelectedCategory(category: MenuCategory) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addToCart(item: MenuItem, quantity: Int = 1, customNote: String = "") {
        _cart.update { current ->
            val existingIndex = current.indexOfFirst { it.item.id == item.id }
            if (existingIndex >= 0) {
                current.toMutableList().apply {
                    val prev = this[existingIndex]
                    val mergedNote = if (customNote.isNotBlank()) customNote else prev.customNote
                    this[existingIndex] = prev.copy(
                        quantity = (prev.quantity + quantity).coerceAtMost(15),
                        customNote = mergedNote
                    )
                }
            } else {
                current + CartEntry(item = item, quantity = quantity.coerceIn(1, 15), customNote = customNote)
            }
        }
        _feedbackMessage.value = "${item.name} agregado al pedido"
    }

    fun updateCartItemQuantity(itemId: String, delta: Int) {
        _cart.update { current ->
            current.mapNotNull { entry ->
                if (entry.item.id != itemId) {
                    entry
                } else {
                    val nextQty = entry.quantity + delta
                    if (nextQty <= 0) null else entry.copy(quantity = nextQty.coerceAtMost(15))
                }
            }
        }
    }

    fun clearCart() {
        _cart.value = emptyList()
    }

    fun consumeFeedbackMessage() {
        _feedbackMessage.value = null
    }

    fun saveStudentCredential(
        numeroControl: String,
        fullName: String,
        specialty: String,
        semester: Long,
        shift: String,
        email: String?,
        onSaved: () -> Unit
    ) {
        if (_isSubmitting.value) return
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.saveStudentProfile(
                numeroControl = numeroControl,
                fullName = fullName,
                specialty = specialty,
                semester = semester,
                shift = shift,
                email = email
            )
            _isSubmitting.value = false
            result.onSuccess {
                _feedbackMessage.value = "Credencial CBTis 134 vinculada correctamente"
                onSaved()
            }.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "Error al guardar la credencial."
            }
        }
    }

    fun placeOrder(
        profile: StudentProfile,
        pickupWindow: String,
        paymentMethod: String,
        generalNotes: String,
        onOrderCreated: (CafeteriaOrder) -> Unit
    ) {
        val currentCart = _cart.value
        if (currentCart.isEmpty() || _isSubmitting.value) return

        val itemNotes = currentCart
            .filter { it.customNote.isNotBlank() }
            .joinToString(" · ") { "${it.item.name}: ${it.customNote}" }
        val combinedNotes = listOf(generalNotes.trim(), itemNotes)
            .filter { it.isNotBlank() }
            .joinToString(" | ")
            .take(200)

        val lineItems = currentCart.map { entry ->
            OrderLineItem(
                itemId = entry.item.id,
                name = entry.item.name,
                quantity = entry.quantity.toLong(),
                unitPriceMxn = entry.item.priceMxn
            )
        }

        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.createOrder(
                profile = profile,
                items = lineItems,
                pickupWindow = pickupWindow,
                paymentMethod = paymentMethod,
                notes = combinedNotes
            )
            _isSubmitting.value = false
            result.onSuccess { order ->
                _cart.value = emptyList()
                _feedbackMessage.value = "Pedido ${order.folio} generado con código QR"
                onOrderCreated(order)
            }.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "No se pudo confirmar el pedido."
            }
        }
    }

    fun advanceOrderStatus(order: CafeteriaOrder) {
        val next = when (order.parsedStatus()) {
            OrderStatus.RECIBIDO -> OrderStatus.PREPARANDO
            OrderStatus.PREPARANDO -> OrderStatus.LISTO
            OrderStatus.LISTO -> OrderStatus.ENTREGADO
            OrderStatus.ENTREGADO, OrderStatus.CANCELADO -> return
        }
        viewModelScope.launch {
            val result = repository.updateOrderStatus(order.id, next)
            result.onSuccess {
                _feedbackMessage.value = "Pedido ${order.folio}: ${next.displayLabel}"
            }.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "No se pudo actualizar el pedido."
            }
        }
    }

    fun cancelOrder(order: CafeteriaOrder) {
        if (order.parsedStatus() != OrderStatus.RECIBIDO) return
        viewModelScope.launch {
            val result = repository.updateOrderStatus(order.id, OrderStatus.CANCELADO)
            result.onSuccess {
                _feedbackMessage.value = "Pedido ${order.folio} cancelado"
            }.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "No se pudo cancelar el pedido."
            }
        }
    }

    private companion object {
        const val TAG = "CafeteriaVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
