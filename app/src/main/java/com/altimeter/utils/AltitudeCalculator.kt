package com.altimeter.utils

import kotlin.math.pow

/**
 * 高度计算器
 * 使用国际标准大气模型公式计算高度
 */
object AltitudeCalculator {
    
    // 标准海平面气压 (hPa)
    const val STANDARD_SEA_LEVEL_PRESSURE = 1013.25
    
    // 标准温度递减率 (K/m)
    private const val TEMPERATURE_LAPSE_RATE = 0.0065
    
    // 标准海平面温度 (K)
    private const val STANDARD_TEMPERATURE = 288.15
    
    // 重力加速度 (m/s²)
    private const val GRAVITY = 9.80665
    
    // 气体常数 (J/(kg·K))
    private const val GAS_CONSTANT = 287.05
    
    /**
     * 使用气压计算高度（国际标准大气公式）
     * @param pressure 当前气压 (hPa)
     * @param seaLevelPressure 海平面参考气压 (hPa)，默认为标准值
     * @return 高度 (米)
     */
    fun calculateAltitudeFromPressure(pressure: Float, seaLevelPressure: Float = STANDARD_SEA_LEVEL_PRESSURE.toFloat()): Float {
        if (pressure <= 0) return 0f
        
        // 气压高度公式: h = 44330 * (1 - (P/P0)^(1/5.255))
        return (44330.0 * (1.0 - Math.pow((pressure / seaLevelPressure).toDouble(), 1.0 / 5.255))).toFloat()
    }
    
    /**
     * 使用气压计算高度（完整公式，更精确）
     * @param pressure 当前气压 (hPa)
     * @param seaLevelPressure 海平面参考气压 (hPa)
     * @param temperature 当前温度 (°C)，默认15°C
     * @return 高度 (米)
     */
    fun calculateAltitudePrecise(
        pressure: Float, 
        seaLevelPressure: Float = STANDARD_SEA_LEVEL_PRESSURE.toFloat(),
        temperature: Float = 15f
    ): Float {
        if (pressure <= 0) return 0f
        
        val temperatureK = temperature + 273.15
        val exponent = (GAS_CONSTANT * TEMPERATURE_LAPSE_RATE) / GRAVITY
        
        return ((temperatureK / TEMPERATURE_LAPSE_RATE) * 
            (1 - Math.pow((pressure / seaLevelPressure).toDouble(), exponent))).toFloat()
    }
    
    /**
     * 根据已知高度反推海平面气压
     * @param pressure 当前气压 (hPa)
     * @param knownAltitude 已知高度 (米)
     * @return 海平面气压 (hPa)
     */
    fun calibrateSeaLevelPressure(pressure: Float, knownAltitude: Float): Float {
        if (pressure <= 0 || knownAltitude < 0) return STANDARD_SEA_LEVEL_PRESSURE.toFloat()
        
        // 反向计算: P0 = P / (1 - h/44330)^5.255
        return (pressure / Math.pow(1 - knownAltitude / 44330.0, 5.255)).toFloat()
    }
    
    /**
     * 米转英尺
     */
    fun metersToFeet(meters: Float): Float {
        return meters * 3.28084f
    }
    
    /**
     * 英尺转米
     */
    fun feetToMeters(feet: Float): Float {
        return feet / 3.28084f
    }
    
    /**
     * 格式化高度显示
     */
    fun formatAltitude(altitude: Float, useFeet: Boolean = false): String {
        val value = if (useFeet) metersToFeet(altitude) else altitude
        return String.format("%,.1f", value)
    }
    
    /**
     * 格式化气压显示
     */
    fun formatPressure(pressure: Float): String {
        return String.format("%.2f", pressure)
    }
    
    /**
     * 计算气压传感器精度对应的高度误差
     * 典型气压传感器精度约 ±0.12 hPa，对应约 ±1米
     */
    fun estimateAccuracy(pressure: Float): Float {
        // 0.12 hPa 约等于 1米误差
        val pressureAccuracy = 0.12f
        val altitudeChangePerHpa = 8.5f // 大约每hPa变化对应8.5米高度变化
        return pressureAccuracy * altitudeChangePerHpa
    }
}
