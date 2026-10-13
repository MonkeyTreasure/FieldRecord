import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:intl/intl.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'shared/theme.dart';
import 'shared/ui_components.dart';
import 'screens/home_screen.dart';
import 'screens/tasks_screen.dart';
import 'screens/gallery_screen.dart';
import 'screens/reports_screen.dart';
import 'screens/profile_screen.dart';
import 'screens/camera_screen.dart';
import 'screens/note_editor_screen.dart';
import 'services/permission_service.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await initializeDateFormatting('id_ID', null);
  
  // Request all required permissions
  await PermissionService.requestAllPermissions();
  
  runApp(const AgriOpsApp());
}

class AgriOpsApp extends StatelessWidget {
  const AgriOpsApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'AgriOps',
      debugShowCheckedModeBanner: false,
      theme: AppThemes.lightTheme,
      darkTheme: AppThemes.darkTheme,
      themeMode: ThemeMode.dark, // Energetic dark natural theme by default
      home: const MainShell(),
    );
  }
}

class MainShell extends StatefulWidget {
  const MainShell({super.key});

  @override
  State<MainShell> createState() => _MainShellState();
}

class _MainShellState extends State<MainShell> {
  int _currentIndex = 0;
  static const platform = MethodChannel('com.example.agriops/widget');

  final List<Widget> _screens = [
    const HomeScreen(),
    const TasksScreen(),
    const GalleryScreen(),
    const ReportsScreen(),
    const ProfileScreen(),
  ];

  final GlobalKey<TasksScreenState> _tasksKey = GlobalKey<TasksScreenState>();

  void _openCameraScreen() {
    Navigator.push(
      context,
      MaterialPageRoute(builder: (_) => const FieldCameraScreen()),
    );
  }

  void _onAddPressed() {
    if (_currentIndex == 1) {
      // Tasks / Notes
      Navigator.push(
        context,
        MaterialPageRoute(builder: (_) => const NoteEditorScreen()),
      );
    } else {
      _openCameraScreen();
    }
  }

  @override
    Widget build(BuildContext context) {
      return Scaffold(
        body: Stack(
          children: [
            IndexedStack(
              index: _currentIndex,
              children: [
                const HomeScreen(),
                TasksScreen(key: _tasksKey),
                const GalleryScreen(),
                const ReportsScreen(),
                const ProfileScreen(),
              ],
            ),
            Positioned(
              bottom: 0,
              left: 0,
              right: 0,
              child: AgriOpsBottomNav(
                currentIndex: _currentIndex,
                onTap: (index) => setState(() => _currentIndex = index),
              ),
            ),
            // FAB positioned above navbar (navbar is 68px tall)
            Positioned(
              bottom: 80, // 68px navbar + 12px gap
              left: 0,
              right: 0,
              child: Center(
                child: FloatingActionButton(
                  backgroundColor: Colors.green, // Changed from greenAccent to green
                  elevation: 6,
                  onPressed: _onAddPressed,
                  child: const Icon(Icons.add, color: Colors.white, size: 28),
                ),
              ),
            ),
          ],
        ),
      );
    }
}
