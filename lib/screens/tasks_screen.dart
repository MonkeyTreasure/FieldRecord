import 'dart:io';
import 'package:flutter/material.dart';
import 'package:path_provider/path_provider.dart';
import 'package:path/path.dart' as p;
import '../shared/ui_components.dart';
import '../services/api_service.dart';
import 'camera_screen.dart';
import '../path_helper.dart';

class TasksScreen extends StatefulWidget {
  const TasksScreen({super.key});
  
  @override
  State<TasksScreen> createState() => TasksScreenState();
}

class TasksScreenState extends State<TasksScreen> with SingleTickerProviderStateMixin {
  late TabController _tabController;
  bool isLoading = true;
  List tasks = [];
  List<Map<String, dynamic>> notes = [];

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 4, vsync: this);
    loadTasks();
    _loadNotes();
  }

  Future<String> _getNotesDirectory() async {
    final baseDir = await AppPathHelper.getAppDirectory();
    final notesDir = Directory(p.join(baseDir, 'Notes'));
    if (!await notesDir.exists()) {
      await notesDir.create(recursive: true);
    }
    return notesDir.path;
  }

  Future<void> _loadNotes() async {
    final notesDirPath = await _getNotesDirectory();
    final notesDir = Directory(notesDirPath);
    if (!await notesDir.exists()) {
      setState(() => notes = []);
      return;
    }
    final files = notesDir.listSync().where((f) => f is File && f.path.endsWith('.md')).cast<File>();
    final loaded = <Map<String, dynamic>>[];
    for (var file in files) {
      final content = await file.readAsString();
      final lines = content.split('\n');
      String title = lines.firstWhere((l) => l.startsWith('# '), orElse: () => p.basenameWithoutExtension(file.path)).substring(2);
      String photoPath = '';
      for (var l in lines) {
        if (l.startsWith('photo:')) {
          photoPath = l.substring(6).trim();
        }
      }
      loaded.add({
        'title': title,
        'content': content,
        'photoPath': photoPath.isNotEmpty ? photoPath : null,
        'time': FileStat.statSync(file.path).modified.toString().split('.').first,
        'filePath': file.path,
      });
    }
    loaded.sort((a, b) => (b['time'] as String).compareTo(a['time'] as String));
    setState(() => notes = loaded);
  }

  Future<void> loadTasks() async {
    setState(() => isLoading = true);
    final data = await ApiService.getTodayTasks(1);
    final dataMap = data ?? {};
    setState(() {
      tasks = [
        ...(dataMap['pending_tasks'] ?? []),
        ...(dataMap['in_progress_tasks'] ?? []),
        ...(dataMap['completed_tasks'] ?? []),
      ];
      isLoading = false;
    });
  }

  void _openAddNoteDialog() async {
    String noteTitle = '';
    String noteContent = '';
    String? selectedPhotoPath;
    await showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: const Color(0xFF1E1E1E),
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(16)),
      ),
      builder: (context) => Padding(
        padding: EdgeInsets.only(
          bottom: MediaQuery.of(context).viewInsets.bottom,
          left: 16,
          right: 16,
          top: 16,
        ),
        child: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  IconButton(
                    icon: const Icon(Icons.close, color: Colors.white),
                    onPressed: () => Navigator.pop(context),
                  ),
                  Row(
                    children: [
                      IconButton(
                        icon: const Icon(Icons.push_pin_outlined, color: Colors.white),
                        onPressed: () {},
                      ),
                      IconButton(
                        icon: const Icon(Icons.add_alert_outlined, color: Colors.white),
                        onPressed: () {},
                      ),
                      IconButton(
                        icon: const Icon(Icons.archive_outlined, color: Colors.white),
                        onPressed: () {},
                      ),
                    ],
                  ),
                ],
              ),
              TextField(
                decoration: const InputDecoration(
                  hintText: 'Title',
                  hintStyle: TextStyle(color: Colors.white54, fontSize: 20, fontWeight: FontWeight.bold),
                  border: InputBorder.none,
                ),
                style: const TextStyle(color: Colors.white, fontSize: 20, fontWeight: FontWeight.bold),
                onChanged: (v) => noteTitle = v,
              ),
              TextField(
                maxLines: 6,
                decoration: const InputDecoration(
                  hintText: 'Note',
                  hintStyle: TextStyle(color: Colors.white54, fontSize: 16),
                  border: InputBorder.none,
                ),
                style: const TextStyle(color: Colors.white, fontSize: 16),
                onChanged: (v) => noteContent = v,
              ),
              const SizedBox(height: 12),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Row(
                    children: [
                      IconButton(
                        icon: const Icon(Icons.add_box_outlined, color: Colors.white70),
                        onPressed: () {},
                      ),
                      IconButton(
                        icon: const Icon(Icons.color_lens_outlined, color: Colors.white70),
                        onPressed: () {},
                      ),
                    ],
                  ),
                  ElevatedButton(
                    style: ElevatedButton.styleFrom(backgroundColor: Colors.greenAccent),
                    onPressed: () async {
                      if (noteTitle.isNotEmpty || noteContent.isNotEmpty) {
                        final notesDir = await _getNotesDirectory();
                        final safeTitle = noteTitle.isEmpty ? 'Untitled' : noteTitle.replaceAll(RegExp(r'[\\/:*?"<>|]'), '_');
                        final fileName = '${safeTitle}_${DateTime.now().millisecondsSinceEpoch}.md';
                        final filePath = p.join(notesDir, fileName);
                        
                        String mdContent = '# ${noteTitle.isEmpty ? "Untitled" : noteTitle}\n\n';
                        mdContent += 'created: ${DateTime.now().toIso8601String()}\n';
                        mdContent += 'tags: [agriops, field-note]\n\n';
                        
                        if (noteContent.isNotEmpty) {
                          mdContent += '$noteContent\n\n';
                        }
                        
                        mdContent += '---\n\n*Created: ${DateTime.now().toString().split('.').first}*';
                        
                        await File(filePath).writeAsString(mdContent);
                        await _loadNotes();
                      }
                      Navigator.pop(context);
                    },
                    child: const Text('Save', style: TextStyle(color: Colors.black, fontWeight: FontWeight.bold)),
                  ),
                ],
              ),
              const SizedBox(height: 16),
            ],
          ),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: PreferredSize(
        preferredSize: const Size.fromHeight(140),
        child: AppBar(
          automaticallyImplyLeading: false,
          backgroundColor: const Color(0xFF1B381B),
          elevation: 0,
          flexibleSpace: SafeArea(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Padding(
                  padding: EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                  child: Text('Manajemen Tugas Lapangan', style: TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold)),
                ),
                TabBar(
                  controller: _tabController,
                  indicatorColor: Colors.greenAccent,
                  labelColor: Colors.greenAccent,
                  unselectedLabelColor: Colors.white60,
                  tabs: const [
                    Tab(text: 'Today'),
                    Tab(text: 'Upcoming'),
                    Tab(text: 'Completed'),
                    Tab(text: 'Notes'),
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
      body: isLoading
          ? const Center(child: CircularProgressIndicator(color: Colors.greenAccent))
          : TabBarView(
              controller: _tabController,
              children: [
                _buildTaskList(tasks),
                _buildTaskList(tasks.where((t) => t['status'] == 'pending').toList()),
                _buildTaskList(tasks.where((t) => t['status'] == 'completed').toList()),
                _buildNotesTab(),
              ],
            ),
      floatingActionButton: null,
    );
  }

  Widget _buildTaskList(List list) {
    if (list.isEmpty) {
      return const Center(child: Text('Tidak ada tugas.', style: TextStyle(color: Colors.white54)));
    }
    return ListView.builder(
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 84),
      itemCount: list.length,
      itemBuilder: (context, index) {
        final task = list[index];
        final status = task['status'] ?? 'pending';
        return Padding(
          padding: const EdgeInsets.only(bottom: 12),
          child: AgriOpsCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    const Text('SBYE → Divisi 5 → Blok K30', style: TextStyle(color: Colors.greenAccent, fontSize: 11, fontWeight: FontWeight.bold)),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                      decoration: BoxDecoration(
                        color: status == 'completed' ? Colors.green.withOpacity(0.2) : (status == 'in_progress' ? Colors.orange.withOpacity(0.2) : Colors.blue.withOpacity(0.2)),
                        borderRadius: BorderRadius.circular(4),
                      ),
                      child: Text(status.toUpperCase(), style: TextStyle(color: status == 'completed' ? Colors.greenAccent : (status == 'in_progress' ? Colors.orangeAccent : Colors.blueAccent), fontSize: 10)),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                Text(task['title'], style: const TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold)),
                const SizedBox(height: 4),
                Text(task['description'] ?? 'Pemeriksaan rutin blok.', style: const TextStyle(color: Colors.white60, fontSize: 13)),
              ],
            ),
          ),
        );
      },
    );
  }

  Widget _buildNotesTab() {
    if (notes.isEmpty) {
      return const Center(child: Text('Tidak ada catatan. Tekan + untuk menambah.', style: TextStyle(color: Colors.white54)));
    }
    return ListView.builder(
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 84),
      itemCount: notes.length,
      itemBuilder: (context, index) {
        final note = notes[index];
        return Padding(
          padding: const EdgeInsets.only(bottom: 12),
          child: Container(
            decoration: BoxDecoration(
              color: const Color(0xFF2E7D32).withOpacity(0.2),
              borderRadius: BorderRadius.circular(8),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Padding(
                  padding: const EdgeInsets.all(12.0),
                  child: Text(note['title'] as String, style: const TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold)),
                ),
                if (note['photoPath'] != null)
                  ClipRRect(
                    borderRadius: const BorderRadius.vertical(bottom: Radius.circular(8)),
                    child: Image.file(File(note['photoPath'] as String), fit: BoxFit.cover, width: double.infinity, height: 180),
                  ),
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                  child: Text(note['time'] as String, style: const TextStyle(color: Colors.white60, fontSize: 12)),
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}