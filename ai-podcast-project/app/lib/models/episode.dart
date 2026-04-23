class Episode {
  final String type; // "dj_talk" 或 "song"
  final String? speaker;
  final String? content;
  final List<LearningWord>? learningWords;
  final String? audioUrl;
  
  // Song 属性
  final int? songId;
  final String? songName;
  final String? artist;

  Episode({
    required this.type,
    this.speaker,
    this.content,
    this.learningWords,
    this.audioUrl,
    this.songId,
    this.songName,
    this.artist,
  });

  factory Episode.fromJson(Map<String, dynamic> json) {
    List<LearningWord>? words;
    if (json['learning_words'] != null) {
      words = (json['learning_words'] as List)
          .map((w) => LearningWord.fromJson(w))
          .toList();
    }

    return Episode(
      type: json['type'],
      speaker: json['speaker'],
      content: json['content'],
      learningWords: words,
      audioUrl: json['audio_url'],
      songId: json['song_id'],
      songName: json['song_name'],
      artist: json['artist'],
    );
  }
}

class LearningWord {
  final String word;
  final String meaning;
  final String example;

  LearningWord({
    required this.word,
    required this.meaning,
    required this.example,
  });

  factory LearningWord.fromJson(Map<String, dynamic> json) {
    return LearningWord(
      word: json['word'] ?? '',
      meaning: json['meaning'] ?? '',
      example: json['example'] ?? '',
    );
  }
}
