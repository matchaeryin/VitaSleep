package com.vitasleep.android.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vitasleep.android.ui.theme.*
import com.vitasleep.android.veepoo.VeepooManager

@Composable
fun ChatScreen(
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val userId = VeepooManager.DEFAULT_USER_ID
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(userId) {
        try {
            viewModel.loadHistory(userId)
        } catch (e: Exception) {
        }
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 标题
        Text(
            text = "健康助手",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = OnBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "基于 GLM-4 大模型，帮你分析健康数据、制定作息计划",
            fontSize = 12.sp,
            color = OnSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        // 消息列表
        Card(
            colors = CardDefaults.cardColors(containerColor = Surface),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (uiState.isLoading && uiState.messages.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (uiState.messages.isEmpty()) {
                        item {
                            WelcomeMessage()
                        }
                    }
                    items(uiState.messages) { message ->
                        ChatBubble(
                            role = message.role,
                            content = message.content,
                            isLoading = false
                        )
                    }
                    if (uiState.isLoading && uiState.messages.isNotEmpty()) {
                        item {
                            ChatBubble(role = "assistant", content = "", isLoading = true)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 输入框
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("询问健康建议…") },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = SurfaceVariant,
                    focusedContainerColor = Surface,
                    unfocusedContainerColor = Surface
                ),
                shape = RoundedCornerShape(24.dp),
                maxLines = 3
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (inputText.isNotBlank() && !uiState.isLoading) {
                        viewModel.sendMessage(userId, inputText)
                        inputText = ""
                    }
                },
                enabled = inputText.isNotBlank() && !uiState.isLoading,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (inputText.isNotBlank()) Primary else SurfaceVariant)
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "发送",
                    tint = OnPrimary
                )
            }
        }
    }
}

@Composable
fun WelcomeMessage() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Icon(
            Icons.Default.SmartToy,
            contentDescription = null,
            tint = Primary,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "你好！我是 VitaSleep 健康助手",
            fontWeight = FontWeight.Medium,
            color = OnSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "你可以问我：\n" +
                    "• 昨晚睡眠质量如何？\n" +
                    "• 今天身体电量怎么样？\n" +
                    "• 建议一个适合我的作息计划",
            color = OnSurfaceVariant,
            fontSize = 13.sp,
            lineHeight = 20.sp
        )
    }
}

@Composable
fun ChatBubble(
    role: String,
    content: String,
    isLoading: Boolean
) {
    val isUser = role == "user"
    val bgColor = if (isUser) Primary else SurfaceVariant
    val alignment = if (isUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        if (isLoading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = OnSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("助手正在思考…", color = OnSurfaceVariant, fontSize = 12.sp)
            }
        } else {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        )
                    )
                    .background(bgColor)
                    .padding(12.dp)
                    .widthIn(max = 280.dp)
            ) {
                Text(
                    content,
                    color = if (isUser) OnPrimary else OnSurface,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
            Text(
                if (isUser) "你" else "助手",
                fontSize = 10.sp,
                color = OnSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}
