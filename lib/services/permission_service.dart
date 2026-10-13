import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';

class PermissionService {
  static Future<bool> requestStoragePermission() async {
    if (await Permission.manageExternalStorage.isGranted) {
      return true;
    }
    
    // For Android 11+ (API 30+)
    final status = await Permission.manageExternalStorage.request();
    return status.isGranted;
  }

  static Future<bool> requestCameraPermission() async {
    final status = await Permission.camera.request();
    return status.isGranted;
  }

  static Future<bool> requestLocationPermission() async {
    final status = await Permission.location.request();
    return status.isGranted;
  }

  static Future<bool> requestAllPermissions() async {
    final results = await Future.wait([
      requestStoragePermission(),
      requestCameraPermission(),
      requestLocationPermission(),
    ]);
    return results.every((granted) => granted);
  }
}