package com.vitasleep.android.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vitasleep.android.ui.components.GlassCard
import com.vitasleep.android.ui.components.OverviewBar
import com.vitasleep.android.ui.components.TypewriterText
import com.vitasleep.android.ui.theme.*
import com.vitasleep.android.veepoo.VeepooManager

@Composable
fun ChatScreen(
    onNavigateToHealth: (() -> Unit)? = null,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val userId = VeepooManager.DEFAULT_USER_ID
    val listState = rememberLazyListState()

    LaunchedEffect(userId) {
        try {
            viewModel.loadHistory(userId)
            viewModel.loadOverview(userId)
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
            .background(DeepBg)
    ) {
        OverviewBar(
            batteryPercentage = uiState.battery,
            statusText = uiState.statusText,
            metricsSummary = uiState.metricsSummary,
            onClick = onNavigateToHealth
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = Dimens.padScreen),
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
            contentPadding = PaddingValues(vertical = Dimens.spaceMd)
        ) {
            if (uiState.isLoading && uiState.messages.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("助手正在思考…", color = TextTertiary, fontSize = 12.sp)
                    }
                }
            } else if (uiState.messages.isEmpty()) {
                item { WelcomeMessage() }
            } else {
                itemsIndexed(uiState.messages) { index, message ->
                    val isLast = index == uiState.messages.lastIndex
                    ChatBubble(
                        role = message.role ?: "user",
                        content = message.content ?: "",
                        useTypewriter = (message.role == "assistant") && isLast && !uiState.isLoading
                    )
                }
                if (uiState.isLoading) {
                    item {
                        Text("助手正在思考…", color = TextTertiary, fontSize = 12.sp, modifier = Modifier.padding(start = Dimens.spaceMd))
                    }
                }
            }
        }

        ChatInputBar(
            text = uiState.inputText,
            onTextChange = { viewModel.onInputTextChanged(it) },
            onSend = {
                if (uiState.inputText.isNotBlank() && !uiState.isLoading) {
                    viewModel.sendMessage(userId, uiState.inputText)
                }
            },
            enabled = !uiState.isLoading
        )
    }
}

@Composable
private fun WelcomeMessage() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(Dimens.spaceXl))
        Icon(
            Icons.Default.SmartToy,
            contentDescription = null,
            tint = IceBlue,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(Dimens.spaceMd))
        Text(
            "你好！我是 VitaSleep 健康助手",
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(Dimens.spaceXs))
        GlassCard {
            Text(
                "你可以问我：\n• 昨晚睡眠质量如何？\n• 今天身体电量怎么样？\n• 建议一个适合我的作息计划",
                color = TextTertiary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ChatBubble(
    role: String,
    content: String,
    useTypewriter: Boolean
) {
    val isUser = role == "user"
    val alignment = if (isUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isUser) 18.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 18.dp
                    )
                )
                .background(if (isUser) IceBlue else GlassBg)
                .padding(horizontal = Dimens.spaceMd, vertical = Dimens.spaceSm)
        ) {
            if (useTypewriter && content.isNotEmpty()) {
                TypewriterText(
                    fullText = content,
                    modifier = Modifier.padding(0.dp)
                )
            } else {
                Text(
                    content,
                    color = if (isUser) TextDark else TextPrimary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        Text(
            if (isUser) "你" else "助手",
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary,
            modifier = Modifier.padding(horizontal = Dimens.spaceXs, vertical = 2.dp)
        )
    }
}

@Composable
private fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Dimens.padScreen),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = { Text("询问健康建议…") },
            modifier = Modifier.weight(1f),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = IceBlue,
                unfocusedBorderColor = GlassBorder,
                focusedContainerColor = GlassBg,
                unfocusedContainerColor = GlassBg,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = IceBlue
            ),
            shape = RoundedCornerShape(24.dp),
            maxLines = 3,
            enabled = enabled
        )
        Spacer(modifier = Modifier.width(Dimens.spaceSm))
        IconButton(
            onClick = onSend,
            enabled = enabled && text.isNotBlank(),
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (text.isNotBlank() && enabled) IceBlue else GlassBorder)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Send,
                contentDescription = "发送",
                tint = TextDark
            )
        }
    }
}
