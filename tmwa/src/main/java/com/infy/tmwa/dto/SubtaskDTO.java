package com.infy.tmwa.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class SubtaskDTO {
    private String title;      // required on create
    private Long   assignedTo; // optional user id
}