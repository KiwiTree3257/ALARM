import 'package:alarm/alarm.dart';
import 'package:flutter/material.dart';
import 'package:flutter_foreground_task/flutter_foreground_task.dart';
import 'package:permission_handler/permission_handler.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await Alarm.init();
  FlutterForegroundTask.initCommunicationPort();
  runApp(const MainApp());
}

class MainApp extends StatelessWidget {
  const MainApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: '알람 앱',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
        useMaterial3: true,
      ),
      home: const _PermissionGateway(),
    );
  }
}

class _PermissionGateway extends StatefulWidget {
  const _PermissionGateway();

  @override
  State<_PermissionGateway> createState() => _PermissionGatewayState();
}

class _PermissionGatewayState extends State<_PermissionGateway> {
  bool _permissionsGranted = false;

  @override
  void initState() {
    super.initState();
    _requestPermissions();
  }

  Future<void> _requestPermissions() async {
    final results = await [
      Permission.scheduleExactAlarm,
      Permission.notification,
      Permission.ignoreBatteryOptimizations,
    ].request();

    final allGranted = results.values.every(
      (s) => s == PermissionStatus.granted,
    );
    if (mounted) setState(() => _permissionsGranted = allGranted);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Center(
        child: _permissionsGranted
            ? const Text('알람 앱 — 기반 설정 완료')
            : Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Text('권한이 필요합니다'),
                  const SizedBox(height: 12),
                  ElevatedButton(
                    onPressed: _requestPermissions,
                    child: const Text('권한 허용'),
                  ),
                ],
              ),
      ),
    );
  }
}
