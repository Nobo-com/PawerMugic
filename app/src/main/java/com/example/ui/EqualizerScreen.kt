package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.MusicViewModel
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun EqualizerScreen(viewModel: MusicViewModel) {
    var selectedEqTab by remember { mutableStateOf(0) }
    val eqTabs = listOf("Volume", "Reverb", "Equalizer")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(com.example.ui.theme.PowerBackground)
    ) {
        TabRow(
            selectedTabIndex = selectedEqTab,
            containerColor = Color(0xFF14161B),
            contentColor = Color.White,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedEqTab]),
                    color = com.example.ui.theme.PowerAccent
                )
            },
            divider = {}
        ) {
            eqTabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedEqTab == index,
                    onClick = { selectedEqTab = index },
                    text = { Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    selectedContentColor = com.example.ui.theme.PowerAccent,
                    unselectedContentColor = Color.Gray
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (selectedEqTab) {
                0 -> VolumeTab()
                1 -> ReverbTab()
                2 -> ParametricEqTab()
            }
        }
    }
}

@Composable
fun VolumeTab() {
    var volume by remember { mutableFloatStateOf(0.67f) }
    var balance by remember { mutableFloatStateOf(0.5f) }
    var stereo by remember { mutableFloatStateOf(0.0f) }
    var tempo by remember { mutableFloatStateOf(0.5f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            KnobControl(
                label = "Balance",
                value = balance,
                onValueChange = { balance = it },
                valueText = String.format("%.2f", (balance - 0.5f) * 2),
                size = 80.dp
            )
            KnobControl(
                label = "Stereo Expand",
                value = stereo,
                onValueChange = { stereo = it },
                valueText = "${(stereo * 100).toInt()}%",
                size = 80.dp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { /*TODO*/ },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2228)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Tempo", color = Color.White)
            }

            KnobControl(
                label = "",
                value = tempo,
                onValueChange = { tempo = it },
                valueText = String.format("%.2fx", tempo * 2),
                size = 100.dp,
                showValueBelow = true
            )

            Column {
                IconButton(onClick = { tempo = (tempo + 0.05f).coerceAtMost(1f) }, modifier = Modifier.background(Color(0xFF1F2228), CircleShape).size(36.dp)) {
                    Text("+", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                IconButton(onClick = { tempo = (tempo - 0.05f).coerceAtLeast(0f) }, modifier = Modifier.background(Color(0xFF1F2228), CircleShape).size(36.dp)) {
                    Text("-", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
             Button(
                onClick = { /*TODO*/ },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2228)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Mono", color = Color.White)
            }

             Button(
                onClick = { /*TODO*/ },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2228)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Reset", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        KnobControl(
            label = "Volume",
            value = volume,
            onValueChange = { volume = it },
            valueText = "${(volume * 100).toInt()}%",
            size = 180.dp,
            glowColor = Color(0xFF90FF00),
            thickness = 8.dp
        )
    }
}

@Composable
fun ReverbTab() {
    var damp by remember { mutableFloatStateOf(0f) }
    var filter by remember { mutableFloatStateOf(0f) }
    var fade by remember { mutableFloatStateOf(0f) }
    var preDelay by remember { mutableFloatStateOf(0f) }
    var preDelayMix by remember { mutableFloatStateOf(0f) }
    var size by remember { mutableFloatStateOf(0f) }
    var mix by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            KnobControl(label = "Damp", value = damp, onValueChange = { damp = it }, valueText = String.format("%.2f", damp), size = 80.dp)
            KnobControl(label = "Filter", value = filter, onValueChange = { filter = it }, valueText = String.format("%.2f", filter), size = 80.dp)
            KnobControl(label = "Fade", value = fade, onValueChange = { fade = it }, valueText = String.format("%.2f", fade), size = 80.dp)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            KnobControl(label = "Pre-Delay", value = preDelay, onValueChange = { preDelay = it }, valueText = String.format("%.2f", preDelay), size = 80.dp)
            KnobControl(label = "Pre-Delay Mix", value = preDelayMix, onValueChange = { preDelayMix = it }, valueText = String.format("%.2f", preDelayMix), size = 80.dp)
            KnobControl(label = "Size", value = size, onValueChange = { size = it }, valueText = String.format("%.2f", size), size = 80.dp)
        }
        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2228))) { Text("Reverb", color = Color.White) }
            Button(onClick = { }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2228))) { Text("Preset", color = Color.White) }
            Button(onClick = { }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2228))) { Text("Save", color = Color.White) }
            Button(onClick = { }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2228))) { Text("Reset", color = Color.White) }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            KnobControl(
                label = "Mix",
                value = mix,
                onValueChange = { mix = it },
                valueText = String.format("%.2f", mix),
                size = 120.dp
            )
        }
    }
}

