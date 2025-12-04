// src/main/java/com/example/konnect_backend/domain/user/controller/UserController.java
package com.example.konnect_backend.domain.user.controller;

import com.example.konnect_backend.domain.user.dto.ChildDto;
import com.example.konnect_backend.domain.user.dto.ChildUpdateDto;
import com.example.konnect_backend.domain.user.dto.UserInfoDto;
import com.example.konnect_backend.domain.user.service.UserService;
import com.example.konnect_backend.global.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "사용자 관련 API")
public class UserController {

    private final UserService userService;

    @PostMapping("/children")
    @Operation(summary = "자녀 추가", description = "현재 로그인한 사용자에게 자녀를 추가합니다.")
    public ApiResponse<List<ChildDto>> addChildren(@Valid @RequestBody List<ChildDto> children) {
        return ApiResponse.onSuccess(userService.addChildren(children));
    }

    @GetMapping("/children")
    @Operation(summary = "자녀 목록 조회", description = "현재 로그인한 사용자의 자녀 목록을 조회합니다.")
    public ApiResponse<List<ChildDto>> getChildren() {
        return ApiResponse.onSuccess(userService.getChildren());
    }

    @PutMapping("/children/{childId}")
    @Operation(summary = "자녀 정보 수정", description = "자녀 정보를 수정합니다. 변경하고 싶은 필드만 전송하면 됩니다.")
    public ApiResponse<ChildDto> updateChild(@PathVariable Long childId, @RequestBody ChildUpdateDto updateDto) {
        return ApiResponse.onSuccess(userService.updateChild(childId, updateDto));
    }

    @DeleteMapping("/children/{childId}")
    @Operation(summary = "자녀 삭제", description = "자녀를 삭제합니다.")
    public ApiResponse<Void> deleteChild(@PathVariable Long childId) {
        userService.deleteChild(childId);
        return ApiResponse.onSuccess(null);
    }

    @GetMapping("")
    @Operation(summary = "유저 조회", description = "현재 로그인한 사용자의 정보를 조회합니다.")
    public ApiResponse<UserInfoDto> getUserInfo() {
        return ApiResponse.onSuccess(userService.getUserInfo());
    }

}