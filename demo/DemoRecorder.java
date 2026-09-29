import java.applet.Applet;
import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.applet.AudioClip;
import java.io.File;
import java.io.PrintWriter;
import java.net.URL;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import javax.imageio.ImageIO;

/**
 * Plays zBalls on autopilot and saves frames to a directory, without touching
 * the real mouse or screen. The pointer is moved with synthetic mouse events
 * and drawn into each frame.
 *
 * The script: dismiss the intro, clear two levels by chasing the world ball,
 * then on the third level touch plain balls until they multiply past the limit
 * and the game is lost.
 *
 * Sounds aren't played. Each play() and stop() is written to sounds.txt with
 * its time in milliseconds, and each frame's time goes to frames.txt, so that
 * record.sh can mix the soundtrack from the original .au files.
 *
 * Usage: java -cp <applet>:<runner>:<demo> DemoRecorder <outdir> [fps] [maxSeconds]
 */
public class DemoRecorder {
  static Applet applet;
  static Point mouse = new Point(175, 420);
  static volatile boolean pointerVisible = true;

  public static void main(String[] args) throws Exception {
    File out = new File(args[0]);
    out.mkdirs();
    int fps = args.length > 1 ? Integer.parseInt(args[1]) : 15;
    int maxSeconds = args.length > 2 ? Integer.parseInt(args[2]) : 90;

    PrintWriter sounds = new PrintWriter(new File(out, "sounds.txt"));
    PrintWriter frames = new PrintWriter(new File(out, "frames.txt"));
    AppletRunner.clipFactory = url -> new LoggingClip(url, sounds);
    AppletRunner runner = new AppletRunner("zballs", 400, 500, new File("."));
    EventQueue.invokeAndWait(runner::start);
    applet = runner.getApplet();

    Thread pilot = new Thread(DemoRecorder::pilot, "pilot");
    pilot.setDaemon(true);
    pilot.start();

    long frameMs = 1000 / fps;
    long start = System.currentTimeMillis();
    int n = 0;
    while (System.currentTimeMillis() - start < maxSeconds * 1000L && !done) {
      long t0 = System.currentTimeMillis();
      BufferedImage img = capture(runner);
      String name = String.format("f%05d.png", n++);
      ImageIO.write(img, "png", new File(out, name));
      frames.println(now() + " " + name);
      long sleep = frameMs - (System.currentTimeMillis() - t0);
      if (sleep > 0) Thread.sleep(sleep);
    }
    frames.close();
    synchronized (sounds) {
      sounds.close();
    }
    System.out.println("frames: " + n);
    System.exit(0);
  }

  static volatile boolean done = false;
  static final long t0 = System.nanoTime();

  static long now() {
    return (System.nanoTime() - t0) / 1_000_000;
  }

  static class LoggingClip implements AudioClip {
    final String name;
    final PrintWriter log;
    boolean playing;

    LoggingClip(URL url, PrintWriter log) {
      this.name = new File(url.getPath()).getName();
      this.log = log;
    }

    public void play() {
      write("play");
      playing = true;
    }

    public void loop() {
      write("loop");
      playing = true;
    }

    // The game calls stop() on every tick, only the ones that cut a sound matter.
    public void stop() {
      if (playing) write("stop");
      playing = false;
    }

    void write(String what) {
      synchronized (log) {
        log.println(now() + " " + what + " " + name);
      }
    }
  }

