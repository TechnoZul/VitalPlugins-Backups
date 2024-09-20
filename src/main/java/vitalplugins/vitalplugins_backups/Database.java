package vitalplugins.vitalplugins_backups;

import org.bukkit.plugin.java.JavaPlugin;
import vitalplugins.vitalplugins_backups.utils.Helper;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.Date;

public class Database {

    private final JavaPlugin plugin;
    private final Helper helper;
    private final DataSource dataSource;

    public Database(JavaPlugin plugin) {
        this.plugin = plugin;
        this.helper = new Helper(plugin);
        this.dataSource = setupDataSource();
    }

    private DataSource setupDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(helper.getUrl());
        config.setUsername(helper.getUser());
        config.setPassword(helper.getPassword());
        config.setMaximumPoolSize(10);

        return new HikariDataSource(config);
    }

    private Connection openConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void insert(String nickname, String uuid, String killedBy, Date deathTime, String ping, String backupId, String inventory) throws SQLException {
        String query = "INSERT INTO vitalplugins_backup (uuid, nickname, killed_by, death_time, ping, backup_id, inventory) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE nickname = VALUES(nickname), killed_by = VALUES(killed_by), death_time = VALUES(death_time), ping = VALUES(ping), backup_id = VALUES(backup_id), inventory = VALUES(inventory)";

        try (Connection connection = openConnection(); PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, uuid);
            ps.setString(2, nickname);
            ps.setString(3, killedBy);
            ps.setTimestamp(4, new Timestamp(deathTime.getTime()));
            ps.setString(5, ping);
            ps.setString(6, backupId);
            ps.setString(7, inventory);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Błąd podczas zapisywania backupa: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void delete(String backupId) {
        String query = "DELETE FROM vitalplugins_backup WHERE backup_id = ?";

        try (Connection connection = openConnection();
             PreparedStatement ps = connection.prepareStatement(query)) {

            ps.setString(1, backupId);
            ps.executeUpdate();

        } catch (SQLException e) {
            plugin.getLogger().severe("Błąd podczas usuwania backupa " + backupId + ": " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void createDatabase() {
        String query = "CREATE TABLE IF NOT EXISTS vitalplugins_backup (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "uuid VARCHAR(36) NOT NULL, " +
                "nickname VARCHAR(64) NOT NULL, " +
                "killed_by VARCHAR(64), " +
                "death_time TIMESTAMP, " +
                "ping VARCHAR(5) NOT NULL, " +
                "backup_id VARCHAR(36) NOT NULL, " +
                "inventory TEXT, " +
                "UNIQUE (uuid, backup_id))";

        try (Connection connection = openConnection(); Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(query);
        } catch (SQLException e) {
            plugin.getLogger().severe("Błąd podczas tworzenia bazy danych: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public List<String> getBackupInfo(String whatCheck, String whatGet, String value) {
        List<String> backupInfoList = new ArrayList<>();
        String query = "SELECT " + whatGet + " FROM vitalplugins_backup WHERE " + whatCheck + " = ? ORDER BY id ASC";

        try (Connection connection = openConnection();
             PreparedStatement ps = connection.prepareStatement(query)) {

            ps.setString(1, value);

            try (ResultSet resultSet = ps.executeQuery()) {
                while (resultSet.next()) {
                    backupInfoList.add(resultSet.getString(whatGet));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Błąd podczas pobierania informacji o backupie: " + e.getMessage());
            throw new RuntimeException(e);
        }

        return backupInfoList;
    }

    public String getInventoryJson(String backupId) {
        String query = "SELECT inventory FROM vitalplugins_backup WHERE backup_id = ?";
        String inventoryJson = null;

        try (Connection connection = openConnection();
             PreparedStatement ps = connection.prepareStatement(query)) {

            ps.setString(1, backupId);

            try (ResultSet resultSet = ps.executeQuery()) {
                if (resultSet.next()) {
                    inventoryJson = resultSet.getString("inventory");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Błąd podczas pobierania JSON: " + e.getMessage());
            throw new RuntimeException(e);
        }

        return inventoryJson;
    }
}
