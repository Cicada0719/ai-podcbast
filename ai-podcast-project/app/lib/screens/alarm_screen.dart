import 'package:flutter/material.dart';

class AlarmScreen extends StatefulWidget {
  const AlarmScreen({super.key});

  @override
  State<AlarmScreen> createState() => _AlarmScreenState();
}

class _AlarmScreenState extends State<AlarmScreen> {
  TimeOfDay _selectedTime = const TimeOfDay(hour: 7, minute: 30);
  bool _isAlarmActive = false;
  bool _isOfflineCacheEnabled = true;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF121212),
      appBar: AppBar(
        title: const Text('Morning Routine', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
        backgroundColor: Colors.transparent,
        elevation: 0,
      ),
      body: ListView(
        padding: const EdgeInsets.all(24.0),
        children: [
          _buildTimePickerCard(),
          const SizedBox(height: 32),
          _buildFeatureToggle(
            title: 'AI Morning DJ Alarm',
            subtitle: 'Wake up with Echo & Leo',
            icon: Icons.alarm,
            value: _isAlarmActive,
            onChanged: (val) {
              setState(() {
                _isAlarmActive = val;
              });
            },
          ),
          const Divider(color: Colors.white12, height: 40),
          _buildFeatureToggle(
            title: 'Smart Offline Caching',
            subtitle: 'Download episodes at night via Wi-Fi',
            icon: Icons.offline_bolt,
            value: _isOfflineCacheEnabled,
            onChanged: (val) {
              setState(() {
                _isOfflineCacheEnabled = val;
              });
            },
          ),
          const SizedBox(height: 48),
          if (_isAlarmActive)
            const Text(
              'Your personalized dual-host radio will be automatically generated and cached at 4:00 AM, ready to wake you up gently.',
              style: TextStyle(color: Colors.amberAccent, fontSize: 14, fontStyle: FontStyle.italic),
              textAlign: TextAlign.center,
            ),
        ],
      ),
    );
  }

  Widget _buildTimePickerCard() {
    return Container(
      decoration: BoxDecoration(
        gradient: const LinearBinding(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [Color(0xFF2C2C2C), Color(0xFF1A1A1A)],
        ),
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: Colors.white10),
      ),
      padding: const EdgeInsets.symmetric(vertical: 40),
      child: Column(
        children: [
          const Text(
            'WAKE UP AT',
            style: TextStyle(color: Colors.white54, fontSize: 14, letterSpacing: 2),
          ),
          const SizedBox(height: 16),
          GestureDetector(
            onTap: () async {
              final TimeOfDay? picked = await showTimePicker(
                context: context,
                initialTime: _selectedTime,
                builder: (context, child) {
                  return Theme(
                    data: ThemeData.dark().copyWith(
                      colorScheme: const ColorScheme.dark(
                        primary: Colors.amber,
                        onPrimary: Colors.black,
                        surface: Color(0xFF2C2C2C),
                        onSurface: Colors.white,
                      ),
                    ),
                    child: child!,
                  );
                },
              );
              if (picked != null && picked != _selectedTime) {
                setState(() {
                  _selectedTime = picked;
                  _isAlarmActive = true;
                });
              }
            },
            child: Text(
              _selectedTime.format(context),
              style: const TextStyle(
                color: Colors.amber,
                fontSize: 64,
                fontWeight: FontWeight.w200,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildFeatureToggle({
    required String title,
    required String subtitle,
    required IconData icon,
    required bool value,
    required ValueChanged<bool> onChanged,
  }) {
    return Row(
      children: [
        Container(
          padding: const EdgeInsets.all(12),
          decoration: BoxDecoration(
            color: Colors.amber.withOpacity(0.1),
            borderRadius: BorderRadius.circular(12),
          ),
          child: Icon(icon, color: Colors.amber, size: 28),
        ),
        const SizedBox(width: 20),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                title,
                style: const TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 4),
              Text(
                subtitle,
                style: const TextStyle(color: Colors.white54, fontSize: 14),
              ),
            ],
          ),
        ),
        Switch(
          value: value,
          onChanged: onChanged,
          activeColor: Colors.amber,
        ),
      ],
    );
  }
}
