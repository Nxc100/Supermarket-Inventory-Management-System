package com.supermarket.inventory.common.files;

import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class MediaController {
  private final ImageStorage storage;

  public MediaController(ImageStorage storage) {
    this.storage = storage;
  }

  @GetMapping("/media/{filename}")
  public ResponseEntity<Resource> image(@PathVariable String filename) {
    Resource file = storage.read(filename);
    return ResponseEntity.ok()
        .contentType(filename.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG)
        .header("X-Content-Type-Options", "nosniff")
        .header("Content-Security-Policy", "default-src 'none'; sandbox")
        .cacheControl(CacheControl.noCache())
        .body(file);
  }
}
