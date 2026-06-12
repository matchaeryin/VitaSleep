package com.vitasleep.android.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vitasleep.android.ui.theme.*
import com.vitasleep.android.veepoo.VeepooManager

@Composable
fun ChatScreen(viewModel: ChatViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val userId = VeepooManager.DEFAULT_USER_ID
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(userId) {
        viewModel.loadChatHistory(userId)
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
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "\u5065\u5eb7\u52a9\u624b",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "\u57fa\u4e8e AI \u7684\u667a\u80fd\u5065\u5eb7\u54a8\u8be2",
            fontSize = 13.sp,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(GlassBg, RoundedCornerShape(24.dp))
                .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
        ) {
            if (uiState.messages.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(IceBlueGlow, RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.SmartToy, null,
                            tint = IceBlue,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "\u4f60\u597d\uff01\u6211\u662f VitaSleep \u5065\u5eb7\u52a9\u624b",
                        color = TextPrimary,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "\u53ef\u4ee5\u8be2\u95ee\u60a8\u7684\u5065\u5eb7\u6570\u636e\u3001\u7761\u7720\u8d28\u91cf\u3001\u8fd0\u52a8\u5efa\u8bae\u7b49\u95ee\u9898",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.messages) { message ->
                        GlassChatBubble(
                            role = message.role,
                            content = message.content
                        )
                    }
                    if (uiState.isLoading) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = IceBlue
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "\u601d\u8003\u4e2d\u2026",
                                    color = TextSecondary,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        if (uiState.error != null && uiState.messages.isEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                uiState.error!!,
                color = RoseRed,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(GlassBg, RoundedCornerShape(22.dp))
                .border(1.dp, GlassBorder, RoundedCornerShape(22.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = {
                    Text(
                        "\u53d1\u9001\u6d88\u606f\u2026",
                        color = TextDim
                    )
                },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = IceBlue
                ),
                maxLines = 3
            )
            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendMessage(userId, inputText)
                        inputText = ""
                    }
                },
                enabled = inputText.isNotBlank() && !uiState.isLoading
            ) {
                Icon(
                    Icons.Default.Send, null,
                    tint = if (inputText.isNotBlank()) IceBlue else TextDim
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun GlassChatBubble(role: String, content: String) {
    val isUser = role == "user"
    Column(
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .then(
                    if (isUser) {
                        Modifier
                            .background(
                                Brush.horizontalGradient(
                                    listOf(IceBlue.copy(alpha = 0.25f), IceBlue.copy(alpha = 0.15f))
                                ),
                                RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
                            )
                            .border(
                                1.dp,
                                IceBlue.copy(alpha = 0.3f),
                                RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
                            )
                    } else {
                        Modifier
                            .background(
                                GlassBgElevated,
                                RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
                            )
                            .border(
                                1.dp,
                                GlassBorder,
                                RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
                            )
                    }
                )
        ) {
            Text(
                content,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                color = if (isUser) TextPrimary else TextSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}
