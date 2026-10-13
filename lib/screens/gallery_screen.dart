import 'dart:io';
import 'dart:async';
import 'package:flutter/material.dart';
import 'dart:math' as math;
import 'package:flutter_map/flutter_map.dart';
import 'package:latlong2/latlong.dart' as ll;
import 'package:location/location.dart';
import 'package:sensors_plus/sensors_plus.dart';
import 'package:path/path.dart' as p;
import '../path_helper.dart';

class GalleryScreen extends StatefulWidget {
  const GalleryScreen({super.key});

  @override
  State<GalleryScreen> createState() => _GalleryScreenState();
}

class _GalleryScreenState extends State<GalleryScreen> {
  ll.LatLng? _userLocation;
  bool _isSatellite = true;
  bool _isNorthLocked = true;
  double _heading = 0.0;
  double _currentZoom = 18.0;
  final MapController _mapController = MapController();
  StreamSubscription<LocationData>? _locationSubscription;
  StreamSubscription<MagnetometerEvent>? _magnetometerSubscription;
  final Location _location = Location();
  
  List<Map<String, dynamic>> _photos = [];

  @override
  void initState() {
    super.initState();
    _startRealtimeLocationTracking();
    _startCompassListener();
    _loadPhotos();
  }

  @override
  void dispose() {
    _locationSubscription?.cancel();
    _magnetometerSubscription?.cancel();
    _mapController.dispose();
    super.dispose();
  }

  Future<void> _loadPhotos() async {
    final appDir = await AppPathHelper.getAppDirectory();
    final dir = Directory(appDir);
    if (!await dir.exists()) return;
    
    final files = dir.listSync().where((f) => f is File && (f.path.toLowerCase().endsWith('.jpg') || f.path.toLowerCase().endsWith('.jpeg'))).cast<File>();
    final loaded = <Map<String, dynamic>>[];
    for (var file in files) {
      final stat = await file.stat();
      final name = p_basename(file.path);
      
      // Try to find a matching .md file for notes
      String note = '';
      final notesDir = Directory(p.join(appDir, 'Notes'));
      if (await notesDir.exists()) {
        final noteFiles = notesDir.listSync().where((f) => f is File && f.path.contains(name)).cast<File>();
        if (noteFiles.isNotEmpty) {
          final content = await noteFiles.first.readAsString();
          final lines = content.split('\n');
          note = lines.length > 3 ? lines[3] : ''; // Simple heuristic for note content
        }
      }

      loaded.add({
        'path': file.path,
        'title': name,
        'time': stat.modified.toString().split('.').first,
        'date': '${stat.modified.day.toString().padLeft(2, '0')}.${stat.modified.month.toString().padLeft(2, '0')}.${stat.modified.year.toString().substring(2)}',
        'hour': '${stat.modified.hour.toString().padLeft(2, '0')}:${stat.modified.minute.toString().padLeft(2, '0')}',
        'lat': -4.20021,
        'lng': 105.34728,
        'note': note,
        'orientation': name.contains('img_') ? 'Landscape' : 'Portrait',
      });
    }
    loaded.sort((a, b) => (b['time'] as String).compareTo(a['time'] as String));
    if (mounted) setState(() => _photos = loaded);
  }

  String p_basename(String path) => path.split(Platform.pathSeparator).last.split('.').first;

  Future<void> _startRealtimeLocationTracking() async {
    try {
      bool serviceEnabled = await _location.serviceEnabled();
      if (!serviceEnabled) {
        serviceEnabled = await _location.requestService();
        if (!serviceEnabled) return;
      }
      PermissionStatus permissionGranted = await _location.hasPermission();
      if (permissionGranted == PermissionStatus.denied) {
        permissionGranted = await _location.requestPermission();
        if (permissionGranted != PermissionStatus.granted) return;
      }
      await _location.changeSettings(accuracy: LocationAccuracy.high, interval: 2000, distanceFilter: 1);
      _locationSubscription = _location.onLocationChanged.listen((locData) {
        if (locData.latitude != null && locData.longitude != null && mounted) {
          setState(() => _userLocation = ll.LatLng(locData.latitude!, locData.longitude!));
        }
      });
      final initialLoc = await _location.getLocation();
      if (initialLoc.latitude != null && initialLoc.longitude != null && mounted) {
        final initPos = ll.LatLng(initialLoc.latitude!, initialLoc.longitude!);
        setState(() => _userLocation = initPos);
        _mapController.move(initPos, 18.0);
      }
    } catch (_) {}
  }

