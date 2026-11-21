import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'data.dart';
import 'screens/bank.dart';
import 'screens/exam.dart';
import 'screens/home.dart';
import 'screens/review.dart';
import 'store.dart';
import 'remote/supabase.dart';
import 'access.dart';
import 'screens/admin.dart';
import 'screens/gate.dart';
import 'search.dart';
import 'services.dart';
import 'sync.dart';
import 'widgets.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final prefs = await SharedPreferences.getInstance();
  final bank = await Bank.load();
  final store = AppStore(bank, prefs);
  final services = Services.create(store, (db) => ContentSync(db, prefs, bank)..loadLocal());
  runApp(AdenApp(services: services));
  if (SupabaseConfig.enabled) {
    await services.access.init();
    await services.sync.refresh();
  }
}

class AdenApp extends StatelessWidget {
  const AdenApp({super.key, required this.services});
  final Services services;
  AppStore get store => services.store;

  ThemeData _theme(Brightness b) => ThemeData(
        useMaterial3: true,
        fontFamily: 'IBMPlexSansArabic',
        colorScheme: ColorScheme.fromSeed(seedColor: brand, brightness: b),
        cardTheme: CardThemeData(elevation: 0, shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16), side: BorderSide(color: b == Brightness.dark ? Colors.white12 : Colors.black12))),
      );

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: store,
      builder: (context, _) => MaterialApp(
        title: 'بوابة القبول',
        debugShowCheckedModeBanner: false,
        locale: const Locale('ar'),
        supportedLocales: const [Locale('ar')],
        localizationsDelegates: const [GlobalMaterialLocalizations.delegate, GlobalWidgetsLocalizations.delegate, GlobalCupertinoLocalizations.delegate],
        theme: _theme(Brightness.light),
        darkTheme: _theme(Brightness.dark),
        themeMode: store.theme,
        home: Root(services: services),
      ),
    );
  }
}

/// Decides what to show: activation gate, maintenance notice, or the app itself.
class Root extends StatelessWidget {
  const Root({super.key, required this.services});
  final Services services;

  @override
  Widget build(BuildContext context) {
    final s = services;
    return ListenableBuilder(
      listenable: Listenable.merge([s.access, s.sync]),
      builder: (context, _) {
        void admin() => openAdmin(context, s);
        if (!SupabaseConfig.enabled) return Shell(services: s);   // no backend configured: open app
        switch (s.access.status) {
          case AccessStatus.loading:
            return const Scaffold(body: Center(child: CircularProgressIndicator()));
          case AccessStatus.locked:
            return GateScreen(access: s.access, onAdmin: admin);
          case AccessStatus.active:
            if (s.sync.config.maintenanceMode) return MaintenanceScreen(message: s.sync.config.maintenanceMessage, onAdmin: admin);
            return Shell(services: s);
        }
      },
    );
  }
}

class Shell extends StatefulWidget {
  const Shell({super.key, required this.services});
  final Services services;
  AppStore get store => services.store;
  @override
  State<Shell> createState() => _ShellState();
}

class _ShellState extends State<Shell> {
  int tab = 0;

  @override
  void initState() {
    super.initState();
    widget.store.addListener(_announce);
  }

  @override
  void dispose() {
    widget.store.removeListener(_announce);
    super.dispose();
  }

  // Tell the user when a badge unlocks.
  void _announce() {
    final s = widget.store;
    if (s.newBadges.isEmpty || !mounted) return;
    final names = s.newBadges.map((b) => b.title).join('، ');
    s.newBadges.clear();
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('وسام جديد: $names')));
  }

  @override
  Widget build(BuildContext context) {
    final s = widget.store;
    final pages = [
      HomeScreen(store: s, sync: widget.services.sync, go: (t) => setState(() => tab = t)),
      BankScreen(store: s),
      ExamScreen(store: s, defaultMinutes: widget.services.sync.config.defaultExamMinutes),
      ReviewScreen(store: s),
    ];
    return Scaffold(
      appBar: AppBar(
        title: GestureDetector(onLongPress: () => openAdmin(context, widget.services), child: const Text('بوابة القبول الذكية')),
        actions: [
          IconButton(tooltip: 'بحث', icon: const Icon(Icons.search), onPressed: () => showSearch(context: context, delegate: QuestionSearch(s))),
          IconButton(
            tooltip: 'الوضع',
            icon: Icon(Theme.of(context).brightness == Brightness.dark ? Icons.light_mode_outlined : Icons.dark_mode_outlined),
            onPressed: () => s.setTheme(Theme.of(context).brightness == Brightness.dark ? ThemeMode.light : ThemeMode.dark),
          ),
        ],
      ),
      body: SafeArea(child: IndexedStack(index: tab, children: pages)),
      bottomNavigationBar: NavigationBar(
        selectedIndex: tab,
        onDestinationSelected: (i) => setState(() => tab = i),
        destinations: [
          const NavigationDestination(icon: Icon(Icons.home_outlined), selectedIcon: Icon(Icons.home), label: 'الرئيسية'),
          const NavigationDestination(icon: Icon(Icons.menu_book_outlined), selectedIcon: Icon(Icons.menu_book), label: 'البنك'),
          const NavigationDestination(icon: Icon(Icons.timer_outlined), selectedIcon: Icon(Icons.timer), label: 'اختبار'),
          NavigationDestination(
            icon: Badge(isLabelVisible: s.dueCount > 0, label: Text('${s.dueCount}'), child: const Icon(Icons.replay_outlined)),
            selectedIcon: const Icon(Icons.replay),
            label: 'مراجعة',
          ),
        ],
      ),
    );
  }
}
