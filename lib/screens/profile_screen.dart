import 'package:flutter/material.dart';
import '../shared/ui_components.dart';

class ProfileScreen extends StatelessWidget {
  const ProfileScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: const AgriOpsHeader(showSearch: false, showFilter: false),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(16, 16, 16, 84),
        children: [
          const Center(
            child: CircleAvatar(
              radius: 40,
              backgroundColor: Colors.greenAccent,
              child: Icon(Icons.person, size: 50, color: Colors.black87),
            ),
          ),
          const SizedBox(height: 12),
          const Center(
            child: Text('Novianto Syahmud', style: TextStyle(color: Colors.white, fontSize: 20, fontWeight: FontWeight.bold)),
          ),
          const Center(
            child: Text('Askep Rayon 1 / Mandor Lapangan', style: TextStyle(color: Colors.greenAccent, fontSize: 13)),
          ),
          const SizedBox(height: 24),
          AgriOpsCard(
            child: Column(
              children: [
                _buildProfileItem(Icons.domain, 'Estate / Rayon', 'SBYE - Rayon 1'),
                const Divider(color: Colors.white24),
                _buildProfileItem(Icons.phone, 'Kontak', '+62 812-3456-7890'),
                const Divider(color: Colors.white24),
                _buildProfileItem(Icons.cloud_sync, 'Status Server', 'Terhubung (:8006)'),
                const Divider(color: Colors.white24),
                _buildProfileItem(Icons.info_outline, 'Versi Aplikasi', 'v1.0.0 (UI Package Aligned)'),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildProfileItem(IconData icon, String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8.0),
      child: Row(
        children: [
          Icon(icon, color: Colors.greenAccent, size: 20),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(label, style: const TextStyle(color: Colors.white54, fontSize: 11)),
                Text(value, style: const TextStyle(color: Colors.white, fontSize: 14, fontWeight: FontWeight.w500)),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
