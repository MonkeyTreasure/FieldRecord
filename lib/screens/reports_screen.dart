import 'package:flutter/material.dart';
import '../shared/ui_components.dart';

class ReportsScreen extends StatelessWidget {
  const ReportsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: const AgriOpsHeader(showSearch: false, showFilter: false),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(16, 16, 16, 84),
        children: [
          const Text('Operational Reports', style: TextStyle(color: Colors.white, fontSize: 20, fontWeight: FontWeight.bold)),
          const SizedBox(height: 16),
          
          // Monthly Summary & Coverage
          AgriOpsCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('Monthly Summary & Coverage', style: TextStyle(color: Colors.greenAccent, fontWeight: FontWeight.bold, fontSize: 16)),
                const SizedBox(height: 16),
                _buildStatRow('Tasks Completed', '42 / 50 tasks'),
                const Divider(color: Colors.white24),
                _buildStatRow('Field Notes Recorded', '38 Notes'),
                const Divider(color: Colors.white24),
                _buildStatRow('Findings Logged', '14 Cases'),
                const Divider(color: Colors.white24),
                _buildStatRow('Estate Coverage', 'Rayon 1 (88%)'),
              ],
            ),
          ),
          const SizedBox(height: 20),

          // Recent Activity
          const Text('RECENT ACTIVITY', style: TextStyle(color: Colors.greenAccent, fontWeight: FontWeight.bold, fontSize: 13, letterSpacing: 1.1)),
          const SizedBox(height: 12),
          _buildActivityItem('Inspeksi Blok K30', 'Selesai • 1h 18m • 4 Findings', 'Hari ini, 09:21'),
          _buildActivityItem('Fertilizer Check K31', 'Selesai • 35m • 2 Findings', 'Hari ini, 10:37'),
          _buildActivityItem('Drainage Check K32', 'Pending • Sesuai Jadwal', 'Hari ini, 14:00'),

          const SizedBox(height: 24),
          ElevatedButton.icon(
            style: ElevatedButton.styleFrom(
              backgroundColor: const Color(0xFF2E7D32),
              foregroundColor: Colors.white,
              padding: const EdgeInsets.symmetric(vertical: 14),
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
            ),
            onPressed: () {
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(content: Text('Laporan bulanan & operasional berhasil disinkronkan ke server!')),
              );
            },
            icon: const Icon(Icons.cloud_upload),
            label: const Text('Sync Reports to Server', style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold)),
          ),
        ],
      ),
    );
  }

  Widget _buildStatRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8.0),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: const TextStyle(color: Colors.white70, fontSize: 14)),
          Text(value, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 14)),
        ],
      ),
    );
  }

  Widget _buildActivityItem(String title, String subtitle, String time) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: AgriOpsCard(
        padding: const EdgeInsets.all(12),
        child: Row(
          children: [
            const Icon(Icons.assignment_turned_in, color: Colors.greenAccent, size: 24),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(title, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 14)),
                  const SizedBox(height: 2),
                  Text(subtitle, style: const TextStyle(color: Colors.white60, fontSize: 12)),
                ],
              ),
            ),
            Text(time, style: const TextStyle(color: Colors.white38, fontSize: 11)),
          ],
        ),
      ),
    );
  }
}
