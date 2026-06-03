package com.auction.client.controller;

import com.auction.client.network.SocketClient;
import com.auction.shared.models.auth.UserAccount;
import com.auction.shared.network.requests.LoginRequest;
import com.auction.shared.network.responses.ServiceResult;
import javafx.application.Platform;
import javafx.scene.control.Button;
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
import com.auction.client.util.SceneNavigator;
public class LoginControllerTest {

    private LoginController loginController;

    private TextField usernameField;
    private PasswordField passwordField;
    private Label errorLabel;
    private Button loginButton;

    @BeforeAll
    static void initJavaFX() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
        }
    }

    @BeforeEach
    void setUp() throws Exception {

        loginController = new LoginController();

        usernameField = new TextField();
        passwordField = new PasswordField();
        errorLabel = new Label();
        loginButton = new Button();

        injectField("usernameField", usernameField);
        injectField("passwordField", passwordField);
        injectField("errorLabel", errorLabel);
        injectField("loginButton", loginButton);
    }

    private void injectField(String fieldName, Object value)
            throws Exception {

        Field field =
                LoginController.class.getDeclaredField(fieldName);

        field.setAccessible(true);
        field.set(loginController, value);
    }

    private void invokeHandleLoginAction()
            throws Exception {

        Method method =
                LoginController.class
                        .getDeclaredMethod("handleLoginAction");

        method.setAccessible(true);
        method.invoke(loginController);
    }

    @Test
    void testEmptyUsernameAndPassword() throws Exception {

        usernameField.setText("");
        passwordField.setText("");

        invokeHandleLoginAction();

        assertEquals(
                "Vui lòng nhập đầy đủ tài khoản và mật khẩu.",
                errorLabel.getText()
        );
    }

    @Test
    void testLoginSuccessBidder() throws Exception {

        usernameField.setText("user");
        passwordField.setText("123456");

        SocketClient socketClient = mock(SocketClient.class);

        UserAccount user =
                new UserAccount(
                        1L,
                        "Nguyen Van A",
                        "user",
                        "user@gmail.com",
                        "TOKEN",
                        "BIDDER",
                        0.0
                );

        ServiceResult<UserAccount> response =
                new ServiceResult<>(
                        true,
                        "Login successful",
                        user
                );

        try (MockedStatic<SocketClient> socketMock =
                     mockStatic(SocketClient.class);
             MockedStatic<SceneNavigator> sceneMock =
                     mockStatic(SceneNavigator.class)) {

            socketMock.when(SocketClient::getInstance)
                    .thenReturn(socketClient);

            when(socketClient.receiveResponse())
                    .thenReturn(response);

            invokeHandleLoginAction();

            verify(socketClient)
                    .sendRequest(any(LoginRequest.class));

            assertEquals(
                    "Login successful",
                    errorLabel.getText()
            );

            assertEquals(
                    "-fx-text-fill: green;",
                    errorLabel.getStyle()
            );
        }
    }


    @Test
    void testLoginSuccessAdmin() throws Exception {

        usernameField.setText("admin");
        passwordField.setText("admin123");

        SocketClient socketClient = mock(SocketClient.class);

        UserAccount user =
                new UserAccount(
                        1L,
                        "Admin",
                        "admin",
                        "admin@gmail.com",
                        "TOKEN",
                        "ADMIN",
                        0.0
                );

        ServiceResult<UserAccount> response =
                new ServiceResult<>(
                        true,
                        "Login successful",
                        user
                );

        try (MockedStatic<SocketClient> socketMock =
                     mockStatic(SocketClient.class);
             MockedStatic<SceneNavigator> sceneMock =
                     mockStatic(SceneNavigator.class)) {

            socketMock.when(SocketClient::getInstance)
                    .thenReturn(socketClient);

            when(socketClient.receiveResponse())
                    .thenReturn(response);

            invokeHandleLoginAction();

            verify(socketClient)
                    .sendRequest(any(LoginRequest.class));

            assertEquals(
                    "Login successful",
                    errorLabel.getText()
            );

            assertEquals(
                    "-fx-text-fill: green;",
                    errorLabel.getStyle()
            );
        }
    }
    @Test
    void testLoginFailed() throws Exception {

        usernameField.setText("user");
        passwordField.setText("wrongpass");

        SocketClient socketClient = mock(SocketClient.class);

        ServiceResult<Object> response =
                new ServiceResult<>(
                        false,
                        "Sai tài khoản hoặc mật khẩu",
                        null
                );

        try (MockedStatic<SocketClient> mocked =
                     mockStatic(SocketClient.class)) {

            mocked.when(SocketClient::getInstance)
                    .thenReturn(socketClient);

            when(socketClient.receiveResponse())
                    .thenReturn(response);

            invokeHandleLoginAction();

            verify(socketClient)
                    .sendRequest(any(LoginRequest.class));

            assertEquals(
                    "Sai tài khoản hoặc mật khẩu",
                    errorLabel.getText()
            );

            assertEquals(
                    "-fx-text-fill: red;",
                    errorLabel.getStyle()
            );
        }
    }

    @Test
    void testInvalidServerResponse() throws Exception {

        usernameField.setText("user");
        passwordField.setText("123456");

        SocketClient socketClient = mock(SocketClient.class);

        try (MockedStatic<SocketClient> mocked =
                     mockStatic(SocketClient.class)) {

            mocked.when(SocketClient::getInstance)
                    .thenReturn(socketClient);

            when(socketClient.receiveResponse())
                    .thenReturn("INVALID_RESPONSE");

            invokeHandleLoginAction();

            assertEquals(
                    "Phản hồi từ Server không hợp lệ.",
                    errorLabel.getText()
            );
        }
    }

    @Test
    void testNetworkError() throws Exception {

        usernameField.setText("user");
        passwordField.setText("123456");

        SocketClient socketClient = mock(SocketClient.class);

        try (MockedStatic<SocketClient> mocked =
                     mockStatic(SocketClient.class)) {

            mocked.when(SocketClient::getInstance)
                    .thenReturn(socketClient);

            doThrow(new RuntimeException("Connection failed"))
                    .when(socketClient)
                    .sendRequest(any());

            invokeHandleLoginAction();

            assertTrue(
                    errorLabel.getText()
                            .contains("Lỗi kết nối mạng")
            );
        }
    }
}