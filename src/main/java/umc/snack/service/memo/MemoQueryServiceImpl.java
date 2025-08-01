package umc.snack.service.memo;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.snack.common.exception.CustomException;
import umc.snack.common.exception.ErrorCode;
import umc.snack.converter.memo.MemoConverter;
import umc.snack.domain.memo.dto.MemoResponseDto;
import umc.snack.domain.memo.entity.Memo;
import umc.snack.domain.user.entity.User;
import umc.snack.repository.memo.MemoRepository;
import umc.snack.repository.user.UserRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemoQueryServiceImpl implements MemoQueryService {
    private final MemoRepository memoRepository;
    private final UserRepository userRepository;

    @Override
    public MemoResponseDto.MemoListDto getMemosByUser(Long userId, int page, int size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_2622));

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Memo> memoPage = memoRepository.findByUser(user, pageable);

        if (memoPage.isEmpty() && page > 0)
            throw new CustomException(ErrorCode.REQ_3104);

        return MemoConverter.toMemoListDto(memoPage);
    }
}
