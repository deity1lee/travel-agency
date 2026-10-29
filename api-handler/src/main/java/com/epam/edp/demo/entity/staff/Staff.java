package com.epam.edp.demo.entity.staff;

import com.epam.edp.demo.entity.auth.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "staff")
public class Staff {
    private String name;
    @Id
    private String email;
    private String phone;
    private String messenger;

    private Role role;
}
