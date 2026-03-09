package com.altimeter

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.altimeter.ui.AltimeterScreen
import com.altimeter.ui.CalibrationDialog
import com.altimeter.ui.ResetCalibrationDialog
import com.altimeter.viewmodel.AltitudeViewModel
import com.altimeter.ui.theme.AltimeterTheme

class MainActivity : ComponentActivity() {
    
    private lateinit var viewModel: AltitudeViewModel
    
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true -> {
                // 精确位置权限已获取
                viewModel.startSensors()
            }
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                // 仅获取了粗略位置权限
                viewModel.startSensors()
            }
            else -> {
                // 权限被拒绝
                Toast.makeText(
                    this, 
                    "位置权限被拒绝，GPS高度功能不可用", 
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        viewModel = ViewModelProvider(this)[AltitudeViewModel::class.java]
        viewModel.loadCalibration()
        
        // 检查位置权限
        checkLocationPermission()
        
        setContent {
            AltimeterTheme {
                var showCalibrationDialog by remember { mutableStateOf(false) }
                var showResetDialog by remember { mutableStateOf(false) }
                
                AltimeterScreen(
                    viewModel = viewModel,
                    onSettingsClick = { showCalibrationDialog = true }
                )
                
                if (showCalibrationDialog) {
                    CalibrationDialog(
                        viewModel = viewModel,
                        onDismiss = { showCalibrationDialog = false }
                    )
                }
            }
        }
    }
    
    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this, 
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // 已有权限，启动传感器
                viewModel.startSensors()
            }
            else -> {
                // 请求权限
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        viewModel.startSensors()
    }
    
    override fun onPause() {
        super.onPause()
        viewModel.stopSensors()
    }
}
