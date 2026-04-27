package it.univ.ddmanager.resource.controller;

import it.univ.ddmanager.resource.dto.SessionResourceDTO;
import it.univ.ddmanager.resource.service.SessionResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SessionResourceController {

    private final SessionResourceService sessionResourceService;

    @PostMapping("/sessions/{sessionId}/upload")
    public ResponseEntity<SessionResourceDTO> uploadFile(
            @PathVariable Long sessionId,
            @RequestParam("file") MultipartFile file) {
        SessionResourceDTO resource = sessionResourceService.uploadResource(sessionId, file);
        return ResponseEntity.ok(resource);
    }

    @GetMapping("/sessions/{sessionId}/resources")
    public ResponseEntity<List<SessionResourceDTO>> getSessionResources(@PathVariable Long sessionId) {
        return ResponseEntity.ok(sessionResourceService.getSessionResources(sessionId));
    }

    @GetMapping("/resources/file/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        Resource resource = sessionResourceService.loadFileAsResource(fileName);

        String contentType = null;
        try {
            contentType = Files.probeContentType(resource.getFile().toPath());
        } catch (IOException ex) {
            // Fallback to default
        }

        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
