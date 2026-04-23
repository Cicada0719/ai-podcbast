import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/episode.dart';

class StorageService {
  static const String _vocabKey = 'saved_vocabulary';

  Future<void> saveWord(LearningWord word) async {
    final prefs = await SharedPreferences.getInstance();
    List<String> vocabList = prefs.getStringList(_vocabKey) ?? [];

    // 避免重复保存
    bool exists = vocabList.any((jsonStr) {
      var map = jsonDecode(jsonStr);
      return map['word'] == word.word;
    });

    if (!exists) {
      vocabList.add(jsonEncode({
        'word': word.word,
        'meaning': word.meaning,
        'example': word.example,
      }));
      await prefs.setStringList(_vocabKey, vocabList);
    }
  }

  Future<List<LearningWord>> getSavedWords() async {
    final prefs = await SharedPreferences.getInstance();
    List<String> vocabList = prefs.getStringList(_vocabKey) ?? [];
    
    return vocabList.map((jsonStr) {
      return LearningWord.fromJson(jsonDecode(jsonStr));
    }).toList();
  }
}
