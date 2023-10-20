import java.io.File;

import com.project.Converter;

public class test {
    public static void main(String[] args) {
        // VVV for char image production
        // try {
        //     File file = new File("res/master.jpg");
        //     file.setReadable(true);
        //     System.out.println(file.getPath()+ " " + file.canRead());
        //     BufferedImage master = ImageIO.read(file);
        //     int name = 32;
        //     for (int w = 0; w < master.getWidth()-1; w += 12) {
        //         if (name < 65 || name > 122) ImageIO.write(grayscale(master.getSubimage(w, 0, 12, 24)), "png", new File("noletter/" + name + ".png"));
        //         name++;
        //     }
        // } catch (Exception e) {e.printStackTrace();}
        long start = System.currentTimeMillis(); 
        String img = "";
        String vid = "";
        // try{ vid = Converter.readTxt(new File("res/Output/vid.txt"));}catch (Exception e) {e.printStackTrace();}
        File file = new File("C:/Users/graha/Downloads/sample-5.mp4");
        int tune = 64;
        try{ vid = Converter.convertVideo(file, "vid.txt", true, false, tune, 1, 5.0, 0.0, 3);} catch (Exception e) {e.printStackTrace();}
        // img = Converter.convertImage(file, "crackers.txt", true, false, tune, 1);
        // img = Converter.convertImage(file, "bw.txt", false, tune, 1);
        // try{ img = Converter.readTxt(new File("Output/crackers.txt"));} catch (FileNotFoundException e) {}
        // try{Converter.contrast(ImageIO.read(new File("C:/Users/graha/Downloads/crackers.jpg")), 128);} catch (Exception e) {e.printStackTrace();}
        long proc = System.currentTimeMillis()-start;
        start = System.currentTimeMillis();
        // System.out.println(img);
        Converter.printVid(vid, 0, false);
        System.out.print("Processing: " + (proc) + " ms Print: " + (System.currentTimeMillis()-start) + " ms");
    }
}
