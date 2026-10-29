package com.epam.edp.demo.entity.travelagent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "travel_agents")
public class TravelAgent {
    private String name;
    @Id
    private String email;
    private String phone;
    private String messenger;
}
