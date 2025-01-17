package com.example.fallenpersondetection

import android.app.Activity
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlin.math.*


class Accelerometer : Activity(), SensorEventListener {
    private val mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
    private val mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    private val aTAG: String = Accelerometer::class.simpleName.toString()


    override fun onResume() {
        super.onResume()
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME)
    }

    override fun onPause() {
        super.onPause()
        mSensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        if(Sensor.TYPE_LINEAR_ACCELERATION == sensor.type){
            Log.println(Log.INFO, aTAG,"Accuracy of our accelerometer has changed to : $accuracy")
        }
    }

    // we need to keep an array of values as history of the most recent measured accelerations
    // these array will have a given length, and the expected time distance between values that populate them is fixed to <interval>
    // therefore, when we receive a new value on the sensor, if more than 2*<interval> has passed from the previous input,
    // we will update the array as if values equal to the previous input had been received periodically up to now, and then we will add the new input.
    private var accArrX: ArrayDeque<Double> = ArrayDeque()
    private var accArrY: ArrayDeque<Double> = ArrayDeque()
    private var accArrZ: ArrayDeque<Double> = ArrayDeque()
    // private var QueueLength: Int = 0

    private var prevAccX: Double = 0.0
    private var prevAccY: Double = 0.0
    private var prevAccZ: Double = 0.0
    private var prevAccTime : Long = 0L

    // this is the time to which the array and prev values are being updated
    private var calcTime : Long = 0L

    // val ACCELEROMETER_TIME_DELTA_MS : Long = 20L // calculations are expected every 20 milliseconds
    // val NUMBER_CONSECUTIVE_SAMPLES_TRG : Int = 5

    override fun onSensorChanged(event: SensorEvent) {
        if(event.sensor?.type == Sensor.TYPE_LINEAR_ACCELERATION){
            // val stdGravity = SensorManager.STANDARD_GRAVITY
            // timestamp gives nanoseconds
            val sChangeTime: Long = event.timestamp / 1000
            val sChangeX = event.values[0].toDouble()
            val sChangeY = event.values[1].toDouble()
            val sChangeZ = event.values[2].toDouble()
            updateArrays(sChangeTime, sChangeX, sChangeY, sChangeZ)
        }
        return
    }

    // Accelerometer sampling is quite irregular, so we need to normalize values over the available sample time deltas
    @Throws(Exception::class)
    private fun updateArrays(newAccTime:Long, newAccX:Double, newAccY:Double, newAccZ:Double){
        // the first time, we don't change the arrays, but only set the "prevAcc..." values !
        // all the other times, we expand the arrays properly
        if(prevAccTime == 0L){
            calcTime = newAccTime + Constants.ACCELEROMETER_TIME_DELTA_MS
        }else{
            // update the queues linearly using the newly acquired sample
            // at every step, check if any trigger should be triggered
            while (calcTime < newAccTime){
                val interimAccX = linearize( prevAccTime, prevAccX, newAccTime, newAccX, calcTime)
                val interimAccY = linearize( prevAccTime, prevAccY, newAccTime, newAccY, calcTime)
                val interimAccZ = linearize( prevAccTime, prevAccZ, newAccTime, newAccZ, calcTime)
                accArrX.addFirst(interimAccX)
                accArrX.addFirst(interimAccY)
                accArrX.addFirst(interimAccZ)
                // assert(accArrX.size == accArrY.size && accArrX.size== accArrZ.size )
                calcTime += Constants.ACCELEROMETER_TIME_DELTA_MS
                checkTriggers()
            }
        }
        prevAccX = newAccX
        prevAccY = newAccY
        prevAccZ = newAccZ
        prevAccTime = newAccTime
        return
    }

    private fun checkTriggers() {
        // need to pop things from queue
        val arrLenX = accArrX.size
        val arrLenY = accArrY.size
        val arrLenZ = accArrZ.size
        try{
            assert(arrLenX == arrLenY)
            assert(arrLenX == arrLenZ)
        }
        catch (err: AssertionError){
            println(err.message)
        }
        // val justPoppedAccX: Double? = popLastUntilLen(accArrX, Constants.NUMBER_CONSECUTIVE_SAMPLES_TRG)
        // val justPoppedAccY: Double? = popLastUntilLen(accArrY, Constants.NUMBER_CONSECUTIVE_SAMPLES_TRG)
        // val justPoppedAccZ: Double? = popLastUntilLen(accArrZ, Constants.NUMBER_CONSECUTIVE_SAMPLES_TRG)
        popLastUntilLen(accArrX, Constants.NUMBER_CONSECUTIVE_SAMPLES_TRG)
        popLastUntilLen(accArrY, Constants.NUMBER_CONSECUTIVE_SAMPLES_TRG)
        popLastUntilLen(accArrZ, Constants.NUMBER_CONSECUTIVE_SAMPLES_TRG)

        // TODO
        // if needed, trigger events.
        val triggerEvaluator = StatusDetector(
            accArrX,
            accArrY,
            accArrZ
        )
        val evaluationResult : Constants.FallState = triggerEvaluator.evaluate()
        print(evaluationResult)
        return
    }

    private fun popLastUntilLen( trgArr:ArrayDeque<Double>, maxLen:Int) : Double?{
        var defaultRet : Double? = 0.0
        while(trgArr.size > maxLen){
            defaultRet = trgArr.removeLastOrNull()
        }
        return defaultRet
    }

    private fun linearize( prevTime:Long, prevValue:Double, newTime:Long, newValue:Double, trgTime:Long): Double{
        // // time-weighted normalization :
        // // y = m t + q
        // // m = Dy/Dx  -> == ( newV-oldV) / (newT-oldT)
        // // q = y - mt -> == newV - newT[(newV-oldV)/(newT-oldT)]
        // // y^ = m t^ + q -> == [(newV-oldV)/(newT-oldT)] * trgTime + newV - newT[(newV-oldV)/(newT-oldT)]
        // //               -> == { tT(nV-oV) + nV(nT-oT) - nT(nV-oV) } / (nT-oT)
        // //               -> == { nVtT-oVtT + nVnT-nVoT -nVnT+oVnT } / (nT-oT)
        // //               -> == { nVtT-oVtT -nVoT+oVnT - oVoT +oVoT } / (nT-oT)
        // //               -> == { oV(nT-oT) + nVtT-oVtT -nVoT + oVoT } / (nT-oT)
        // //               -> == oV + (nV-oV)(tT-oT)/(nT-oT)
        // val deltaTime : newTime-prevTime
        val deltaNewTimOldTim : Double = timeDiffs(prevTime, newTime).toDouble()
        val deltaTrgTimOldTim : Double = timeDiffs(prevTime, trgTime).toDouble()
        return prevTime + (newValue - prevValue)*(deltaTrgTimOldTim)/(deltaNewTimOldTim)
    }

    @Throws(Exception::class)
    private fun timeDiffs( ancientTime: Long, recentTime: Long ) : Long{
        // return recentTime - ancientTime
        if (recentTime > ancientTime){
            return recentTime-ancientTime
        }else{
            // // newTime(negative) - minTime( -2^-63) + maxTime(2^63 -1) - prevTime(positive)
            // in order to be able to calculate this, we need to pack differently
            // { newTime(negative) + maxTime(positive } - { minTime(negative) + prevTime(positive) }
            try{
                assert(recentTime < 0L)
                assert(Constants.LONG_MAX_VAL > 0L)
                assert(ancientTime > 0L)
                assert(Constants.LONG_MIN_VAL < 0L)
            }
            catch (err: AssertionError){
                println(err.message)
                print("very big problem")
            }
            val sumNewTimeMaxVal : Long = recentTime + Constants.LONG_MAX_VAL
            val sumMinValPrevTime : Long = Constants.LONG_MIN_VAL + ancientTime
            val diff = sumNewTimeMaxVal - sumMinValPrevTime
            if (diff < 0){
                // overflow happened anyways, cannot be avoided sadly (?)
                return Constants.LONG_MAX_VAL
            }
            return diff
        }
    }

}


