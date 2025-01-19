package com.example.fallenpersondetection

import android.app.Activity
import kotlin.math.abs

class StatusDetector(
    aArrX: ArrayDeque<Double>,
    aArrY: ArrayDeque<Double>,
    aArrZ: ArrayDeque<Double>
) : Activity() {
    private val accArrX: ArrayDeque<Double> = aArrX
    private val accArrY: ArrayDeque<Double> = aArrY
    private val accArrZ: ArrayDeque<Double> = aArrZ

    private var arrLenX = accArrX.size
    private val arrLenY = accArrY.size
    private val arrLenZ = accArrZ.size

    private var minArrLength : Int = -1

    // SignalMagnitudeArea
    private var SMA : Double = 0.0
    private var xAbsMean : Double = 0.0
    private var yAbsMean : Double = 0.0
    private var zAbsMean : Double = 0.0
    private var zSignMean : Double = 0.0

    // overflow and errors counter
    private var errorsCount = 0

    private fun extractFeatures() : Boolean {

        // # setup SMA -----------------------------------
        minArrLength = minOf(arrLenX, arrLenY, arrLenZ)
        print("operating with minArrLen = $minArrLength")
        if (minArrLength <= 0){
            errorsCount++
            return false
        }
        val smaMaxIteration = minOf(minArrLength, Constants.NUMBER_CONSECUTIVE_SAMPLES_TRG)
        // var lastAccX = accArrX.takeLast(sma_max_iteration)
        // var lastAccY = accArrY.takeLast(sma_max_iteration)
        // var lastAccZ = accArrZ.takeLast(sma_max_iteration)
        val smaSum = sumAbsOfADQ(accArrX) + sumAbsOfADQ(accArrY) + sumAbsOfADQ(accArrZ)
        SMA = smaSum / smaMaxIteration.toDouble()

        // # setup xAbsMean, yAbsMean, zAbsMean -----------------------------------
        xAbsMean = getMeanOfADQ(accArrX, useAbsVal = true)
        yAbsMean = getMeanOfADQ(accArrY, useAbsVal = true)
        zAbsMean = getMeanOfADQ(accArrZ, useAbsVal = true)
        zSignMean = getMeanOfADQ(accArrZ, useAbsVal = false)

        return true
    }

    private fun testTh1SMA() : Boolean{
        return ( SMA > Constants.THRESHOLD_1)
    }

    private fun testTh2X() : Boolean{
        return ( xAbsMean > Constants.THRESHOLD_2)
    }

    private fun testTh2Z() : Boolean{
        return ( zAbsMean > Constants.THRESHOLD_2)
    }

    private fun testTh3Z() : Boolean{
        // this test does not use the absolute values of z , but the signed last value !!!
        // return ( zSignMean < Constants.THRESHOLD_3)
        val zLastAcc : Double = accArrZ.last()
        return ( zLastAcc < Constants.THRESHOLD_3 )
    }

    fun evaluate(): Constants.FallState {
        if ( !extractFeatures() ){
            // some weird problem during parameters calculations
            print("How Did We Get Here?")
            return Constants.FallState.UndefinedError
        }
        if ( !testTh1SMA() ){
            // low movement overall
            // target hs not fallen, it was a motionless analysis : NO MOTION
            return Constants.FallState.NoFallMotionlessAct
        }else{
            // we are moving
            if ( testTh2X() ){
                // XXX : high movement on X-axis :
                // target has FALLEN laterally : RIGHT / LEFT
                trgHasFallen()
                return Constants.FallState.LateralFall
            }else{
                if ( ! testTh2Z() ){
                    // then we are just randomly moving (maybe Y-axis?)
                    return Constants.FallState.OtherMotionAct
                }
                else  {
                    // ZZZ : high movement on Z-axis
                    if (testTh3Z()) {
                        // target surely has FALLEN FORWARDS
                        trgHasFallen()
                        return Constants.FallState.ForwardsFall
                    } else {
                        // TODO : better false positive than missing backwards fall imo
                        // COULD BE BACKWARDS FALL
                        // or could be jump / lie_down / sit_down ... idk :/
                        // i would call alarm since might be backwards fall .
                        trgHasFallen()
                        return Constants.FallState.BackwardsFall
                        // return Constants.FallState.ZMotionJumpLieSit
                    }
                }
            }
        }
    }


    private fun trgHasFallen(){
        print("sad")
    }

    private fun sumAbsOfADQ(trgList : ArrayDeque<Double>) : Double{
        var i = 0
        val trgLen = trgList.size
        var sum = 0.0
        while (i<trgLen && !trgList[i].isNaN() ){
            sum += abs(trgList[i])
            i++
        }
        if (trgList[i].isNaN()){
            print("very weird error")
            errorsCount ++
        }
        if (sum < 0.0 ){
            // sum overflow could happen
            print("overflow or smt")
            errorsCount ++
            return Constants.DOUBLE_MAX_VAL
        }
        return sum
    }

    private fun getMeanOfADQ(trgList : ArrayDeque<Double>, useAbsVal : Boolean) : Double{
        val defaultMin = 0.0
        val defaultMax : Double = Constants.DOUBLE_MAX_VAL
        val trgSize = trgList.size
        if (trgSize <=0){
            // should never happen
            print("How Did We Get Here?")
            errorsCount ++
            return defaultMin
        }
        val trgSum = if(useAbsVal) sumAbsOfADQ(trgList) else trgList.sum()
        if (trgSum < 0.0 ){
            // sum overflow could happen
            print("overflow or smt")
            errorsCount ++
            return defaultMax
        }
        else{
            return ( trgList.sum() / trgSize )
        }
    }



}