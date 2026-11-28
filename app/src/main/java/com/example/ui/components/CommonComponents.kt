package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LaptopMac
import androidx.compose.material.icons.filled.LocalPizza
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CardBackgroundDark
import com.example.ui.theme.CardBackgroundLight
import com.example.ui.theme.CardBorderLight
import com.example.ui.theme.LocalIsDarkTheme

import androidx.compose.ui.graphics.luminance

@Composable
fun TranslucentCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    elevation: Dp = 2.dp,
    border: BorderStroke? = null,
    isDarkTheme: Boolean = LocalIsDarkTheme.current ?: (MaterialTheme.colorScheme.background.luminance() < 0.5f),
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val bgColor = remember(isDarkTheme) {
        if (isDarkTheme) CardBackgroundDark else CardBackgroundLight
    }
    val cardContentColor = MaterialTheme.colorScheme.onSurface
    val effectiveBorder = remember(isDarkTheme, border) {
        border ?: if (isDarkTheme) BorderStroke(1.dp, Color(0x22FFFFFF)) else BorderStroke(1.dp, CardBorderLight)
    }

    val cardShape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    val shadowAmbientColor = remember(isDarkTheme) {
        if (isDarkTheme) Color.Black.copy(alpha = 0.32f) else Color(0xFF4F46E5).copy(alpha = 0.08f)
    }
    val shadowSpotColor = remember(isDarkTheme) {
        if (isDarkTheme) Color.Black.copy(alpha = 0.42f) else Color(0xFF4F46E5).copy(alpha = 0.12f)
    }
    val cardModifier = if (elevation > 0.dp) {
        modifier
            .shadow(
                elevation = elevation,
                shape = cardShape,
                ambientColor = shadowAmbientColor,
                spotColor = shadowSpotColor
            )
            .clip(cardShape)
    } else {
        modifier.clip(cardShape)
    }

    key(isDarkTheme) {
        if (onClick != null) {
            Card(
                onClick = onClick,
                modifier = cardModifier,
                shape = cardShape,
                colors = CardDefaults.cardColors(
                    containerColor = bgColor,
                    contentColor = cardContentColor
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = effectiveBorder
            ) {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.material3.LocalContentColor provides cardContentColor
                ) {
                    content()
                }
            }
        } else {
            Card(
                modifier = cardModifier,
                shape = cardShape,
                colors = CardDefaults.cardColors(
                    containerColor = bgColor,
                    contentColor = cardContentColor
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = effectiveBorder
            ) {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.material3.LocalContentColor provides cardContentColor
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onBackground
            ),
            modifier = Modifier.weight(1f)
        )
        if (actionText != null && onActionClick != null) {
            TextButton(
                onClick = onActionClick,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

fun getIconForName(iconName: String): ImageVector {
    return when (iconName.lowercase().trim()) {
        "category" -> Icons.Default.Category
        "shopping_cart", "cart" -> Icons.Default.ShoppingCart
        "restaurant", "food", "dining" -> Icons.Default.Restaurant
        "directions_car", "car" -> Icons.Default.DirectionsCar
        "shopping_bag", "bag" -> Icons.Default.ShoppingBag
        "receipt" -> Icons.Default.Receipt
        "movie", "film" -> Icons.Default.Movie
        "medical_services", "medical" -> Icons.Default.MedicalServices
        "school", "education" -> Icons.Default.School
        "sync", "autorenew" -> Icons.Default.Sync
        "eco", "leaf" -> Icons.Default.Eco
        "home", "house" -> Icons.Default.Home
        "more_horiz", "more" -> Icons.Default.MoreHoriz
        "local_atm", "atm" -> Icons.Default.LocalAtm
        "work", "briefcase" -> Icons.Default.Work
        "trending_up", "trend" -> Icons.Default.TrendingUp
        "card_giftcard", "gift" -> Icons.Default.CardGiftcard
        "add_circle", "plus" -> Icons.Default.AddCircle
        "savings", "piggy" -> Icons.Default.Savings
        "account_balance", "bank" -> Icons.Default.AccountBalance
        "credit_card" -> Icons.Default.CreditCard
        "payments", "bills" -> Icons.Default.Payments
        "account_balance_wallet", "wallet" -> Icons.Default.AccountBalanceWallet
        "swap_horiz", "transfer" -> Icons.Default.SwapHoriz
        "groups", "group", "people" -> Icons.Default.Groups
        "percent" -> Icons.Default.Percent
        "flight", "airplane" -> Icons.Default.Flight
        "pets", "pet" -> Icons.Default.Pets
        "fitness_center", "gym", "fitness" -> Icons.Default.FitnessCenter
        "face" -> Icons.Default.Face
        "local_cafe", "coffee" -> Icons.Default.LocalCafe
        "wifi" -> Icons.Default.Wifi
        "phone_android", "phone" -> Icons.Default.PhoneAndroid
        "flatware", "cutlery" -> Icons.Default.Flatware
        "fastfood", "burger" -> Icons.Default.Fastfood
        "local_gas_station", "gas", "fuel" -> Icons.Default.LocalGasStation
        "local_parking", "parking" -> Icons.Default.LocalParking
        "directions_bus", "bus" -> Icons.Default.DirectionsBus
        "local_taxi", "taxi" -> Icons.Default.LocalTaxi
        "cottage" -> Icons.Default.Cottage
        "electric_bolt", "bolt", "electricity" -> Icons.Default.ElectricBolt
        "water_drop", "water" -> Icons.Default.WaterDrop
        "checkroom", "clothing", "hanger" -> Icons.Default.Checkroom
        "devices", "computer" -> Icons.Default.Devices
        "weekend", "sofa", "furniture" -> Icons.Default.Weekend
        "palette", "art" -> Icons.Default.Palette
        "child_care", "baby" -> Icons.Default.ChildCare
        "local_laundry_service", "laundry" -> Icons.Default.LocalLaundryService
        "healing", "first_aid" -> Icons.Default.Healing
        "business_center" -> Icons.Default.BusinessCenter
        "flight_takeoff" -> Icons.Default.FlightTakeoff
        "hotel", "bed" -> Icons.Default.Hotel
        "beach_access", "umbrella" -> Icons.Default.BeachAccess
        "menu_book", "book" -> Icons.Default.MenuBook
        "receipt_long" -> Icons.Default.ReceiptLong
        "sports_esports", "games", "gaming" -> Icons.Default.SportsEsports
        "music_note", "music" -> Icons.Default.MusicNote
        "apps", "grid" -> Icons.Default.Apps
        "stroller" -> Icons.Default.Stroller
        "star" -> Icons.Default.Star
        "update", "history" -> Icons.Default.Update
        "pie_chart", "chart" -> Icons.Default.PieChart
        "directions_walk", "walk" -> Icons.Default.DirectionsWalk
        "monetization_on", "coin" -> Icons.Default.MonetizationOn
        "directions_bike", "bike" -> Icons.Default.DirectionsBike
        "local_pizza", "pizza" -> Icons.Default.LocalPizza
        "favorite", "heart" -> Icons.Default.Favorite
        "family_restroom", "family" -> Icons.Default.FamilyRestroom
        "person" -> Icons.Default.Person
        "storefront" -> Icons.Default.Storefront
        "laptop_mac", "laptop" -> Icons.Default.LaptopMac
        "subscriptions", "subscription" -> Icons.Default.Subscriptions
        "spa", "beauty", "salon" -> Icons.Default.Spa
        "build", "tools", "repair" -> Icons.Default.Build
        "security", "insurance" -> Icons.Default.Security
        "volunteer_activism", "charity", "donation" -> Icons.Default.VolunteerActivism
        "diamond", "jewelry" -> Icons.Default.Diamond
        "redeem", "reward" -> Icons.Default.Redeem
        "local_pharmacy", "pharmacy" -> Icons.Default.LocalPharmacy
        "lunch_dining" -> Icons.Default.LunchDining
        "delivery_dining" -> Icons.Default.DeliveryDining
        "handshake", "debt", "loan" -> Icons.Default.Handshake
        "paid" -> Icons.Default.Paid
        "lightbulb" -> Icons.Default.Lightbulb
        "bike", "motorcycle", "two_wheeler", "cycling" -> Icons.Default.TwoWheeler
        "shopping", "retail", "store" -> Icons.Default.ShoppingBag
        "fuel", "gas_station", "petrol", "petrol_pump" -> Icons.Default.LocalGasStation
        "phone", "call", "mobile" -> Icons.Default.PhoneAndroid
        "groceries", "grocery", "supermarket", "basket" -> Icons.Default.LocalGroceryStore
        "car", "vehicle", "automobile" -> Icons.Default.DirectionsCar
        "bus", "commute", "transit" -> Icons.Default.DirectionsBus
        "food", "dining", "tea", "cafe" -> Icons.Default.Restaurant
        "wallet", "card" -> Icons.Default.AccountBalanceWallet
        "bills", "utilities", "electricity" -> Icons.Default.Payments
        "rent", "housing", "apartment" -> Icons.Default.Home
        "health", "pharmacy", "medicine" -> Icons.Default.LocalPharmacy
        "education", "books", "study" -> Icons.Default.School
        "entertainment", "movie", "fun" -> Icons.Default.Movie
        "travel", "flight", "trip" -> Icons.Default.Flight
        "gift", "present" -> Icons.Default.CardGiftcard
        "work", "freelance", "salary" -> Icons.Default.Work
        else -> Icons.Default.Category
    }
}

@Composable
fun CategoryIcon(
    iconName: String,
    colorHex: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val tint = try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(tint.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = getIconForName(iconName),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(size * 0.52f)
        )
    }
}

@Composable
fun AvatarBadge(
    initials: String,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    photoUrl: String? = null,
    symbolName: String? = null,
    backgroundColor: Color = Color(0xFF6366F1),
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = backgroundColor,
        onClick = onClick ?: {}
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (!photoUrl.isNullOrBlank()) {
                coil.compose.AsyncImage(
                    model = photoUrl,
                    contentDescription = "Avatar",
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.size(size)
                )
            } else if (!symbolName.isNullOrBlank() && symbolName != "initials") {
                val icon = getIconForName(symbolName)
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.52f)
                )
            } else {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = (size.value * 0.36f).sp
                    )
                )
            }
        }
    }
}
