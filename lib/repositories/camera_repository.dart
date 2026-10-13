import 'dart:io';
import 'dart:async';
import 'package:flutter/foundation.dart';
import 'package:image/image.dart' as img;
import 'package:path/path.dart' as p;
import '../path_helper.dart';

class CameraRepository {
  static String? currentMonth;
  static int photoCounter = 1;

  static String getNextPhotoName() {
    final now = DateTime.now();
    final month = now.month.toString().padLeft(2, '0');
    if (currentMonth != month) {
      currentMonth = month;
      photoCounter = 1;
    }
    final seriesStr = photoCounter.toString().padLeft(3, '0');
    final name = "img_${month}_$seriesStr";
    photoCounter++;
    return name;
  }

  static Future<String> processAndSavePhoto(String originalPath) async {
    return await compute(_rotateAndSaveInBackground, originalPath);
  }

  static Future<String> _rotateAndSaveInBackground(String originalPath) async {
    try {
      final bytes = await File(originalPath).readAsBytes();
      img.Image? originalImage = img.decodeImage(bytes);
      if (originalImage != null) {
        img.Image rotatedImage = img.copyRotate(originalImage, angle: -90);
        final rotatedBytes = img.encodeJpg(rotatedImage);
        final dir = await AppPathHelper.getAppDirectory();
        final newPath = p.join(dir, '${getNextPhotoName()}.jpg');
        await File(newPath).writeAsBytes(rotatedBytes);
        return newPath;
      }
    } catch (e) {
      debugPrint("Background rotation error: $e");
    }
    return originalPath;
  }
}
