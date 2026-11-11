package com.supermarket.inventory.common.files;

import com.supermarket.inventory.common.BusinessException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/** Untrusted uploads are decoded and re-encoded before becoming public resources. */
@Component
public class ImageStorage {
  private static final Logger LOG = LoggerFactory.getLogger(ImageStorage.class);
  private static final Pattern GENERATED = Pattern.compile("[a-f0-9-]{36}\\.(png|jpg)");
  private final Path root;

  public ImageStorage(@Value("${app.upload-directory}") String directory) {
    root = Path.of(directory).toAbsolutePath().normalize();
  }

  public record StoredImage(String path, String mime, long size, int width, int height) {}

  public StoredImage store(MultipartFile file) {
    if (file.isEmpty() || file.getSize() > 2 * 1024 * 1024) throw invalid("图片不能为空，且不得超过2 MB");
    String filename = file.getOriginalFilename();
    String lower = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
    if (!lower.matches(".*\\.(jpg|jpeg|png)$")) throw invalid("仅支持 JPG、JPEG 或 PNG 图片");
    Path temporary = null;
    try (InputStream input = file.getInputStream();
        ImageInputStream imageInput = ImageIO.createImageInputStream(input)) {
      if (imageInput == null) throw invalid("图片无法正常解码");
      var readers = ImageIO.getImageReaders(imageInput);
      if (!readers.hasNext()) throw invalid("图片内容不是有效的 JPG 或 PNG");
      ImageReader reader = readers.next();
      BufferedImage image;
      String format;
      int width, height;
      try {
        format = reader.getFormatName().toLowerCase(Locale.ROOT);
        if (!format.equals("png") && !format.equals("jpeg") && !format.equals("jpg"))
          throw invalid("图片内容格式不受支持");
        reader.setInput(imageInput, true, true);
        width = reader.getWidth(0);
        height = reader.getHeight(0);
        if (width < 64 || height < 64 || width > 4096 || height > 4096)
          throw invalid("图片宽高必须为64～4096像素");
        image = reader.read(0);
        if (image == null) throw invalid("图片无法正常解码");
      } finally {
        reader.dispose();
      }
      Files.createDirectories(root);
      String extension = format.equals("png") ? "png" : "jpg";
      String path = UUID.randomUUID() + "." + extension;
      temporary = Files.createTempFile(root, "upload-", ".tmp");
      if (!ImageIO.write(image, extension, temporary.toFile())) throw invalid("图片无法正常编码");
      long size = Files.size(temporary);
      if (size > 2 * 1024 * 1024) throw invalid("解码后的图片超过2 MB，请压缩后上传");
      Files.move(temporary, resolve(path), StandardCopyOption.ATOMIC_MOVE);
      return new StoredImage(
          path, extension.equals("png") ? "image/png" : "image/jpeg", size, width, height);
    } catch (BusinessException e) {
      throw e;
    } catch (javax.imageio.IIOException e) {
      throw invalid("图片内容损坏，无法正常解码");
    } catch (IOException e) {
      throw new BusinessException(
          HttpStatus.SERVICE_UNAVAILABLE, "FILE_UNAVAILABLE", "图片保存失败，请检查文件目录后重试，原图片保持不变");
    } finally {
      if (temporary != null)
        try {
          Files.deleteIfExists(temporary);
        } catch (IOException e) {
          LOG.warn("Temporary image cleanup deferred");
        }
    }
  }

  public Resource read(String filename) {
    if (!isGenerated(filename)) throw BusinessException.notFound();
    Path path = resolve(filename);
    if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) throw BusinessException.notFound();
    try {
      return new UrlResource(path.toUri());
    } catch (java.net.MalformedURLException e) {
      throw BusinessException.notFound();
    }
  }

  public void discard(String filename) {
    // Sample/shared names and user-supplied paths can never be deleted by this component.
    if (!isGenerated(filename)) return;
    try {
      Files.deleteIfExists(resolve(filename));
    } catch (IOException e) {
      LOG.warn("Unreferenced image cleanup deferred for {}", filename);
    }
  }

  public static boolean isGenerated(String filename) {
    return filename != null && GENERATED.matcher(filename).matches();
  }

  private Path resolve(String filename) {
    Path path = root.resolve(filename).normalize();
    if (!path.startsWith(root) || !path.getParent().equals(root))
      throw BusinessException.notFound();
    return path;
  }

  private BusinessException invalid(String message) {
    return new BusinessException(HttpStatus.BAD_REQUEST, "IMAGE_INVALID", message);
  }
}
