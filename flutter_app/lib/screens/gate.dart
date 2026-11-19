import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../access.dart';
import '../widgets.dart';

class _Upper extends TextInputFormatter {
  @override
  TextEditingValue formatEditUpdate(TextEditingValue o, TextEditingValue n) => n.copyWith(text: n.text.toUpperCase());
}

Widget brandMark({VoidCallback? onLongPress, double size = 72}) => GestureDetector(
      onLongPress: onLongPress,
      child: Container(
        width: size, height: size,
        decoration: BoxDecoration(gradient: const LinearGradient(colors: [Color(0xFF12A583), Color(0xFF0A7A62)]), borderRadius: BorderRadius.circular(size * .3)),
        child: Icon(Icons.school, color: Colors.white, size: size * .5),
      ),
    );

/// First screen until a valid activation code is entered. Long-press the logo for admin sign-in.
class GateScreen extends StatefulWidget {
  const GateScreen({super.key, required this.access, required this.onAdmin});
  final AccessController access;
  final VoidCallback onAdmin;
  @override
  State<GateScreen> createState() => _GateScreenState();
}

class _GateScreenState extends State<GateScreen> {
  final _c = TextEditingController();
  bool _busy = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _error = widget.access.message;
  }

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (_busy) return;
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      await widget.access.activate(_c.text);
    } on ActivationException catch (e) {
      if (mounted) setState(() => _error = e.message);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Scaffold(
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 420),
              child: Column(children: [
                brandMark(onLongPress: widget.onAdmin),
                const SizedBox(height: 16),
                Text('بوابة القبول الذكية', style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w800)),
                const SizedBox(height: 4),
                Text('جامعة عدن · 2026/2027', style: TextStyle(color: cs.onSurfaceVariant)),
                const SizedBox(height: 28),
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(20),
                    child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
                      Row(children: [Icon(Icons.vpn_key_outlined, color: cs.primary), const SizedBox(width: 8), Text('أدخل رمز التفعيل', style: Theme.of(context).textTheme.titleMedium)]),
                      const SizedBox(height: 6),
                      Text('الرمز يعمل على جهاز واحد فقط. تحصل عليه من المشرف.', style: TextStyle(color: cs.onSurfaceVariant, fontSize: 13)),
                      const SizedBox(height: 16),
                      TextField(
                        controller: _c,
                        textDirection: TextDirection.ltr,
                        textAlign: TextAlign.center,
                        autocorrect: false,
                        enableSuggestions: false,
                        textCapitalization: TextCapitalization.characters,
                        inputFormatters: [_Upper()],
                        style: const TextStyle(fontFamily: 'monospace', fontSize: 18, letterSpacing: 1.5),
                        decoration: const InputDecoration(hintText: 'ADEN-XXXX-XXXX-XXXX', border: OutlineInputBorder()),
                        onSubmitted: (_) => _submit(),
                      ),
                      if (_error != null)
                        Padding(
                          padding: const EdgeInsets.only(top: 12),
                          child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
                            Icon(Icons.error_outline, size: 18, color: cs.error),
                            const SizedBox(width: 6),
                            Expanded(child: Text(_error!, style: TextStyle(color: cs.error), semanticsLabel: _error)),
                          ]),
                        ),
                      const SizedBox(height: 16),
                      FilledButton(
                        onPressed: _busy ? null : _submit,
                        style: FilledButton.styleFrom(minimumSize: const Size.fromHeight(48)),
                        child: _busy ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2)) : const Text('تفعيل'),
                      ),
                    ]),
                  ),
                ),
                const SizedBox(height: 12),
                TextButton.icon(onPressed: widget.onAdmin, icon: const Icon(Icons.admin_panel_settings_outlined, size: 18), label: const Text('دخول المشرف')),
              ]),
            ),
          ),
        ),
      ),
    );
  }
}

class MaintenanceScreen extends StatelessWidget {
  const MaintenanceScreen({super.key, required this.message, required this.onAdmin});
  final String message;
  final VoidCallback onAdmin;
  @override
  Widget build(BuildContext context) => Scaffold(
        body: SafeArea(
          child: Center(
            child: Padding(
              padding: const EdgeInsets.all(28),
              child: Column(mainAxisSize: MainAxisSize.min, children: [
                brandMark(onLongPress: onAdmin),
                const SizedBox(height: 20),
                const Icon(Icons.construction_outlined, size: 40, color: brand),
                const SizedBox(height: 10),
                Text('التطبيق في وضع الصيانة', style: Theme.of(context).textTheme.titleLarge),
                const SizedBox(height: 8),
                Text(message.isEmpty ? 'نعمل على التحسين، عد بعد قليل.' : message, textAlign: TextAlign.center),
              ]),
            ),
          ),
        ),
      );
}
