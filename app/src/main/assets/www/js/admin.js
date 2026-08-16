/*
 * Admin — yönetim paneli ekranları.
 *
 * Bu dosyadaki ekranlara yalnızca yönetici oturumunda ulaşılır. Yine de her ekran kendi başına
 * rolü doğrular (savunma katmanı); asıl karar native köprüdedir: kullanıcı rolünde bu uçlar veri
 * değil hata döner, dolayısıyla arayüz kurcalansa bile yönetim verisi elde edilemez.
 */
(function (global) {
  'use strict';

  var UI = global.PixelUI;
  var Art = global.PixelArt;
  var Views = global.PixelViews;
  var el = UI.el;

  function denied() {
    return el('div', { class: 'screen' }, Views.emptyState('Bu bölüm için yönetici yetkisi gerekir.'));
  }

  function isAdmin(ctx) {
    return !!(ctx.state.session && ctx.state.session.isAdmin);
  }

  /* ---- Form alanları ----------------------------------------------------------------------- */

  function field(label, input, hint) {
    return el('div', { class: 'form-row' }, [
      el('label', { class: 'field-label', for: input.id, text: label }),
      input,
      hint ? el('span', { class: 'form-hint', text: hint }) : null
    ]);
  }

  function textInput(id, value, placeholder, type) {
    return el('input', {
      class: 'field', id: id, type: type || 'text',
      value: value === undefined || value === null ? '' : String(value),
      placeholder: placeholder || ''
    });
  }

  function selectInput(id, value, options) {
    var select = el('select', { class: 'field', id: id });
    options.forEach(function (option) {
      var node = el('option', { value: option.value, text: option.label });
      if (String(option.value) === String(value)) node.selected = true;
      select.appendChild(node);
    });
    return select;
  }

  /* ---- İstatistik paneli ------------------------------------------------------------------- */

  function statCard(value, label, tone) {
    return el('div', { class: 'admin-stat admin-stat--' + (tone || 'default') }, [
      el('strong', { class: 'admin-stat__value', text: value }),
      el('span', { class: 'admin-stat__label', text: label })
    ]);
  }

  function categoryBars(perCategory) {
    var entries = Object.keys(perCategory).map(function (key) {
      return { name: key, count: perCategory[key] };
    }).sort(function (a, b) { return b.count - a.count; });
    var max = entries.reduce(function (acc, item) { return Math.max(acc, item.count); }, 1);

    return el('div', { class: 'bars' }, entries.map(function (item) {
      return el('div', { class: 'bar-row' }, [
        el('span', { class: 'bar-row__name', text: item.name }),
        UI.segBar(item.count / max, 10, 'seg-bar--stat'),
        el('span', { class: 'bar-row__count', text: String(item.count) })
      ]);
    }));
  }

  /* ---- Yönetim ana ekranı ------------------------------------------------------------------ */

  function adminAppRow(app, ctx, onChanged) {
    var published = app.published !== false;

    return el('div', { class: 'admin-row' + (published ? '' : ' admin-row--draft') }, [
      el('div', { class: 'admin-row__icon' }, Art.iconElement(app.iconSeed || app.id, app.palette)),
      el('div', { class: 'admin-row__body' }, [
        el('strong', { class: 'admin-row__title', text: app.title }),
        el('span', { class: 'admin-row__meta', text: app.category + ' • v' + app.version + ' • ' + UI.formatCount(app.installs) + ' indirme' }),
        el('span', { class: 'admin-row__meta', text: 'id: ' + app.id })
      ]),
      el('div', { class: 'admin-row__actions' }, [
        el('button', {
          class: 'icon-btn', type: 'button', title: 'Düzenle', 'aria-label': app.title + ' kaydını düzenle',
          onclick: function () {
            UI.tap('select');
            ctx.navigate('admin-edit', { id: app.id });
          }
        }, Art.glyphElement('pencil', '#f2c14e')),
        el('button', {
          class: 'icon-btn ' + (published ? 'is-on' : ''), type: 'button',
          title: published ? 'Yayından kaldır' : 'Yayına al',
          'aria-label': app.title + (published ? ' yayından kaldır' : ' yayına al'),
          onclick: function () {
            UI.tap('move', 8);
            global.PixelBridge.adminSetPublished(app.id, !published).then(function (res) {
              if (!res.ok) {
                UI.toast(res.error, 'error');
                return;
              }
              UI.toast(app.title + (res.published ? ' yayına alındı' : ' yayından kaldırıldı'), 'success');
              onChanged();
            });
          }
        }, Art.glyphElement(published ? 'dotOn' : 'dotOff', published ? '#6be3a0' : '#9a9ac2')),
        el('button', {
          class: 'icon-btn icon-btn--danger', type: 'button', title: 'Sil',
          'aria-label': app.title + ' kaydını sil',
          onclick: function () {
            UI.tap('cancel');
            UI.confirm('Kaydı sil', '"' + app.title + '" kalıcı olarak silinsin mi?', 'Sil').then(function (yes) {
              if (!yes) return;
              global.PixelBridge.adminDeleteApp(app.id).then(function (res) {
                if (!res.ok) {
                  UI.toast(res.error, 'error');
                  return;
                }
                UI.toast('Kayıt silindi', 'success');
                onChanged();
              });
            });
          }
        }, Art.glyphElement('cross', '#ff6b8b'))
      ])
    ]);
  }

  function renderAdminHome(ctx) {
    if (!isAdmin(ctx)) return denied();

    var statsHost = el('div', { class: 'admin-stats' });
    var barsHost = el('div', {});
    var listHost = el('div', { class: 'admin-list' });

    function reload() {
      global.PixelBridge.adminGetStats().then(function (res) {
        UI.clear(statsHost);
        UI.clear(barsHost);
        if (!res.ok) {
          statsHost.appendChild(el('p', { class: 'muted', text: res.error }));
          return;
        }
        var stats = res.stats;
        UI.append(statsHost, [
          statCard(String(stats.totalApps), 'toplam kayıt', 'gold'),
          statCard(String(stats.published), 'yayında', 'mint'),
          statCard(String(stats.drafts), 'taslak', 'rose'),
          statCard(UI.formatCount(stats.totalInstalls), 'indirme', 'sky'),
          statCard(UI.formatRating(stats.averageRating), 'ort. puan', 'gold')
        ]);
        barsHost.appendChild(Views.sectionTitle('Türlere göre dağılım'));
        barsHost.appendChild(categoryBars(stats.perCategory || {}));
      });

      ctx.refreshCatalog().then(function () {
        UI.clear(listHost);
        if (!ctx.state.apps.length) {
          listHost.appendChild(Views.emptyState('Katalog boş.'));
          return;
        }
        ctx.state.apps.slice().sort(function (a, b) {
          return String(a.title).localeCompare(String(b.title), 'tr');
        }).forEach(function (app) {
          listHost.appendChild(adminAppRow(app, ctx, reload));
        });
      });
    }

    reload();

    return el('div', { class: 'screen screen--admin' }, [
      el('div', { class: 'admin-banner' }, [
        el('span', { class: 'badge badge--admin', text: 'YÖNETİCİ MODU' }),
        el('p', { class: 'muted', text: 'Bu bölüm yalnızca yönetici oturumunda yüklenir. Kullanıcı rolünde sekme oluşturulmaz ve köprü uçları veri döndürmez.' })
      ]),

      Views.sectionTitle('Özet'),
      statsHost,
      barsHost,

      Views.sectionTitle('Hızlı işlemler'),
      el('div', { class: 'admin-actions' }, [
        el('button', {
          class: 'btn btn--primary', type: 'button',
          onclick: function () { UI.tap('select'); ctx.navigate('admin-edit', {}); }
        }, '+ YENİ KAYIT'),
        el('button', {
          class: 'btn btn--ghost', type: 'button',
          onclick: function () { UI.tap('select'); ctx.navigate('admin-users'); }
        }, 'KULLANICILAR'),
        el('button', {
          class: 'btn btn--danger', type: 'button',
          onclick: function () {
            UI.tap('cancel');
            UI.confirm(
              'Katalogu sıfırla',
              'Tüm düzenlemeler silinip fabrika kataloğu geri yüklenecek. Devam edilsin mi?',
              'Sıfırla'
            ).then(function (yes) {
              if (!yes) return;
              global.PixelBridge.adminResetCatalog().then(function (res) {
                if (!res.ok) {
                  UI.toast(res.error, 'error');
                  return;
                }
                UI.toast('Katalog sıfırlandı', 'success');
                reload();
              });
            });
          }
        }, 'KATALOĞU SIFIRLA')
      ]),

      Views.sectionTitle('Katalog yönetimi'),
      listHost
    ]);
  }

  /* ---- Kullanıcı listesi -------------------------------------------------------------------- */

  function renderAdminUsers(ctx) {
    if (!isAdmin(ctx)) return denied();

    var host = el('div', { class: 'admin-list' }, el('p', { class: 'muted', text: 'Yükleniyor…' }));

    global.PixelBridge.adminListUsers().then(function (res) {
      UI.clear(host);
      if (!res.ok) {
        host.appendChild(Views.emptyState(res.error));
        return;
      }
      res.users.forEach(function (user) {
        host.appendChild(el('div', { class: 'admin-row' }, [
          el('div', { class: 'admin-row__icon' }, Art.avatarElement(user.avatarSeed, user.role === 'admin')),
          el('div', { class: 'admin-row__body' }, [
            el('strong', { class: 'admin-row__title', text: user.displayName }),
            el('span', { class: 'admin-row__meta', text: '@' + user.username + ' • katılım ' + user.joinedAt }),
            el('span', { class: 'admin-row__meta', text: user.note })
          ]),
          el('div', { class: 'admin-row__actions' },
            el('span', { class: 'badge badge--' + user.role, text: user.role === 'admin' ? 'YÖNETİCİ' : 'KULLANICI' }))
        ]));
      });
    });

    return el('div', { class: 'screen screen--admin' }, [
      Views.sectionTitle('Kullanıcılar'),
      el('p', { class: 'muted', text: 'Demo dağıtımında hesap tablosu sabittir; parolalar yalnızca özet (SHA-256) olarak saklanır ve arayüze hiç gönderilmez.' }),
      host
    ]);
  }

  /* ---- Kayıt düzenleyici -------------------------------------------------------------------- */

  var SCENE_LABELS = {
    title: 'Açılış', field: 'Ova', battle: 'Savaş',
    town: 'Kasaba', cave: 'Mağara', menu: 'Menü'
  };

  function screenshotEditor(app, redrawPreview) {
    var host = el('div', { class: 'shot-editor' });

    function paint() {
      UI.clear(host);
      if (!app.screenshots.length) {
        host.appendChild(el('p', { class: 'muted', text: 'Henüz ekran görüntüsü yok. En fazla 8 tane ekleyebilirsin.' }));
      }
      app.screenshots.forEach(function (shot, index) {
        var preview = el('div', { class: 'shot-editor__preview' }, Art.screenshotElement(shot, app.palette));

        function refresh() {
          UI.clear(preview);
          preview.appendChild(Art.screenshotElement(shot, app.palette));
        }

        var sceneSelect = selectInput('shot-scene-' + index, shot.scene, Art.sceneNames().map(function (name) {
          return { value: name, label: SCENE_LABELS[name] || name };
        }));
        sceneSelect.addEventListener('change', function () {
          shot.scene = sceneSelect.value;
          refresh();
        });

        var kindSelect = selectInput('shot-kind-' + index, shot.kind, [
          { value: 'phone', label: 'Telefon' },
          { value: 'tablet', label: 'Tablet' }
        ]);
        kindSelect.addEventListener('change', function () {
          shot.kind = kindSelect.value;
          refresh();
        });

        var caption = textInput('shot-caption-' + index, shot.caption, 'başlık');
        caption.addEventListener('input', function () { shot.caption = caption.value; });

        host.appendChild(el('div', { class: 'shot-editor__item' }, [
          preview,
          el('div', { class: 'shot-editor__controls' }, [
            sceneSelect,
            kindSelect,
            caption,
            el('div', { class: 'shot-editor__buttons' }, [
              el('button', {
                class: 'icon-btn', type: 'button', title: 'Yeni varyasyon',
                onclick: function () {
                  UI.tap('move', 8);
                  shot.seed = shot.seed.replace(/-v\d+$/, '') + '-v' + Math.floor(Math.random() * 9999);
                  refresh();
                }
              }, Art.glyphElement('star', '#f2c14e')),
              el('button', {
                class: 'icon-btn icon-btn--danger', type: 'button', title: 'Kaldır',
                onclick: function () {
                  UI.tap('cancel');
                  app.screenshots.splice(index, 1);
                  paint();
                }
              }, Art.glyphElement('cross', '#ff6b8b'))
            ])
          ])
        ]));
      });

      host.appendChild(el('button', {
        class: 'btn btn--ghost btn--wide',
        type: 'button',
        disabled: app.screenshots.length >= 8 ? true : null,
        onclick: function () {
          if (app.screenshots.length >= 8) return;
          UI.tap('select');
          app.screenshots.push({
            seed: (app.iconSeed || 'shot') + '-' + Math.floor(Math.random() * 9999),
            kind: 'phone',
            scene: 'field',
            caption: ''
          });
          paint();
        }
      }, '+ EKRAN GÖRÜNTÜSÜ EKLE'));
    }

    paint();
    return host;
  }

  function blankApp() {
    return {
      id: '',
      title: '',
      developer: '',
      category: 'RPG',
      version: '1.0.0',
      sizeMb: 25,
      rating: 4.0,
      ratingCount: 0,
      contentRating: '7+',
      published: true,
      updatedAt: new Date().toISOString().slice(0, 10),
      iconSeed: 'yeni-' + Math.floor(Math.random() * 9999),
      palette: 'emerald',
      tags: [],
      shortDescription: '',
      longDescription: '',
      downloadUrl: '',
      screenshots: []
    };
  }

  function renderAdminEditor(ctx, params) {
    if (!isAdmin(ctx)) return denied();

    var host = el('div', { class: 'screen screen--admin' }, el('p', { class: 'muted', text: 'Yükleniyor…' }));

    function build(app) {
      UI.clear(host);
      app.screenshots = app.screenshots || [];
      app.tags = app.tags || [];

      var iconPreview = el('div', { class: 'icon-preview' },
        Art.iconElement(app.iconSeed, app.palette, 'px-icon px-icon--lg'));

      function redrawIcon() {
        UI.clear(iconPreview);
        iconPreview.appendChild(Art.iconElement(app.iconSeed, app.palette, 'px-icon px-icon--lg'));
      }

      var titleInput = textInput('f-title', app.title, 'Oyun adı');
      var devInput = textInput('f-dev', app.developer, 'Stüdyo adı');
      var categoryInput = selectInput('f-category', app.category, ctx.state.categories.map(function (cat) {
        return { value: cat.name, label: cat.name };
      }));
      var versionInput = textInput('f-version', app.version, '1.0.0');
      var sizeInput = textInput('f-size', app.sizeMb, '25', 'number');
      var ratingInput = textInput('f-rating', app.rating, '4.5', 'number');
      var ratingCountInput = textInput('f-rating-count', app.ratingCount, '0', 'number');
      var contentInput = selectInput('f-content', app.contentRating, ['3+', '7+', '12+', '16+', '18+'].map(function (v) {
        return { value: v, label: v };
      }));
      var shortInput = textInput('f-short', app.shortDescription, 'Tek cümlelik tanıtım');
      var longInput = el('textarea', {
        class: 'field field--area', id: 'f-long', rows: '6',
        placeholder: 'Uzun açıklama. Paragraflar için satır atlayabilirsin.'
      });
      longInput.value = app.longDescription || '';
      var tagsInput = textInput('f-tags', (app.tags || []).join(', '), 'Sıra tabanlı, Hikâye');
      var downloadInput = textInput('f-download', app.downloadUrl, 'https://…');
      var seedInput = textInput('f-seed', app.iconSeed, 'ikon tohumu');
      seedInput.addEventListener('input', function () {
        app.iconSeed = seedInput.value;
        redrawIcon();
      });
      var paletteInput = selectInput('f-palette', app.palette, Art.paletteNames().map(function (name) {
        return { value: name, label: name };
      }));
      paletteInput.addEventListener('change', function () {
        app.palette = paletteInput.value;
        redrawIcon();
      });

      var publishedInput = el('input', {
        class: 'switch__input', type: 'checkbox', id: 'f-published',
        checked: app.published !== false ? true : null
      });

      function collect() {
        return {
          id: app.id,
          title: titleInput.value.trim(),
          developer: devInput.value.trim(),
          category: categoryInput.value,
          version: versionInput.value.trim(),
          sizeMb: parseFloat(sizeInput.value) || 1,
          rating: parseFloat(ratingInput.value) || 0,
          ratingCount: parseInt(ratingCountInput.value, 10) || 0,
          contentRating: contentInput.value,
          published: publishedInput.checked,
          updatedAt: new Date().toISOString().slice(0, 10),
          iconSeed: seedInput.value.trim() || app.id || 'yeni',
          palette: paletteInput.value,
          tags: tagsInput.value.split(',').map(function (t) { return t.trim(); }).filter(Boolean),
          shortDescription: shortInput.value.trim(),
          longDescription: longInput.value.trim(),
          downloadUrl: downloadInput.value.trim(),
          screenshots: app.screenshots
        };
      }

      function save() {
        var payload = collect();
        if (!payload.title) {
          UI.toast('Başlık boş olamaz', 'error');
          titleInput.focus();
          return;
        }
        global.PixelBridge.adminSaveApp(payload).then(function (res) {
          if (!res.ok) {
            UI.toast(res.error, 'error');
            return;
          }
          UI.toast('"' + res.app.title + '" kaydedildi', 'success');
          ctx.refreshCatalog().then(function () {
            ctx.back();
          });
        });
      }

      UI.append(host, [
        Views.sectionTitle(app.id ? 'Kaydı düzenle' : 'Yeni kayıt'),

        el('div', { class: 'panel panel--icon' }, [
          iconPreview,
          el('div', { class: 'icon-fields' }, [
            field('İKON TOHUMU', seedInput, 'Aynı tohum her zaman aynı ikonu üretir.'),
            field('PALET', paletteInput),
            el('button', {
              class: 'btn btn--ghost', type: 'button',
              onclick: function () {
                UI.tap('move', 8);
                seedInput.value = 'seed-' + Math.floor(Math.random() * 99999);
                app.iconSeed = seedInput.value;
                redrawIcon();
              }
            }, 'RASTGELE İKON')
          ])
        ]),

        el('div', { class: 'panel' }, [
          field('BAŞLIK', titleInput),
          field('GELİŞTİRİCİ', devInput),
          field('TÜR', categoryInput),
          field('SÜRÜM', versionInput),
          field('BOYUT (MB)', sizeInput),
          field('PUAN', ratingInput, '0 ile 5 arası.'),
          field('OY SAYISI', ratingCountInput),
          field('İÇERİK YAŞI', contentInput),
          el('label', { class: 'switch', for: 'f-published' }, [
            el('span', { class: 'switch__label', text: 'Mağazada yayında' }),
            publishedInput,
            el('span', { class: 'switch__track' }, el('span', { class: 'switch__knob' }))
          ]),
          el('span', { class: 'form-hint', text: 'Yayında değilse kayıt yalnızca yönetici oturumunda görünür.' })
        ]),

        el('div', { class: 'panel' }, [
          field('KISA AÇIKLAMA', shortInput),
          field('UZUN AÇIKLAMA', longInput),
          field('ETİKETLER', tagsInput, 'Virgülle ayır, en fazla 6 etiket.'),
          field('GELİŞTİRİCİ BAĞLANTISI', downloadInput, 'İsteğe bağlı. Sistem tarayıcısında açılır.')
        ]),

        Views.sectionTitle('Ekran görüntüleri'),
        screenshotEditor(app, redrawIcon),

        el('div', { class: 'admin-actions admin-actions--sticky' }, [
          el('button', { class: 'btn btn--primary', type: 'button', onclick: save }, 'KAYDET'),
          el('button', {
            class: 'btn btn--ghost', type: 'button',
            onclick: function () { UI.tap('cancel'); ctx.back(); }
          }, 'VAZGEÇ')
        ])
      ]);
    }

    if (params && params.id) {
      global.PixelBridge.getApp(params.id).then(function (res) {
        if (!res.ok) {
          UI.clear(host);
          host.appendChild(Views.emptyState(res.error));
          return;
        }
        build(JSON.parse(JSON.stringify(res.app)));
      });
    } else {
      build(blankApp());
    }

    return host;
  }

  global.PixelAdmin = {
    renderAdminHome: renderAdminHome,
    renderAdminUsers: renderAdminUsers,
    renderAdminEditor: renderAdminEditor
  };
})(window);
