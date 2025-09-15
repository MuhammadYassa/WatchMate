package com.project.watchmate.Services;

import org.springframework.stereotype.Service;

import com.project.watchmate.Dto.FollowListDTO;
import com.project.watchmate.Dto.FollowStatusDTO;
import com.project.watchmate.Dto.UserProfileDTO;
import com.project.watchmate.Models.Users;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SocialService {public FollowStatusDTO followUser(Long userId, Users user) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'followUser'");
    }

public FollowStatusDTO unfollowUser(Long userId, Users user) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'unfollowUser'");
}

public FollowStatusDTO getFollowStatus(Long userId, Users user) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getFollowStatus'");
}

public FollowStatusDTO blockUser(Long userId, Users user) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'blockUser'");
}

public FollowListDTO getFollowersList(Users user) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getFollowersList'");
}

public FollowListDTO getFollowingList(Users user) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getFollowingList'");
}

public UserProfileDTO getUserProfile(Users user, Long userId) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getUserProfile'");
}

}
