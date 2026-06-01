package net.turkeynw.tkupon;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class FileManager {

    private final TKupon plugin;
    private File configFile, messagesFile, couponsFile;
    private FileConfiguration configConfig, messagesConfig, couponsConfig;

    public FileManager(TKupon plugin) {
        this.plugin = plugin;
    }

    public void setup() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdir();
        }

        configFile = new File(plugin.getDataFolder(), "config.yml");
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        couponsFile = new File(plugin.getDataFolder(), "coupons.yml");

        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        if (!couponsFile.exists()) {
            try {
                couponsFile.createNewFile();
            } catch (IOException ignored) {}
        }

        configConfig = YamlConfiguration.loadConfiguration(configFile);
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        couponsConfig = YamlConfiguration.loadConfiguration(couponsFile);
    }

    public FileConfiguration getConfig() {
        return configConfig;
    }

    public FileConfiguration getMessages() {
        return messagesConfig;
    }

    public FileConfiguration getCoupons() {
        return couponsConfig;
    }

    public void saveCoupons() {
        try {
            couponsConfig.save(couponsFile);
        } catch (IOException ignored) {}
    }

    public void reloadFiles() {
        configConfig = YamlConfiguration.loadConfiguration(configFile);
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        couponsConfig = YamlConfiguration.loadConfiguration(couponsFile);
    }
}