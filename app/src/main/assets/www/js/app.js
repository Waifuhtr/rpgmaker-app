/*
 * App — kabuk, yönlendirme ve oturum akışı.
 *
 * Native taraf yalnızca iki noktadan buraya dokunur:
 *   PixelStore.handleBack()  -> donanım geri tuşu
 *   PixelStore.onHostPause() -> uygulama arka plana geçti
 */
(function (global) {
  'use strict';

  var UI = global.PixelUI;
  var Art = global.PixelArt;
  var Views = global.PixelViews;
  var Admin = global.PixelAdmin;
  var el = UI.el;

  var state = {
    session: null,
    apps: [],
    categories: [],
    query: '',
    category: null,
    sort: 'featured'
  };

  /** Geçmiş yığını: her giriş bir ekran ve o ekranın kaydırma konumu. */
  var stack = [];

  var dom = {};

  var ROUTES = {
    home:          { title: 'PIXELSTORE',  tab: 'home',       render: function (ctx) { return Views.renderHome(ctx); } },
    categories:    { title: 'TÜRLER',      tab: 'categories', render: function (ctx) { return Views.renderCategories(ctx); } },
    profile:       { title: 'PROFİL',      tab: 'profile',    render: function (ctx) { return Views.renderProfile(ctx); } },
    detail:        { title: 'KAYIT',       render: function (ctx, params) { return Views.renderDetail(ctx, params); } },
    admin:         { title: 'YÖNETİM',     tab: 'admin', adminOnly: true, render: function (ctx) { return Admin.renderAdminHome(ctx); } },
    'admin-users': { title: 'KULLANICILAR', adminOnly: true, render: function (ctx) { return Admin.renderAdminUsers(ctx); } },
    'admin-edit':  { title: 'KAYIT DÜZENLE', adminOnly: true, render: function (ctx, params) { return Admin.renderAdminEditor(ctx, params); } }
  };

  var TABS = [
    { id: 'home', route: 'home', label: 'Mağaza', glyph: 'shield' },
    { id: 'categories', route: 'categories', label: 'Türler', glyph: 'map' },
    { id: 'profile', route: 'profile', label: 'Profil', glyph: 'star' },
    { id: 'admin', route: 'admin', label: 'Yönetim', glyph: 'wrench', adminOnly: true }
  ];

  /* ---- Ekran bağlamı ------------------------------------------------------------------------ */

  var ctx = {
    state: state,
    navigate: navigate,
    back: back,
    openApp: function (id) { navigate('detail', { id: id }); },
    refreshCatalog: refreshCatalog,
    logout: logout,
    onLogin: onLogin
  };

  /* ---- Katalog ------------------------------------------------------------------------------ */

  function refreshCatalog() {
    return global.PixelBridge.getCatalog().then(function (res) {
      if (!res.ok) {
        UI.toast(res.error, 'error');
        return false;
      }
      state.apps = res.apps || [];
      state.categories = res.categories || [];
      return true;
    });
  }

  /* ---- Oturum ------------------------------------------------------------------------------- */

  function onLogin(session) {
    state.session = session;
    refreshCatalog().then(function () {
      stack = [];
      buildTabs();
      navigate('home', {}, { replace: true });
      document.body.dataset.role = session.role;
      UI.toast('Hoş geldin, ' + session.displayName, 'success');
    });
  }

  function logout() {
    global.PixelBridge.logout().then(function () {
      state.session = null;
      state.apps = [];
      state.query = '';
      state.category = null;
      stack = [];
      delete document.body.dataset.role;
      showLogin();
    });
  }

  function showLogin() {
    dom.shell.classList.add('is-login');
    UI.clear(dom.tabbar);
    UI.clear(dom.topbar);
    UI.clear(dom.screen);
    dom.screen.appendChild(Views.renderLogin(ctx));
    dom.screen.scrollTop = 0;
  }

  /* ---- Yönlendirme -------------------------------------------------------------------------- */

  function currentEntry() {
    return stack[stack.length - 1] || null;
  }

  function navigate(routeName, params, options) {
    var route = ROUTES[routeName];
    if (!route) return;
    // Yetki kontrolü: admin ekranları kullanıcı oturumunda hiç oluşturulmaz.
    if (route.adminOnly && !(state.session && state.session.isAdmin)) {
      UI.toast('Bu bölüm için yönetici yetkisi gerekir.', 'error');
      return;
    }

    var entry = currentEntry();
    if (entry) entry.scroll = dom.screen.scrollTop;

    if (options && options.replace) stack = [];
    if (route.tab) {
      // Sekme kökleri yığını sıfırlar; alt ekranlar üstüne binmez.
      stack = [{ route: routeName, params: params || {}, scroll: 0 }];
    } else {
      stack.push({ route: routeName, params: params || {}, scroll: 0 });
    }
    paint();
  }

  function back() {
    if (stack.length <= 1) return false;
    stack.pop();
    paint();
    return true;
  }

  function paint() {
    var entry = currentEntry();
    if (!entry) return;
    var route = ROUTES[entry.route];

    UI.clear(dom.screen);
    dom.screen.appendChild(route.render(ctx, entry.params));
    dom.screen.scrollTop = entry.scroll || 0;

    paintTopbar(route);
    paintTabs(route);
  }

  function paintTopbar(route) {
    UI.clear(dom.topbar);
    var canGoBack = stack.length > 1;

    UI.append(dom.topbar, [
      canGoBack ? el('button', {
        class: 'topbar__back', type: 'button', 'aria-label': 'Geri',
        onclick: function () { UI.tap('cancel'); back(); }
      }, '◀') : el('span', { class: 'topbar__crest' }, Art.iconElement('pixelstore-crest', 'amber')),
      el('h1', { class: 'topbar__title', text: route.title }),
      state.session ? el('button', {
        class: 'topbar__user', type: 'button', 'aria-label': 'Profil',
        onclick: function () { UI.tap('select'); navigate('profile'); }
      }, [
        el('span', { class: 'topbar__role badge badge--' + state.session.role, text: state.session.isAdmin ? 'ADM' : 'USR' }),
        Art.avatarElement(state.session.avatarSeed, state.session.isAdmin)
      ]) : null
    ]);
  }

  function buildTabs() {
    UI.clear(dom.tabbar);
    var isAdmin = !!(state.session && state.session.isAdmin);
    TABS.forEach(function (tab) {
      // Yönetim sekmesi kullanıcı rolünde DOM'a hiç eklenmez; CSS ile gizlenmiş bir düğüm bırakmayız.
      if (tab.adminOnly && !isAdmin) return;
      dom.tabbar.appendChild(el('button', {
        class: 'tab', type: 'button', dataset: { tab: tab.id },
        onclick: function () {
          UI.tap('move', 8);
          navigate(tab.route);
        }
      }, [
        el('span', { class: 'tab__glyph' }, Art.glyphElement(tab.glyph, '#9a9ac2')),
        el('span', { class: 'tab__label', text: tab.label })
      ]));
    });
  }

  function paintTabs(route) {
    var activeTab = route.tab || (currentEntry() && ROUTES[stack[0].route] ? ROUTES[stack[0].route].tab : null);
    Array.prototype.forEach.call(dom.tabbar.children, function (node) {
      var active = node.dataset.tab === activeTab;
      node.classList.toggle('is-active', active);
      var glyph = node.querySelector('canvas');
      if (glyph) {
        var replacement = Art.glyphElement(
          TABS.filter(function (t) { return t.id === node.dataset.tab; })[0].glyph,
          active ? '#f2c14e' : '#9a9ac2'
        );
        replacement.className = glyph.className;
        glyph.parentNode.replaceChild(replacement, glyph);
      }
    });
  }

  /* ---- Native geri çağrıları ---------------------------------------------------------------- */

  function handleBack() {
    if (UI.isModalOpen()) {
      UI.closeModal();
      return true;
    }
    if (back()) return true;
    // Ana sekmede değilsek önce mağazaya dön; oradan geri tuşu uygulamadan çıkar.
    var entry = currentEntry();
    if (entry && entry.route !== 'home' && state.session) {
      navigate('home');
      return true;
    }
    return false;
  }

  function onHostPause() {
    // Arka plana geçerken açık kalan geçici durumları bırak: odak, basılı düğme görünümü.
    if (document.activeElement && document.activeElement.blur) document.activeElement.blur();
    var entry = currentEntry();
    if (entry) entry.scroll = dom.screen.scrollTop;
  }

  /* ---- Açılış -------------------------------------------------------------------------------- */

  function boot() {
    dom.shell = document.getElementById('shell');
    dom.topbar = document.getElementById('topbar');
    dom.screen = document.getElementById('screen');
    dom.tabbar = document.getElementById('tabbar');

    UI.applySettingsToBody();

    var splash = document.getElementById('splash');

    global.PixelBridge.getSession().then(function (res) {
      var session = res.ok ? res.session : null;
      if (!session) {
        showLogin();
        return finishSplash();
      }
      state.session = session;
      document.body.dataset.role = session.role;
      return refreshCatalog().then(function () {
        dom.shell.classList.remove('is-login');
        buildTabs();
        navigate('home', {}, { replace: true });
        finishSplash();
      });
    }).catch(function (error) {
      // Açılış hatasında kullanıcıyı boş ekranla baş başa bırakma.
      finishSplash();
      showLogin();
      UI.toast('Başlatma hatası: ' + error.message, 'error');
    });

    function finishSplash() {
      if (!splash) return;
      splash.classList.add('is-done');
      global.setTimeout(function () {
        if (splash.parentNode) splash.parentNode.removeChild(splash);
      }, 420);
    }
  }

  // Giriş ekranından ana kabuğa geçerken shell sınıfını temizle.
  var originalOnLogin = ctx.onLogin;
  ctx.onLogin = function (session) {
    dom.shell.classList.remove('is-login');
    originalOnLogin(session);
  };

  global.PixelStore = {
    handleBack: handleBack,
    onHostPause: onHostPause,
    navigate: navigate,
    state: state
  };

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', boot);
  } else {
    boot();
  }
})(window);
