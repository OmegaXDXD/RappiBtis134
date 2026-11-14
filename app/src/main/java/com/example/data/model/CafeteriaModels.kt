package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class StudentProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val numeroControl: String = "",
    val fullName: String = "",
    val specialty: String = "Programación",
    val semester: Long = 1L,
    val shift: String = "Matutino",
    val email: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class OrderLineItem(
    val itemId: String = "",
    val name: String = "",
    val quantity: Long = 1L,
    val unitPriceMxn: Double = 0.0
)

@IgnoreExtraProperties
data class CafeteriaOrder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val folio: String = "",
    val numeroControl: String = "",
    val studentName: String = "",
    val itemsSummary: String = "",
    val items: List<OrderLineItem> = emptyList(),
    val totalMxn: Double = 0.0,
    val pickupWindow: String = "",
    val paymentMethod: String = "Efectivo",
    val notes: String = "",
    val qrPayload: String = "",
    val status: String = OrderStatus.RECIBIDO.firestoreValue,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun parsedStatus(): OrderStatus = OrderStatus.fromValue(status)
}

enum class OrderStatus(val firestoreValue: String, val displayLabel: String, val stepIndex: Int) {
    RECIBIDO("recibido", "Recibido", 0),
    PREPARANDO("preparando", "En preparación", 1),
    LISTO("listo", "Listo para recoger", 2),
    ENTREGADO("entregado", "Entregado", 3),
    CANCELADO("cancelado", "Cancelado", -1);

    companion object {
        fun fromValue(value: String): OrderStatus =
            entries.firstOrNull { it.firestoreValue == value } ?: RECIBIDO
    }
}

enum class MenuCategory(val label: String) {
    ALL("Todo"),
    DESAYUNOS("Desayunos"),
    TORTAS("Tortas y Burritos"),
    ANTOJITOS("Antojitos"),
    BEBIDAS("Bebidas"),
    SNACKS("Snacks")
}

data class MenuItem(
    val id: String,
    val name: String,
    val description: String,
    val priceMxn: Double,
    val category: MenuCategory,
    val prepMinutes: Int,
    val badge: String? = null,
    val isComboHighlight: Boolean = false
)

data class CartEntry(
    val item: MenuItem,
    val quantity: Int,
    val customNote: String = ""
) {
    val subtotalMxn: Double get() = item.priceMxn * quantity
}

object CbtisCatalog {
    val specialties = listOf(
        "Programación",
        "Contabilidad",
        "Administración de Recursos Humanos",
        "Electricidad",
        "Mecatrónica",
        "Soporte y Mantenimiento"
    )

    val shifts = listOf("Matutino", "Vespertino")

    val pickupWindows = listOf(
        "Receso Matutino (10:00 - 10:30)",
        "Cambio de Módulo (12:00)",
        "Receso Vespertino (16:30 - 17:00)",
        "Lo antes posible (10-15 min)"
    )

    val paymentMethods = listOf("Efectivo", "Transferencia")

