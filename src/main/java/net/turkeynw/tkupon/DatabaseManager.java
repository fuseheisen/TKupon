package net.turkeynw.tkupon;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private final TKupon plugin;
    private Connection connection;

    public DatabaseManager(TKupon plugin) {
        this.plugin = plugin;
    }

    public void connect() {
        plugin.getFoliaLib().getImpl().runAsync((task) -> {
            try {
                String dbType = plugin.getFileManager().getConfig().getString("database.type", "SQLITE").toUpperCase();

                if (dbType.equals("MYSQL") || dbType.equals("MARIADB")) {
                    String host = plugin.getFileManager().getConfig().getString("database.mysql.host");
                    String port = plugin.getFileManager().getConfig().getString("database.mysql.port");
                    String database = plugin.getFileManager().getConfig().getString("database.mysql.database");
                    String username = plugin.getFileManager().getConfig().getString("database.mysql.username");
                    String password = plugin.getFileManager().getConfig().getString("database.mysql.password");

                    // MariaDB ve MySQL için evrensel JDBC URL
                    String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?autoReconnect=true&useSSL=false";
                    connection = DriverManager.getConnection(url, username, password);

                } else {
                    // Varsayılan olarak SQLite (Yerel Dosya) kullanılır.
                    File dataFolder = new File(plugin.getDataFolder(), "database.db");
                    if (!dataFolder.exists()) {
                        try {
                            dataFolder.createNewFile();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    Class.forName("org.sqlite.JDBC");
                    String url = "jdbc:sqlite:" + dataFolder;
                    connection = DriverManager.getConnection(url);
                }

                createTables(dbType);
            } catch (SQLException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        });
    }

    private void createTables(String dbType) {
        try (Statement statement = connection.createStatement()) {

            // SQLite ve MySQL/MariaDB'nin AUTO_INCREMENT mantığı ufak bir farklılık gösterir.
            String autoIncrement = dbType.equals("SQLITE") ? "AUTOINCREMENT" : "AUTO_INCREMENT";

            String createListTable = "CREATE TABLE IF NOT EXISTS Kupon_Listesi (" +
                    "ID INTEGER PRIMARY KEY " + autoIncrement + ", " +
                    "Kupon_Kodu VARCHAR(50) NOT NULL, " +
                    "Olusturan_Yetkili VARCHAR(50) NOT NULL, " +
                    "Olusturulma_Tarihi TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ");";
            statement.execute(createListTable);

            String createUsageTable = "CREATE TABLE IF NOT EXISTS Kupon_Kullanimlari (" +
                    "ID INTEGER PRIMARY KEY " + autoIncrement + ", " +
                    "Kupon_Kodu VARCHAR(50) NOT NULL, " +
                    "Oyuncu_Ismi VARCHAR(50) NOT NULL, " +
                    "Oyuncu_UUID VARCHAR(36) NOT NULL, " +
                    "IP_Adresi VARCHAR(45) NOT NULL, " +
                    "Kullanma_Tarihi TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "VIP_Alindi_Mi BOOLEAN DEFAULT FALSE, " +
                    "Aktif_Sure_Saniye INT DEFAULT 0" +
                    ");";
            statement.execute(createUsageTable);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        return connection;
    }
}