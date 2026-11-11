package com.supermarket.inventory.profile.service;

import com.supermarket.inventory.common.files.ImageLifecycle;
import com.supermarket.inventory.security.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AvatarService {
  private final ImageLifecycle images;
  private final AvatarBindingService binding;
  private final AuthService auth;

  public AvatarService(ImageLifecycle images, AvatarBindingService binding, AuthService auth) {
    this.images = images;
    this.binding = binding;
    this.auth = auth;
  }

  public UserVO upload(long id, MultipartFile file) {
    var image = images.prepare(file);
    String old;
    try {
      old = binding.bind(id, image.path(), image.metadata());
    } catch (RuntimeException e) {
      images.discard(image);
      throw e;
    }
    images.cleanup(old);
    return auth.me(id);
  }

  public UserVO remove(long id) {
    String old = binding.bind(id, null, null);
    images.cleanup(old);
    return auth.me(id);
  }
}
