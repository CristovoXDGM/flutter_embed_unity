package com.jamesncl.dev.flutter_embed_unity_android.unity

import android.annotation.SuppressLint
import android.app.Activity
import android.view.InputDevice
import android.view.MotionEvent
import com.jamesncl.dev.flutter_embed_unity_android.constants.FlutterEmbedConstants.Companion.logTag
import com.unity3d.player.UnityPlayer
import io.flutter.Log


@SuppressLint("ViewConstructor")
class UnityEngineSingleton private constructor (activity: Activity) : UnityPlayer(activity) {

    // We must use a singleton UnityPlayer, because it was never designed to be
    // reused in multiple views. Calling unityPlayer.destroy() will kill the
    // whole process the FlutterActivity runs in. The workaround is to only
    // create UnityPlayer once, and keep it alive when the view is disposed
    // so it can be reattach onto the next view. I think it's okay to suppress
    // warning about static field leak because the UnityPlayer must be kept
    // alive while the app is running
    companion object {
        @SuppressLint("StaticFieldLeak")
        private var singleton: UnityEngineSingleton? = null

        fun getOrCreateInstance(activity: Activity) : UnityEngineSingleton {
            singleton.let{
                if(it != null) {
                    return it
                }
                else {
                    val player = UnityEngineSingleton(activity)
                    singleton = player
                    return player
                }
            }
        }

        // should only be called after getOrCreateInstance!
        fun getInstance(): UnityEngineSingleton? {
            return singleton
        }
    }

    // This is required for Unity to receive touch events
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(motionEvent: MotionEvent): Boolean {
        motionEvent.source = InputDevice.SOURCE_TOUCHSCREEN
        return super.onTouchEvent(motionEvent)
    }

    fun orientationChanged() {
        Log.d(logTag, "UnityPlatformView orientationChanged: pausing and resuming")
        // For some unknown reason, when orientation changes, Unity rendering appears to
        // freeze (not always, but usually). The underlying UnityPlayer is still active and
        // still responds to messages, so it is purely a UI thing. Presumably a bug, although
        // using UnityPlayer to render in a View is not supported anyway (see
        // https://docs.unity3d.com/Manual/UnityasaLibrary-Android.html)
        // As a workaround, pause and resume the player seems to work
        pause()
        resume()
    }

    // Overriding kill() was an experiment to try to resolve app closing / crashing when
    // player.destroy() is called. It didn't work. The problem is that calling player.destroy() also
    // kills the process it is running in (the entire app, or just the activity if the activity
    // uses a separate process as specified by android:process in AndroidManifest.xml, see
    // https://developer.android.com/guide/topics/manifest/activity-element#proc)
    // So, the idea was to override the kill() function to do nothing. This works, however
    // the next time UnityPlayer is created, you then get an exception:
    //
    // JNI DETECTED ERROR IN APPLICATION: JNI NewGlobalRef called with pending exception
    // java.lang.RuntimeException: PlayAssetDeliveryUnityWrapper.init() should be called only once.
    // Use getInstance() instead.
    // F/android_exampl(31048): java_vm_ext.cc:577]   at com.unity3d.player.PlayAssetDeliveryUnityWrapper
    // com.unity3d.player.PlayAssetDeliveryUnityWrapper.init(android.content.Context)
    //
    // See also: https://forum.unity.com/threads/unityplayer-on-android-using-single-process.847555/
    // https://stackoverflow.com/questions/23467994/errors-managing-the-unityplayer-lifecycle-in-a-native-android-application
    // https://stackoverflow.com/questions/36718387/how-to-keep-android-app-running-and-quit-unity-activity
    // https://forum.unity.com/threads/how-to-restart-unity-player-android.567790/
    //
    // So, abandoned this hack in favour of using a singleton UnityPlayer which is never destroyed

//    override fun kill() {
//        Process.killProcess(Process.myPid())
//    }
}
