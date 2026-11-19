import 'package:http/http.dart' as http;
import 'access.dart';
import 'remote/admin_api.dart';
import 'remote/auth.dart';
import 'remote/firestore.dart';
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

  factory Services.create(AppStore store, ContentSync Function(Firestore) makeSync, {http.Client? client}) {
    final c = client ?? http.Client();
    final auth = AdminAuth(c);
    final publicFs = Firestore(c);
    return Services._(store, AccessController(AccessService(publicFs, FlutterSecureKv())), makeSync(publicFs), auth, AdminApi(Firestore(c, token: auth.token)));
  }
}
