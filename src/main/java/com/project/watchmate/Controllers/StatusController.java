package com.project.watchmate.Controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.watchmate.Dto.UpdateWatchStatusRequestDTO;
import com.project.watchmate.Dto.UserMediaStatusDTO;
import com.project.watchmate.Models.UserPrincipal;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Services.StatusService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/status")
@RequiredArgsConstructor
public class StatusController {

	private final StatusService statusService;

	@PostMapping("/update")
	public ResponseEntity<UserMediaStatusDTO> updateStatus(
			@AuthenticationPrincipal UserPrincipal userPrincipal,
			@RequestBody UpdateWatchStatusRequestDTO request) {
		Users user = userPrincipal.getUser();
		UserMediaStatusDTO dto = statusService.updateWatchStatus(user, request);
		return ResponseEntity.ok(dto);
	}
}


