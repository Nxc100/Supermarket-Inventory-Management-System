package com.supermarket.inventory.system.vo;

import com.fasterxml.jackson.databind.JsonNode;

public record OperationLogDetailVO(OperationLogVO record, JsonNode before, JsonNode after) {}
