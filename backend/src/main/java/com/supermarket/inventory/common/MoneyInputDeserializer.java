package com.supermarket.inventory.common;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import java.io.IOException;
import java.math.BigDecimal;

/** Monetary commands accept explicit two-decimal strings, never JSON floating point numbers. */
public class MoneyInputDeserializer extends JsonDeserializer<BigDecimal> {
  @Override
  public BigDecimal deserialize(JsonParser parser, DeserializationContext context)
      throws IOException {
    if (parser.currentToken() != JsonToken.VALUE_STRING
        || !parser.getText().matches("[0-9]{1,14}\\.[0-9]{2}"))
      return context.reportInputMismatch(BigDecimal.class, "金额必须是两位小数字符串");
    return new BigDecimal(parser.getText());
  }
}
