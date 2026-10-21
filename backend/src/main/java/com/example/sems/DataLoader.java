package com.example.sems;

import com.example.sems.common.Role;
import com.example.sems.domain.*;
import com.example.sems.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/**
 * Seeds demo data on first run so the system is immediately usable / demoable.
 * Accounts: admin / admin123, teacher / teacher123, student / student123.
 */
@Component
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final VenueRepository venueRepository;
    private final VenueScheduleRepository venueScheduleRepository;
    private final EquipmentRepository equipmentRepository;
    private final PasswordEncoder passwordEncoder;

    public DataLoader(UserRepository userRepository, VenueRepository venueRepository,
                      VenueScheduleRepository venueScheduleRepository, EquipmentRepository equipmentRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.venueRepository = venueRepository;
        this.venueScheduleRepository = venueScheduleRepository;
        this.equipmentRepository = equipmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }
        User admin = user("admin", "admin123", "Admin User", Role.ADMIN);
        User teacher = user("teacher", "teacher123", "Ms. Li", Role.TEACHER);
        user("student", "student123", "Amy Chen", Role.STUDENT);

        Venue badminton = venue("羽毛球场", VenueType.BADMINTON, true, "体育馆", "2号场", 4,
                LocalTime.of(8, 0), LocalTime.of(21, 0), "请穿运动鞋；禁止穿鞋底发黑鞋入场");
        Venue squash = venue("室内壁球场", VenueType.SQUASH, true, "体育馆", "B1壁球馆", 2,
                LocalTime.of(9, 0), LocalTime.of(22, 0), "需预约并借壁球拍");
        Venue tennisIn = venue("室内网球场", VenueType.TENNIS_INDOOR, true, "网球中心", "室内1号", 4,
                LocalTime.of(8, 0), LocalTime.of(22, 0), "灯光费另计");
        Venue tennisOut = venue("室外网球场", VenueType.TENNIS_OUTDOOR, false, "网球中心", "室外2号", 4,
                LocalTime.of(7, 0), LocalTime.of(20, 0), "雨天关闭");
        Venue football = venue("足球场", VenueType.FOOTBALL, false, "操场", "主球场", 22,
                LocalTime.of(8, 0), LocalTime.of(19, 0), "比赛优先，训练需审批");
        Venue volleyball = venue("排球场", VenueType.VOLLEYBALL, false, "操场", "东侧", 12,
                LocalTime.of(8, 0), LocalTime.of(19, 0), "班级活动常用");

        for (Venue v : List.of(badminton, squash, tennisIn, tennisOut, football, volleyball)) {
            for (int wd = DayOfWeek.MONDAY.getValue(); wd <= DayOfWeek.FRIDAY.getValue(); wd++) {
                venueScheduleRepository.save(VenueSchedule.builder()
                        .venueId(v.getId())
                        .weekday(wd)
                        .startTime(v.getOpenTime())
                        .endTime(v.getCloseTime())
                        .build());
            }
            // weekend half-day
            venueScheduleRepository.save(VenueSchedule.builder()
                    .venueId(v.getId())
                    .weekday(DayOfWeek.SATURDAY.getValue())
                    .startTime(LocalTime.of(9, 0))
                    .endTime(LocalTime.of(12, 0))
                    .build());
        }

        equipmentRepository.save(Equipment.builder()
                .name("羽毛球拍").venueId(badminton.getId()).total(10).available(10).build());
        equipmentRepository.save(Equipment.builder()
                .name("壁球拍").venueId(squash.getId()).total(4).available(4).build());
        equipmentRepository.save(Equipment.builder()
                .name("网球").venueId(tennisIn.getId()).total(20).available(20).build());

        // touch references so they are not flagged unused
        admin.getId();
        teacher.getId();
    }

    private User user(String username, String password, String realName, Role role) {
        return userRepository.save(User.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(password))
                .realName(realName)
                .role(role)
                .status(com.example.sems.common.Status.ACTIVE)
                .build());
    }

    private Venue venue(String name, VenueType type, boolean indoor, String building, String location,
                       int capacity, LocalTime open, LocalTime close, String rules) {
        return venueRepository.save(Venue.builder()
                .name(name).type(type).indoor(indoor).building(building).location(location)
                .capacity(capacity).openTime(open).closeTime(close)
                .status(VenueStatus.OPEN).rules(rules).build());
    }
}
