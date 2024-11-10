package homeTry.admin.service;

import homeTry.admin.dto.request.AdminCodeRequest;
import homeTry.common.auth.jwt.JwtAuth;
import homeTry.member.dto.MemberDTO;
import homeTry.member.service.MemberService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

@Service
@PropertySource("classpath:application-secret.properties")
public class AdminPageService {

    private final MemberService memberService;
    private final JwtAuth jwtAuth;

    public AdminPageService(MemberService memberService, JwtAuth jwtAuth) {
        this.memberService = memberService;
        this.jwtAuth = jwtAuth;
    }

    private static final int COOKIE_EXPIRATION_TIME = 60 * 60 * 24;

    @Value("${admin-code}")
    private String adminPromoteCode;

    public String promoteAdmin(String inputAdminCode, Long memberId, HttpServletResponse response) {

        if (isValidAdminCode(inputAdminCode)) {

            String adminToken = getAdminToken(memberId);
            setAdminTokenCookie(response, adminToken);

            return "redirect:/admin";
        }

        return "index";
    }

    private boolean isValidAdminCode(String inputAdminCode) {
        return adminPromoteCode.equals(inputAdminCode);
    }

    private String getAdminToken(Long memberId) {
        memberService.promoteToAdmin(memberId);
        return jwtAuth.generateToken(memberService.getMember(memberId));
    }

    private void setAdminTokenCookie(HttpServletResponse response, String adminToken) {
        Cookie adminTokenCookie = new Cookie("adminToken", adminToken);
        adminTokenCookie.setHttpOnly(true);  // 자바스크립트에서 접근할 수 없도록 설정
        adminTokenCookie.setPath("/admin");  // 쿠키의 유효 범위를 /admin 도메인으로 설정
        adminTokenCookie.setMaxAge(COOKIE_EXPIRATION_TIME);  // 쿠키의 유효 기간을 하루로 설정 (초 단위)
        response.addCookie(adminTokenCookie);  // 쿠키를 응답에 추가
    }
}