package com.auction.client.controller;

import com.auction.client.service.AuthService;
import com.auction.shared.network.requests.RegistrationRequest;
import com.auction.shared.network.responses.ServiceResult;
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
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class RegisterControllerTest {

    @InjectMocks
    private RegisterController registerController;

    @Mock
    private AuthService authService;

    private TextField fullNameField;
    private TextField usernameField;
    private TextField emailField;
    private PasswordField passwordField;
    private PasswordField confirmPasswordField;
    private Label messageLabel;

    @BeforeAll
    public static void initJavaFX() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
        }
    }

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        Field authServiceField = RegisterController.class.getDeclaredField("authService");
        authServiceField.setAccessible(true);
        authServiceField.set(registerController, authService);

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

    @Test
    public void testHandleRegisterActionEmptyFields() throws Exception {
        fullNameField.setText("");
        
        invokeHandleRegisterAction();

        assertEquals("Vui lòng nhập đầy đủ thông tin.", messageLabel.getText());
        verify(authService, never()).register(any());
    }

    @Test
    public void testHandleRegisterActionInvalidEmail() throws Exception {
        fullNameField.setText("Full Name");
        usernameField.setText("user");
        emailField.setText("invalidemail");
        passwordField.setText("pass");
        confirmPasswordField.setText("pass");

        invokeHandleRegisterAction();

        assertEquals("Email không hợp lệ.", messageLabel.getText());
        verify(authService, never()).register(any());
    }

    @Test
    public void testHandleRegisterActionPasswordMismatch() throws Exception {
        fullNameField.setText("Full Name");
        usernameField.setText("user");
        emailField.setText("test@example.com");
        passwordField.setText("pass1");
        confirmPasswordField.setText("pass2");

        invokeHandleRegisterAction();

        assertEquals("Mật khẩu xác nhận không khớp.", messageLabel.getText());
        verify(authService, never()).register(any());
    }

    @Test
    public void testHandleRegisterActionSuccess() throws Exception {
        fullNameField.setText("Full Name");
        usernameField.setText("user");
        emailField.setText("test@example.com");
        passwordField.setText("pass");
        confirmPasswordField.setText("pass");

        when(authService.register(any(RegistrationRequest.class)))
            .thenReturn(new ServiceResult<>(true, "Registration successful", null));

        invokeHandleRegisterAction();

        verify(authService).register(any(RegistrationRequest.class));
        assertEquals("Registration successful", messageLabel.getText());
        // Verify fields are cleared
        assertTrue(fullNameField.getText().isEmpty());
    }

    @Test
    public void testHandleRegisterActionServerFailure() throws Exception {
        fullNameField.setText("Full Name");
        usernameField.setText("existinguser");
        emailField.setText("test@example.com");
        passwordField.setText("pass");
        confirmPasswordField.setText("pass");

        when(authService.register(any(RegistrationRequest.class)))
            .thenReturn(new ServiceResult<>(false, "Username already exists", null));

        invokeHandleRegisterAction();

        verify(authService).register(any(RegistrationRequest.class));
        assertEquals("Username already exists", messageLabel.getText());
        // Verify fields are NOT cleared
        assertEquals("Full Name", fullNameField.getText());
    }

    @Test
    public void testHandleBackToLogin() throws Exception {
        Button backToLoginButton = new Button();
        injectField("backToLoginButton", backToLoginButton);
        
        Platform.runLater(() -> {
            javafx.scene.Scene scene = new javafx.scene.Scene(new javafx.scene.layout.StackPane(backToLoginButton));
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setScene(scene);
            
            try {
                java.lang.reflect.Method method = RegisterController.class.getDeclaredMethod("handleBackToLogin");
                method.setAccessible(true);
                method.invoke(registerController);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        Thread.sleep(200);
    }

    private void invokeHandleRegisterAction() throws Exception {
        java.lang.reflect.Method method = RegisterController.class.getDeclaredMethod("handleRegisterAction");
        method.setAccessible(true);
        method.invoke(registerController);
    }
}
