package com.altimeter.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.altimeter.utils.AltitudeCalculator
import com.altimeter.viewmodel.AltitudeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalibrationDialog(
    viewModel: AltitudeViewModel,
    onDismiss: () -> Unit
) {
    val seaLevelPressure by viewModel.seaLevelPressure.observeAsState(
        AltitudeCalculator.STANDARD_SEA_LEVEL_PRESSURE.toFloat()
    )
    val currentAltitude by viewModel.currentAltitude.observeAsState(0f)
    val useFeet by viewModel.useFeet.observeAsState(false)
    
    var selectedTab by remember { mutableIntStateOf(0) }
    var knownAltitude by remember { mutableStateOf("") }
    var manualPressure by remember { mutableStateOf(
        AltitudeCalculator.formatPressure(seaLevelPressure) 
    ) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("校准高度表") },
        text = {
            Column {
                // 标签页切换
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("已知高度") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("气压值") }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                when (selectedTab) {
                    0 -> {
                        // 已知高度校准
                        Text(
                            "输入你当前已知的确切高度，系统将自动计算海平面气压。",
                            style = MaterialTheme.typography.bodySmall
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = knownAltitude,
                            onValueChange = { knownAltitude = it },
                            label = { 
                                Text("当前高度 (${if (useFeet) "ft" else "m"})") 
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        if (currentAltitude > 0) {
                            Text(
                                "当前测量: ${String.format("%.1f", currentAltitude)} ${if (useFeet) "ft" else "m"}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                    
                    1 -> {
                        // 手动输入海平面气压
                        Text(
                            "输入当地海平面气压（可从气象局获取）。",
                            style = MaterialTheme.typography.bodySmall
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = manualPressure,
                            onValueChange = { manualPressure = it },
                            label = { Text("海平面气压 (hPa)") },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Text(
                            "标准值: 1013.25 hPa",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when (selectedTab) {
                        0 -> {
                            // 使用已知高度校准
                            knownAltitude.toFloatOrNull()?.let { altitude ->
                                val altitudeInMeters = if (useFeet) {
                                    AltitudeCalculator.feetToMeters(altitude)
                                } else altitude
                                viewModel.calibrate(altitudeInMeters)
                            }
                        }
                        1 -> {
                            // 使用手动气压校准
                            manualPressure.toFloatOrNull()?.let { pressure ->
                                viewModel.setSeaLevelPressure(pressure)
                            }
                        }
                    }
                    onDismiss()
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun ResetCalibrationDialog(
    viewModel: AltitudeViewModel,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("重置校准") },
        text = { Text("确定要重置为默认海平面气压值吗？") },
        confirmButton = {
            TextButton(
                onClick = {
                    viewModel.resetCalibration()
                    onDismiss()
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
