package com.Ragex.mcheliloader;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.*;

//import static cpw.mods.fml.common.Loader.minecraftDir;
//import net.minecraftforge.common.



@Mod(
        modid = "mcheliloader",
        name = "mcheliloader",
        dependencies = "required-after:Forge@[10.13.2.1230,)"
)


public class mcheliloader {
    Path AssetsourceDir = Paths.get("assets/mcheli");

    Path CodesourceDir = Paths.get("code/mcheli");


    @Mod.EventHandler
    public void PreInit(FMLPreInitializationEvent evt) {
        try {
            File configDir = evt.getModConfigurationDirectory();
            File minecraftDir = configDir.getParentFile();
            Path modsDir = Paths.get(minecraftDir.getPath(), "mods");
            Path mcheliDir = Paths.get(modsDir.toString(), "mcheli");
            Path mcheliAssetsDir = Paths.get(modsDir.toString(), "mcheli/assets");
            //Path mcheliCodeDir = Paths.get(modsDir.toString(), "mcheli");
            if (!Files.exists(mcheliDir)) {
                //assets need to go into mods/mcheli/assets/mcheli
                //code needs to go into mods/mcheli/mcheli
                Files.createDirectory(mcheliDir);
                if(!Files.exists(mcheliAssetsDir)) {
                    Files.createDirectory(mcheliAssetsDir);
                }

            }

            System.out.println("Copying from " + CodesourceDir + " to " + mcheliDir);

            Files.walk(CodesourceDir).forEach(source -> {
                Path destination = Paths.get(mcheliDir.toString(), source.toString().substring(CodesourceDir.toString().length()));
                try {
                    Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            System.out.println("Copying from " + AssetsourceDir + " to " + mcheliAssetsDir);


            Files.walk(AssetsourceDir).forEach(source -> {
                Path destination = Paths.get(mcheliAssetsDir.toString(), source.toString().substring(AssetsourceDir.toString().length()));
                try {
                    Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            boolean isCodeCopied = Files.walk(mcheliDir).count() == Files.walk(CodesourceDir).count();
            boolean isAssetsCopied = Files.walk(mcheliAssetsDir).count() == Files.walk(AssetsourceDir).count();

            if (!isCodeCopied || !isAssetsCopied) {
                // Handle the error
                System.out.println("Error: Files were not copied correctly.");
            }



        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}