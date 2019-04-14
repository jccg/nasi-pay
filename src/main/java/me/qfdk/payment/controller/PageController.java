package me.qfdk.payment.controller;

import me.qfdk.payment.entity.MyPayment;
import me.qfdk.payment.entity.Product;
import me.qfdk.payment.repository.PaymentRepository;
import me.qfdk.payment.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;

@Controller
public class PageController {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Value("${adminToken}")
    private String myToken;

    @RequestMapping("/")
    public String showPage(Model model) {
        model.addAttribute("products", productRepository.findAll());
        return "index";
    }

    @RequestMapping("/admin")
    public String adminPage(HttpServletRequest req, Model model) {

        String token = req.getParameter("token");

        if (myToken.equals(token)) {

            String action = req.getParameter("action");
            String id = req.getParameter("id");

            if ("delete".equals(action)) {
                List<MyPayment> list = paymentRepository.findByProductId(Integer.parseInt(id));
                if (list.isEmpty()) {
                    if (productRepository.findById(Integer.parseInt(id)).isPresent()) {
                        productRepository.deleteById(Integer.parseInt(id));
                    }
                } else {
                    model.addAttribute("products", productRepository.findAll());
                    model.addAttribute("error", "支付记录已存在该产品，请手动删除该记录");
                    return "admin";
                }
            }

            if ("add".equals(action)) {
                Product p = new Product();
                p.setName(req.getParameter("productName"));
                p.setDescription(req.getParameter("description"));
                p.setPriceAliPay(new BigDecimal(req.getParameter("priceAliPay")));
                p.setPricePaypal(new BigDecimal(req.getParameter("pricePayPal")));
                productRepository.save(p);
            }
            model.addAttribute("products", productRepository.findAll());
            return "admin";
        }

        return "index";
    }

}
