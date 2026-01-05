package com.example.ai_cos.service;

import com.example.ai_cos.model.Client;
import com.example.ai_cos.model.Order;
import com.example.ai_cos.model.OrderStatus;
import com.example.ai_cos.repository.ClientRepository;
import com.example.ai_cos.repository.OrderRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DatabaseTools {

    private final ClientRepository clientRepository;
    private final OrderRepository orderRepository;

    public DatabaseTools(ClientRepository clientRepository, OrderRepository orderRepository) {
        this.clientRepository = clientRepository;
        this.orderRepository = orderRepository;
    }

    @Tool(description = "Pobiera listę wszystkich klientów z bazy danych")
    public String listAllClients() {
        List<Client> clients = clientRepository.findAll();
        if (clients.isEmpty()) {
            return "Brak klientów w bazie danych.";
        }
        return clients.stream()
                .map(c -> String.format("ID: %d, Imię: %s, Nazwisko: %s, Email: %s, Telefon: %s",
                        c.getId(), c.getFirstName(), c.getLastName(), c.getEmail(), c.getPhone()))
                .collect(Collectors.joining("\n"));
    }

    @Tool(description = "Pobiera klienta po jego ID")
    public String getClientById(@ToolParam(description = "ID klienta") Long id) {
        Optional<Client> client = clientRepository.findById(id);
        return client.map(c -> String.format("ID: %d, Imię: %s, Nazwisko: %s, Email: %s, Telefon: %s, Data rejestracji: %s",
                c.getId(), c.getFirstName(), c.getLastName(), c.getEmail(), c.getPhone(), c.getRegistrationDate()))
                .orElse("Nie znaleziono klienta o ID: " + id);
    }

    @Tool(description = "Zwraca liczbę klientów w bazie danych")
    public String getClientCount() {
        long count = clientRepository.count();
        return "Liczba klientów w bazie: " + count;
    }

    @Tool(description = "Szuka klienta po adresie email")
    public String searchClientByEmail(@ToolParam(description = "Adres email klienta") String email) {
        Optional<Client> client = clientRepository.findByEmail(email);
        return client.map(c -> String.format("ID: %d, Imię: %s, Nazwisko: %s, Email: %s, Telefon: %s",
                c.getId(), c.getFirstName(), c.getLastName(), c.getEmail(), c.getPhone()))
                .orElse("Nie znaleziono klienta z emailem: " + email);
    }

    @Tool(description = "Szuka klientów po imieniu lub nazwisku")
    public String searchClientByName(@ToolParam(description = "Imię lub nazwisko do wyszukania") String name) {
        List<Client> clients = clientRepository.searchByName(name);
        if (clients.isEmpty()) {
            return "Nie znaleziono klientów pasujących do: " + name;
        }
        return "Znalezieni klienci:\n" + clients.stream()
                .map(c -> String.format("ID: %d, Imię: %s, Nazwisko: %s, Email: %s",
                        c.getId(), c.getFirstName(), c.getLastName(), c.getEmail()))
                .collect(Collectors.joining("\n"));
    }

    @Tool(description = "Pobiera listę wszystkich zamówień z bazy danych")
    public String listAllOrders() {
        List<Order> orders = orderRepository.findAll();
        if (orders.isEmpty()) {
            return "Brak zamówień w bazie danych.";
        }
        return orders.stream()
                .map(o -> String.format("ID: %d, Produkt: %s, Ilość: %d, Cena: %.2f PLN, Status: %s, Klient ID: %d",
                        o.getId(), o.getProductName(), o.getQuantity(), o.getPrice(), o.getStatus(),
                        o.getClient().getId()))
                .collect(Collectors.joining("\n"));
    }

    @Tool(description = "Pobiera zamówienie po jego ID")
    public String getOrderById(@ToolParam(description = "ID zamówienia") Long id) {
        Optional<Order> order = orderRepository.findById(id);
        return order.map(o -> String.format("ID: %d, Produkt: %s, Ilość: %d, Cena: %.2f PLN, Status: %s, Klient ID: %d, Data: %s",
                o.getId(), o.getProductName(), o.getQuantity(), o.getPrice(), o.getStatus(),
                o.getClient().getId(), o.getOrderDate()))
                .orElse("Nie znaleziono zamówienia o ID: " + id);
    }

    @Tool(description = "Pobiera wszystkie zamówienia dla konkretnego klienta")
    public String getOrdersForClient(@ToolParam(description = "ID klienta") Long clientId) {
        Optional<Client> client = clientRepository.findById(clientId);
        if (client.isEmpty()) {
            return "Nie znaleziono klienta o ID: " + clientId;
        }
        
        List<Order> orders = orderRepository.findByClientId(clientId);
        if (orders.isEmpty()) {
            return "Klient " + client.get().getFirstName() + " " + client.get().getLastName() + " nie ma żadnych zamówień.";
        }
        return "Zamówienia klienta " + client.get().getFirstName() + " " + client.get().getLastName() + ":\n" +
                orders.stream()
                .map(o -> String.format("  ID: %d, Produkt: %s, Ilość: %d, Cena: %.2f PLN, Status: %s",
                        o.getId(), o.getProductName(), o.getQuantity(), o.getPrice(), o.getStatus()))
                .collect(Collectors.joining("\n"));
    }

    @Tool(description = "Zwraca liczbę zamówień w bazie danych")
    public String getOrderCount() {
        long count = orderRepository.count();
        return "Liczba zamówień w bazie: " + count;
    }

    @Tool(description = "Pobiera zamówienia według statusu: NEW, PROCESSING, SHIPPED, DELIVERED, CANCELLED")
    public String getOrdersByStatus(@ToolParam(description = "Status zamówienia") String status) {
        try {
            OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
            List<Order> orders = orderRepository.findByStatus(orderStatus);
            if (orders.isEmpty()) {
                return "Brak zamówień ze statusem: " + status;
            }
            return "Zamówienia ze statusem " + status + ":\n" + orders.stream()
                    .map(o -> String.format("  ID: %d, Produkt: %s, Klient ID: %d",
                            o.getId(), o.getProductName(), o.getClient().getId()))
                    .collect(Collectors.joining("\n"));
        } catch (IllegalArgumentException e) {
            return "Nieprawidłowy status. Dostępne statusy: NEW, PROCESSING, SHIPPED, DELIVERED, CANCELLED";
        }
    }

    @Tool(description = "Oblicza ile łącznie wydał dany klient na zamówienia")
    public String getTotalSpentByClient(@ToolParam(description = "ID klienta") Long clientId) {
        Optional<Client> client = clientRepository.findById(clientId);
        if (client.isEmpty()) {
            return "Nie znaleziono klienta o ID: " + clientId;
        }
        
        BigDecimal total = orderRepository.getTotalSpentByClient(clientId);
        if (total == null) {
            return "Klient " + client.get().getFirstName() + " " + client.get().getLastName() + " nie ma żadnych zamówień.";
        }
        return String.format("Klient %s %s wydał łącznie: %.2f PLN", 
                client.get().getFirstName(), client.get().getLastName(), total);
    }

    @Tool(description = "Zwraca podsumowanie bazy danych - liczbę klientów, zamówień i łączną wartość")
    public String getDatabaseSummary() {
        long clientCount = clientRepository.count();
        long orderCount = orderRepository.count();
        
        BigDecimal totalRevenue = orderRepository.findAll().stream()
                .map(o -> o.getPrice().multiply(BigDecimal.valueOf(o.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return String.format("""
                Podsumowanie bazy danych:
                - Liczba klientów: %d
                - Liczba zamówień: %d
                - Łączna wartość zamówień: %.2f PLN""",
                clientCount, orderCount, totalRevenue);
    }

    @Tool(description = "Wyszukuje zamówienia po nazwie produktu")
    public String searchOrdersByProduct(@ToolParam(description = "Nazwa produktu") String productName) {
        List<Order> orders = orderRepository.findByProductNameContainingIgnoreCase(productName);
        if (orders.isEmpty()) {
            return "Nie znaleziono zamówień dla produktu: " + productName;
        }
        return "Zamówienia dla produktu '" + productName + "':\n" + orders.stream()
                .map(o -> String.format("  ID: %d, Produkt: %s, Ilość: %d, Cena: %.2f PLN, Klient: %s %s",
                        o.getId(), o.getProductName(), o.getQuantity(), o.getPrice(),
                        o.getClient().getFirstName(), o.getClient().getLastName()))
                .collect(Collectors.joining("\n"));
    }

    @Tool(description = "Zwraca ranking klientów według liczby zamówień - kto ma najwięcej zamówień")
    public String getClientsRankedByOrderCount() {
        List<Client> clients = clientRepository.findAll();
        if (clients.isEmpty()) {
            return "Brak klientów w bazie.";
        }
        
        var ranked = clients.stream()
                .map(c -> {
                    long orderCount = orderRepository.countOrdersByClient(c.getId());
                    BigDecimal totalSpent = orderRepository.getTotalSpentByClient(c.getId());
                    return new Object[]{c, orderCount, totalSpent != null ? totalSpent : BigDecimal.ZERO};
                })
                .sorted((a, b) -> Long.compare((Long) b[1], (Long) a[1]))
                .toList();
        
        StringBuilder sb = new StringBuilder("Ranking klientów według liczby zamówień:\n");
        int position = 1;
        for (var entry : ranked) {
            Client c = (Client) entry[0];
            long count = (Long) entry[1];
            BigDecimal spent = (BigDecimal) entry[2];
            sb.append(String.format("%d. %s %s - %d zamówień, wydał łącznie: %.2f PLN\n",
                    position++, c.getFirstName(), c.getLastName(), count, spent));
        }
        return sb.toString();
    }

    @Tool(description = "Zwraca ranking klientów według wydanej kwoty - kto wydał najwięcej pieniędzy")
    public String getClientsRankedBySpending() {
        List<Client> clients = clientRepository.findAll();
        if (clients.isEmpty()) {
            return "Brak klientów w bazie.";
        }
        
        var ranked = clients.stream()
                .map(c -> {
                    BigDecimal totalSpent = orderRepository.getTotalSpentByClient(c.getId());
                    long orderCount = orderRepository.countOrdersByClient(c.getId());
                    return new Object[]{c, totalSpent != null ? totalSpent : BigDecimal.ZERO, orderCount};
                })
                .sorted((a, b) -> ((BigDecimal) b[1]).compareTo((BigDecimal) a[1]))
                .toList();
        
        StringBuilder sb = new StringBuilder("Ranking klientów według wydanych pieniędzy:\n");
        int position = 1;
        for (var entry : ranked) {
            Client c = (Client) entry[0];
            BigDecimal spent = (BigDecimal) entry[1];
            long count = (Long) entry[2];
            sb.append(String.format("%d. %s %s - wydał %.2f PLN (%d zamówień)\n",
                    position++, c.getFirstName(), c.getLastName(), spent, count));
        }
        return sb.toString();
    }

    @Tool(description = "Zwraca najpopularniejsze produkty według liczby zamówień")
    public String getMostPopularProducts() {
        List<Order> orders = orderRepository.findAll();
        if (orders.isEmpty()) {
            return "Brak zamówień w bazie.";
        }
        
        var productStats = orders.stream()
                .collect(Collectors.groupingBy(
                        Order::getProductName,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> new Object[]{
                                        list.size(),
                                        list.stream().mapToInt(Order::getQuantity).sum(),
                                        list.stream()
                                                .map(o -> o.getPrice().multiply(BigDecimal.valueOf(o.getQuantity())))
                                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                }
                        )
                ));
        
        var ranked = productStats.entrySet().stream()
                .sorted((a, b) -> Integer.compare((Integer) b.getValue()[0], (Integer) a.getValue()[0]))
                .toList();
        
        StringBuilder sb = new StringBuilder("Najpopularniejsze produkty:\n");
        int position = 1;
        for (var entry : ranked) {
            String product = entry.getKey();
            int orderCount = (Integer) entry.getValue()[0];
            int totalQty = (Integer) entry.getValue()[1];
            BigDecimal totalValue = (BigDecimal) entry.getValue()[2];
            sb.append(String.format("%d. %s - %d zamówień, %d sztuk, wartość: %.2f PLN\n",
                    position++, product, orderCount, totalQty, totalValue));
        }
        return sb.toString();
    }

    @Tool(description = "Zwraca klienta z największą liczbą zamówień")
    public String getTopClientByOrderCount() {
        List<Client> clients = clientRepository.findAll();
        if (clients.isEmpty()) {
            return "Brak klientów w bazie.";
        }
        
        Client topClient = null;
        long maxOrders = 0;
        
        for (Client c : clients) {
            long count = orderRepository.countOrdersByClient(c.getId());
            if (count > maxOrders) {
                maxOrders = count;
                topClient = c;
            }
        }
        
        if (topClient == null || maxOrders == 0) {
            return "Żaden klient nie ma zamówień.";
        }
        
        BigDecimal spent = orderRepository.getTotalSpentByClient(topClient.getId());
        return String.format("Klient z największą liczbą zamówień: %s %s\n- Liczba zamówień: %d\n- Łącznie wydał: %.2f PLN",
                topClient.getFirstName(), topClient.getLastName(), maxOrders, spent != null ? spent : BigDecimal.ZERO);
    }

    @Tool(description = "Zwraca klienta który wydał najwięcej pieniędzy")
    public String getTopClientBySpending() {
        List<Client> clients = clientRepository.findAll();
        if (clients.isEmpty()) {
            return "Brak klientów w bazie.";
        }
        
        Client topClient = null;
        BigDecimal maxSpent = BigDecimal.ZERO;
        
        for (Client c : clients) {
            BigDecimal spent = orderRepository.getTotalSpentByClient(c.getId());
            if (spent != null && spent.compareTo(maxSpent) > 0) {
                maxSpent = spent;
                topClient = c;
            }
        }
        
        if (topClient == null) {
            return "Żaden klient nie ma zamówień.";
        }
        
        long orderCount = orderRepository.countOrdersByClient(topClient.getId());
        return String.format("Klient który wydał najwięcej: %s %s\n- Łącznie wydał: %.2f PLN\n- Liczba zamówień: %d",
                topClient.getFirstName(), topClient.getLastName(), maxSpent, orderCount);
    }
}
