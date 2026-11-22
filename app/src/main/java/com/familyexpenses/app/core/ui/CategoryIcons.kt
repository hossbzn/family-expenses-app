package com.familyexpenses.app.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

data class IconOption(
    val key: String,
    val label: String,
)

val AVAILABLE_CATEGORY_ICONS: List<IconOption> = listOf(
    IconOption("shopping_cart", "Compra"),
    IconOption("home", "Hogar"),
    IconOption("directions_car", "Transporte"),
    IconOption("restaurant", "Comida"),
    IconOption("medical_services", "Salud"),
    IconOption("sports_esports", "Ocio"),
    IconOption("receipt_long", "Facturas"),
    IconOption("checkroom", "Ropa"),
    IconOption("school", "Educación"),
    IconOption("payments", "Pagos"),
    IconOption("savings", "Ahorro"),
    IconOption("local_cafe", "Café"),
    IconOption("flight", "Viajes"),
    IconOption("pets", "Mascotas"),
    IconOption("fitness_center", "Deporte"),
    IconOption("work", "Trabajo"),
    IconOption("card_giftcard", "Regalos"),
    IconOption("build", "Mantenimiento"),
    IconOption("person", "Personal"),
    IconOption("category", "General"),
)

fun resolveAppIcon(iconName: String): ImageVector = when (iconName) {
    "shopping_cart" -> Icons.Default.ShoppingCart
    "home" -> Icons.Default.Home
    "directions_car" -> Icons.Default.DirectionsCar
    "restaurant" -> Icons.Default.Restaurant
    "medical_services" -> Icons.Default.MedicalServices
    "sports_esports" -> Icons.Default.SportsEsports
    "receipt_long" -> Icons.AutoMirrored.Filled.ReceiptLong
    "checkroom" -> Icons.Default.Checkroom
    "school" -> Icons.Default.School
    "payments" -> Icons.Default.Payments
    "savings" -> Icons.Default.Savings
    "local_cafe" -> Icons.Default.LocalCafe
    "flight" -> Icons.Default.Flight
    "pets" -> Icons.Default.Pets
    "fitness_center" -> Icons.Default.FitnessCenter
    "work" -> Icons.Default.Work
    "card_giftcard" -> Icons.Default.CardGiftcard
    "build" -> Icons.Default.Build
    "person" -> Icons.Default.Person
    else -> Icons.Default.Category
}
