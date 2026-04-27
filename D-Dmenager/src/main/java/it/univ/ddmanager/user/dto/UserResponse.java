// src/main/java/it/univ/ddmanager/user/dto/UserResponse.java
package it.univ.ddmanager.user.dto;

import it.univ.ddmanager.security.UserPrincipal;
import it.univ.ddmanager.user.model.Role;
import it.univ.ddmanager.user.model.User;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String email;
    private String nickname;
    private Set<String> roles;

    public static UserResponse fromEntity(User user) {
        Set<String> roleNames = user.getRoles().stream()
            .map(Role::getName)
            .collect(Collectors.toSet());
        return new UserResponse(user.getId(), user.getEmail(), user.getNickname(), roleNames);
    }

    public static UserResponse fromPrincipal(UserPrincipal principal) {
        Set<String> roleNames = principal.getAuthorities().stream()
            .map(authority -> authority.getAuthority())
            .collect(Collectors.toSet());
        return new UserResponse(principal.getId(), principal.getUsername(), principal.getNickname(), roleNames);
    }
}
