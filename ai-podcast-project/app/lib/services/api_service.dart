import 'dart:convert';
import 'package:http/http.dart' as http;
import '../models/episode.dart';

class ApiService {
  // 如果是 Android 模拟器访问本机后端，通常使用 10.0.2.2。桌面端使用 localhost
  static const String baseUrl = "http://127.0.0.1:8000"; 

  Future<List<Episode>> fetchPodcastScript(String context) async {
    try {
      final response = await http.post(
        Uri.parse('$baseUrl/generate_script'),
        headers: {"Content-Type": "application/json"},
        body: jsonEncode({
          "context": context,
          "language_ratio": "70% 中文，30% 英文",
          "limit_songs": 3
        }),
      );

      if (response.statusCode == 200) {
        // 使用 utf8.decode 解决中文乱码
        final data = jsonDecode(utf8.decode(response.bodyBytes));
        final episodesJson = data['data']['episodes'] as List;
        return episodesJson.map((e) => Episode.fromJson(e)).toList();
      } else {
        throw Exception('Failed to load script. Status: ${response.statusCode}');
      }
    } catch (e) {
      throw Exception('Network error: $e');
    }
  }
}
