package br.edu.cesmac.frota_ops.home;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import br.edu.cesmac.frota_ops.home.dashboard.DashboardService;
import jakarta.servlet.http.HttpSession;

@Controller
public class HomeController {

    private final DashboardService dashboardService;

    public HomeController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/")
    public String index(HttpSession session, Model model) {
        model.addAttribute("dashboard", dashboardService.montar(session));
        return "home/index";
    }
}
