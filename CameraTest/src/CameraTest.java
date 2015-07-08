import java.io.IOException;
import lejos.hardware.video.Video;
import lejos.hardware.video.YUYVImage;
import lejos.hardware.BrickFinder;
import lejos.hardware.Button;
import lejos.hardware.lcd.GraphicsLCD;
 
 public class CameraTest {
     public static void main(String[] args) {
         try {
             Video wc = BrickFinder.getDefault().getVideo();
             wc.open(160,120);
             byte[] frame = wc.createFrame();
             YUYVImage img = new YUYVImage(frame, wc.getWidth(), wc.getHeight());
             GraphicsLCD g = BrickFinder.getDefault().getGraphicsLCD();
             int threshold = 128;
             boolean auto = true;
             while (!Button.ESCAPE.isDown()) {
                 wc.grabFrame(frame);
                 if (auto)
                     threshold = img.getMeanY();
                 img.display(g, 0, 0, threshold);
                 if (Button.UP.isDown()) {
                     threshold++;
                     if (threshold > 255)
                         threshold = 255;
                     auto = false;
                 }
                 if (Button.DOWN.isDown()) {
                     threshold--;
                     if (threshold < 0)
                         threshold = 0;
                     auto = false;
                 }
                 if (Button.ENTER.isDown()) {
                     auto = true;
                 }
             }
             wc.close();
             g.clear();
         } catch (IOException ioe) {
             ioe.printStackTrace();
             System.out.println("Driver exception: " + ioe.getMessage());
         }
     }
 }
