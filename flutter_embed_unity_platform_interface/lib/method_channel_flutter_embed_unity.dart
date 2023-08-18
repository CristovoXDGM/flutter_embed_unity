
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter_embed_unity_platform_interface/flutter_embed_constants.dart';

import 'flutter_embed_unity_platform_interface.dart';

const MethodChannel _channel = MethodChannel(FlutterEmbedConstants.uniqueIdentifier);

/// An implementation of [FlutterEmbedUnityPlatform] that uses method channels.
class MethodChannelFlutterEmbedUnity extends FlutterEmbedUnityPlatform {

  @override
  void sendToUnity(String gameObjectName, String methodName, String data) {
    _channel.invokeMethod(
      "sendToUnity",
      [gameObjectName, methodName, data],
    );
  }

  @override
  void orientationChanged() {
    _channel.invokeMethod(
      "orientationChanged",
    );
  }
}
