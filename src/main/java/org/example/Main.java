package org.example;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.sql.*;
import java.util.*;

public class Main {

    // ==== НАСТРОЙКИ БАЗЫ ДАННЫХ ====
    // Используем ту же БД и пользователя, что и во 2-м задании
    private static final String DB_URL =
            "jdbc:mysql://localhost:3306/string_menu?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DB_USER = "app";
    private static final String DB_PASS = "app123";

    public static void main(String[] args) {
        try (Connection cn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Scanner sc = new Scanner(System.in)) {

            ensureResultsTable(cn);

            while (true) {
                printMenu();
                System.out.print("Ваш выбор (0 — выход): ");
                String choice = sc.nextLine().trim();

                switch (choice) {
                    case "0" -> {
                        System.out.println("Выход. До встречи!");
                        return;
                    }
                    case "1" -> listAllTables(cn);
                    case "2" -> {
                        ensureResultsTable(cn);
                        System.out.println("Таблица int_results создана/проверена.");
                    }
                    case "3" -> runBaseTask(sc, cn);
                    case "4" -> exportToExcel(cn, "int_results.xlsx");
                    default -> System.out.println("Неизвестная команда. Повторите ввод.");
                }
                System.out.println();
            }

        } catch (SQLException e) {
            System.err.println("Ошибка БД: " + e.getMessage());
        }
    }

    // ===== МЕНЮ =====

    private static void printMenu() {
        System.out.println("====================================");
        System.out.println("1.  Вывести все таблицы из MySQL.");
        System.out.println("2.  Создать таблицу в MySQL.");
        System.out.println("3.  Выполнение задачи базового варианта, результат сохранить в MySQL с последующим выводом в консоль.");
        System.out.println("4.  Сохранить все данные (вышеполученные результаты) из MySQL в Excel и вывести на экран.");
        System.out.println("====================================");
    }

    // ===== СОЗДАНИЕ ТАБЛИЦЫ =====

