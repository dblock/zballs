import java.applet.Applet;
import java.applet.AppletContext;
import java.applet.AppletStub;
import java.applet.AudioClip;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Label;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;

/**
 * Hosts a 1990s AWT applet in a plain Frame, standing in for a browser or
 * appletviewer. Provides the document base, images, audio clips and the
 * status bar that the applet expects.
 *
 * Usage: java -cp <applet classes>:<runner classes> AppletRunner <class> <width> <height> [dir]
 */
public class AppletRunner implements AppletStub, AppletContext {
  private final Applet applet;
  private final Frame frame;
  private final Label status = new Label(" ");
  private final URL base;
  private final Map<URL, AudioClip> clips = new HashMap<>();

  /** Overrides how audio clips are created, e.g. to record when they play. */
  static Function<URL, AudioClip> clipFactory;

  AppletRunner(String className, int width, int height, File dir) throws Exception {
    base = dir.getCanonicalFile().toURI().toURL();
    applet = (Applet) Class.forName(className).getDeclaredConstructor().newInstance();
    applet.setPreferredSize(new Dimension(width, height));
    applet.setSize(width, height);

    frame = new Frame(className);
    frame.setLayout(new BorderLayout());
    frame.add(applet, BorderLayout.CENTER);
    frame.add(status, BorderLayout.SOUTH);
    frame.setResizable(false);
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        applet.stop();
        applet.destroy();
        System.exit(0);
      }
    });
    applet.setStub(this);
    frame.pack();
    frame.setLocationRelativeTo(null);
  }

  void start() {
    applet.init();
    frame.setVisible(true);
    applet.start();
  }

  Applet getApplet() {
    return applet;
  }

  Frame getFrame() {
    return frame;
  }

  Label getStatusLabel() {
    return status;
  }

  // AppletStub
  public boolean isActive() { return true; }
  public URL getDocumentBase() { return base; }
  public URL getCodeBase() { return base; }
  public String getParameter(String name) { return System.getProperty("applet." + name); }
  public AppletContext getAppletContext() { return this; }
  public void appletResize(int width, int height) {
    applet.setPreferredSize(new Dimension(width, height));
    if (frame != null) frame.pack();
  }

  // AppletContext
  public synchronized AudioClip getAudioClip(URL url) {
    return clips.computeIfAbsent(url, u -> {
      if (clipFactory != null) return clipFactory.apply(u);
      return Boolean.getBoolean("mute") ? new SilentClip() : Applet.newAudioClip(u);
    });
  }
  public Image getImage(URL url) { return Toolkit.getDefaultToolkit().getImage(url); }
  public Applet getApplet(String name) { return null; }
  public Enumeration<Applet> getApplets() { return Collections.enumeration(Collections.singletonList(applet)); }
  public void showDocument(URL url) { }
  public void showDocument(URL url, String target) { }
  public void showStatus(String text) { status.setText(text); }
  public void setStream(String key, InputStream stream) { }
  public InputStream getStream(String key) { return null; }
  public Iterator<String> getStreamKeys() { return Collections.<String>emptyList().iterator(); }

  static class SilentClip implements AudioClip {
    public void play() { }
    public void loop() { }
    public void stop() { }
  }

  public static void main(String[] args) throws Exception {
    if (args.length < 3) {
      System.err.println("usage: AppletRunner <class> <width> <height> [dir]");
      System.exit(1);
    }
    File dir = new File(args.length > 3 ? args[3] : ".");
    AppletRunner runner = new AppletRunner(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]), dir);
    java.awt.EventQueue.invokeAndWait(runner::start);
  }
}
