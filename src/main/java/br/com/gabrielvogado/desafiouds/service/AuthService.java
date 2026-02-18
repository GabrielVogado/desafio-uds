package br.com.gabrielvogado.desafiouds.service;

import br.com.gabrielvogado.desafiouds.dto.AuthRequest;
import br.com.gabrielvogado.desafiouds.dto.AuthResponse;
import br.com.gabrielvogado.desafiouds.dto.RegisterRequest;
import br.com.gabrielvogado.desafiouds.exception.UserAlreadyExistsException;
import br.com.gabrielvogado.desafiouds.model.User;
import br.com.gabrielvogado.desafiouds.repository.UserRepository;
import br.com.gabrielvogado.desafiouds.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthResponse login(AuthRequest authRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authRequest.getUsername(),
                            authRequest.getPassword()
                    )
            );

            String token = jwtTokenProvider.generateToken(authentication);
            User user = userRepository.findByUsername(authRequest.getUsername()).get();

            return AuthResponse.builder()
                    .token(token)
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .role(user.getRole())
                    .build();
        } catch (AuthenticationException e) {
            throw new br.com.gabrielvogado.desafiouds.exception.AuthenticationException("Invalid credentials");
        }
    }

    @Transactional
    public AuthResponse register(RegisterRequest registerRequest) {
        if (registerRequest == null || registerRequest.getUsername() == null || registerRequest.getEmail() == null) {
            throw new IllegalArgumentException("Username, email and password are required");
        }

        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new UserAlreadyExistsException("Username already exists: " + registerRequest.getUsername());
        }

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new UserAlreadyExistsException("Email already exists: " + registerRequest.getEmail());
        }

        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPasswordHash(passwordEncoder.encode(registerRequest.getPassword()));
        user.setRole(User.UserRole.USER);

        User savedUser = userRepository.save(user);

        String token = jwtTokenProvider.generateToken(savedUser.getUsername());

        return AuthResponse.builder()
                .token(token)
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .build();
    }
}

