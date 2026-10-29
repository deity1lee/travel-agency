package com.epam.edp.demo.entity.booking;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TravelAgentSnapshot {
    private String name;
    private String email;
    private String phone;
    private String messenger;
}
