import 'dart:async';
import 'package:flutter/foundation.dart';
import 'package:just_audio/just_audio.dart';
import '../models/episode.dart';

class PlayerService extends ChangeNotifier {
  final AudioPlayer _player = AudioPlayer();
  List<Episode> _episodes = [];
  int _currentIndex = 0;
  bool _isPlaying = false;
  bool _isLoading = false;
  Timer? _sleepTimer;
  int? _sleepMinutesLeft;

  List<Episode> get episodes => _episodes;
  Episode? get currentEpisode => _episodes.isEmpty ? null : _episodes[_currentIndex];
  bool get isPlaying => _isPlaying;
  bool get isLoading => _isLoading;
  int? get sleepMinutesLeft => _sleepMinutesLeft;

  // 使用本机 IP 供 Android 模拟器访问，若为 Windows/Web 请改回 127.0.0.1
  final String baseUrl = "http://127.0.0.1:8000";

  PlayerService() {
    _player.currentIndexStream.listen((index) {
      if (index != null) {
        _currentIndex = index;
        notifyListeners();
      }
    });

    _player.playingStream.listen((playing) {
      _isPlaying = playing;
      notifyListeners();
    });
  }

  Future<void> loadEpisodes(List<Episode> newEpisodes) async {
    _isLoading = true;
    notifyListeners();

    try {
      _episodes = newEpisodes;
      _currentIndex = 0;

      final playlist = ConcatenatingAudioSource(
        children: newEpisodes.map((ep) {
          if (ep.type == 'dj_talk' && ep.audioUrl != null) {
            // TTS 音频
            return AudioSource.uri(Uri.parse('$baseUrl${ep.audioUrl}'));
          } else if (ep.type == 'song' && ep.songId != null) {
            // 网易云音乐直链 (注: 实际生产中应动态调用 API 获取真实的 mp3 URL)
            return AudioSource.uri(
              Uri.parse('https://music.163.com/song/media/outer/url?id=${ep.songId}.mp3'),
            );
          }
          return AudioSource.uri(Uri.parse(''));
        }).where((source) => source.uri.toString().isNotEmpty).toList(),
      );

      await _player.setAudioSource(playlist);
    } catch (e) {
      debugPrint("Error loading playlist: $e");
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  void play() => _player.play();
  void pause() => _player.pause();
  void next() => _player.seekToNext();
  void previous() => _player.seekToPrevious();

  void setSleepTimer(int minutes) {
    _sleepTimer?.cancel();
    if (minutes <= 0) {
      _sleepMinutesLeft = null;
      notifyListeners();
      return;
    }
    _sleepMinutesLeft = minutes;
    notifyListeners();
    
    _sleepTimer = Timer.periodic(const Duration(minutes: 1), (timer) {
      if (_sleepMinutesLeft != null && _sleepMinutesLeft! > 1) {
        _sleepMinutesLeft = _sleepMinutesLeft! - 1;
        notifyListeners();
      } else {
        pause();
        _sleepMinutesLeft = null;
        timer.cancel();
        notifyListeners();
      }
    });
  }

  @override
  void dispose() {
    _sleepTimer?.cancel();
    _player.dispose();
    super.dispose();
  }
}
