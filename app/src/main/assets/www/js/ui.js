/*
 * UI — DOM yardımcıları, bildirimler, onay kutusu, ses efektleri ve kullanıcı tercihleri.
 *
 * Ses dosyası taşınmaz; efektler WebAudio ile üretilir (8-bit kare dalga). AudioContext ilk
 * kullanıcı dokunuşuna kadar oluşturulmaz, aksi halde mobil tarayıcılar başlatmayı reddeder.
 */
(function (global) {
  'use strict';

  var SETTINGS_KEY = 'pixelstore.settings.v1';
  var DEFAULTS = { sound: true, haptics: true, scanlines: true };

  var settings = (function () {
    try {
      var raw = localStorage.getItem(SETTINGS_KEY);
      if (!raw) return Object.assign({}, DEFAULTS);
      return Object.assign({}, DEFAULTS, JSON.parse(raw));
    } catch (e) {
      return Object.assign({}, DEFAULTS);
    }
  })();

  function saveSettings() {
    try {
      localStorage.setItem(SETTINGS_KEY, JSON.stringify(settings));
    } catch (e) { /* Depolama kapalıysa tercihler yalnızca bu oturumda geçerli olur. */ }
  }

  /* ---- DOM --------------------------------------------------------------------------------- */

  function el(tag, props, children) {
    var node = document.createElement(tag);
    if (props) {
      Object.keys(props).forEach(function (key) {
        var value = props[key];
        if (value === null || value === undefined || value === false) return;
        if (key === 'class') node.className = value;
        else if (key === 'text') node.textContent = value;
        else if (key === 'html') node.innerHTML = value;
        else if (key === 'dataset') Object.assign(node.dataset, value);
        else if (key.indexOf('on') === 0 && typeof value === 'function') {
          node.addEventListener(key.slice(2).toLowerCase(), value);
        } else node.setAttribute(key, value === true ? '' : value);
      });
    }
    append(node, children);
    return node;
  }

  function append(parent, children) {
    if (children === null || children === undefined || children === false) return parent;
    if (Array.isArray(children)) {
      children.forEach(function (child) { append(parent, child); });
      return parent;
    }
    if (children instanceof Node) parent.appendChild(children);
    else parent.appendChild(document.createTextNode(String(children)));
    return parent;
  }

  function clear(node) {
    while (node.firstChild) node.removeChild(node.firstChild);
    return node;
  }

  /* ---- Ses --------------------------------------------------------------------------------- */

  var audioContext = null;

  var TONES = {
    move:   { freq: 440, duration: 0.04, type: 'square', gain: 0.05 },
    select: { freq: 660, duration: 0.07, type: 'square', gain: 0.06 },
    cancel: { freq: 220, duration: 0.08, type: 'square', gain: 0.05 },
    error:  { freq: 140, duration: 0.16, type: 'sawtooth', gain: 0.06 },
    coin:   { freq: 880, duration: 0.10, type: 'square', gain: 0.06, sweepTo: 1320 },
    open:   { freq: 320, duration: 0.09, type: 'triangle', gain: 0.06, sweepTo: 640 }
  };

  function sfx(name) {
    if (!settings.sound) return;
    var tone = TONES[name] || TONES.move;
    try {
      if (!audioContext) {
        var Ctor = global.AudioContext || global.webkitAudioContext;
        if (!Ctor) return;
        audioContext = new Ctor();
      }
      if (audioContext.state === 'suspended') audioContext.resume();
      var now = audioContext.currentTime;
      var osc = audioContext.createOscillator();
      var gain = audioContext.createGain();
      osc.type = tone.type;
      osc.frequency.setValueAtTime(tone.freq, now);
      if (tone.sweepTo) osc.frequency.linearRampToValueAtTime(tone.sweepTo, now + tone.duration);
      gain.gain.setValueAtTime(tone.gain, now);
      gain.gain.exponentialRampToValueAtTime(0.0001, now + tone.duration);
      osc.connect(gain).connect(audioContext.destination);
      osc.start(now);
      osc.stop(now + tone.duration + 0.02);
    } catch (e) {
      // Ses başarısız olursa arayüz çalışmaya devam etmeli.
    }
  }

  function haptic(ms) {
    if (!settings.haptics) return;
    global.PixelBridge.haptic(ms || 12);
  }

  /** Dokunma geri bildirimi: ses + titreşim birlikte. */
  function tap(name, hapticMs) {
    sfx(name || 'select');
    haptic(hapticMs);
  }

  /* ---- Bildirim ---------------------------------------------------------------------------- */

  var toastHost = null;

  function toast(message, kind) {
    if (!toastHost) {
      toastHost = el('div', { class: 'toast-host', id: 'toast-host' });
      document.body.appendChild(toastHost);
    }
    var node = el('div', { class: 'toast toast--' + (kind || 'info') }, [
      el('span', { class: 'toast__mark', text: kind === 'error' ? '!' : kind === 'success' ? '+' : '*' }),
      el('span', { class: 'toast__text', text: message })
    ]);
    toastHost.appendChild(node);
    sfx(kind === 'error' ? 'error' : 'coin');
    global.setTimeout(function () {
      node.classList.add('is-leaving');
      global.setTimeout(function () {
        if (node.parentNode) node.parentNode.removeChild(node);
      }, 220);
    }, 2400);
  }

  /* ---- Modal ------------------------------------------------------------------------------- */

  var openModal = null;

  function modal(options) {
    closeModal();
    var body = options.body instanceof Node ? options.body : el('p', { class: 'modal__text', text: options.body || '' });

    var actions = (options.actions || []).map(function (action) {
      return el('button', {
        class: 'btn ' + (action.variant ? 'btn--' + action.variant : ''),
        type: 'button',
        onclick: function () {
          tap(action.variant === 'danger' ? 'cancel' : 'select');
          if (action.onSelect) action.onSelect();
          if (action.keepOpen !== true) closeModal();
        }
      }, action.label);
    });

    var dialog = el('div', { class: 'modal', role: 'dialog', 'aria-modal': 'true' }, [
      el('div', { class: 'modal__frame' }, [
        el('div', { class: 'modal__title', text: options.title || '' }),
        el('div', { class: 'modal__body' }, body),
        el('div', { class: 'modal__actions' }, actions)
      ])
    ]);

    var backdrop = el('div', {
      class: 'modal-backdrop',
      onclick: function (event) {
        if (event.target === backdrop && options.dismissible !== false) {
          tap('cancel');
          closeModal();
        }
      }
    }, dialog);

    document.body.appendChild(backdrop);
    openModal = { node: backdrop, onDismiss: options.onDismiss };
    sfx('open');
    return backdrop;
  }

  function closeModal() {
    if (!openModal) return false;
    var current = openModal;
    openModal = null;
    if (current.node.parentNode) current.node.parentNode.removeChild(current.node);
    if (current.onDismiss) current.onDismiss();
    return true;
  }

  function isModalOpen() {
    return !!openModal;
  }

  function confirm(title, message, confirmLabel) {
    return new Promise(function (resolve) {
      var settled = false;
      modal({
        title: title,
        body: message,
        onDismiss: function () { if (!settled) resolve(false); },
        actions: [
          { label: 'Vazgeç', variant: 'ghost', onSelect: function () { settled = true; resolve(false); } },
          { label: confirmLabel || 'Onayla', variant: 'danger', onSelect: function () { settled = true; resolve(true); } }
        ]
      });
    });
  }

  /* ---- Biçimlendirme ------------------------------------------------------------------------ */

  function formatCount(value) {
    var n = Number(value) || 0;
    if (n >= 1000000) return (Math.round(n / 100000) / 10).toString().replace('.', ',') + ' Mn';
    if (n >= 1000) return (Math.round(n / 100) / 10).toString().replace('.', ',') + ' B';
    return String(n);
  }

  function formatSize(mb) {
    var n = Number(mb) || 0;
    if (n >= 1024) return (Math.round((n / 1024) * 10) / 10).toString().replace('.', ',') + ' GB';
    return (Math.round(n * 10) / 10).toString().replace('.', ',') + ' MB';
  }

  function formatRating(value) {
    var n = Number(value) || 0;
    return (Math.round(n * 10) / 10).toString().replace('.', ',');
  }

  /** Piksel yıldız satırı: dolu / yarım / boş. */
  function stars(rating) {
    var value = Number(rating) || 0;
    var row = el('span', { class: 'stars', 'aria-label': formatRating(value) + ' / 5' });
    for (var i = 1; i <= 5; i++) {
      var state = value >= i ? 'full' : (value >= i - 0.5 ? 'half' : 'empty');
      row.appendChild(el('span', { class: 'star star--' + state, 'aria-hidden': 'true' }));
    }
    return row;
  }

  /**
   * Segmentli ilerleme çubuğu.
   *
   * Genişliği inline `style` ile ayarlamak yerine ayrık bloklar kullanılır: CSP `style-src 'self'`
   * altında inline stil yasaktır ve ayrık bloklar 8-bit çubuklara zaten daha çok benzer.
   */
  function segBar(ratio, segments, cssClass) {
    var total = segments || 10;
    var value = Number(ratio);
    if (!isFinite(value)) value = 0;
    var on = Math.max(0, Math.min(total, Math.round(value * total)));
    var bar = el('span', { class: 'seg-bar' + (cssClass ? ' ' + cssClass : '') });
    for (var i = 0; i < total; i++) {
      bar.appendChild(el('span', { class: 'seg' + (i < on ? ' is-on' : '') }));
    }
    return bar;
  }

  function applySettingsToBody() {
    document.body.classList.toggle('no-scanlines', !settings.scanlines);
  }

  global.PixelUI = {
    el: el,
    append: append,
    clear: clear,
    sfx: sfx,
    tap: tap,
    haptic: haptic,
    toast: toast,
    modal: modal,
    closeModal: closeModal,
    isModalOpen: isModalOpen,
    confirm: confirm,
    formatCount: formatCount,
    formatSize: formatSize,
    formatRating: formatRating,
    stars: stars,
    segBar: segBar,
    settings: settings,
    saveSettings: function () {
      saveSettings();
      applySettingsToBody();
    },
    applySettingsToBody: applySettingsToBody
  };
})(window);
