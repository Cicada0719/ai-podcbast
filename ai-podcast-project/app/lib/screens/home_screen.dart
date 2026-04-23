import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../services/api_service.dart';
import '../services/player_service.dart';
import 'player_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  double _enRatio = 0.5;
  String _selectedTheme = "随机";
  String _selectedPersonality = "幽默风趣";
  String _targetLanguage = "English";
  final TextEditingController _msgController = TextEditingController();

  final List<String> _themes = ["随机", "科技新闻", "情感树洞", "历史冷知识", "每日通勤"];
  final List<String> _personalities = ["幽默风趣", "知性优雅", "二次元傲娇", "严肃专业"];
  final List<String> _languages = ["English", "日本語", "Français", "Español"];

  @override
  void dispose() {
    _msgController.dispose();
    super.dispose();
  }

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
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Center(
              child: Container(
                padding: const EdgeInsets.all(24),
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
                      blurRadius: 30,
                      spreadRadius: 10,
                    )
                  ],
                ),
                child: const Icon(Icons.radio, size: 60, color: Colors.black87),
              ),
            ),
            const SizedBox(height: 32),
            
            const Text("DJ 个性化设置", style: TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold)),
            const SizedBox(height: 16),
            
            // 目标语言和比例
            Row(
              children: [
                Expanded(
                  child: _buildDropdown("学习语言", _targetLanguage, _languages, (val) {
                    if (val != null) setState(() => _targetLanguage = val);
                  }),
                ),
              ],
            ),
            const SizedBox(height: 16),
            
            Text("外语比例: ${(_enRatio * 100).toInt()}%", style: const TextStyle(color: Colors.white70)),
            Slider(
              value: _enRatio,
              min: 0.0,
              max: 1.0,
              divisions: 10,
              activeColor: Colors.amber,
              onChanged: (val) {
                setState(() => _enRatio = val);
              },
            ),
            const SizedBox(height: 16),

            // 主播性格与主题
            Row(
              children: [
                Expanded(
                  child: _buildDropdown("主播性格", _selectedPersonality, _personalities, (val) {
                    if (val != null) setState(() => _selectedPersonality = val);
                  }),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: _buildDropdown("播客主题", _selectedTheme, _themes, (val) {
                    if (val != null) setState(() => _selectedTheme = val);
                  }),
                ),
              ],
            ),
            const SizedBox(height: 24),

            // 听众留言 (Call-in)
            const Text("给 DJ 留言 (点歌/互动)", style: TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold)),
            const SizedBox(height: 8),
            TextField(
              controller: _msgController,
              style: const TextStyle(color: Colors.white),
              decoration: InputDecoration(
                hintText: "例如：我今天考试通过了，想听首开心的歌！",
                hintStyle: const TextStyle(color: Colors.white30),
                filled: true,
                fillColor: Colors.white10,
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12),
                  borderSide: BorderSide.none,
                ),
              ),
              maxLines: 2,
            ),
            const SizedBox(height: 32),

            Center(
              child: ElevatedButton.icon(
                icon: const Icon(Icons.auto_awesome),
                label: const Text('生成我的私人电台'),
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
                    showDialog(
                      context: context,
                      barrierDismissible: false,
                      builder: (ctx) => const Center(
                        child: Column(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            CircularProgressIndicator(color: Colors.amber),
                            SizedBox(height: 20),
                            Text("Echo & Leo 正在准备您的节目...", style: TextStyle(color: Colors.amber, fontSize: 16, fontWeight: FontWeight.bold)),
                          ],
                        ),
                      ),
                    );

                    int cnPercent = ((1.0 - _enRatio) * 100).toInt();
                    int targetPercent = (_enRatio * 100).toInt();
                    String ratioStr = "$cnPercent% 中文，$targetPercent% $_targetLanguage";

                    final episodes = await apiService.fetchPodcastScript(
                      "早间通勤",
                      languageRatio: ratioStr,
                      theme: _selectedTheme,
                      djPersonality: _selectedPersonality,
                      targetLanguage: _targetLanguage,
                      userMessage: _msgController.text,
                    );

                    await playerService.loadEpisodes(episodes);

                    if (context.mounted) Navigator.pop(context);

                    if (context.mounted) {
                      Navigator.push(
                        context,
                        MaterialPageRoute(builder: (_) => const PlayerScreen()),
                      );
                      playerService.play();
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
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildDropdown(String label, String value, List<String> items, ValueChanged<String?> onChanged) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label, style: const TextStyle(color: Colors.white54, fontSize: 12)),
        const SizedBox(height: 4),
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 12),
          decoration: BoxDecoration(
            color: Colors.white10,
            borderRadius: BorderRadius.circular(12),
          ),
          child: DropdownButtonHideUnderline(
            child: DropdownButton<String>(
              value: value,
              isExpanded: true,
              dropdownColor: const Color(0xFF2C2C2C),
              style: const TextStyle(color: Colors.white),
              items: items.map((String item) {
                return DropdownMenuItem<String>(
                  value: item,
                  child: Text(item),
                );
              }).toList(),
              onChanged: onChanged,
            ),
          ),
        ),
      ],
    );
  }
}
