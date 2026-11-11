package com.supermarket.inventory.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CoreConfiguration {
  public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

  @Bean
  Clock clock() {
    return Clock.system(ZONE);
  }

  @Bean
  Jackson2ObjectMapperBuilderCustomizer json() {
    return builder -> {
      builder.featuresToDisable(DeserializationFeature.ACCEPT_FLOAT_AS_INT);
      SimpleModule module = new SimpleModule();
      // IDs use String in public VOs; Long represents quantities and stays numeric.
      module.addSerializer(
          BigDecimal.class,
          new JsonSerializer<>() {
            @Override
            public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider provider)
                throws IOException {
              gen.writeString(value.setScale(2).toPlainString());
            }
          });
      module.addSerializer(
          LocalDateTime.class,
          new JsonSerializer<>() {
            @Override
            public void serialize(
                LocalDateTime value, JsonGenerator gen, SerializerProvider provider)
                throws IOException {
              gen.writeString(value.atZone(ZONE).toOffsetDateTime().toString());
            }
          });
      builder.modulesToInstall(module);
    };
  }
}
