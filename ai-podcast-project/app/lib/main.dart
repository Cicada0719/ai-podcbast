import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'screens/player_screen.dart';
import 'services/api_service.dart';
import 'services/player_service.dart';

void main() {
  runApp(
    MultiProvider(
      providers: [
        ChangeNotifierProvider(create: (_) => PlayerService()),
        Provider(create: (_) => ApiService()),
      ],
      child: const AIPodcastApp(),
    ),
  );
}

class AIPodcastApp extends StatelessWidget {
  const AIPodcastApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'AI DJ Radio',
      theme: ThemeData(
        brightness: Brightness.dark,
        primarySwatch: Colors.amber,
      ),
      home: const HomeScreen(),
    );
  }
}

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final apiService = context.read<ApiService>();
    final playerService = context.read<PlayerService>();

    return Scaffold(
      backgroundColor: Colors.black87,
      appBar: AppBar(
        title: const Text('Good Morning', style: TextStyle(color: Colors.white)),
        backgroundColor: Colors.transparent,
        elevation: 0,
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.radio, size: 100, color: Colors.amber),
            const SizedBox(height: 24),
            ElevatedButton.icon(
              icon: const Icon(Icons.auto_awesome),
              label: const Text('Generate Daily Commute Radio'),
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.amber,
                foregroundColor: Colors.black87,
                padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
                textStyle: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
              ),
              onPressed: () async {
                try {
                  // 显示加载弹窗
                  showDialog(
                    context: context,
                    barrierDismissible: false,
                    builder: (ctx) => const Center(child: CircularProgressIndicator(color: Colors.amber)),
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
                      SnackBar(content: Text('Error: $e')),
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
