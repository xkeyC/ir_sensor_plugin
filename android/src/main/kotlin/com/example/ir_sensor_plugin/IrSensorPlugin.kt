package com.example.ir_sensor_plugin

import android.content.Context
import android.hardware.ConsumerIrManager
import android.os.Build
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.FlutterPlugin.FlutterPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.PluginRegistry.Registrar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

/**
 * IrSensorPlugin
 */
class IrSensorPlugin : FlutterPlugin, MethodCallHandler {
    private var channel: MethodChannel? = null
    private var irManager: ConsumerIrManager? = null
    private var codeForEmitter: String? = ""
    private var frequency = 38028 //Hz

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "ir_sensor_plugin")
        channel!!.setMethodCallHandler(this)
        val context = flutterPluginBinding.applicationContext
        irManager = context.getSystemService(Context.CONSUMER_IR_SERVICE) as ConsumerIrManager
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "hasIrEmitter" -> result.success(hasIrEmitter())
            "codeForEmitter" -> {
                codeForEmitter = call.argument("codeForEmitter")
                if (codeForEmitter != null) {
                    transmit(result)
                }
            }
            "setFrequency" -> {
                val newFrequency = call.argument<Int>("setFrequency")!!
                if (newFrequency != frequency) {
                    setFrequency(newFrequency)
                    result.success("Frequency Changed")
                }
            }
            "transmitListInt" -> {
                val listInt = call.argument<ArrayList<Int>>("transmitListInt")
                listInt?.let { transmit(result, it) }
            }
            "getCarrierFrequencies" -> if (hasIrEmitter()) {
                result.success(carrierFrequencies)
            } else {
                result.success("[]")
            }
            "getPlatformVersion" -> result.success("Android " + Build.VERSION.RELEASE)
            else -> throw IllegalArgumentException("Unknown method " + call.method)
        }
    }

    /**
     * This method convert the Count Pattern to Duration Pattern by multiplying each value by the pulses
     *
     * @param data String in hex code.
     * @return a int Array with all of the Duration values.
     * @see [This method was created by Randy](http://stackoverflow.com/users/1679571/randy)>
     */
    private fun hex2dec(data: String?): IntArray {
        val list: MutableList<String> = ArrayList(listOf(*data!!.split(" ").toTypedArray()))
        list.removeAt(0)
        var frequency: Int = list.removeAt(0).toInt(16)
        list.removeAt(0)
        list.removeAt(0)
        frequency = (1000000 / (frequency * 0.241246)).toInt()
        val pulses = 1000000 / frequency
        var count: Int
        val pattern = IntArray(list.size)
        for (i in list.indices) {
            count = list[i].toInt(16)
            pattern[i] = count * pulses
        }
        return pattern
    }

    /**
     * Transmit an infrared pattern,
     *
     * @param result return a String "Emitting" if there was no problem in the process.
     */
    private fun transmit(result: MethodChannel.Result) {
        GlobalScope.launch {

        }
        //final int frequency = 38;
        if (codeForEmitter != "") {
            GlobalScope.launch(Dispatchers.IO) {
                irManager!!.transmit(frequency, hex2dec(codeForEmitter))
                GlobalScope.launch(Dispatchers.Main) {
                    result.success("Emitting")
                }
            }
        }
    }

    private fun transmit(result: MethodChannel.Result, listInt: ArrayList<Int>) {
        if (listInt.isNotEmpty()) {
            GlobalScope.launch(Dispatchers.IO) {
                irManager!!.transmit(frequency, convertIntegers(listInt))
                GlobalScope.launch(Dispatchers.Main) {
                    result.success("Emitting")
                }
            }
        }
    }

    /**
     * Check whether the device has an infrared emitter.
     *
     * @return "true" if the device has an infrared emitter, else "false" .
     */
    private fun hasIrEmitter(): Boolean {
        return irManager!!.hasIrEmitter()
    }

    /**
     * Change the frequency with which it is transmitted
     */
    private fun setFrequency(newFrequency: Int) {
        frequency = newFrequency
    }//stringBuilder.append("IR Carrier Frequencies:\n");

    /**
     * Query the infrared transmitter's supported carrier frequencies in `Hertz`.
     */
    private val carrierFrequencies: String
        get() {
            val stringBuilder = StringBuilder()
            val freq = irManager!!.carrierFrequencies
            //stringBuilder.append("IR Carrier Frequencies:\n");
            for (range in freq) {
                stringBuilder.append(
                    String.format(
                        "  %d - %d\n", range.minFrequency,
                        range.maxFrequency
                    )
                )
            }
            return stringBuilder.toString()
        }

    override fun onDetachedFromEngine(binding: FlutterPluginBinding) {
        channel!!.setMethodCallHandler(null)
    }

    companion object {
        fun registerWith(registrar: Registrar) {
            val channel = MethodChannel(registrar.messenger(), "ir_sensor_plugin")
            channel.setMethodCallHandler(IrSensorPlugin())
        }

        private fun convertIntegers(integers: List<Int>): IntArray {
            val ret = IntArray(integers.size)
            val iterator = integers.iterator()
            for (i in ret.indices) {
                ret[i] = iterator.next()
            }
            return ret
        }
    }
}