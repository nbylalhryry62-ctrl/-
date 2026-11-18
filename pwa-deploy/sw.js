/* Service worker - بوابة القبول الذكية (جامعة عدن) */
var VERSION = 'v92a8e50561';
var CORE = 'aden-core-' + VERSION;
var RUNTIME = 'aden-runtime-' + VERSION;
var PRECACHE = ['./', './index.html', './manifest.json', './icons/icon-192.png', './icons/icon-512.png', './icons/icon-maskable-512.png', './icons/apple-touch-icon.png', './js/data/syllabus.js', './js/data/qbank-data.js', './js/core/util.js', './js/core/storage.js', './js/core/store.js', './js/core/actions.js', './js/core/router.js', './js/ui/icons.js', './js/ui/theme.js', './js/ui/mathtext.js', './js/ui/components.js', './js/ui/charts.js', './js/data/bank.js', './js/ui/question.js', './js/features/progress.js', './js/features/learning.js', './js/features/exam.js', './js/ui/search.js', './js/ui/shell.js', './js/views/home.js', './js/views/syllabus.js', './js/views/bank.js', './js/views/exam.js', './js/views/review.js', './js/main.js', './css/tokens.css', './css/base.css', './css/components.css', './css/shell.css', './css/views.css'];

self.addEventListener('install', function (event) {
  event.waitUntil(caches.open(CORE).then(function (c) { return c.addAll(PRECACHE); }));
  self.skipWaiting();
});

self.addEventListener('activate', function (event) {
  event.waitUntil(
    caches.keys().then(function (keys) {
      return Promise.all(keys.filter(function (k) { return k !== CORE && k !== RUNTIME; }).map(function (k) { return caches.delete(k); }));
    }).then(function () { return self.clients.claim(); })
  );
});

function store(name, req, res) {
  if (res && (res.ok || res.type === 'opaque')) {
    var copy = res.clone();
    caches.open(name).then(function (c) { c.put(req, copy); });
  }
  return res;
}

self.addEventListener('fetch', function (event) {
  var req = event.request;
  if (req.method !== 'GET') return;
  var url = new URL(req.url);
  if (url.protocol !== 'http:' && url.protocol !== 'https:') return;

  // Pages: network first, fall back to the cached app shell when offline.
  if (req.mode === 'navigate') {
    event.respondWith(
      fetch(req).then(function (res) { return store(CORE, './index.html', res); })
        .catch(function () { return caches.match('./index.html'); })
    );
    return;
  }

  // Same-origin files: cache first.
  if (url.origin === self.location.origin) {
    event.respondWith(
      caches.match(req).then(function (hit) {
        return hit || fetch(req).then(function (res) { return store(CORE, req, res); });
      })
    );
    return;
  }

  // CDN files (Tailwind, icons, fonts): serve from cache and refresh in the background.
  event.respondWith(
    caches.open(RUNTIME).then(function (cache) {
      return cache.match(req).then(function (hit) {
        var net = fetch(req).then(function (res) { return store(RUNTIME, req, res); }).catch(function () { return hit; });
        return hit || net;
      });
    })
  );
});
