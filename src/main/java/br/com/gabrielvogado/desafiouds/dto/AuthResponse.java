package br.com.gabrielvogado.desafiouds.dto;

import br.com.gabrielvogado.desafiouds.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String token;
    private String username;
    private String email;
    private User.UserRole role;
}

