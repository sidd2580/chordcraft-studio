package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.GuitarAudioSynthesizer
import com.example.data.model.ChordVoicing

/**
 * Vector Fretboard Widget rendered via Jetpack Compose Canvas / CustomPainter.
 * Displays 6 strings with realistic gauge thicknesses, frets, finger numbers (1-4),
 * open (O) and muted (X) string markers, barre chord bars, base fret offset,
 * and Left-Handed mode horizontal mirroring.
 */
@Composable
fun FretboardView(
    voicing: ChordVoicing,
    modifier: Modifier = Modifier,
    isLeftHanded: Boolean = false,
    showStrumButton: Boolean = true,
    onStrumClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.testTag("fretboard_card_${voicing.chordName}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Chord header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = voicing.chordName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${voicing.voicingName} • ${voicing.difficulty}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (showStrumButton) {
                    IconButton(
                        onClick = {
                            onStrumClick?.invoke() ?: GuitarAudioSynthesizer.strumChord(voicing.frets)
                        },
                        modifier = Modifier.testTag("strum_button_${voicing.chordName}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Strum ${voicing.chordName}",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Vector Fretboard Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clickable {
                        GuitarAudioSynthesizer.strumChord(voicing.frets)
                    },
                contentAlignment = Alignment.Center
            ) {
                val primaryColor = MaterialTheme.colorScheme.primary
                val onSurfaceColor = MaterialTheme.colorScheme.onSurface
                val surfaceColor = MaterialTheme.colorScheme.surface
                val accentColor = MaterialTheme.colorScheme.secondary

                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawFretboardDiagram(
                        voicing = voicing,
                        isLeftHanded = isLeftHanded,
                        primaryColor = primaryColor,
                        onSurfaceColor = onSurfaceColor,
                        surfaceColor = surfaceColor,
                        accentColor = accentColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Notes summary chips
            val notes = voicing.getNoteNames()
            if (notes.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Notes: " + notes.joinToString(" • "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawFretboardDiagram(
    voicing: ChordVoicing,
    isLeftHanded: Boolean,
    primaryColor: Color,
    onSurfaceColor: Color,
    surfaceColor: Color,
    accentColor: Color
) {
    val totalWidth = size.width
    val totalHeight = size.height

    val marginHorizontal = 44f
    val marginTop = 36f
    val marginBottom = 20f

    val boardWidth = totalWidth - (marginHorizontal * 2)
    val boardHeight = totalHeight - marginTop - marginBottom

    val numFrets = 5
    val numStrings = 6

    val stringSpacing = boardWidth / (numStrings - 1)
    val fretSpacing = boardHeight / numFrets

    // Reorder frets and fingers if left-handed (mirrors string 6..1 to 1..6)
    val frets = if (isLeftHanded) voicing.frets.reversed() else voicing.frets
    val fingers = if (isLeftHanded) voicing.fingers.reversed() else voicing.fingers

    // Nut or Base Fret Header
    val isNut = voicing.baseFret <= 1
    if (isNut) {
        // Draw heavy nut bar at top
        drawRect(
            color = onSurfaceColor,
            topLeft = Offset(marginHorizontal - 2f, marginTop - 4f),
            size = Size(boardWidth + 4f, 8f)
        )
    } else {
        // Draw normal thin fret at top and fret number indicator on left/right
        drawLine(
            color = onSurfaceColor.copy(alpha = 0.5f),
            start = Offset(marginHorizontal, marginTop),
            end = Offset(marginHorizontal + boardWidth, marginTop),
            strokeWidth = 2.5f
        )
        // Draw Base Fret Label (e.g. "3fr" or "5fr")
        val labelX = if (isLeftHanded) marginHorizontal + boardWidth + 12f else marginHorizontal - 36f
        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
                color = onSurfaceColor.hashCode()
                textSize = 30f
                isAntiAlias = true
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            drawText("${voicing.baseFret}fr", labelX, marginTop + fretSpacing * 0.7f, paint)
        }
    }

    // Draw horizontal frets (frets 1 to 5)
    for (f in 1..numFrets) {
        val y = marginTop + (f * fretSpacing)
        drawLine(
            color = onSurfaceColor.copy(alpha = 0.35f),
            start = Offset(marginHorizontal, y),
            end = Offset(marginHorizontal + boardWidth, y),
            strokeWidth = 2.5f
        )
    }

    // Draw vertical strings with varying realistic gauges (string 6 thickest, string 1 thinnest)
    for (s in 0 until numStrings) {
        val x = marginHorizontal + (s * stringSpacing)
        val stringIndexOriginal = if (isLeftHanded) 5 - s else s
        // String gauge: string 0 (low E) has stroke 4.5f, string 5 (high e) has stroke 1.5f
        val strokeGauge = 4.5f - (stringIndexOriginal * 0.6f)

        drawLine(
            color = onSurfaceColor.copy(alpha = 0.7f),
            start = Offset(x, marginTop),
            end = Offset(x, marginTop + boardHeight),
            strokeWidth = strokeGauge
        )

        // Draw Open 'O' or Muted 'X' above nut
        val fretVal = frets[s]
        val symbolY = marginTop - 14f

        if (fretVal == 0) {
            // Open string 'O'
            drawCircle(
                color = primaryColor,
                radius = 7f,
                center = Offset(x, symbolY),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
            )
        } else if (fretVal < 0) {
            // Muted string 'X'
            val xRadius = 6f
            drawLine(
                color = onSurfaceColor.copy(alpha = 0.5f),
                start = Offset(x - xRadius, symbolY - xRadius),
                end = Offset(x + xRadius, symbolY + xRadius),
                strokeWidth = 2.5f
            )
            drawLine(
                color = onSurfaceColor.copy(alpha = 0.5f),
                start = Offset(x - xRadius, symbolY + xRadius),
                end = Offset(x + xRadius, symbolY - xRadius),
                strokeWidth = 2.5f
            )
        }
    }

    // Draw Barre Bars (if any)
    val effectiveBase = if (voicing.baseFret <= 1) 1 else voicing.baseFret
    for (barreFret in voicing.barres) {
        val relativeFret = barreFret - effectiveBase + 1
        if (relativeFret in 1..numFrets) {
            val y = marginTop + (relativeFret - 0.5f) * fretSpacing
            // Find start and end string for barre
            val barredStringIndices = frets.indices.filter { frets[it] >= barreFret }
            if (barredStringIndices.isNotEmpty()) {
                val startX = marginHorizontal + (barredStringIndices.minOrNull()!! * stringSpacing)
                val endX = marginHorizontal + (barredStringIndices.maxOrNull()!! * stringSpacing)
                drawRoundRect(
                    color = primaryColor.copy(alpha = 0.85f),
                    topLeft = Offset(startX - 12f, y - 12f),
                    size = Size(endX - startX + 24f, 24f),
                    cornerRadius = CornerRadius(12f, 12f)
                )
            }
        }
    }

    // Draw Finger Dots on Frets
    val dotRadius = 13f
    for (s in 0 until numStrings) {
        val fretVal = frets[s]
        val fingerVal = fingers[s]

        if (fretVal > 0) {
            val relativeFret = if (voicing.baseFret <= 1) fretVal else (fretVal - voicing.baseFret + 1)
            if (relativeFret in 1..numFrets) {
                val cx = marginHorizontal + (s * stringSpacing)
                val cy = marginTop + (relativeFret - 0.5f) * fretSpacing

                // Glowing outer circle
                drawCircle(
                    color = primaryColor.copy(alpha = 0.3f),
                    radius = dotRadius + 3f,
                    center = Offset(cx, cy)
                )

                // Main finger dot
                drawCircle(
                    color = primaryColor,
                    radius = dotRadius,
                    center = Offset(cx, cy)
                )

                // Finger number text inside dot (1, 2, 3, 4)
                if (fingerVal > 0) {
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            textSize = 24f
                            textAlign = android.graphics.Paint.Align.CENTER
                            isAntiAlias = true
                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                        }
                        drawText(fingerVal.toString(), cx, cy + 8f, paint)
                    }
                }
            }
        }
    }
}
