package net.turkeynw.tkupon;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CouponCommand implements CommandExecutor, TabCompleter {

    private final TKupon plugin;
    private final SessionListener sessionListener;

    public CouponCommand(TKupon plugin, SessionListener sessionListener) {
        this.plugin = plugin;
        this.sessionListener = sessionListener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("tkupon.admin")) {
                player.sendMessage(plugin.getFileManager().getMessages().getString("errors.no-permission").replace("&", "§"));
                return true;
            }
            plugin.getFileManager().reloadFiles();
            player.sendMessage("§aTKupon ayarları başarıyla yenilendi!");
            return true;
        }

        if (args[0].equalsIgnoreCase("düzenle") && args.length == 2) {
            if (!player.hasPermission("tkupon.admin")) {
                player.sendMessage(plugin.getFileManager().getMessages().getString("errors.no-permission").replace("&", "§"));
                return true;
            }

            String kod = args[1];
            Inventory gui = Bukkit.createInventory(null, 54, "Kupon Düzenle: " + kod);

            String base64 = plugin.getFileManager().getCoupons().getString("coupons." + kod + ".items");
            if (base64 != null) {
                try {
                    ItemStack[] items = Base64Utils.itemStackArrayFromBase64(base64);
                    gui.setContents(items);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            plugin.getFoliaLib().getImpl().runNextTick((task) -> {
                player.openInventory(gui);
            });
            return true;
        }

        if (args[0].equalsIgnoreCase("kullan") && args.length == 2) {
            String kod = args[1];
            String base64 = plugin.getFileManager().getCoupons().getString("coupons." + kod + ".items");

            if (base64 == null) {
                player.sendMessage(plugin.getFileManager().getMessages().getString("errors.invalid-coupon").replace("&", "§"));
                return true;
            }

            plugin.getFoliaLib().getImpl().runAsync((task) -> {
                try {
                    Connection connection = plugin.getDatabaseManager().getConnection();

                    PreparedStatement checkUsed = connection.prepareStatement("SELECT ID FROM Kupon_Kullanimlari WHERE Oyuncu_UUID = ? AND Kupon_Kodu = ?");
                    checkUsed.setString(1, player.getUniqueId().toString());
                    checkUsed.setString(2, kod);
                    ResultSet usedResult = checkUsed.executeQuery();

                    if (usedResult.next()) {
                        player.sendMessage(plugin.getFileManager().getMessages().getString("errors.already-used").replace("&", "§"));
                        usedResult.close();
                        checkUsed.close();
                        return;
                    }
                    usedResult.close();
                    checkUsed.close();

                    String dbType = plugin.getFileManager().getConfig().getString("database.type", "SQLITE").toUpperCase();
                    String limitQuery;

                    if (dbType.equals("MYSQL") || dbType.equals("MARIADB")) {
                        limitQuery = "SELECT COUNT(*) FROM Kupon_Kullanimlari WHERE Oyuncu_UUID = ? AND MONTH(Kullanma_Tarihi) = ? AND YEAR(Kullanma_Tarihi) = ?";
                    } else {
                        limitQuery = "SELECT COUNT(*) FROM Kupon_Kullanimlari WHERE Oyuncu_UUID = ? AND cast(strftime('%m', Kullanma_Tarihi) as integer) = ? AND cast(strftime('%Y', Kullanma_Tarihi) as integer) = ?";
                    }

                    PreparedStatement checkLimit = connection.prepareStatement(limitQuery);
                    checkLimit.setString(1, player.getUniqueId().toString());
                    checkLimit.setInt(2, Calendar.getInstance().get(Calendar.MONTH) + 1);
                    checkLimit.setInt(3, Calendar.getInstance().get(Calendar.YEAR));
                    ResultSet limitResult = checkLimit.executeQuery();

                    int maxUsage = plugin.getFileManager().getConfig().getInt("max-usage-per-month");

                    if (limitResult.next() && limitResult.getInt(1) >= maxUsage) {
                        player.sendMessage(plugin.getFileManager().getMessages().getString("errors.limit-reached").replace("&", "§"));
                        limitResult.close();
                        checkLimit.close();
                        return;
                    }
                    limitResult.close();
                    checkLimit.close();

                    plugin.getFoliaLib().getImpl().runNextTick((syncTask) -> {
                        try {
                            ItemStack[] items = Base64Utils.itemStackArrayFromBase64(base64);
                            boolean hasSpace = false;

                            for (ItemStack item : player.getInventory().getStorageContents()) {
                                if (item == null) {
                                    hasSpace = true;
                                    break;
                                }
                            }

                            if (!hasSpace) {
                                player.sendMessage(plugin.getFileManager().getMessages().getString("errors.inventory-full").replace("&", "§"));
                                return;
                            }

                            for (ItemStack item : items) {
                                if (item != null) {
                                    player.getInventory().addItem(item);
                                }
                            }

                            player.sendMessage(plugin.getFileManager().getMessages().getString("success.coupon-claimed").replace("&", "§"));

                            // Bilgilendirme Mesajları (Chat ve Actionbar)
                            int targetHours = plugin.getFileManager().getConfig().getInt("delayed-rewards.required-playtime-hours");

                            String chatReminder = plugin.getFileManager().getMessages().getString("success.reward-reminder-chat");
                            if (chatReminder != null && !chatReminder.isEmpty()) {
                                player.sendMessage(chatReminder.replace("&", "§").replace("%hours%", String.valueOf(targetHours)));
                            }

                            String actionbarReminder = plugin.getFileManager().getMessages().getString("success.reward-reminder-actionbar");
                            if (actionbarReminder != null && !actionbarReminder.isEmpty()) {
                                player.sendActionBar(actionbarReminder.replace("&", "§").replace("%hours%", String.valueOf(targetHours)));
                            }

                            plugin.getFoliaLib().getImpl().runAsync((insertTask) -> {
                                try {
                                    PreparedStatement insertUsage = connection.prepareStatement("INSERT INTO Kupon_Kullanimlari (Kupon_Kodu, Oyuncu_Ismi, Oyuncu_UUID, IP_Adresi) VALUES (?, ?, ?, ?)");
                                    insertUsage.setString(1, kod);
                                    insertUsage.setString(2, player.getName());
                                    insertUsage.setString(3, player.getUniqueId().toString());
                                    insertUsage.setString(4, player.getAddress().getAddress().getHostAddress());
                                    insertUsage.executeUpdate();
                                    insertUsage.close();
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                }
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });

                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
            return true;
        }

        if (args[0].equalsIgnoreCase("süre")) {
            plugin.getFoliaLib().getImpl().runAsync((task) -> {
                try {
                    Connection connection = plugin.getDatabaseManager().getConnection();
                    PreparedStatement statement = connection.prepareStatement("SELECT Aktif_Sure_Saniye, VIP_Alindi_Mi FROM Kupon_Kullanimlari WHERE Oyuncu_UUID = ? ORDER BY ID DESC LIMIT 1");
                    statement.setString(1, player.getUniqueId().toString());
                    ResultSet resultSet = statement.executeQuery();

                    if (resultSet.next()) {
                        boolean vipAlindi = resultSet.getBoolean("VIP_Alindi_Mi");
                        if (vipAlindi) {
                            player.sendMessage(plugin.getFileManager().getMessages().getString("time.already-claimed").replace("&", "§"));
                        } else {
                            long dbSeconds = resultSet.getLong("Aktif_Sure_Saniye");
                            Long loginTime = sessionListener.getLoginTimes().get(player.getUniqueId());
                            long sessionSeconds = loginTime != null ? (System.currentTimeMillis() - loginTime) / 1000 : 0;

                            long totalSeconds = dbSeconds + sessionSeconds;
                            int targetHours = plugin.getFileManager().getConfig().getInt("delayed-rewards.required-playtime-hours");
                            long targetSeconds = targetHours * 3600L;

                            long remainingSeconds = targetSeconds - totalSeconds;

                            if (remainingSeconds > 0) {
                                long hours = remainingSeconds / 3600;
                                long minutes = (remainingSeconds % 3600) / 60;
                                String msg = plugin.getFileManager().getMessages().getString("time.remaining")
                                        .replace("&", "§")
                                        .replace("%hours%", String.valueOf(hours))
                                        .replace("%minutes%", String.valueOf(minutes));
                                player.sendMessage(msg);
                            } else {
                                player.sendMessage(plugin.getFileManager().getMessages().getString("time.completed").replace("&", "§"));
                            }
                        }
                    } else {
                        player.sendMessage(plugin.getFileManager().getMessages().getString("errors.no-usage-found").replace("&", "§"));
                    }
                    resultSet.close();
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
            return true;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        List<String> commands = new ArrayList<>();

        if (args.length == 1) {
            commands.add("kullan");
            commands.add("süre");
            if (sender.hasPermission("tkupon.admin")) {
                commands.add("düzenle");
                commands.add("reload");
            }
            for (String c : commands) {
                if (c.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(c);
                }
            }
            return completions;
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("düzenle") || args[0].equalsIgnoreCase("kullan"))) {
            completions.add("<Kupon_Kodu>");
            return completions;
        }

        return completions;
    }
}