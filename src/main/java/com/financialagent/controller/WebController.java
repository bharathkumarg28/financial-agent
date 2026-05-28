package com.financialagent.controller;

import com.financialagent.config.OrderConfig;
import com.financialagent.dto.UserRegistrationDto;
import com.financialagent.service.AngelOneSessionService;
import com.financialagent.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Web controller for serving UI pages.
 */
@Controller
@RequiredArgsConstructor
public class WebController {

    private final AngelOneSessionService sessionService;
    private final OrderConfig orderConfig;
    private final AuthenticationService authenticationService;

    /**
     * Root path redirects to login page.
     */
    @GetMapping("/")
    public String index() {
        return "redirect:/login";
    }

    /**
     * Login page.
     */
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /**
     * Registration page.
     */
    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("userRegistrationDto", new UserRegistrationDto());
        return "register";
    }

    /**
     * Main dashboard page (after login).
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("connected", sessionService.isAuthenticated());
        model.addAttribute("dryRunMode", orderConfig.isDryRun());
        model.addAttribute("currentPage", "dashboard");
        return "index";
    }

    /**
     * Stock analysis page.
     */
    @GetMapping("/analysis")
    public String analysis(Model model) {
        model.addAttribute("connected", sessionService.isAuthenticated());
        model.addAttribute("currentPage", "analysis");
        return "analysis";
    }

    /**
     * Market data page.
     */
    @GetMapping("/market-data")
    public String marketData(Model model) {
        model.addAttribute("connected", sessionService.isAuthenticated());
        model.addAttribute("currentPage", "market-data");
        return "market-data";
    }

    /**
     * Order placement page.
     */
    @GetMapping("/orders")
    public String orders(Model model) {
        model.addAttribute("connected", sessionService.isAuthenticated());
        model.addAttribute("dryRunMode", orderConfig.isDryRun());
        model.addAttribute("currentPage", "orders");
        return "orders";
    }

    /**
     * Settings/Configuration page.
     */
    @GetMapping("/settings")
    public String settings(Model model) {
        model.addAttribute("connected", sessionService.isAuthenticated());
        model.addAttribute("dryRunMode", orderConfig.isDryRun());
        model.addAttribute("currentPage", "settings");
        return "settings";
    }

    /**
     * Watchlist page.
     */
    @GetMapping("/watchlist")
    public String watchlist(Model model) {
        model.addAttribute("connected", sessionService.isAuthenticated());
        model.addAttribute("currentPage", "watchlist");
        return "watchlist";
    }

    /**
     * Logout endpoint with AngelOne disconnection.
     */
    @PostMapping("/logout")
    public String logout(Authentication authentication, RedirectAttributes redirectAttributes) {
        String username = authentication != null ? authentication.getName() : "unknown";

        try {
            // Invalidate AngelOne session for the user
            authenticationService.invalidateAngelOneSession(username);

            redirectAttributes.addFlashAttribute("message",
                    "Logged out successfully. AngelOne session disconnected for user: " + username);

        } catch (Exception e) {
            // Log error but don't fail logout
            System.err.println("Error disconnecting AngelOne session: " + e.getMessage());
            redirectAttributes.addFlashAttribute("message", "Logged out successfully");
        }

        return "redirect:/login?logout=true";
    }
}
