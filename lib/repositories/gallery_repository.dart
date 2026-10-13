import 'dart:io';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../path_helper.dart';

final galleryPhotosProvider = StateNotifierProvider<GalleryNotifier, List<Map<String, dynamic>>>((ref) {
  return GalleryNotifier();
});

class GalleryNotifier extends StateNotifier<List<Map<String, dynamic>>> {
  GalleryNotifier() : super([]) {
    loadPhotos();
  }

  Future<void> loadPhotos() async {
    try {
      final appDir = await AppPathHelper.getAppDirectory();
      final dir = Directory(appDir);
      if (!await dir.exists()) return;
      
      final files = dir.listSync()
          .where((f) => f is File && (f.path.toLowerCase().endsWith('.jpg') || f.path.toLowerCase().endsWith('.jpeg')))
          .cast<File>();
      
      final loaded = <Map<String, dynamic>>[];
      for (var file in files) {
        final stat = await file.stat();
        final name = file.uri.pathSegments.last.split('.').first;
        loaded.add({
          'path': file.path,
          'title': name,
          'time': stat.modified.toString().split('.').first,
          'lat': -4.20021,
          'lng': 105.34728,
          'orientation': name.contains('img_') ? 'Landscape' : 'Portrait',
        });
      }
      loaded.sort((a, b) => (b['time'] as String).compareTo(a['time'] as String));
      state = loaded;
    } catch (_) {}
  }

  void addPhoto(Map<String, dynamic> photo) {
    state = [photo, ...state];
  }
}