  void _startCompassListener() {
    _magnetometerSubscription = magnetometerEvents.listen((event) {
      if (!mounted) return;
      double heading = math.atan2(event.y, event.x) * (180 / math.pi);
      heading = (heading + 360) % 360;
      setState(() => _heading = heading);
    });
  }

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: 4,
      child: Scaffold(
        backgroundColor: const Color(0xFF121212),
        appBar: AppBar(
          automaticallyImplyLeading: false,
          backgroundColor: const Color(0xFF1E1E1E),
          elevation: 0,
          title: const Text('AgriOps Gallery & Map', style: TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.w500)),
          bottom: const TabBar(
            isScrollable: false,
            indicatorColor: Colors.amber,
            labelColor: Colors.amber,
            unselectedLabelColor: Colors.white60,
            tabs: [
              Tab(text: 'Timeline'),
              Tab(text: 'Gallery'),
              Tab(text: 'Map'),
              Tab(text: 'Favorites'),
            ],
          ),
        ),
        body: TabBarView(
          children: [
            _buildTimelineTab(),
            _buildGooglePhotosGrid(),
            _buildMapTab(context),
            _buildFavoritesTab(),
          ],
        ),
      ),
    );
  }

  Widget _buildTimelineTab() {
    if (_photos.isEmpty) {
      return const Center(child: Text('Belum ada foto di folder AgriOps.', style: TextStyle(color: Colors.white60)));
    }

    // Group photos by Year / Date or display in 3-column grid with timeline line
    return SingleChildScrollView(
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 84),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Left side: Timeline Date & Line indicator
          SizedBox(
            width: 50,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  '2026',
                  style: TextStyle(color: Colors.amber, fontWeight: FontWeight.bold, fontSize: 13),
                ),
                const SizedBox(height: 4),
                const Text(
                  '30/08',
                  style: TextStyle(color: Colors.white60, fontSize: 11),
                ),
                const SizedBox(height: 12),
                Container(
                  width: 2,
                  height: 400,
                  color: Colors.greenAccent.withOpacity(0.3),
                ),
              ],
            ),
          ),
          const SizedBox(width: 8),
          // Right side: 3-column grid of 1:1 photos
          Expanded(
            child: GridView.builder(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              itemCount: _photos.length,
              gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount: 3,
                crossAxisSpacing: 6,
                mainAxisSpacing: 6,
                childAspectRatio: 1.0,
              ),
              itemBuilder: (context, index) {
                final photo = _photos[index];
                return GestureDetector(
                  onTap: () => _showFullscreenPhoto(context, index),
                  child: Container(
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(4),
                      border: Border.all(color: Colors.white24),
                    ),
                    child: ClipRRect(
                      borderRadius: BorderRadius.circular(4),
                      child: Image.file(
                        File(photo['path']),
                        fit: BoxFit.cover,
                      ),
                    ),
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildGooglePhotosGrid() {
    if (_photos.isEmpty) {
      return const Center(child: Text('Belum ada foto.', style: TextStyle(color: Colors.white60)));
    }
    return GridView.builder(
      padding: const EdgeInsets.fromLTRB(4, 8, 4, 84),
      itemCount: _photos.length,
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 3,
        crossAxisSpacing: 2,
        mainAxisSpacing: 2,
      ),
      itemBuilder: (context, index) {
        final photo = _photos[index];
        return GestureDetector(
          onTap: () => _showFullscreenPhoto(context, index),
          child: Image.file(File(photo['path']), fit: BoxFit.cover),
        );
      },
    );
  }

  void _showFullscreenPhoto(BuildContext context, int initialIndex) {
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => _FullscreenGalleryView(photos: _photos, initialIndex: initialIndex),
      ),
    );
  }

  Widget _buildMapTab(BuildContext context) {
    final markers = _photos.map((photo) {
      return Marker(
        point: ll.LatLng(photo['lat'], photo['lng']),
        width: 30,
        height: 30,
        child: GestureDetector(
          onTap: () => _showPhotoDetailModal(context, photo),
          child: Container(
            decoration: BoxDecoration(
              color: Colors.red,
              shape: BoxShape.circle,
              border: Border.all(color: Colors.white, width: 2),
              boxShadow: const [BoxShadow(color: Colors.black45, blurRadius: 4, offset: Offset(0, 2))],
            ),
          ),
        ),
      );
    }).toList();

    if (_userLocation != null) {
      markers.add(Marker(
        point: _userLocation!,
        width: 48,
        height: 48,
        child: Stack(
          alignment: Alignment.center,
          children: [
            Transform.rotate(
              angle: (_heading * math.pi / 180),
              child: CustomPaint(
                size: const Size(48, 48),
                painter: DirectionBeamPainter(),
              ),
            ),
            Container(
              width: 36,
              height: 36,
              decoration: BoxDecoration(
                color: Colors.blue.withOpacity(0.2),
                shape: BoxShape.circle,
              ),
            ),
            Container(
              width: 18,
              height: 18,
              decoration: BoxDecoration(
                color: const Color(0xFF4285F4),
                shape: BoxShape.circle,
                border: Border.all(color: Colors.white, width: 2.5),
                boxShadow: const [BoxShadow(color: Colors.black38, blurRadius: 3, offset: Offset(0, 1))],
              ),
            ),
          ],
        ),
      ));
    }

    String scaleText = '10m / 30ft';
    if (_currentZoom > 18) {
      scaleText = '5m / 15ft';
    } else if (_currentZoom > 16) {
      scaleText = '20m / 60ft';
    } else if (_currentZoom > 14) {
      scaleText = '100m / 300ft';
    }

    return Stack(
      children: [
        FlutterMap(
          mapController: _mapController,
          options: MapOptions(
            initialCenter: _userLocation ?? const ll.LatLng(-4.20021, 105.34728),
            initialZoom: 18,
            maxZoom: 19.5,
            minZoom: 3,
            interactionOptions: InteractionOptions(
              flags: _isNorthLocked ? (InteractiveFlag.all & ~InteractiveFlag.rotate) : InteractiveFlag.all,
            ),
            onPositionChanged: (position, hasGesture) {
              if (position.zoom != null) setState(() => _currentZoom = position.zoom!);
            },
          ),
          children: [
            TileLayer(
              urlTemplate: _isSatellite ? 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}' : 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
              userAgentPackageName: 'com.example.agriops',
              maxZoom: 20,
              maxNativeZoom: 18,
            ),
            MarkerLayer(markers: markers),
          ],
        ),
        Align(
          alignment: Alignment.bottomLeft,
          child: Container(
            margin: const EdgeInsets.only(bottom: 24, left: 16),
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
            decoration: BoxDecoration(
              color: Colors.black54,
              borderRadius: BorderRadius.circular(4),
            ),
            child: Text('Scale: $scaleText', style: const TextStyle(color: Colors.white, fontSize: 10, fontWeight: FontWeight.bold)),
          ),
        ),
        Positioned(
          top: 16,
          right: 16,
          child: FloatingActionButton(
            mini: true,
            backgroundColor: Colors.amber,
            onPressed: () => setState(() => _isSatellite = !_isSatellite),
            child: Icon(_isSatellite ? Icons.satellite : Icons.map),
          ),
        ),
        Positioned(
          top: 76,
          right: 16,
          child: FloatingActionButton(
            mini: true,
            backgroundColor: _isNorthLocked ? Colors.white : Colors.blueAccent,
            foregroundColor: _isNorthLocked ? Colors.black87 : Colors.white,
            onPressed: () {
              setState(() {
                _isNorthLocked = !_isNorthLocked;
                if (_isNorthLocked) _mapController.rotate(0.0);
              });
            },
            child: Transform.rotate(
              angle: _isNorthLocked ? 0.0 : (_heading * math.pi / 180),
              child: const Icon(Icons.navigation),
            ),
          ),
        ),
        Positioned(
          bottom: 80,
          right: 16,
          child: FloatingActionButton(
            mini: true,
            backgroundColor: Colors.white,
            foregroundColor: Colors.black87,
            onPressed: () {
              if (_userLocation != null) {
                if (_isNorthLocked) {
                  _mapController.move(_userLocation!, 18.0);
                } else {
                  _mapController.moveAndRotate(_userLocation!, 18.0, 0.0);
                }
              }
            },
            child: const Icon(Icons.my_location),
          ),
        ),
      ],
    );
  }

  void _showPhotoDetailModal(BuildContext context, Map<String, dynamic> photo) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        backgroundColor: const Color(0xFF1E1E1E),
        title: Text(photo['title'], style: const TextStyle(color: Colors.white, fontSize: 16)),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            ClipRRect(
              borderRadius: BorderRadius.circular(8),
              child: AspectRatio(
                aspectRatio: photo['orientation'] == 'Landscape' ? 4 / 3 : 3 / 4,
                child: Image.file(File(photo['path']), fit: BoxFit.cover),
              ),
            ),
            const SizedBox(height: 8),
            Text('Waktu: ${photo['time']}', style: const TextStyle(color: Colors.white54, fontSize: 12)),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Tutup', style: TextStyle(color: Colors.amber)),
          ),
        ],
      ),
    );
  }

  Widget _buildFavoritesTab() => const Center(child: Text('Kosong', style: TextStyle(color: Colors.white60)));
}

class DirectionBeamPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = const Color(0xFF4285F4).withOpacity(0.4)
      ..style = PaintingStyle.fill;

    final path = Path();
    final center = Offset(size.width / 2, size.height / 2);
    path.moveTo(center.dx, center.dy);
    path.lineTo(center.dx - 14, center.dy - 24);
    path.arcToPoint(
      Offset(center.dx + 14, center.dy - 24),
      radius: const Radius.circular(20),
      clockwise: true,
    );
    path.close();

    canvas.drawPath(path, paint);
  }
  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => true;
}

class _FullscreenGalleryView extends StatefulWidget {
  final List<Map<String, dynamic>> photos;
  final int initialIndex;
  const _FullscreenGalleryView({required this.photos, required this.initialIndex});

  @override
  State<_FullscreenGalleryView> createState() => _FullscreenGalleryViewState();
}

class _FullscreenGalleryViewState extends State<_FullscreenGalleryView> {
  late PageController _pageController;

  @override
  void initState() {
    super.initState();
    _pageController = PageController(initialPage: widget.initialIndex);
  }

  @override
  void dispose() {
    _pageController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      body: PageView.builder(
        controller: _pageController,
        itemCount: widget.photos.length,
        itemBuilder: (context, index) {
          final photo = widget.photos[index];
          return GestureDetector(
            onTap: () => Navigator.pop(context),
            child: Center(
              child: Image.file(File(photo['path'])),
            ),
          );
        },
      ),
    );
  }
}
