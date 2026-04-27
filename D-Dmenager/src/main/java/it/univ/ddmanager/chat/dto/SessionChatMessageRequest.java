package it.univ.ddmanager.chat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SessionChatMessageRequest {

    @NotBlank
    private String content;

    @NotBlank
    private String language;

    private Long senderCharacterId;

    private String messageType;

    private Long recipientUserId;
}
