package com.supermarket.inventory.ai.service;

import com.supermarket.inventory.ai.client.SuggestionFailure;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.security.SecureRandom;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.spec.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** The database stores only authenticated ciphertext. The master key never leaves this boundary. */
@Component
public class AiSecretCipher {
  private final String configuredMaster;
  private final Path file;
  private final SecureRandom random = new SecureRandom();

  public AiSecretCipher(
      @Value("${app.ai.config-master-key:}") String master,
      @Value("${app.ai.config-master-file:.local/secrets/ai-master.key}") String file) {
    this.configuredMaster = master;
    this.file = Path.of(file);
  }

  private synchronized byte[] master(boolean create) throws Exception {
    if (!configuredMaster.isBlank()) {
      byte[] key = Base64.getDecoder().decode(configuredMaster.strip());
      if (key.length != 32) throw new IllegalArgumentException();
      return key;
    }
    if (Files.isSymbolicLink(file)) throw new IllegalArgumentException();
    if (!Files.exists(file) && create) {
      Files.createDirectories(file.toAbsolutePath().getParent());
      byte[] key = new byte[32];
      random.nextBytes(key);
      try {
        Files.write(file, key, StandardOpenOption.CREATE_NEW);
      } catch (FileAlreadyExistsException ignored) {
        /* Another server created the same persistent key. */
      }
      var posix = Files.getFileAttributeView(file, PosixFileAttributeView.class);
      if (posix != null) posix.setPermissions(PosixFilePermissions.fromString("rw-------"));
      else {
        var acl = Files.getFileAttributeView(file, AclFileAttributeView.class);
        if (acl != null)
          acl.setAcl(
              List.of(
                  AclEntry.newBuilder()
                      .setType(AclEntryType.ALLOW)
                      .setPrincipal(Files.getOwner(file))
                      .setPermissions(EnumSet.allOf(AclEntryPermission.class))
                      .build()));
      }
    }
    byte[] key = Files.readAllBytes(file);
    if (key.length != 32) throw new IllegalArgumentException();
    return key;
  }

  public String encrypt(String plain) {
    return crypt(plain, true);
  }

  public String decrypt(String encrypted) {
    return crypt(encrypted, false);
  }

  private String crypt(String value, boolean encrypt) {
    try {
      byte[] nonce = new byte[12];
      byte[] input;
      if (encrypt) {
        random.nextBytes(nonce);
        input = value.getBytes(StandardCharsets.UTF_8);
      } else {
        byte[] packed = Base64.getDecoder().decode(value);
        if (packed.length < 29) throw new IllegalArgumentException();
        System.arraycopy(packed, 0, nonce, 0, 12);
        input = Arrays.copyOfRange(packed, 12, packed.length);
      }
      var cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(
          encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE,
          new SecretKeySpec(master(encrypt), "AES"),
          new GCMParameterSpec(128, nonce));
      cipher.updateAAD("supermarket-ai-provider-v1".getBytes(StandardCharsets.UTF_8));
      byte[] output = cipher.doFinal(input);
      if (!encrypt) return new String(output, StandardCharsets.UTF_8);
      byte[] packed = Arrays.copyOf(nonce, nonce.length + output.length);
      System.arraycopy(output, 0, packed, nonce.length, output.length);
      return Base64.getEncoder().encodeToString(packed);
    } catch (Exception ignored) {
      throw new SuggestionFailure("CONFIGURATION", "模型密钥无法加密或解密，请检查后端主密钥文件，或由管理员重新保存API Key");
    }
  }
}
