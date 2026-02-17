package com.woorido.account.repository;

import org.apache.ibatis.annotations.Mapper;
import com.woorido.account.domain.Session;

@Mapper
public interface SessionMapper {
  // Learning note:
  // - Methods here map 1:1 to MyBatis XML statement ids.
    void save(Session session);

    Session findById(String id);

    int markAsUsed(String id);

    int markAsUsedIfUnused(String id);
}
