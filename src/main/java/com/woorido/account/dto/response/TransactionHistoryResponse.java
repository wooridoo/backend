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
        private Long transactionId; // API 스펙상 Long (실제로는 UUID일 수 있으나 일단 Long으로 정의)
        // TODO: UUID -> Long 매핑 이슈 해결 필요. 현재는 임시로 해시나 변환 로직이 필요하거나 스펙 변경 필요.
        // 여기서는 일단 String id를 Long으로 변환하지 못하므로, 차후에 String으로 변경될 가능성 높음.
        // 하지만 일단은 스펙대로 Long으로 둠.

        private String type;
        private Long amount;
        private Long balance; // balanceAfter
        private String description;
        private RelatedChallenge relatedChallenge;
        private String createdAt; // ISO 8601 format
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelatedChallenge {
        private Long challengeId; // API 스펙상 Long
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
        private String startDate; // YYYY-MM-DD
        private String endDate; // YYYY-MM-DD
    }
}
