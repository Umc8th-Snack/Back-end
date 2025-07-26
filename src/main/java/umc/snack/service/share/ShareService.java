package umc.snack.service.share;

import umc.snack.domain.share.dto.ShareResultDto;
import umc.snack.domain.share.dto.SharedArticleContentDto;

public interface ShareService {
    ShareResultDto createShareLink(Long articleId, Long userId);
    SharedArticleContentDto getSharedArticleByUuid(String uuid);
}