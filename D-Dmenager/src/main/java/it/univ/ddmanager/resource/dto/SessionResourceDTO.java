package it.univ.ddmanager.resource.dto;

import java.time.Instant;
import lombok.Data;

@Data
public class SessionResourceDTO {
    private Long id;
    private Long sessionId;
    private String fileName;
    private String fileType;
    private String fileUrl;
    private Long fileSize;
    private Instant uploadedAt;
}
