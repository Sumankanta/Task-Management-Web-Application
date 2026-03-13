package com.infy.tmwa.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AttachmentListDTO{

    private Long id;
    private Long taskId;
    private Long uploaderId;
    private String uploaderName;
    private String originalName;
    private String mimeType;
    private Long fileSizeBytes;
    private LocalDateTime uploadedAt;
}
