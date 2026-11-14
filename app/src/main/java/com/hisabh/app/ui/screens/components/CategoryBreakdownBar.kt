package com.hisabh.app.ui.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hisabh.app.ui.theme.CreamBorder
import com.hisabh.app.ui.theme.CreamCard
import com.hisabh.app.ui.theme.CreamSurface
import com.hisabh.app.ui.theme.ForestGreenPrimary
import com.hisabh.app.ui.theme.TextMuted
import com.hisabh.app.ui.theme.TextPrimary
import com.hisabh.app.ui.theme.TextSecondary
import com.hisabh.app.ui.viewmodel.CategorySpend
import com.hisabh.app.util.CurrencyFormatter
import java.util.Locale

@Composable
fun CategoryBreakdownBar(
    categorySpends: List<CategorySpend>,
    totalSpent: Double,
    modifier: Modifier = Modifier
) {
    if (categorySpends.isEmpty()) {
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CreamSurface)
            .border(1.dp, CreamBorder, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Spending Breakdown",
                color = ForestGreenPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${CurrencyFormatter.formatRupees(totalSpent)} total",
                color = TextSecondary,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Segmented Horizontal Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(CreamCard)
        ) {
            categorySpends.forEach { spend ->
                if (spend.percentage > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(spend.percentage.coerceAtLeast(1f))
                            .height(8.dp)
                            .background(spend.color)
                    )
                    Spacer(modifier = Modifier.width(1.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Legend Grid (Top 4 categories)
        val displayCategories = categorySpends.take(4)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (i in displayCategories.indices step 2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Item 1
                    val item1 = displayCategories[i]
                    LegendItem(item = item1, modifier = Modifier.weight(1f))

                    // Item 2 (if exists)
                    if (i + 1 < displayCategories.size) {
                        val item2 = displayCategories[i + 1]
                        LegendItem(item = item2, modifier = Modifier.weight(1f))
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(
    item: CategorySpend,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(item.color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = item.categoryName,
            color = TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "${String.format(Locale.US, "%.0f", item.percentage)}%",
            color = TextMuted,
            fontSize = 10.sp
        )
    }
}

