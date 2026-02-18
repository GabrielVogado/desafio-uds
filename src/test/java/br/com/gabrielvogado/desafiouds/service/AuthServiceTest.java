package br.com.gabrielvogado.desafiouds.service;

import br.com.gabrielvogado.desafiouds.dto.AuthRequest;
import br.com.gabrielvogado.desafiouds.dto.AuthResponse;
import br.com.gabrielvogado.desafiouds.dto.RegisterRequest;
import br.com.gabrielvogado.desafiouds.exception.AuthenticationException;
import br.com.gabrielvogado.desafiouds.exception.UserAlreadyExistsException;
import br.com.gabrielvogado.desafiouds.model.User;
import br.com.gabrielvogado.desafiouds.repository.UserRepository;
import br.com.gabrielvogado.desafiouds.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private AuthRequest loginRequest;
    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hashedpassword")
                .role(User.UserRole.USER)
                .build();

        loginRequest = AuthRequest.builder()
                .username("testuser")
                .password("password123")
                .build();

        registerRequest = RegisterRequest.builder()
                .username("newuser")
                .email("newuser@example.com")
                .password("password123")
                .build();
    }

    @Test
    void testLoginSuccess() {
        // Arrange
        UsernamePasswordAuthenticationToken authToken =
            new UsernamePasswordAuthenticationToken("testuser", "password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authToken);
        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(testUser));
        when(jwtTokenProvider.generateToken(authToken))
                .thenReturn("jwt-token");

        // Act
        AuthResponse response = authService.login(loginRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getUsername()).isEqualTo("testuser");
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getRole()).isEqualTo(User.UserRole.USER);

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository, times(1)).findByUsername("testuser");
        verify(jwtTokenProvider, times(1)).generateToken(authToken);
    }

    @Test
    void testLoginInvalidCredentials_throwsException() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // Act & Assert
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("Invalid username or password");

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository, never()).findByUsername(anyString());
    }

    @Test
    void testRegisterSuccess() {
        // Arrange
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encrypted-password");
        when(userRepository.save(any(User.class))).thenReturn(User.builder()
                .id(2L)
                .username("newuser")
                .email("newuser@example.com")
                .passwordHash("encrypted-password")
                .role(User.UserRole.USER)
                .build());
        when(jwtTokenProvider.generateToken("newuser")).thenReturn("jwt-token");

        // Act
        AuthResponse response = authService.register(registerRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getUsername()).isEqualTo("newuser");
        assertThat(response.getEmail()).isEqualTo("newuser@example.com");

        verify(userRepository, times(1)).existsByUsername("newuser");
        verify(userRepository, times(1)).existsByEmail("newuser@example.com");
        verify(passwordEncoder, times(1)).encode("password123");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testRegisterUsernameTaken_throwsException() {
        // Arrange
        when(userRepository.existsByUsername("newuser")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("Username already exists");

        verify(userRepository, times(1)).existsByUsername("newuser");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testRegisterEmailTaken_throwsException() {
        // Arrange
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("Email already exists");

        verify(userRepository, times(1)).existsByUsername("newuser");
        verify(userRepository, times(1)).existsByEmail("newuser@example.com");
        verify(userRepository, never()).save(any(User.class));
    }
}

