package umc.snack.service.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.snack.common.exception.CustomException;
import umc.snack.common.exception.ErrorCode;
import umc.snack.domain.user.dto.UserClicksDto;
import umc.snack.domain.user.entity.UserClicks;
import umc.snack.repository.user.UserClickRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserClickService {
    
    private final UserClickRepository userClickRepository;
    
    public void saveUserClick(UserClicksDto userClicksDto) {
        try {
            // 중복 클릭 방지: 같은 사용자가 같은 기사를 이미 클릭했는지 확인
            boolean exists = userClickRepository.existsByUserIdAndArticleId(
                userClicksDto.getUserId(), 
                userClicksDto.getArticleId()
            );
            
            if (!exists) {
                UserClicks userClick = UserClicks.builder()
                    .clickId(System.currentTimeMillis()) // 현재 시간을 clickId로 사용
                    .userId(userClicksDto.getUserId())
                    .articleId(userClicksDto.getArticleId())
                    .build();
                
                userClickRepository.save(userClick);
            }
        } catch (Exception e) {
            throw new CustomException(ErrorCode.CLICK_9400);
        }
    }
    
    @Transactional(readOnly = true)
    public List<Long> getClickedArticleIds(Long userId) {
        List<UserClicks> userClicks = userClickRepository.findTop15ByUserIdOrderByCreatedAtDesc(userId);
        
        if (userClicks.isEmpty()) {
            throw new CustomException(ErrorCode.FEED_9505);
        }
        
        return userClicks.stream()
                .map(UserClicks::getArticleId)
                .collect(Collectors.toList());
    }
}
