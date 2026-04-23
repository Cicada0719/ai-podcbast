import 'dart:async';
import 'package:flutter/foundation.dart';
import 'package:just_audio/just_audio.dart';
import 'package:just_audio_background/just_audio_background.dart';
import '../models/episode.dart';

class PlayerService extends ChangeNotifier {
  final AudioPlayer _player = AudioPlayer();
  final AudioPlayer _bgmPlayer = AudioPlayer(); // 需求 4: 环境音/白噪音
  
  List<Episode> _episodes = [];
  int _currentIndex = 0;
  bool _isPlaying = false;
  bool _isLoading = false;
  Timer? _sleepTimer;
  int? _sleepMinutesLeft;
  String _currentAmbientSound = '无';

  List<Episode> get episodes => _episodes;
  Episode? get currentEpisode => _episodes.isEmpty ? null : _episodes[_currentIndex];
  bool get isPlaying => _isPlaying;
  bool get isLoading => _isLoading;
  int? get sleepMinutesLeft => _sleepMinutesLeft;

  // 使用本机 IP 供 Android 模拟器访问，若为 Windows/Web 请改回 127.0.0.1
  final String baseUrl = "http://127.0.0.1:8000";

  PlayerService() {
    _player.currentIndexStream.listen((index) {
      if (index != null && _episodes.isNotEmpty) {
        _currentIndex = index;
        _handleAmbientSound();
        notifyListeners();
      }
    });

    _player.playingStream.listen((playing) {
      _isPlaying = playing;
      if (!playing) {
        _bgmPlayer.pause();
      } else {
        _handleAmbientSound();
      }
      notifyListeners();
    });
  }

  // 设置环境音类型
  Future<void> setAmbientSound(String type) async {
    _currentAmbientSound = type;
    if (type == '无') {
      await _bgmPlayer.stop();
      return;
    }
    
    // 实际项目中应替换为本地 assets 路径 (如 assets/audio/rain.mp3)
    String mockBgmUrl = 'https://cdn.pixabay.com/download/audio/2021/08/09/audio_827f329910.mp3?filename=rain-and-thunder-16705.mp3'; // 模拟雨声
    if (type == '咖啡馆') {
      mockBgmUrl = 'https://cdn.pixabay.com/download/audio/2022/02/07/audio_10f0f5b40c.mp3?filename=cafe-background-noise-24536.mp3'; // 模拟咖啡馆
    } else if (type == 'Lo-Fi') {
      mockBgmUrl = 'https://cdn.pixabay.com/download/audio/2022/05/27/audio_1808fbf589.mp3?filename=lofi-study-112191.mp3'; // 模拟 LoFi
    }

    try {
      await _bgmPlayer.setAudioSource(AudioSource.uri(Uri.parse(mockBgmUrl)));
      await _bgmPlayer.setLoopMode(LoopMode.all);
      await _bgmPlayer.setVolume(0.15); // 低音量鸭子避让
      if (_isPlaying) _handleAmbientSound();
    } catch (e) {
      debugPrint('Failed to load ambient sound: $e');
    }
  }

  void _handleAmbientSound() {
    if (_currentAmbientSound == '无' || _episodes.isEmpty) return;
    
    final ep = _episodes[_currentIndex];
    // 仅在 DJ 说话时播放环境音，听歌时暂停
    if (ep.type == 'dj_talk' && _isPlaying) {
      _bgmPlayer.play();
    } else {
      _bgmPlayer.pause();
    }
  }

  Future<void> loadEpisodes(List<Episode> newEpisodes) async {
    _isLoading = true;
    notifyListeners();

    try {
      _episodes = newEpisodes;
      _currentIndex = 0;

      final playlist = ConcatenatingAudioSource(
        children: newEpisodes.map((ep) {
          final mediaItem = MediaItem(
            id: ep.songId?.toString() ?? ep.hashCode.toString(),
            album: ep.type == 'dj_talk' ? 'AI DJ Radio' : ep.artist ?? 'Unknown Artist',
            title: ep.type == 'dj_talk' ? 'DJ ${ep.speaker ?? "Echo"} Speaking' : (ep.songName ?? 'Unknown Song'),
            artUri: Uri.parse('https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?q=80&w=256&auto=format&fit=crop'), // 模拟封面
          );

          if (ep.type == 'dj_talk' && ep.audioUrl != null) {
            // TTS 音频
            return AudioSource.uri(Uri.parse('$baseUrl${ep.audioUrl}'), tag: mediaItem);
          } else if (ep.type == 'song' && ep.songId != null) {
            // 网易云音乐直链 (注: 实际生产中应动态调用 API 获取真实的 mp3 URL)
            return AudioSource.uri(
              Uri.parse('https://music.163.com/song/media/outer/url?id=${ep.songId}.mp3'),
              tag: mediaItem,
            );
          }
          return AudioSource.uri(Uri.parse(''), tag: mediaItem);
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
    _bgmPlayer.dispose();
    _player.dispose();
    super.dispose();
  }
}
