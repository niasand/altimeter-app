# 高度表 (Altimeter)

一个Android实时高度表应用，使用气压传感器和GPS双数据源精确测量海拔高度。

## 功能特性

- **双数据源**：优先使用气压传感器（精度±1米），无传感器时自动切换到GPS
- **实时显示**：大号数字显示当前高度，一目了然
- **单位切换**：支持米(m)和英尺(ft)两种单位
- **校准功能**：
  - 通过已知高度校准
  - 手动设置海平面气压
  - 自动保存校准值
- **精度指示**：显示当前测量精度
- **美观UI**：Material Design 3 + 渐变背景

## 技术栈

- Kotlin
- Jetpack Compose
- Material Design 3
- ViewModel + LiveData
- 传感器API (TYPE_PRESSURE)
- GPS定位API

## 安装

1. 克隆项目
```bash
git clone <repository-url>
cd android-altimeter
```

2. 使用Android Studio打开项目

3. 编译并安装到设备
```bash
./gradlew installDebug
```

## 权限要求

- `ACCESS_FINE_LOCATION` - 获取GPS精确位置
- `HIGH_SAMPLING_RATE_SENSORS` - 高频率读取气压传感器

## 使用说明

1. **首次启动**：应用会自动请求位置权限
2. **查看高度**：主界面大号数字显示当前高度
3. **切换单位**：点击底部"切换为英尺/米"按钮
4. **校准**：
   - 点击"校准"按钮
   - 选择校准方式：已知高度 或 海平面气压
   - 输入参考值并确认
5. **重置**：在校准界面可将设置恢复为默认值

## 高度计算公式

使用国际标准大气(ISA)公式：
```
h = 44330 * (1 - (P/P0)^(1/5.255))
```

其中：
- h = 高度(米)
- P = 当前气压(hPa)
- P0 = 海平面参考气压(hPa)，默认1013.25

## 注意事项

- 气压传感器精度约为±0.12 hPa，对应高度误差约±1米
- GPS高度精度较低（±5-50米），仅在无气压传感器设备上使用
- 天气变化会影响气压，建议定期校准

## 兼容性

- Android 8.0+ (API 26+)
- 支持设备：有气压传感器的设备精度最高，无传感器设备使用GPS备选

## License

MIT License
