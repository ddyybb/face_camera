import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

main() async {
  await _initVideoPlugin();
  runApp(new MyApp());
}

final MethodChannel _channel = const MethodChannel('flutter.io/SurfaceTest');
int _textureId;

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return new MaterialApp(
      title: 'Test',
      home: new Scaffold(
          body: Column(
        children: <Widget>[
          AspectRatio(child: new VideoView(), aspectRatio: 4 / 3),
          Expanded(
              child: Center(
                  child: new FlatButton(
                      padding: EdgeInsets.only(
                        left: 25.0, 
                        right: 25.0, 
                        top: 15.0, 
                        bottom: 15.0
                      ),
                      onPressed: _render,
                      child: new Text("render"))))
        ],
      )
          ),
    );
  }
}

Future<void> _render() async {
  _channel.invokeMethod(
    'render',
    <String, dynamic>{},
  );
}

class VideoView extends StatefulWidget {
  @override
  State createState() {
    return new VideoState();
  }
}

_initVideoPlugin() async {
  final Map<dynamic, dynamic> response = await _channel.invokeMethod('init');
  _textureId = response['textureId'];
}

class VideoState extends State<VideoView> {
  @override
  Widget build(BuildContext context) {
    return new Texture(textureId: _textureId);
  }
}
