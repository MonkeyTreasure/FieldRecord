import 'dart:io';
import 'package:flutter/material.dart';
import 'package:camera/camera.dart';
import 'package:location/location.dart';
import 'package:permission_handler/permission_handler.dart';

class AppGalleryData {
  static final List<Map<String, dynamic>> capturedPhotos = [];
}

class FieldCameraWidget extends StatefulWidget {
  final VoidCallback? onPhotoCaptured;
  const FieldCameraWidget({super.key, this.onPhotoCaptured});

  @override
  State<FieldCameraWidget> createState() => _FieldCameraWidgetState();
}

class _FieldCameraWidgetState extends State<FieldCameraWidget> {
  CameraController? _controller;
  List<CameraDescription>? _cameras;
  bool _isInitialized = false;
  bool _hasPermission = false;
  LocationData? _locationData;
  String? _error;

  @override
  void initState() {
    super.initState();
    _initCamera();
  }

  Future<void> _initCamera() async {
    try {
      // Request camera permission
      final cameraStatus = await Permission.camera.request();
      if (!cameraStatus.isGranted) {
        setState(() => _error = 'Izin kamera ditolak');
        return;
      }

      _cameras = await availableCameras();
      if (_cameras!.isEmpty) {
        setState(() => _error = 'Tidak ada kamera tersedia');
        return;
      }

      _controller = CameraController(
        _cameras![0],
        ResolutionPreset.medium,
        enableAudio: false,
      );
      await _controller!.initialize();
      
      // Request location permission
      final location = Location();
      _locationData = await location.getLocation();

      if (mounted) {
        setState(() {
          _isInitialized = true;
          _hasPermission = true;
          _error = null;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() => _error = 'Error: $e');
      }
    }
  }

  @override
  void dispose() {
    _controller?.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    // Show error state
    if (_error != null) {
      return Container(
        width: 140,
        height: 140,
        decoration: BoxDecoration(color: Colors.red.withOpacity(0.2), borderRadius: BorderRadius.circular(12), border: Border.all(color: Colors.redAccent)),
        child: Center(
          child: Padding(
            padding: const EdgeInsets.all(8.0),
            child: Text(_error!, style: const TextStyle(color: Colors.redAccent, fontSize: 10), textAlign: TextAlign.center),
          ),
        ),
      );
    }

    // Show loading state
    if (!_isInitialized || _controller == null) {
      return Container(
        width: 140,
        height: 140,
        decoration: BoxDecoration(color: Colors.black87, borderRadius: BorderRadius.circular(12)),
        child: const Center(child: CircularProgressIndicator(color: Colors.greenAccent, strokeWidth: 2)),
      );
    }

    // Show camera preview
    return ClipRRect(
      borderRadius: BorderRadius.circular(12),
      child: SizedBox(
        width: 140,
        height: 140,
        child: Stack(
          fit: StackFit.expand,
          children: [
            CameraPreview(_controller!),
            Positioned(
              bottom: 8,
              left: 8,
              right: 8,
              child: ElevatedButton.icon(
                style: ElevatedButton.styleFrom(
                  backgroundColor: Colors.green,
                  foregroundColor: Colors.white,
                  padding: const EdgeInsets.symmetric(vertical: 4),
                  minimumSize: const Size(0, 32),
                ),
                onPressed: () async {
                  try {
                    final image = await _controller!.takePicture();
                    final lat = _locationData?.latitude ?? -4.20021;
                    final lng = _locationData?.longitude ?? 105.34728;

                    AppGalleryData.capturedPhotos.insert(0, {
                      "path": image.path,
                      "title": "Temuan 1x1 Widget",
                      "time": DateTime.now().toString().split('.').first,
                      "lat": lat,
                      "lng": lng,
                    });

                    if (widget.onPhotoCaptured != null) {
                      widget.onPhotoCaptured!();
                    }

                    if (mounted) {
                      ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(content: Text('Foto 1x1 & Pinpoint tersimpan!'), duration: Duration(seconds: 1)),
                      );
                    }
                  } catch (_) {}
                },
                icon: const Icon(Icons.camera_alt, size: 14),
                label: const Text('Capture', style: TextStyle(fontSize: 11)),
              ),
            ),
          ],
        ),
      ),
    );
  }
}