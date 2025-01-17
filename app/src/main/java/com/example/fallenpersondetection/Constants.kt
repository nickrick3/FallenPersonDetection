package com.example.fallenpersondetection

object Constants {
    // accelerometer inputs are expected every 20 milliseconds
    const val ACCELEROMETER_TIME_DELTA_MS : Long = 20L

    // averages are to be calculated over a span of (at most) 5 values
    const val NUMBER_CONSECUTIVE_SAMPLES_TRG : Int = 5

    // SMA threshold_1 is : + 27 m/s^2
    const val THRESHOLD_1 : Double = +27.00

    // Threshold_2 is : +10 m/s^2
    const val THRESHOLD_2 : Double = +10.05

    // Threshold_3 is : -10 m/s^2
    const val THRESHOLD_3 : Double = -10.00

    enum class FallState{
        NoFallMotionlessAct,
        LateralFall,
        BackwardsFall,
        ForwardsFall,
        OtherMotionAct,
        UndefinedError
    }

    const val LONG_MAX_VAL : Long = Long.MAX_VALUE
    const val LONG_MIN_VAL : Long  = Long.MIN_VALUE
    const val DOUBLE_MAX_VAL : Double  = Double.MAX_VALUE
    // const val DOUBLE_MIN_VAL : Double = kotlin.Double.MIN_VALUE

}