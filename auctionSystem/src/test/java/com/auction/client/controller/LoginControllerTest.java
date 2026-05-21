package com.auction.client.controller;

import com.auction.client.network.SocketClient;
import com.auction.client.service.AuthService;
import com.auction.shared.models.AuthUser;
import com.auction.shared.network.LoginRequest;
import com.auction.shared.network.ServiceResult;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
        } catch (IllegalStateException e) {
            // Platform already started
        }
    }

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        SocketClient mockSocket = mock(SocketClient.class);
        SocketClient.setInstance(mockSocket);
        
        // Inject mock AuthService into private final field using reflection
        Field authServiceField = LoginController.class.getDeclaredField("authService");
        authServiceField.setAccessible(true);
        authServiceField.set(loginController, authService);

        // Initialize JavaFX components and inject them
        usernameField = new TextField();
        passwordField = new PasswordField();
        errorLabel = new Label();
        loginButton = new Button();

        injectField("usernameField", usernameField);
        injectField("passwordField", passwordField);
        injectField("errorLabel", errorLabel);
        injectField("loginButton", loginButton);
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
        
        AuthUser authUser = new AuthUser("testuser", "Full Name", "test@example.com", "TOKEN");
        ServiceResult<AuthUser> successResult = new ServiceResult<>(true, "Login successful", authUser);
        
        when(authService.login(any(LoginRequest.class))).thenReturn(successResult);

        // Use reflection to call private method directly
        java.lang.reflect.Method method = LoginController.class.getDeclaredMethod("handleLoginAction");
        method.setAccessible(true);
        try {
            method.invoke(loginController);
        } catch (Exception e) {
            // Expected failure in SceneNavigator due to missing Stage
        }

        verify(authService).login(argThat(request -> 
            request.username().equals("testuser") && request.password().equals("password")
        ));
        assertEquals("Login successful", errorLabel.getText());
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
