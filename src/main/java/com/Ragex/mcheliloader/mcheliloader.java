package com.Ragex.mcheliloader;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicBoolean;

@Mod(
        modid = "mcheliloader",
        name = "mcheliloader",
        dependencies = "required-after:Forge@[10.13.2.1230,)"
)
public class mcheliloader {
    private static final Path ASSET_SOURCE_DIR = Paths.get("assets/mcheli");
    private static final Path CODE_SOURCE_DIR = Paths.get("code/mcheli");

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

            copyDirectory(CODE_SOURCE_DIR, mcheliCodeDir);
            copyDirectory(ASSET_SOURCE_DIR, mcheliAssetsDir);

            validateFiles(CODE_SOURCE_DIR, mcheliCodeDir);
            validateFiles(ASSET_SOURCE_DIR, mcheliAssetsDir);

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

    private void copyDirectory(Path sourceDir, Path targetDir) throws IOException {
        Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path targetFile = targetDir.resolve(sourceDir.relativize(file));
                Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                LOGGER.debug("Copied file: " + file + " to " + targetFile);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) {
                    throw exc;
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void validateFiles(Path sourceDir, Path targetDir) throws IOException {
        long originalFiles = Files.walk(sourceDir).filter(Files::isRegularFile).count();
        long copiedFiles = Files.walk(targetDir).filter(Files::isRegularFile).count();

        if (originalFiles != copiedFiles) {
            LOGGER.error("Error: Not all files were copied correctly. Source: " + originalFiles + " files, Target: " + copiedFiles + " files.");
        } else {
            LOGGER.info("All files copied successfully from " + sourceDir + " to " + targetDir);
        }
    }
}