  static BufferedImage capture(AppletRunner runner) throws Exception {
    Component status = runner.getStatusLabel();
    int w = applet.getWidth(), h = applet.getHeight(), sh = 22;
    BufferedImage img = new BufferedImage(w, h + sh, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = img.createGraphics();
    g.setColor(Color.WHITE);
    g.fillRect(0, 0, w, h + sh);
    Image buf = (Image) get(applet, "imgBuf");
    if (buf != null) g.drawImage(buf, 0, 0, null);
    for (Component c : applet.getComponents()) {
      Graphics2D cg = (Graphics2D) g.create(c.getX(), c.getY(), c.getWidth(), c.getHeight());
      drawButton(cg, c);
      cg.dispose();
    }
    g.setColor(new Color(0xEE, 0xEE, 0xEE));
    g.fillRect(0, h, w, sh);
    g.setColor(Color.DARK_GRAY);
    g.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 11));
    g.drawString(((java.awt.Label) status).getText(), 4, h + 15);
    if (pointerVisible) drawPointer(g, mouse.x, mouse.y);
    g.dispose();
    return img;
  }

  static void drawButton(Graphics2D g, Component c) {
    int w = c.getWidth(), h = c.getHeight();
    g.setColor(new Color(0xDD, 0xDD, 0xDD));
    g.fillRoundRect(0, 0, w - 1, h - 1, 6, 6);
    g.setColor(Color.GRAY);
    g.drawRoundRect(0, 0, w - 1, h - 1, 6, 6);
    g.setColor(Color.BLACK);
    g.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 12));
    String label = ((java.awt.Button) c).getLabel();
    int tw = g.getFontMetrics().stringWidth(label);
    g.drawString(label, (w - tw) / 2, h / 2 + 5);
  }

  static void drawPointer(Graphics2D g, int x, int y) {
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    Polygon p = new Polygon(
        new int[] {x, x, x + 4, x + 7, x + 9, x + 6, x + 11},
        new int[] {y, y + 16, y + 12, y + 18, y + 17, y + 11, y + 11}, 7);
    g.setColor(Color.WHITE);
    g.fillPolygon(p);
    g.setColor(Color.BLACK);
    g.drawPolygon(p);
  }

  // Autopilot ---------------------------------------------------------------

  static void pilot() {
    try {
      int levelsToWin = 2;
      int level = 0;
      while (true) {
        waitFor(() -> bool("WaitingClick"));
        sleep(level == 0 ? 3000 : 2500);
        glideTo(new Point(175, 250), 600);
        click();
        waitFor(() -> !bool("WaitingClick"));
        sleep(400);
        if (level < levelsToWin) {
          int startLevel = levelsCleared;
          chaseWorld(() -> levelsCleared > startLevel);
          level++;
        } else {
          overflow();
          waitFor(() -> bool("WaitingClick"));
          sleep(4000);
          done = true;
          return;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      done = true;
    }
  }

  static volatile int levelsCleared = 0;

  static void chaseWorld(java.util.function.BooleanSupplier until) throws Exception {
    Object levelImage = get(applet, "levelImage");
    while (!until.getAsBoolean()) {
      if (bool("WaitingClick") && get(applet, "currentImage") == levelImage) {
        levelsCleared++;
        return;
      }
      Rectangle target = findBall(true);
      if (target != null) {
        Point c = new Point(target.x + target.width / 2, target.y + target.height / 2);
        step(c, avoidList(), 7);
      }
      sleep(25);
    }
  }

  static void overflow() throws Exception {
    while (!bool("WaitingClick")) {
      Rectangle target = findBall(false);
      if (target != null) {
        Point c = new Point(target.x + target.width / 2, target.y + target.height / 2);
        step(c, new ArrayList<>(), 8);
      }
      sleep(25);
    }
  }

  // Moves the pointer one step towards target, steering around plain balls.
  static void step(Point target, List<Rectangle> avoid, int speed) {
    double dx = target.x - mouse.x, dy = target.y - mouse.y;
    for (Rectangle r : avoid) {
      double cx = r.getCenterX(), cy = r.getCenterY();
      double ax = mouse.x - cx, ay = mouse.y - cy;
      double d = Math.hypot(ax, ay);
      if (d < 45 && d > 0) {
        dx += ax / d * (45 - d) * 3;
        dy += ay / d * (45 - d) * 3;
      }
    }
    double len = Math.hypot(dx, dy);
    if (len < 1) return;
    double s = Math.min(speed, len);
    move(new Point((int) Math.round(mouse.x + dx / len * s), (int) Math.round(mouse.y + dy / len * s)));
  }

  static List<Rectangle> avoidList() throws Exception {
    List<Rectangle> list = new ArrayList<>();
    Object normal = get(applet, "normalImage");
    for (Object o : objects()) {
      if (o.getClass().getName().equals("zBall") && get(o, "Sprite") == normal) {
        list.add(new Rectangle((Rectangle) get(o, "Field")));
      }
    }
    return list;
  }

  static Rectangle findBall(boolean world) throws Exception {
    Object want = get(applet, world ? "worldImage" : "normalImage");
    Rectangle best = null;
    double bestD = Double.MAX_VALUE;
    for (Object o : objects()) {
      if (o.getClass().getName().equals("zBall") && get(o, "Sprite") == want) {
        Rectangle r = new Rectangle((Rectangle) get(o, "Field"));
        double d = Math.hypot(r.getCenterX() - mouse.x, r.getCenterY() - mouse.y);
        if (d < bestD) {
          bestD = d;
          best = r;
        }
      }
    }
    return best;
  }

  static List<Object> objects() throws Exception {
    Vector<?> v = (Vector<?>) get(applet, "InterractableObjects");
    synchronized (v) {
      return new ArrayList<Object>(v);
    }
  }

  static void glideTo(Point p, int ms) throws Exception {
    Point from = new Point(mouse);
    int steps = Math.max(1, ms / 20);
    for (int i = 1; i <= steps; i++) {
      double t = (double) i / steps;
      t = t * t * (3 - 2 * t);
      move(new Point((int) (from.x + (p.x - from.x) * t), (int) (from.y + (p.y - from.y) * t)));
      sleep(20);
    }
  }

  static void move(Point p) {
    mouse = p;
    post(MouseEvent.MOUSE_MOVED, p);
  }

  static void click() {
    post(MouseEvent.MOUSE_PRESSED, mouse);
    post(MouseEvent.MOUSE_RELEASED, mouse);
  }

  static void post(int id, Point p) {
    long now = System.currentTimeMillis();
    int button = id == MouseEvent.MOUSE_MOVED ? MouseEvent.NOBUTTON : MouseEvent.BUTTON1;
    MouseEvent e = new MouseEvent(applet, id, now, 0, p.x, p.y, 1, false, button);
    EventQueue.invokeLater(() -> applet.dispatchEvent(e));
  }

  static void waitFor(java.util.function.BooleanSupplier cond) {
    while (!cond.getAsBoolean()) sleep(50);
  }

  static void sleep(long ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  static boolean bool(String name) {
    try {
      return (Boolean) get(applet, name);
    } catch (Exception e) {
      return false;
    }
  }

  static Object get(Object o, String name) throws Exception {
    Class<?> c = o.getClass();
    while (c != null) {
      try {
        Field f = c.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(o);
      } catch (NoSuchFieldException e) {
        c = c.getSuperclass();
      }
    }
    throw new NoSuchFieldException(name);
  }
}
