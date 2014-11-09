package com.hability;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] pArgs ) throws IOException {

            String tFile = null;
            boolean tRecursive = false;
            boolean tDryRun = false;
            String tPattern = null;

            for (int i = 0; i < pArgs.length; i++) {

                switch (pArgs[i].toLowerCase()) {
                    case "-r":
                        tRecursive = true;
                        break;

                    case "-d":
                        tDryRun = true;
                        break;

                    default:

                        if (tFile == null) {
                            tFile = pArgs[i];
                        } else if (tPattern == null) {
                            tPattern = pArgs[i];
                        }

                        break;
                }
            }

            if (tFile == null) {
                System.out.println("Enter file");
                tFile =
                        new BufferedReader(new InputStreamReader(System.in)).readLine();
            }


            if (tPattern == null) {
                String tDefaultPattern = "{yyyy}{MM}{dd}_{HH}{mm}{ss}";

                System.out.println("Enter pattern (" + tDefaultPattern + ")");

                tPattern =
                        new BufferedReader(new InputStreamReader(System.in)).readLine();

                if (tPattern.equals("")) {
                    tPattern = tDefaultPattern;
                }
            }

            process(new File(tFile), tPattern, tRecursive, tDryRun);
        }

    private static void process(
            File pFile, String pPattern, boolean pRecursive, boolean pDryRun) {

        if (!pFile.exists()) {
            System.out.println("File " + pFile + " does not exist");
            return;
        }

        if (pFile.isDirectory()) {
            for (File tSubFile : pFile.listFiles()) {
                if (tSubFile.isDirectory()) {
                    if (pRecursive) {
                        process(tSubFile, pPattern, pRecursive, pDryRun);
                    }
                } else {
                    processFile(tSubFile, pPattern, pDryRun);
                }
            }

        } else {
            processFile(pFile, pPattern, pDryRun);
        }
    }

    static boolean all = false;

    private static void processFile(File pFile, String pPattern, boolean pDryRun) {

        if (!validFile(pFile)) {
            System.out.println("Skipping invalid file " + pFile.getAbsolutePath());
            return;
        }

        try {
            Metadata tMetadata = ImageMetadataReader.readMetadata(pFile);

            String tNewFileName = getNewFileName(tMetadata, pPattern);

            if (tNewFileName == null) {
                System.out.println("Error getting new filename for " + pFile.getAbsolutePath());
                return;
            }

            String tNewPath = pFile.getParent() + File.separator + tNewFileName + getExt(pFile.getName());

            boolean tConfirmed = false;
            String tComment = null;
            Boolean tResult = null;

            if (tNewPath.compareToIgnoreCase((pFile.getAbsolutePath())) == 0) {
                tComment = "Same filename";
            } else {
                int tIdx = 0;
                while (new File(tNewPath).exists()) {
                    tNewPath =
                            pFile.getParent() + File.separator + tNewFileName + "-(" + tIdx + ")" + getExt(pFile.getName());
                    tIdx++;
                }

                if (!all) {
                    System.out.println("Rename");
                    System.out.println(pFile.getAbsolutePath());
                    System.out.println("to");
                    System.out.println(tNewPath);
                    System.out.println("?");
                    String tLine =
                            new BufferedReader(new InputStreamReader(System.in)).readLine();

                    if (tLine.compareToIgnoreCase("a") == 0) {
                        all = true;
                        tComment = "confirmed by user, will auto confirm rest";
                    } else if (tLine.compareToIgnoreCase("y") == 0) {
                        tConfirmed = true;
                        tComment = "confirmed by user";
                    } else {
                        tComment = "declined by user";
                    }

                } else {
                    tComment = "auto confirmed";
                }


                if (!pDryRun && (all || tConfirmed)) {
                    tResult = pFile.renameTo(new File(tNewPath));
                }
            }

            System.out.println(
                    pFile.getAbsolutePath() + ";" +
                            tNewPath + ";" +
                            pFile.getName() + ";" +
                            tNewFileName + ";" +
                            tResult + ";" +
                            tComment);


        } catch (Throwable e) {
            System.out.println("Error processing file " + pFile);
            e.printStackTrace();
        }
    }

    private static String getExt(String name) {
        if (name.indexOf(".") == -1) {
            return "";
        }

        return name.substring(name.indexOf("."));
    }

    private static boolean validFile(File pFile) {
        if (pFile.getName().indexOf(".") == -1) {
            return false;
        }

        String tExt = pFile.getName().substring(pFile.getName().indexOf("."));

        switch (tExt.toLowerCase()) {
            case ".jpeg":
            case ".jpg":
                return true;
            default:
                return false;

        }
    }

    private static String getNewFileName(Metadata tMetadata, String pPattern) {

        MyMeta tMyMeta = getMyMeta(tMetadata);

        if (tMyMeta == null) {
            return null;
        }

        return pPattern
                .replace("{yyyy}", tMyMeta.year)
                .replace("{MM}", tMyMeta.month)
                .replace("{dd}", tMyMeta.day)
                .replace("{HH}", tMyMeta.hours)
                .replace("{mm}", tMyMeta.minutes)
                .replace("{ss}", tMyMeta.seconds);
    }


    private static MyMeta getMyMeta(Metadata tMetadata) {

        Directory tDir = tMetadata.getDirectory(ExifSubIFDDirectory.class);

        if (tDir != null) {
            if (tDir.containsTag(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)) {
                return new MyMeta(tDir.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL));
            }

            if (tDir.containsTag(ExifSubIFDDirectory.TAG_DATETIME_DIGITIZED)) {
                return new MyMeta(tDir.getDate(ExifSubIFDDirectory.TAG_DATETIME_DIGITIZED));
            }
        }

        tDir = tMetadata.getDirectory(ExifIFD0Directory.class);

        if (tDir != null) {
            if (tDir.containsTag(ExifIFD0Directory.TAG_DATETIME)) {
                return new MyMeta(tDir.getDate(ExifIFD0Directory.TAG_DATETIME));
            }
        }

        for (Directory tDira : tMetadata.getDirectories()) {
            for (Tag tTag : tDira.getTags()) {
                System.out.println(tTag);
            }
        }
        return null;
    }


    static class MyMeta {
        String year;
        String month;
        String day;
        String seconds;
        String hours;
        String minutes;

        public MyMeta(Date date) {
            year = getDateString(date, "yyyy");
            month = getDateString(date, "MM");
            day = getDateString(date, "dd");
            hours = getDateString(date, "HH");
            minutes = getDateString(date, "mm");
            seconds = getDateString(date, "ss");
        }

        private String getDateString(Date date, String format) {
            SimpleDateFormat tFormat = new SimpleDateFormat(format);
            return tFormat.format(date);
        }
    }

}
