package com.threadly.media.service.impl;

import com.threadly.media.dto.MediaResponse;
import com.threadly.media.entity.MediaFile;
import com.threadly.media.exception.ApiException;
import com.threadly.media.port.ObjectStorage;
import com.threadly.media.repository.MediaFileRepository;
import com.threadly.media.service.MediaService;
import com.threadly.media.util.AuditActors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaServiceImpl implements MediaService {

    private static final Logger log = LoggerFactory.getLogger(MediaServiceImpl.class);

    private final MediaFileRepository repository;
    private final ObjectStorage objectStorage;

    @Override
    @Transactional
    public MediaResponse store(UUID uploaderId, String actorUsername, MultipartFile file) {
        try {
            String by = AuditActors.resolve(actorUsername, uploaderId);
            ObjectStorage.StoredObject stored = objectStorage.store(file);
            MediaFile saved = repository.save(MediaFile.builder()
                    .originalName(stored.originalName())
                    .storedName(stored.storedName())
                    .contentType(stored.contentType())
                    .sizeBytes(stored.sizeBytes())
                    .uploaderId(uploaderId)
                    .uploader(Map.of("id", uploaderId.toString(), "username", by))
                    .mediaKind(stored.kind().name())
                    .createdBy(by)
                    .updatedBy(by)
                    .build());
            log.info("Media stored id={} kind={} uploaderId={}", saved.getId(), saved.getMediaKind(), uploaderId);
            return toResponse(saved);
        } catch (ApiException ex) {
            throw ex;
        } catch (IOException e) {
            log.error("Media store failed: {}", e.getMessage());
            throw new ApiException("Failed to store file", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public MediaResponse meta(UUID id) {
        return toResponse(find(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Resource loadAsResource(UUID id) {
        MediaFile media = find(id);
        return objectStorage.load(media.getStoredName());
    }

    @Override
    @Transactional(readOnly = true)
    public MediaFile find(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ApiException("Media not found", HttpStatus.NOT_FOUND.value()));
    }

    private MediaResponse toResponse(MediaFile m) {
        String username = m.getUploader() != null && m.getUploader().get("username") != null
                ? String.valueOf(m.getUploader().get("username")) : null;
        return MediaResponse.builder()
                .id(m.getId())
                .originalName(m.getOriginalName())
                .contentType(m.getContentType())
                .kind(m.getMediaKind())
                .sizeBytes(m.getSizeBytes())
                .uploaderId(m.getUploaderId())
                .uploaderUsername(username)
                .uploader(m.getUploader())
                .url("/api/v1/media/" + m.getId() + "/content")
                .createdAt(m.getCreatedAt())
                .createdBy(m.getCreatedBy())
                .updatedBy(m.getUpdatedBy())
                .build();
    }
}
