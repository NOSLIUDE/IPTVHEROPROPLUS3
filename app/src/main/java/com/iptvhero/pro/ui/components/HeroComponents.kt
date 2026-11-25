package com.iptvhero.pro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.iptvhero.pro.data.local.ProfileEntity
import com.iptvhero.pro.ui.theme.DcCyan
import com.iptvhero.pro.ui.theme.GoldFocus
import com.iptvhero.pro.ui.theme.MarvelYellow
import com.iptvhero.pro.ui.theme.TextDim
import com.iptvhero.pro.ui.theme.TextPrimary

fun Modifier.heroFocusable(
    shape: Shape = RoundedCornerShape(10.dp),
    borderColor: Color = GoldFocus,
    onClick: () -> Unit
): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }
    this
        .onFocusChanged { isFocused = it.isFocused }
        .border(
            width = if (isFocused) 2.dp else 1.dp,
            color = if (isFocused) borderColor else Color.White.copy(alpha = 0.12f),
            shape = shape
        )
        .onKeyEvent { event ->
            if (event.type == KeyEventType.KeyUp &&
                (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
            ) {
                onClick()
                true
            } else {
                false
            }
        }
        .clickable { onClick() }
        .focusable()
}

@Composable
fun ProfileManagerDialog(
    profiles: List<ProfileEntity>,
    activeProfileId: Long?,
    onDismiss: () -> Unit,
    onActivateProfile: (Long) -> Unit,
    onDeleteProfile: (Long) -> Unit,
    onImportContent: (String) -> Unit,
    onImportGithubHero: (String) -> Unit,
    onImportXtream: (String, String, String) -> Unit
) {
    var githubUrl by remember { mutableStateOf("https://raw.githubusercontent.com/NOSLIUDE/IPTVHeroProTV/main/lista.m3u") }
    var server by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var rawTxt by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xF50C101E))
                .border(1.5.dp, DcCyan, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("GERENCIAR LISTAS & PROXY", color = GoldFocus, fontWeight = FontWeight.Black, fontSize = 15.sp)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF3B1219))
                        .heroFocusable { onDismiss() }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("FECHAR", color = Color(0xFFFF6B6E), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (profiles.isNotEmpty()) {
                LazyColumn(modifier = Modifier.height(110.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(profiles) { p ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (p.id == activeProfileId) Color(0xFF1A2744) else Color(0xFF13192B))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(p.name, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF0476F2))
                                        .heroFocusable {
                                            onActivateProfile(p.id)
                                            onDismiss()
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("ENTRAR", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF3B1219))
                                        .heroFocusable { onDeleteProfile(p.id) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("EXCLUIR", color = Color(0xFFFF6B6E), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = githubUrl,
                onValueChange = { githubUrl = it },
                label = { Text("URL Lista M3U / GitHub Hero", color = TextDim, fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE62429))
                    .heroFocusable { onImportGithubHero(githubUrl) }
                    .padding(vertical = 9.dp)
            ) {
                Text(
                    text = "CARREGAR LISTA M3U / GITHUB E ENTRAR",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = server,
                    onValueChange = { server = it },
                    label = { Text("Servidor Xtream", fontSize = 10.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1.3f)
                )
                OutlinedTextField(
                    value = user,
                    onValueChange = { user = it },
                    label = { Text("Usuário", fontSize = 10.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = pass,
                    onValueChange = { pass = it },
                    label = { Text("Senha", fontSize = 10.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0277BD))
                    .heroFocusable {
                        if (server.isNotBlank() && user.isNotBlank()) {
                            onImportXtream(server, user, pass)
                        }
                    }
                    .padding(vertical = 9.dp)
            ) {
                Text(
                    text = "CONECTAR XTREAM CODES E ENTRAR",
                    color = MarvelYellow,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }

            OutlinedTextField(
                value = rawTxt,
                onValueChange = { rawTxt = it },
                label = { Text("Colar Conteúdo TXT / M3U", fontSize = 10.sp) },
                modifier = Modifier.fillMaxWidth().height(72.dp)
            )
            if (rawTxt.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1B5E20))
                        .heroFocusable { onImportContent(rawTxt) }
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "IMPORTAR TXT E ENTRAR",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}