@Composable
fun ParametricEqTab() {
    var preamp by remember { mutableFloatStateOf(0.5f) }
    var bass by remember { mutableFloatStateOf(0.5f) }
    var mid by remember { mutableFloatStateOf(0.5f) }
    var treble by remember { mutableFloatStateOf(0.5f) }
    
    var gain1 by remember { mutableFloatStateOf(0.8f) }
    var freq1 by remember { mutableFloatStateOf(0.2f) }
    var q1 by remember { mutableFloatStateOf(0.5f) }
    
    var gain2 by remember { mutableFloatStateOf(0.3f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(260.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Preamp Slider
            EqSlider(
                value = preamp,
                onValueChange = { preamp = it },
                label = "Preamp",
                valueText = String.format("%.1f", (preamp - 0.5f) * 12),
                color = Color.Gray,
                modifier = Modifier.weight(1f)
            )

            // Band 1 Config
            Column(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight()
                    .background(Color(0xFF4A0E0E), RoundedCornerShape(16.dp))
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    EqSlider(
                        value = gain1,
                        onValueChange = { gain1 = it },
                        label = "Gain",
                        valueText = String.format("%.1f", (gain1 - 0.5f) * 12),
                        color = Color.Yellow,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        SmallKnob(value = 0.5f, onValueChange = {})
                        
                        Box(
                            modifier = Modifier.background(Color.Black, RoundedCornerShape(8.dp)).padding(4.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Q", color = Color.White, fontSize = 10.sp)
                                Text(String.format("%.2f", q1), color = Color.White, fontSize = 12.sp)
                            }
                        }

                        SmallKnob(value = freq1, onValueChange = { freq1 = it }, glowColor = Color.Cyan)
                        Text("Freq", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("${(freq1 * 200).toInt()}", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
            
            // Band 2 Config
            Column(
                modifier = Modifier
                    .weight(1.5f)
                    .fillMaxHeight()
                    .background(Color(0xFF000B4A), RoundedCornerShape(16.dp))
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                EqSlider(
                    value = gain2,
                    onValueChange = { gain2 = it },
                    label = "Gain",
                    valueText = String.format("%.1f", (gain2 - 0.5f) * 12),
                    color = Color.Green,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Pseudo Chart
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(Color(0xFF1F2228), RoundedCornerShape(8.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val path = androidx.compose.ui.graphics.Path()
                path.moveTo(0f, size.height / 2)
                path.cubicTo(
                    size.width * 0.2f, size.height * 0.2f,
                    size.width * 0.4f, size.height * 0.8f,
                    size.width * 0.6f, size.height / 2
                )
                path.cubicTo(
                    size.width * 0.8f, size.height * 0.3f,
                    size.width * 0.9f, size.height * 0.7f,
                    size.width, size.height / 2
                )
                drawPath(
                    path = path,
                    color = Color.Yellow,
                    style = Stroke(width = 4f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Button(onClick = { }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2228)), modifier = Modifier.height(36.dp)) { Text("Equ", color = Color.White, fontSize = 12.sp) }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2228)), modifier = Modifier.height(36.dp)) { Text("Tone", color = Color.White, fontSize = 12.sp) }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2228)), modifier = Modifier.height(36.dp)) { Text("Limit", color = Color.White, fontSize = 12.sp) }
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(
                    onClick = { },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("1Custom SA02", color = Color.White)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    KnobControl(label = "Bass", value = bass, onValueChange = { bass = it }, valueText = "${(bass * 100).toInt()}%", size = 60.dp)
                    KnobControl(label = "Treble", value = treble, onValueChange = { treble = it }, valueText = "${(treble * 100).toInt()}%", size = 60.dp)
                }
            }
            
            IconButton(onClick = {}, modifier = Modifier.background(Color(0xFF1F2228), CircleShape)) {
                Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.White)
            }
        }
    }
}

@Composable
fun EqSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    label: String,
    valueText: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .width(40.dp)
                .background(Color(0xFF14161B), RoundedCornerShape(20.dp))
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            // Track
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(Color.DarkGray, CircleShape)
            )
            // Active Track
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight(value)
                    .align(Alignment.BottomCenter)
                    .background(color, CircleShape)
            )
            
            // Thumb
            var sliderValue by remember { mutableFloatStateOf(value) }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val newVal = (sliderValue - dragAmount.y / size.height).coerceIn(0f, 1f)
                            sliderValue = newVal
                            onValueChange(newVal)
                        }
                    }
            ) {
                 Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = (-value * 200).dp) // approximation
                        .width(24.dp)
                        .height(48.dp)
                        .background(Color(0xFF2C2F36), RoundedCornerShape(12.dp)),
                     contentAlignment = Alignment.Center
                 ) {
                     Box(modifier = Modifier.width(12.dp).height(2.dp).background(Color.White))
                 }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(valueText, color = Color.Gray, fontSize = 12.sp)
    }
}


