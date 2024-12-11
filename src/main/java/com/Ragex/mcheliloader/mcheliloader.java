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

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        minecraftDir = event.getModConfigurationDirectory().getParentFile();
        String[] fileURLs = {
                "https://github.com/RagexPrince683/mchelio/archive/refs/heads/pain.zip"
        };

        Path modsDir = Paths.get(minecraftDir.getPath(), "mods");

        // Set up custom font for JOptionPane
        setCustomFont();

        // Create a JFrame to be the owner of the JOptionPane dialogs
        JFrame frame = new JFrame();
        frame.setAlwaysOnTop(true);
        frame.setUndecorated(true); // Optional: removes window decorations
        frame.setSize(1, 1); // Minimizes the frame size
        frame.setLocationRelativeTo(null); // Center the frame on screen

        JOptionPane.showMessageDialog(frame, "Please do not close the forge application. Mcheli is downloading and will take longer than normal.",
                "Downloading", JOptionPane.INFORMATION_MESSAGE);

        // Check if HBM is installed
      // Path hbmModFilePath = Paths.get(modsDir.toString(), "HBM-NTM-.1.0.27_X5061.jar");
      // if (!Files.exists(hbmModFilePath)) {
      //     JOptionPane.showMessageDialog(frame, "HBM is not installed. Please download and install HBM before proceeding.",
      //             "Mod Not Found", JOptionPane.ERROR_MESSAGE);
      //     LOGGER.info("HBM mod is not installed.");
      //     System.exit(0); // Terminate the application
      // }

        // Proceed with installing MCHELI
        for (String fileURL : fileURLs) {
            try {
                // Download the ZIP file
                Path zipFilePath = downloadFile(fileURL, modsDir.toString());

                // Unzip the file
                unzipFile(zipFilePath.toString(), modsDir.toString());

                // Process the new-vehicles file
                if (fileURL.contains("pain")) {
                    // For the new-vehicles file, ensure the extracted folder exists
                    Path extractedFolder = Paths.get(modsDir.toString(), "mchelio-pain");
                    if (Files.exists(extractedFolder)) {
                        Path targetFolder = Paths.get(modsDir.toString(), "pain");
                        Files.move(extractedFolder, targetFolder, StandardCopyOption.REPLACE_EXISTING);

                        // Clean up the ZIP file
                        Files.delete(zipFilePath);

                        LOGGER.info("Unzipped and moved the pain files to mods folder.");
                    } else {
                        LOGGER.error("Extracted folder 'mchelio-pain' does not exist. Skipping ZIP file deletion.");
                    }
                }

                LOGGER.info("Processed file: " + fileURL);
            } catch (IOException e) {
                LOGGER.error("Failed to download, unzip, or move the file.", e);
            }
        }

        // Show success message
        JOptionPane.showMessageDialog(frame, "Mchelio was successfully downloaded. Please restart your instance.",
                "Success", JOptionPane.INFORMATION_MESSAGE);

        try {
            scheduleSelfDeletion(event.getSourceFile().getPath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.exit(0); // Terminate the application to allow deletion
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
        // Set a custom font for JOptionPane dialogs
        UIManager.put("OptionPane.messageFont", new Font("Arial", Font.PLAIN, 20));
    }

    private void scheduleSelfDeletion(String jarFilePath) throws IOException {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            // Create a batch file for Windows
            Path batchFile = Paths.get(minecraftDir.getPath(), "delete_self.bat");
            Path vbsFile = Paths.get(minecraftDir.getPath(), "run_silent.vbs");

            try (BufferedWriter writer = Files.newBufferedWriter(batchFile)) {
                writer.write("ping 127.0.0.1 -n 2 > nul\n"); // Short delay to ensure the Java process has terminated
                writer.write("del \"" + jarFilePath + "\"\n");
                writer.write("del \"%~f0\""); // Delete the batch file itself
            }

            try (BufferedWriter writer = Files.newBufferedWriter(vbsFile)) {
                writer.write("Set WshShell = CreateObject(\"WScript.Shell\")\n");
                writer.write("WshShell.Run chr(34) & \"" + batchFile.toAbsolutePath() + "\" & chr(34), 0\n");
                writer.write("Set WshShell = Nothing\n");

                writer.write("Sub Main()\n");
                writer.write("    discardScript()\n");
                writer.write("End Sub\n");

                writer.write("Function discardScript()\n");
                writer.write("    On Error Resume Next\n");
                writer.write("    Set objFSO = CreateObject(\"Scripting.FileSystemObject\")\n");
                writer.write("    strScript = Wscript.ScriptFullName\n");
                writer.write("    objFSO.DeleteFile(strScript)\n");
                writer.write("End Function\n");

                writer.write("Main\n");  // This calls the Main subroutine
            }

            Runtime.getRuntime().exec("wscript " + vbsFile);

        } else if (os.contains("nix") || os.contains("nux") || os.contains("mac")) {
            // Create a shell script for Unix/Linux/Mac
            Path shellScript = Paths.get(minecraftDir.getPath(), "delete_self.sh");
            try (BufferedWriter writer = Files.newBufferedWriter(shellScript)) {
                writer.write("#!/bin/sh\n");
                writer.write("sleep 2\n"); // Short delay to ensure the Java process has terminated
                writer.write("rm -f \"" + jarFilePath + "\"\n");
                writer.write("rm -- \"$0\""); // Delete the shell script itself
            }
            Runtime.getRuntime().exec("sh " + shellScript.toString());

            // Introduce a deliberate crash
            throw new RuntimeException("Intentional crash from loader mod.");
        }
    }
}