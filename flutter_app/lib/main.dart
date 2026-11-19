import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'data.dart';
import 'screens/bank.dart';
import 'screens/exam.dart';
import 'screens/home.dart';
import 'screens/review.dart';
import 'store.dart';
import 'search.dart';
import 'widgets.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final prefs = await SharedPreferences.getInstance();
  final bank = await Bank.load();
  runApp(AdenApp(store: AppStore(bank, prefs)));
}

class AdenApp extends StatelessWidget {
  const AdenApp({super.key, required this.store});
  final AppStore store;

  ThemeData _theme(Brightness b) => ThemeData(
        useMaterial3: true,
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
        home: Shell(store: store),
      ),
    );
  }
}

class Shell extends StatefulWidget {
  const Shell({super.key, required this.store});
  final AppStore store;
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
      HomeScreen(store: s, go: (t) => setState(() => tab = t)),
      BankScreen(store: s),
      ExamScreen(store: s),
      ReviewScreen(store: s),
    ];
    return Scaffold(
      appBar: AppBar(
        title: const Text('بوابة القبول الذكية'),
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
