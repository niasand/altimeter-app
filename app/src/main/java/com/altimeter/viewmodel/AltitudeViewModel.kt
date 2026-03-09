package com.altimeter.viewmodel

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.altimeter.utils.AltitudeCalculator
import kotlin.math.abs

/**
 * 高度表ViewModel
 * 管理气压传感器和GPS数据源
 */
class AltitudeViewModel(application: Application) : AndroidViewModel(application) {
    
    private val sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val locationManager = application.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    
    // 传感器
    private var pressureSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
    
    // 数据LiveData
    private val _currentAltitude = MutableLiveData(0f)
    val currentAltitude: LiveData<Float> = _currentAltitude
    
    private val _currentPressure = MutableLiveData(0f)
    val currentPressure: LiveData<Float> = _currentPressure
    
    private val _gpsAltitude = MutableLiveData<Float?>(null)
    val gpsAltitude: LiveData<Float?> = _gpsAltitude
    
    private val _seaLevelPressure = MutableLiveData(AltitudeCalculator.STANDARD_SEA_LEVEL_PRESSURE.toFloat())
    val seaLevelPressure: LiveData<Float> = _seaLevelPressure
    
    private val _accuracy = MutableLiveData(0f)
    val accuracy: LiveData<Float> = _accuracy
    
    private val _dataSource = MutableLiveData(DataSource.NONE)
    val dataSource: LiveData<DataSource> = _dataSource
    
    private val _hasPressureSensor = MutableLiveData(pressureSensor != null)
    val hasPressureSensor: LiveData<Boolean> = _hasPressureSensor
    
    private val _useFeet = MutableLiveData(false)
    val useFeet: LiveData<Boolean> = _useFeet
    
    private val _isCalibrating = MutableLiveData(false)
    val isCalibrating: LiveData<Boolean> = _isCalibrating
    
    // 平滑处理
    private val pressureReadings = mutableListOf<Float>()
    private val maxReadings = 10
    
    // 传感器监听器
    private val pressureListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                val pressure = it.values[0]
                _currentPressure.postValue(pressure)
                
                // 平滑处理
                pressureReadings.add(pressure)
                if (pressureReadings.size > maxReadings) {
                    pressureReadings.removeAt(0)
                }
                
                val smoothedPressure = pressureReadings.average().toFloat()
                val altitude = AltitudeCalculator.calculateAltitudeFromPressure(
                    smoothedPressure, 
                    _seaLevelPressure.value ?: AltitudeCalculator.STANDARD_SEA_LEVEL_PRESSURE.toFloat()
                )
                
                _currentAltitude.postValue(altitude)
                _accuracy.postValue(AltitudeCalculator.estimateAccuracy(smoothedPressure))
                
                if (_dataSource.value != DataSource.GPS) {
                    _dataSource.postValue(DataSource.BAROMETER)
                }
            }
        }
        
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }
    
    // GPS监听器
    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            if (location.hasAltitude()) {
                _gpsAltitude.postValue(location.altitude.toFloat())
                
                // 如果没有气压传感器，使用GPS高度
                if (pressureSensor == null) {
                    _currentAltitude.postValue(location.altitude.toFloat())
                    _dataSource.postValue(DataSource.GPS)
                    // GPS精度约为 ±5-50米
                    _accuracy.postValue(location.accuracy)
                }
            }
        }
        
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }
    
    init {
        startSensors()
    }
    
    /**
     * 启动传感器
     */
    fun startSensors() {
        // 启动气压传感器
        pressureSensor?.let {
            sensorManager.registerListener(
                pressureListener,
                it,
                SensorManager.SENSOR_DELAY_UI
            )
        }
        
        // 启动GPS
        startGps()
    }
    
    /**
     * 启动GPS定位
     */
    private fun startGps() {
        val context = getApplication<Application>()
        
        if (ContextCompat.checkSelfPermission(
                context, 
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED) {
            
            try {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000L, // 1秒更新一次
                    1f,    // 1米位移更新
                    locationListener,
                    Looper.getMainLooper()
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 停止传感器
     */
    fun stopSensors() {
        sensorManager.unregisterListener(pressureListener)
        locationManager.removeUpdates(locationListener)
    }
    
    /**
     * 校准海平面气压
     * @param knownAltitude 已知的参考高度（米）
     */
    fun calibrate(knownAltitude: Float) {
        _currentPressure.value?.let { pressure ->
            val newSeaLevelPressure = AltitudeCalculator.calibrateSeaLevelPressure(pressure, knownAltitude)
            _seaLevelPressure.value = newSeaLevelPressure
            
            // 重新计算高度
            val newAltitude = AltitudeCalculator.calculateAltitudeFromPressure(pressure, newSeaLevelPressure)
            _currentAltitude.value = newAltitude
            
            // 保存校准值
            saveCalibration(newSeaLevelPressure)
        }
    }
    
    /**
     * 手动设置海平面气压
     */
    fun setSeaLevelPressure(pressure: Float) {
        _seaLevelPressure.value = pressure
        _currentPressure.value?.let { currentPressure ->
            val altitude = AltitudeCalculator.calculateAltitudeFromPressure(currentPressure, pressure)
            _currentAltitude.value = altitude
        }
        saveCalibration(pressure)
    }
    
    /**
     * 重置为默认值
     */
    fun resetCalibration() {
        _seaLevelPressure.value = AltitudeCalculator.STANDARD_SEA_LEVEL_PRESSURE.toFloat()
        _currentPressure.value?.let { pressure ->
            val altitude = AltitudeCalculator.calculateAltitudeFromPressure(
                pressure, 
                AltitudeCalculator.STANDARD_SEA_LEVEL_PRESSURE.toFloat()
            )
            _currentAltitude.value = altitude
        }
        clearCalibration()
    }
    
    /**
     * 切换单位
     */
    fun toggleUnit() {
        _useFeet.value = !(_useFeet.value ?: false)
    }
    
    /**
     * 设置校准模式
     */
    fun setCalibrating(calibrating: Boolean) {
        _isCalibrating.value = calibrating
    }
    
    /**
     * 保存校准值到SharedPreferences
     */
    private fun saveCalibration(seaLevelPressure: Float) {
        val prefs = getApplication<Application>()
            .getSharedPreferences("altimeter_prefs", Context.MODE_PRIVATE)
        prefs.edit().putFloat("sea_level_pressure", seaLevelPressure).apply()
    }
    
    /**
     * 清除校准值
     */
    private fun clearCalibration() {
        val prefs = getApplication<Application>()
            .getSharedPreferences("altimeter_prefs", Context.MODE_PRIVATE)
        prefs.edit().remove("sea_level_pressure").apply()
    }
    
    /**
     * 加载保存的校准值
     */
    fun loadCalibration() {
        val prefs = getApplication<Application>()
            .getSharedPreferences("altimeter_prefs", Context.MODE_PRIVATE)
        val savedPressure = prefs.getFloat("sea_level_pressure", -1f)
        if (savedPressure > 0) {
            _seaLevelPressure.value = savedPressure
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        stopSensors()
    }
    
    enum class DataSource {
        NONE, BAROMETER, GPS
    }
}
