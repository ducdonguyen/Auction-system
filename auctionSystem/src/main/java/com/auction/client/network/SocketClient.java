package com.auction.client.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class SocketClient {
    private static SocketClient instance;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    // Cấu hình mạng
    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 8080;

    // Chặn khởi tạo tự do để ép dùng Singleton
    private SocketClient() {
    }

    public static SocketClient getInstance() {
        if (instance == null) {
            instance = new SocketClient();
        }
        return instance;
    }

    /**
     * Mở kết nối tới Server. Gọi hàm này lúc Client vừa bật lên.
     */
    public void connect() throws IOException {
        if (socket == null || socket.isClosed()) {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            // LƯU Ý SỐNG CÒN: Phải tạo Output trước Input để tránh bị Deadlock (treo app)
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            System.out.println("[Client] Đã kết nối thành công tới Server " + SERVER_IP + ":" + SERVER_PORT);
        }
    }

    /**
     * Gửi một gói tin (Object) lên Server
     */
    public void sendRequest(Object request) throws IOException {
        if (out != null) {
            out.writeObject(request);
            out.flush();
        }
    }

    /**
     * Lắng nghe phản hồi từ Server
     */
    public Object receiveResponse() throws IOException, ClassNotFoundException {
        if (in != null) {
            return in.readObject();
        }
        return null;
    }

    /**
     * Ngắt kết nối an toàn khi tắt App
     */
    public void disconnect() {
        try {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            System.out.println("[Client] Đã ngắt kết nối.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
