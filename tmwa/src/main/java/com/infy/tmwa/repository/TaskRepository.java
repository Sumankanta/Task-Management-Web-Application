package com.infy.tmwa.repository;

import com.infy.tmwa.entity.Task;
import com.infy.tmwa.entity.User;
import jdk.jfr.Registered;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

@Registered
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByUser(User user);
}