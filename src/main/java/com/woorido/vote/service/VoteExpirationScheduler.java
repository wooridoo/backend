package com.woorido.vote.service;

import com.woorido.expense.repository.ExpenseRequestMapper;
import com.woorido.vote.repository.ExpenseVoteMapper;
import com.woorido.vote.repository.GeneralVoteMapper;
import com.woorido.vote.repository.VoteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VoteExpirationScheduler {
  private final VoteMapper voteMapper;
  private final ExpenseVoteMapper expenseVoteMapper;
  private final GeneralVoteMapper generalVoteMapper;
  private final ExpenseRequestMapper expenseRequestMapper;

  @Scheduled(fixedDelayString = "${vote.expiration.fixed-delay-ms:60000}")
  public void expirePendingVotes() {
    voteMapper.expirePendingVotes();
    expenseVoteMapper.expirePendingVotes();
    generalVoteMapper.expirePendingVotes();
    expenseRequestMapper.markExpiredVotingAsRejected();
  }
}
