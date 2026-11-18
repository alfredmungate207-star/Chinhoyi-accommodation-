package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ChatMessage
import com.example.data.ChatThread
import com.example.data.PhotoCompressionHelper
import com.example.data.UiState
import com.example.ui.components.HouseImage
import com.example.ui.theme.ChatIncomingBubble
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.IndigoDeep
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.IndigoSoft
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Chat Page copied & beautified from Template #3 (Messages Inbox + Conversation Detail):
 * - Rounded Indigo-Blue Header with active contact avatars & search button
 * - Clean white inbox list with unread badge circles
 * - Conversation view with rounded blue header, Phone/Info icons, light incoming & royal-blue outgoing bubbles,
 *   plus Photo Picker attachment button & instant Firestore send
 */
@Composable
fun ChatScreen(
    currentUserId: String,
    threadsState: UiState<List<ChatThread>>,
    activeThread: ChatThread?,
    messages: List<ChatMessage>,
    onSelectThread: (ChatThread?) -> Unit,
    onSendMessage: (text: String, imageDataUri: String?) -> Unit,
    onGoToHome: () -> Unit,
    onOpenHouseMap: (houseId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (activeThread != null) {
        BackHandler { onSelectThread(null) }
        ChatThreadDetailView(
            currentUserId = currentUserId,
            thread = activeThread,
            messages = messages,
            onBack = { onSelectThread(null) },
            onSendMessage = onSendMessage,
            onOpenHouseMap = { onOpenHouseMap(activeThread.houseId) },
            modifier = modifier
        )
    } else {
        ChatInboxView(
            currentUserId = currentUserId,
            threadsState = threadsState,
            onSelectThread = { onSelectThread(it) },
            onGoToHome = onGoToHome,
            modifier = modifier
        )
    }
}

@Composable
private fun ChatInboxView(
    currentUserId: String,
    threadsState: UiState<List<ChatThread>>,
    onSelectThread: (ChatThread) -> Unit,
    onGoToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Template #3 Rounded Royal-Blue Header
        Surface(
            color = IndigoPrimary,
            shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
            shadowElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .background(Brush.verticalGradient(listOf(IndigoDeep, IndigoPrimary)))
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MESSAGES",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = CircleShape,
                        modifier = Modifier.size(40.dp)
                    ) {
                        IconButton(onClick = { showSearch = !showSearch }) {
                            Icon(Icons.Filled.Search, contentDescription = "Search chats", tint = Color.White)
                        }
                    }
                }

                if (showSearch) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search landlord or house...") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Active Contacts Horizontal Avatar Strip (Template #3)
                if (threadsState is UiState.Success && threadsState.data.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(threadsState.data, key = { "avatar_${it.id}" }) { thread ->
                            val peerName = if (currentUserId == thread.customerUid) thread.landlordName else thread.customerName
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { onSelectThread(thread) }
                            ) {
                                Box {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.22f))
                                            .border(2.dp, Color.White, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = (peerName.firstOrNull() ?: 'L').uppercase(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .align(Alignment.BottomEnd)
                                            .clip(CircleShape)
                                            .background(EmeraldAccent)
                                            .border(2.dp, IndigoPrimary, CircleShape)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = peerName.substringBefore(" ").take(10),
                                    color = Color.White.copy(alpha = 0.9f),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        }

        when (threadsState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = IndigoPrimary)
                }
            }
            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(threadsState.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is UiState.Success -> {
                val filtered = threadsState.data.filter {
                    searchQuery.isBlank() ||
                        it.houseName.contains(searchQuery, ignoreCase = true) ||
                        it.landlordName.contains(searchQuery, ignoreCase = true) ||
                        it.customerName.contains(searchQuery, ignoreCase = true)
                }

                if (filtered.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(IndigoSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.ChatBubbleOutline, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("No Unlocked Chats Yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Pay the small EcoCash unlock fee on any boarding house to open direct messaging with the landlord.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Surface(
                            color = IndigoPrimary,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.clickable(onClick = onGoToHome)
                        ) {
                            Text(
                                "Browse Boarding Houses",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(filtered, key = { it.id }) { thread ->
                            val isCustomer = currentUserId == thread.customerUid
                            val peerName = if (isCustomer) thread.landlordName else thread.customerName
                            val unread = if (isCustomer) thread.unreadByCustomer else thread.unreadByLandlord
                            val timeStr = remember(thread.updatedAt) {
                                thread.updatedAt?.toDate()?.let {
                                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(it)
                                } ?: ""
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectThread(thread) }
                                    .padding(horizontal = 20.dp, vertical = 14.dp)
                                    .testTag("chat_thread_${thread.id}"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(IndigoSoft),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = (peerName.firstOrNull() ?: 'U').uppercase(),
                                        color = IndigoDeep,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = peerName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = IndigoPrimary,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = thread.houseName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF0D9488),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = thread.lastMessage,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = timeStr,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (unread > 0L) {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(IndigoPrimary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = unread.toString(),
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatThreadDetailView(
    currentUserId: String,
    thread: ChatThread,
    messages: List<ChatMessage>,
    onBack: () -> Unit,
    onSendMessage: (text: String, imageDataUri: String?) -> Unit,
    onOpenHouseMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var messageInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val isCustomer = currentUserId == thread.customerUid
    val peerName = if (isCustomer) thread.landlordName else thread.customerName

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val dataUri = PhotoCompressionHelper.uriToCompressedDataUri(context, uri)
                if (dataUri != null) {
                    onSendMessage("Shared a photo", dataUri)
                }
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFAFBFF))
    ) {
        // Template #3 Blue Rounded Chat Top Bar
        Surface(
            color = IndigoPrimary,
            shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
            shadowElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .background(Brush.horizontalGradient(listOf(IndigoDeep, IndigoPrimary)))
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Messages", tint = Color.White)
                }

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f))
                        .border(1.5.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (peerName.firstOrNull() ?: 'U').uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = peerName,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Online • ${thread.houseName}",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(40.dp)
                ) {
                    IconButton(onClick = onOpenHouseMap) {
                        Icon(Icons.Filled.HomeWork, contentDescription = "View House Map Pin", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
                Surface(
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(40.dp)
                ) {
                    IconButton(onClick = { onSendMessage("Hi! Can we schedule a viewing for ${thread.houseName}?", null) }) {
                        Icon(Icons.Filled.Info, contentDescription = "Quick viewing request", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val isMine = msg.senderUid == currentUserId
                val timeText = remember(msg.createdAt) {
                    msg.createdAt?.toDate()?.let {
                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(it)
                    } ?: "Sending..."
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
                    verticalAlignment = Alignment.Bottom
                ) {
                    if (!isMine) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(IndigoSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (msg.senderName.firstOrNull() ?: 'U').uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = IndigoDeep,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Column(horizontalAlignment = if (isMine) Alignment.End else Alignment.Start) {
                        Surface(
                            color = if (isMine) IndigoPrimary else ChatIncomingBubble,
                            shape = RoundedCornerShape(
                                topStart = 18.dp,
                                topEnd = 18.dp,
                                bottomStart = if (isMine) 18.dp else 4.dp,
                                bottomEnd = if (isMine) 4.dp else 18.dp
                            ),
                            modifier = Modifier.widthIn(max = 270.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (!msg.imageData.isNullOrBlank()) {
                                    HouseImage(
                                        photoRef = msg.imageData,
                                        photoCache = emptyMap(),
                                        contentDescription = "Chat attachment",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(150.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    )
                                }
                                if (msg.text.isNotBlank()) {
                                    Text(
                                        text = msg.text,
                                        color = if (isMine) Color.White else Color(0xFF1E293B),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = timeText,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                            if (isMine) {
                                Icon(Icons.Filled.DoneAll, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(13.dp))
                            }
                        }
                    }
                }
            }
        }

        // Bottom Rounded Input Bar (Template #3)
        Surface(
            color = Color.White,
            shadowElevation = 10.dp,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = "Attach Photo", tint = IndigoPrimary)
                }

                OutlinedTextField(
                    value = messageInput,
                    onValueChange = { messageInput = it },
                    placeholder = { Text("Type Your Message") },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF3F6FC),
                        unfocusedContainerColor = Color(0xFFF3F6FC),
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_message_input")
                )

                Surface(
                    color = IndigoPrimary,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.size(48.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (messageInput.isNotBlank()) {
                                onSendMessage(messageInput, null)
                                messageInput = ""
                            }
                        },
                        modifier = Modifier.testTag("chat_send_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Message", tint = Color.White)
                    }
                }
            }
        }
    }
}
