import 'dart:io';
import 'dart:async';
import 'dart:math' as math;
import 'package:flutter/material.dart';
import 'package:camera/camera.dart';
import 'package:sensors_plus/sensors_plus.dart';
import 'package:path/path.dart' as p;
import 'package:image/image.dart' as img;
import '../path_helper.dart';
import 'gallery_screen.dart';

class AppGalleryData {
  static final List<Map<String, dynamic>> capturedPhotos = [];
  static String? currentMonth;
  static int photoCounter = 1;
  static String? _appDir;

  static Future<String> getAppDirectory() async {
    final dir = await AppPathHelper.getAppDirectory();
    _appDir = dir;
    return dir;
  }

  // Scan existing folder and set counter based on max series for current month
  static Future<void> rescanFolder() async {
    if (_appDir == null) await getAppDirectory();
    final directory = Directory(_appDir!);
    if (!await directory.exists()) return;
    final files = directory.listSync().whereType<File>();
    capturedPhotos.clear();
    int maxSeries = 0;
    final now = DateTime.now();
    final monthStr = now.month.toString().padLeft(2, '0');
    for (var f in files) {
      final name = p.basename(f.path);
      final match = RegExp(r"img_(\d{2})_(\d{3})").firstMatch(name);
      if (match != null) {
        final fileMonth = match.group(1);
        final series = int.parse(match.group(2)!);
        if (fileMonth == monthStr && series > maxSeries) maxSeries = series;
        capturedPhotos.add({
          "path": f.path,
          "title": p.basenameWithoutExtension(f.path),
          "time": f.statSync().modified.toString().split('.').first,
        });
      }
    }
    // sort newest first
    capturedPhotos.sort((a, b) => (b['time'] as String).compareTo(a['time'] as String));
    currentMonth = monthStr;
    photoCounter = maxSeries + 1;
  }

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

  // Save photo to AgriOps folder with proper naming and watermark
  static Future<File> savePhotoToAppDir(File tempFile, String location, String gps, String bearing, String orientation) async {
    final appDir = await getAppDirectory();
    final name = getNextPhotoName();
    final targetPath = p.join(appDir, '$name.jpg');
    
    // Watermark drawing
    final bytes = await tempFile.readAsBytes();
    final img.Image? originalImage = img.decodeImage(bytes);
    
    img.Image? processedImage = originalImage;
    if (processedImage != null) {
      // 1. rotate if needed (already handled earlier, but double-check)
      // 2. Crop to target aspect ratio
      double targetAspect = orientation == 'Landscape' ? (4/3) : (3/4);
      int srcW = processedImage.width;
      int srcH = processedImage.height;
      double srcAspect = srcW / srcH;
      if (srcAspect > targetAspect) {
        // too wide – crop width
        int cropW = (srcH * targetAspect).round();
        int offsetX = ((srcW - cropW) / 2).round();
        processedImage = img.copyCrop(processedImage, x: offsetX, y: 0, width: cropW, height: srcH);
      } else if (srcAspect < targetAspect) {
        // too tall – crop height
        int cropH = (srcW / targetAspect).round();
        int offsetY = ((srcH - cropH) / 2).round();
        processedImage = img.copyCrop(processedImage, x: 0, y: offsetY, width: srcW, height: cropH);
      }
      
      // 3. Add watermark bar
      final int barHeight = 80;
      final img.Image watermarkedImage = img.Image(width: processedImage.width, height: processedImage.height + barHeight);
      img.fill(watermarkedImage, color: img.ColorRgb8(255, 255, 255)); // White background
      img.compositeImage(watermarkedImage, processedImage, dstX: 0, dstY: 0);
      
      // Texts
      img.drawString(watermarkedImage, "$location\n${DateTime.now().toString().split('.').first}\n$gps", x: 10, y: processedImage.height + 10, color: img.ColorRgb8(0,0,0), font: img.arial24);
      img.drawString(watermarkedImage, "W $bearing", x: processedImage.width - 100, y: processedImage.height + 10, color: img.ColorRgb8(0,0,255), font: img.arial24);
      
      await File(targetPath).writeAsBytes(img.encodeJpg(watermarkedImage));
    } else {
      await tempFile.copy(targetPath);
    }
    
    final savedFile = File(targetPath);
    capturedPhotos.insert(0, {
      "path": savedFile.path,
      "title": name,
      "time": DateTime.now().toString().split('.').first,
    });
    return savedFile;
  }

}

