import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;

import javax.imageio.*;

import com.asciigen.Converter;

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
        try{ vid = Converter.convertVideo(file, "vid.txt", true, true, tune, 1.0, 15, 0.0, 5);} catch (Exception e) {e.printStackTrace();}
        // try{vid = Converter.readTxt("Output/vid.txt");} catch (Exception e) {e.printStackTrace();}
        // vid = Converter.optimizeVid(vid);
        // try{
        //     FileOutputStream thing = new FileOutputStream(new File("src/res/Output/op2.txt"), false);
        //     thing.write(vid.getBytes());
        //     thing.close();
        // } catch (Exception e) {e.printStackTrace();}

        // img = Converter.convertImage(file, "crackers.txt", true, false, tune, 1.0);
        // String imgo = "";
        // try {imgo = Converter.optimize(new File("src/res/Output/crackers.txt"), false);} catch (Exception e) {e.printStackTrace();}
        // img = Converter.convertImage(file, "bw.txt", false, tune, 1);
        // try{ img = Converter.readTxt(new File("Output/crackers.txt"));} catch (FileNotFoundException e) {}
        // try{Converter.contrast(ImageIO.read(new File("C:/Users/graha/Downloads/crackers.jpg")), 128);} catch (Exception e) {e.printStackTrace();}
        // The line `// System.out.println(img);` is commented out, which means it is not being
        // executed. It is simply a comment that is used to provide information or explanations about
        // the code. In this case, it seems like it was used to print the `img` variable, but it has
        // been commented out and is not being used in the code.
        // System.out.println(img);
        // System.out.println(imgo);
        
        long proc = System.currentTimeMillis()-start;
        long printing = System.currentTimeMillis();
        long avg = 0;
        for (int i = 1; i <= 1; i ++) {
            start = System.currentTimeMillis();
            try {Converter.printVid(vid, 0, false);} catch (Exception e) {e.printStackTrace();}
            avg += ((System.currentTimeMillis()-start));
        }
        System.out.print("Processing: " + proc + " ms Printing: " + (System.currentTimeMillis()-printing) + " ms PrintAvg: " + ((avg)-5000)/75 + " ms");
        // for (int i = 0; i < 30; i++) {
        //     System.out.print((int)vid.charAt(i) + " ");
        // }
        // System.out.print("Processing: " + (proc) + " ms Print: " + (System.currentTimeMillis()-start) + " ms");
    }
    public static void main2(String[] args) {
        try{
            BufferedImage img = ImageIO.read(new File("src/res/Output/edge.jpg"));
            BufferedImage scl = Converter.scale(img,.69);
            // System.out.print(Converter.compare(img, scl));
            ImageIO.write(scl, "jpg", new File("src/res/Output/scale.jpg"));
        } catch (Exception e) {e.printStackTrace();}
    }
    public static void main3(String[] args) {
        String img = Converter.convertImage(new File("src/res/Output/cracker.jpg"), null, true, true, 32, 100);
        System.out.println("asldkjf;lkj\nasdjf;lkj" + img);
    }
}
