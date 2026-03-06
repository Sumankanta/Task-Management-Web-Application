package com.infy.tmwa.repository;

import com.infy.tmwa.entity.ActivityLog;
import com.infy.tmwa.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityRepository extends JpaRepository<ActivityLog, Long> {

    List<ActivityLog> findTop20ByActorOrderByCreatedAtDesc(User actor);
}