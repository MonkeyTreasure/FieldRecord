import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../shared/ui_components.dart';
import '../services/api_service.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  bool isLoading = true;
  Map<String, dynamic>? data;

  @override
  void initState() {
    super.initState();
    loadData();
  }

  Future<void> loadData() async {
    setState(() => isLoading = true);
    final res = await ApiService.getTodayTasks(1);
    setState(() {
      data = res;
      isLoading = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    final todayStr = DateFormat('EEEE, dd MMM yyyy', 'id_ID').format(DateTime.now()).toUpperCase();
    final total = data?['total_today'] ?? 0;
    final completed = data?['completed_count'] ?? 0;
    final progress = total > 0 ? completed / total : 0.0;
    final pending = data?['pending_tasks'] as List? ?? [];
    final inProgress = data?['in_progress_tasks'] as List? ?? [];

    return Scaffold(
      appBar: AgriOpsHeader(onSearch: () {}, onFilter: () {}),
      body: isLoading
          ? const Center(child: CircularProgressIndicator(color: Colors.greenAccent))
          : RefreshIndicator(
              onRefresh: loadData,
              child: ListView(
                padding: const EdgeInsets.fromLTRB(16, 16, 16, 84),
                children: [
                  Text(todayStr, style: const TextStyle(color: Colors.white70, fontSize: 13, fontWeight: FontWeight.w500)),
                  const SizedBox(height: 6),
                  const Text('Rencana Lapangan Hari Ini', style: TextStyle(color: Colors.white, fontSize: 22, fontWeight: FontWeight.bold)),
                  const SizedBox(height: 16),
                  
                  // Task Summary
                  AgriOpsCard(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text('Task Summary', style: TextStyle(color: Colors.greenAccent, fontWeight: FontWeight.bold, fontSize: 13)),
                        const SizedBox(height: 8),
                        LinearProgressIndicator(value: progress, color: Colors.greenAccent, backgroundColor: Colors.black26),
                        const SizedBox(height: 8),
                        Text('$completed / $total Tugas', style: const TextStyle(color: Colors.white, fontSize: 14)),
                      ],
                    ),
                  ),
                  const SizedBox(height: 24),

                  const Text('NEXT TASKS', style: TextStyle(color: Colors.greenAccent, fontWeight: FontWeight.bold, fontSize: 13, letterSpacing: 1.1)),
                  const SizedBox(height: 12),
                  if (inProgress.isNotEmpty)
                    _buildTaskCard(inProgress.first, isRunning: true)
                  else if (pending.isNotEmpty)
                    _buildTaskCard(pending.first, isRunning: false)
                  else
                    const AgriOpsCard(child: Center(child: Text('Semua tugas selesai!', style: TextStyle(color: Colors.white60)))),
                ],
              ),
            ),
    );
  }

  Widget _buildTaskCard(Map<String, dynamic> task, {required bool isRunning}) {
    return AgriOpsCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                decoration: BoxDecoration(color: isRunning ? Colors.orange.withOpacity(0.2) : Colors.blue.withOpacity(0.2), borderRadius: BorderRadius.circular(6)),
                child: Text(isRunning ? 'IN PROGRESS' : 'SCHEDULED', style: TextStyle(color: isRunning ? Colors.orangeAccent : Colors.blueAccent, fontSize: 11, fontWeight: FontWeight.bold)),
              ),
              Text(task['scheduled_at'] != null ? task['scheduled_at'].substring(11, 16) : '08:00', style: const TextStyle(color: Colors.white54, fontSize: 12)),
            ],
          ),
          const SizedBox(height: 12),
          Text(task['title'], style: const TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold)),
          const SizedBox(height: 6),
          Text(task['description'] ?? 'Blok K30 - Divisi 5', style: const TextStyle(color: Colors.white60, fontSize: 13)),
          const SizedBox(height: 16),
          SizedBox(
            width: double.infinity,
            child: ElevatedButton(
              style: ElevatedButton.styleFrom(backgroundColor: isRunning ? Colors.orange : Colors.green, foregroundColor: Colors.white, padding: const EdgeInsets.symmetric(vertical: 12), shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8))),
              onPressed: () async {
                if (!isRunning) await ApiService.startTask(task['id']);
                else await ApiService.completeTask(task['id'], 'Selesai');
                loadData();
              },
              child: Text(isRunning ? 'Selesaikan Tugas' : 'Mulai Tugas'),
            ),
          ),
        ],
      ),
    );
  }
}
