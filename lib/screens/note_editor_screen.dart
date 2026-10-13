import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../shared/ui_components.dart';

class NoteEditorScreen extends StatefulWidget {
  const NoteEditorScreen({Key? key}) : super(key: key);

  @override
  State<NoteEditorScreen> createState() => _NoteEditorScreenState();
}

class _NoteEditorScreenState extends State<NoteEditorScreen> {
  String noteTitle = '';
  String noteContent = '';
  String? selectedPhotoPath;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: const Color(0xFF1E1E1E),
        title: const Text(''),
        actions: const [
          Icon(Icons.push_pin_outlined, color: Colors.white),
          SizedBox(width: 12),
          Icon(Icons.add_alert_outlined, color: Colors.white),
          SizedBox(width: 12),
          Icon(Icons.archive_outlined, color: Colors.white),
          SizedBox(width: 12),
        ],
        elevation: 0,
      ),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            TextField(
              decoration: const InputDecoration(
                hintText: 'Title',
                hintStyle: TextStyle(color: Colors.white54, fontSize: 20, fontWeight: FontWeight.bold),
                border: InputBorder.none,
              ),
              style: const TextStyle(color: Colors.white, fontSize: 20, fontWeight: FontWeight.bold),
              onChanged: (v) => noteTitle = v,
            ),
            const SizedBox(height: 12),
            Expanded(
              child: TextField(
                maxLines: null,
                expands: true,
                decoration: const InputDecoration(
                  hintText: 'Note',
                  hintStyle: TextStyle(color: Colors.white54, fontSize: 16),
                  border: InputBorder.none,
                ),
                style: const TextStyle(color: Colors.white, fontSize: 16),
                onChanged: (v) => noteContent = v,
              ),
            ),
          ],
        ),
      ),
      bottomNavigationBar: BottomAppBar(
        color: const Color(0xFF1E1E1E),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 8.0),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: const [
              Icon(Icons.add_box_outlined, color: Colors.white70),
              Icon(Icons.color_lens_outlined, color: Colors.white70),
              Icon(Icons.image_outlined, color: Colors.white70),
              Icon(Icons.check_circle_outlined, color: Colors.greenAccent),
            ],
          ),
        ),
      ),
    );
  }
}
