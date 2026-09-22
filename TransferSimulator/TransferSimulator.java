import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;

public class TransferSimulator {

    private static final int PORT = 4444;
    private static final String BASE = "/TransferSimulator";

    private static final Random RANDOM = new Random();

    /*
     * Каждый объект — отдельный тестовый клиент.
     *
     * При каждом запросе сервер случайно выбирает одного клиента.
     * Поэтому:
     *
     * GET /fullName
     *   -> случайное ФИО
     *
     * GET /snils
     *   -> случайный СНИЛС
     *
     * GET /inn
     *   -> случайный ИНН
     *
     * и т.д.
     */

    private static final List<Client> CLIENTS = List.of(

            // 1. Всё корректно
            new Client(
                    "Иванов Иван Иванович",
                    "123-456-789 64",
                    "1234567894",
                    "ivanov.ivan@example.com",
                    "10 20 123456"
            ),

            // 2. Ошибка в СНИЛС
            new Client(
                    "Петров Пётр Сергеевич",
                    "234-567-890 12",
                    "2345678908",
                    "petrov.petr@example.com",
                    "10 21 234567"
            ),

            // 3. Ошибка в ИНН
            new Client(
                    "Сидоров Алексей Олегович",
                    "345-678-901 23",
                    "3456789011",
                    "sidorov.alex@example.com",
                    "10 22 345678"
            ),

            // 4. Ошибка в email
            new Client(
                    "Кузнецов Максим Игоревич",
                    "456-789-012 38",
                    "4567890120",
                    "kuznetsov.maxim@",
                    "10 23 456789"
            ),

            // 5. Ошибка в номере карты
            new Client(
                    "Смирнов Антон Дмитриевич",
                    "567-890-123 43",
                    "5678901234",
                    "smirnov.anton@example.com",
                    "1024ABC"
            ),

            // 6. Несколько ошибок
            new Client(
                    "Волков Артём",
                    "678-901-234 99",
                    "6789012345",
                    "volkov.artem@",
                    "123"
            ),

            // 7. Всё корректно
            new Client(
                    "Морозов Дмитрий Андреевич",
                    "789-012-345 23",
                    "7890123454",
                    "morozov.dmitry@example.com",
                    "10 26 789012"
            ),

            // 8. Ошибка в СНИЛС + email
            new Client(
                    "Фёдоров Никита Романович",
                    "890-123-456 00",
                    "8901234560",
                    "fedorov.nikitaexample.com",
                    "10 27 890123"
            ),

            // 9. Ошибка в ИНН + карте
            new Client(
                    "Попов Кирилл Викторович",
                    "901-234-567 64",
                    "9012345679",
                    "popov.kirill@example.com",
                    "10-28-901234"
            ),

            // 10. Всё корректно
            new Client(
                    "Орлов Михаил Александрович",
                    "567-890-123 43",
                    "5678901234",
                    "orlov.mikhail@example.com",
                    "10 29 135792"
            )
    );

    public static void main(String[] args) throws IOException {

        HttpServer server = HttpServer.create(
                new InetSocketAddress("localhost", PORT),
                0
        );

        server.createContext(BASE, TransferSimulator::handleRequest);

        server.setExecutor(null);
        server.start();

        System.out.println();
        System.out.println("==============================================");
        System.out.println(" TransferSimulator запущен");
        System.out.println(" http://localhost:4444/TransferSimulator/");
        System.out.println(" Клиенты: " + CLIENTS.size());
        System.out.println(" Режим: случайный клиент");
        System.out.println("==============================================");
        System.out.println();
        System.out.println("Остановить сервер: Ctrl+C");
    }

    private static void handleRequest(HttpExchange exchange)
            throws IOException {

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        // Разрешаем только GET
        if (!"GET".equalsIgnoreCase(method)) {

            send(
                    exchange,
                    405,
                    "{\"error\":\"Method Not Allowed\"}"
            );

            return;
        }

        String endpoint = path
                .substring(BASE.length())
                .replaceAll("^/|/$", "");

        // Разрешённые методы API
        if (!List.of(
                "fullName",
                "snils",
                "inn",
                "email",
                "identityCard"
        ).contains(endpoint)) {

            send(
                    exchange,
                    404,
                    "{\"error\":\"Not Found\"}"
            );

            return;
        }

        // Случайно выбираем клиента
        Client client = CLIENTS.get(
                RANDOM.nextInt(CLIENTS.size())
        );

        String value;

        switch (endpoint) {

            case "fullName":
                value = client.fullName();
                break;

            case "snils":
                value = client.snils();
                break;

            case "inn":
                value = client.inn();
                break;

            case "email":
                value = client.email();
                break;

            case "identityCard":
                value = client.identityCard();
                break;

            default:
                value = "";
        }

        String json = "{\"value\":\""
                + escapeJson(value)
                + "\"}";

        send(exchange, 200, json);
    }

    private static void send(
            HttpExchange exchange,
            int status,
            String body
    ) throws IOException {

        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type",
                "application/json; charset=UTF-8"
        );

        exchange.sendResponseHeaders(
                status,
                bytes.length
        );

        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private static String escapeJson(String value) {

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    private record Client(
            String fullName,
            String snils,
            String inn,
            String email,
            String identityCard
    ) {}
}