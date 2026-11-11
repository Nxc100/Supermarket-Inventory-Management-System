package com.supermarket.inventory.catalog.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.supermarket.inventory.common.IdInputDeserializer;
import com.supermarket.inventory.common.MoneyInputDeserializer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ProductRequest(
    @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{2,32}", message = "编码须为2～32位字母数字、短横线或下划线")
        String code,
    @Pattern(regexp = "[0-9]{4,32}", message = "条码须为4～32位数字") String barcode,
    @NotBlank @Size(min = 2, max = 100) String name,
    @NotBlank
        @Pattern(regexp = "[1-9][0-9]{0,18}")
        @JsonDeserialize(using = IdInputDeserializer.class)
        String categoryId,
    @Size(max = 100) String specification,
    @NotBlank @Pattern(regexp = "件|瓶|袋|盒|箱|包|罐|支|本|卷", message = "请选择固定基本单位") String unit,
    @JsonDeserialize(using = MoneyInputDeserializer.class)
        @DecimalMin("0.00")
        @Digits(integer = 14, fraction = 2)
        @Schema(type = "string", example = "3.00")
        BigDecimal referenceCost,
    @NotNull
        @JsonDeserialize(using = MoneyInputDeserializer.class)
        @DecimalMin(value = "0.00", inclusive = false)
        @Digits(integer = 14, fraction = 2)
        @Schema(type = "string", example = "5.00")
        BigDecimal salePrice,
    @NotNull Boolean managesExpiry,
    @Min(1) Integer shelfLifeDays,
    @NotNull @Min(0) Integer nearExpiryDays,
    @NotNull @Min(0) @Max(999999999999L) Long stockLowerLimit,
    @NotNull Boolean canPurchase,
    @NotNull Boolean canSell,
    @Size(max = 500) String remark) {
  public ProductRequest {
    if (code != null) code = code.trim();
    if (name != null) name = name.trim();
    if (barcode != null) barcode = barcode.isBlank() ? null : barcode.trim();
  }

  @AssertTrue(message = "管理保质期时须填写正整数天数，普通商品须清空保质期天数")
  public boolean isExpiryValid() {
    return managesExpiry == null
        || (managesExpiry ? shelfLifeDays != null && shelfLifeDays > 0 : shelfLifeDays == null);
  }
}
