package com.project;
import java.io.*;
import java.awt.image.*;
import javax.imageio.*;
import java.util.*;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

public class Converter {
    public static String convertImage(BufferedImage raw, String output, boolean color, boolean background, int tune, int scale) {
        // String type = input.getName().substring(input.getName().indexOf("."));
        // if (type != ".jpg" || type != ".png") return "";
        try {
            File[] chars = new File("res/chars").listFiles();
            String str = "\u001b[39;49m";
            if (scale != 1) raw = scale(raw, scale);
            BufferedImage img = contrast(gaussFilter(grayscale(edgeDetection(raw, true, tune))), 255, 32);
            for (int h = 0; h < img.getHeight()-24; h += 24){
                for (int w = 0; w < img.getWidth()-12; w += 12) {
                    BufferedImage subimg = img.getSubimage(w, h, 12, 24);
                    int chr = 0;
                    int leastDif = Integer.MAX_VALUE;
                    for (File file : chars) {
                        if (file.getName() != "32" && !background) {
                            int dif = compare(subimg, ImageIO.read(file));
                            if (dif < leastDif) {
                                leastDif = dif;
                                chr = Integer.parseInt(file.getName().substring(0,file.getName().indexOf(".")));
                            }
                        }
                    }
                    if (color) str += colorCode(getColorAvg(raw.getSubimage(w, h, 12, 24)),!background);
                    str += String.valueOf((char)chr);
                }
                if(color) str += "\u001b[39;49m";
                str += "\n";
            }
            str += "\u001b[39;49m";
            if (output != null) {
                FileOutputStream thing = new FileOutputStream(new File("res/Output/" + output));
                thing.write(str.getBytes());
                thing.close();
            }
            return str;
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String convertImage(File file, String output, boolean color, boolean background, int tune, int scale) {
        try {
            BufferedImage raw = ImageIO.read(file);
            return convertImage(raw, output, color, background, tune, scale);
        } catch (Exception e) {e.printStackTrace(); return "";}
    }

    public static String convertVideo(File input, String output, boolean color, boolean background, int tune, int scale, double frate, double start, double end) {
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(input.getAbsoluteFile());
        Java2DFrameConverter conv = new Java2DFrameConverter();
        String out = "";
        try {
            grabber.start();
            double rate = (grabber.getFrameRate()/frate);
            grabber.setVideoFrameNumber((int)(grabber.getFrameRate()*start));
            int len = (int)((end-start)*grabber.getFrameRate());
            System.out.println(rate);
            out += "<meta>" + frate + "</meta>";
            for (double i = 0; i < len; i += rate) {
                System.out.println(i/rate);
                grabber.setVideoFrameNumber((int)i);
                BufferedImage img = conv.convert(grabber.grab());
                if (img != null) out += "\u001b[H\u001b[2J" + convertImage(img, null, color, background, tune, scale) + "\r";
            }
            grabber.stop();
        } catch (Exception e) {e.printStackTrace();}
        try {
            if (output != null) {
                FileOutputStream thing = new FileOutputStream(new File("res/Output/" + output));
                thing.write(out.getBytes());
                thing.close();
            }
        } catch (Exception e) {e.printStackTrace();}
        conv.close();
        return out;
    }

    public static BufferedImage combine(BufferedImage a, BufferedImage b){
        for (int h = 0; h < a.getHeight(); h++) {
            for (int w = 0; w < a.getWidth(); w++) {
                int avg = (pixelAvg(a.getRGB(w, h), true) + pixelAvg(b.getRGB(w, h), true))&0xff;
                a.setRGB(w, h, ((avg)|(avg<<8)|(avg<<16)));
            }
        }
        try {
            System.out.println("c:" + ImageIO.write(a, "jpg", new File("res/Output/comb.jpg")));
        } catch (Exception e) {e.printStackTrace();}
        return a;
    }

    public static BufferedImage edgeDetection(BufferedImage input, boolean gauss, int thresh) {
        double[][][] kernels = {
            {
                {1,2,1},
                {0,0,0},
                {-1,-2,-1}
            },{
                {1,0,-1},
                {2,0,-2},
                {1,0,-1}
            }};
        // BufferedImage g = null;
        // try {
        //     System.out.println(ImageIO.write(g, "jpeg", new File("Output/gauss.jpg")));
        // } catch (IOException e) {e.printStackTrace();}
        // BufferedImage out = convolution(input, kernels[0], 0);
        BufferedImage out;
        if (gauss) out = convolution(gaussFilter(input), kernels, thresh, true);
        else out = convolution((input), kernels, thresh, true);
        try {
            System.out.println("e:" + ImageIO.write(out, "jpeg", new File("res/Output/edge.jpg")));
        } catch (IOException e) {e.printStackTrace();}
        return out;
    }

    public static BufferedImage gaussFilter(BufferedImage input) {
        double[][] gauss = {
            {2/159.0,4/159.0,5/159.0,4/159.0,2/159.0},
            {4/159.0,9/159.0,12/159.0,9/159.0,4/159.0},
            {5/159.0,12/159.0,15/159.0,12/159.0,5/159.0},
            {4/159.0,9/159.0,12/159.0,9/159.0,4/159.0},
            {2/159.0,4/159.0,5/159.0,4/159.0,2/159.0}};
        BufferedImage g = convolution(input, gauss, 0);
        try {
            System.out.println("g:" + ImageIO.write(g, "jpeg", new File("res/Output/gauss.jpg")));
        } catch (IOException e) {e.printStackTrace();}
        return g;
    }

    public static BufferedImage convolution(BufferedImage input, double[][][] kernel, int thresh, boolean geom) {
        int width = input.getWidth()-kernel[0][0].length+1;
        int height = input.getHeight()-kernel[0].length+1;
        BufferedImage out = new BufferedImage(width, height, input.getType());
        boolean alpha = out.getType() == BufferedImage.TYPE_INT_ARGB;
        for (int h = 0; h < height; h++) {
            for (int w = 0; w < width; w++) {
                double[] rgb = {0,0,0,0};
                for (double[][] k : kernel) {
                    double[] temp = convolve(input, k, w, h);
                    for (int i = 0; i < 4; i++) {
                        if (geom) rgb[i] = Math.sqrt(rgb[i]*rgb[i]+temp[i]*temp[i]);
                        else rgb[i] += (temp[i])/kernel.length;
                    }
                }
                int rgbInt = ((int)rgb[0]&0xff) | (((int)rgb[1]&0xff)<<8) | (((int)rgb[2]&0xff)<<16);
                if (alpha) rgbInt = rgbInt | ((((int)rgb[3])&0xff)<<24);
                if (pixelAvg(rgbInt, true) > thresh) out.setRGB(w, h, rgbInt);
            }
        }
        return out;
    }

    public static BufferedImage convolution(BufferedImage input, double[][] kernel, int thresh) {
        int width = input.getWidth()-kernel[0].length+1;
        int height = input.getHeight()-kernel.length+1;
        BufferedImage out = new BufferedImage(width, height, input.getType());
        boolean alpha = out.getType() == BufferedImage.TYPE_INT_ARGB;
        for (int h = 0; h < height; h++) {
            for (int w = 0; w < width; w++) {
                double[] rgb = convolve(input, kernel, w, h);
                int rgbInt = (((int)rgb[0])&0xff) | ((((int)rgb[1])&0xff)<<8) | ((((int)rgb[2])&0xff)<<16);
                if (alpha) rgbInt = rgbInt | ((((int)rgb[3])&0xff)<<24);
                if (pixelAvg(rgbInt, true) > thresh) {
                    out.setRGB(w, h, rgbInt);
                    // System.out.println("v: " + rgbInt + " a: " + pixelAvg(rgbInt) + " " + thresh + " p: " + out.getRGB(w, h));
                }
            }
        }
        // System.out.println(out.getRGB(100, 100));
        return out;
    }

    public static double[] convolve(BufferedImage input, double [][] kernel, int x, int y) {
        double[] arr = new double[4];
        for (int h = 0; h < kernel.length; h++) {
            for (int w = 0; w < kernel[0].length; w++) {
                int rgb = input.getRGB(x+w, y+h);
                for (int i = 0; i < arr.length; i++) {
                    arr[i] += ((rgb>>(8*i))&0xff)*kernel[h][w];
                }
            }
        }
        // System.out.println(arr[0]);
        return arr;
    }
    
    public static BufferedImage contrast(BufferedImage img, int level, int center) {
        double c = (double) (259*(level+255))/(255*(259-level));
        for(int h = 0; h < img.getHeight(); h++) {
            for(int w = 0; w < img.getWidth(); w++) {
                int rgb = img.getRGB(w, h);
                int fin = 0;
                for (int i = 0; i < 3; i++) {
                    int val = (int)(c*(((rgb>>(8*i))&0xff)-center)+center);
                    if (val > 255) val = 255;
                    if (val < 0) val = 0;
                    fin = fin|((val)&0xff)<<(8*i);
                }
                img.setRGB(w,h,fin);
            }
        }
        try {
            ImageIO.write(img, "jpg", new File("res/Output/cont.jpg"));
        } catch (Exception e) {}
        return img;
    }

    public static BufferedImage grayscale(BufferedImage img) {
        int[] pixels = img.getRGB(0, 0, img.getWidth(), img.getHeight(), null, 0, img.getWidth());
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = pixelAvg(pixels[i], false);
        }
        img.setRGB(0, 0, img.getWidth(), img.getHeight(), pixels, 0, img.getWidth());
        try {
            ImageIO.write(img, "jpg", new File("res/Output/gray.jpg"));
        } catch (Exception e) {}
        return img;
    }

    public static BufferedImage scale(BufferedImage img, double scale) {
        BufferedImage out = new BufferedImage((int) (img.getWidth()*scale), (int) (img.getHeight()*scale), img.getType());
        for (int h = 0; h < out.getHeight(); h++) {
            for (int w = 0; w < out.getWidth(); w++) {
                out.setRGB(w, h, img.getRGB((int)(w/scale), (int)(h/scale)));
            }
        }
        return out;
    }

    public static int pixelAvg (int rgb, boolean r) {
        int avg = (((rgb>>16)&0xff) + ((rgb>>8)&0xff) + ((rgb)&0xff))/3;
        if (r) return avg;
        return (((rgb>>24)&0xff)<<24) | (avg<<16) | (avg<<8) | avg;
    }

    public static int getColorAvg(BufferedImage input) {
        int[] arr = {0,0,0,0};
        for (int h = 0; h < input.getHeight(); h++) {
            for (int w = 0; w < input.getWidth(); w++) {
                int rgb = input.getRGB(w, h);
                for (int i = 0; i < 4; i++) {
                    arr[i] += (rgb>>(8*i))&0xFF;
                }
            }
        }
        int out = 0;
        int size = input.getWidth()*input.getHeight();
        for (int i = 0; i<4; i++) {
            out += (arr[i]/size)<<(8*i);
        }
        return out;
    }

    public static String colorCode(int rgb, boolean foreground) {
        if (foreground) {
            return "\u001b[38;2;" + ((rgb >> 16) & 0xff) + ";" + ((rgb >> 8) & 0xff) + ";" + (rgb & 0xff) + "m";
        }
        else {
            return "\u001b[48;2;" + ((rgb >> 16) & 0xff) + ";" + ((rgb >> 8) & 0xff) + ";" + (rgb & 0xff) + "m";
        }
    }

    public static int compare(BufferedImage img1, BufferedImage img2) {
        try {
            int dif = 0;
            for (int h = 0; h < img1.getHeight(); h++) {
                for (int w = 0; w < img1.getWidth(); w++) {
                    int rgb1 = img1.getRGB(w, h);
                    int rgb2 = img2.getRGB(w, h);
                    for (int i = 0; i < 24; i += 8) {
                        int val1 = (rgb1>>i)&0xff;
                        int val2 = (rgb2>>i)&0xff;
                        // if (val1 == 0 || val2 == 0) dif += 16384;
                        dif += (val1-val2)*(val1-val2);
                    }
                }
            }
            return dif;
        } catch (Exception e) {
            e.printStackTrace();
            return Integer.MAX_VALUE;
        }
    }

    public static String readTxt(File txt) throws FileNotFoundException{
        Scanner in = new Scanner(txt);
        in.useDelimiter("\\Z");
        String str = in.next();
        in.close();
        return str;
    }

    public static void printVid(String vid, double rate, boolean loop) {
        do{
            Scanner scan = new Scanner(vid);
            scan.useDelimiter("</meta>");
            String meta = scan.next();
            if (rate == 0) {
                rate = Double.valueOf(meta.substring(meta.indexOf(">")+1));
            }
            scan.useDelimiter("\r");
            while (scan.hasNext()) {
                System.out.print(scan.next());
                try{Thread.sleep((int)(1000/rate)-20);}catch(Exception e) {}
            }
            scan.close();
        }while (loop);
    }

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
        // try{ vid = readTxt(new File("res/Output/vid.txt"));}catch (Exception e) {e.printStackTrace();}
        File file = new File("C:/Users/graha/Downloads/earth2.mp4");
        // int tune = 32;
        // try{ vid = convertVideo(file, "vid.txt", true, tune, 1, 5.0, 0.0, 6.0);} catch (Exception e) {e.printStackTrace();}
        // img = convertImage(file, "crackers.txt", true, tune, 1);
        // img = convertImage(file, "bw.txt", false, tune, 1);
        // try{ img = readTxt(new File("Output/crackers.txt"));} catch (FileNotFoundException e) {}
        // try{contrast(ImageIO.read(new File("C:/Users/graha/Downloads/crackers.jpg")), 128);} catch (Exception e) {e.printStackTrace();}
        long proc = System.currentTimeMillis()-start;
        start = System.currentTimeMillis();
        printVid(vid, 0, false);
        System.out.print("Processing: " + (proc) + " ms Print: " + (System.currentTimeMillis()-start) + " ms");
        try{
            Scanner in = new Scanner(System.in);
            System.out.print("Input File: ");
            String input = in.next();
            String media;
            double rate = 0;
            if (input.substring(input.indexOf(".")) == ".txt") {
                media = readTxt(new File(input));
            } else {
                int tune = 32;
                int scale = 1;
                boolean color = true;
                System.out.print("Default Options? (y/n) ");
                if (in.next() == "n") {
                    System.out.print("Tune: ");
                    tune = in.nextInt();
                    System.out.print("Scale: ");
                    scale = in.nextInt();
                    System.out.print("Color? (y/n) ");
                    if (in.next() == "n") color = false;
                    System.out.print("Frame Rate: ");
                    rate = in.nextDouble();
                }
                System.out.print("Output File (null if none): ");
                String output = in.next();
                System.out.print("Video? (y/n) ");
                if (in.next() == "y"){
                    System.out.println("Beginning/End:");
                    double beg = in.nextDouble();
                    double end = in.nextDouble();
                    System.out.print("Frame Rate: ");
                    double frate = in.nextDouble();
                    media = convertVideo(new File(input), output, true, false, tune, scale, frate, beg, end);
                } else {
                    media = convertImage(new File(input), output, true, false, tune, scale);
                }
            }
            if (media.indexOf("<meta>") == 0) printVid(media, proc, false);
        } catch (Exception e) {e.printStackTrace();}
    }
}