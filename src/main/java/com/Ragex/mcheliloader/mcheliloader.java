package com.Ragex.mcheliloader;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
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

    private static final String EXTRACTED_FOLDER_DW = "DWbout-it-1";
    private static final String EXTRACTED_FOLDER_VEHICLES = "mchelio-new-vehicles";
    private static final String VEHICLES_FOLDER_NAME = "mchelio";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        minecraftDir = event.getModConfigurationDirectory().getParentFile();
        Path modsDir = Paths.get(minecraftDir.getPath(), "mods");

        // Check if mods are already installed
        Path mchelioFolder = modsDir.resolve("mchelio");
        boolean isHBMModPresent = false;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(modsDir, "*HBM*.jar")) {
            for (Path path : stream) {
                isHBMModPresent = true;
                break;
            }
        } catch (IOException e) {
            LOGGER.error("Failed to check for HBM mod in the mods folder.", e);
        }

        if (isHBMModPresent) {
            LOGGER.info("HBM found, canceling modloader.");
            scheduleSelfDeletionForAlreadyExistsError(event);
            return;
        }

        if (Files.exists(mchelioFolder)) {
            LOGGER.info("MCHELIO found, canceling modloader.");
            scheduleSelfDeletionForAlreadyExistsError(event);
            return;
        }

        // Set custom font size for JOptionPane
        setCustomFont();

        // Show "Don't close" message
        JFrame frame = new JFrame();
        frame.setAlwaysOnTop(true);
        frame.setUndecorated(true);
        frame.setSize(1, 1);
        frame.setLocationRelativeTo(null);

        JOptionPane.showMessageDialog(frame, "Please do not close the forge application. Mcheli is extracting and will take longer than normal.",
                "Extracting", JOptionPane.INFORMATION_MESSAGE);

        // Unzip files directly from the JAR resources into the mods directory
        try {
            unzipResourceToDirectory("/ntm.zip", modsDir.toString());
            unzipResourceToDirectory("/mchelio.zip", modsDir.toString());

            Path extractedFolderDW = Paths.get(modsDir.toString(), EXTRACTED_FOLDER_DW);
            Path extractedFolderVehicles = Paths.get(modsDir.toString(), EXTRACTED_FOLDER_VEHICLES);

            // Handle NTME extraction if necessary
            if (Files.exists(extractedFolderDW)) {
                handleHBMExtraction(extractedFolderDW, modsDir);
                deleteFolderRecursively(extractedFolderDW);
            } else {
                LOGGER.error("Extracted folder 'DWbout-it-1' does not exist. Skipping HBM handling.");
            }

            // Handle Mchelio extraction
            if (Files.exists(extractedFolderVehicles)) {
                Path targetFolder = modsDir.resolve(VEHICLES_FOLDER_NAME);
                Files.move(extractedFolderVehicles, targetFolder, StandardCopyOption.REPLACE_EXISTING);

                LOGGER.info("Unzipped and moved the mchelio files to mods folder.");
            } else {
                LOGGER.error("Extracted folder 'mchelio-new-vehicles' does not exist. Skipping Mchelio handling.");
            }

            // Show success message
            JOptionPane.showMessageDialog(frame, "Mchelio was successfully extracted. Please restart your instance.",
                    "Success", JOptionPane.INFORMATION_MESSAGE);

        } catch (IOException e) {
            LOGGER.error("Failed to extract or move the files.", e);
        }

        // Schedule self-deletion
        try {
            scheduleSelfDeletion(event.getSourceFile().getPath());
        } catch (IOException e) {
            LOGGER.error("Failed to schedule self-deletion.", e);
        }

        System.exit(0); // Terminate application
    }

    private void scheduleSelfDeletionForAlreadyExistsError(FMLPreInitializationEvent event) {
        try {
            scheduleSelfDeletion(event.getSourceFile().getPath());
        } catch (IOException e) {
            LOGGER.error("Failed to schedule self-deletion.", e);
        }
        System.exit(0); // Exit immediately after scheduling deletion
    }

    private void unzipResourceToDirectory(String resourcePath, String destDir) throws IOException {
        try (InputStream zipStream = getClass().getResourceAsStream(resourcePath)) {
            if (zipStream == null) {
                throw new FileNotFoundException("Resource not found: " + resourcePath);
            }

            Path tempZipFile = Files.createTempFile("tempZip", ".zip");
            Files.copy(zipStream, tempZipFile, StandardCopyOption.REPLACE_EXISTING);
            unzipFile(tempZipFile.toString(), destDir);
            Files.delete(tempZipFile);
        }
    }

    private void handleHBMExtraction(Path extractedFolder, Path modsDir) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(extractedFolder, "*.txt")) {
            for (Path entry : stream) {
                if (entry.getFileName().toString().contains("HBM")) {
                    String modFileName = entry.getFileName().toString().replace(".txt", ".jar");
                    Path jarFilePath = modsDir.resolve(modFileName);
                    Files.move(entry, jarFilePath, StandardCopyOption.REPLACE_EXISTING);
                    LOGGER.info("Moved and renamed the HBM TXT file to JAR.");
                    break;
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to find or move the HBM TXT file.", e);
        }
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
                    try (InputStream in = zipFile.getInputStream(entry)) {
                        Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }
    }

    private void setCustomFont() {
        Font customFont = new Font("Arial", Font.PLAIN, 18);
        UIManager.put("OptionPane.messageFont", customFont);
        UIManager.put("OptionPane.buttonFont", customFont);
    }

    private void scheduleSelfDeletion(String jarFilePath) throws IOException {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            // Create a batch file
            Path batchFile = Paths.get(minecraftDir.getPath(), "delete_self.bat");
            Path vbsFile = Paths.get(minecraftDir.getPath(), "run_silent.vbs");

            try (BufferedWriter writer = Files.newBufferedWriter(batchFile)) {
                writer.write("ping 127.0.0.1 -n 8 > nul\n"); 
                writer.write(":deleteLoop\n"); 
                writer.write("if exist \"" + jarFilePath + "\" (\n");
                writer.write("    del \"" + jarFilePath + "\"\n"); 
                writer.write("    if exist \"" + jarFilePath + "\" goto deleteLoop\n"); // If the file still exists loop
                writer.write(")\n");
                writer.write("del \"%~f0\""); 
            }

            try (BufferedWriter writer = Files.newBufferedWriter(vbsFile)) {
                writer.write("Sub Main()\n");
                writer.write("    Set WshShell = CreateObject(\"WScript.Shell\")\n");
                writer.write("    WshShell.Run chr(34) & \"" + batchFile.toAbsolutePath() + "\" & chr(34), 0\n");
                writer.write("    Set WshShell = Nothing\n");
                writer.write("    discardScript()\n");
                writer.write("End Sub\n");
                writer.write("Function discardScript()\n");
                writer.write("    On Error Resume Next\n");
                writer.write("    Set objFSO = CreateObject(\"Scripting.FileSystemObject\")\n");
                writer.write("    objFSO.DeleteFile WScript.ScriptFullName\n"); // Deletes the VBScript itself
                writer.write("End Function\n");
                writer.write("Main()\n"); // Call the Main function to execute the batch and discard the script
            }
            Runtime.getRuntime().exec("wscript " + vbsFile);
        } else if (os.contains("nix") || os.contains("nux") || os.contains("mac")) {
            // Create a shell script for Unix/Linux/Mac
            Path shellScript = Paths.get(minecraftDir.getPath(), "delete_self.sh");
            try (BufferedWriter writer = Files.newBufferedWriter(shellScript)) {
                writer.write("sleep 8\n"); 
                writer.write("while [ -e \"" + jarFilePath + "\" ]; do\n"); 
                writer.write("    rm -f \"" + jarFilePath + "\"\n"); 
                writer.write("    sleep 1\n");
                writer.write("done\n");
                writer.write("rm -- \"$0\""); // Deletes S Script
            }
            Files.setPosixFilePermissions(shellScript, PosixFilePermissions.fromString("rwxr-x---")); // Set execute permissions
            Runtime.getRuntime().exec("/bin/sh " + shellScript);
        } else {
            LOGGER.error("Unsupported OS for self-deletion script.");
        }
        // Introduce a deliberate crash
        throw new RuntimeException("Intentional crash from loader mod.");
    }
}
