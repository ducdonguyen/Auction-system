package com.auction.client.controller;

import com.auction.client.network.SocketClient;
import com.auction.client.service.AuthService;
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

        // Attach button to a Scene and Stage to avoid SceneNavigator failure
        Platform.runLater(() -> {
            javafx.scene.Scene scene = new javafx.scene.Scene(new javafx.scene.layout.StackPane(loginButton));
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setScene(scene);
        });
        // Give some time for JavaFX thread to process
        Thread.sleep(200);
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

        UserAccount userAccount = new UserAccount("Full Name", "testuser", "test@example.com", "TOKEN", "BIDDER");
        ServiceResult<UserAccount> successResult = new ServiceResult<>(true, "Login successful", userAccount);
        
        when(authService.login(any(LoginRequest.class))).thenReturn(successResult);

        // Use reflection to call private method on FX thread
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                java.lang.reflect.Method method = LoginController.class.getDeclaredMethod("handleLoginAction");
                method.setAccessible(true);
                method.invoke(loginController);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });
        latch.await(2, java.util.concurrent.TimeUnit.SECONDS);

        verify(authService).login(argThat(request -> 
            request.username().equals("testuser") && request.password().equals("password")
        ));
        assertEquals("Login successful", errorLabel.getText());
    }

    @Test
    public void testHandleLoginActionSuccessAdmin() throws Exception {
        usernameField.setText("admin");
        passwordField.setText("adminpass");

        UserAccount userAccount = new UserAccount("Admin User", "admin", "admin@example.com", "TOKEN", "ADMIN");
        ServiceResult<UserAccount> successResult = new ServiceResult<>(true, "Login successful", userAccount);
        
        when(authService.login(any(LoginRequest.class))).thenReturn(successResult);

        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                java.lang.reflect.Method method = LoginController.class.getDeclaredMethod("handleLoginAction");
                method.setAccessible(true);
                method.invoke(loginController);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });
        latch.await(2, java.util.concurrent.TimeUnit.SECONDS);

        verify(authService).login(any(LoginRequest.class));
        assertEquals("Login successful", errorLabel.getText());
        assertEquals("-fx-text-fill: green;", errorLabel.getStyle());
    }

    @Test
    public void testHandleGoToRegister() throws Exception {
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                java.lang.reflect.Method method = LoginController.class.getDeclaredMethod("handleGoToRegister");
                method.setAccessible(true);
                method.invoke(loginController);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });
        latch.await(2, java.util.concurrent.TimeUnit.SECONDS);
    }
}
