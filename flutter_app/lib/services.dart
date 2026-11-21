import 'package:http/http.dart' as http;
import 'access.dart';
import 'remote/admin_api.dart';
import 'remote/auth.dart';
import 'remote/supabase.dart';
import 'store.dart';
import 'sync.dart';

/// Everything the UI needs, wired once in main().
class Services {
  Services._(this.store, this.access, this.sync, this.auth, this.admin);
  final AppStore store;
  final AccessController access;
  final ContentSync sync;
  final AdminAuth auth;
  final AdminApi admin;

  factory Services.create(AppStore store, ContentSync Function(Backend) makeSync, {http.Client? client}) {
    final c = client ?? http.Client();
    final auth = AdminAuth(c);
    final publicDb = Backend(c);
    return Services._(store, AccessController(AccessService(publicDb, FlutterSecureKv())), makeSync(publicDb), auth, AdminApi(Backend(c, token: auth.token)));
  }
}
