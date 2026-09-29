// zBalls is played by hovering the mouse, which a finger can't do. This turns
// touches on the game into mouse events that CheerpJ passes to the applet: a
// tap is a mouse move and a click, and sliding a finger moves the mouse. When
// the finger lifts, the mouse moves to the top-left corner, where no ball can
// go, so that it doesn't keep touching a ball.
(function () {
  function game(target) {
    return target instanceof Element && target.closest('.game');
  }

  function canvas() {
    return document.querySelector('.game canvas');
  }

  function send(type, target, x, y, buttons) {
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
    // Let the Restart ! and Sound ! buttons handle their own taps.
    if (e.target.tagName === 'INPUT') return;
    e.preventDefault();
    e.stopImmediatePropagation();
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
  noKeyboard(document);
})();
