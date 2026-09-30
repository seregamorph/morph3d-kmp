import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders the application icon: a rotating dotted torus with fading trails, its colors
 * cross-fading between the cyan and purple color sets of the demo.
 * <p>
 * Run from the project root: {@code ./icon/generate.sh}
 * <ul>
 *     <li>{@code full.png} - full bleed opaque square (iOS masks the corners itself)</li>
 *     <li>{@code macos.png} - rounded square with the standard macOS margin and shadow</li>
 * </ul>
 */
public class IconGenerator {

    private static final int SIZE = 1024;

    // nearest shades of the cyan and purple color sets (Palettes.kt), VGA 6-bit RGB
    private static final float[] CYAN = {30 / 63f, 60 / 63f, 60 / 63f};
    private static final float[] PURPLE = {60 / 63f, 35 / 63f, 60 / 63f};

    private record Dot(double x, double y, double z, double u, int k) {
    }

    public static void main(String[] args) throws Exception {
        File outDir = new File(args.length > 0 ? args[0] : ".");
        outDir.mkdirs();

        BufferedImage full = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = full.createGraphics();
        setup(g);
        drawScene(g, 0, 0, SIZE);
        g.dispose();
        ImageIO.write(full, "png", new File(outDir, "full.png"));

        // macOS Big Sur grid: 824x824 rounded square centered in 1024x1024, corner radius ~185
        BufferedImage mac = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        g = mac.createGraphics();
        setup(g);
        int inset = 100;
        int body = SIZE - 2 * inset;
        // soft drop shadow
        for (int i = 16; i > 0; i--) {
            g.setColor(new Color(0, 0, 0, 4));
            g.fill(new RoundRectangle2D.Double(inset - i, inset + 12 - i, body + 2 * i, body + 2 * i,
                    370 + 2 * i, 370 + 2 * i));
        }
        BufferedImage content = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D cg = content.createGraphics();
        setup(cg);
        cg.fill(new RoundRectangle2D.Double(inset, inset, body, body, 370, 370));
        BufferedImage scene = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D sg = scene.createGraphics();
        setup(sg);
        drawScene(sg, inset, inset, body);
        sg.dispose();
        cg.setComposite(AlphaComposite.SrcIn);
        cg.drawImage(scene, 0, 0, null);
        cg.dispose();
        g.drawImage(content, 0, 0, null);
        g.dispose();
        ImageIO.write(mac, "png", new File(outDir, "macos.png"));
    }

    private static void setup(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    private static void drawScene(Graphics2D g, int ox, int oy, int size) {
        // dark "room" with a faint violet glow in the middle
        g.setPaint(new RadialGradientPaint(new Point2D.Double(ox + size * 0.5, oy + size * 0.45), size * 0.75f,
                new float[]{0f, 0.55f, 1f},
                new Color[]{new Color(0x1C1433), new Color(0x0A0814), new Color(0x000000)},
                MultipleGradientPaint.CycleMethod.NO_CYCLE));
        g.fillRect(ox, oy, size, size);

        double scale = size / 1024.0;
        double cx = ox + size * 0.5;
        double cy = oy + size * 0.5;

        // torus: ring radius 1, tube radius 0.42
        int ringSteps = 26;
        int tubeSteps = 8;
        double ringR = 1.0;
        double tubeR = 0.36;
        double tiltX = Math.toRadians(52);
        double tiltZ = Math.toRadians(-24);
        double camera = 4.2;
        double focal = 1150 * scale;

        List<Dot> dots = new ArrayList<>();
        int trail = 9;
        for (int i = 0; i < ringSteps; i++) {
            for (int j = 0; j < tubeSteps; j++) {
                // dots spiral along the surface, like the flowing dots of the demo
                double u0 = 2 * Math.PI * (i + j * 0.5 / tubeSteps) / ringSteps;
                double v = 2 * Math.PI * j / tubeSteps;
                for (int k = trail; k >= 0; k--) {
                    double u = u0 - k * 0.018;
                    double vv = v - k * 0.025;
                    double x = (ringR + tubeR * Math.cos(vv)) * Math.cos(u);
                    double y = (ringR + tubeR * Math.cos(vv)) * Math.sin(u);
                    double z = tubeR * Math.sin(vv);
                    // rotate around X, then Z
                    double y1 = y * Math.cos(tiltX) - z * Math.sin(tiltX);
                    double z1 = y * Math.sin(tiltX) + z * Math.cos(tiltX);
                    double x2 = x * Math.cos(tiltZ) - y1 * Math.sin(tiltZ);
                    double y2 = x * Math.sin(tiltZ) + y1 * Math.cos(tiltZ);
                    dots.add(new Dot(x2, y2, z1, u0, k));
                }
            }
        }
        // painter's algorithm: far dots first
        dots.sort((a, b) -> Double.compare(b.z, a.z));

        for (Dot d : dots) {
            double z = d.z;
            int k = d.k;
            double persp = focal / (camera + z);
            double px = cx + d.x * persp;
            double py = cy - d.y * persp;
            // depth 0 (near) .. 1 (far)
            double depth = Math.max(0, Math.min(1, (z + 1.3) / 2.6));
            double shade = 1.0 - 0.55 * depth;
            double fade = 1.0 - k / (trail + 1.0);
            double t = 0.5 + 0.5 * Math.sin(d.u + 0.9);
            float r = (float) ((CYAN[0] + (PURPLE[0] - CYAN[0]) * t) * shade);
            float gr = (float) ((CYAN[1] + (PURPLE[1] - CYAN[1]) * t) * shade);
            float b = (float) ((CYAN[2] + (PURPLE[2] - CYAN[2]) * t) * shade);
            double radius = (13 - 6 * depth) * scale * (k == 0 ? 1.0 : 0.3 + 0.45 * fade);
            float alpha = (float) (k == 0 ? 1.0 : 0.75 * fade * fade);

            if (k == 0) {
                // soft glow around the head of the trail
                double gr2 = radius * 3.2;
                g.setPaint(new RadialGradientPaint(new Point2D.Double(px, py), (float) gr2,
                        new float[]{0f, 1f},
                        new Color[]{new Color(r, gr, b, 0.22f * (float) shade), new Color(r, gr, b, 0f)}));
                g.fill(new Ellipse2D.Double(px - gr2, py - gr2, gr2 * 2, gr2 * 2));
            }
            g.setPaint(new Color(clamp(r), clamp(gr), clamp(b), alpha));
            g.fill(new Ellipse2D.Double(px - radius, py - radius, radius * 2, radius * 2));
        }
    }

    private static float clamp(float c) {
        return Math.max(0f, Math.min(1f, c));
    }
}
