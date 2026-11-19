import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:aden_portal/data.dart';
import 'package:aden_portal/store.dart';

void main() {
  test('Leitner: right climbs, wrong resets to box 1', () async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    final q = Question('q1', 'law', 'الحقوق', 'مادة', 'وحدة', 'سهل', 'ar', 'س', ['أ', 'ب'], 0, '');
    final store = AppStore(Bank.forTest([q]), prefs);
    store.record([const MapEntry('q1', true)]);
    expect(store.stats['q1']![0], 1);
    store.record([const MapEntry('q1', true)]);
    expect(store.stats['q1']![0], 2);
    store.record([const MapEntry('q1', false)]);
    expect(store.stats['q1']![0], 1);
    expect(store.mistakes, contains('q1'));
    expect(store.streak, 1);
    expect(store.todayCount, 3);
  });
}
