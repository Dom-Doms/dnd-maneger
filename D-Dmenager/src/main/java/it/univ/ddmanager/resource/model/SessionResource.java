package it.univ.ddmanager.resource.model;

import it.univ.ddmanager.campaign.model.Session;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "session_resources")
@Getter
@Setter
@NoArgsConstructor
public class SessionResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String fileType; // MIME type or simple category like "IMAGE", "PDF"

    @Column(nullable = false)
    private String fileUrl; // Path or URL to access the file

    @Column(nullable = false)
    private Long fileSize; // Size in bytes

    @Column(nullable = false)
    private Instant uploadedAt = Instant.now();
}
