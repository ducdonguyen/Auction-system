package com.auction.client.controller;

import com.auction.client.network.SocketClient;
import com.auction.shared.network.requests.RegistrationRequest;
import com.auction.shared.network.responses.ServiceResult;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class RegisterControllerTest {

    private RegisterController registerController;

    private TextField fullNameField;
    private TextField usernameField;
    private TextField emailField;
    private PasswordField passwordField;
    private PasswordField confirmPasswordField;
    private Label messageLabel;

    @BeforeAll
    static void initJavaFX() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        registerController = new RegisterController();

        fullNameField = new TextField();
        usernameField = new TextField();
        emailField = new TextField();
        passwordField = new PasswordField();
        confirmPasswordField = new PasswordField();
        messageLabel = new Label();

        injectField("fullNameField", fullNameField);
        injectField("usernameField", usernameField);
        injectField("emailField", emailField);
        injectField("passwordField", passwordField);
        injectField("confirmPasswordField", confirmPasswordField);
        injectField("messageLabel", messageLabel);
    }

    private void injectField(String fieldName, Object value) throws Exception {
        Field field = RegisterController.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(registerController, value);
    }

    private void invokeHandleRegisterAction() throws Exception {
        Method method =
                RegisterController.class.getDeclaredMethod("handleRegisterAction");
        method.setAccessible(true);
        method.invoke(registerController);
    }

    @Test
    void testEmptyFields() throws Exception {
        fullNameField.setText("");

        invokeHandleRegisterAction();

        assertEquals(
                "Vui lòng nhập đầy đủ thông tin.",
                messageLabel.getText()
        );
    }

    @Test
    void testInvalidEmail() throws Exception {
        fullNameField.setText("Nguyen Van A");
        usernameField.setText("user");
        emailField.setText("invalid-email");
        passwordField.setText("123456");
        confirmPasswordField.setText("123456");

        invokeHandleRegisterAction();

        assertEquals(
                "Email không hợp lệ.",
                messageLabel.getText()
        );
    }

    @Test
    void testPasswordMismatch() throws Exception {
        fullNameField.setText("Nguyen Van A");
        usernameField.setText("user");
        emailField.setText("test@gmail.com");
        passwordField.setText("123456");
        confirmPasswordField.setText("654321");

        invokeHandleRegisterAction();

        assertEquals(
                "Mật khẩu xác nhận không khớp.",
                messageLabel.getText()
        );
    }

    @Test
    void testRegisterSuccess() throws Exception {

        fullNameField.setText("Nguyen Van A");
        usernameField.setText("user");
        emailField.setText("test@gmail.com");
        passwordField.setText("123456");
        confirmPasswordField.setText("123456");

        SocketClient socketClient = mock(SocketClient.class);

        try (MockedStatic<SocketClient> mocked =
                     mockStatic(SocketClient.class)) {

            mocked.when(SocketClient::getInstance)
                    .thenReturn(socketClient);

            when(socketClient.receiveResponse())
                    .thenReturn(
                            new ServiceResult<>(
                                    true,
                                    "Registration successful",
                                    null,
                                    System.currentTimeMillis()
                            )
                    );

            invokeHandleRegisterAction();

            verify(socketClient)
                    .sendRequest(any(RegistrationRequest.class));

            assertEquals(
                    "Registration successful",
                    messageLabel.getText()
            );

            assertTrue(fullNameField.getText().isEmpty());
            assertTrue(usernameField.getText().isEmpty());
            assertTrue(emailField.getText().isEmpty());
            assertTrue(passwordField.getText().isEmpty());
            assertTrue(confirmPasswordField.getText().isEmpty());
        }
    }

    @Test
    void testRegisterFailed() throws Exception {

        fullNameField.setText("Nguyen Van A");
        usernameField.setText("existingUser");
        emailField.setText("test@gmail.com");
        passwordField.setText("123456");
        confirmPasswordField.setText("123456");

        SocketClient socketClient = mock(SocketClient.class);

        try (MockedStatic<SocketClient> mocked =
                     mockStatic(SocketClient.class)) {

            mocked.when(SocketClient::getInstance)
                    .thenReturn(socketClient);

            when(socketClient.receiveResponse())
                    .thenReturn(
                            new ServiceResult<>(
                                    false,
                                    "Username already exists",
                                    null,
                                    System.currentTimeMillis()
                            )
                    );

            invokeHandleRegisterAction();

            verify(socketClient)
                    .sendRequest(any(RegistrationRequest.class));

            assertEquals(
                    "Username already exists",
                    messageLabel.getText()
            );

            assertEquals(
                    "Nguyen Van A",
                    fullNameField.getText()
            );
        }
    }

    @Test
    void testServerReturnsInvalidResponse() throws Exception {

        fullNameField.setText("Nguyen Van A");
        usernameField.setText("user");
        emailField.setText("test@gmail.com");
        passwordField.setText("123456");
        confirmPasswordField.setText("123456");

        SocketClient socketClient = mock(SocketClient.class);

        try (MockedStatic<SocketClient> mocked =
                     mockStatic(SocketClient.class)) {

            mocked.when(SocketClient::getInstance)
                    .thenReturn(socketClient);

            when(socketClient.receiveResponse())
                    .thenReturn("INVALID_RESPONSE");

            invokeHandleRegisterAction();

            assertEquals(
                    "Phản hồi từ Server không hợp lệ.",
                    messageLabel.getText()
            );
        }
    }

    @Test
    void testConnectionError() throws Exception {

        fullNameField.setText("Nguyen Van A");
        usernameField.setText("user");
        emailField.setText("test@gmail.com");
        passwordField.setText("123456");
        confirmPasswordField.setText("123456");

        SocketClient socketClient = mock(SocketClient.class);

        try (MockedStatic<SocketClient> mocked =
                     mockStatic(SocketClient.class)) {

            mocked.when(SocketClient::getInstance)
                    .thenReturn(socketClient);

            doThrow(new RuntimeException("Network error"))
                    .when(socketClient)
                    .sendRequest(any());

            invokeHandleRegisterAction();

            assertTrue(
                    messageLabel.getText()
                            .contains("Lỗi kết nối mạng")
            );
        }
    }
}