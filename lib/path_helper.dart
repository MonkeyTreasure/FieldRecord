import 'dart:io';
import 'package:path_provider/path_provider.dart';
import 'package:path/path.dart' as p;

class AppPathHelper {
  static Future<String> getAppDirectory() async {
    Directory? directory;
    if (Platform.isAndroid) {
      directory = Directory('/storage/emulated/0/AgriOps');
    } else {
      final extDir = await getExternalStorageDirectory();
      directory = Directory(p.join(extDir!.parent.parent.parent.parent.path, 'AgriOps'));
    }
    if (!await directory.exists()) {
      await directory.create(recursive: true);
    }
    return directory.path;
  }
}
