package com.beautifultracer.app.ui.component

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.beautifultracer.app.domain.model.WhoisInfo
import com.beautifultracer.app.ui.theme.MonoFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhoisBottomSheet(
    sheetState: SheetState,
    ipAddress: String,
    whoisInfo: WhoisInfo?,
    isLoading: Boolean,
    error: String?,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Text(
                text = "IP Details",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = ipAddress,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonoFamily),
                color = MaterialTheme.colorScheme.primary
            )

            // Cache indicator
            if (whoisInfo?.isCached == true) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Cached,
                        contentDescription = "Cached",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Loaded from cache",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(16.dp))

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                error != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                whoisInfo != null -> {
                    WhoisDetailRow(
                        icon = Icons.Default.Wifi,
                        label = "ISP",
                        value = whoisInfo.isp
                    )
                    WhoisDetailRow(
                        icon = Icons.Default.Business,
                        label = "Organization",
                        value = whoisInfo.org
                    )
                    WhoisDetailRow(
                        icon = Icons.Default.LocationCity,
                        label = "City",
                        value = whoisInfo.city
                    )
                    WhoisDetailRow(
                        icon = Icons.Default.Public,
                        label = "Region",
                        value = whoisInfo.region
                    )
                    WhoisDetailRow(
                        icon = Icons.Default.Cloud,
                        label = "Country",
                        value = whoisInfo.country
                    )
                    WhoisDetailRow(
                        icon = Icons.Default.Router,
                        label = "ASN",
                        value = buildString {
                            if (!whoisInfo.asn.isNullOrBlank()) append(whoisInfo.asn)
                            if (!whoisInfo.asName.isNullOrBlank()) {
                                if (isNotEmpty()) append(" · ")
                                append(whoisInfo.asName)
                            }
                        }.ifBlank { null }
                    )
                }
            }
        }
    }
}

@Composable
private fun WhoisDetailRow(
    icon: ImageVector,
    label: String,
    value: String?,
    modifier: Modifier = Modifier
) {
    if (value.isNullOrBlank()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
