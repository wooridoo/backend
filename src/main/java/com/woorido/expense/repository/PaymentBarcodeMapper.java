package com.woorido.expense.repository;

import com.woorido.expense.domain.PaymentBarcode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PaymentBarcodeMapper {
  void insert(PaymentBarcode barcode);

  PaymentBarcode findByExpenseRequestId(@Param("expenseRequestId") String expenseRequestId);
}
