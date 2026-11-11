package com.supermarket.inventory.catalog.service;

import com.supermarket.inventory.catalog.vo.ProductVO;
import com.supermarket.inventory.common.files.ImageLifecycle;
import com.supermarket.inventory.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProductImageService {
  private final ProductService products;
  private final ProductImageBindingService binding;
  private final ImageLifecycle images;

  public ProductImageService(
      ProductService products, ProductImageBindingService binding, ImageLifecycle images) {
    this.products = products;
    this.binding = binding;
    this.images = images;
  }

  public ProductVO upload(long id, MultipartFile file, CurrentUser user) {
    products.get(id);
    var image = images.prepare(file);
    String old;
    try {
      old = binding.bind(id, image.path(), image.metadata(), user);
    } catch (RuntimeException e) {
      images.discard(image);
      throw e;
    }
    images.cleanup(old);
    return products.get(id);
  }

  public ProductVO remove(long id, CurrentUser user) {
    String old = binding.bind(id, null, null, user);
    images.cleanup(old);
    return products.get(id);
  }

  public void delete(long id, CurrentUser user) {
    String old = products.delete(id, user);
    images.cleanup(old);
  }
}
