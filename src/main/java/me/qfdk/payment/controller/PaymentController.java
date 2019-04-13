package me.qfdk.payment.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import me.qfdk.payment.common.Constant;
import me.qfdk.payment.config.pay.AliPayConfig;
import me.qfdk.payment.entity.Payment;
import me.qfdk.payment.entity.Product;
import me.qfdk.payment.repository.PaymentRepository;
import me.qfdk.payment.repository.ProductRepository;
import me.qfdk.payment.service.WebSocketServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.Map;

@Controller
@RequestMapping("/payments")
public class PaymentController {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    AliPayConfig aliPayConfig;

    @Value("${domain}")
    private String DOMAIN;

    @RequestMapping(value = "/add", method = RequestMethod.POST)
    public String add(@RequestParam Map<String, Object> map, Model model) throws AlipayApiException {

        String page = "index";
        String nickName = map.get("nickName").toString();
        String info = map.get("info").toString();
        String payType = map.get("payType").toString();
        // 根据id 得到product 信息 防止篡改数据

        String productId = map.get("productId").toString();
        Product product = productRepository.findById(Integer.parseInt(productId)).get();

        Payment tmp = paymentRepository.findByNickNameAndStatusAndProductName(nickName, Constant.PAYMENT_STATUS_WAITING, product.getName());

        model.addAttribute("url", DOMAIN);
        model.addAttribute("nickName", nickName);
        model.addAttribute("product", product);

        System.out.println(tmp);
        if (tmp == null) {
            Payment payment = new Payment();
            if (payType.equals(Constant.PAYTYPE_ALIPAY)) {
                System.out.println("支付宝");
                AlipayTradePrecreateResponse response = aliPayConfig.getReponse(product.getPrice().toString(), product.getName());
                if (response.getMsg().equals("Success")) {
                    String qrcode = response.getQrCode();
                    payment.setQrCode(response.getQrCode());
                    payment.setNumeroTransaction(response.getOutTradeNo());
                    model.addAttribute("qrCode", qrcode);
                    model.addAttribute("out_trade_no", response.getOutTradeNo());

                    page = "pay/alipay";
                }
            }
            if (payType.equals(Constant.PAYTYPE_PAYPAL)) {
                System.out.println("paypal");
                // todo
            }
            payment.setNickName(nickName);
            payment.setProductName(product.getName());
            payment.setInfo(info);
            payment.setStatus(Constant.PAYMENT_STATUS_WAITING);
            payment.setMoney(product.getPrice());
            payment.setPayType(payType);
            payment.setCreateDate(new Date());
            paymentRepository.save(payment);
        } else {
            model.addAttribute("qrCode", tmp.getQrCode());
            model.addAttribute("out_trade_no", tmp.getNumeroTransaction());
            page = "pay/" + tmp.getPayType();
        }


        return page;
    }


    @RequestMapping(value = "/notify", method = RequestMethod.POST)
    @ResponseBody
    public void notify(@RequestParam Map<String, Object> params) throws ParseException, IOException {

        // 支付宝
        if (params.get("trade_status").equals(Constant.PAYMENT_STATUS_SUCCESS)) {
            Payment p = paymentRepository.findPaymentByNumeroTransactionAndStatus(params.get("out_trade_no").toString(), Constant.PAYMENT_STATUS_WAITING);
            p.setStatus(Constant.PAYMENT_STATUS_SUCCESS);

            // todo 成功的逻辑
            System.out.println("支付成功 发送邮件 ...");
            paymentRepository.save(p);

            WebSocketServer.sendInfo("{\n" +
                    "\"nikeName\":\"" + p.getNickName() + "\",\n" +
                    "\"status\":\"TRADE_SUCCESS\"\n" +
                    "}", p.getNickName());
        }

    }
}
