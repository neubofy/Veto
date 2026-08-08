package com.neubofy.veto.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.background
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import com.neubofy.veto.permissions.Permission
import com.neubofy.veto.ui.theme.glassmorphism

import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionManagerScreen(
    permissions: List<Permission>,
    activity: AppCompatActivity,
    highlightName: Int = -1
) {
    val listState = rememberLazyListState()

    LaunchedEffect(highlightName) {
        if (highlightName != -1) {
            val index = permissions.indexOfFirst { it.name == highlightName }
            if (index != -1) {
                listState.animateScrollToItem(index)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Permission Manager", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(permissions, key = { it.name }) { permission ->
            val isHighlighted = permission.name == highlightName
            var startHighlight by remember { mutableStateOf(false) }

            LaunchedEffect(isHighlighted) {
                if (isHighlighted) {
                    startHighlight = true
                    kotlinx.coroutines.delay(2000)
                    startHighlight = false
                }
            }

            val highlightColor by animateColorAsState(
                targetValue = if (startHighlight) androidx.compose.ui.graphics.Color(0x33FF9800) else androidx.compose.ui.graphics.Color.Transparent,
                animationSpec = tween(durationMillis = 2000),
                label = "highlightColor"
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(highlightColor)
                    .glassmorphism(),
                colors = CardDefaults.cardColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    PermissionItem(permission = permission, activity = activity)
                }
            }
        }
    }
    }
}
