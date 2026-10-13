import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:google_sign_in/google_sign_in.dart';
import 'package:shared_preferences/shared_preferences.dart';

class ApiService {
  static const String baseUrl = "http://192.168.1.4:8006";
  
  static final GoogleSignIn _googleSignIn = GoogleSignIn(
    // Optional: add serverClientId if backend validation requires Web client ID
    scopes: ['email', 'profile'],
  );

  static Future<String?> getToken() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString('jwt_token');
  }

  static Future<void> saveToken(String token) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('jwt_token', token);
  }

  // Google Sign-In & Sign-Up
  static Future<Map<String, dynamic>?> signInWithGoogle() async {
    try {
      final GoogleSignInAccount? googleUser = await _googleSignIn.signIn();
      if (googleUser == null) return null; // User canceled sign-in

      final GoogleSignInAuthentication googleAuth = await googleUser.authentication;
      final String? idToken = googleAuth.idToken;

      if (idToken == null) {
        // Fallback to accessToken if idToken is null on certain platforms
        print("Google idToken is null, using server authentication exchange...");
      }

      // Send credential to backend
      final response = await http.post(
        Uri.parse('$baseUrl/auth/google'),
        headers: {"Content-Type": "application/json"},
        body: json.encode({"credential": idToken ?? googleAuth.accessToken}),
      );

      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        await saveToken(data['access_token']);
        return data['user'];
      } else {
        print("Backend Google Auth failed: ${response.body}");
      }
    } catch (e) {
      print("Google Sign-In Error: $e");
    }
    return null;
  }

  static Future<Map<String, dynamic>?> getTodayTasks(int userId) async {
    try {
      final token = await getToken();
      final response = await http.get(
        Uri.parse('$baseUrl/tasks/today?user_id=$userId'),
        headers: token != null ? {"Authorization": "Bearer $token"} : {},
      );
      if (response.statusCode == 200) {
        return json.decode(response.body);
      }
    } catch (e) {
      print("Error fetching tasks: $e");
    }
    return null;
  }

  static Future<bool> startTask(int taskId) async {
    try {
      final token = await getToken();
      final response = await http.post(
        Uri.parse('$baseUrl/tasks/$taskId/start'),
        headers: token != null ? {"Authorization": "Bearer $token"} : {},
      );
      return response.statusCode == 200;
    } catch (e) {
      print("Error starting task: $e");
      return false;
    }
  }

  static Future<bool> completeTask(int taskId, String notes) async {
    try {
      final token = await getToken();
      final response = await http.post(
        Uri.parse('$baseUrl/tasks/$taskId/complete?notes=${Uri.encodeComponent(notes)}'),
        headers: token != null ? {"Authorization": "Bearer $token"} : {},
      );
      return response.statusCode == 200;
    } catch (e) {
      print("Error completing task: $e");
      return false;
    }
  }
}
