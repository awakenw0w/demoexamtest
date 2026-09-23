import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;

/** Локальный эмулятор API с валидными и намеренно невалидными данными. */
public class TransferSimulator {

    private static final int PORT = 4444;
    private static final String BASE = "/TransferSimulator";
    private static final Random RANDOM = new Random();

    /* В каждом наборе есть корректные и некорректные значения. */
    private static final List<String> FULL_NAMES = List.of(
            "Иванов Иван Иванович",
            "Петров Пётр Сергеевич",
            "Сидоров Алексей Олегович",
            "Иванов Иван2 Иванович",       // цифра в ФИО
            "Петров-Петров Пётр Сергеевич", // запрещённый символ
            "Смирнов Антон",                // не три части
            "Кузнецов  Максим Игоревич",    // два пробела
            "Орлов Михаил Александрович!"    // знак пунктуации
    );

    private static final List<String> SNILS = List.of(
            "112-233-445 95",
            "901-144-044 41",
            "123-456-789 64",
            "123--456-789 64",  // два дефиса
            "123-456--789 64",  // два дефиса
            "123-456-789-64",   // дефис вместо пробела
            "12A-456-789 64",   // буква вместо цифры
            "123-456-7896",     // отсутствует разделитель
            "123-45-789 64"      // неверное количество цифр в группе
    );

    private static final List<String> INNS = List.of(
            "7736050003",
            "500100732259",
            "1234567894",
            "123456789",         // 9 цифр
            "1234567890123",     // 13 цифр
            "12345A7890",        // буква
            "1234-567-890",      // разделители
            " 1234567890"         // лишний пробел
    );

    private static final List<String> EMAILS = List.of(
            "ivanov.ivan@example.com",
            "petrov.petr@example.com",
            "kuznetsov.maxim@",
            "fedorov.nikitaexample.com",
            "smirnov@@example.com",
            "popov kirill@example.com"
    );

    private static final List<String> IDENTITY_CARDS = List.of(
            "10 20 123456",
            "10 26 789012",
            "1024ABC",
            "10-28-901234",
            "10 2 123456",
            "10 20 12345A",
            "1020123456"
    );

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", PORT), 0);
        server.createContext(BASE, TransferSimulator::handleRequest);
        server.setExecutor(null);
        server.start();

        System.out.println("TransferSimulator запущен: http://localhost:4444/TransferSimulator/");
        System.out.println("Режим: случайные валидные и невалидные данные");
        System.out.println("Остановить сервер: Ctrl+C");
    }

    private static void handleRequest(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        String endpoint = exchange.getRequestURI().getPath()
                .substring(BASE.length())
                .replaceAll("^/|/$", "");

        String value = switch (endpoint) {
            case "fullName" -> randomValue(FULL_NAMES);
            case "snils" -> randomValue(SNILS);
            case "inn" -> randomValue(INNS);
            case "email" -> randomValue(EMAILS);
            case "identityCard" -> randomValue(IDENTITY_CARDS);
            default -> null;
        };

        if (value == null) {
            send(exchange, 404, "{\"error\":\"Not Found\"}");
            return;
        }

        send(exchange, 200, "{\"value\":\"" + escapeJson(value) + "\"}");
    }

    private static String randomValue(List<String> values) {
        return values.get(RANDOM.nextInt(values.size()));
    }

    private static void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
