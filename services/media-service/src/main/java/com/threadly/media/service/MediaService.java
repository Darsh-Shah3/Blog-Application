package com.threadly.media.service;

import com.threadly.media.dto.MediaResponse;
import com.threadly.media.entity.MediaFile;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface MediaService {

    MediaResponse store(Long uploaderId, String actorUsername, MultipartFile file);

    MediaResponse meta(Long id);

    Resource loadAsResource(Long id);

    MediaFile find(Long id);
}
