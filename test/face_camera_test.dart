import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:face_camera/face_camera.dart';

void main() {
  const MethodChannel channel = MethodChannel('face_camera');

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('getPlatformVersion', () async {
    expect(await FaceCamera.platformVersion, '42');
  });
}
