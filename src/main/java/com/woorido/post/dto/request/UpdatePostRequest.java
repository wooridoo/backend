package com.woorido.post.dto.request;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdatePostRequest {
    private String title;
    private String content;
    private String category;
    private List<String> attachmentIds;
    private List<String> imageUrls;

    public List<String> getImageUrls() {
        if (imageUrls != null) {
            return imageUrls;
        }
        return attachmentIds;
    }

    public List<String> getAttachmentIds() {
        return getImageUrls();
    }
}
