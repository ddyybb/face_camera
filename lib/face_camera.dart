import 'dart:async';

import 'package:flutter/services.dart';

class FaceCamera {
  static const MethodChannel _channel =
      const MethodChannel('face_camera');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }
}
