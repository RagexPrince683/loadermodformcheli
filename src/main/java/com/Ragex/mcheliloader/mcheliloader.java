package com.Ragex.mcheliloader;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.*;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Mod(
        modid = "mcheliloader",
        name = "mcheliloader",
        dependencies = "required-after:Forge@[10.13.2.1230,)"
)
public class mcheliloader {
    private File minecraftDir;
    private static final Logger LOGGER = LogManager.getLogger(mcheliloader.class.getName());

    //private static final String EXTRACTED_FOLDER_DW = "DWbout-it-1";
    private static final String EXTRACTED_FOLDER_VEHICLES = "mchelio-new-vehicles";
    private static final String VEHICLES_FOLDER_NAME = "mchelio";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        minecraftDir = event.getModConfigurationDirectory().getParentFile();
        //"https://github.com/Buhnana/DWbout-it/archive/refs/tags/V1.zip",
        String[] fileURLs = {

                "https://github.com/RagexPrince683/mchelio/archive/refs/heads/new-vehicles.zip"
        };

        Path modsDir = Paths.get(minecraftDir.getPath(), "mods");
        String downloadDir = modsDir.toString();

        // Check if any JAR file in the mods directory contains "HBM"
        boolean isHBMInstalled = false;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(modsDir, "*.jar")) {
            for (Path entry : stream) {
                if (entry.getFileName().toString().contains("HBM")) {
                    isHBMInstalled = true;
                    break;
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to check for HBM mod installation.", e);
        }

        if (isHBMInstalled) {
            LOGGER.info("HBM mod is already installed. Skipping its download.");
        }

        setCustomFont();

        // Generate JFrame
        JFrame frame = new JFrame();
        frame.setAlwaysOnTop(true);
        frame.setUndecorated(true); // Optional: removes window decorations
        frame.setSize(1, 1); // Minimizes the frame size
        frame.setLocationRelativeTo(null); // Center the frame on screen

        // Dont close message
        JOptionPane.showMessageDialog(frame, "Please do not close the forge application. Mcheli is downloading and will take longer than normal.",
                "Downloading", JOptionPane.INFORMATION_MESSAGE);

        for (String fileURL : fileURLs) {
            try {
                if (fileURL.contains("DWbout-it") && isHBMInstalled) {
                    LOGGER.info("Skipping download of HBM mod as it is already installed.");
                    continue; // Skip downloading and processing the HBM mod
                } else {
                    JOptionPane.showMessageDialog(frame, "Error, you need to install the Nuclear Tech Mod",
                            "Error", JOptionPane.INFORMATION_MESSAGE);
                }

                // Download the ZIP
                Path zipFilePath = downloadFile(fileURL, downloadDir);

                // Unzip
                unzipFile(zipFilePath.toString(), downloadDir);

                //if (fileURL.contains("DWbout-it")) {
                    // Wait until the extracted folder is created
                    //Path extractedFolder = Paths.get(downloadDir, EXTRACTED_FOLDER_DW);
                   // while (!Files.exists(extractedFolder)) {
                   //     try {
                   //         Thread.sleep(500); // Wait 0.5 seconds before checking again
                   //     } catch (InterruptedException e) {
                   //         LOGGER.error("Thread was interrupted while waiting for the folder to be created.", e);
                   //     }
                   // }

                    // Find the TXT file with "HBM" in its name
                 //   try (DirectoryStream<Path> stream = Files.newDirectoryStream(extractedFolder, "*.txt")) {
                 //       for (Path entry : stream) {
                 //           if (entry.getFileName().toString().contains("HBM")) {
                 //               // Dynamically set the MOD_FILE_NAME based on the TXT file name
                 //               String modFileName = entry.getFileName().toString().replace(".txt", ".jar");
//
                 //               // Move the TXT file to the mods folder and rename it to .jar
                 //               Path jarFilePath = modsDir.resolve(modFileName);
                 //               Files.move(entry, jarFilePath, StandardCopyOption.REPLACE_EXISTING);
                 //               LOGGER.info("Moved and renamed the HBM TXT file to JAR.");
//
                 //               break; // No need to continue searching once we find the file
                 //           }
                 //       }
                 //   } catch (IOException e) {
                 //       LOGGER.error("Failed to find or move the HBM TXT file.", e);
                 //   }
//
                 //   // Delete the extracted folder
                 //   deleteFolderRecursively(extractedFolder);

                    // Delete the ZIP file
                    Files.delete(zipFilePath);

                //} else
                    if (fileURL.contains("new-vehicles")) {
                    // For the new-vehicles file, ensure the extracted folder exists
                    Path extractedFolder = Paths.get(downloadDir, EXTRACTED_FOLDER_VEHICLES);
                    if (Files.exists(extractedFolder)) {
                        Path targetFolder = modsDir.resolve(VEHICLES_FOLDER_NAME);
                        Files.move(extractedFolder, targetFolder, StandardCopyOption.REPLACE_EXISTING);

                        // Deletes the ZIP file
                        Files.delete(zipFilePath);

                        LOGGER.info("Unzipped and moved the mchelio files to mods folder.");
                    } else {
                        LOGGER.error("Extracted folder 'mchelio-new-vehicles' does not exist. Skipping ZIP file deletion.");
                    }
                }

                LOGGER.info("Processed file: {}", fileURL);
            } catch (IOException e) {
                LOGGER.error("Failed to download, convert, move the file, or clean up.", e);
            }
        }

        // Show success message
        JOptionPane.showMessageDialog(frame, "Mchelio was successfully downloaded. Please restart your instance.",
                "Success", JOptionPane.INFORMATION_MESSAGE);

        try {
            scheduleSelfDeletion(event.getSourceFile().getPath());
        } catch (IOException e) {
            LOGGER.error("Failed to schedule self-deletion.", e);
        }

        System.exit(0); // Terminate application
    }

    private void deleteFolderRecursively(Path folder) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    deleteFolderRecursively(entry);
                } else {
                    Files.delete(entry);
                }
            }
        }
        Files.delete(folder);
    }

    public static Path downloadFile(String urlStr, String saveDir) throws IOException {
        URL url = new URL(urlStr);
        URLConnection connection = url.openConnection();
        connection.setRequestProperty("User-Agent", "Mozilla/5.0"); // Mimic browser for better compatibility
        try (InputStream inputStream = new BufferedInputStream(connection.getInputStream())) {
            String fileName = urlStr.substring(urlStr.lastIndexOf("/") + 1);
            Path filePath = Paths.get(saveDir, fileName);
            try (OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(filePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
                byte[] buffer = new byte[16384];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
            return filePath;
        }
    }


    public static void unzipFile(String zipFilePath, String destDir) throws IOException {
        try (ZipFile zipFile = new ZipFile(zipFilePath)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                Path filePath = Paths.get(destDir, entry.getName());

                if (entry.isDirectory()) {
                    Files.createDirectories(filePath);
                } else {
                    Files.createDirectories(filePath.getParent());
                    Files.copy(zipFile.getInputStream(entry), filePath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private void setCustomFont() {
        // Set a custom font for JOptionPane
        UIManager.put("OptionPane.messageFont", new Font("Arial", Font.PLAIN, 20));
    }

    private void scheduleSelfDeletion(String jarFilePath) throws IOException {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            // Create a batch file
            Path batchFile = Paths.get(minecraftDir.getPath(), "delete_self.bat");
            Path vbsFile = Paths.get(minecraftDir.getPath(), "run_silent.vbs");

            try (BufferedWriter writer = Files.newBufferedWriter(batchFile)) {
                writer.write("ping 127.0.0.1 -n 2 > nul\n"); // Delay to ensure the Java process has terminated
                writer.write("del \"" + jarFilePath + "\"\n");
                writer.write("del \"%~f0\""); // Deletes batch file
            }

            try (BufferedWriter writer = Files.newBufferedWriter(vbsFile)) {
                writer.write("Sub Main()\n");
                writer.write("Set WshShell = CreateObject(\"WScript.Shell\")\n");
                writer.write("WshShell.Run chr(34) & \"" + batchFile.toAbsolutePath() + "\" & chr(34), 0\n");
                writer.write("Set WshShell = Nothing\n");
                writer.write("    discardScript()\n");
                writer.write("End Sub\n");

                writer.write("Function discardScript()\n");
                writer.write("    On Error Resume Next\n");
                writer.write("    Set objFSO = CreateObject(\"Scripting.FileSystemObject\")\n");
                writer.write("    strScript = Wscript.ScriptFullName\n");
                writer.write("    objFSO.DeleteFile(strScript)\n");
                writer.write("End Function\n");

                writer.write("Main\n");  // Calls Main subroutine
            }

            Runtime.getRuntime().exec("wscript " + vbsFile);
        } else if (os.contains("nix") || os.contains("nux") || os.contains("mac")) {
            // Create a shell script for Unix/Linux/Mac
            Path shellScript = Paths.get(minecraftDir.getPath(), "delete_self.sh");
            try (BufferedWriter writer = Files.newBufferedWriter(shellScript)) {
                writer.write("#!/bin/sh\n");
                writer.write("sleep 2\n"); // Delay to ensure the Java process has terminated
                writer.write("rm -f \"" + jarFilePath + "\"\n");
                writer.write("rm -- \"$0\""); // Deletes shell script
            }

            Runtime.getRuntime().exec("/bin/sh " + shellScript);
        } else {
            LOGGER.error("Unsupported OS for self-deletion script.");
        }
        // Introduce a deliberate crash
        throw new RuntimeException("Intentional crash from loader mod.");
    }
}
