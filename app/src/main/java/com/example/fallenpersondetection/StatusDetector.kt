package com.example.fallenpersondetection

import android.util.Log
import kotlin.math.abs

class StatusDetector(
    aArrX: ArrayDeque<Double>,
    aArrY: ArrayDeque<Double>,
    aArrZ: ArrayDeque<Double>
) {
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

    // DEBUG
    private val sTAG: String = StatusDetector::class.simpleName.toString()

    private fun extractFeatures() : Boolean {

        // # setup SMA -----------------------------------
        minArrLength = minOf(arrLenX, arrLenY, arrLenZ)
        // DEBUG: print("ExtractFeatures : operating with minArrLen = $minArrLength \n")
        if (minArrLength < 0){
            errorsCount++
            return false
        }
        val smaMaxIteration = minOf(minArrLength, Constants.NUMBER_CONSECUTIVE_SAMPLES_TRG)
        // var lastAccX = accArrX.takeLast(sma_max_iteration)
        // var lastAccY = accArrY.takeLast(sma_max_iteration)
        // var lastAccZ = accArrZ.takeLast(sma_max_iteration)
        val smaSum = sumAbsOfADQ(accArrX) + sumAbsOfADQ(accArrY) + sumAbsOfADQ(accArrZ)
        SMA = smaSum / smaMaxIteration.toDouble()
        // DEBUG: print("smaSum : $SMA")

        // # setup xAbsMean, yAbsMean, zAbsMean -----------------------------------
        xAbsMean = getMeanOfADQ(accArrX, useAbsVal = true)
        yAbsMean = getMeanOfADQ(accArrY, useAbsVal = true)
        zAbsMean = getMeanOfADQ(accArrZ, useAbsVal = true)
        zSignMean = getMeanOfADQ(accArrZ, useAbsVal = false)

        // DEBUG: print(" end of ExtractFeatures true \n")
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
            // DEBUG: print("evaluate _ How Did We Get Here? : extractFeatures failed \n")
            return Constants.FallState.UndefinedError
        }
        val testTh1SMAresult = testTh1SMA()
        if ( !testTh1SMAresult ){
            // low movement overall
            // target hs not fallen, it was a motionless analysis : NO MOTION
            return Constants.FallState.NoFallMotionlessAct
        }else{
            // we are moving
            val testTh2Xresult = testTh2X()
            if ( testTh2Xresult ){
                // XXX : high movement on X-axis :
                // target has FALLEN laterally : RIGHT / LEFT
                return Constants.FallState.LateralFall
            }else{
                val testTh2Zresult = testTh2Z()
                if ( ! testTh2Zresult ){
                    // then we are just randomly moving (maybe Y-axis?)
                    return Constants.FallState.OtherMotionAct
                }
                else  {
                    // ZZZ : high movement on Z-axis
                    val testTh3Zresult = testTh3Z()
                    if ( testTh3Zresult ) {
                        // target surely has FALLEN FORWARDS
                        return Constants.FallState.ForwardsFall
                    } else {
                        // COULD BE BACKWARDS FALL
                        // or could be jump / lie_down / sit_down ... idk :/
                        // i would call alarm since might be backwards fall .
                        return Constants.FallState.BackwardsFall
                        // return Constants.FallState.ZMotionJumpLieSit
                    }
                }
            }
        }
    }

    private fun sumAbsOfADQ(trgList : ArrayDeque<Double>) : Double{
        // DEBUG: print("begin sumAbsOfADQ \n ")
        val trgLen = trgList.size
        if (trgLen == 0){
            return 0.0
        }else{
            var i = 0
            var sum = 0.0
            // DEBUG: print(" _ trying to sumAbsOfADQ ")
            while (i < trgLen ){
                sum += abs(trgList[i])
                i++
            }
            // DEBUG: print(" _ out of while")

            if (sum < 0.0 ){
                // sum overflow could happen
                // DEBUG: print("overflow or smt \n")
                errorsCount ++
                return Constants.DOUBLE_MAX_VAL
            }
            return sum
        }
    }

    private fun getMeanOfADQ(trgList : ArrayDeque<Double>, useAbsVal : Boolean) : Double{
        val defaultMin = 0.0
        val defaultMax : Double = Constants.DOUBLE_MAX_VAL
        val trgSize = trgList.size
        if (trgSize < 0){
            // should never happen
            // DEBUG: print("getMeanOfADQ _ How Did We Get Here? \n")
            errorsCount ++
            return defaultMin
        }
        val trgSum = if(useAbsVal) sumAbsOfADQ(trgList) else trgList.sum()
        if (trgSum < 0.0 ){
            // sum overflow could happen
            // DEBUG: print("overflow or smt")
            errorsCount ++
            return defaultMax
        }
        else{
            return ( trgList.sum() / trgSize )
        }
    }



}