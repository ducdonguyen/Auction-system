package com.auction.client.controller;

import com.auction.client.service.AuthService;
import com.auction.client.util.SceneNavigator; // Import UIManager mới
import com.auction.shared.models.AuthUser;
import com.auction.shared.network.LoginRequest;
import com.auction.shared.network.ServiceResult;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class LoginControllerTest {

    @InjectMocks
    private LoginController loginController;

    @Mock
    private AuthService authService;

    private TextField usernameField;
    private PasswordField passwordField;
    private Label errorLabel;
    private Button loginButton;

    @BeforeAll
    public static void initJavaFX() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) { }
    }

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Khởi tạo các thành phần UI
        usernameField = new TextField();
        passwordField = new PasswordField();
        errorLabel = new Label();
        loginButton = new Button();

        injectField("usernameField", usernameField);
        injectField("passwordField", passwordField);
        injectField("errorLabel", errorLabel);
        injectField("loginButton", loginButton);
        injectField("authService", authService); // Inject trực tiếp service mock vào controller
    }

    private void injectField(String fieldName, Object value) throws Exception {
        Field field = LoginController.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(loginController, value);
    }

    @Test
    public void testHandleLoginActionSuccess() throws Exception {
        usernameField.setText("testuser");
        passwordField.setText("password");

        AuthUser authUser = new AuthUser("testuser", "Full Name", "test@example.com", "TOKEN", "BIDDER");
        ServiceResult<AuthUser> successResult = new ServiceResult<>(true, "Login successful", authUser);

        when(authService.login(any(LoginRequest.class))).thenReturn(successResult);

        // SỬA: Mock phương thức static của SceneNavigator
        try (MockedStatic<SceneNavigator> sceneNavigator = mockStatic(SceneNavigator.class)) {

            java.lang.reflect.Method method = LoginController.class.getDeclaredMethod("handleLoginAction");
            method.setAccessible(true);
            method.invoke(loginController);

            // Xác nhận logic đăng nhập đã chạy
            verify(authService).login(any(LoginRequest.class));

            // SỬA: Verify phương thức với đúng 2 tham số (Node, Scene)
            sceneNavigator.verify(() -> SceneNavigator.switchScene(eq(loginButton), any(com.auction.client.util.Scene.class)));

            assertEquals("Login successful", errorLabel.getText());
        }
    }

    @Test
    public void testHandleLoginActionFailure() throws Exception {
        usernameField.setText("wronguser");
        passwordField.setText("wrongpass");

        ServiceResult<AuthUser> failureResult = new ServiceResult<>(false, "Invalid credentials", null);

        when(authService.login(any(LoginRequest.class))).thenReturn(failureResult);

        java.lang.reflect.Method method = LoginController.class.getDeclaredMethod("handleLoginAction");
        method.setAccessible(true);
        method.invoke(loginController);

        verify(authService).login(any(LoginRequest.class));
        assertEquals("Invalid credentials", errorLabel.getText());
        assertEquals("-fx-text-fill: red;", errorLabel.getStyle());
    }
}