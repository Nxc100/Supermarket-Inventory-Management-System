package com.supermarket.inventory.common;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import java.io.IOException;

public class IdInputDeserializer extends JsonDeserializer<String> {
  @Override
  public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
    if (parser.currentToken() != JsonToken.VALUE_STRING)
      return context.reportInputMismatch(String.class, "ID必须为十进制字符串");
    return parser.getText();
  }
}
