package com.example.tvremote

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.InetAddress

private val Ink = Color(0xFF0D0F14)
private val Panel = Color(0xFF191D26)
private val Accent = Color(0xFF9FC5FF)
private val Muted = Color(0xFF9AA4B5)

 data class TvDevice(
    val name: String,
    val host: InetAddress?,
    val port: Int,
    val serviceType: String,
    val serviceName: String
) {
    val address: String get() = host?.hostAddress ?: "Address pending"
    val protocolLabel: String get() = if (serviceType.contains("mediaremotetv")) "Media Remote" else "Companion Link"
    val key: String get() = "$serviceName|$address|$port|$serviceType"
}

class MainActivity : ComponentActivity() {
    private lateinit var nsdManager: NsdManager
    private lateinit var wifiLock: WifiManager.MulticastLock
    private val foundDevices = mutableStateListOf<TvDevice>()
    private val activeListeners = mutableListOf<Pair<String, NsdManager.DiscoveryListener>>()
    private var scanning by mutableStateOf(false)
    private var scanMessage by mutableStateOf("Ready to scan your local Wi-Fi network")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nsdManager = getSystemService(Context.NSD_SERVICE) as NsdManager
        val wifi = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiLock = wifi.createMulticastLock("tvremote-mdns").apply { setReferenceCounted(false) }
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Accent,
                onPrimary = Ink,
                background = Ink,
                surface = Panel,
                onBackground = Color(0xFFF4F6FA),
                onSurface = Color(0xFFF4F6FA),
                secondary = Muted
            )) {
                RemoteApp(
                    devices = foundDevices,
                    scanning = scanning,
                    scanMessage = scanMessage,
                    onScan = { startDiscovery() }
                )
            }
        }
    }

    private fun startDiscovery() {
        stopDiscovery()
        foundDevices.clear()
        scanning = true
        scanMessage = "Searching for Apple TV services…"
        try {
            if (!wifiLock.isHeld) wifiLock.acquire()
        } catch (_: Exception) { }
        listOf("_mediaremotetv._tcp.", "_companion-link._tcp.").forEach { type ->
            val listener = object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(serviceType: String) { }
                override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                    if (!serviceInfo.serviceType.contains(type.removeSuffix("."), ignoreCase = true)) return
                    try {
                        @Suppress("DEPRECATION")
                        nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                            override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) { }
                            override fun onServiceResolved(info: NsdServiceInfo) {
                                val device = TvDevice(
                                    name = info.serviceName.ifBlank { "Apple TV" },
                                    host = info.host,
                                    port = info.port,
                                    serviceType = info.serviceType,
                                    serviceName = info.serviceName
                                )
                                runOnUiThread {
                                    if (foundDevices.none { it.key == device.key }) foundDevices.add(device)
                                    scanMessage = if (foundDevices.isEmpty()) "Searching for Apple TV services…" else "Found ${foundDevices.size} service${if (foundDevices.size == 1) "" else "s"}"
                                }
                            }
                        })
                    } catch (_: Exception) { }
                }
                override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                    runOnUiThread { foundDevices.removeAll { it.serviceName == serviceInfo.serviceName && it.serviceType == serviceInfo.serviceType } }
                }
                override fun onDiscoveryStopped(serviceType: String) { }
                override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                    try { nsdManager.stopServiceDiscovery(this) } catch (_: Exception) { }
                }
                override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                    try { nsdManager.stopServiceDiscovery(this) } catch (_: Exception) { }
                }
            }
            try {
                nsdManager.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, listener)
                activeListeners.add(type to listener)
            } catch (_: Exception) { }
        }
        window.decorView.postDelayed({
            if (scanning) {
                scanning = false
                scanMessage = if (foundDevices.isEmpty()) "No compatible services found. Check that your phone and Apple TV share the same Wi-Fi." else "Found ${foundDevices.size} service${if (foundDevices.size == 1) "" else "s"}"
                stopDiscovery(keepStatus = true)
            }
        }, 12000)
    }

    private fun stopDiscovery(keepStatus: Boolean = false) {
        activeListeners.toList().forEach { (_, listener) ->
            try { nsdManager.stopServiceDiscovery(listener) } catch (_: Exception) { }
        }
        activeListeners.clear()
        scanning = false
        try { if (wifiLock.isHeld) wifiLock.release() } catch (_: Exception) { }
        if (!keepStatus && foundDevices.isEmpty()) scanMessage = "Ready to scan your local Wi-Fi network"
    }

    override fun onDestroy() {
        stopDiscovery()
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemoteApp(
    devices: List<TvDevice>,
    scanning: Boolean,
    scanMessage: String,
    onScan: () -> Unit
) {
    var selected by remember { mutableStateOf<TvDevice?>(null) }
    var showPairing by remember { mutableStateOf(false) }
    var showKeyboard by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }
    var typedText by remember { mutableStateOf("") }
    var darkMode by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }

    Scaffold(
        containerColor = Ink,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("TV Remote", fontWeight = FontWeight.Bold, fontSize = 21.sp)
                        Text("Apple TV companion", color = Muted, fontSize = 12.sp)
                    }
                },
                navigationIcon = {
                    if (selected != null) IconButton(onClick = { selected = null }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    } else Icon(Icons.Default.Tv, contentDescription = null, modifier = Modifier.padding(start = 16.dp), tint = Accent)
                },
                actions = {
                    IconButton(onClick = onScan) { Icon(Icons.Default.Refresh, contentDescription = "Rescan") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Ink)
            )
        },
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)) {
            if (selected == null) {
                Spacer(Modifier.height(10.dp))
                Text("Your devices", fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Text("Discover compatible Apple TV services on your network.", color = Muted, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp, bottom = 18.dp))
                Button(onClick = onScan, enabled = !scanning, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) {
                    if (scanning) CircularProgressIndicator(Modifier.size(19.dp), strokeWidth = 2.dp, color = Ink)
                    else Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(Modifier.width(9.dp))
                    Text(if (scanning) "Scanning network…" else "Find Apple TV")
                }
                Text(scanMessage, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 12.dp))
                if (devices.isEmpty()) {
                    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Tv, contentDescription = null, tint = Accent, modifier = Modifier.size(42.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("No devices yet", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                            Text("Keep your Apple TV awake and connect both devices to the same Wi-Fi network, then scan again.", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                } else {
                    devices.distinctBy { it.key }.forEach { device ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable { selected = device },
                            colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)
                        ) {
                            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(48.dp).background(Color(0xFF293447), CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Tv, contentDescription = null, tint = Accent, modifier = Modifier.size(25.dp))
                                }
                                Spacer(Modifier.width(13.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(device.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                    Text("${device.address}:${device.port}", color = Muted, fontSize = 12.sp)
                                    Text(device.protocolLabel, color = Accent, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                                }
                                Icon(Icons.Default.ArrowForward, contentDescription = "Open device", tint = Muted)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Discovery uses Bonjour / DNS-SD. Seeing a device does not mean it is paired yet.", color = Muted, fontSize = 12.sp)
            } else {
                RemoteScreen(
                    device = selected!!,
                    onPair = { showPairing = true },
                    onKeyboard = { showKeyboard = true },
                    onCommand = { command -> scope.launch { snackbarHostState.showSnackbar("$command needs the Apple TV secure-control protocol, which is not implemented in this prototype.") } }
                )
            }
        }
    }

    if (showPairing) {
        AlertDialog(
            onDismissRequest = { showPairing = false },
            title = { Text("Pair with ${selected?.name ?: "Apple TV"}") },
            text = {
                Column {
                    Text("Enter the code shown on your TV. This prototype does not yet send the secure pairing exchange, so entering a code will not pair the device.", color = Muted)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = pin, onValueChange = { pin = it.filter(Char::isDigit).take(6) }, label = { Text("TV pairing code") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                }
            },
            confirmButton = { TextButton(onClick = { showPairing = false; scope.launch { snackbarHostState.showSnackbar("Pairing protocol is not implemented yet.") } }) { Text("Continue") } },
            dismissButton = { TextButton(onClick = { showPairing = false }) { Text("Cancel") } }
        )
    }
    if (showKeyboard) {
        AlertDialog(
            onDismissRequest = { showKeyboard = false },
            title = { Text("Phone keyboard") },
            text = {
                Column {
                    Text("Type text here. Sending text to Apple TV requires an authenticated protocol session, not included yet.", color = Muted)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = typedText, onValueChange = { typedText = it }, label = { Text("Text to send") })
                }
            },
            confirmButton = { TextButton(onClick = { showKeyboard = false; scope.launch { snackbarHostState.showSnackbar("Keyboard text is not sent in this prototype.") } }) { Text("Done") } },
            dismissButton = { TextButton(onClick = { showKeyboard = false }) { Text("Close") } }
        )
    }
}

@Composable
private fun RemoteScreen(device: TvDevice, onPair: () -> Unit, onKeyboard: () -> Unit, onCommand: (String) -> Unit) {
    Spacer(Modifier.height(4.dp))
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Tv, contentDescription = null, tint = Accent, modifier = Modifier.size(35.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(device.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text("${device.address}:${device.port}", color = Muted, fontSize = 12.sp)
                Text("Discovered · Not connected", color = Color(0xFFFFD18A), fontSize = 12.sp)
            }
            OutlinedButton(onClick = onPair, shape = RoundedCornerShape(12.dp)) { Text("Pair") }
        }
    }
    Spacer(Modifier.height(18.dp))
    Text("REMOTE", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
    Spacer(Modifier.height(10.dp))
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(100.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            RoundControl(Icons.Default.ArrowUpward, "Up") { onCommand("Up") }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                RoundControl(Icons.Default.ArrowBack, "Left") { onCommand("Left") }
                Spacer(Modifier.width(20.dp))
                Button(onClick = { onCommand("Select") }, modifier = Modifier.size(94.dp), shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink)) {
                    Text("SELECT", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(20.dp))
                RoundControl(Icons.Default.ArrowForward, "Right") { onCommand("Right") }
            }
            RoundControl(Icons.Default.ArrowDownward, "Down") { onCommand("Down") }
        }
    }
    Spacer(Modifier.height(16.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        RoundControl(Icons.Default.ArrowBack, "Back") { onCommand("Back") }
        RoundControl(Icons.Default.Home, "Home") { onCommand("Home") }
        RoundControl(Icons.Default.Keyboard, "Keyboard") { onKeyboard() }
        RoundControl(Icons.Default.PowerSettingsNew, "Power") { onCommand("Power") }
    }
    Spacer(Modifier.height(14.dp))
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            RoundControl(Icons.Default.SkipPrevious, "Previous") { onCommand("Previous") }
            RoundControl(Icons.Default.PlayArrow, "Play") { onCommand("Play") }
            RoundControl(Icons.Default.Pause, "Pause") { onCommand("Pause") }
            RoundControl(Icons.Default.SkipNext, "Next") { onCommand("Next") }
        }
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            RoundControl(Icons.Default.VolumeDown, "Vol −") { onCommand("Volume down") }
            RoundControl(Icons.Default.VolumeUp, "Vol +") { onCommand("Volume up") }
        }
    }
    Spacer(Modifier.height(12.dp))
    Text("Controls are UI-only until secure pairing and command transport are added.", color = Muted, fontSize = 12.sp)
}

@Composable
private fun RoundControl(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 3.dp, vertical = 3.dp).clickable(onClick = onClick)) {
        Surface(shape = CircleShape, color = Color(0xFF293447), modifier = Modifier.size(48.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = label, tint = Color(0xFFF4F6FA), modifier = Modifier.size(22.dp)) }
        }
        Text(label, color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
    }
}
