 
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
 
import lejos.hardware.BrickFinder;
import lejos.hardware.Button;
import lejos.hardware.ev3.EV3;
import lejos.hardware.lcd.LCD;
import lejos.hardware.video.Video;
 
public class StreamVideo {
    private static final int WIDTH = 160;
    private static final int HEIGHT = 120;
    private static final String HOST = "192.168.1.24";
    private static final int PORT = 55555;
 
    public static void main(String[] args) throws IOException  {
 
        EV3 ev3 = (EV3) BrickFinder.getLocal();
        Video video = ev3.getVideo();
        video.open(WIDTH, HEIGHT);
        byte[] frame = video.createFrame();
 
        Socket sock = new Socket(HOST, PORT);
        BufferedOutputStream bos = new BufferedOutputStream(sock.getOutputStream());
        long start = System.currentTimeMillis();
        int frames = 0;
        LCD.drawString("fps:", 0, 2);
 
        while(Button.ESCAPE.isUp()) {
            try {
                video.grabFrame(frame);
                LCD.drawString("" + (++frames * 1000f/(System.currentTimeMillis() - start)), 5,2);
 
                bos.write(frame);
                bos.flush();
            } catch (IOException e) {
                break;
            }
        }
 
        bos.close();
        sock.close();
        video.close();
    }
}