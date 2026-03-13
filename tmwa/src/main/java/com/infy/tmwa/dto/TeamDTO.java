package com.infy.tmwa.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class TeamDTO {
    private String name;        // required
    private String description; // optional
}