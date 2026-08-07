package com.threadly.media.media;

import com.threadly.media.config.MediaProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MediaTypePolicyTest {

    @Test
    void classifiesCommonTypes() {
        assertEquals(MediaKind.IMAGE, MediaTypePolicy.kindOf("image/png", "a.png"));
        assertEquals(MediaKind.VIDEO, MediaTypePolicy.kindOf("video/mp4", "clip.mp4"));
        assertEquals(MediaKind.ARCHIVE, MediaTypePolicy.kindOf("application/zip", "bundle.zip"));
        assertEquals(MediaKind.DOCUMENT, MediaTypePolicy.kindOf("application/pdf", "notes.pdf"));
    }

    @Test
    void blocksExecutables() {
        assertTrue(MediaTypePolicy.isBlocked("application/octet-stream", "malware.exe"));
        assertTrue(MediaTypePolicy.isBlocked("text/plain", "run.sh"));
        assertFalse(MediaTypePolicy.isBlocked("application/zip", "ok.zip"));
    }

    @Test
    void categoryCapsPreferImageLimit() {
        MediaProperties props = new MediaProperties();
        props.setMaxImageBytes(5_000_000);
        props.setMaxFileBytes(50_000_000);
        assertEquals(5_000_000, MediaTypePolicy.maxBytesFor(MediaKind.IMAGE, props));
        assertEquals(props.getMaxVideoBytes(), MediaTypePolicy.maxBytesFor(MediaKind.VIDEO, props));
    }
}
