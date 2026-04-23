import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:just_audio_background/just_audio_background.dart';
import 'screens/main_screen.dart';
import 'services/api_service.dart';
import 'services/player_service.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  
  // 需求 11: 车载模式 (CarPlay / Android Auto) 与后台锁屏控制
  await JustAudioBackground.init(
    androidNotificationChannelId: 'com.ryanheise.bg_demo.channel.audio',
    androidNotificationChannelName: 'AI DJ Radio Playback',
    androidNotificationOngoing: true,
  );

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
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        brightness: Brightness.dark,
        primarySwatch: Colors.amber,
        scaffoldBackgroundColor: const Color(0xFF121212),
        fontFamily: 'Roboto', // Replace with a custom font if desired
      ),
      home: const MainScreen(),
    );
  }
}

