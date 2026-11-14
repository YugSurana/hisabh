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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hisabh.app.ui.theme.CreamBackground
import com.hisabh.app.ui.theme.CreditPine
import com.hisabh.app.ui.theme.DebitTerracotta
import com.hisabh.app.ui.theme.ForestGreenLight
import com.hisabh.app.ui.theme.ForestGreenPrimary
import com.hisabh.app.util.CurrencyFormatter

@Composable
fun SummaryCards(
    totalSpent: Double,
    totalReceived: Double,
    netBalance: Double,
    debitCount: Int,
    creditCount: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(ForestGreenPrimary)
            .padding(20.dp)
    ) {
        // Top Row: Net Balance Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = "NET MONTHLY SAVINGS",
                    color = Color(0xFFA5C0B1),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = CurrencyFormatter.formatRupees(netBalance),
                    color = CreamBackground,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
            }

            // Sync Status Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(ForestGreenLight)
                    .border(1.dp, Color(0xFF38664D), RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "HDFC Auto-Sync",
                    color = Color(0xFFC7E5D3),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.8.dp)
                .background(Color(0xFF28543C))
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Bottom Row: Spent vs Received Split
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Total Spent (Debit)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "TOTAL SPENT",
                    color = Color(0xFFA5C0B1),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = CurrencyFormatter.formatRupees(totalSpent),
                    color = CreamBackground,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$debitCount expenses",
                    color = Color(0xFF869E90),
                    fontSize = 10.sp
                )
            }

            // Total Received (Credit)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "TOTAL RECEIVED",
                    color = Color(0xFFA5C0B1),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = CurrencyFormatter.formatRupees(totalReceived),
                    color = Color(0xFF8CE3B1),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$creditCount credits",
                    color = Color(0xFF869E90),
                    fontSize = 10.sp
                )
            }
        }
    }
}

