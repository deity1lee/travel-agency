package com.epam.edp.demo.repository.staff;

import com.epam.edp.demo.entity.auth.Role;
import com.epam.edp.demo.entity.staff.Staff;

import java.util.List;
import java.util.Optional;

public interface StaffRepository {
    boolean staffExists(String email, Role role);
    Optional<Staff> getAnyAvailableTravelAgent();
    void saveAll(List<Staff> travelAgents);
}
