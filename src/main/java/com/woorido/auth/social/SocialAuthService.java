package com.woorido.auth.social;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.woorido.account.domain.Account;
import com.woorido.account.repository.AccountMapper;
import com.woorido.auth.dto.request.SocialAuthCompleteRequest;
import com.woorido.auth.dto.request.SocialAuthStartRequest;
import com.woorido.auth.dto.response.LoginResponse;
import com.woorido.auth.dto.response.SocialProviderStatusResponse;
import com.woorido.auth.dto.response.SocialAuthStartResponse;
import com.woorido.auth.dto.response.UserInfo;
import com.woorido.common.entity.AccountStatus;
import com.woorido.common.entity.SocialProvider;
import com.woorido.common.entity.User;
import com.woorido.common.mapper.UserMapper;
import com.woorido.common.util.JwtUtil;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SocialAuthService {
    private final UserMapper userMapper;
    private final AccountMapper accountMapper;
    private final JwtUtil jwtUtil;
    private final SocialStateService socialStateService;
    private final List<SocialOAuthClient> oauthClients;

    @PostConstruct
    void logOAuthConfiguration() {
        for (SocialOAuthClient client : oauthClients) {
            String redirectUri = client.defaultRedirectUri();
            if (client.isConfigured()) {
                log.info("OAuth client configured: provider={}, redirectUri={}", client.provider(), redirectUri);
                continue;
            }

            log.warn("OAuth client is not fully configured: provider={}, redirectUri={}", client.provider(), redirectUri);
        }
    }

    public SocialAuthStartResponse start(SocialAuthStartRequest request) {
        SocialProvider provider = parseProvider(request.getProvider());
        SocialAuthIntent intent = SocialAuthIntent.from(request.getIntent());
        SocialOAuthClient client = getClient(provider);

        if (!client.isConfigured()) {
            throw new RuntimeException("AUTH_011:소셜 로그인 설정이 완료되지 않았습니다");
        }

        String safeReturnTo = sanitizeReturnTo(request.getReturnTo());
        String state = socialStateService.issue(provider, intent, safeReturnTo);
        String authorizeUrl = client.buildAuthorizeUrl(state);

        return SocialAuthStartResponse.builder()
                .authorizeUrl(authorizeUrl)
                .build();
    }

    public SocialProviderStatusResponse getProviderStatuses() {
        return SocialProviderStatusResponse.builder()
                .providers(List.of(
                        buildProviderStatus(SocialProvider.GOOGLE),
                        buildProviderStatus(SocialProvider.KAKAO)))
                .build();
    }

    public LoginResponse complete(SocialAuthCompleteRequest request) {
        SocialProvider provider = parseProvider(request.getProvider());
        SocialOAuthClient client = getClient(provider);

        if (!client.isConfigured()) {
            throw new RuntimeException("AUTH_011:소셜 로그인 설정이 완료되지 않았습니다");
        }

        SocialStatePayload statePayload = socialStateService.verify(request.getState());
        if (statePayload.getProvider() != provider) {
            throw new RuntimeException("AUTH_012:유효하지 않은 소셜 인증 요청입니다");
        }

        SocialUserProfile profile = client.fetchUserProfile(request.getCode());
        if (!StringUtils.hasText(profile.getSocialId())) {
            throw new RuntimeException("AUTH_014:소셜 인증 처리에 실패했습니다");
        }
        if (!StringUtils.hasText(profile.getEmail())) {
            throw new RuntimeException("AUTH_015:소셜 계정 이메일 정보를 확인할 수 없습니다");
        }

        User user = userMapper.findBySocial(provider.name(), profile.getSocialId());
        if (user == null) {
            user = userMapper.findByEmail(profile.getEmail());
            if (user != null) {
                if (user.getSocialProvider() != null
                        && user.getSocialProvider() != provider
                        && user.getSocialId() != null
                        && !user.getSocialId().equals(profile.getSocialId())) {
                    throw new RuntimeException("AUTH_016:기존 계정과 소셜 계정 연결에 실패했습니다");
                }
                userMapper.linkSocialAccount(user.getId(), provider.name(), profile.getSocialId());
                user = userMapper.findById(user.getId());
            } else {
                user = createSocialUser(provider, profile);
            }
        }

        if (user == null) {
            throw new RuntimeException("AUTH_014:소셜 인증 처리에 실패했습니다");
        }
        if (user.getAccountStatus() == AccountStatus.WITHDRAWN) {
            throw new RuntimeException("USER_005:탈퇴 대기 상태입니다");
        }

        userMapper.resetFailedLoginAttempts(user.getId());
        userMapper.updateLastLoginAt(user.getId());

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());
        boolean isNewUser = user.getCreatedAt() != null
                && user.getCreatedAt().isAfter(LocalDateTime.now().minusDays(7));
        boolean requiresOnboarding = isOnboardingRequired(user);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn((int) jwtUtil.getAccessTokenExpiration())
                .returnTo(sanitizeReturnTo(statePayload.getReturnTo()))
                .user(UserInfo.builder()
                        .userId(user.getId())
                        .email(user.getEmail())
                        .nickname(user.getNickname())
                        .profileImage(user.getProfileImageUrl())
                        .status(user.getAccountStatus().name())
                        .hasPassword(user.getPasswordHash() != null && !user.getPasswordHash().isBlank())
                        .isNewUser(isNewUser)
                        .requiresOnboarding(requiresOnboarding)
                        .build())
                .build();
    }

    private User createSocialUser(SocialProvider provider, SocialUserProfile profile) {
        LocalDateTime now = LocalDateTime.now();
        String userId = UUID.randomUUID().toString();
        String nickname = generateUniqueNickname(profile.getNickname(), profile.getEmail());
        String displayName = StringUtils.hasText(profile.getName()) ? profile.getName() : nickname;

        User newUser = User.builder()
                .id(userId)
                .email(profile.getEmail())
                .passwordHash(null)
                .name(displayName)
                .nickname(nickname)
                .profileImageUrl(profile.getProfileImageUrl())
                .socialProvider(provider)
                .socialId(profile.getSocialId())
                .accountStatus(AccountStatus.ACTIVE)
                .agreedTerms("N")
                .agreedPrivacy("N")
                .agreedMarketing("N")
                .createdAt(now)
                .build();

        userMapper.insertUser(newUser);

        Account account = Account.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .balance(0L)
                .lockedBalance(0L)
                .accountNumber(generateAccountNumber())
                .version(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
        accountMapper.save(account);

        userMapper.insertInitialUserScore(UUID.randomUUID().toString(), userId, 12.0);
        return userMapper.findById(userId);
    }

    private String generateUniqueNickname(String preferredNickname, String email) {
        String emailPrefix = null;
        if (StringUtils.hasText(email) && email.contains("@")) {
            emailPrefix = email.substring(0, email.indexOf('@'));
        }

        String base = StringUtils.hasText(preferredNickname) ? preferredNickname : emailPrefix;
        if (!StringUtils.hasText(base)) {
            base = "우리두회원";
        }
        base = base.trim();
        if (base.length() > 20) {
            base = base.substring(0, 20);
        }

        if (userMapper.countByNickname(base) == 0) {
            return base;
        }

        String compactBase = base.length() > 15 ? base.substring(0, 15) : base;
        for (int i = 0; i < 20; i++) {
            int suffix = (int) (Math.random() * 9000) + 1000;
            String candidate = compactBase + suffix;
            if (userMapper.countByNickname(candidate) == 0) {
                return candidate;
            }
        }
        return compactBase + UUID.randomUUID().toString().replace("-", "").substring(0, 4);
    }

    private String generateAccountNumber() {
        return String.valueOf((long) (Math.random() * 900000000000L) + 100000000000L);
    }

    private SocialProvider parseProvider(String rawProvider) {
        if (!StringUtils.hasText(rawProvider)) {
            throw new RuntimeException("AUTH_013:지원하지 않는 소셜 제공자입니다");
        }
        try {
            SocialProvider provider = SocialProvider.valueOf(rawProvider.trim().toUpperCase());
            if (provider != SocialProvider.GOOGLE && provider != SocialProvider.KAKAO) {
                throw new RuntimeException("AUTH_013:지원하지 않는 소셜 제공자입니다");
            }
            return provider;
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("AUTH_013:지원하지 않는 소셜 제공자입니다");
        }
    }

    private SocialOAuthClient getClient(SocialProvider provider) {
        Map<SocialProvider, SocialOAuthClient> clientMap = new EnumMap<>(SocialProvider.class);
        for (SocialOAuthClient client : oauthClients) {
            clientMap.put(client.provider(), client);
        }
        SocialOAuthClient client = clientMap.get(provider);
        if (client == null) {
            throw new RuntimeException("AUTH_013:지원하지 않는 소셜 제공자입니다");
        }
        return client;
    }

    private SocialProviderStatusResponse.ProviderStatus buildProviderStatus(SocialProvider provider) {
        SocialOAuthClient client = findClient(provider);
        if (client == null) {
            return SocialProviderStatusResponse.ProviderStatus.builder()
                    .provider(provider.name())
                    .enabled(false)
                    .reasonCode("AUTH_013")
                    .build();
        }

        boolean enabled = client.isConfigured();
        return SocialProviderStatusResponse.ProviderStatus.builder()
                .provider(provider.name())
                .enabled(enabled)
                .reasonCode(enabled ? null : "AUTH_011")
                .build();
    }

    private SocialOAuthClient findClient(SocialProvider provider) {
        for (SocialOAuthClient client : oauthClients) {
            if (client.provider() == provider) {
                return client;
            }
        }
        return null;
    }

    private boolean isOnboardingRequired(User user) {
        if (user.getSocialProvider() == null) {
            return false;
        }

        boolean agreedTerms = "Y".equalsIgnoreCase(user.getAgreedTerms());
        boolean agreedPrivacy = "Y".equalsIgnoreCase(user.getAgreedPrivacy());
        return !agreedTerms || !agreedPrivacy;
    }

    private String sanitizeReturnTo(String returnTo) {
        if (!StringUtils.hasText(returnTo)) {
            return "/";
        }
        String normalized = returnTo.trim();
        if (!normalized.startsWith("/") || normalized.startsWith("//")) {
            return "/";
        }
        return normalized;
    }
}
