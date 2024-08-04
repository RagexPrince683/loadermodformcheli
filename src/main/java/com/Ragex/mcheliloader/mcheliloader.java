package com.Ragex.mcheliloader;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Mod(
        modid = "mcheliloader",
        name = "mcheliloader",
        dependencies = "required-after:Forge@[10.13.2.1230,)"
)
public class mcheliloader {
    private static final Logger LOGGER = LogManager.getLogger(mcheliloader.class.getName());

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        try {
            File configDir = event.getModConfigurationDirectory();
            File minecraftDir = configDir.getParentFile();
            Path modsDir = Paths.get(minecraftDir.getPath(), "mods");
            Path mcheliDir = modsDir.resolve("mcheli");
            Path mcheliAssetsDir = mcheliDir.resolve("assets/mcheli");
            Path mcheliCodeDir = mcheliDir.resolve("mcheli");

            createDirectories(mcheliDir, mcheliAssetsDir, mcheliCodeDir);

            copyDirectoryFromJar("assets/mcheli", mcheliAssetsDir);
            copyDirectoryFromJar("code/mcheli", mcheliCodeDir);

            validateFiles("assets/mcheli", mcheliAssetsDir);
            validateFiles("code/mcheli", mcheliCodeDir);

        } catch (Exception e) {
            LOGGER.error("An error occurred during pre-initialization.", e);
        }
    }

    private void createDirectories(Path... paths) throws IOException {
        for (Path path : paths) {
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                LOGGER.debug("Created directory: " + path);
            }
        }
    }

    private void copyDirectoryFromJar(String sourceDir, Path targetDir) throws IOException, URISyntaxException {
        // Remove leading slash if present
        if (sourceDir.startsWith("/")) {
            sourceDir = sourceDir.substring(1);
        }
        try (ZipFile jarFile = new ZipFile(new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI()))) {
            Enumeration<? extends ZipEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().startsWith(sourceDir)) {
                    String relativePath = entry.getName().substring(sourceDir.length());
                    Path targetPath = targetDir.resolve(relativePath).normalize();

                    if (entry.isDirectory()) {
                        Files.createDirectories(targetPath);
                    } else {
                        try (InputStream is = jarFile.getInputStream(entry)) {
                            Files.createDirectories(targetPath.getParent());
                            Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            LOGGER.error("Failed to copy file: " + entry.getName() + " to " + targetPath, e);
                            throw e;
                        }
                    }
                    LOGGER.debug("Copied file: " + entry.getName() + " to " + targetPath);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to copy directory from jar: " + sourceDir, e);
            throw new IOException("Failed to copy directory from jar: " + sourceDir, e);
        }
    }

    private void validateFiles(String sourceDir, Path targetDir) throws IOException, URISyntaxException {
        long originalFiles = 0;
        // Remove leading slash if present
        if (sourceDir.startsWith("/")) {
            sourceDir = sourceDir.substring(1);
        }
        try (ZipFile jarFile = new ZipFile(new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI()))) {
            Enumeration<? extends ZipEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().startsWith(sourceDir) && !entry.isDirectory()) {
                    originalFiles++;
                }
            }
        }
        long copiedFiles = Files.walk(targetDir).filter(Files::isRegularFile).count();

        if (originalFiles != copiedFiles) {
            LOGGER.error("Error: Not all files were copied correctly. Source: " + originalFiles + " files, Target: " + copiedFiles + " files.");
        } else {
            LOGGER.info("All files copied successfully from " + sourceDir + " to " + targetDir);
        }
    }
}