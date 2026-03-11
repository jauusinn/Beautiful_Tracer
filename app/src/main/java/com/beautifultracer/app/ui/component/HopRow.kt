package com.beautifultracer.app.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beautifultracer.app.domain.model.HopNode
import com.beautifultracer.app.ui.theme.MonoFamily
import com.beautifultracer.app.ui.theme.PingBad
import com.beautifultracer.app.ui.theme.PingExcellent
import com.beautifultracer.app.ui.theme.PingFair
import com.beautifultracer.app.ui.theme.PingGood
import com.beautifultracer.app.ui.theme.PingPoor
import com.beautifultracer.app.ui.theme.PingTimeout

/**
 * A row component that displays information about a single traceroute hop.
 * @param hop The hop data to display.
 * @param onClick Callback when the hop card is clicked.
 */
@Composable
fun HopRow(
    hop: HopNode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Custom colors for destination
    val destBgColor = Color(0xFFFFB3BA)
    val destFontColor = Color(0xFFB22234) // Fixed typo from user: B222234 -> B22234

    val pingColor by animateColorAsState(
        targetValue = if (hop.isDestination) destFontColor else getPingColor(hop.latencyMs, hop.isTimeout),
        animationSpec = tween(300),
        label = "pingColor"
    )

    val lossProgress by animateFloatAsState(
        targetValue = hop.packetLoss / 100f,
        animationSpec = tween(500),
        label = "lossProgress"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(enabled = !hop.isTimeout) { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (hop.isDestination) destBgColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (hop.isDestination) 4.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hop number badge
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (hop.isDestination) destFontColor else MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${hop.hopNumber}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (hop.isDestination) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Flag emoji
            Text(
                text = if (hop.isTimeout) "⏱" else hop.flagEmoji.ifBlank { "🌐" },
                fontSize = 20.sp,
                modifier = Modifier.width(30.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // IP address and hostname
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (hop.isDestination && !hop.displayAddress.isNullOrBlank()) {
                        hop.displayAddress
                    } else {
                        hop.ipAddress
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = MonoFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (hop.isDestination) destFontColor else if (hop.isTimeout) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!hop.hostname.isNullOrBlank()) {
                    Text(
                        text = hop.hostname,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (hop.isDestination) destFontColor.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Ping stats column
            if (!hop.isTimeout) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Latency badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (hop.isDestination) destFontColor.copy(alpha = 0.1f) else pingColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (hop.latencyMs >= 0) {
                                "${String.format("%.1f", hop.latencyMs)} ms"
                            } else {
                                "— ms"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = MonoFamily,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = if (hop.isDestination) destFontColor else pingColor
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Packet loss bar
                    if (hop.packetsSent > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "${String.format("%.0f", hop.packetLoss)}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (hop.isDestination) destFontColor else if (hop.packetLoss > 5f) {
                                    PingBad
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            LinearProgressIndicator(
                                progress = { lossProgress },
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = if (hop.isDestination) destFontColor else if (hop.packetLoss > 10f) PingBad else PingGood,
                                trackColor = if (hop.isDestination) destFontColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Determines the color of the ping badge based on latency.
 */
private fun getPingColor(latencyMs: Float, isTimeout: Boolean): androidx.compose.ui.graphics.Color {
    if (isTimeout || latencyMs < 0) return PingTimeout
    return when {
        latencyMs < 20f -> PingExcellent
        latencyMs < 50f -> PingGood
        latencyMs < 100f -> PingFair
        latencyMs < 200f -> PingPoor
        else -> PingBad
    }
}
