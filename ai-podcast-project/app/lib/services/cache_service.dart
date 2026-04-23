import 'dart:io';
import 'package:dio/dio.dart';
import 'package:path_provider/path_provider.dart';

class CacheService {
  final Dio _dio = Dio();

  Future<String?> downloadAndCacheAudio(String url, String filename) async {
    try {
      final directory = await getApplicationDocumentsDirectory();
      final filePath = '${directory.path}/$filename';
      
      final file = File(filePath);
      if (await file.exists()) {
        // 如果文件已缓存，直接返回本地路径
        return filePath;
      }

      await _dio.download(url, filePath);
      return filePath;
    } catch (e) {
      print("Download error: $e");
      return null;
    }
  }

  Future<void> clearCache() async {
    try {
      final directory = await getApplicationDocumentsDirectory();
      final files = directory.listSync();
      for (var file in files) {
        if (file is File && file.path.endsWith('.mp3')) {
          await file.delete();
        }
      }
    } catch (e) {
      print("Clear cache error: $e");
    }
  }
}
