package com.crous.gateopener.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crous.gateopener.data.GateSettings
import com.crous.gateopener.data.TriggerLog
import com.crous.gateopener.ui.MainViewModel
import com.crous.gateopener.ui.PermissionState
import com.crous.gateopener.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onRequestFineLocation: () -> Unit,
    onRequestBackgroundLocation: () -> Unit,
    onRequestCallPhone: () -> Unit,
    onRequestOverlay: () -> Unit,
    onRequestIgnoreBattery: () -> Unit,
    onRequestNotifications: () -> Unit,
    onOpenOemSettings: () -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val permissions by viewModel.permissions.collectAsState()
    val currentWifi by viewModel.currentWifi.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DoorSliding,
                            contentDescription = null,
                            tint = Teal400,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Portail CROUS",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshState(context) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Rafraîchir")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                HeroStatusCard(
                    settings = settings,
                    permissions = permissions,
                    onToggleGeofence = { active -> viewModel.toggleGeofence(context, active) }
                )
            }

            item {
                PermissionsChecklistCard(
                    permissions = permissions,
                    onRequestFineLocation = onRequestFineLocation,
                    onRequestBackgroundLocation = onRequestBackgroundLocation,
                    onRequestCallPhone = onRequestCallPhone,
                    onRequestOverlay = onRequestOverlay,
                    onRequestIgnoreBattery = onRequestIgnoreBattery,
                    onRequestNotifications = onRequestNotifications,
                    onOpenOemSettings = onOpenOemSettings
                )
            }

            item {
                SpatialConfigCard(
                    settings = settings,
                    onUpdateCoordinates = { lat, lng, radius, resp ->
                        viewModel.updateCoordinates(lat, lng, radius, resp)
                    },
                    onUseCurrentGps = { viewModel.useCurrentGpsLocation(context) }
                )
            }

            item {
                WifiFailSafeCard(
                    settings = settings,
                    currentWifi = currentWifi,
                    onCaptureWifi = { viewModel.captureCurrentWifi(context) },
                    onUpdateWifi = { ssid, bssid -> viewModel.updateWifi(ssid, bssid) }
                )
            }

            item {
                TelephonyCard(
                    settings = settings,
                    onUpdatePhone = { viewModel.updatePhoneNumber(it) },
                    onToggleDryRun = { viewModel.setDryRun(it) },
                    onUpdateCooldown = { viewModel.updateCooldown(it) }
                )
            }

            item {
                TestDeckCard(
                    onSimulateGeofence = { viewModel.simulateTrigger(context) },
                    onTestDirectCall = { viewModel.testDirectCall(context) }
                )
            }

            item {
                LogHistoryCard(
                    logs = logs,
                    onClearLogs = { viewModel.clearLogs() }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun HeroStatusCard(
    settings: GateSettings,
    permissions: PermissionState,
    onToggleGeofence: (Boolean) -> Unit
) {
    val isArmed = settings.isGeofenceActive
    val now = System.currentTimeMillis()
    val elapsed = now - settings.lastCallTimestampMs
    val cooldownMs = settings.cooldownMinutes * 60 * 1000L
    val isInCooldown = elapsed < cooldownMs && settings.lastCallTimestampMs > 0
    val remainingMin = if (isInCooldown) ((cooldownMs - elapsed) / 60000L) + 1 else 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ÉTAT DU SYSTÈME",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate400,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isArmed) Emerald500 else Slate400)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isArmed) "ARMÉ & ACTIF" else "DÉSARMÉ",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isArmed) Emerald500 else Slate400
                        )
                    }
                }

                Switch(
                    checked = isArmed,
                    onCheckedChange = onToggleGeofence,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Slate900,
                        checkedTrackColor = Emerald500
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Badges complémentaires
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (settings.isDryRunMode) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Mode Test Dry-Run", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Science, contentDescription = null, tint = Amber500, modifier = Modifier.size(16.dp))
                        },
                        colors = AssistChipDefaults.assistChipColors(labelColor = Amber500)
                    )
                }

                if (isInCooldown) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Cooldown : $remainingMin min", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.HourglassTop, contentDescription = null, tint = Amber500, modifier = Modifier.size(16.dp))
                        },
                        colors = AssistChipDefaults.assistChipColors(labelColor = Amber500)
                    )
                } else {
                    AssistChip(
                        onClick = {},
                        label = { Text("Prêt à déclencher", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Teal400, modifier = Modifier.size(16.dp))
                        },
                        colors = AssistChipDefaults.assistChipColors(labelColor = Teal400)
                    )
                }
            }

            if (!permissions.isAllCriticalGranted) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "⚠️ Certaines permissions critiques sont manquantes. Consultez la checklist ci-dessous.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Rose500,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun PermissionsChecklistCard(
    permissions: PermissionState,
    onRequestFineLocation: () -> Unit,
    onRequestBackgroundLocation: () -> Unit,
    onRequestCallPhone: () -> Unit,
    onRequestOverlay: () -> Unit,
    onRequestIgnoreBattery: () -> Unit,
    onRequestNotifications: () -> Unit,
    onOpenOemSettings: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Permissions & Résilience",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (permissions.isAllCriticalGranted) "100% Configuré" else "Action requise",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (permissions.isAllCriticalGranted) Emerald500 else Rose500,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            PermissionRow(
                title = "Localisation Haute Précision",
                subtitle = "Position GPS et détection du BSSID Wi-Fi",
                isGranted = permissions.hasFineLocation,
                onRequest = onRequestFineLocation
            )

            PermissionRow(
                title = "Localisation Arrière-Plan (\"Toujours\")",
                subtitle = "Réveil matériel par Google Play Services Geofencing",
                isGranted = permissions.hasBackgroundLocation,
                onRequest = onRequestBackgroundLocation
            )

            PermissionRow(
                title = "Téléphonie Directe (CALL_PHONE)",
                subtitle = "Appel transparent sans ouvrir le composeur",
                isGranted = permissions.hasCallPhone,
                onRequest = onRequestCallPhone
            )

            PermissionRow(
                title = "Afficher sur les applications (BAL)",
                subtitle = "Autorise le déclenchement écran éteint / verrouillé",
                isGranted = permissions.hasOverlayPermission,
                onRequest = onRequestOverlay
            )

            PermissionRow(
                title = "Exemption Doze Mode",
                subtitle = "Évite l'endormissement du récepteur par Android",
                isGranted = permissions.isIgnoringBatteryOptimizations,
                onRequest = onRequestIgnoreBattery
            )

            PermissionRow(
                title = "Notifications d'état",
                subtitle = "Affichage de la confirmation d'appel (Android 13+)",
                isGranted = permissions.hasPostNotifications,
                onRequest = onRequestNotifications
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onOpenOemSettings,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.BatteryChargingFull, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Paramètres batterie constructeur (MIUI / Samsung)", fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    subtitle: String,
    isGranted: Boolean,
    onRequest: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = Slate400)
        }
        Spacer(modifier = Modifier.width(12.dp))
        if (isGranted) {
            FilledTonalButton(
                onClick = {},
                enabled = false,
                colors = ButtonDefaults.filledTonalButtonColors(
                    disabledContainerColor = Emerald500.copy(alpha = 0.15f),
                    disabledContentColor = Emerald500
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("OK", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Button(
                onClick = onRequest,
                colors = ButtonDefaults.buttonColors(containerColor = Rose500),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text("Activer", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SpatialConfigCard(
    settings: GateSettings,
    onUpdateCoordinates: (Double, Double, Float, Int) -> Unit,
    onUseCurrentGps: () -> Unit
) {
    var latText by remember(settings.geofenceLat) { mutableStateOf(settings.geofenceLat.toString()) }
    var lngText by remember(settings.geofenceLng) { mutableStateOf(settings.geofenceLng.toString()) }
    var radius by remember(settings.geofenceRadius) { mutableFloatStateOf(settings.geofenceRadius) }
    var responsiveness by remember(settings.responsivenessMs) { mutableIntStateOf(settings.responsivenessMs) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Calibration Spatiale Décentrée",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onUseCurrentGps) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Capturer GPS", tint = Teal400)
                }
            }

            Text(
                text = "Centre du geofence positionné en amont sur la rue (~120 m avant la grille) pour éviter les faux positifs de l'appartement à 65 m.",
                style = MaterialTheme.typography.bodyMedium,
                color = Slate400,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = latText,
                    onValueChange = {
                        latText = it
                        it.toDoubleOrNull()?.let { lat ->
                            onUpdateCoordinates(lat, settings.geofenceLng, radius, responsiveness)
                        }
                    },
                    label = { Text("Latitude") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = lngText,
                    onValueChange = {
                        lngText = it
                        it.toDoubleOrNull()?.let { lng ->
                            onUpdateCoordinates(settings.geofenceLat, lng, radius, responsiveness)
                        }
                    },
                    label = { Text("Longitude") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Rayon de déclenchement", style = MaterialTheme.typography.bodyMedium)
                Text("${radius.toInt()} m", fontWeight = FontWeight.Bold, color = Teal400)
            }
            Slider(
                value = radius,
                onValueChange = {
                    radius = it
                    onUpdateCoordinates(settings.geofenceLat, settings.geofenceLng, radius, responsiveness)
                },
                valueRange = 40f..200f,
                steps = 15
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Réactivité Play Services", style = MaterialTheme.typography.bodyMedium)
                Text("${responsiveness} ms", fontWeight = FontWeight.Bold, color = Teal400)
            }
            Slider(
                value = responsiveness.toFloat(),
                onValueChange = {
                    responsiveness = it.toInt()
                    onUpdateCoordinates(settings.geofenceLat, settings.geofenceLng, radius, responsiveness)
                },
                valueRange = 1000f..10000f,
                steps = 8
            )
        }
    }
}

@Composable
private fun WifiFailSafeCard(
    settings: GateSettings,
    currentWifi: Pair<String?, String?>,
    onCaptureWifi: () -> Unit,
    onUpdateWifi: (String, String) -> Unit
) {
    var ssidText by remember(settings.homeWifiSsid) { mutableStateOf(settings.homeWifiSsid) }
    var bssidText by remember(settings.homeWifiBssid) { mutableStateOf(settings.homeWifiBssid) }

    val (currentSsid, currentBssid) = currentWifi
    val isCurrentlyBlocked = (currentBssid != null && currentBssid.equals(settings.homeWifiBssid, ignoreCase = true)) ||
            (currentSsid != null && currentSsid.equals(settings.homeWifiSsid, ignoreCase = true))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Verrou Anti-Faux Positifs (Wi-Fi)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (isCurrentlyBlocked) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Verrou Actif (Logement)", fontSize = 11.sp) },
                        colors = AssistChipDefaults.assistChipColors(labelColor = Amber500)
                    )
                }
            }

            Text(
                text = "Interdit l'appel si l'appareil est connecté à ce SSID ou BSSID (adresse MAC physique de la box dans le logement).",
                style = MaterialTheme.typography.bodyMedium,
                color = Slate400,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = ssidText,
                onValueChange = {
                    ssidText = it
                    onUpdateWifi(it, bssidText)
                },
                label = { Text("SSID Wi-Fi Logement (ex: Freebox-A45)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = bssidText,
                onValueChange = {
                    bssidText = it
                    onUpdateWifi(ssidText, it)
                },
                label = { Text("BSSID Wi-Fi (ex: 34:27:92:aa:bb:cc)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onCaptureWifi,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.WifiFind, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Capturer le Wi-Fi actuel : ${currentSsid ?: "Non connecté"}", fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun TelephonyCard(
    settings: GateSettings,
    onUpdatePhone: (String) -> Unit,
    onToggleDryRun: (Boolean) -> Unit,
    onUpdateCooldown: (Int) -> Unit
) {
    var phoneText by remember(settings.targetPhoneNumber) { mutableStateOf(settings.targetPhoneNumber) }
    var cooldown by remember(settings.cooldownMinutes) { mutableIntStateOf(settings.cooldownMinutes) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Téléphonie & Anti-Spam",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = phoneText,
                onValueChange = {
                    phoneText = it
                    onUpdatePhone(it)
                },
                label = { Text("Numéro du portail CROUS") },
                placeholder = { Text("0600000000") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Teal400) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Cooldown logiciel (Hystérésis)", style = MaterialTheme.typography.bodyMedium)
                Text("$cooldown min", fontWeight = FontWeight.Bold, color = Teal400)
            }
            Slider(
                value = cooldown.toFloat(),
                onValueChange = {
                    cooldown = it.toInt()
                    onUpdateCooldown(cooldown)
                },
                valueRange = 5f..60f,
                steps = 10
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Mode Test / Dry-Run", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("Journalise et simule l'appel sans composer le numéro", style = MaterialTheme.typography.labelSmall, color = Slate400)
                }
                Switch(
                    checked = settings.isDryRunMode,
                    onCheckedChange = onToggleDryRun,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Slate900,
                        checkedTrackColor = Amber500
                    )
                )
            }
        }
    }
}

@Composable
private fun TestDeckCard(
    onSimulateGeofence: () -> Unit,
    onTestDirectCall: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Simulation & Validation Locale",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Teste la chaîne complète de décision (Wi-Fi, cooldown, appel) sans bouger de son bureau.",
                style = MaterialTheme.typography.bodyMedium,
                color = Slate400,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onSimulateGeofence,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Teal600)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Simuler Geofence", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onTestDirectCall,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.PhoneCallback, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tester Appel", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun LogHistoryCard(
    logs: List<TriggerLog>,
    onClearLogs: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Historique & Diagnostics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (logs.isNotEmpty()) {
                    TextButton(onClick = onClearLogs) {
                        Text("Effacer", fontSize = 12.sp, color = Slate400)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (logs.isEmpty()) {
                Text(
                    text = "Aucun événement enregistré pour le moment.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate400,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    logs.take(10).forEach { log ->
                        LogRow(log)
                    }
                }
            }
        }
    }
}

@Composable
private fun LogRow(log: TriggerLog) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Slate800.copy(alpha = 0.5f))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (log.success) Emerald500 else Rose500)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = log.type, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Slate200)
                Text(text = log.formattedTime, style = MaterialTheme.typography.labelSmall, color = Slate400)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = log.reason, style = MaterialTheme.typography.bodyMedium, fontSize = 12.sp, color = Slate400)
        }
    }
}
