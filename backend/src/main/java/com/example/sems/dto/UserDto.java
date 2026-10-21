package com.example.sems.dto;

import com.example.sems.common.Gender;
import com.example.sems.common.Role;
import com.example.sems.common.Status;
import com.example.sems.domain.User;

import java.time.Instant;

public class UserDto {
    public Long id;
    public String username;
    public String realName;
    public Role role;
    public Gender gender;
    public Integer age;
    public String phone;
    public String studentNo;
    public Long classId;
    public Status status;
    public Instant createdAt;

    public static UserDto from(User u) {
        UserDto d = new UserDto();
        d.id = u.getId();
        d.username = u.getUsername();
        d.realName = u.getRealName();
        d.role = u.getRole();
        d.gender = u.getGender();
        d.age = u.getAge();
        d.phone = u.getPhone();
        d.studentNo = u.getStudentNo();
        d.classId = u.getClassId();
        d.status = u.getStatus();
        d.createdAt = u.getCreatedAt();
        return d;
    }
}
