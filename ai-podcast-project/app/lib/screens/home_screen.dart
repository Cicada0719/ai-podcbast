import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../services/api_service.dart';
import '../services/player_service.dart';
import 'player_screen.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final apiService = context.read<ApiService>();
    final playerService = context.read<PlayerService>();

    return Scaffold(
      backgroundColor: const Color(0xFF121212),
      appBar: AppBar(
        title: const Text('Good Morning', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
        backgroundColor: Colors.transparent,
        elevation: 0,
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
              padding: const EdgeInsets.all(32),
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                gradient: const LinearGradient(
                  colors: [Colors.amberAccent, Colors.orange],
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                ),
                boxShadow: [
                  BoxShadow(
                    color: Colors.amber.withOpacity(0.3),
                    blurRadius: 40,
                    spreadRadius: 20,
                  )
                ],
              ),
              child: const Icon(Icons.radio, size: 100, color: Colors.black87),
            ),
            const SizedBox(height: 48),
            ElevatedButton.icon(
              icon: const Icon(Icons.auto_awesome),
              label: const Text('Generate Daily Commute Radio'),
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.amber,
                foregroundColor: Colors.black87,
                padding: const EdgeInsets.symmetric(horizontal: 32, vertical: 20),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(30)),
                textStyle: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                elevation: 8,
              ),
              onPressed: () async {
                try {
                  // 显示精美加载弹窗
                  showDialog(
                    context: context,
                    barrierDismissible: false,
                    builder: (ctx) => const Center(
                      child: Column(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          CircularProgressIndicator(color: Colors.amber),
                          SizedBox(height: 20),
                          Text("Echo & Leo are preparing your radio...", style: TextStyle(color: Colors.amber, fontSize: 16, fontWeight: FontWeight.bold)),
                        ],
                      ),
                    ),
                  );
                  
                  // 1. 调用后端 API 生成播客台本和 TTS
                  final episodes = await apiService.fetchPodcastScript("早间通勤");
                  
                  // 2. 加载进播放器
                  await playerService.loadEpisodes(episodes);
                  
                  // 关闭弹窗
                  if (context.mounted) Navigator.pop(context);
                  
                  // 3. 跳转到播放器界面
                  if (context.mounted) {
                    Navigator.push(
                      context,
                      MaterialPageRoute(builder: (_) => const PlayerScreen()),
                    );
                    playerService.play(); // 自动播放
                  }
                } catch (e) {
                  if (context.mounted) Navigator.pop(context);
                  debugPrint('Failed to generate radio: $e');
                  if (context.mounted) {
                    ScaffoldMessenger.of(context).showSnackBar(
                      SnackBar(content: Text('Error: $e'), backgroundColor: Colors.redAccent),
                    );
                  }
                }
              },
            ),
          ],
        ),
      ),
    );
  }
}