    private static void ensureResultsTable(Connection cn) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS int_results (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    original_text VARCHAR(64) NOT NULL,
                    is_integer TINYINT(1) NOT NULL,
                    int_value INT NULL,
                    is_even TINYINT(1) NULL,
                    note VARCHAR(255) NULL
                )
                """;
        try (Statement st = cn.createStatement()) {
            st.execute(sql);
        }
    }

    // ===== ПУНКТ 1: СПИСОК ТАБЛИЦ =====

    private static void listAllTables(Connection cn) throws SQLException {
        String sql = """
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                ORDER BY table_name
                """;
        try (Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            System.out.println("Таблицы в текущей базе:");
            boolean any = false;
            while (rs.next()) {
                any = true;
                System.out.println(" - " + rs.getString(1));
            }
            if (!any) System.out.println("(нет таблиц)");
        }
    }

    // ===== ПУНКТ 3: БАЗОВАЯ ЗАДАЧА =====

    private static void runBaseTask(Scanner sc, Connection cn) {
        System.out.println("Введите несколько значений через пробел (например: 10 3.5 abc -4 0):");
        System.out.print("Ввод: ");
        String line = sc.nextLine().trim();

        if (line.isEmpty()) {
            System.out.println("Пустой ввод, ничего не проверяем.");
            return;
        }

        String[] tokens = line.split("\\s+");
        for (String token : tokens) {
            checkOneValue(token, cn);
        }
    }

    private static void checkOneValue(String token, Connection cn) {
        boolean isInteger;
        Integer intValue = null;
        Boolean isEven = null;
        String note = null;

        try {
            int v = Integer.parseInt(token);
            isInteger = true;
            intValue = v;
            isEven = (v % 2 == 0);

            String msg = String.format(
                    "Ввод: '%s' -> целое: ДА, значение: %d, чётное: %s",
                    token, v, isEven ? "ДА" : "НЕТ"
            );
            System.out.println(msg);

        } catch (NumberFormatException ex) {
            isInteger = false;
            note = "not an integer";
            System.out.printf("Ввод: '%s' -> не целое число или не число%n", token);
        }

        insertResult(cn, token, isInteger, intValue, isEven, note);
    }

    // ===== ВСТАВКА РЕЗУЛЬТАТА В БД =====

    private static void insertResult(Connection cn,
                                     String originalText,
                                     boolean isInteger,
                                     Integer intValue,
                                     Boolean isEven,
                                     String note) {
        String sql = """
                INSERT INTO int_results(original_text, is_integer, int_value, is_even, note)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, originalText);
            ps.setBoolean(2, isInteger);

            if (intValue != null) {
                ps.setInt(3, intValue);
            } else {
                ps.setNull(3, Types.INTEGER);
            }

            if (isEven != null) {
                ps.setBoolean(4, isEven);
            } else {
                ps.setNull(4, Types.TINYINT);
            }

            if (note != null) {
                ps.setString(5, note);
            } else {
                ps.setNull(5, Types.VARCHAR);
            }

            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Ошибка вставки результата: " + e.getMessage());
        }
    }

    // ===== ВЫБОРКА РЕЗУЛЬТАТОВ ДЛЯ ЭКСПОРТА =====

    private static List<Map<String, Object>> selectAllResults(Connection cn) throws SQLException {
        String sql = """
                SELECT id, created_at, original_text, is_integer, int_value, is_even, note
                FROM int_results
                ORDER BY id
                """;
        try (Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            List<Map<String, Object>> rows = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", rs.getLong("id"));
                row.put("created_at", rs.getTimestamp("created_at"));
                row.put("original_text", rs.getString("original_text"));
                row.put("is_integer", rs.getBoolean("is_integer"));
                row.put("int_value", rs.getObject("int_value"));
                row.put("is_even", rs.getObject("is_even"));
                row.put("note", rs.getString("note"));
                rows.add(row);
            }
            return rows;
        }
    }

    // ===== ПУНКТ 4: ЭКСПОРТ В EXCEL =====

    private static void exportToExcel(Connection cn, String fileName) {
        try {
            List<Map<String, Object>> rows = selectAllResults(cn);

            if (rows.isEmpty()) {
                System.out.println("В таблице int_results пока нет данных.");
            } else {
                System.out.println("Данные для экспорта:");
                for (Map<String, Object> r : rows) {
                    System.out.printf("#%s | %s | '%s' | integer=%s | int_value=%s | even=%s | note=%s%n",
                            r.get("id"),
                            r.get("created_at"),
                            r.get("original_text"),
                            r.get("is_integer"),
                            Objects.toString(r.get("int_value"), "null"),
                            Objects.toString(r.get("is_even"), "null"),
                            Objects.toString(r.get("note"), "null")
                    );
                }
            }

            try (Workbook wb = new XSSFWorkbook()) {
                Sheet sheet = wb.createSheet("int_results");
                int rowIdx = 0;

                Row header = sheet.createRow(rowIdx++);
                String[] heads = {
                        "id", "created_at", "original_text",
                        "is_integer", "int_value", "is_even", "note"
                };
                for (int i = 0; i < heads.length; i++) {
                    header.createCell(i).setCellValue(heads[i]);
                }

                for (Map<String, Object> r : rows) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(((Number) r.get("id")).longValue());
                    row.createCell(1).setCellValue(Objects.toString(r.get("created_at"), ""));
                    row.createCell(2).setCellValue(Objects.toString(r.get("original_text"), ""));
                    row.createCell(3).setCellValue((Boolean) r.get("is_integer"));
                    row.createCell(4).setCellValue(
                            r.get("int_value") == null ? "" : String.valueOf(r.get("int_value"))
                    );
                    row.createCell(5).setCellValue(
                            r.get("is_even") == null ? "" : String.valueOf(r.get("is_even"))
                    );
                    row.createCell(6).setCellValue(Objects.toString(r.get("note"), ""));
                }

                for (int i = 0; i < heads.length; i++) {
                    sheet.autoSizeColumn(i);
                }

                try (FileOutputStream out = new FileOutputStream(fileName)) {
                    wb.write(out);
                }
            }

            System.out.println("Экспорт завершён: " + fileName);

        } catch (Exception e) {
            System.err.println("Ошибка экспорта в Excel: " + e.getMessage());
        }
    }
}
