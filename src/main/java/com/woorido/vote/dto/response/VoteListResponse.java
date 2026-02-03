package com.woorido.vote.dto.response;

import com.woorido.vote.dto.VoteDto;
import com.woorido.common.dto.PageInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoteListResponse {
  private List<VoteDto> content;
  private PageInfo page;
}
