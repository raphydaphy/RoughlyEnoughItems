package me.shedaniel.rei.api;

import me.shedaniel.rei.client.ConfigObject;

import java.io.IOException;

public interface ConfigManager {
    
    void saveConfig() throws IOException;
    
    void loadConfig() throws IOException;
    
    ConfigObject getConfig();
    
    boolean isCraftableOnlyEnabled();
    
    void toggleCraftableOnly();
    
}