class FieldCameraScreen extends StatefulWidget {
  const FieldCameraScreen({super.key});

  @override
  State<FieldCameraScreen> createState() => _FieldCameraScreenState();
}

class _FieldCameraScreenState extends State<FieldCameraScreen> {
  CameraController? _controller;
  List<CameraDescription>? _cameras;
  bool _isInitialized = false;
  bool _isTakingPicture = false;
  StreamSubscription<AccelerometerEvent>? _accelerometerSubscription;
  StreamSubscription<MagnetometerEvent>? _magnetometerSubscription;
  double _heading = 0.0;
  String _orientationLabel = 'Portrait';

  double _zoomLevel = 1.0;
  bool _flashOn = false;

  double _mockLat = -4.20021;
  double _mockLng = 105.34728;
  String _searchLocationName = 'Divisi 3 - Blok K30 (SBYE)';

  @override
  void initState() {
    super.initState();
    _initCamera();
    _initOrientationListener();
    _initCompassListener();
    // Load any existing photos from the app's internal folder and set the series counter
    AppGalleryData.rescanFolder();
  }

  void _initCompassListener() {
    _magnetometerSubscription = magnetometerEvents.listen((event) {
      if (!mounted) return;
      double heading = math.atan2(event.y, event.x) * (180 / math.pi);
      heading = (heading + 360) % 360;
      setState(() => _heading = heading);
    });
  }

  bool _isUltraWideSupported = false;
  
  Future<void> _initCamera() async {
    try {
      _cameras = await availableCameras();
      if (_cameras != null && _cameras!.isNotEmpty) {
        _controller = CameraController(
          _cameras![0],
          ResolutionPreset.high,
          enableAudio: false,
        );
        await _controller!.initialize();
        
        // Check if zoom out is possible (usually ultra-wide)
        final minZoom = await _controller!.getMinZoomLevel();
        _isUltraWideSupported = minZoom < 1.0;
        
        if (mounted) setState(() => _isInitialized = true);
      }
    } catch (e) {
      debugPrint("Camera init error: $e");
    }
  }

  void _initOrientationListener() {
    _accelerometerSubscription = accelerometerEvents.listen((event) {
      if (!mounted) return;
      if (event.x.abs() > event.y.abs() + 1.5) {
        if (_orientationLabel != 'Landscape') setState(() => _orientationLabel = 'Landscape');
      } else if (event.y.abs() > event.x.abs() + 1.5) {
        if (_orientationLabel != 'Portrait') setState(() => _orientationLabel = 'Portrait');
      }
    });
  }

  @override
  void dispose() {
    _accelerometerSubscription?.cancel();
    _magnetometerSubscription?.cancel();
    _controller?.dispose();
    super.dispose();
  }

  Future<void> _toggleFlash() async {
    if (_controller == null) return;
    _flashOn = !_flashOn;
    await _controller!.setFlashMode(_flashOn ? FlashMode.torch : FlashMode.off);
    setState(() {});
  }

  Future<void> _setZoom(double zoom) async {
    if (_controller == null || !_controller!.value.isInitialized) return;
    try {
      final minZoom = await _controller!.getMinZoomLevel();
      final maxZoom = await _controller!.getMaxZoomLevel();
      final targetZoom = zoom.clamp(minZoom, maxZoom);
      debugPrint("Setting zoom: $targetZoom (range: $minZoom - $maxZoom)");
      
      _zoomLevel = targetZoom;
      await _controller!.setZoomLevel(targetZoom);
      setState(() {});
    } catch (e) {
      debugPrint("Zoom error: $e");
    }
  }

