package org.hw;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;

public class Server {

    public static final int PORT = 8181;

    private static long clientIdCounter = 1L;
    private static Map<Long, SocketWrapper> clients = new HashMap<>();

    public static void main(String[] args) throws IOException {
        try (ServerSocket server = new ServerSocket(PORT)) {
            System.out.println("Сервер запущен на порту " + PORT);
            while (true) {
                final Socket client = server.accept();
                final long clientId = clientIdCounter++;

                SocketWrapper wrapper = new SocketWrapper(clientId, client);
                System.out.println("Подключился новый клиент [" + clientId + "=" + wrapper + "]");
                clients.put(clientId, wrapper);
                for (Map.Entry<Long, SocketWrapper> client1 : clients.entrySet()) {
                    client1.getValue().getOutput().println("Подключился новый клиент [" + clientId + "=" + wrapper + "]");
                }
                sendListClientsForAll();

                new Thread(() -> {
                    try (Scanner input = wrapper.getInput(); PrintWriter output = wrapper.getOutput()) {
                        output.println("Подключение успешно");

                        while (true) {
                            String clientInput = input.nextLine();
                            if (Objects.equals("q", clientInput)) {
                                clients.remove(clientId);
                                clients.values().forEach(it -> it.getOutput().println("Клиент [" +
                                        clientId + "] отключился"));
                                sendListClientsForAll();
                                break;
                            }

                            if (clientInput.startsWith("!admin")) {
                                clients.get(clientId).setAdmin(true);
                                clients.get(clientId).getOutput().println("Получены права администратора");
                            } else if (clients.get(clientId).getAdmin() && clientInput.startsWith("kick")
                                    && clientInput.length() > 5 && clientInput.substring(5, 6).matches("[0-9]")) {
                                long destinationId = Long.parseLong(clientInput.substring(5, 6));
                                if (clients.containsKey(destinationId)) {
                                    SocketWrapper destination = clients.get(destinationId);
                                    destination.getSocket().close();
                                    clients.remove(destinationId);
                                    clients.values().forEach(it -> it.getOutput().println("Клиент [" +
                                            destinationId + "] отключился"));
                                    sendListClientsForAll();
                                } else {
                                    sendMessageAllClients(clientInput, clientId);
                                }
                            } else if (clientInput.charAt(0) == '@' && clientInput.length() > 1
                                    && clientInput.substring(1, 2).matches("[0-9]")) {
                                long destinationId = Long.parseLong(clientInput.substring(1, 2));
                                SocketWrapper destination = clients.get(destinationId);
                                destination.getOutput().println(clientInput);
                            } else {
                                sendMessageAllClients(clientInput, clientId);
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("Клиент [" + wrapper.getId() + "] отключился");
                    }
                }).start();
            }
        }
    }

    private static void sendListClientsForAll() {
        for (Map.Entry<Long, SocketWrapper> client1 : clients.entrySet()) {
            client1.getValue().getOutput().println("Список всех клиентов: " + clients);
        }
    }

    private static void sendMessageAllClients(String message, long sourceId) {
        for (Map.Entry<Long, SocketWrapper> client1 : clients.entrySet()) {
            if (client1.getKey() != sourceId) {
                client1.getValue().getOutput().println(message);
            }
        }
    }
}