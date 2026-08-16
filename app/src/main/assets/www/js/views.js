/*
 * Views — kullanıcı tarafındaki ekranlar.
 *
 * Her ekran bir DOM düğümü döndürür; yönlendirme app.js içindeki PixelStore router'ında yapılır.
 * Bu dosyadaki hiçbir ekran yönetim verisi göstermez; admin ekranları admin.js içindedir ve
 * yalnızca admin oturumunda yüklenir.
 */
(function (global) {
  'use strict';

  var UI = global.PixelUI;
  var Art = global.PixelArt;
  var el = UI.el;

  var INSTALLED_KEY = 'pixelstore.installed.v1';

  function installedSet() {
    try {
      return JSON.parse(localStorage.getItem(INSTALLED_KEY) || '[]');
    } catch (e) {
      return [];
    }
  }

  function isInstalled(id) {
    return installedSet().indexOf(id) >= 0;
  }

  function markInstalled(id) {
    var list = installedSet();
    if (list.indexOf(id) < 0) list.push(id);
    try {
      localStorage.setItem(INSTALLED_KEY, JSON.stringify(list));
    } catch (e) { /* Depolama kapalıysa kurulum durumu bu oturumla sınırlı kalır. */ }
  }

  function removeInstalled(id) {
    try {
      localStorage.setItem(INSTALLED_KEY, JSON.stringify(installedSet().filter(function (item) {
        return item !== id;
      })));
    } catch (e) { /* yok say */ }
  }

  /* ---- Ortak parçalar ---------------------------------------------------------------------- */

  function sectionTitle(text, action) {
    return el('div', { class: 'section-head' }, [
      el('h2', { class: 'section-title', text: text }),
      action || null
    ]);
  }

  function appCard(app, onOpen) {
    var meta = el('div', { class: 'card__meta' }, [
      UI.stars(app.rating),
      el('span', { class: 'card__rating', text: app.rating > 0 ? UI.formatRating(app.rating) : 'Yeni' }),
      el('span', { class: 'card__dot', text: '•' }),
      el('span', { class: 'card__size', text: UI.formatSize(app.sizeMb) })
    ]);

    var badges = el('div', { class: 'card__badges' }, [
      el('span', { class: 'badge badge--category', text: app.category }),
      app.published === false ? el('span', { class: 'badge badge--draft', text: 'TASLAK' }) : null,
      isInstalled(app.id) ? el('span', { class: 'badge badge--installed', text: 'YÜKLÜ' }) : null
    ]);

    return el('button', {
      class: 'card',
      type: 'button',
      onclick: function () {
        UI.tap('select');
        onOpen(app.id);
      }
    }, [
      el('div', { class: 'card__icon' }, Art.iconElement(app.iconSeed || app.id, app.palette)),
      el('div', { class: 'card__body' }, [
        el('h3', { class: 'card__title', text: app.title }),
        el('p', { class: 'card__dev', text: app.developer }),
        el('p', { class: 'card__desc', text: app.shortDescription }),
        meta,
        badges
      ])
    ]);
  }

  function emptyState(message) {
    return el('div', { class: 'empty' }, [
      el('div', { class: 'empty__art' }, Art.glyphElement('map', '#4b4b74')),
      el('p', { class: 'empty__text', text: message })
    ]);
  }

  /* ---- Giriş ------------------------------------------------------------------------------- */

  function renderLogin(ctx) {
    var username = el('input', {
      class: 'field', type: 'text', id: 'login-user',
      autocomplete: 'username', autocapitalize: 'none', spellcheck: 'false',
      placeholder: 'kullanıcı adı'
    });
    var password = el('input', {
      class: 'field', type: 'password', id: 'login-pass',
      autocomplete: 'current-password', placeholder: 'parola'
    });
    var error = el('p', { class: 'login__error', role: 'alert' });

    function submit(user, pass) {
      error.textContent = '';
      global.PixelBridge.login(user, pass).then(function (res) {
        if (!res.ok) {
          error.textContent = res.error;
          UI.sfx('error');
          return;
        }
        UI.tap('coin');
        ctx.onLogin(res.session);
      });
    }

    var form = el('form', {
      class: 'login__form',
      onsubmit: function (event) {
        event.preventDefault();
        submit(username.value, password.value);
      }
    }, [
      el('label', { class: 'field-label', for: 'login-user', text: 'KULLANICI' }),
      username,
      el('label', { class: 'field-label', for: 'login-pass', text: 'PAROLA' }),
      password,
      error,
      el('button', { class: 'btn btn--primary btn--wide', type: 'submit' }, 'GİRİŞ YAP')
    ]);

    function demoButton(label, user, pass, role) {
      return el('button', {
        class: 'demo-card demo-card--' + role,
        type: 'button',
        onclick: function () {
          username.value = user;
          password.value = pass;
          submit(user, pass);
        }
      }, [
        el('div', { class: 'demo-card__avatar' }, Art.avatarElement(user === 'admin' ? 'warden-01' : 'wanderer-07', role === 'admin')),
        el('div', { class: 'demo-card__text' }, [
          el('strong', { text: label }),
          el('span', { text: user + ' / ' + pass })
        ])
      ]);
    }

    return el('div', { class: 'screen screen--login' }, [
      el('div', { class: 'login__crest' }, Art.iconElement('pixelstore-crest', 'amber', 'px-icon px-icon--xl')),
      el('h1', { class: 'login__title', text: 'PIXELSTORE' }),
      el('p', { class: 'login__tagline', text: 'Piksel dünyanın uygulama mağazası' }),
      el('div', { class: 'panel' }, form),
      el('p', { class: 'login__hint', text: 'DEMO HESAPLAR' }),
      el('div', { class: 'demo-grid' }, [
        demoButton('Yönetici olarak gir', 'admin', 'admin123', 'admin'),
        demoButton('Kullanıcı olarak gir', 'user', 'user123', 'user')
      ])
    ]);
  }

  /* ---- Ana sayfa --------------------------------------------------------------------------- */

  var SORTS = [
    { id: 'featured', label: 'Öne çıkanlar' },
    { id: 'installs', label: 'En çok indirilen' },
    { id: 'rating', label: 'En yüksek puan' },
    { id: 'recent', label: 'En yeni' }
  ];

  function sortApps(apps, mode) {
    var list = apps.slice();
    switch (mode) {
      case 'installs':
        return list.sort(function (a, b) { return (b.installs || 0) - (a.installs || 0); });
      case 'rating':
        return list.sort(function (a, b) { return (b.rating || 0) - (a.rating || 0); });
      case 'recent':
        return list.sort(function (a, b) { return String(b.updatedAt).localeCompare(String(a.updatedAt)); });
      default:
        return list.sort(function (a, b) {
          return ((b.rating || 0) * Math.log10((b.installs || 0) + 10)) -
                 ((a.rating || 0) * Math.log10((a.installs || 0) + 10));
        });
    }
  }

  function matchesQuery(app, query) {
    if (!query) return true;
    var haystack = [app.title, app.developer, app.category, (app.tags || []).join(' ')]
      .join(' ')
      .toLocaleLowerCase('tr');
    return haystack.indexOf(query.toLocaleLowerCase('tr')) >= 0;
  }

  function heroBanner(app, onOpen) {
    var shot = Art.screenshotElement(
      { seed: app.iconSeed + '-hero', kind: 'tablet', scene: 'title' },
      app.palette,
      'px-shot hero__shot'
    );
    return el('button', {
      class: 'hero',
      type: 'button',
      onclick: function () { UI.tap('select'); onOpen(app.id); }
    }, [
      el('div', { class: 'hero__frame' }, shot),
      el('div', { class: 'hero__info' }, [
        el('span', { class: 'hero__label', text: 'HAFTANIN OYUNU' }),
        el('strong', { class: 'hero__title', text: app.title }),
        el('span', { class: 'hero__dev', text: app.developer })
      ])
    ]);
  }

  function renderHome(ctx) {
    var state = ctx.state;
    var listHost = el('div', { class: 'card-list' });

    var search = el('input', {
      class: 'search__input',
      type: 'search',
      value: state.query || '',
      placeholder: 'oyun, geliştirici, etiket ara',
      'aria-label': 'Mağazada ara'
    });
    search.addEventListener('input', function () {
      state.query = search.value;
      paint();
    });

    var chips = el('div', { class: 'chips' });
    var sortRow = el('div', { class: 'sort-row' });

    function paint() {
      UI.clear(listHost);
      var filtered = state.apps.filter(function (app) {
        if (state.category && app.category !== state.category) return false;
        return matchesQuery(app, state.query);
      });
      var sorted = sortApps(filtered, state.sort);
      if (!sorted.length) {
        listHost.appendChild(emptyState('Aramanla eşleşen kayıt yok.'));
        return;
      }
      sorted.forEach(function (app) {
        listHost.appendChild(appCard(app, ctx.openApp));
      });
    }

    function paintChips() {
      UI.clear(chips);
      var all = [{ name: 'Tümü', value: null }].concat(state.categories.map(function (cat) {
        return { name: cat.name, value: cat.name, glyph: cat.glyph };
      }));
      all.forEach(function (item) {
        var active = state.category === item.value;
        chips.appendChild(el('button', {
          class: 'chip' + (active ? ' is-active' : ''),
          type: 'button',
          onclick: function () {
            UI.tap('move', 8);
            state.category = active ? null : item.value;
            paintChips();
            paint();
          }
        }, [
          item.glyph ? Art.glyphElement(item.glyph, active ? '#10101c' : '#f2c14e') : null,
          el('span', { text: item.name })
        ]));
      });
    }

    function paintSort() {
      UI.clear(sortRow);
      SORTS.forEach(function (option) {
        var active = (state.sort || 'featured') === option.id;
        sortRow.appendChild(el('button', {
          class: 'sort' + (active ? ' is-active' : ''),
          type: 'button',
          onclick: function () {
            UI.tap('move', 8);
            state.sort = option.id;
            paintSort();
            paint();
          }
        }, option.label));
      });
    }

    paintChips();
    paintSort();
    paint();

    var featured = sortApps(state.apps.filter(function (app) {
      return app.published !== false;
    }), 'featured')[0];

    return el('div', { class: 'screen' }, [
      el('div', { class: 'search' }, [
        el('span', { class: 'search__glyph' }, Art.glyphElement('gem', '#6be3a0')),
        search
      ]),
      featured ? heroBanner(featured, ctx.openApp) : null,
      sectionTitle('Kategoriler'),
      chips,
      sectionTitle('Mağaza'),
      sortRow,
      listHost
    ]);
  }

  /* ---- Kategoriler ------------------------------------------------------------------------- */

  function renderCategories(ctx) {
    var state = ctx.state;
    var grid = el('div', { class: 'cat-grid' });

    state.categories.forEach(function (cat) {
      var count = state.apps.filter(function (app) { return app.category === cat.name; }).length;
      grid.appendChild(el('button', {
        class: 'cat-tile',
        type: 'button',
        onclick: function () {
          UI.tap('select');
          state.category = cat.name;
          ctx.navigate('home');
        }
      }, [
        el('div', { class: 'cat-tile__glyph' }, Art.glyphElement(cat.glyph, '#f2c14e')),
        el('strong', { class: 'cat-tile__name', text: cat.name }),
        el('span', { class: 'cat-tile__count', text: count + ' kayıt' })
      ]));
    });

    return el('div', { class: 'screen' }, [
      sectionTitle('Türler'),
      el('p', { class: 'muted', text: 'Bir tür seç, mağaza o türe göre filtrelensin.' }),
      grid
    ]);
  }

  /* ---- Detay ------------------------------------------------------------------------------- */

  function infoRow(label, value) {
    return el('div', { class: 'info-row' }, [
      el('span', { class: 'info-row__label', text: label }),
      el('span', { class: 'info-row__value', text: value })
    ]);
  }

  function screenshotStrip(app) {
    var strip = el('div', { class: 'shots' });
    (app.screenshots || []).forEach(function (shot, index) {
      var frame = el('button', {
        class: 'shot shot--' + (shot.kind === 'tablet' ? 'tablet' : 'phone'),
        type: 'button',
        onclick: function () {
          UI.tap('open');
          openShotViewer(app, index);
        }
      }, [
        Art.screenshotElement(shot, app.palette),
        shot.caption ? el('span', { class: 'shot__caption', text: shot.caption }) : null
      ]);
      strip.appendChild(frame);
    });
    if (!strip.childNodes.length) {
      strip.appendChild(el('p', { class: 'muted', text: 'Bu kayıt için ekran görüntüsü yok.' }));
    }
    return strip;
  }

  function openShotViewer(app, startIndex) {
    var index = startIndex;
    var shots = app.screenshots || [];
    var host = el('div', { class: 'viewer__frame' });
    var caption = el('p', { class: 'viewer__caption' });

    function paint() {
      UI.clear(host);
      var shot = shots[index];
      host.appendChild(Art.screenshotElement(shot, app.palette, 'px-shot viewer__shot'));
      caption.textContent = (index + 1) + ' / ' + shots.length + (shot.caption ? ' — ' + shot.caption : '');
    }

    function step(delta) {
      index = (index + delta + shots.length) % shots.length;
      UI.tap('move', 8);
      paint();
    }

    paint();
    UI.modal({
      title: app.title,
      body: el('div', { class: 'viewer' }, [
        host,
        caption,
        el('div', { class: 'viewer__nav' }, [
          el('button', { class: 'btn btn--ghost', type: 'button', onclick: function () { step(-1); } }, '◀'),
          el('button', { class: 'btn btn--ghost', type: 'button', onclick: function () { step(1); } }, '▶')
        ])
      ]),
      actions: [{ label: 'Kapat', variant: 'ghost' }]
    });
  }

  function installFlow(app, button, ctx) {
    var progress = el('span', { class: 'install-progress' });
    UI.clear(button);
    button.disabled = true;
    button.appendChild(el('span', { text: 'İNDİRİLİYOR' }));
    button.appendChild(progress);
    UI.sfx('open');

    var percent = 0;
    function paintProgress() {
      UI.clear(progress);
      progress.appendChild(UI.segBar(percent / 100, 16, 'seg-bar--install'));
    }
    paintProgress();

    var timer = global.setInterval(function () {
      percent = Math.min(100, percent + 6 + Math.random() * 12);
      paintProgress();
      if (percent >= 100) {
        global.clearInterval(timer);
        global.PixelBridge.recordInstall(app.id).then(function (res) {
          markInstalled(app.id);
          if (res.ok) app.installs = res.installs;
          UI.toast(app.title + ' yüklendi', 'success');
          ctx.refreshCatalog();
          paintInstallButton(app, button, ctx);
        });
      }
    }, 140);
  }

  function paintInstallButton(app, button, ctx) {
    UI.clear(button);
    button.disabled = false;
    if (isInstalled(app.id)) {
      button.className = 'btn btn--ghost btn--wide';
      button.appendChild(el('span', { text: 'KALDIR' }));
      button.onclick = function () {
        UI.tap('cancel');
        removeInstalled(app.id);
        UI.toast(app.title + ' kaldırıldı', 'info');
        paintInstallButton(app, button, ctx);
      };
      return;
    }
    button.className = 'btn btn--primary btn--wide';
    button.appendChild(el('span', { text: 'İNDİR' }));
    button.onclick = function () {
      UI.tap('select');
      installFlow(app, button, ctx);
    };
  }

  function renderDetail(ctx, params) {
    var host = el('div', { class: 'screen' }, el('p', { class: 'muted', text: 'Yükleniyor…' }));

    global.PixelBridge.getApp(params.id).then(function (res) {
      UI.clear(host);
      if (!res.ok) {
        host.appendChild(emptyState(res.error));
        return;
      }
      var app = res.app;
      var installButton = el('button', { type: 'button' });
      paintInstallButton(app, installButton, ctx);

      var tags = el('div', { class: 'tag-row' }, (app.tags || []).map(function (tag) {
        return el('span', { class: 'tag', text: tag });
      }));

      host.appendChild(el('div', { class: 'detail__head' }, [
        el('div', { class: 'detail__icon' }, Art.iconElement(app.iconSeed || app.id, app.palette, 'px-icon px-icon--lg')),
        el('div', { class: 'detail__id' }, [
          el('h1', { class: 'detail__title', text: app.title }),
          el('p', { class: 'detail__dev', text: app.developer }),
          el('div', { class: 'detail__badges' }, [
            el('span', { class: 'badge badge--category', text: app.category }),
            el('span', { class: 'badge', text: app.contentRating }),
            app.published === false ? el('span', { class: 'badge badge--draft', text: 'TASLAK' }) : null
          ])
        ])
      ]));

      host.appendChild(el('div', { class: 'stat-strip' }, [
        el('div', { class: 'stat' }, [
          UI.stars(app.rating),
          el('span', { class: 'stat__value', text: app.rating > 0 ? UI.formatRating(app.rating) : '—' }),
          el('span', { class: 'stat__label', text: UI.formatCount(app.ratingCount) + ' oy' })
        ]),
        el('div', { class: 'stat' }, [
          el('span', { class: 'stat__value', text: UI.formatCount(app.installs) }),
          el('span', { class: 'stat__label', text: 'indirme' })
        ]),
        el('div', { class: 'stat' }, [
          el('span', { class: 'stat__value', text: UI.formatSize(app.sizeMb) }),
          el('span', { class: 'stat__label', text: 'boyut' })
        ])
      ]));

      host.appendChild(installButton);

      if (app.downloadUrl) {
        host.appendChild(el('button', {
          class: 'btn btn--ghost btn--wide',
          type: 'button',
          onclick: function () {
            UI.tap('select');
            global.PixelBridge.openExternalUrl(app.downloadUrl).then(function (res2) {
              if (!res2.ok) UI.toast(res2.error, 'error');
            });
          }
        }, 'GELİŞTİRİCİ SAYFASI'));
      }

      host.appendChild(sectionTitle('Ekran görüntüleri'));
      host.appendChild(screenshotStrip(app));

      host.appendChild(sectionTitle('Açıklama'));
      var description = el('div', { class: 'prose' });
      String(app.longDescription || '').split('\n').forEach(function (line) {
        if (line.trim()) description.appendChild(el('p', { text: line.trim() }));
      });
      host.appendChild(description);

      if ((app.tags || []).length) {
        host.appendChild(sectionTitle('Etiketler'));
        host.appendChild(tags);
      }

      host.appendChild(sectionTitle('Bilgiler'));
      host.appendChild(el('div', { class: 'info-table' }, [
        infoRow('Sürüm', app.version),
        infoRow('Güncelleme', app.updatedAt),
        infoRow('Tür', app.category),
        infoRow('İçerik', app.contentRating),
        infoRow('Paket', app.id)
      ]));

      if (ctx.state.session && ctx.state.session.isAdmin) {
        host.appendChild(el('button', {
          class: 'btn btn--admin btn--wide',
          type: 'button',
          onclick: function () {
            UI.tap('select');
            ctx.navigate('admin-edit', { id: app.id });
          }
        }, 'YÖNETİCİ: BU KAYDI DÜZENLE'));
      }
    });

    return host;
  }

  /* ---- Profil ------------------------------------------------------------------------------ */

  function toggleRow(label, key, onChange) {
    var input = el('input', {
      class: 'switch__input', type: 'checkbox', id: 'set-' + key,
      checked: UI.settings[key] ? true : null
    });
    input.addEventListener('change', function () {
      UI.settings[key] = input.checked;
      UI.saveSettings();
      UI.tap('move', 8);
      if (onChange) onChange(input.checked);
    });
    return el('label', { class: 'switch', for: 'set-' + key }, [
      el('span', { class: 'switch__label', text: label }),
      input,
      el('span', { class: 'switch__track' }, el('span', { class: 'switch__knob' }))
    ]);
  }

  function renderProfile(ctx) {
    var session = ctx.state.session;
    var buildLine = el('p', { class: 'muted', text: 'Sürüm bilgisi alınıyor…' });

    global.PixelBridge.getBuildInfo().then(function (res) {
      if (!res.ok) return;
      buildLine.textContent = res.applicationId + ' • v' + res.versionName +
        ' (' + res.versionCode + ') • köprü v' + res.bridgeApi +
        (global.PixelBridge.isNative ? ' • native' : ' • web demo');
    });

    return el('div', { class: 'screen' }, [
      el('div', { class: 'profile__head' }, [
        el('div', { class: 'profile__avatar' }, Art.avatarElement(session.avatarSeed, session.isAdmin, 'px-icon px-icon--lg')),
        el('div', {}, [
          el('h1', { class: 'profile__name', text: session.displayName }),
          el('p', { class: 'profile__role' }, [
            el('span', { class: 'badge badge--' + (session.isAdmin ? 'admin' : 'user'), text: session.isAdmin ? 'YÖNETİCİ' : 'KULLANICI' }),
            el('span', { class: 'muted', text: ' @' + session.username })
          ])
        ])
      ]),

      sectionTitle('Kütüphanem'),
      (function () {
        var installed = ctx.state.apps.filter(function (app) { return isInstalled(app.id); });
        if (!installed.length) return emptyState('Henüz bir şey indirmedin.');
        return el('div', { class: 'card-list' }, installed.map(function (app) {
          return appCard(app, ctx.openApp);
        }));
      })(),

      sectionTitle('Ayarlar'),
      el('div', { class: 'panel' }, [
        toggleRow('Ses efektleri', 'sound'),
        toggleRow('Titreşim', 'haptics'),
        toggleRow('Tarama çizgileri', 'scanlines')
      ]),

      sectionTitle('Hakkında'),
      el('div', { class: 'panel' }, [
        el('p', { class: 'muted', text: 'PixelStore, RPG Maker estetiğinde bir mağaza vitrini demosudur. Katalog cihazda saklanır, dış sunucuya veri gönderilmez.' }),
        buildLine
      ]),

      el('button', {
        class: 'btn btn--danger btn--wide',
        type: 'button',
        onclick: function () {
          UI.tap('cancel');
          UI.confirm('Çıkış', 'Oturumu kapatmak istiyor musun?', 'Çıkış yap').then(function (yes) {
            if (yes) ctx.logout();
          });
        }
      }, 'ÇIKIŞ YAP')
    ]);
  }

  global.PixelViews = {
    renderLogin: renderLogin,
    renderHome: renderHome,
    renderCategories: renderCategories,
    renderDetail: renderDetail,
    renderProfile: renderProfile,
    appCard: appCard,
    sectionTitle: sectionTitle,
    emptyState: emptyState,
    isInstalled: isInstalled
  };
})(window);
