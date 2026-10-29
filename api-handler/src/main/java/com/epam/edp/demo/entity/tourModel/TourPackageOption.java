package com.epam.edp.demo.entity.tourModel;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class TourPackageOption {
    private LocalDate startDate;
    private String duration;
    private int price;
    private int availableAdultSpots;
    private int availableChildSpots;
}
