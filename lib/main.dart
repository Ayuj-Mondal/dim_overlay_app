import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() {
  runApp(const DimApp());
}

class DimApp extends StatelessWidget {
  const DimApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      theme: ThemeData.dark().copyWith(
        scaffoldBackgroundColor: const Color(0xFF0F1115),
        colorScheme: const ColorScheme.dark(primary: Colors.deepPurple),
      ),
      home: const HomeScreen(),
    );
  }
}

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  static const platform = MethodChannel('dim_channel');

  double dimLevel = 0.5;
  bool isRunning = false;

  @override
  void initState() {
    super.initState();
    // Listen for updates from the Android Service
    platform.setMethodCallHandler((call) async {
      if (call.method == "onUpdate") {
        setState(() {
          dimLevel = call.arguments["level"];
        });
      } else if (call.method == "onStop") {
        setState(() {
          isRunning = false;
        });
      }
    });
  }

  Future<void> startDim() async {
    // Pass the current 'dimLevel' instead of a fixed value
    await platform.invokeMethod('startDim', {"level": dimLevel});
    setState(() => isRunning = true);
  }

  Future<void> stopDim() async {
    await platform.invokeMethod('stopDim');
    setState(() => isRunning = false);
  }

  Future<void> updateDim() async {
    await platform.invokeMethod('updateDim', {"level": dimLevel});
  }

  Future<void> requestOverlayPermission() async {
    await platform.invokeMethod('requestOverlay');
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("Dim Screen"),
        centerTitle: true,
        elevation: 0,
      ),
      body: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          children: [
            const SizedBox(height: 20),

            // 🔹 Status Card
            Container(
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                // ignore: deprecated_member_use
                color: Colors.white.withOpacity(0.05),
                borderRadius: BorderRadius.circular(20),
              ),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  const Text("Dim Status", style: TextStyle(fontSize: 16)),
                  Switch(
                    value: isRunning,
                    // Inside main.dart Switch onChanged:
                    onChanged: (val) async {
                      if (val) {
                        setState(() {
                          dimLevel = 0.5; // Update UI slider
                          isRunning = true;
                        });
                        // Explicitly send 0.5 to bypass any async delay in state[cite: 1]
                        await platform.invokeMethod('startDim', {"level": 0.5});
                      } else {
                        await stopDim();
                      }
                    },
                  ),
                ],
              ),
            ),

            const SizedBox(height: 30),

            // 🔹 Dim Level Card
            Container(
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                // ignore: deprecated_member_use
                color: Colors.white.withOpacity(0.05),
                borderRadius: BorderRadius.circular(20),
              ),
              child: Column(
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      const Text("Brightness Level"),
                      Text(
                        "${(dimLevel * 100).toInt()}%",
                        style: const TextStyle(
                          fontWeight: FontWeight.bold,
                          fontSize: 16,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 10),
                  Slider(
                    value: dimLevel,
                    min: 0.0,
                    max: 1.0,
                    divisions: 100,
                    label: "${(dimLevel * 100).toInt()}%",
                    onChanged: (value) {
                      setState(() => dimLevel = value);
                      updateDim();
                    },
                  ),
                ],
              ),
            ),

            const SizedBox(height: 30),

            // 🔹 Permission Button
            SizedBox(
              width: double.infinity,
              child: ElevatedButton.icon(
                onPressed: requestOverlayPermission,
                icon: const Icon(Icons.security),
                label: const Text("Grant Overlay Permission"),
                style: ElevatedButton.styleFrom(
                  padding: const EdgeInsets.symmetric(vertical: 14),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12),
                  ),
                ),
              ),
            ),

            const Spacer(),

            // 🔹 Footer
            const Text(
              "Runs in background • Optimized for gaming",
              style: TextStyle(color: Colors.white54, fontSize: 12),
            ),

            const SizedBox(height: 10),
          ],
        ),
      ),
    );
  }
}
