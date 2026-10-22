package com.gunjan.teletowa.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.gunjan.teletowa.R
import com.gunjan.teletowa.ui.theme.NeoGlassCard
import com.gunjan.teletowa.ui.theme.NeoGlassStyle
import com.gunjan.teletowa.ui.theme.Poppins
import com.gunjan.teletowa.viewmodel.MainViewModel

@Composable
fun OnboardingScreen(vm: MainViewModel, onComplete: () -> Unit) {
    var token by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var step by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Glowing Ambient Background
        Box(
            modifier = Modifier
                .size(260.dp)
                .offset(x = (-40).dp, y = 40.dp)
                .blur(110.dp)
                .background(Color(0x357B2CBF), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(220.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = (-40).dp)
                .blur(110.dp)
                .background(Color(0x2500E5CC), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp)
                .statusBarsPadding()
        ) {
            Spacer(Modifier.height(50.dp))

            // Logo
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(R.drawable.logo),
                    contentDescription = "TeleToWA",
                    modifier = Modifier.height(36.dp),
                    contentScale = ContentScale.FillHeight
                )
            }

            Spacer(Modifier.height(32.dp))

            // Step Progress Indicator
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(2) { i ->
                    Box(
                        modifier = Modifier
                            .height(4.dp)
                            .weight(1f)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (i <= step) Color(0xFF00E5CC)
                                else Color(0x338B5CF6)
                            )
                    )
                }
            }

            Spacer(Modifier.height(44.dp))

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                },
                label = "step"
            ) { currentStep ->
                Column {
                    when (currentStep) {
                        0 -> {
                            Text(
                                "Welcome",
                                fontFamily = Poppins,
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-1.5).sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Initialize your Telegram Bot API gateway to unlock direct sticker extraction and conversion.",
                                fontFamily = Poppins,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                lineHeight = 24.sp
                            )

                            Spacer(Modifier.height(36.dp))

                            NeoGlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(22.dp)
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Text(
                                        "TELEGRAM BOT TOKEN",
                                        fontFamily = Poppins,
                                        fontSize = 11.sp,
                                        color = Color(0xFF00E5CC),
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color(0x22101422))
                                            .border(1.dp, NeoGlassStyle.GlassInputBorder, RoundedCornerShape(14.dp))
                                    ) {
                                        TextField(
                                            value = token,
                                            onValueChange = { token = it },
                                            placeholder = {
                                                Text(
                                                    "Paste BotFather token...",
                                                    color = MaterialTheme.colorScheme.outline.copy(0.5f),
                                                    fontFamily = Poppins,
                                                    fontSize = 14.sp
                                                )
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = TextFieldDefaults.colors(
                                                focusedContainerColor = Color.Transparent,
                                                unfocusedContainerColor = Color.Transparent,
                                                focusedIndicatorColor = Color.Transparent,
                                                unfocusedIndicatorColor = Color.Transparent,
                                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                            ),
                                            singleLine = true,
                                            visualTransformation = PasswordVisualTransformation(),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                                        )
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        "Message @BotFather on Telegram -> /newbot -> paste your unique token key.",
                                        fontFamily = Poppins,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                        1 -> {
                            Text(
                                "Creator\nSignature",
                                fontFamily = Poppins,
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-1.5).sp,
                                color = MaterialTheme.colorScheme.onBackground,
                                lineHeight = 42.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "This creator identifier will be embedded as the author/publisher in exported WhatsApp sticker trays.",
                                fontFamily = Poppins,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                lineHeight = 24.sp
                            )

                            Spacer(Modifier.height(36.dp))

                            NeoGlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(22.dp)
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Text(
                                        "PUBLISHER / AUTHOR HANDLE",
                                        fontFamily = Poppins,
                                        fontSize = 11.sp,
                                        color = Color(0xFF00E5CC),
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color(0x22101422))
                                            .border(1.dp, NeoGlassStyle.GlassInputBorder, RoundedCornerShape(14.dp))
                                    ) {
                                        TextField(
                                            value = author,
                                            onValueChange = { author = it },
                                            placeholder = {
                                                Text(
                                                    "e.g. NeoStudio, Gunjan",
                                                    color = MaterialTheme.colorScheme.outline.copy(0.5f),
                                                    fontFamily = Poppins,
                                                    fontSize = 14.sp
                                                )
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = TextFieldDefaults.colors(
                                                focusedContainerColor = Color.Transparent,
                                                unfocusedContainerColor = Color.Transparent,
                                                focusedIndicatorColor = Color.Transparent,
                                                unfocusedIndicatorColor = Color.Transparent,
                                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                            ),
                                            singleLine = true
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Action button with vibrant gradient
            Button(
                onClick = {
                    if (step == 0 && token.isNotBlank()) {
                        step = 1
                    } else if (step == 1 && author.isNotBlank()) {
                        vm.completeOnboarding(token, author, "{name}_by_{author}")
                        onComplete()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if ((step == 0 && token.isNotBlank()) || (step == 1 && author.isNotBlank()))
                            NeoGlassStyle.VioletCyanGradient
                        else Brush.horizontalGradient(listOf(Color(0xFF232A3E), Color(0xFF232A3E)))
                    ),
                shape = RoundedCornerShape(18.dp),
                enabled = if (step == 0) token.isNotBlank() else author.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent
                )
            ) {
                Text(
                    if (step == 0) "Continue to Signature" else "Launch TeleToWA Studio",
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }

            Spacer(Modifier.height(48.dp))
        }
    }
}
