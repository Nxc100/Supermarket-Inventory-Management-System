package com.supermarket.inventory.common.files;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.Map;
import org.slf4j.*;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * File IO stays outside the short binding transaction; only generated, unreferenced paths are
 * reclaimed.
 */
@Service
public class ImageLifecycle {
  private static final Logger LOG = LoggerFactory.getLogger(ImageLifecycle.class);
  private final ImageStorage storage;
  private final ImageReferenceMapper references;
  private final ObjectMapper json;
  private final Clock clock;

  public ImageLifecycle(
      ImageStorage storage, ImageReferenceMapper references, ObjectMapper json, Clock clock) {
    this.storage = storage;
    this.references = references;
    this.json = json;
    this.clock = clock;
  }

  public record Prepared(String path, String metadata) {}

  public Prepared prepare(MultipartFile file) {
    var image = storage.store(file);
    try {
      return new Prepared(
          image.path(),
          json.writeValueAsString(
              Map.of(
                  "mime",
                  image.mime(),
                  "size",
                  image.size(),
                  "width",
                  image.width(),
                  "height",
                  image.height(),
                  "uploadedAt",
                  clock.instant().toString())));
    } catch (JsonProcessingException e) {
      storage.discard(image.path());
      throw new IllegalStateException("Image metadata cannot be encoded", e);
    }
  }

  public void discard(Prepared image) {
    storage.discard(image.path());
  }

  public void cleanup(String path) {
    if (!ImageStorage.isGenerated(path)) return;
    try {
      if (references.count(path) == 0) storage.discard(path);
    } catch (DataAccessException e) {
      LOG.warn("Image cleanup deferred; the committed image binding remains valid");
    }
  }
}
