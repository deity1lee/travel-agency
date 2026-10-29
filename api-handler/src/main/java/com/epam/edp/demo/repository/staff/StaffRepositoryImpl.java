package com.epam.edp.demo.repository.staff;

import com.epam.edp.demo.entity.auth.Role;
import com.epam.edp.demo.entity.staff.Staff;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Repository
public class StaffRepositoryImpl implements StaffRepository {
    private final MongoTemplate mongoTemplate;

    @Autowired
    public StaffRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public boolean staffExists(String email, Role role) {
        log.info("Checking if user {} is {}", email, role);
        Query query = Query.query(Criteria
                .where("email").is(email)
                .and("role").is(role));
        return mongoTemplate.exists(query, Staff.class);
    }

    @Override
    public Optional<Staff> getAnyAvailableTravelAgent() {
        log.info("Fetching any travel agent");
        List<Staff> agents = mongoTemplate.findAll(Staff.class).stream()
                .filter(staff -> staff.getRole() == Role.TRAVEL_AGENT)
                .toList();
        return Optional.ofNullable(agents.get(ThreadLocalRandom.current().nextInt(agents.size())));
    }

    @Override
    public void saveAll(List<Staff> staff) {
        if (staff.isEmpty()) {
            log.info("No staff to save");
            return;
        }
        log.info("Saving {} staff in batch", staff.size());
        mongoTemplate.insertAll(staff);
        log.info("Saved {} staff", staff.size());
    }
}
