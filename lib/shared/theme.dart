import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

class AppThemes {
  static final ThemeData lightTheme = ThemeData(
    brightness: Brightness.light,
    colorScheme: ColorScheme.fromSeed(
      seedColor: const Color(0xFF2E7D32), // emerald
      primary: const Color(0xFF2E7D32),
      secondary: const Color(0xFFFFB300),
      brightness: Brightness.light,
    ),
    useMaterial3: true,
    textTheme: GoogleFonts.interTextTheme(),
  );

  static final ThemeData darkTheme = ThemeData(
    brightness: Brightness.dark,
    colorScheme: ColorScheme.fromSeed(
      seedColor: const Color(0xFF004D40), // deep emerald
      brightness: Brightness.dark,
      primary: const Color(0xFF004D40),
      secondary: const Color(0xFFFF8F00),
    ),
    useMaterial3: true,
    textTheme: GoogleFonts.interTextTheme(
      ThemeData(brightness: Brightness.dark).textTheme,
    ),
  );
}
