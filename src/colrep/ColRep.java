package colrep;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;

/**
 *
 * @author KathreinRobert
 */
public class ColRep implements Runnable {

    private static ExecutorService tpool;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        File folder = new File(args[0]);
        int threads = Runtime.getRuntime().availableProcessors();
        tpool = Executors.newFixedThreadPool(threads);

        System.out.println("Start processing dir: " + folder.getAbsolutePath() + " with " + threads + " threads");

        for (final File fileEntry : folder.listFiles()) {
            if (!fileEntry.isDirectory()) {
                tpool.submit(new ColRep(fileEntry));
            }
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.out.print("Shutting down... ");
                tpool.shutdown();
                System.out.println("done");
            }
        });
    }

    private final File file;

    private static final AtomicInteger counter = new AtomicInteger(0);
    private static final AtomicInteger total = new AtomicInteger(0);
    
    public static final int BUILDING_COLOR_RED = -33024;
    public static final int BUILDING_COLOR_BLUE = -16426314;
    public static final int BUILDING_COLOR_ORANGE = -5885125;

    public static final int REPLACE_BUILDING_COLOR = BUILDING_COLOR_BLUE;
    public static final int REPLACE_OTHER_COLOR = Color.WHITE.getRGB();

    public ColRep(File file) {
        this.file = file;
        counter.incrementAndGet();
        total.incrementAndGet();
    }

    @Override
    public void run() {

        System.out.println(Thread.currentThread().getId() + ": Start processing file " + file.getName());
        
        try {
            BufferedImage img = ColRep.readFileToImage(this.file);
            for (int x = 0; x < img.getHeight(); x++) {
                for (int y = 0; y < img.getWidth(); y++) {
                    switch (img.getRGB(x, y)) {
                        case -33024:
                        case -16426314:
                        case -5885125:
                            img.setRGB(x, y, REPLACE_BUILDING_COLOR);
                            continue;
                        default:
                            img.setRGB(x, y, REPLACE_OTHER_COLOR);
                    }
                }
            }
            img = ColRep.removeAlpha(img);
            ImageIO.write(img, this.getExtension(), this.file);
        } catch (Throwable ex) {
            System.err.println("Error while processing file: " + this.file.getAbsolutePath() + "\r\n" + ex.getMessage());
//            ex.printStackTrace();
            if(ex instanceof OutOfMemoryError){
                System.err.println("Out of memmory. Try start with  -Xms2G -Xmx2G");
                System.exit(-1);
            }

        }finally{
            if(counter.decrementAndGet() == 0){
                tpool.shutdown();
            }
            System.out.println( (total.get() - counter.get()) + " of " + total.get() + " Done. Thread-id: " + Thread.currentThread().getId() );
            
        }
    }

    private String getExtension() {
        String name = this.file.getName();
        try {
            return name.substring(name.lastIndexOf(".") + 1);
        } catch (Exception e) {
            return "";
        }
    }
    private static synchronized BufferedImage readFileToImage(File imagefile) throws IOException{
        return ImageIO.read(imagefile);
    }

    public static BufferedImage removeAlpha(BufferedImage img) {
        Color fillColor = Color.BLUE; // just to verify
        BufferedImage bi2 = new BufferedImage(img.getWidth(), img.getHeight(),
                BufferedImage.TYPE_3BYTE_BGR);
        bi2.getGraphics().drawImage(img, 0, 0, fillColor, null);
        // you can do a more complex saving to tune the compression  parameters
        return bi2;
    }
}
