package com.woorido.expense.repository;

import org.apache.ibatis.annotations.Mapper;
import com.woorido.expense.domain.ExpenseRequest;

@Mapper
public interface ExpenseRequestMapper {
    void insert(ExpenseRequest expenseRequest);

    ExpenseRequest findById(String id);
}
