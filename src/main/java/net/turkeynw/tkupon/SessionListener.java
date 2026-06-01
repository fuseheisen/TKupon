package net.turkeynw.tkupon;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionListener implements Listener {

    private final TKupon plugin;
    private final Map<UUID, Long> loginTimes;

    public SessionListener(TKupon plugin) {
        this.plugin = plugin;
        this.loginTimes = new ConcurrentHashMap<>();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        loginTimes.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (loginTimes.containsKey(uuid)) {
            long loginTime = loginTimes.remove(uuid);
            long playedSeconds = (System.currentTimeMillis() - loginTime) / 1000;

            plugin.getFoliaLib().getImpl().runAsync((task) -> {
                try {
                    Connection connection = plugin.getDatabaseManager().getConnection();
                    PreparedStatement statement = connection.prepareStatement(
                            "UPDATE Kupon_Kullanimlari SET Aktif_Sure_Saniye = Aktif_Sure_Saniye + ? WHERE Oyuncu_UUID = ? AND VIP_Alindi_Mi = FALSE"
                    );

                    statement.setLong(1, playedSeconds);
                    statement.setString(2, uuid.toString());
                    statement.executeUpdate();
                    statement.close();

                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public Map<UUID, Long> getLoginTimes() {
        return loginTimes;
    }
}