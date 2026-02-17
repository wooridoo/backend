package com.woorido.auth.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.woorido.auth.dto.request.PasswordResetExecuteRequest;
import com.woorido.auth.dto.response.PasswordResetExecuteResponse;
import com.woorido.auth.dto.response.PasswordResetResponse;
import com.woorido.common.entity.User;
import com.woorido.common.mapper.UserMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {
  // Learning note:
  // - Read flow as: validate auth/role -> execute domain logic -> persist via Mapper.

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    // ??쑬?甕곕뜇????苑??筌띻낱寃??醫륁뒞 ??볦퍢 (30??= 1800??
    private static final int RESET_TOKEN_EXPIRES_IN = 1800;

    /**
     * ??쑬?甕곕뜇????苑???遺욧퍕 筌ｌ꼶??
     * - ??李??곗쨮 ?????鈺곕똻???類ㅼ뵥
     * - ??苑???醫뤾쿃 ??밴쉐 獄?????
     * - ??쇱젫 ??곸겫??띻펾?癒?퐣????李??獄쏆뮇??嚥≪뮇彛??袁⑹뒄
     */
    // [학습] 비밀번호 재설정 토큰을 발급한다.
    public PasswordResetResponse requestPasswordReset(String email) {
        // 1. ??李??곗쨮 ?????鈺곌퀬??
        User user = userMapper.findByEmail(email);

        if (user == null) {
            throw new RuntimeException("USER_001:사용자를 찾을 수 없습니다");
        }

        log.info("Password reset requested - userId: {}, email: {}", user.getId(), email);

        // 2. ??苑???醫뤾쿃 ??밴쉐
        String resetToken = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(RESET_TOKEN_EXPIRES_IN);

        // 3. ?醫뤾쿃??DB??????
        userMapper.updatePasswordResetToken(user.getId(), resetToken, expiresAt);

        // 4. TODO: ??쇱젫 ??곸겫??띻펾?癒?퐣????李??獄쏆뮇??
        // emailService.sendPasswordResetEmail(email, resetToken);
        log.info("Password reset token issued - userId: {}, expiresAt: {}", user.getId(), expiresAt);

        // 5. ?臾먮뼗 ??밴쉐
        return PasswordResetResponse.of(email, RESET_TOKEN_EXPIRES_IN);
    }

    /**
     * ??쑬?甕곕뜇????苑????쎈뻬
     * - ?醫뤾쿃 ?醫륁뒞??野꺜筌?
     * - ????쑬?甕곕뜇???酉???獄???낅쑓??꾨뱜
     * - ?醫뤾쿃 筌띾슢利?筌ｌ꼶??
     */
    @Transactional
    // [학습] 재설정 토큰 검증 후 비밀번호를 변경한다.
    public PasswordResetExecuteResponse resetPassword(PasswordResetExecuteRequest request) {
        // 1. ?醫뤾쿃??곗쨮 ?????鈺곌퀬??獄??醫륁뒞??野꺜筌?
        User user = userMapper.findByPasswordResetToken(request.getToken());

        if (user == null) {
            throw new RuntimeException("AUTH_009:Invalid reset token");
        }

        if (user.getPasswordResetExpires().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("AUTH_009:Reset token has expired");
        }

        // 2. ??쑬?甕곕뜇???類ㅼ뵥
        if (!request.getNewPassword().equals(request.getNewPasswordConfirm())) {
            throw new RuntimeException("VALIDATION_001:비밀번호가 일치하지 않습니다");
        }

        // 3. ??쑬?甕곕뜇???酉???
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());

        // 4. ??쑬?甕곕뜇????낅쑓??꾨뱜 獄??醫뤾쿃 ?λ뜃由??
        userMapper.updatePassword(user.getId(), encodedPassword);
        userMapper.clearPasswordResetToken(user.getId());

        log.info("Password reset completed - userId: {}", user.getId());

        return PasswordResetExecuteResponse.success();
    }
}
