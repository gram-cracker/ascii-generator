package com.project;
import java.io.*;
import java.net.URI;
import java.awt.image.*;
import javax.imageio.*;
import java.util.*;
import java.net.URL;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

public class Converter {
    public static BufferedImage[] chars = new BufferedImage[95];

    public static void getChars() {
        if (chars[0] == null) {
            try { try {
                for (int i = 32; i < 127; i++) {
                    chars[i-32] = ImageIO.read(Class.forName("Converter").getResourceAsStream("src/res/chars/" + i + ".png"));
                }
            } catch (ClassNotFoundException e) {
                int i = 68;
                for(File file : new File("src/res/chars").listFiles()) {
                    chars[i] = ImageIO.read(file);
                    if (i == 126-32) i = -1;
                    i++;
                }
            }} catch (Exception e) {e.printStackTrace();}
        }
    }

    public static String convertImage(BufferedImage raw, String output, boolean color, boolean background, int tune, double scale) {
        // String type = input.getName().substring(input.getName().indexOf("."));
        // if (type != ".jpg" || type != ".png") return "";
        try {
            // Map<String, InputStream> chars = new HashMap<String, InputStream>();
            getChars();
            String str = "\u001b[39;49m";
            if (scale != 1.0) raw = scale(raw, scale);
            BufferedImage img = contrast(grayscale(gaussFilter(edgeDetection(gaussFilter(raw), true, tune))), 255, 64);
            if (!background) img = grayscale(combine(img, raw));
            for (int h = 0; h < img.getHeight()-24; h += 24){
                for (int w = 0; w < img.getWidth()-12; w += 12) {
                    BufferedImage subimg = img.getSubimage(w, h, 12, 24);
                    int chr = 0;
                    int leastDif = Integer.MAX_VALUE;
                    int name = 31;
                    for (BufferedImage file : chars) {
                        name++;
                        if (name != 32 || background) {
                            int dif = compare(subimg, file);
                            if (dif < leastDif) {
                                leastDif = dif;
                                // chr = Integer.parseInt(file.getName().substring(0,file.getName().indexOf(".")));
                                chr = name;
                                // System.out.print(chr + ":" + dif + " ");
                            }
                        }
                    }
                    // System.out.println();
                    if (color) str += colorCode(getColorAvg(raw.getSubimage(w, h, 12, 24)),!background);
                    str += String.valueOf((char)chr);
                }
                if(color) str += "\u001b[39;49m";
                str += "\n";
            }
            str += "\u001b[39;49m";
            if (output != null) {
                FileOutputStream thing = new FileOutputStream(new File(output));
                thing.write(str.getBytes());
                thing.close();
            }
            return str;
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String convertImage(File file, String output, boolean color, boolean background, int tune, double scale) {
        try {
            BufferedImage raw = ImageIO.read(file);
            return convertImage(raw, output, color, background, tune, scale);
        } catch (Exception e) {e.printStackTrace(); return "";}
    }

    public static String convertVideo(File input, String output, boolean color, boolean background, int tune, double scale, double frate, double start, double end) {
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(input.getAbsoluteFile());
        Java2DFrameConverter conv = new Java2DFrameConverter();
        String out = "";
        try {
            grabber.start();
            double rate = (grabber.getFrameRate()/frate);
            grabber.setVideoFrameNumber((int)(grabber.getFrameRate()*start));
            int len = (int)((end-start)*grabber.getFrameRate());
            System.out.println("Rate scale:" + rate);
            out += "<meta>" + frate + "</meta>";
            for (double i = 0; i < len; i += rate) {
                System.out.println("frame:" + i/rate);
                grabber.setVideoFrameNumber((int)i);
                BufferedImage img = conv.convert(grabber.grab());
                if (img != null) out += "\u001b[H" /*+"\u001b[2J"*/ + convertImage(img, null, color, background, tune, scale) + "\r";
            }
            grabber.stop();
        } catch (Exception e) {e.printStackTrace();}
        try {
            if (output != null) {
                FileOutputStream thing = new FileOutputStream(new File("src/res/Output/" + output));
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
                int avg = (pixelAvg(a.getRGB(w, h), true) + pixelAvg(b.getRGB(w, h), true));
                if (avg > 255) avg = 255;
                else if (avg < 0) avg = 0;
                a.setRGB(w, h, ((avg)|(avg<<8)|(avg<<16)));
            }
        }
        try {
            System.out.println("c:" + ImageIO.write(a, "jpg", new File("src/res/Output/comb.jpg")));
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
        // try {
        //     System.out.println("e:" + ImageIO.write(out, "jpeg", new File("src/res/Output/edge.jpg")));
        // } catch (IOException e) {e.printStackTrace();}
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
        // try {
        //     System.out.println("g:" + ImageIO.write(g, "jpeg", new File("src/res/Output/gauss.jpg")));
        // } catch (IOException e) {e.printStackTrace();}
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
        // try {
        //     ImageIO.write(img, "jpg", new File("src/res/Output/cont.jpg"));
        // } catch (Exception e) {}
        return img;
    }

    public static BufferedImage grayscale(BufferedImage img) {
        int[] pixels = img.getRGB(0, 0, img.getWidth(), img.getHeight(), null, 0, img.getWidth());
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = pixelAvg(pixels[i], false);
        }
        img.setRGB(0, 0, img.getWidth(), img.getHeight(), pixels, 0, img.getWidth());
        // try {
        //     ImageIO.write(img, "jpg", new File("src/res/Output/gray.jpg"));
        // } catch (Exception e) {}
        return img;
    }

    public static BufferedImage scale(BufferedImage img, double scale) {
        BufferedImage out = new BufferedImage((int) (img.getWidth()*scale), (int) (img.getHeight()*scale), img.getType());
        for (int h = 0; h < out.getHeight(); h++) {
            for (int w = 0; w < out.getWidth(); w++) {
                double[] rgba = new double[4];
                double ysum = 0;
                // if (h==0) System.out.println("check");
                while (ysum<1) {
                    double sy = h%scale;
                    if (sy==0 || ysum!=0) sy = scale;
                    if (ysum+scale>1) sy = 1 - ysum; // extra bit on edge of pixel
                    if (sy > 1) sy = 1; //scale up
                    double xsum = 0;
                    // if (h==0) System.out.println("check");
                    while(xsum < 1) {
                        double sx = w%scale;
                        if (sx==0 || xsum!=0) sx = scale;
                        if (xsum+scale>1) sx = 1 - xsum; // extra bit on edge of pixel
                        if (sx > 1) sx = 1; //scale up
                        try{
                            int rgb = img.getRGB((int)(1/scale*(w+xsum)), (int)(1/scale*(h+ysum)));
                            // if (h==0) System.out.println("check get");
                            for (int i = 0; i<4; i++) {
                                rgba[i] += sy*sx*(rgb>>(i*8)&0xff);
                                if (rgba[i] > 255.0) rgba[i] = 255.0;
                            }
                        }catch(Exception e) {e.printStackTrace();}
                        // if (h==1 && w==1) System.out.println(sx + " " + sy);
                        xsum += sx;
                    }
                    // if (h==0) System.out.println("check");
                    ysum += sy;
                }
                // if (h==0) System.out.println("check");
                out.setRGB(w,h,((int)rgba[3]<<24)|((int)rgba[2]<<16)|((int)rgba[1]<<8)|((int)rgba[0]));
                // out.setRGB(w, h, img.getRGB((int)(w/scale), (int)(h/scale)));
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
                    // int rgb1 = img1.getRGB(w, h);
                    // int rgb2 = img2.getRGB(w, h);
                    // for (int i = 0; i < 24; i += 8) {
                    //     int val1 = (rgb1>>i)&0xff;
                    //     int val2 = (rgb2>>i)&0xff;
                    //     // if (val1 == 0 || val2 == 0) dif += 16384;
                    //     dif += (val1-val2)*(val1-val2);
                    // }
                    int val1 = pixelAvg(img1.getRGB(w, h), true);
                    int val2 = pixelAvg(img2.getRGB(w, h), true);
                    dif += (val1-val2)*(val1-val2);
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

    public static String readTxt(String txt) throws FileNotFoundException{
        return readTxt(new File(txt));
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
                try{Thread.sleep((int)(1000.0/rate-5.5));}catch(Exception e) {}
            }
            scan.close();
        }while (loop);
    }

    public static void main(String[] args) {
        try{
            Scanner in = new Scanner(System.in);
            System.out.print("Input File: ");
            String input = in.next();
            System.out.println(input);
            String media;
            double rate = 0;
            long start = 0;
            boolean txt = input.contains(".txt");
            if (input.contains(".txt")) {
                start = System.currentTimeMillis();
                media = readTxt(new File(input));
            } else {
                int tune = 32;
                double scale = 1;
                boolean color = true;
                boolean back = true;
                System.out.print("Default Options? (y/n) ");
                if (in.next().equals("n")) {
                    System.out.print("Tune: ");
                    tune = in.nextInt();
                    System.out.print("Scale: ");
                    scale = in.nextDouble();
                    System.out.print("Color? (y/n) ");
                    if (in.next() == "n") color = false;
                    System.out.print("Background color? (y/n) ");
                    if (in.next() == "n") color = false;
                }
                System.out.print("Output File (null if none): ");
                String output = in.next();
                System.out.print("Video? (y/n) ");
                String vid = in.next();
                if (vid.equals("y")){
                    System.out.println("Beginning/End:");
                    double beg = in.nextDouble();
                    double end = in.nextDouble();
                    System.out.print("Frame Rate: ");
                    double frate = in.nextDouble();
                    start = System.currentTimeMillis();
                    media = convertVideo(new File(input), output, color, back, tune, scale, frate, beg, end);
                } else {
                    start = System.currentTimeMillis();
                    media = convertImage(new File(input), output, color, back, tune, scale);
                }
            }
            if (media.contains("<meta>")) {
                System.out.print("Frame Rate (0 for default): ");
                double framerate = in.nextDouble();
                System.out.println("Processing time: " + (System.currentTimeMillis()-start) + " ms");
                start = System.currentTimeMillis();
                printVid(media, framerate, false);
                System.out.println("Printing time: " + (System.currentTimeMillis()-start) + " ms");

            } else {
                System.out.println("Processing time: " + (System.currentTimeMillis()-start) + " ms");
                start = System.currentTimeMillis();
                System.out.print(media);
                System.out.println("Printing time: " + (System.currentTimeMillis()-start) + " ms");
            }
            in.close();
        } catch (Exception e) {e.printStackTrace();}
    }
}