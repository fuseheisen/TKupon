package net.turkeynw.tkupon;

import com.tcoded.folialib.FoliaLib;
import org.bukkit.plugin.java.JavaPlugin;

public final class TKupon extends JavaPlugin {

    private FoliaLib foliaLib;
    private FileManager fileManager;
    private DatabaseManager databaseManager;
    private SessionListener sessionListener;

    @Override
    public void onEnable() {
        this.foliaLib = new FoliaLib(this);

        this.fileManager = new FileManager(this);
        this.fileManager.setup();

        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.connect();

        this.sessionListener = new SessionListener(this);
        getServer().getPluginManager().registerEvents(this.sessionListener, this);
        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);

        CouponCommand couponCommand = new CouponCommand(this, this.sessionListener);
        getCommand("kupon").setExecutor(couponCommand);
        getCommand("kupon").setTabCompleter(couponCommand);

        RewardTask rewardTask = new RewardTask(this, this.sessionListener);
        rewardTask.startTask();
    }

    @Override
    public void onDisable() {
        // Sunucu aniden kapanırsa içerideki oyuncuların süresini kurtar/kaydet
        if (this.sessionListener != null && this.databaseManager != null) {
            for (org.bukkit.entity.Player player : getServer().getOnlinePlayers()) {
                java.util.UUID uuid = player.getUniqueId();
                if (sessionListener.getLoginTimes().containsKey(uuid)) {
                    long loginTime = sessionListener.getLoginTimes().remove(uuid);
                    long playedSeconds = (System.currentTimeMillis() - loginTime) / 1000;
                    try {
                        java.sql.Connection connection = databaseManager.getConnection();
                        if (connection != null && !connection.isClosed()) {
                            java.sql.PreparedStatement statement = connection.prepareStatement(
                                    "UPDATE Kupon_Kullanimlari SET Aktif_Sure_Saniye = Aktif_Sure_Saniye + ? WHERE Oyuncu_UUID = ? AND VIP_Alindi_Mi = FALSE"
                            );
                            statement.setLong(1, playedSeconds);
                            statement.setString(2, uuid.toString());
                            statement.executeUpdate();
                            statement.close();
                        }
                    } catch (Exception ignored) {}
                }
            }
        }

        // Veritabanı bağlantısını güvenlice kapat
        if (this.databaseManager != null) {
            this.databaseManager.disconnect();
        }
    }

    public FoliaLib getFoliaLib() {
        return foliaLib;
    }

    public FileManager getFileManager() {
        return fileManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}