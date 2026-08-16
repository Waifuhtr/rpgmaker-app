/*
 * Bridge — native köprü sarmalayıcı.
 *
 * Uygulama içinde `PixelNative` (Kotlin @JavascriptInterface) kullanılır. Tarayıcıda açıldığında
 * (Hugging Face Space üzerindeki canlı demo) aynı API yüzeyini taklit eden bir yedek devreye girer.
 *
 * ÖNEMLİ: Tarayıcı yedeği yalnızca vitrin amaçlıdır. Gerçek yetki kararı APK içinde native tarafta
 * verilir; kullanıcı rolüyle girildiğinde admin verisi köprüden hiç dönmez.
 */
(function (global) {
  'use strict';

  var native = global.PixelNative || null;
  var isNative = !!(native && typeof native.getSession === 'function');

  function parse(raw) {
    try {
      return JSON.parse(raw);
    } catch (e) {
      return { ok: false, error: 'Köprü yanıtı okunamadı.' };
    }
  }

  /** Native çağrıyı bir sonraki mikro göreve erteler; çizim döngüsü bloklanmasın. */
  function callNative(name, args) {
    return Promise.resolve().then(function () {
      return parse(native[name].apply(native, args || []));
    });
  }

  /* ---- Tarayıcı yedeği --------------------------------------------------------------------- */

  var DEMO_ACCOUNTS = {
    admin: { password: 'admin123', displayName: 'Yönetici Yamato', role: 'admin', avatarSeed: 'warden-01', joinedAt: '2024-03-11', note: 'Katalog yöneticisi' },
    user: { password: 'user123', displayName: 'Gezgin Kaya', role: 'user', avatarSeed: 'wanderer-07', joinedAt: '2025-01-08', note: 'Standart oyuncu' }
  };

  var WEB_STORE_KEY = 'pixelstore.web.catalog.v1';
  var WEB_SESSION_KEY = 'pixelstore.web.session.v1';
  var ADMIN_ONLY_FIELDS = ['published', 'createdAt', 'internalNote'];

  function WebFallback() {
    this.root = null;
    this.session = null;
    try {
      var saved = localStorage.getItem(WEB_SESSION_KEY);
      if (saved && DEMO_ACCOUNTS[saved]) this.session = this.sessionFor(saved);
    } catch (e) { /* localStorage kapalı olabilir; oturumsuz devam. */ }
  }

  WebFallback.prototype.sessionFor = function (username) {
    var account = DEMO_ACCOUNTS[username];
    return {
      username: username,
      displayName: account.displayName,
      role: account.role,
      avatarSeed: account.avatarSeed,
      isAdmin: account.role === 'admin'
    };
  };

  WebFallback.prototype.load = function () {
    var self = this;
    if (this.root) return Promise.resolve(this.root);
    var stored = null;
    try {
      stored = localStorage.getItem(WEB_STORE_KEY);
    } catch (e) { /* yok say */ }
    if (stored) {
      try {
        self.root = JSON.parse(stored);
        return Promise.resolve(self.root);
      } catch (e) { /* bozuksa tohum veriden yükle */ }
    }
    return fetch('data/catalog.json').then(function (res) {
      return res.json();
    }).then(function (json) {
      self.root = json;
      self.persist();
      return json;
    });
  };

  WebFallback.prototype.persist = function () {
    try {
      localStorage.setItem(WEB_STORE_KEY, JSON.stringify(this.root));
    } catch (e) { /* kota dolabilir; bellekte devam. */ }
  };

  WebFallback.prototype.strip = function (app) {
    var copy = JSON.parse(JSON.stringify(app));
    ADMIN_ONLY_FIELDS.forEach(function (field) { delete copy[field]; });
    return copy;
  };

  WebFallback.prototype.requireSession = function () {
    return this.session ? null : { ok: false, error: 'Oturum açılmamış.' };
  };

  WebFallback.prototype.requireAdmin = function () {
    if (!this.session) return { ok: false, error: 'Oturum açılmamış.' };
    if (!this.session.isAdmin) return { ok: false, error: 'Bu işlem için yönetici yetkisi gerekir.' };
    return null;
  };

  WebFallback.prototype.call = function (name, args) {
    var self = this;
    return this.load().then(function () {
      return self.dispatch(name, args || []);
    });
  };

  WebFallback.prototype.dispatch = function (name, args) {
    var self = this;
    var guard;
    switch (name) {
      case 'login': {
        var username = String(args[0] || '').trim().toLowerCase();
        var account = DEMO_ACCOUNTS[username];
        if (!account || account.password !== String(args[1] || '')) {
          return { ok: false, error: 'Kullanıcı adı veya parola hatalı.' };
        }
        this.session = this.sessionFor(username);
        try { localStorage.setItem(WEB_SESSION_KEY, username); } catch (e) { /* yok say */ }
        return { ok: true, session: this.session };
      }
      case 'logout':
        this.session = null;
        try { localStorage.removeItem(WEB_SESSION_KEY); } catch (e) { /* yok say */ }
        return { ok: true };
      case 'getSession':
        return { ok: true, session: this.session };
      case 'getCatalog': {
        guard = this.requireSession();
        if (guard) return guard;
        var apps = this.root.apps
          .filter(function (app) { return self.session.isAdmin || app.published !== false; })
          .map(function (app) { return self.session.isAdmin ? app : self.strip(app); });
        return { ok: true, apps: apps, categories: this.root.categories, role: this.session.role };
      }
      case 'getApp': {
        guard = this.requireSession();
        if (guard) return guard;
        var found = this.root.apps.filter(function (app) { return app.id === args[0]; })[0];
        if (!found || (!this.session.isAdmin && found.published === false)) {
          return { ok: false, error: 'Kayıt bulunamadı.' };
        }
        return { ok: true, app: this.session.isAdmin ? found : this.strip(found) };
      }
      case 'recordInstall': {
        guard = this.requireSession();
        if (guard) return guard;
        var target = this.root.apps.filter(function (app) { return app.id === args[0]; })[0];
        if (!target || target.published === false) return { ok: false, error: 'Kayıt bulunamadı.' };
        target.installs = (target.installs || 0) + 1;
        this.persist();
        return { ok: true, installs: target.installs };
      }
      case 'openExternalUrl': {
        guard = this.requireSession();
        if (guard) return guard;
        var url = String(args[0] || '');
        if (!/^https?:\/\//.test(url)) return { ok: false, error: 'Yalnızca http/https bağlantıları açılabilir.' };
        global.open(url, '_blank', 'noopener');
        return { ok: true };
      }
      case 'adminSaveApp': {
        guard = this.requireAdmin();
        if (guard) return guard;
        var incoming = JSON.parse(args[0]);
        var index = -1;
        this.root.apps.forEach(function (app, i) { if (app.id === incoming.id) index = i; });
        if (index >= 0) {
          incoming.installs = this.root.apps[index].installs || 0;
          incoming.createdAt = this.root.apps[index].createdAt;
          this.root.apps[index] = incoming;
        } else {
          incoming.id = incoming.id || 'kayit-' + Date.now().toString(36);
          incoming.installs = 0;
          incoming.createdAt = incoming.updatedAt;
          this.root.apps.push(incoming);
        }
        this.persist();
        return { ok: true, app: incoming };
      }
      case 'adminDeleteApp': {
        guard = this.requireAdmin();
        if (guard) return guard;
        var before = this.root.apps.length;
        this.root.apps = this.root.apps.filter(function (app) { return app.id !== args[0]; });
        if (this.root.apps.length === before) return { ok: false, error: 'Kayıt bulunamadı.' };
        this.persist();
        return { ok: true };
      }
      case 'adminSetPublished': {
        guard = this.requireAdmin();
        if (guard) return guard;
        var toggled = this.root.apps.filter(function (app) { return app.id === args[0]; })[0];
        if (!toggled) return { ok: false, error: 'Kayıt bulunamadı.' };
        toggled.published = !!args[1];
        this.persist();
        return { ok: true, published: toggled.published };
      }
      case 'adminListUsers': {
        guard = this.requireAdmin();
        if (guard) return guard;
        return {
          ok: true,
          users: Object.keys(DEMO_ACCOUNTS).map(function (key) {
            var account = DEMO_ACCOUNTS[key];
            return {
              username: key,
              displayName: account.displayName,
              role: account.role,
              avatarSeed: account.avatarSeed,
              joinedAt: account.joinedAt,
              note: account.note
            };
          })
        };
      }
      case 'adminGetStats': {
        guard = this.requireAdmin();
        if (guard) return guard;
        var stats = { totalApps: 0, published: 0, drafts: 0, totalInstalls: 0, averageRating: 0, perCategory: {} };
        var ratingSum = 0;
        var ratingCount = 0;
        this.root.apps.forEach(function (app) {
          stats.totalApps++;
          if (app.published === false) stats.drafts++; else stats.published++;
          stats.totalInstalls += app.installs || 0;
          if (app.rating > 0) { ratingSum += app.rating; ratingCount++; }
          stats.perCategory[app.category] = (stats.perCategory[app.category] || 0) + 1;
        });
        stats.averageRating = ratingCount ? Math.round((ratingSum / ratingCount) * 10) / 10 : 0;
        return { ok: true, stats: stats };
      }
      case 'adminResetCatalog': {
        guard = this.requireAdmin();
        if (guard) return guard;
        this.root = null;
        try { localStorage.removeItem(WEB_STORE_KEY); } catch (e) { /* yok say */ }
        return { ok: true };
      }
      case 'getBuildInfo':
        return { ok: true, versionName: 'web-demo', versionCode: 0, applicationId: 'com.waifuhtr.pixelstore', bridgeApi: 1, debug: true };
      default:
        return { ok: false, error: 'Bilinmeyen köprü çağrısı: ' + name };
    }
  };

  var fallback = isNative ? null : new WebFallback();

  function call(name, args) {
    if (isNative) return callNative(name, args);
    return fallback.call(name, args);
  }

  global.PixelBridge = {
    isNative: isNative,

    login: function (username, password) { return call('login', [username, password]); },
    logout: function () { return call('logout'); },
    getSession: function () { return call('getSession'); },
    getCatalog: function () { return call('getCatalog'); },
    getApp: function (id) { return call('getApp', [id]); },
    recordInstall: function (id) { return call('recordInstall', [id]); },
    openExternalUrl: function (url) { return call('openExternalUrl', [url]); },
    getBuildInfo: function () { return call('getBuildInfo'); },

    adminSaveApp: function (app) { return call('adminSaveApp', [JSON.stringify(app)]); },
    adminDeleteApp: function (id) { return call('adminDeleteApp', [id]); },
    adminSetPublished: function (id, published) { return call('adminSetPublished', [id, published]); },
    adminListUsers: function () { return call('adminListUsers'); },
    adminGetStats: function () { return call('adminGetStats'); },
    adminResetCatalog: function () { return call('adminResetCatalog'); },

    /** Kısa dokunsal geri bildirim; tarayıcıda sessizce yok sayılır. */
    haptic: function (durationMs) {
      if (isNative && typeof native.vibrate === 'function') {
        native.vibrate(durationMs || 12);
      } else if (global.navigator && global.navigator.vibrate) {
        global.navigator.vibrate(durationMs || 12);
      }
    },

    exitApp: function () {
      if (isNative && typeof native.exitApp === 'function') native.exitApp();
    }
  };
})(window);
