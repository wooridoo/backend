package com.woorido.expense.repository;

import com.woorido.expense.domain.PaymentBarcode;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaymentBarcodeMapper {
  void insert(PaymentBarcode barcode);

  PaymentBarcode findByExpenseRequestId(String expenseRequestId);
}
