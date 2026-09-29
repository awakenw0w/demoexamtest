package ru.demoexam.algorithms.auth;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** Отвечает за создание базы пользователей и проверку данных для входа. */
public class UserRepository {
    private static final String DATABASE_URL = "jdbc:sqlite:users.db";
    private static final int MAX_FAILED_ATTEMPTS = 3;

    /** Подготавливает таблицу и добавляет демо-учётки при первом запуске. */
    public UserRepository() throws SQLException {
        createTable();
        addLockoutColumnsIfNeeded();
        addDemoUsersIfDatabaseIsEmpty();
    }

    /** Создаёт минимальную таблицу пользователей, если её ещё нет. */
    private void createTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS users ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "login TEXT NOT NULL UNIQUE, "
                + "password_hash TEXT NOT NULL, "
                + "role TEXT NOT NULL, "
                + "failed_attempts INTEGER NOT NULL DEFAULT 0, "
                + "is_blocked INTEGER NOT NULL DEFAULT 0)";

        try (Connection connection = DriverManager.getConnection(DATABASE_URL);
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    /** Добавляет поля блокировки в старую таблицу, созданную в первый день. */
    private void addLockoutColumnsIfNeeded() throws SQLException {
        if (!columnExists("failed_attempts")) {
            addColumn("failed_attempts INTEGER NOT NULL DEFAULT 0");
        }
        if (!columnExists("is_blocked")) {
            addColumn("is_blocked INTEGER NOT NULL DEFAULT 0");
        }
    }

    /** Проверяет по описанию таблицы, есть ли в ней указанный столбец. */
    private boolean columnExists(String columnName) throws SQLException {
        try (Connection connection = DriverManager.getConnection(DATABASE_URL);
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("PRAGMA table_info(users)")) {
            while (result.next()) {
                if (columnName.equals(result.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Добавляет в таблицу один новый столбец. */
    private void addColumn(String columnDefinition) throws SQLException {
        try (Connection connection = DriverManager.getConnection(DATABASE_URL);
             Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE users ADD COLUMN " + columnDefinition);
        }
    }

    /** Добавляет администратора и пользователя только в новую пустую базу. */
    private void addDemoUsersIfDatabaseIsEmpty() throws SQLException {
        String countSql = "SELECT COUNT(*) FROM users";
        boolean isEmpty;

        try (Connection connection = DriverManager.getConnection(DATABASE_URL);
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(countSql)) {
            isEmpty = result.next() && result.getInt(1) == 0;
        }

        // Добавляем демо-пользователей только после закрытия запроса к таблице.
        if (isEmpty) {
            try (Connection connection = DriverManager.getConnection(DATABASE_URL)) {
                addUser(connection, "admin", "Admin123!", "admin");
                addUser(connection, "user", "User123!", "user");
            }
        }
    }

    /** Добавляет одну запись с хешированным паролем. */
    private void addUser(Connection connection, String login, String password, String role)
            throws SQLException {
        String sql = "INSERT INTO users (login, password_hash, role) VALUES (?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, login);
            statement.setString(2, PasswordHasher.hashPassword(password));
            statement.setString(3, role);
            statement.executeUpdate();
        }
    }

    /** Проверяет пару логин/пароль по записи в SQLite. */
    public boolean authenticate(String login, String password) throws SQLException {
        String sql = "SELECT password_hash FROM users WHERE login = ?";

        try (Connection connection = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, login);

            try (ResultSet result = statement.executeQuery()) {
                return result.next()
                        && PasswordHasher.passwordMatches(password, result.getString("password_hash"));
            }
        }
    }

    /** Проверяет, заблокирована ли учётная запись с таким логином. */
    public boolean isBlocked(String login) throws SQLException {
        String sql = "SELECT is_blocked FROM users WHERE login = ?";

        try (Connection connection = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, login);

            try (ResultSet result = statement.executeQuery()) {
                return result.next() && result.getInt("is_blocked") == 1;
            }
        }
    }

    /** Считает ошибку и возвращает true, если запись теперь заблокирована. */
    public boolean recordFailedAttempt(String login) throws SQLException {
        String sql = "UPDATE users SET failed_attempts = failed_attempts + 1, "
                + "is_blocked = CASE WHEN failed_attempts + 1 >= "
                + MAX_FAILED_ATTEMPTS + " THEN 1 ELSE 0 END "
                + "WHERE login = ?";

        try (Connection connection = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, login);
            statement.executeUpdate();
        }

        return isBlocked(login);
    }

    /** Обнуляет ошибки после успешной авторизации. */
    public void resetFailedAttempts(String login) throws SQLException {
        String sql = "UPDATE users SET failed_attempts = 0, is_blocked = 0 WHERE login = ?";

        try (Connection connection = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, login);
            statement.executeUpdate();
        }
    }
}
