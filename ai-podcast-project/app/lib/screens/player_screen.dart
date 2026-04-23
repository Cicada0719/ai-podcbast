import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../services/player_service.dart';
import '../services/storage_service.dart';
import '../models/episode.dart';

class PlayerScreen extends StatefulWidget {
  const PlayerScreen({super.key});

  @override
  State<PlayerScreen> createState() => _PlayerScreenState();
}

class _PlayerScreenState extends State<PlayerScreen> {
  final StorageService _storageService = StorageService();

  @override
  Widget build(BuildContext context) {
    final playerService = context.watch<PlayerService>();
    final currentEp = playerService.currentEpisode;

    return Scaffold(
      backgroundColor: Colors.black87,
      appBar: AppBar(
        title: const Text('AI DJ Radio', style: TextStyle(color: Colors.white)),
        backgroundColor: Colors.transparent,
        elevation: 0,
        actions: [
          IconButton(
            icon: Icon(
              playerService.sleepMinutesLeft != null ? Icons.timer : Icons.timer_off,
              color: playerService.sleepMinutesLeft != null ? Colors.amber : Colors.white,
            ),
            onPressed: () => _showSleepTimerDialog(context, playerService),
          ),
          if (playerService.sleepMinutesLeft != null)
            Center(
              child: Padding(
                padding: const EdgeInsets.only(right: 16.0),
                child: Text(
                  '${playerService.sleepMinutesLeft}m',
                  style: const TextStyle(color: Colors.amber, fontWeight: FontWeight.bold),
                ),
              ),
            ),
        ],
      ),
      body: SafeArea(
        child: playerService.isLoading
            ? const Center(child: CircularProgressIndicator(color: Colors.amber))
            : currentEp == null
                ? const Center(
                    child: Text(
                      'No Radio Playing',
                      style: TextStyle(color: Colors.white54, fontSize: 18),
                    ),
                  )
                : Column(
                    children: [
                      // 动态封面或信息区
                      Expanded(
                        child: Padding(
                          padding: const EdgeInsets.all(24.0),
                          child: _buildInfoCard(currentEp),
                        ),
                      ),
                      
                      // DJ 播放时的中英文字幕
                      if (currentEp.type == 'dj_talk' && currentEp.content != null)
                        _buildSubtitles(currentEp.content!),
                      
                      // 学习卡片区 (如果是 DJ_Talk)
                      if (currentEp.type == 'dj_talk' &&
                          currentEp.learningWords != null &&
                          currentEp.learningWords!.isNotEmpty)
                        _buildLearningCards(currentEp.learningWords!),

                      // 控制面板
                      _buildControlPanel(playerService),
                    ],
                  ),
      ),
    );
  }

  void _showSleepTimerDialog(BuildContext context, PlayerService playerService) {
    showModalBottomSheet(
      context: context,
      backgroundColor: const Color(0xFF1E1E1E),
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (context) {
        return SafeArea(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Padding(
                padding: EdgeInsets.all(16.0),
                child: Text('Sleep Timer', style: TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold)),
              ),
              ...[15, 30, 45, 60].map((mins) => ListTile(
                title: Text('$mins Minutes', style: const TextStyle(color: Colors.white70)),
                leading: const Icon(Icons.timer, color: Colors.amber),
                onTap: () {
                  playerService.setSleepTimer(mins);
                  Navigator.pop(context);
                },
              )),
              ListTile(
                title: const Text('Turn Off', style: TextStyle(color: Colors.redAccent)),
                leading: const Icon(Icons.timer_off, color: Colors.redAccent),
                onTap: () {
                  playerService.setSleepTimer(0);
                  Navigator.pop(context);
                },
              ),
            ],
          ),
        );
      },
    );
  }

  Widget _buildInfoCard(Episode ep) {
    bool isDj = ep.type == 'dj_talk';
    // 双人播客场景下动态切换头像颜色
    Color themeColor = isDj 
        ? (ep.speaker == 'Leo' ? Colors.greenAccent : Colors.amber) 
        : Colors.blue;

    return Container(
      decoration: BoxDecoration(
        color: themeColor.withOpacity(0.1),
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: themeColor, width: 2),
      ),
      child: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              isDj ? (ep.speaker == 'Leo' ? Icons.face : Icons.mic_external_on) : Icons.music_note,
              size: 80,
              color: themeColor,
            ),
            const SizedBox(height: 24),
            Text(
              isDj ? 'AI DJ: ${ep.speaker ?? 'Echo'}' : (ep.songName ?? 'Unknown Song'),
              style: const TextStyle(
                color: Colors.white,
                fontSize: 24,
                fontWeight: FontWeight.bold,
              ),
              textAlign: TextAlign.center,
            ),
            if (!isDj && ep.artist != null) ...[
              const SizedBox(height: 8),
              Text(
                ep.artist!,
                style: const TextStyle(color: Colors.white70, fontSize: 18),
              ),
            ]
          ],
        ),
      ),
    );
  }

  Widget _buildSubtitles(String content) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 24.0, vertical: 8.0),
      child: Text(
        content,
        style: const TextStyle(
          color: Colors.white70,
          fontSize: 16,
          height: 1.5,
        ),
        textAlign: TextAlign.center,
      ),
    );
  }

  Widget _buildLearningCards(List<LearningWord> words) {
    return SizedBox(
      height: 120,
      child: ListView.builder(
        scrollDirection: Axis.horizontal,
        itemCount: words.length,
        padding: const EdgeInsets.symmetric(horizontal: 16),
        itemBuilder: (context, index) {
          final w = words[index];
          return Container(
            width: 250,
            margin: const EdgeInsets.only(right: 16, bottom: 20),
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: Colors.white.withOpacity(0.1),
              borderRadius: BorderRadius.circular(16),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(
                      w.word,
                      style: const TextStyle(
                        color: Colors.amberAccent,
                        fontSize: 20,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    Text(
                      w.meaning,
                      style: const TextStyle(color: Colors.white70, fontSize: 14),
                    ),
                    IconButton(
                      icon: const Icon(Icons.bookmark_add, color: Colors.amberAccent),
                      onPressed: () async {
                        await _storageService.saveWord(w);
                        if (context.mounted) {
                          ScaffoldMessenger.of(context).showSnackBar(
                            SnackBar(content: Text('已保存单词: ${w.word}')),
                          );
                        }
                      },
                      padding: EdgeInsets.zero,
                      constraints: const BoxConstraints(),
                    )
                  ],
                ),
                const SizedBox(height: 8),
                Text(
                  w.example,
                  style: const TextStyle(
                    color: Colors.white54,
                    fontSize: 12,
                    fontStyle: FontStyle.italic,
                  ),
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
              ],
            ),
          );
        },
      ),
    );
  }

  Widget _buildControlPanel(PlayerService player) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 40.0, top: 20),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
        children: [
          IconButton(
            icon: const Icon(Icons.skip_previous, color: Colors.white, size: 40),
            onPressed: player.previous,
          ),
          GestureDetector(
            onTap: player.isPlaying ? player.pause : player.play,
            child: Container(
              height: 80,
              width: 80,
              decoration: const BoxDecoration(
                shape: BoxShape.circle,
                color: Colors.amber,
              ),
              child: Icon(
                player.isPlaying ? Icons.pause : Icons.play_arrow,
                color: Colors.black87,
                size: 40,
              ),
            ),
          ),
          IconButton(
            icon: const Icon(Icons.skip_next, color: Colors.white, size: 40),
            onPressed: player.next,
          ),
        ],
      ),
    );
  }
}
