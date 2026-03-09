package com.altimeter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altimeter.utils.AltitudeCalculator
import com.altimeter.viewmodel.AltitudeViewModel

@Composable
fun AltimeterScreen(viewModel: AltitudeViewModel, onSettingsClick: () -> Unit) {
    val altitude by viewModel.currentAltitude.observeAsState(0f)
    val pressure by viewModel.currentPressure.observeAsState(0f)
    val gpsAltitude by viewModel.gpsAltitude.observeAsState(null)
    val accuracy by viewModel.accuracy.observeAsState(0f)
    val dataSource by viewModel.dataSource.observeAsState(AltitudeViewModel.DataSource.NONE)
    val useFeet by viewModel.useFeet.observeAsState(false)
    val hasPressureSensor by viewModel.hasPressureSensor.observeAsState(false)
    
    val unit = if (useFeet) "ft" else "m"
    val displayAltitude = if (useFeet) {
        AltitudeCalculator.metersToFeet(altitude)
    } else {
        altitude
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A237E),
                        Color(0xFF0D47A1),
                        Color(0xFF01579B)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 顶部工具栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "高度表",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "设置",
                        tint = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 主高度显示
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1565C0).copy(alpha = 0.3f)
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = String.format("%,.1f", displayAltitude),
                            style = MaterialTheme.typography.displayLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 80.sp
                        )
                        
                        Text(
                            text = unit,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 数据源和精度信息
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1565C0).copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    InfoRow(
                        label = "数据源",
                        value = when (dataSource) {
                            AltitudeViewModel.DataSource.BAROMETER -> "气压传感器"
                            AltitudeViewModel.DataSource.GPS -> "GPS"
                            else -> "等待数据..."
                        }
                    )
                    
                    if (pressure > 0) {
                        InfoRow(
                            label = "气压",
                            value = "${AltitudeCalculator.formatPressure(pressure)} hPa"
                        )
                    }
                    
                    if (accuracy > 0) {
                        val accuracyUnit = if (useFeet) {
                            AltitudeCalculator.metersToFeet(accuracy)
                        } else accuracy
                        InfoRow(
                            label = "精度",
                            value = "±${String.format("%.1f", accuracyUnit)} $unit"
                        )
                    }
                    
                    if (gpsAltitude != null && dataSource == AltitudeViewModel.DataSource.BAROMETER) {
                        val gpsDisplay = if (useFeet) {
                            AltitudeCalculator.metersToFeet(gpsAltitude!!)
                        } else gpsAltitude!!
                        InfoRow(
                            label = "GPS高度",
                            value = "${String.format("%.1f", gpsDisplay)} $unit"
                        )
                    }
                    
                    if (!hasPressureSensor) {
                        Text(
                            text = "⚠️ 设备无气压传感器，使用GPS数据",
                            color = Color(0xFFFFA726),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // 底部按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { viewModel.toggleUnit() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF42A5F5)
                    ),
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    Text(if (useFeet) "切换为米" else "切换为英尺")
                }
                
                Button(
                    onClick = onSettingsClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF42A5F5)
                    ),
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                ) {
                    Text("校准")
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
