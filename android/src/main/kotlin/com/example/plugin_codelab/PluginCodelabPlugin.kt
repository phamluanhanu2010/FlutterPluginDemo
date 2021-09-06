package com.example.plugin_codelab

import android.content.Intent
import android.os.Bundle
import androidx.annotation.NonNull
import com.example.videoeditor.RecordActivity
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.util.GeneratedPluginRegister
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

/** PluginCodelabPlugin */
class PluginCodelabPlugin:
// FlutterActivity() {
//  private val CHANNEL = "plugin_codelab"
//  private val RECORD_VIDEO_ACTIVITY_REQUEST_CODE = 10001
//  private lateinit var resultMethodChanel: MethodChannel.Result
//
//  override fun onCreate(savedInstanceState: Bundle?) {
//    super.onCreate(savedInstanceState)
//    GeneratedPluginRegister.registerGeneratedPlugins(FlutterEngine(baseContext))
//    MethodChannel(
//      flutterEngine?.dartExecutor?.binaryMessenger,
//      CHANNEL
//    ).setMethodCallHandler { call, result ->
//      if (call.method.equals("getPlatformVersion")) {
//        resultMethodChanel = result
//        startRecordActivity()
//      } else {
//        result.notImplemented()
//      }
//    }
//
//  }
//
//  private fun startRecordActivity() {
////        val intent = Intent(this, RecordActivity::class.java)
//    val intent = Intent(this, RecordActivity::class.java)
//    startActivityForResult(intent, RECORD_VIDEO_ACTIVITY_REQUEST_CODE)
//  }
//
//  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//    super.onActivityResult(requestCode, resultCode, data)
//    if (requestCode == RECORD_VIDEO_ACTIVITY_REQUEST_CODE) {
//      resultMethodChanel.success("Close record video activity")
//    }
//  }
//}

  FlutterPlugin, MethodCallHandler, FlutterActivity() {
  private val RECORD_VIDEO_ACTIVITY_REQUEST_CODE = 10001
  private lateinit var resultMethodChanel: MethodChannel.Result
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private lateinit var channel : MethodChannel

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "plugin_codelab")
    channel.setMethodCallHandler(this)
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    if (call.method == "getPlatformVersion") {
      resultMethodChanel = result
      val intent = Intent(this, RecordActivity::class.java)
      startActivityForResult(intent, RECORD_VIDEO_ACTIVITY_REQUEST_CODE)
//      result.success("Android ${android.os.Build.VERSION.RELEASE}")
    } else {
      result.notImplemented()
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == RECORD_VIDEO_ACTIVITY_REQUEST_CODE) {
      resultMethodChanel.success("Close record video activity")
    }
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }
}
