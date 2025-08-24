package com.Ragex.mcheliloader;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import net.minecraft.client.gui.GuiButton;
import net.minecraftforge.client.event.GuiScreenEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

import java.io.*;
import java.net.URL;
//modrinth.
//god I fucking hate this project
@Mod(modid = "mcho_installer", version = "1.0")
public class mcheliloadermodrinth {

    private static final String DOWNLOAD_URL = "https://github.com/RagexPrince683/loadermodformcheli/releases/download/KILL/mcheliloader-MCHO+v1.8.2.jar";
    private static final String TARGET_NAME = "MCHO.jar";
    private static boolean installed = false;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        File modsDir = new File(event.getModConfigurationDirectory().getParentFile(), "mods");
        File target = new File(modsDir, TARGET_NAME);

        if (!target.exists()) {
            try {
                downloadFile(DOWNLOAD_URL, target);
                installed = true;

                // Register GUI hook to tell user to restart
                FMLCommonHandler.instance().bus().register(this);

            } catch (IOException e) {
                throw new RuntimeException("Failed to download MCHO from " + DOWNLOAD_URL, e);
            }
        }
    }

    private void downloadFile(String urlStr, File target) throws IOException {
        URL url = new URL(urlStr);
        try (InputStream in = url.openStream();
             FileOutputStream out = new FileOutputStream(target)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }

    // Hook GUI open event after main menu shows up
    @SubscribeEvent
    public void onGuiOpen(GuiScreenEvent.InitGuiEvent.Post event) {
        if (installed && event.gui instanceof net.minecraft.client.gui.GuiMainMenu) {
            event.buttonList.add(new GuiButton(9999, event.gui.width / 2 - 100, event.gui.height / 4 + 120, "MCHO installed - Restart"));
        }
    }

    // Handle button click
    @SubscribeEvent
    public void onButtonClick(GuiScreenEvent.ActionPerformedEvent.Post event) {
        if (event.button.id == 9999) {
            throw new RuntimeException("MCHO installed successfully. Please restart Minecraft.");
        }
    }
}
