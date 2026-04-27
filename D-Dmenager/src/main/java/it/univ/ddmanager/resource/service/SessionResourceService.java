package it.univ.ddmanager.resource.service;

import it.univ.ddmanager.campaign.model.Session;
import it.univ.ddmanager.campaign.repository.SessionRepository;
import it.univ.ddmanager.common.ResourceNotFoundException;
import it.univ.ddmanager.resource.dto.SessionResourceDTO;
import it.univ.ddmanager.resource.model.SessionResource;
import it.univ.ddmanager.resource.repository.SessionResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SessionResourceService {

    private final SessionResourceRepository sessionResourceRepository;
    private final SessionRepository sessionRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public SessionResourceDTO uploadResource(Long sessionId, MultipartFile file) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with id " + sessionId));

        String fileName = fileStorageService.store(file);

        // Build download URL
        String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/resources/file/")
                .path(fileName)
                .toUriString();

        SessionResource resource = new SessionResource();
        resource.setSession(session);
        resource.setFileName(file.getOriginalFilename());
        resource.setFileType(identifyFileType(file));
        resource.setFileUrl(fileDownloadUri);
        resource.setFileSize(file.getSize());

        SessionResource saved = sessionResourceRepository.save(resource);
        return mapToDTO(saved);
    }

    private String identifyFileType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null) {
            if (contentType.startsWith("image/"))
                return "IMAGE";
            if (contentType.equals("application/pdf"))
                return "PDF";
        }
        return "OTHER";
    }

    public List<SessionResourceDTO> getSessionResources(Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with id " + sessionId));
        return sessionResourceRepository.findBySessionOrderByUploadedAtDesc(session)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public Resource loadFileAsResource(String fileName) {
        return fileStorageService.loadAsResource(fileName);
    }

    private SessionResourceDTO mapToDTO(SessionResource entity) {
        SessionResourceDTO dto = new SessionResourceDTO();
        dto.setId(entity.getId());
        dto.setSessionId(entity.getSession().getId());
        dto.setFileName(entity.getFileName());
        dto.setFileType(entity.getFileType());
        dto.setFileUrl(entity.getFileUrl());
        dto.setFileSize(entity.getFileSize());
        dto.setUploadedAt(entity.getUploadedAt());
        return dto;
    }
}
