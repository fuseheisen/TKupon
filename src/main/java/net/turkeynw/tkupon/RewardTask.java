package net.turkeynw.tkupon;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class RewardTask {

    private final TKupon plugin;
    private final SessionListener sessionListener;

    public RewardTask(TKupon plugin, SessionListener sessionListener) {
        this.plugin = plugin;
        this.sessionListener = sessionListener;
    }

    public void startTask() {
        int requiredHours = plugin.getFileManager().getConfig().getInt("delayed-rewards.required-playtime-hours");
        long requiredSeconds = requiredHours * 3600L;

        plugin.getFoliaLib().getImpl().runTimerAsync((task) -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                Long loginTime = sessionListener.getLoginTimes().get(player.getUniqueId());
                if (loginTime == null) continue;

                long sessionSeconds = (System.currentTimeMillis() - loginTime) / 1000;

                try {
                    Connection connection = plugin.getDatabaseManager().getConnection();
                    PreparedStatement statement = connection.prepareStatement(
                            "SELECT ID, Aktif_Sure_Saniye FROM Kupon_Kullanimlari WHERE Oyuncu_UUID = ? AND VIP_Alindi_Mi = FALSE"
                    );
                    statement.setString(1, player.getUniqueId().toString());
                    ResultSet resultSet = statement.executeQuery();

                    while (resultSet.next()) {
                        int id = resultSet.getInt("ID");
                        long dbSeconds = resultSet.getLong("Aktif_Sure_Saniye");

                        if ((dbSeconds + sessionSeconds) >= requiredSeconds) {
                            PreparedStatement update = connection.prepareStatement(
                                    "UPDATE Kupon_Kullanimlari SET VIP_Alindi_Mi = TRUE WHERE ID = ?"
                            );
                            update.setInt(1, id);
                            update.executeUpdate();
                            update.close();

                            plugin.getFoliaLib().getImpl().runNextTick((syncTask) -> {
                                if (player.isOnline()) {
                                    List<String> commands = plugin.getFileManager().getConfig().getStringList("delayed-rewards.commands");
                                    for (String cmd : commands) {
                                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", player.getName()));
                                    }

                                    String message = plugin.getFileManager().getConfig().getString("delayed-rewards.messages.chat");
                                    if (message != null) {
                                        player.sendMessage(message.replace("&", "§"));
                                    }
                                }
                            });
                        }
                    }
                    resultSet.close();
                    statement.close();

                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }, 6000L, 6000L);
    }
}