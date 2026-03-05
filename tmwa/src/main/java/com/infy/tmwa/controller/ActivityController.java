//package com.infy.tmwa.controller;
//
//import com.infy.tmwa.entity.ActivityLog;
//import com.infy.tmwa.service.ActivityService;
//import com.infy.tmwa.entity.User;
//import lombok.RequiredArgsConstructor;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/activity")
//@RequiredArgsConstructor
//@CrossOrigin
//public class ActivityController {
//
//    private final ActivityService activityService;
//
//    @GetMapping
//    public List<ActivityLog> getActivity(
//            @AuthenticationPrincipal User user
//    ) {
//        return activityService.getRecentActivity(user);
//    }
//}