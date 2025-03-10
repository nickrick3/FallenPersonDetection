package com.example.fallenpersondetection

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Process
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlin.math.*


class AccelerometerService : Service(), SensorEventListener {
    companion object {
        // lets the main activity know if the service is already running
        var running = false
        // avoid sending multiple notifications
        var alarmStarted = false
    }

    // thread handling
    private lateinit var mSensorThread : HandlerThread
    private lateinit var mHandler: Handler

    // tag for logging
    private val aTAG: String = AccelerometerService::class.simpleName.toString()

    override fun onCreate() {
        super.onCreate()

        // start handler thread
        mSensorThread = HandlerThread("Sensor Thread", Process.THREAD_PRIORITY_MORE_FAVORABLE)
        mSensorThread.start()
        mHandler = Handler(mSensorThread.looper)

        // register sensor listener in handler thread
        val mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        mSensorManager.registerListener(
            this,
            mAccelerometer,
            SensorManager.SENSOR_DELAY_GAME,
            mHandler)

        // Create the NotificationChannel.
        val name = getString(R.string.channel_name)
        val descriptionText = getString(R.string.channel_description)
        val importance = NotificationManager.IMPORTANCE_HIGH
        val mChannel = NotificationChannel(Constants.CHANNEL_ID, name, importance)
        mChannel.description = descriptionText

        // Register the channel with the system.
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(mChannel)

        // service is up
        running = true
        alarmStarted = false

        // DEBUG
        // Log.i(aTAG, "onCreate")
    }

    /*
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // DEBUG
        // Log.i(aTAG, "onStartCommand")
        return START_STICKY
    }
    */

    override fun onDestroy() {
        super.onDestroy()

        // stop listener
        val mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        mSensorManager.unregisterListener(this)

        // stop handler thread
        mSensorThread.quitSafely()

        // delete channel
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.deleteNotificationChannel(Constants.CHANNEL_ID)

        running = false
    }

    override fun onBind(intent: Intent?): IBinder? {
        // binding is not needed
        return null
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        if(Sensor.TYPE_LINEAR_ACCELERATION == sensor.type){
            Log.i(aTAG,"Accuracy of our accelerometer has changed to : $accuracy")
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
            // timestamp gives nanoseconds, we use milliseconds -> 10^6 difference
            val sChangeTime: Long = event.timestamp / 1000000
            val sChangeX = event.values[0].toDouble()
            val sChangeY = event.values[1].toDouble()
            val sChangeZ = event.values[2].toDouble()
            // DEBUG
            Log.d(aTAG,"DBG _ received values : $sChangeX _ $sChangeY _ $sChangeZ \n")
            updateArrays(sChangeTime, sChangeX, sChangeY, sChangeZ)
        }
        return
    }

    // Accelerometer sampling is quite irregular, so we need to normalize values over the available sample time deltas
    @Throws(Exception::class)
    private fun updateArrays(newAccTime:Long, newAccX:Double, newAccY:Double, newAccZ:Double){
        // DEBUG
        //Log.i(aTAG, "received update")

        // the first time, we don't change the arrays, but only set the "prevAcc..." values !
        // all the other times, we expand the arrays properly
        if(prevAccTime == 0L){
            calcTime = newAccTime + Constants.ACCELEROMETER_TIME_DELTA_MS
        }else{
            // update the queues linearly using the newly acquired sample
            // at every step, check if any trigger should be triggered
            while (calcTime < newAccTime){
                val interimAccX = linearize( prevAccTime, prevAccX, newAccTime, newAccX, calcTime)
                accArrX.addFirst(interimAccX)

                val interimAccY = linearize( prevAccTime, prevAccY, newAccTime, newAccY, calcTime)
                accArrY.addFirst(interimAccY)

                val interimAccZ = linearize( prevAccTime, prevAccZ, newAccTime, newAccZ, calcTime)
                accArrZ.addFirst(interimAccZ)

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
            // DEBUG: println("_ checkTriggers assertion : ${err.message} , xSize : $arrLenX ; ySize : $arrLenY ; zSize : $arrLenZ ;   \n")
        }
        // val justPoppedAccX: Double? = popLastUntilLen(accArrX, Constants.NUMBER_CONSECUTIVE_SAMPLES_TRG)
        // val justPoppedAccY: Double? = popLastUntilLen(accArrY, Constants.NUMBER_CONSECUTIVE_SAMPLES_TRG)
        // val justPoppedAccZ: Double? = popLastUntilLen(accArrZ, Constants.NUMBER_CONSECUTIVE_SAMPLES_TRG)
        popLastUntilLen(accArrX, Constants.NUMBER_CONSECUTIVE_SAMPLES_TRG)
        popLastUntilLen(accArrY, Constants.NUMBER_CONSECUTIVE_SAMPLES_TRG)
        popLastUntilLen(accArrZ, Constants.NUMBER_CONSECUTIVE_SAMPLES_TRG)

        val triggerEvaluator = StatusDetector(
            accArrX,
            accArrY,
            accArrZ
        )
        val evaluationResult : Constants.FallState = triggerEvaluator.evaluate()

        if (evaluationResult in listOf( Constants.FallState.LateralFall,
                                        Constants.FallState.ForwardsFall,
                                        Constants.FallState.BackwardsFall)) {
            if (!alarmStarted) {
                alarmStarted = true
                sendAlert()
            }
        }

        // DEBUG:
        Log.d(aTAG," !!! _ _ _ results : $evaluationResult \n")
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
        val deltaTrgTimOldTim : Double = timeDiffs(prevTime, trgTime).toDouble()
        val deltaNewTimOldTim : Double = timeDiffs(prevTime, newTime).toDouble()
        val newVal =  prevValue + (newValue - prevValue)*(deltaTrgTimOldTim)/(deltaNewTimOldTim)
        if (abs(newVal) > 100){
            Log.e(aTAG,"!!!!!!!! _ BIG NUMBER ERROR")
        }
        return newVal
    }

    @Throws(Exception::class)
    private fun timeDiffs( ancientTime: Long, recentTime: Long ) : Long{
        // return recentTime - ancientTime
        if (recentTime >= ancientTime){
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
                err.message?.let { Log.e(aTAG, it) }
                Log.e(aTAG,"timeDiffs assert : ${err.message} , recT : $recentTime _ ancT : $ancientTime \n")
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

    // send alert if a fall is detected
    private fun sendAlert() {
        // set up full screen intent
        val fullScreenIntent = Intent(applicationContext, AlarmActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // set up notification
        val nBuilder = NotificationCompat.Builder(applicationContext, Constants.CHANNEL_ID)
            .setSmallIcon(R.mipmap.danger_sym)
            .setContentTitle("Fall Notification")
            .setContentText("FALL DETECTED")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setFullScreenIntent(pendingIntent, true)

        // send notification
        with(NotificationManagerCompat.from(this)) {
            if ((ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED)
                or
                (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.USE_FULL_SCREEN_INTENT
                ) != PackageManager.PERMISSION_GRANTED)
            ) {
                // this should not happens since permissions must have been granted
                // to start this service
                Log.e(aTAG, "ERROR: PERMISSION DENIED")
                return@with
            }
            notify(Constants.NOTIFICATION_ID, nBuilder.build())
        }
    }
}