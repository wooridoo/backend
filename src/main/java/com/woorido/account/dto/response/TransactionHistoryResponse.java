package com.woorido.account.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionHistoryResponse {
    private List<TransactionItem> content;
    private PageInfo page;
    private Summary summary;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionItem {
        private String transactionId;
        private String type;
        private Long amount;
        private Long balance;
        private String description;
        private RelatedChallenge relatedChallenge;
        private String createdAt;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelatedChallenge {
        private String challengeId;
        private String name;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageInfo {
        private Integer number;
        private Integer size;
        private Long totalElements;
        private Integer totalPages;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private Long totalIncome;
        private Long totalExpense;
        private Period period;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Period {
        private String startDate;
        private String endDate;
    }
}