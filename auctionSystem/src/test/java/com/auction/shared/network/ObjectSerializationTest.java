package com.auction.shared.network;

import com.auction.shared.models.Bidder;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

public class ObjectSerializationTest {

    @Test
    public void testSerializeAndDeserializeBidder() throws IOException, ClassNotFoundException {
        // 1. Tạo đối tượng Bidder ban đầu
        String username = "testUser";
        String password = "testPassword";
        double balance = 1000.0;
        Bidder originalBidder = new Bidder(username, password, balance);

        // 2. Ghi đối tượng vào ByteArrayOutputStream sử dụng ObjectOutputStream
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(originalBidder);
        oos.flush();
        oos.close();

        byte[] serializedData = baos.toByteArray();
        assertNotNull(serializedData);
        assertTrue(serializedData.length > 0);

        // 3. Đọc đối tượng từ ByteArrayInputStream sử dụng ObjectInputStream
        ByteArrayInputStream bais = new ByteArrayInputStream(serializedData);
        ObjectInputStream ois = new ObjectInputStream(bais);
        Bidder deserializedBidder = (Bidder) ois.readObject();
        ois.close();

        // 4. Kiểm tra xem đối tượng deserialized có giống với đối tượng ban đầu không
        assertNotNull(deserializedBidder);
        assertEquals(originalBidder.getId(), deserializedBidder.getId(), "ID must be the same");
        assertEquals(originalBidder.getUsername(), deserializedBidder.getUsername(), "Username must be the same");
        assertEquals(originalBidder.getPassword(), deserializedBidder.getPassword(), "Password must be the same");
        assertEquals(originalBidder.getBalance(), deserializedBidder.getBalance(), 0.001, "Balance must be the same");
        
        System.out.println("[DEBUG_LOG] Serialization and Deserialization successful for Bidder: " + deserializedBidder.getUsername());
    }

    @Test
    public void testSerializeAndDeserializeLoginRequest() throws IOException, ClassNotFoundException {
        // 1. Tạo đối tượng LoginRequest
        LoginRequest originalRequest = new LoginRequest("admin", "secret123");

        // 2. Ghi đối tượng
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(originalRequest);
        oos.close();

        // 3. Đọc đối tượng
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
        LoginRequest deserializedRequest = (LoginRequest) ois.readObject();
        ois.close();

        // 4. Kiểm tra
        assertNotNull(deserializedRequest);
        assertEquals(originalRequest.username(), deserializedRequest.username());
        assertEquals(originalRequest.password(), deserializedRequest.password());

        System.out.println("[DEBUG_LOG] Serialization and Deserialization successful for LoginRequest: " + deserializedRequest.username());
    }
}
