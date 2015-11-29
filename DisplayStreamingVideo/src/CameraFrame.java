import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.net.ServerSocket;
import java.net.Socket;
 
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
 
public class CameraFrame {
    private static final int WIDTH = 160;
    private static final int HEIGHT = 120;
    private static final int NUM_PIXELS = WIDTH * HEIGHT;
    private static final int BUFFER_SIZE = NUM_PIXELS * 2;
    private static final int PORT = 55555;
 
    private ServerSocket ss;
    private Socket sock;
    private byte[] buffer = new byte[BUFFER_SIZE];
    private BufferedInputStream bis;
    private BufferedImage image;
    private CameraPanel panel = new CameraPanel();
    private JFrame frame;
 
    public CameraFrame() {  
        try {
            ss = new ServerSocket(PORT);
            sock = ss.accept();
            bis = new BufferedInputStream(sock.getInputStream());
        } catch (Exception e) {
            System.err.println("Failed to connect: " + e);
            System.exit(1);
        }
 
        image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
    }
 
    public void createAndShowGUI() {
        frame = new JFrame("EV3 Camera View");
 
        frame.getContentPane().add(panel);
        frame.setPreferredSize(new Dimension(WIDTH, HEIGHT));
 
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                close();
            }
        });
 
        frame.pack();
        frame.setVisible(true);
    }
 
    public void close() {
        try {
            if (bis != null) bis.close();
            if (sock != null) sock.close();
            if (ss != null) ss.close();
        } catch (Exception e1) {
            System.err.println("Exception closing window: " + e1);
        }
    }
 
    private int convertYUVtoARGB(int y, int u, int v) {
        int c = y - 16;
        int d = u - 128;
        int e = v - 128;
        int r = (298*c+409*e+128)/256;
        int g = (298*c-100*d-208*e+128)/256;
        int b = (298*c+516*d+128)/256;
        r = r>255? 255 : r<0 ? 0 : r;
        g = g>255? 255 : g<0 ? 0 : g;
        b = b>255? 255 : b<0 ? 0 : b;
        return 0xff000000 | (r<<16) | (g<<8) | b;
    }
 
    public void run() {
        while(true) {
            synchronized (this) {
                try {
                    int offset = 0;
                    while (offset < BUFFER_SIZE) {
                        offset += bis.read(buffer, offset, BUFFER_SIZE - offset);
                    }
                    for(int i=0;i<BUFFER_SIZE;i+=4) {
                        int y1 = buffer[i] & 0xFF;
                        int y2 = buffer[i+2] & 0xFF;
                        int u = buffer[i+1] & 0xFF;
                        int v = buffer[i+3] & 0xFF;
                        int rgb1 = convertYUVtoARGB(y1,u,v);
                        int rgb2 = convertYUVtoARGB(y2,u,v);
                        image.setRGB((i % (WIDTH * 2)) / 2, i / (WIDTH * 2), rgb1);
                        image.setRGB((i % (WIDTH * 2)) / 2 + 1, i / (WIDTH * 2), rgb2);
                    }
                } catch (Exception e) {
                    break;
                }
            }
            panel.repaint(1);
        }
    }
 
    class CameraPanel extends JPanel {
        private static final long serialVersionUID = 1L;
 
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            // Ensure that we don't paint while the image is being refreshed
            synchronized(CameraFrame.this) {
                g.drawImage(image, 0, 0, null);
            }
        }   
    }
 
    public static void main(String[] args) {    
        final CameraFrame cameraFrame = new CameraFrame();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                cameraFrame.createAndShowGUI(); 
            }
        });
        cameraFrame.run();
    }
}