  void _switchCamera() async {
    if (_cameras == null || _cameras!.length < 2) return;
    int newIndex = (_cameras!.indexOf(_controller!.description) + 1) % _cameras!.length;
    _controller = CameraController(
      _cameras![newIndex],
      ResolutionPreset.high,
      enableAudio: false,
    );
    await _controller!.initialize();
    setState(() {});
  }

  void _showLocationSearchDialog() {
    String query = '';
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        backgroundColor: const Color(0xFF1E1E1E),
        title: const Text('Cari / Mock Lokasi', style: TextStyle(color: Colors.white)),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              decoration: const InputDecoration(
                hintText: 'Masukkan Nama Lokasi / Blok',
                hintStyle: TextStyle(color: Colors.white54),
                border: OutlineInputBorder(),
              ),
              style: const TextStyle(color: Colors.white),
              onChanged: (v) => query = v,
            ),
            const SizedBox(height: 12),
            ListTile(
              title: const Text('Divisi 1 - Blok A10', style: TextStyle(color: Colors.white)),
              onTap: () {
                setState(() {
                  _searchLocationName = 'Divisi 1 - Blok A10';
                  _mockLat = -4.18221;
                  _mockLng = 105.32145;
                });
                Navigator.pop(context);
              },
            ),
            ListTile(
              title: const Text('Divisi 3 - Blok K30', style: TextStyle(color: Colors.white)),
              onTap: () {
                setState(() {
                  _searchLocationName = 'Divisi 3 - Blok K30';
                  _mockLat = -4.20021;
                  _mockLng = 105.34728;
                });
                Navigator.pop(context);
              },
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Batal', style: TextStyle(color: Colors.amber)),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(backgroundColor: Colors.greenAccent),
            onPressed: () {
              if (query.isNotEmpty) {
                setState(() => _searchLocationName = query);
              }
              Navigator.pop(context);
            },
            child: const Text('Gunakan', style: TextStyle(color: Colors.black)),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    if (!_isInitialized || _controller == null) {
      return const Scaffold(
        backgroundColor: Colors.black,
        body: Center(child: CircularProgressIndicator(color: Colors.greenAccent)),
      );
    }

    final Size screenSize = MediaQuery.of(context).size;
    final bool isPortrait = screenSize.height >= screenSize.width;

    final double boxWidth = isPortrait ? screenSize.width : screenSize.height * (4.0 / 3.0);
    final double boxHeight = isPortrait ? screenSize.width * (4.0 / 3.0) : screenSize.height;
    final bool isLandscapeOrientation = _orientationLabel == 'Landscape';

    return Scaffold(
      backgroundColor: Colors.black,
      body: SafeArea(
        child: Column(
          children: [
            // Top Bar
            Padding(
              padding: const EdgeInsets.only(left: 16, right: 16, top: 8, bottom: 8),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  _buildTopIcon(
                    icon: _flashOn ? Icons.flash_on : Icons.flash_off,
                    onTap: _toggleFlash,
                    size: 40,
                  ),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                    decoration: BoxDecoration(
                      color: Colors.black54,
                      borderRadius: BorderRadius.circular(6),
                      border: Border.all(color: Colors.white24),
                    ),
                    child: const Text('3:4', style: TextStyle(color: Colors.white, fontSize: 12, fontWeight: FontWeight.w500)),
                  ),
                  _buildTopIcon(
                    icon: Icons.settings,
                    onTap: () {},
                    size: 40,
                  ),
                ],
              ),
            ),

            // Viewfinder
            Expanded(
              child: Center(
                child: SizedBox(
                  width: boxWidth,
                  height: boxHeight,
                  child: ClipRRect(
                    borderRadius: BorderRadius.circular(20),
                    child: Stack(
                      fit: StackFit.expand,
                      children: [
                        OverflowBox(
                          alignment: Alignment.center,
                          child: FittedBox(
                            fit: BoxFit.cover,
                            child: SizedBox(
                              width: _controller!.value.previewSize?.height ?? boxWidth,
                              height: _controller!.value.previewSize?.width ?? boxHeight,
                              child: CameraPreview(_controller!),
                            ),
                          ),
                        ),

                        // GPS & COMPASS MOCKUP
                        Positioned(
                          bottom: isLandscapeOrientation ? null : 55,
                          left: 12,
                          top: isLandscapeOrientation ? 12 : null,
                          child: isLandscapeOrientation
                              ? RotatedBox(
                                  quarterTurns: 1,
                                  child: SizedBox(
                                    width: boxHeight - 24,
                                    child: _buildGpsAndCompassCard(),
                                  ),
                                )
                              : SizedBox(
                                  width: boxWidth - 24,
                                  child: _buildGpsAndCompassCard(),
                                ),
                        ),

                        // ZOOM SELECTOR
                        Positioned(
                          bottom: 12,
                          left: 0,
                          right: 0,
                          child: Center(
                            child: Container(
                              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                              decoration: BoxDecoration(
                                color: Colors.black54,
                                borderRadius: BorderRadius.circular(20),
                                border: Border.all(color: Colors.white24),
                              ),
                              child: Row(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  if (_isUltraWideSupported) _buildZoomButton('0.5', 0.5),
                                  _buildZoomButton('1x', 1.0),
                                  _buildZoomButton('2', 2.0),
                                ],
                              ),
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ),

            // Bottom Bar
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 48, horizontal: 24),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                children: [
                  // Thumbnail (Left)
                  if (AppGalleryData.capturedPhotos.isNotEmpty)
                    GestureDetector(
                      onTap: () {
                        Navigator.push(
                          context,
                          MaterialPageRoute(builder: (_) => const MainGalleryTabOnly()),
                        );
                      },
                      child: Container(
                        width: 56,
                        height: 56,
                        decoration: BoxDecoration(
                          borderRadius: BorderRadius.circular(8),
                          border: Border.all(color: Colors.greenAccent, width: 1.5),
                        ),
                        child: ClipRRect(
                          borderRadius: BorderRadius.circular(6),
                          child: Image.file(
                            File(AppGalleryData.capturedPhotos.first['path']),
                            fit: BoxFit.cover,
                          ),
                        ),
                      ),
                    )
                  else
                    Container(
                      width: 56,
                      height: 56,
                      decoration: BoxDecoration(
                        borderRadius: BorderRadius.circular(8),
                        border: Border.all(color: Colors.white24),
                      ),
                      child: const Icon(Icons.photo_library, color: Colors.white38, size: 24),
                    ),

                  // Shutter (Center)
                  _isTakingPicture
                      ? const CircularProgressIndicator(color: Colors.greenAccent, strokeWidth: 3)
                      : GestureDetector(
                          onTap: () async {
                            setState(() => _isTakingPicture = true);
                            try {
                              final image = await _controller!.takePicture();
                              // Correct rotation for landscape mode (rotate 90 left as requested)
                              String finalPath = image.path;
                              if (_orientationLabel == 'Landscape') {
                                try {
                                  final bytes = await File(image.path).readAsBytes();
                                  final img.Image? originalImage = img.decodeImage(bytes);
                                  if (originalImage != null) {
                                    // Rotate 90 degrees counter-clockwise (left)
                                    final img.Image rotatedImage = img.copyRotate(originalImage, angle: -90);
                                    final rotatedFile = File(image.path)..writeAsBytesSync(img.encodeJpg(rotatedImage));
                                    finalPath = rotatedFile.path;
                                  }
                                } catch (e) {
                                  debugPrint("Rotation error: $e");
                                }
                              }
                              await AppGalleryData.savePhotoToAppDir(File(finalPath), _searchLocationName, 'Lat: ${_mockLat.toStringAsFixed(5)}, Lng: ${_mockLng.toStringAsFixed(5)}', '${_heading.toStringAsFixed(0)}°', _orientationLabel);
                              setState(() => _isTakingPicture = false);
                            } catch (e) {
                              setState(() => _isTakingPicture = false);
                              debugPrint("Capture error: $e");
                            }
                          },
                          child: Container(
                            width: 72,
                            height: 72,
                            decoration: BoxDecoration(
                              shape: BoxShape.circle,
                              border: Border.all(color: Colors.white, width: 4),
                              color: Colors.transparent,
                            ),
                          ),
                        ),

                  // Switch Camera (Right)
                  _buildTopIcon(
                    icon: Icons.flip_camera_ios,
                    onTap: _switchCamera,
                    size: 56,
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildTopIcon({
    required IconData icon,
    required VoidCallback onTap,
    double size = 44,
  }) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        width: size,
        height: size,
        decoration: BoxDecoration(
          color: Colors.black54,
          shape: BoxShape.circle,
          border: Border.all(color: Colors.white24),
        ),
        child: Icon(icon, color: Colors.white, size: 22),
      ),
    );
  }

  Widget _buildZoomButton(String label, double zoom) {
    final bool active = (_zoomLevel - zoom).abs() < 0.01;
    return GestureDetector(
      onTap: () => _setZoom(zoom),
      child: Container(
        margin: const EdgeInsets.symmetric(horizontal: 6),
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 6),
        decoration: BoxDecoration(
          color: active ? Colors.deepOrangeAccent : Colors.transparent,
          borderRadius: BorderRadius.circular(16),
        ),
        child: Text(
          label,
          style: TextStyle(
            color: active ? Colors.white : Colors.white70,
            fontSize: 13,
            fontWeight: active ? FontWeight.bold : FontWeight.normal,
          ),
        ),
      ),
    );
  }

  Widget _buildGpsAndCompassCard() {
    String directionStr = '';
    if (_heading >= 337.5 || _heading < 22.5) {
      directionStr = 'Utara (0°/360°)';
    } else if (_heading >= 22.5 && _heading < 67.5) {
      directionStr = 'Timur Laut (45°)';
    } else if (_heading >= 67.5 && _heading < 112.5) {
      directionStr = 'Timur (90°)';
    } else if (_heading >= 112.5 && _heading < 157.5) {
      directionStr = 'Tenggara (135°)';
    } else if (_heading >= 157.5 && _heading < 202.5) {
      directionStr = 'Selatan (180°)';
    } else if (_heading >= 202.5 && _heading < 247.5) {
      directionStr = 'Barat Daya (225°)';
    } else if (_heading >= 247.5 && _heading < 292.5) {
      directionStr = 'Barat (270°)';
    } else {
      directionStr = 'Barat Laut (315°)';
    }

    return GestureDetector(
      onTap: _showLocationSearchDialog,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        decoration: BoxDecoration(
          color: Colors.black87.withOpacity(0.85),
          borderRadius: BorderRadius.circular(8),
          border: Border.all(color: Colors.greenAccent, width: 0.8),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisSize: MainAxisSize.min,
          children: [
            // Baris 1: GPS
            Row(
              children: [
                const Text('📍 ', style: TextStyle(fontSize: 11)),
                Expanded(
                  child: Text(
                    '$_searchLocationName | Lat: ${_mockLat.toStringAsFixed(5)}, Lng: ${_mockLng.toStringAsFixed(5)}',
                    style: const TextStyle(color: Colors.white, fontSize: 11, fontWeight: FontWeight.bold),
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 1),
                  decoration: BoxDecoration(
                    color: Colors.green.withOpacity(0.3),
                    borderRadius: BorderRadius.circular(3),
                  ),
                  child: Text(_orientationLabel, style: const TextStyle(color: Colors.greenAccent, fontSize: 8)),
                ),
              ],
            ),
            const SizedBox(height: 3),
            const Divider(color: Colors.white24, height: 1, thickness: 0.5),
            const SizedBox(height: 3),
            // Baris 2: Kompas
            Row(
              children: [
                const Text('🧭 ', style: TextStyle(fontSize: 11)),
                Expanded(
                  child: Text(
                    'Kompas: ${_heading.toStringAsFixed(1)}° ($directionStr)',
                    style: const TextStyle(color: Colors.greenAccent, fontSize: 10, fontWeight: FontWeight.w500),
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
                const Icon(Icons.search, color: Colors.amber, size: 14),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class MainGalleryTabOnly extends StatelessWidget {
  const MainGalleryTabOnly({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Galeri Foto App'),
        backgroundColor: const Color(0xFF1B381B),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => Navigator.pop(context),
        ),
      ),
      body: GalleryScreen(),
    );
  }
}