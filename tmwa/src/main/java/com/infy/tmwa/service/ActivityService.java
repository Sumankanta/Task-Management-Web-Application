package com.infy.tmwa.service;

import com.infy.tmwa.entity.ActivityLog;
import com.infy.tmwa.entity.User;
import com.infy.tmwa.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityRepository activityRepository;

    public List<ActivityLog> getRecentActivity(User user) {
        return activityRepository
                .findTop20ByActorOrderByCreatedAtDesc(user);
    }

    public void logActivity(
            User actor,
            Long taskId,
            String actionCode,
            String message
    ) {

        ActivityLog log = ActivityLog.builder()
                .actor(actor)
                .taskId(taskId)
                .actionCode(actionCode)
                .message(message)
                .build();

        activityRepository.save(log);
    }

}