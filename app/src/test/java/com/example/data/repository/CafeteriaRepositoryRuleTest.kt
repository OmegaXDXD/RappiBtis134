package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.OrderLineItem
import com.example.data.model.OrderStatus
import com.example.data.model.StudentProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CafeteriaRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveStudentProfile_andObserveProfile_succeedsForOwner() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = CafeteriaRepository(firestore, auth)

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveStudentProfile(
                numeroControl = "23320051340123",
                fullName = "Ana Sofia Martinez",
                specialty = "Programación",
                semester = 4L,
                shift = "Matutino",
                email = ALICE_EMAIL
            )
        }
        assertTrue("Expected profile save to succeed: ${saveResult.exceptionOrNull()}", saveResult.isSuccess)

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeStudentProfile(uid).first { it != null }
        }
        assertNotNull(observed)
        assertEquals("23320051340123", observed?.numeroControl)
        assertEquals("Programación", observed?.specialty)
    }

    @Test
    fun createOrder_andAdvanceLifecycle_succeedsForOwnerAndLocksTerminalState() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = CafeteriaRepository(firestore, auth)

        val profile = StudentProfile(
            id = uid,
            userId = uid,
            numeroControl = "23320051340123",
            fullName = "Ana Sofia Martinez",
            specialty = "Programación",
            semester = 4L,
            shift = "Matutino",
            email = ALICE_EMAIL
        )

        val items = listOf(
            OrderLineItem(
                itemId = "chilaquiles_pollo",
                name = "Chilaquiles Verdes con Pollo",
                quantity = 1L,
                unitPriceMxn = 45.0
            ),
            OrderLineItem(
                itemId = "agua_horchata",
                name = "Agua de Horchata (500 ml)",
                quantity = 1L,
                unitPriceMxn = 18.0
            )
        )

        val orderResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createOrder(
                profile = profile,
                items = items,
                pickupWindow = "Receso Matutino (10:00 - 10:30)",
                paymentMethod = "Efectivo",
                notes = "Sin cebolla"
            )
        }
        assertTrue("Expected order create to succeed: ${orderResult.exceptionOrNull()}", orderResult.isSuccess)
        val createdOrder = orderResult.getOrThrow()
        assertTrue(createdOrder.id.isNotBlank())
        assertTrue(createdOrder.qrPayload.contains("23320051340123"))

        val ordersList = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeOrders(uid).first { list -> list.any { it.id == createdOrder.id } }
        }
        assertTrue(ordersList.any { it.folio == createdOrder.folio })

        // Valid lifecycle progression: recibido -> preparando -> listo -> entregado
        assertTrue(
            withTimeout(DEFAULT_TIMEOUT_MS) {
                repository.updateOrderStatus(createdOrder.id, OrderStatus.PREPARANDO)
            }.isSuccess
        )
        assertTrue(
            withTimeout(DEFAULT_TIMEOUT_MS) {
                repository.updateOrderStatus(createdOrder.id, OrderStatus.LISTO)
            }.isSuccess
        )
        assertTrue(
            withTimeout(DEFAULT_TIMEOUT_MS) {
                repository.updateOrderStatus(createdOrder.id, OrderStatus.ENTREGADO)
            }.isSuccess
        )

        // Terminal state cannot be mutated afterward
        val postTerminalResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateOrderStatus(createdOrder.id, OrderStatus.CANCELADO)
        }
        assertTrue(postTerminalResult.isFailure)
        val terminalException = postTerminalResult.exceptionOrNull()?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, terminalException?.code)
    }

    @Test
    fun crossUserOrderAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = CafeteriaRepository(firestore, auth)

        val profile = StudentProfile(
            id = aliceUid,
            userId = aliceUid,
            numeroControl = "23320051340123",
            fullName = "Ana Sofia Martinez",
            specialty = "Programación",
            semester = 4L,
            shift = "Matutino",
            email = ALICE_EMAIL
        )
        val aliceOrder = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createOrder(
                profile = profile,
                items = listOf(OrderLineItem("torta_milanesa", "Torta de Milanesa", 1L, 42.0)),
                pickupWindow = "Receso Matutino (10:00 - 10:30)",
                paymentMethod = "Efectivo",
                notes = ""
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users").document(aliceUid)
                    .collection("orders").document(aliceOrder.id)
                    .get().await()
            }
            fail("Expected PERMISSION_DENIED when Bob reads Alice's order")
        } catch (e: Exception) {
            val firestoreEx = e.unwrapFirestoreException()
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
        }
    }

    @Test
    fun observeOrders_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = CafeteriaRepository(firestore, auth)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeOrders("some_unauth_uid").first()
            }
            fail("Expected PERMISSION_DENIED for unauthenticated orders observation")
        } catch (e: Exception) {
            val firestoreEx = e.unwrapFirestoreException()
            assertNotNull("Expected nested FirebaseFirestoreException, got $e", firestoreEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "ana.cbtis134@test.com"
        const val BOB_EMAIL = "luis.cbtis134@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
