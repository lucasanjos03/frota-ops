package br.edu.cesmac.frota_ops;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller 
public class HomeController {

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/veiculos")
    public String veiculos() {
        return "veiculos";
    }

    @GetMapping("/veiculos/novo")
    public String veiculoNovo() {
        return "veiculo-novo";
    }

    @PostMapping("/veiculos/novo")
    public String cadastrarVeiculo() {
        return "redirect:/veiculos";
    }

    @GetMapping("/veiculos/{id}")
    public String veiculoDetalhes(@PathVariable("id") Long id) {
        return "veiculo-detalhes";
    }

    @GetMapping("/veiculos/{id}/editar")
    public String veiculoEditar(@PathVariable("id") Long id) {
        return "veiculo-editar";
    }

    @PostMapping("/veiculos/{id}/editar")
    public String atualizarVeiculo(@PathVariable("id") Long id) {
        return "redirect:/veiculos/" + id;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @PostMapping("/login")
    public String postLogin() {
        return "redirect:/";
    }
}
