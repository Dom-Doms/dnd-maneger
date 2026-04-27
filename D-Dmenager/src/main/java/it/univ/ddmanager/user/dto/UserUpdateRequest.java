// src/main/java/it/univ/ddmanager/user/dto/UserUpdateRequest.java
package it.univ.ddmanager.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {

    private String nickname;
    private String password;
}
