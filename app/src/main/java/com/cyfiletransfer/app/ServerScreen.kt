package com.cyfiletransfer.app

import android.graphics.Bitmap
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ServerScreen(onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val appContext = LocalContext.current
    val scope = rememberCoroutineScope()

    var isRunning by remember { mutableStateOf(LocalHttpServer.isRunning()) }
    var isStarting by remember { mutableStateOf(false) }
    var ipAddress by remember {
        mutableStateOf(if (isRunning) LocalHttpServer.getLocalIpAddress() else null)
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            val running = withContext(Dispatchers.IO) {
                LocalHttpServer.isRunning()
            }

            isRunning = running
            ipAddress = if (running) {
                LocalHttpServer.getLocalIpAddress()
            } else {
                null
            }

            kotlinx.coroutines.delay(1000)
        }
    }

    val url = ipAddress?.let { "http://$it:${LocalHttpServer.getPort()}" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("← Назад")
            }

            Spacer(Modifier.weight(1f))

            Text(
                "CyFileTransfer",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = colors.onBackground
            )
        }

        Text(
            "Подключение к устройству",
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold,
            color = colors.onBackground,
            textAlign = TextAlign.Center
        )

        if (isRunning && url != null) {
            Text(
                "Сервер запущен",
                color = colors.primary,
                fontWeight = FontWeight.SemiBold
            )

            val qrBitmap = remember(url) {
                val matrix = QRCodeWriter().encode(
                    url,
                    BarcodeFormat.QR_CODE,
                    512,
                    512
                )

                Bitmap.createBitmap(512, 512, Bitmap.Config.RGB_565).also { bitmap ->
                    for (x in 0 until 512) {
                        for (y in 0 until 512) {
                            bitmap.setPixel(
                                x,
                                y,
                                if (matrix[x, y]) android.graphics.Color.BLACK
                                else android.graphics.Color.WHITE
                            )
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "QR-код для подключения к CyFileTransfer",
                    modifier = Modifier
                        .size(220.dp)
                        .padding(10.dp)
                )
            }

            Text(
                "Отсканируйте QR-код камерой телефона",
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Text(
                url,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = colors.primary,
                textAlign = TextAlign.Center
            )

            Text(
                "Откройте этот адрес в браузере устройства, подключённого к той же сети Wi-Fi.",
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Button(
                onClick = {
                    isStarting = true
                    scope.launch {
                        val stopIntent = Intent(appContext, FileServerService::class.java).apply {
                            action = FileServerService.ACTION_STOP
                        }
                        appContext.startService(stopIntent)
                        isRunning = false
                        ipAddress = null
                        isStarting = false
                    }
                },
                enabled = !isStarting,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Остановить сервер")
            }
        } else {
            Text(
                if (isStarting) "Запуск сервера..." else "Сервер остановлен",
                color = colors.onSurfaceVariant
            )

            errorMessage?.let {
                Text(it, color = colors.error, textAlign = TextAlign.Center)
            }

            Button(
                onClick = {
                    isStarting = true
                    errorMessage = null

                    scope.launch {
                        try {
                            val serviceIntent = Intent(appContext, FileServerService::class.java)
                            androidx.core.content.ContextCompat.startForegroundService(
                                appContext,
                                serviceIntent
                            )

                            kotlinx.coroutines.delay(700)

                            isRunning = LocalHttpServer.isRunning()
                            ipAddress = LocalHttpServer.getLocalIpAddress()

                            if (!isRunning) {
                                errorMessage = "Не удалось запустить сервер. Проверьте порт 1111."
                            } else if (ipAddress == null) {
                                errorMessage = "Сервер запущен, но IP-адрес не найден."
                            }
                        } catch (e: Exception) {
                            errorMessage = "Ошибка запуска службы: ${e.message}"
                        } finally {
                            isStarting = false
                        }
                    }
                },
                enabled = !isStarting,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Запустить сервер")
            }
        }
    }
}
