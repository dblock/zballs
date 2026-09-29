# Bolongas zBalls

https://github.com/user-attachments/assets/9a266a2d-5d2a-43dd-a3f1-b5a9816fc266

A Java 1.1 applet game written on January 29, 1998 by Daniel Doubrovkine and Olivier Chekroun at the University of Geneva.

**[Play it online](https://dblock.github.io/zballs/)** in your browser.

## How to Play

Click anywhere to start, then move the mouse. Don't click.

* Touch the dark "world" ball to blow it up. The next ball becomes the world ball.
* Blow up all the balls to get to the next level. Each level doubles the number of balls.
* Touching a plain ball spawns another one. Too many balls and you lose.
* Each level has a time limit, shown in the status bar along with the number of balls you have left.

The police-line bars are obstacles. **Restart !** starts over, and **Sound !** toggles sound.

## Running

Browsers no longer run Java applets, and the JDK no longer ships `appletviewer`. [runner/AppletRunner.java](runner/AppletRunner.java) is a small applet host that runs the game in a regular window.

```sh
./run.sh             # compile the 1998 sources with a modern javac and run them
./run.sh --original  # run the 1998 .class files as shipped
```

[build.sh](build.sh) copies the sources to `build/src` and fixes them before compiling.

* The sources have `import zInterract;`, a single-name import from the unnamed package. That has been illegal since Java 1.4, so these lines are removed.
* [patches/01-restart-without-thread-stop.patch](patches/01-restart-without-thread-stop.patch): **Restart !** killed the game thread with `Thread.stop()`, which throws since JDK 20 and doesn't work in CheerpJ. The game thread now checks whether it was replaced and exits.
* [patches/02-buttons-action-listeners.patch](patches/02-buttons-action-listeners.patch): **Restart !** and **Sound !** listened for mouse presses, which CheerpJ doesn't send to buttons. They now listen for button actions.

The patched build needs JDK 8 to 25, because JDK 26 removed the Applet API. The original `.class` files need JDK 8 to 19 for **Restart !** to work.

## Playing Online

[CheerpJ](https://cheerpj.com) runs Java applets in the browser. [web/index.html](web/index.html) embeds the game with a `<cheerpj-applet>` tag, and [publish.sh](publish.sh) builds it and copies it with the images and sounds to the `gh-pages` branch.

Phones need some help, in [web/touch.js](web/touch.js). The game is played by hovering the mouse, so touches become mouse moves, and taps on the buttons become clicks. It also stops CheerpJ from bringing up the keyboard and turns on sound after the first tap.

```sh
./publish.sh --push
```

## Recording the Demo

[demo/record.sh](demo/record.sh) runs [demo/DemoRecorder.java](demo/DemoRecorder.java), which plays the game on autopilot and saves each frame to disk. It dismisses the intro, chases the world ball through two levels, and then touches plain balls until time runs out. The mouse moves through synthetic events, and the pointer is drawn into each frame.

Sounds aren't played during the recording. The recorder logs the time of every `play()` and `stop()`, and [demo/mix.py](demo/mix.py) mixes the soundtrack from the original `.au` files. The script then encodes [zballs.mp4](zballs.mp4) with ffmpeg.

```sh
./demo/record.sh
```

## License

zBalls was written in 1998 by Daniel Doubrovkine and Olivier Chekroun at the University of Geneva. It is released, along with its build and demo scripts, under the [MIT License](LICENSE).
