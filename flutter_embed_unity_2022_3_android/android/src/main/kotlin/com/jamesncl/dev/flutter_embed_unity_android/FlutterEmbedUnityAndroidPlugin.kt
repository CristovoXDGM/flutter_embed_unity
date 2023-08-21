package com.jamesncl.dev.flutter_embed_unity_android

import com.jamesncl.dev.flutter_embed_unity_android.FlutterEmbedConstants.Companion.logTag
import com.jamesncl.dev.flutter_embed_unity_android.FlutterEmbedConstants.Companion.uniqueIdentifier
import com.jamesncl.dev.flutter_embed_unity_android.view.PlatformViewRegistry
import com.jamesncl.dev.flutter_embed_unity_android.view.UnityViewFactory
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodChannel
import io.flutter.Log

/**
 * The plugin implements ActivityAware so that it can respond to Android activity-related events.
 * See https://docs.flutter.dev/release/breaking-changes/plugin-api-migration#uiactivity-plugin
 * and https://api.flutter.dev/javadoc/io/flutter/embedding/engine/plugins/activity/ActivityAware.html
 * */
class FlutterEmbedUnityAndroidPlugin : FlutterPlugin, ActivityAware {
    // The MethodChannel that will the communication between Flutter and native Android
    private lateinit var channel: MethodChannel

    // This is a simple reference holder for the currently active PlatformView, which we can pass to
    // the method call handler (so method calls can interact with the UnityPlayer) and to
    // the PlatformViewFactory (which will update the reference when a new PlatformView is created)
    private val platformViewRegistry = PlatformViewRegistry()
    private val flutterActivityRegistry = FlutterActivityRegistry()
    private val methodCallHandler = FlutterMethodCallHandler(platformViewRegistry)

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        Log.d(logTag, "onAttachedToEngine")
        // Register the method call handler
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, uniqueIdentifier)
        channel.setMethodCallHandler(methodCallHandler)
        // Register the method channel with SendToFlutter static class (which is called by Unity)
        // so messages from Unity can be forwarded on to Flutter
        SendToFlutter.methodChannel = channel

        // Register a view factory
        // On the Flutter side, when we create a PlatformView with our unique identifier:
        // AndroidView(
        //    viewType: Constants.uniqueViewIdentifier,
        // )
        // the UnityViewFactory will be invoked to create a UnityPlatformView:
        flutterPluginBinding
            .platformViewRegistry
            .registerViewFactory(
                uniqueIdentifier,
                // The factory needs the activity which will be received in onAttachedToActivity
                // so that the UnityPlayer can be created. It also needs to be able to update the
                // platformViewRegistry with the current PlatformView each time a PlatformView is created
                UnityViewFactory(flutterActivityRegistry, platformViewRegistry)
            )
    }


    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        Log.d(logTag, "onDetachedFromEngine")
        channel.setMethodCallHandler(null)
    }

    // ActivityAware
    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        Log.d(logTag, "onAttachedToActivity")
        flutterActivityRegistry.activity = binding.activity
    }

    // ActivityAware
    override fun onDetachedFromActivityForConfigChanges() {
        Log.w(
            logTag, "onDetachedFromActivityForConfigChanges - this means the Flutter activity " +
                    "for your app was destroyed to process a configuration change. This scenario is not supported " +
                    "and may lead to unexpected behaviour or crashes. You may be able to prevent configuration " +
                    "changes causing the activity to be destroyed by adding values to the android:configChanges " +
                    "attribute for your main activity in your app's AndroidManifest.xml. For example, if this " +
                    "happened on orientation change, add orientation|keyboardHidden|screenSize|screenLayout to " +
                    "android:configChanges. See " +
                    "https://developer.android.com/guide/topics/resources/runtime-changes#restrict-activity " +
                    "for more information"
        )
        // This will be called if the Activity is destroyed / recreated due to a 'configuration change'.
        // Not sure if this can be handled at all - UnityPlayer is designed to only run in it's own
        // activity, on a separate process, and not to be reused
        // See https://docs.unity3d.com/Manual/UnityasaLibrary-Android.html
        // TODO(Is there any way to handle FlutterActivity onDestroy()?)

        flutterActivityRegistry.activity = null
    }

    // ActivityAware
    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        Log.d(logTag, "onReattachedToActivityForConfigChanges")
        flutterActivityRegistry.activity = binding.activity
    }

    // ActivityAware
    override fun onDetachedFromActivity() {
        Log.w(
            logTag, "onDetachedFromActivity - this means the Flutter activity " +
                    "for your app was destroyed. This scenario is not supported "
        )
        // TODO(Is there any way to handle FlutterActivity onDestroy()?)
        flutterActivityRegistry.activity = null
    }
}