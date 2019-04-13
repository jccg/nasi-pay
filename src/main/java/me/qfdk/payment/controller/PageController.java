package me.qfdk.payment.controller;

import me.qfdk.payment.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class PageController {

    @Autowired
    private ProductRepository productRepository;


    @RequestMapping("/")
    public String showPage(Model model) {
        model.addAttribute("products", productRepository.findAll());
        return "index";
    }

}
