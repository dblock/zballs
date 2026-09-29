// zBalls is played by hovering the mouse, which a finger can't do. This turns
// touches on the game into mouse events that CheerpJ passes to the applet: a
// tap is a mouse move and a click, and sliding a finger moves the mouse. When
// the finger lifts, the mouse moves to the top-left corner, where no ball can
// go, so that it doesn't keep touching a ball.
//
// CheerpJ ignores touches on AWT buttons too, so taps on Restart ! and Sound !
// are turned into mouse presses. Phones only allow sound after a tap, so every
// tap resumes the audio that CheerpJ creates. This script must load before
// CheerpJ to see its audio contexts.
(function () {
  var contexts = [];
  ['AudioContext', 'webkitAudioContext'].forEach(function (name) {
    var Original = window[name];
    if (!Original) return;
    var Wrapped = function () {
      var ctx = new (Function.prototype.bind.apply(Original, [null].concat([].slice.call(arguments))))();
      contexts.push(ctx);
      return ctx;
    };
    Wrapped.prototype = Original.prototype;
    window[name] = Wrapped;
  });

  // Play sound even when an iPhone's ring/silent switch is set to silent.
  if (navigator.audioSession) navigator.audioSession.type = 'playback';

  function unlockAudio() {
    contexts.forEach(function (ctx) {
      if (ctx.state !== 'running') ctx.resume();
    });
  }
  ['touchend', 'click'].forEach(function (type) {
    window.addEventListener(type, unlockAudio, true);
  });

  function game(target) {
    return target instanceof Element && target.closest('.game');
  }

  function canvas() {
    return document.querySelector('.game canvas');
  }

  function send(type, target, x, y, buttons) {
    if (!target) return;
    target.dispatchEvent(new PointerEvent(type, {
      bubbles: true,
      cancelable: true,
      composed: true,
      pointerId: 1,
      pointerType: 'mouse',
      isPrimary: true,
      clientX: x,
      clientY: y,
      button: type === 'pointermove' ? -1 : 0,
      buttons: buttons
    }));
  }

  function onPointer(e) {
    if (e.pointerType === 'mouse' || !game(e.target)) return;
    e.preventDefault();
    e.stopImmediatePropagation();
    if (e.target.tagName === 'INPUT') {
      if (e.type === 'pointerdown') {
        send('pointermove', e.target, e.clientX, e.clientY, 0);
        send('pointerdown', e.target, e.clientX, e.clientY, 1);
      } else if (e.type === 'pointerup') {
        send('pointerup', e.target, e.clientX, e.clientY, 0);
      }
      return;
    }
    var target = canvas() || e.target;
    if (e.type === 'pointerdown') {
      send('pointermove', target, e.clientX, e.clientY, 0);
      send('pointerdown', target, e.clientX, e.clientY, 1);
      send('pointerup', target, e.clientX, e.clientY, 0);
    } else if (e.type === 'pointermove') {
      send('pointermove', target, e.clientX, e.clientY, 0);
    } else {
      var r = target.getBoundingClientRect();
      send('pointermove', target, r.left + 1, r.top + 1, 0);
    }
  }

  ['pointerdown', 'pointermove', 'pointerup', 'pointercancel'].forEach(function (type) {
    window.addEventListener(type, onPointer, { capture: true, passive: false });
  });

  // CheerpJ focuses a hidden textarea for keyboard input, which pops up the
  // on-screen keyboard. The game doesn't use the keyboard.
  function noKeyboard(root) {
    root.querySelectorAll('textarea').forEach(function (t) {
      t.setAttribute('inputmode', 'none');
    });
  }
  new MutationObserver(function () { noKeyboard(document); })
    .observe(document.documentElement, { childList: true, subtree: true });
})();
