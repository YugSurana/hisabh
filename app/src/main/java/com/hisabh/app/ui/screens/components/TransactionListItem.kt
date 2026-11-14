package com.hisabh.app.ui.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hisabh.app.data.local.entity.TransactionEntity
import com.hisabh.app.data.model.Category
import com.hisabh.app.data.model.TransactionType
import com.hisabh.app.ui.theme.CreamBorder
import com.hisabh.app.ui.theme.CreamCard
import com.hisabh.app.ui.theme.CreamSurface
import com.hisabh.app.ui.theme.CreditPine
import com.hisabh.app.ui.theme.CreditPineContainer
import com.hisabh.app.ui.theme.DebitTerracotta
import com.hisabh.app.ui.theme.DebitTerracottaContainer
import com.hisabh.app.ui.theme.ForestGreenPrimary
import com.hisabh.app.ui.theme.TextMuted
import com.hisabh.app.ui.theme.TextPrimary
import com.hisabh.app.ui.theme.TextSecondary
import com.hisabh.app.util.CurrencyFormatter
import com.hisabh.app.util.DateUtils

@Composable
fun TransactionListItem(
    transaction: TransactionEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDebit = transaction.txType == TransactionType.DEBIT.name
    val icon = getCategoryIcon(transaction.category)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CreamSurface)
            .border(1.dp, CreamBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side: Category Icon + Merchant & Category Pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDebit) CreamCard else CreditPineContainer)
                    .border(1.dp, if (isDebit) CreamBorder else Color(0xFFC7E3D1), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = transaction.category,
                    tint = if (isDebit) ForestGreenPrimary else CreditPine,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = transaction.displayName,
                    color = TextPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(CreamCard)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = transaction.category,
                            color = ForestGreenPrimary,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "• " + DateUtils.formatTime(transaction.timestamp),
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Right side: Amount + UTR / Manual badge
        Column(horizontalAlignment = Alignment.End) {
            val prefix = if (isDebit) "-" else "+"
            Text(
                text = prefix + CurrencyFormatter.formatRupees(transaction.amount),
                color = if (isDebit) DebitTerracotta else CreditPine,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(2.dp))

            val refText = if (transaction.isManual) {
                "Cash Entry"
            } else if (transaction.utrNumber.length == 12) {
                "Ref ${transaction.utrNumber.takeLast(6)}"
            } else {
                "SMS Tracked"
            }

            Text(
                text = refText,
                color = TextMuted,
                fontSize = 9.5.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

private fun getCategoryIcon(categoryName: String): ImageVector {
    val cat = Category.fromTitle(categoryName)
    return when (cat) {
        Category.FOOD_AND_DINING -> Icons.Default.Restaurant
        Category.TRAVEL_AND_FUEL -> Icons.Default.DirectionsCar
        Category.GROCERIES -> Icons.Default.ShoppingCart
        Category.BILLS_AND_RECHARGE -> Icons.Default.ReceiptLong
        Category.SHOPPING -> Icons.Default.ShoppingBag
        Category.ENTERTAINMENT -> Icons.Default.Movie
        Category.TRANSFER_AND_FRIENDS -> Icons.Default.SwapHoriz
        Category.POCKET_MONEY_RECEIVED -> Icons.Default.AccountBalanceWallet
        Category.OTHER -> Icons.Default.Category
    }
}
