import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'screens/main_screen.dart';
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

