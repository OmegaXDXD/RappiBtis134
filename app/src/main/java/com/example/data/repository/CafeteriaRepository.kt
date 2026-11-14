package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.CafeteriaOrder
import com.example.data.model.OrderLineItem
import com.example.data.model.OrderStatus
import com.example.data.model.StudentProfile
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Locale
import kotlin.random.Random

class CafeteriaRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("El estudiante debe iniciar sesión con Google antes de acceder a Firestore.")
    }

    fun observeStudentProfile(userId: String): Flow<StudentProfile?> {
        val docRef = db.collection("users").document(userId)
        return docRef.snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) {
                    null
                } else {
                    snapshot.toObject(
                        StudentProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
                }
                throw error
            }
    }

    suspend fun saveStudentProfile(
        numeroControl: String,
        fullName: String,
        specialty: String,
        semester: Long,
        shift: String,
        email: String?
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid)
        return try {
            val existing = docRef.get().await()
            val cleanControl = numeroControl.trim().uppercase(Locale.ROOT)
            val cleanName = fullName.trim()
            val cleanSpecialty = specialty.trim()
            val cleanShift = if (shift == "Vespertino") "Vespertino" else "Matutino"
            val cleanEmail = email?.trim().orEmpty()

            if (!existing.exists()) {
                val payload = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "numeroControl" to cleanControl,
                    "fullName" to cleanName,
                    "specialty" to cleanSpecialty,
                    "semester" to semester.coerceIn(1L, 6L),
                    "shift" to cleanShift,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (cleanEmail.isNotBlank()) {
                    payload["email"] = cleanEmail
                }
                docRef.set(payload).await()
            } else {
                val updates = mutableMapOf<String, Any>(
                    "numeroControl" to cleanControl,
                    "fullName" to cleanName,
                    "specialty" to cleanSpecialty,
                    "semester" to semester.coerceIn(1L, 6L),
                    "shift" to cleanShift,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (cleanEmail.isNotBlank()) {
                    updates["email"] = cleanEmail
                }
                docRef.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeOrders(userId: String): Flow<List<CafeteriaOrder>> {
        val collectionRef = db.collection("users").document(userId).collection("orders")
        return collectionRef.snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CafeteriaOrder::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, collectionRef.path)
                }
                throw error
            }
    }

    suspend fun createOrder(
        profile: StudentProfile,
        items: List<OrderLineItem>,
        pickupWindow: String,
        paymentMethod: String,
        notes: String
    ): Result<CafeteriaOrder> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        require(items.isNotEmpty()) { "El pedido debe incluir al menos un platillo." }

        val collectionRef = db.collection("users").document(uid).collection("orders")
        val docRef = collectionRef.document()

        val folioNumber = Random.nextInt(1000, 9999)
        val folio = "CBT134-$folioNumber"
        val totalMxn = items.sumOf { it.unitPriceMxn * it.quantity }
        val summary = items.joinToString(", ") { "${it.quantity}x ${it.name}" }.take(500)
        val qrPayload = buildOrderQrPayload(
            folio = folio,
            orderId = docRef.id,
            numeroControl = profile.numeroControl,
            totalMxn = totalMxn,
            pickupWindow = pickupWindow,
            itemsSummary = summary
        )

        val itemMaps = items.map { item ->
            mapOf(
                "itemId" to item.itemId,
                "name" to item.name,
                "quantity" to item.quantity,
                "unitPriceMxn" to item.unitPriceMxn
            )
        }

        val payload = mutableMapOf<String, Any>(
            "userId" to uid,
            "folio" to folio,
            "numeroControl" to profile.numeroControl,
            "studentName" to profile.fullName,
            "itemsSummary" to summary,
            "items" to itemMaps,
            "totalMxn" to totalMxn,
            "pickupWindow" to pickupWindow,
            "paymentMethod" to paymentMethod,
            "qrPayload" to qrPayload,
            "status" to OrderStatus.RECIBIDO.firestoreValue,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        val cleanNotes = notes.trim().take(200)
        if (cleanNotes.isNotEmpty()) {
            payload["notes"] = cleanNotes
        }

        return try {
            docRef.set(payload).await()
            val createdOrder = CafeteriaOrder(
                id = docRef.id,
                userId = uid,
                folio = folio,
                numeroControl = profile.numeroControl,
                studentName = profile.fullName,
                itemsSummary = summary,
                items = items,
                totalMxn = totalMxn,
                pickupWindow = pickupWindow,
                paymentMethod = paymentMethod,
                notes = cleanNotes,
                qrPayload = qrPayload,
                status = OrderStatus.RECIBIDO.firestoreValue,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
            Result.success(createdOrder)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getOrderById(orderId: String): Result<CafeteriaOrder?> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid).collection("orders").document(orderId)
        return try {
            val snap = docRef.get().await()
            val order = if (snap.exists()) {
                snap.toObject(
                    CafeteriaOrder::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            } else {
                null
            }
            Result.success(order)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateOrderStatus(orderId: String, nextStatus: OrderStatus): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid).collection("orders").document(orderId)
        return try {
            docRef.update(
                mapOf(
                    "status" to nextStatus.firestoreValue,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    companion object {
        fun buildOrderQrPayload(
            folio: String,
            orderId: String,
            numeroControl: String,
            totalMxn: Double,
            pickupWindow: String,
            itemsSummary: String
        ): String {
            val formattedTotal = String.format(Locale.US, "%.2f", totalMxn)
            return "CBTIS134|FOLIO:$folio|ID:$orderId|NC:$numeroControl|TOTAL:$formattedTotal|HORA:$pickupWindow|PEDIDO:$itemsSummary"
                .take(580)
        }
    }
}