    val menuItems = listOf(
        MenuItem(
            id = "combo_cbtis",
            name = "Combo Tigre 134",
            description = "Chilaquiles verdes o rojos con pollo deshebrado, crema, queso y agua fresca de horchata (500 ml).",
            priceMxn = 58.0,
            category = MenuCategory.DESAYUNOS,
            prepMinutes = 10,
            badge = "Más pedido",
            isComboHighlight = true
        ),
        MenuItem(
            id = "chilaquiles_pollo",
            name = "Chilaquiles Verdes con Pollo",
            description = "Totopos crujientes bañados en salsa verde de la casa, pollo, crema, queso fresco y cebolla.",
            priceMxn = 45.0,
            category = MenuCategory.DESAYUNOS,
            prepMinutes = 8,
            badge = "Popular"
        ),
        MenuItem(
            id = "molletes_pico",
            name = "Molletes con Pico de Gallo",
            description = "Dos mitades de bolillo dorado con frijoles refritos, queso manchego gratinado y pico de gallo.",
            priceMxn = 32.0,
            category = MenuCategory.DESAYUNOS,
            prepMinutes = 7,
            badge = "Rápido"
        ),
        MenuItem(
            id = "huevos_gusto",
            name = "Desayuno con Huevo y Frijoles",
            description = "Huevos revueltos con jamón o a la mexicana acompañados de frijoles y 4 tortillas calientes.",
            priceMxn = 40.0,
            category = MenuCategory.DESAYUNOS,
            prepMinutes = 9
        ),
        MenuItem(
            id = "torta_milanesa",
            name = "Torta de Milanesa",
            description = "Bolillo crujiente con milanesa de res, aguacate, jitomate, lechuga, frijoles y rajas.",
            priceMxn = 42.0,
            category = MenuCategory.TORTAS,
            prepMinutes = 8,
            badge = "Popular"
        ),
        MenuItem(
            id = "torta_cubana",
            name = "Torta Especial 134",
            description = "Jamón, salchicha, queso amarillo, quesillo, aguacate y chipotle en pan calientito a la plancha.",
            priceMxn = 46.0,
            category = MenuCategory.TORTAS,
            prepMinutes = 9
        ),
        MenuItem(
            id = "burrito_deshebrada",
            name = "Burrito de Deshebrada",
            description = "Tortilla de harina grande rellena de carne deshebrada guisada, frijoles y queso fundido.",
            priceMxn = 38.0,
            category = MenuCategory.TORTAS,
            prepMinutes = 6,
            badge = "Rápido"
        ),
        MenuItem(
            id = "sincronizada",
            name = "Sincronizada Doble",
            description = "Dos tortillas de harina doradas con jamón de pavo, queso Oaxaca y salsa casera.",
            priceMxn = 35.0,
            category = MenuCategory.TORTAS,
            prepMinutes = 5
        ),
        MenuItem(
            id = "quesadillas_oaxaca",
            name = "Quesadillas de Queso Oaxaca (2)",
            description = "Dos quesadillas hechas al momento en comal con queso Oaxaca y salsa verde o roja.",
            priceMxn = 30.0,
            category = MenuCategory.ANTOJITOS,
            prepMinutes = 6
        ),
        MenuItem(
            id = "tacos_dorados",
            name = "Orden de Tacos Dorados (4)",
            description = "Crujientes tacos dorados de pollo con lechuga, crema, queso rallado y salsa verde.",
            priceMxn = 38.0,
            category = MenuCategory.ANTOJITOS,
            prepMinutes = 7
        ),
        MenuItem(
            id = "agua_horchata",
            name = "Agua de Horchata (500 ml)",
            description = "Preparada diariamente con arroz natural, leche, canela y hielo.",
            priceMxn = 18.0,
            category = MenuCategory.BEBIDAS,
            prepMinutes = 2,
            badge = "Fría"
        ),
        MenuItem(
            id = "agua_jamaica",
            name = "Agua de Jamaica (500 ml)",
            description = "Infusión natural de flor de jamaica ligeramente endulzada y bien fría.",
            priceMxn = 18.0,
            category = MenuCategory.BEBIDAS,
            prepMinutes = 2
        ),
        MenuItem(
            id = "cafe_olla",
            name = "Café de Olla / Capuchino",
            description = "Vaso caliente de 12 oz ideal para las primeras horas del turno matutino.",
            priceMxn = 22.0,
            category = MenuCategory.BEBIDAS,
            prepMinutes = 3
        ),
        MenuItem(
            id = "vaso_fruta",
            name = "Vaso de Fruta de Temporada",
            description = "Jícama, pepino, mango y sandía frescos con limón y chile piquín al gusto.",
            priceMxn = 25.0,
            category = MenuCategory.SNACKS,
            prepMinutes = 3,
            badge = "Fresco"
        ),
        MenuItem(
            id = "pay_queso",
            name = "Rebanada de Pay de Queso",
            description = "Postre cremoso horneado con base de galleta de vainilla.",
            priceMxn = 24.0,
            category = MenuCategory.SNACKS,
            prepMinutes = 2
        )
    )
}
