package com.threadly.media.service;

import com.threadly.media.dto.MediaResponse;
import com.threadly.media.entity.MediaFile;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface MediaService {

    MediaResponse store(UUID uploaderId, String actorUsername, MultipartFile file);

    MediaResponse meta(UUID id);

    Resource loadAsResource(UUID id);

    MediaFile find(UUID id);
}
