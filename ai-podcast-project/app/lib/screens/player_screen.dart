import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../services/player_service.dart';
import '../models/episode.dart';

class PlayerScreen extends StatelessWidget {
  const PlayerScreen({super.key});

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

  Widget _buildInfoCard(Episode ep) {
    bool isDj = ep.type == 'dj_talk';
    return Container(
      decoration: BoxDecoration(
        color: isDj ? Colors.amber.withOpacity(0.1) : Colors.blue.withOpacity(0.1),
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: isDj ? Colors.amber : Colors.blue, width: 2),
      ),
      child: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              isDj ? Icons.mic_external_on : Icons.music_note,
              size: 80,
              color: isDj ? Colors.amber : Colors.blue,
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
