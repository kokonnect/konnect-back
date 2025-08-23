// src/main/java/com/example/konnect_backend/domain/user/service/UserService.java
package com.example.konnect_backend.domain.user.service;

import com.example.konnect_backend.domain.user.dto.ChildDto;
import com.example.konnect_backend.domain.user.dto.ChildUpdateDto;
import com.example.konnect_backend.domain.user.entity.Child;
import com.example.konnect_backend.domain.user.entity.User;
import com.example.konnect_backend.domain.user.repository.ChildRepository;
import com.example.konnect_backend.domain.user.repository.UserRepository;
import com.example.konnect_backend.global.code.status.ErrorStatus;
import com.example.konnect_backend.global.exception.GeneralException;
import com.example.konnect_backend.global.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final ChildRepository childRepository;

    @Transactional
    public List<ChildDto> addChildren(List<ChildDto> childDtos) {
        Long userId = SecurityUtil.getCurrentUserIdOrNull();
        if (userId == null) {
            throw new GeneralException(ErrorStatus.UNAUTHORIZED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        List<Child> children = childDtos.stream()
                .map(dto -> Child.builder()
                        .user(user)
                        .name(dto.getName())
                        .school(dto.getSchool())
                        .grade(dto.getGrade())
                        .birthDate(dto.getBirthDate())
                        .className(dto.getClassName())
                        .teacherName(dto.getTeacherName())
                        .build())
                .collect(Collectors.toList());

        List<Child> savedChildren = childRepository.saveAll(children);
        return savedChildren.stream()
                .map(ChildDto::from)
                .collect(Collectors.toList());
    }

    public List<ChildDto> getChildren() {
        Long userId = SecurityUtil.getCurrentUserIdOrNull();
        if (userId == null) {
            throw new GeneralException(ErrorStatus.UNAUTHORIZED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        List<Child> children = childRepository.findByUser(user);
        return children.stream()
                .map(ChildDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public ChildDto updateChild(Long childId, ChildUpdateDto updateDto) {
        Long userId = SecurityUtil.getCurrentUserIdOrNull();
        if (userId == null) {
            throw new GeneralException(ErrorStatus.UNAUTHORIZED);
        }

        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.CHILD_NOT_FOUND));

        if (!child.getUser().getId().equals(userId)) {
            throw new GeneralException(ErrorStatus.FORBIDDEN);
        }

        child.update(
                updateDto.getName(),
                updateDto.getSchool(),
                updateDto.getGrade(),
                updateDto.getBirthDate(),
                updateDto.getClassName(),
                updateDto.getTeacherName()
        );

        Child saved = childRepository.save(child);
        return ChildDto.from(saved);
    }

    @Transactional
    public void deleteChild(Long childId) {
        Long userId = SecurityUtil.getCurrentUserIdOrNull();
        if (userId == null) {
            throw new GeneralException(ErrorStatus.UNAUTHORIZED);
        }

        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.CHILD_NOT_FOUND));

        if (!child.getUser().getId().equals(userId)) {
            throw new GeneralException(ErrorStatus.FORBIDDEN);
        }

        childRepository.delete(child);
    }
}