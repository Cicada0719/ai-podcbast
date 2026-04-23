import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../services/api_service.dart';
import '../services/player_service.dart';
import 'home_screen.dart';
import 'vocab_screen.dart';
import 'alarm_screen.dart';

class MainScreen extends StatefulWidget {
  const MainScreen({super.key});

  @override
  State<MainScreen> createState() => _MainScreenState();
}

class _MainScreenState extends State<MainScreen> {
  int _currentIndex = 0;
  final List<Widget> _screens = const [
    HomeScreen(),
    VocabularyScreen(),
    AlarmScreen(),
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: IndexedStack(
        index: _currentIndex,
        children: _screens,
      ),
      bottomNavigationBar: Container(
        decoration: BoxDecoration(
          boxShadow: [
            BoxShadow(
              color: Colors.black.withOpacity(0.5),
              blurRadius: 10,
              offset: const Offset(0, -2),
            ),
          ],
        ),
        child: BottomNavigationBar(
          currentIndex: _currentIndex,
          backgroundColor: const Color(0xFF1A1A1A),
          selectedItemColor: Colors.amber,
          unselectedItemColor: Colors.white54,
          showSelectedLabels: true,
          showUnselectedLabels: false,
          elevation: 0,
          type: BottomNavigationBarType.fixed,
          onTap: (index) {
            setState(() {
              _currentIndex = index;
            });
          },
          items: const [
            BottomNavigationBarItem(
              icon: Icon(Icons.radio),
              activeIcon: Icon(Icons.radio, size: 30),
              label: 'Radio',
            ),
            BottomNavigationBarItem(
              icon: Icon(Icons.menu_book),
              activeIcon: Icon(Icons.menu_book, size: 30),
              label: 'Vocab',
            ),
            BottomNavigationBarItem(
              icon: Icon(Icons.alarm),
              activeIcon: Icon(Icons.alarm, size: 30),
              label: 'Routine',
            ),
          ],
        ),
      ),
    );
  }
}
