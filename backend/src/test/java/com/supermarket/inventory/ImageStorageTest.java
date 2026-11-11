package com.supermarket.inventory;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supermarket.inventory.common.BusinessException;
import com.supermarket.inventory.common.files.ImageStorage;
import com.supermarket.inventory.profile.service.*;
import com.supermarket.inventory.security.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.*;
import java.time.Clock;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class ImageStorageTest {
  @TempDir Path directory;

  static byte[] png(int width, int height) throws Exception {
    var output = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "png", output);
    return output.toByteArray();
  }

  @Test
  void decodesRealImageAndUsesGeneratedFilename() throws Exception {
    var storage = new ImageStorage(directory.toString());
    var image =
        storage.store(new MockMultipartFile("file", "../../custom.png", "image/png", png(64, 80)));
    assertThat(image.width()).isEqualTo(64);
    assertThat(image.height()).isEqualTo(80);
    assertThat(image.path()).matches("[a-f0-9-]{36}\\.png");
    assertThat(storage.read(image.path()).exists()).isTrue();
    assertThatThrownBy(() -> storage.read("../secret.png")).isInstanceOf(BusinessException.class);
    storage.discard(image.path());
    assertThat(directory.resolve(image.path())).doesNotExist();
  }

  @Test
  void rejectsDisguisedSvgOversizedAndOutOfBoundsImages() throws Exception {
    var storage = new ImageStorage(directory.toString());
    for (var file :
        new MockMultipartFile[] {
          new MockMultipartFile(
              "file", "fake.png", "image/png", "<script>alert(1)</script>".getBytes()),
          new MockMultipartFile("file", "test.svg", "image/svg+xml", "<svg/>".getBytes()),
          new MockMultipartFile("file", "large.png", "image/png", new byte[2 * 1024 * 1024 + 1]),
          new MockMultipartFile("file", "small.png", "image/png", png(63, 64)),
          new MockMultipartFile("file", "wide.png", "image/png", png(4097, 64))
        }) {
      assertThatThrownBy(() -> storage.store(file)).isInstanceOf(BusinessException.class);
    }
    try (var files = Files.list(directory)) {
      assertThat(files.count()).isZero();
    }
  }

  @Test
  void failedDatabaseBindingCleansNewFileAndPreservesOldFiles() throws Exception {
    var storage = new ImageStorage(directory.toString());
    var binding = mock(AvatarBindingService.class);
    when(binding.bind(anyLong(), anyString(), anyString()))
        .thenThrow(new IllegalStateException("simulated database failure"));
    var service =
        new AvatarService(
            new com.supermarket.inventory.common.files.ImageLifecycle(
                storage,
                mock(com.supermarket.inventory.common.files.ImageReferenceMapper.class),
                new ObjectMapper(),
                Clock.systemUTC()),
            binding,
            mock(AuthService.class));
    assertThatThrownBy(
            () ->
                service.upload(
                    1, new MockMultipartFile("file", "test.png", "image/png", png(64, 64))))
        .isInstanceOf(IllegalStateException.class);
    try (var files = Files.list(directory)) {
      assertThat(files.count()).isZero();
    }
    Files.writeString(directory.resolve("shared.jpg"), "shared fixture");
    storage.discard("shared.jpg");
    assertThat(directory.resolve("shared.jpg")).exists();
  }
}
