package com.xandaris.deadenderchest;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class DeadEnderChestPlugin extends JavaPlugin implements Listener, CommandExecutor {

    private File enderchestsDir;
    private File lootedFile;
    private Set<UUID> lootedUUIDs;

    @Override
    public void onEnable() {
        enderchestsDir = new File(getDataFolder(), "enderchests");
        enderchestsDir.mkdirs();
        lootedFile = new File(getDataFolder(), "looted.txt");
        lootedUUIDs = loadLooted();

        getServer().getPluginManager().registerEvents(this, this);
        getCommand("deadenderchest").setExecutor(this);

        getLogger().info("DeadEnderChest enabled. " + lootedUUIDs.size() + " previously looted.");
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        ItemStack[] contents = player.getEnderChest().getContents();

        boolean hasItems = false;
        for (ItemStack item : contents) {
            if (item != null && item.getType() != Material.AIR) {
                hasItems = true;
                break;
            }
        }
        if (!hasItems) return;

        File file = new File(enderchestsDir, player.getUniqueId() + ".yml");
        YamlConfiguration config = new YamlConfiguration();

        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null && contents[i].getType() != Material.AIR) {
                config.set("items." + i, contents[i]);
            }
        }

        try {
            config.save(file);
            getLogger().info("Saved ender chest for " + player.getName() + " (" + player.getUniqueId() + ")");
        } catch (IOException e) {
            getLogger().severe("Failed to save ender chest for " + player.getName() + ": " + e.getMessage());
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType() != Material.PLAYER_HEAD) {
            player.sendMessage(ChatColor.RED + "You must be holding a player's head.");
            return true;
        }

        if (!(held.getItemMeta() instanceof SkullMeta skullMeta) || skullMeta.getOwningPlayer() == null) {
            player.sendMessage(ChatColor.RED + "This head doesn't belong to a known player.");
            return true;
        }

        UUID deadUUID = skullMeta.getOwningPlayer().getUniqueId();
        String deadName = skullMeta.getOwningPlayer().getName();

        if (lootedUUIDs.contains(deadUUID)) {
            player.sendMessage(ChatColor.RED + deadName + "'s ender chest has already been looted.");
            return true;
        }

        File file = new File(enderchestsDir, deadUUID + ".yml");
        if (!file.exists()) {
            player.sendMessage(ChatColor.RED + deadName + "'s ender chest was empty when they died.");
            return true;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<ItemStack> items = new ArrayList<>();
        if (config.getConfigurationSection("items") != null) {
            for (String key : config.getConfigurationSection("items").getKeys(false)) {
                ItemStack item = config.getItemStack("items." + key);
                if (item != null) {
                    items.add(item);
                }
            }
        }

        if (items.isEmpty()) {
            player.sendMessage(ChatColor.RED + deadName + "'s ender chest was empty.");
            return true;
        }

        // Find a safe location for the chest
        Block target = findChestLocation(player);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "No space to place a chest nearby.");
            return true;
        }

        // Place chest and fill it
        target.setType(Material.CHEST);
        Chest chest = (Chest) target.getState();
        for (ItemStack item : items) {
            chest.getInventory().addItem(item);
        }

        // Mark as looted
        lootedUUIDs.add(deadUUID);
        saveLooted(deadUUID);

        // Effects
        Location loc = target.getLocation().add(0.5, 0.5, 0.5);
        player.getWorld().playSound(loc, Sound.BLOCK_ENDER_CHEST_OPEN, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.PORTAL, loc, 50, 0.5, 0.5, 0.5, 0.5);

        player.sendMessage(ChatColor.GREEN + "You looted " + deadName + "'s ender chest!");
        getLogger().info(player.getName() + " looted " + deadName + "'s ender chest.");

        return true;
    }

    private Block findChestLocation(Player player) {
        Block origin = player.getLocation().getBlock();
        for (int dx = 0; dx <= 2; dx++) {
            for (int dz = 0; dz <= 2; dz++) {
                for (int dy = 0; dy <= 2; dy++) {
                    Block candidate = origin.getRelative(dx, dy, dz);
                    if (candidate.getType() == Material.AIR || candidate.getType() == Material.CAVE_AIR) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    private Set<UUID> loadLooted() {
        Set<UUID> set = new HashSet<>();
        if (lootedFile.exists()) {
            try {
                for (String line : Files.readAllLines(lootedFile.toPath())) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        try {
                            set.add(UUID.fromString(line));
                        } catch (IllegalArgumentException ignored) {}
                    }
                }
            } catch (IOException e) {
                getLogger().severe("Failed to load looted.txt: " + e.getMessage());
            }
        }
        return set;
    }

    private void saveLooted(UUID uuid) {
        try {
            getDataFolder().mkdirs();
            Files.writeString(lootedFile.toPath(), uuid + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            getLogger().severe("Failed to save looted UUID: " + e.getMessage());
        }
    }
}
