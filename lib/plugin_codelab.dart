
import 'dart:async';

import 'package:flutter/services.dart';

class PluginCodelab {
  static const MethodChannel _channel =
      const MethodChannel('plugin_codelab');

  static Future<String?> get platformVersion async {
    final String? version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }
}
