package com.woorido.account.repository;

import org.apache.ibatis.annotations.Mapper;
import com.woorido.account.domain.Session;

@Mapper
public interface SessionMapper {
    void save(Session session);

    Session findById(String id);

    int markAsUsed(String id);
}