@Composable
fun KnobControl(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueText: String,
    size: androidx.compose.ui.unit.Dp,
    glowColor: Color = Color.Transparent,
    thickness: androidx.compose.ui.unit.Dp = 2.dp,
    showValueBelow: Boolean = true
) {
    var angle by remember { mutableFloatStateOf(value * 270f - 135f) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (!showValueBelow) {
            Text(label, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
        }

        Box(
            modifier = Modifier
                .size(size)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val newAngle = (angle + dragAmount.x).coerceIn(-135f, 135f)
                        angle = newAngle
                        onValueChange((newAngle + 135f) / 270f)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.toPx() / 2, size.toPx() / 2)
                val radius = size.toPx() / 2 - thickness.toPx()

                // Background track
                if (glowColor != Color.Transparent) {
                     drawArc(
                        color = Color.DarkGray,
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = thickness.toPx(), cap = StrokeCap.Round),
                        topLeft = Offset(thickness.toPx(), thickness.toPx()),
                        size = Size(radius * 2, radius * 2)
                    )
                     
                     // Glow arc
                    drawArc(
                        color = glowColor,
                        startAngle = 135f,
                        sweepAngle = (angle + 135f),
                        useCenter = false,
                        style = Stroke(width = thickness.toPx(), cap = StrokeCap.Round),
                        topLeft = Offset(thickness.toPx(), thickness.toPx()),
                        size = Size(radius * 2, radius * 2)
                    )
                }

                // Knob body
                drawCircle(
                    color = Color(0xFF23262E),
                    radius = if (glowColor != Color.Transparent) radius - 16.dp.toPx() else radius
                )
                
                // Tick mark
                val tickAngleRad = (angle - 90f) * (PI / 180f)
                val tickRadius = if (glowColor != Color.Transparent) radius - 32.dp.toPx() else radius - 16.dp.toPx()
                val tickStart = Offset(
                    x = center.x + (tickRadius - 8.dp.toPx()) * cos(tickAngleRad).toFloat(),
                    y = center.y + (tickRadius - 8.dp.toPx()) * sin(tickAngleRad).toFloat()
                )
                val tickEnd = Offset(
                    x = center.x + tickRadius * cos(tickAngleRad).toFloat(),
                    y = center.y + tickRadius * sin(tickAngleRad).toFloat()
                )
                drawLine(
                    color = Color.LightGray,
                    start = tickStart,
                    end = tickEnd,
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        if (showValueBelow) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(valueText, color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
fun SmallKnob(
    value: Float,
    onValueChange: (Float) -> Unit,
    glowColor: Color = Color.Transparent
) {
    var angle by remember { mutableFloatStateOf(value * 270f - 135f) }

    Box(
        modifier = Modifier
            .size(40.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val newAngle = (angle + dragAmount.x).coerceIn(-135f, 135f)
                    angle = newAngle
                    onValueChange((newAngle + 135f) / 270f)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.width / 2

            // Glow indicator around knob
            if (glowColor != Color.Transparent) {
                drawArc(
                    color = glowColor,
                    startAngle = 135f,
                    sweepAngle = (angle + 135f),
                    useCenter = false,
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
                    topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                    size = Size(size.width - 8.dp.toPx(), size.height - 8.dp.toPx())
                )
            }

            drawCircle(
                color = Color(0xFF1F2228),
                radius = radius - 8.dp.toPx()
            )

            val tickAngleRad = (angle - 90f) * (PI / 180f)
            val tickRadius = radius - 12.dp.toPx()
            val tickStart = Offset(
                x = center.x + (tickRadius - 4.dp.toPx()) * cos(tickAngleRad).toFloat(),
                y = center.y + (tickRadius - 4.dp.toPx()) * sin(tickAngleRad).toFloat()
            )
            val tickEnd = Offset(
                x = center.x + tickRadius * cos(tickAngleRad).toFloat(),
                y = center.y + tickRadius * sin(tickAngleRad).toFloat()
            )
            drawLine(
                color = Color.LightGray,
                start = tickStart,
                end = tickEnd,
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}
