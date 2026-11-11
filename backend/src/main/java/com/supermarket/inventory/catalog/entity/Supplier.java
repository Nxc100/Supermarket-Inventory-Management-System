package com.supermarket.inventory.catalog.entity;

/** Persistence projection restricted to archive-owned writable fields. */
public class Supplier {
  private Long id;
  private String code;
  private String name;
  private String contactName;
  private String contactPhone;
  private String address;
  private String remark;
  private long createdBy;

  public Long getId() {
    return id;
  }

  public void setId(Long value) {
    id = value;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String value) {
    code = value;
  }

  public String getName() {
    return name;
  }

  public void setName(String value) {
    name = value;
  }

  public String getContactName() {
    return contactName;
  }

  public void setContactName(String value) {
    contactName = value;
  }

  public String getContactPhone() {
    return contactPhone;
  }

  public void setContactPhone(String value) {
    contactPhone = value;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String value) {
    address = value;
  }

  public String getRemark() {
    return remark;
  }

  public void setRemark(String value) {
    remark = value;
  }

  public long getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(long value) {
    createdBy = value;
  }
}
