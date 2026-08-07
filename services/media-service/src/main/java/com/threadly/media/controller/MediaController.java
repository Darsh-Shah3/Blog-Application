package com.threadly.media.controller;

import com.threadly.media.dto.MediaResponse;
import com.threadly.media.entity.MediaFile;
import com.threadly.media.exception.ApiException;
import com.threadly.media.service.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    @PostMapping(path = "/uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaResponse> upload(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Username", required = false) String username,
            @RequestPart("file") MultipartFile file) {
        if (userId == null) {
            throw new ApiException("Authentication required", 401);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(mediaService.store(userId, username, file));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MediaResponse> meta(@PathVariable Long id) {
        return ResponseEntity.ok(mediaService.meta(id));
    }

    @GetMapping("/{id}/content")
    public ResponseEntity<Resource> content(@PathVariable Long id) {
        MediaFile media = mediaService.find(id);
        Resource resource = mediaService.loadAsResource(id);
        String kind = media.getMediaKind() == null ? "OTHER" : media.getMediaKind();
        // Images/videos preview inline; zip and docs download as attachment.
        String disposition = (kind.equals("IMAGE") || kind.equals("VIDEO") || kind.equals("AUDIO"))
                ? "inline"
                : "attachment";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        disposition + "; filename=\"" + media.getOriginalName().replace("\"", "") + "\"")
                .contentType(MediaType.parseMediaType(media.getContentType()))
                .body(resource);
    }
}
