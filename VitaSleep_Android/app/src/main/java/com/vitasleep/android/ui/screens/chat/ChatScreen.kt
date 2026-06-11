package com.vitasleep.android.ui.screens.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vitasleep.android.ui.theme.*
import com.vitasleep.android.veepoo.VeepooManager

@Composable
fun ChatScreen(viewModel: ChatViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val userId = VeepooManager.DEFAULT_USER_ID
    var inputText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("健康助手", style = MaterialTheme.typography.headlineMedium, color = OnBackground)
        Spacer(modifier = Modifier.height(16.dp))

        Card(colors = CardDefaults.cardColors(containerColor = Surface), modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (uiState.messages.isEmpty()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize().padding(32.dp)) {
                    Icon(Icons.Default.SmartToy, null, tint = Primary, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("你好！我是 VitaSleep 健康助手", color = OnSurface)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.messages) { message -> ChatBubble(role = message.role, content = message.content) }
                    if (uiState.isLoading) item { Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Primary); Spacer(Modifier.width(8.dp)); Text("思考中…", color = OnSurfaceVariant) } }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = inputText, onValueChange = { inputText = it }, placeholder = { Text("发送消息…") }, modifier = Modifier.weight(1f), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary))
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { if (inputText.isNotBlank()) { viewModel.sendMessage(userId, inputText); inputText = "" } }, enabled = inputText.isNotBlank() && !uiState.isLoading) { Icon(Icons.Default.Send, null, tint = if (inputText.isNotBlank()) Primary else OnSurfaceVariant) }
        }
    }
}

@Composable
fun ChatBubble(role: String, content: String) {
    val isUser = role == "user"
    val bgColor = if (isUser) Primary else SurfaceVariant
    Column(horizontalAlignment = if (isUser) Alignment.End else Alignment.Start, modifier = Modifier.fillMaxWidth()) {
        Card(colors = CardDefaults.cardColors(containerColor = bgColor), modifier = Modifier.widthIn(max = 280.dp)) { Text(content, modifier = Modifier.padding(12.dp), color = if (isUser) OnPrimary else OnSurface) }
    }
}
