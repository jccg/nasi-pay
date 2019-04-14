package me.qfdk.payment.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.paypal.api.payments.Links;
import com.paypal.api.payments.Payment;
import com.paypal.api.payments.PaymentExecution;
import com.paypal.base.rest.PayPalRESTException;
import me.qfdk.payment.common.Constant;
import me.qfdk.payment.common.EmailUtils;
import me.qfdk.payment.common.Tools;
import me.qfdk.payment.config.pay.AliPayConfig;
import me.qfdk.payment.config.pay.PayPalConfig;
import me.qfdk.payment.entity.MyPayment;
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

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/payments")
public class PaymentController {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private EmailUtils emailUtils;

    @Autowired
    AliPayConfig aliPayConfig;

    @Autowired
    PayPalConfig payPalConfig;

    @Value("${domain}")
    private String DOMAIN;

    @RequestMapping(value = "/add", method = RequestMethod.POST)
    public String add(@RequestParam Map<String, Object> map, Model model) throws AlipayApiException, PayPalRESTException {

        String page = "index";
        String nickName = map.get("nickName").toString();
        String info = map.get("info").toString();
        String payType = map.get("payType").toString();
        String email = map.get("email").toString();

        String transactionNumber = Tools.generateTransactionNumber();
        String productId = map.get("productId").toString();
        // 根据id 得到product 信息 防止篡改数据
        Product product = productRepository.findById(Integer.parseInt(productId)).get();

        List<MyPayment> tmp = paymentRepository.findMyPaymentByNickNameAndProductIdAndStatusAndPayType(nickName, product.getId(), Constant.PAYMENT_STATUS_WAITING, payType);

        model.addAttribute("url", DOMAIN);
        model.addAttribute("product", product);

        if (tmp.isEmpty()) {
            MyPayment myPayment = new MyPayment();
            // 支付宝逻辑
            if (payType.equals(Constant.PAYTYPE_ALIPAY)) {
                System.out.println("支付宝");
                myPayment.setMoney(product.getPriceAliPay());
                AlipayTradePrecreateResponse response = aliPayConfig.getReponse(transactionNumber, product);
                if (response.getMsg().equals("Success")) {
                    myPayment.setQrCode(response.getQrCode());
                    page = "pay/alipay";
                }
            }
            // PayPal 逻辑
            if (payType.equals(Constant.PAYTYPE_PAYPAL)) {
                System.out.println("paypal");
                myPayment.setMoney(product.getPricePaypal());

                Payment payment = payPalConfig.getPayment(transactionNumber, product);

                Payment createdPayment = payment.create(payPalConfig.getApiContext());

                Iterator links = createdPayment.getLinks().iterator();
                while (links.hasNext()) {
                    Links link = (Links) links.next();
                    if (link.getRel().equalsIgnoreCase("approval_url")) {
                        // Redirect the customer to link.getHref()
                        myPayment.setQrCode(link.getHref());
                        page = "redirect:" + link.getHref();
                    }
                }
            }
            // 建立 mypayment 区分paypal
            myPayment.setNickName(nickName);
            myPayment.setProductId(product.getId());
            myPayment.setNumeroTransaction(transactionNumber);
            myPayment.setInfo(info);
            myPayment.setStatus(Constant.PAYMENT_STATUS_WAITING);
            myPayment.setPayType(payType);
            myPayment.setCreateDate(new Date());
            myPayment.setEmail(email);
            // 插入数据库
            paymentRepository.save(myPayment);
            model.addAttribute("payment", myPayment);

        } else {
            if (payType.equals(Constant.PAYTYPE_ALIPAY)) {
                System.out.println("支付宝");
                model.addAttribute("qrCode", tmp.get(tmp.size() - 1).getQrCode());
                page = "pay/alipay";
            }
            if (payType.equals(Constant.PAYTYPE_PAYPAL)) {
                System.out.println("paypal");
                page = "redirect:" + tmp.get(tmp.size() - 1).getQrCode();
            }

            model.addAttribute("payment", tmp.get(tmp.size() - 1));
        }

        return page;
    }


    @RequestMapping(value = "/notify", method = RequestMethod.POST)
    @ResponseBody
    // 支付宝回调
    public void notify(@RequestParam Map<String, Object> params) throws IOException {
        if (params.get("trade_status").equals(Constant.PAYMENT_STATUS_SUCCESS)) {
            MyPayment p = paymentRepository.findMyPaymentByNumeroTransactionAndStatus(params.get("out_trade_no").toString(), Constant.PAYMENT_STATUS_WAITING);
            p.setStatus(Constant.PAYMENT_STATUS_SUCCESS);
            paymentRepository.save(p);

            doSuccess(p, productRepository.findById(p.getProductId()).get());

            WebSocketServer.sendInfo("{\n" +
                    "\"nikeName\":\"" + p.getNickName() + "\",\n" +
                    "\"status\":\"TRADE_SUCCESS\"\n" +
                    "}", p.getNickName());
        }

    }

    @RequestMapping(value = "/process")
    // PayPal回调
    public String process(HttpServletRequest req, Model model) {
        Payment payment = new Payment();
        payment.setId(req.getParameter("paymentId"));

        PaymentExecution paymentExecution = new PaymentExecution();
        paymentExecution.setPayerId(req.getParameter("PayerID"));
        String out_trade_no = req.getParameter("out_trade_no");
        MyPayment myPayment = paymentRepository.findMyPaymentByNumeroTransactionAndStatus(out_trade_no, Constant.PAYMENT_STATUS_WAITING);

        // 未支付
        if (myPayment != null) {
            try {
                Payment createdPayment = payment.execute(payPalConfig.getApiContext(), paymentExecution);
                if (createdPayment.getState().equals("approved")) {
                    Product product = productRepository.findById(myPayment.getProductId()).get();
                    myPayment.setStatus(Constant.PAYMENT_STATUS_SUCCESS);
                    paymentRepository.save(myPayment);
                    doSuccess(myPayment, product);
                    model.addAttribute("out_trade_no", out_trade_no);
                    model.addAttribute("product", product);
                    model.addAttribute("nickName", myPayment.getNickName());
                    return "pay/paypal";
                }
            } catch (PayPalRESTException e) {
                if (e.getDetails().getName().equals("PAYMENT_ALREADY_DONE")) {
                    model.addAttribute("out_trade_no", myPayment.getNumeroTransaction());
                    model.addAttribute("product", productRepository.findById(myPayment.getProductId()).get());
                    model.addAttribute("nickName", myPayment.getNickName());
                    return "pay/paypal";
                }
            }
        } else {
            MyPayment tmp = paymentRepository.findMyPaymentByNumeroTransactionAndStatus(out_trade_no, Constant.PAYMENT_STATUS_SUCCESS);
            model.addAttribute("out_trade_no", out_trade_no);
            model.addAttribute("product", productRepository.findById(tmp.getProductId()).get());
            model.addAttribute("nickName", tmp.getNickName());
            return "pay/paypal";
        }

        return "redirect:/";
    }

    @RequestMapping(value = "/query")
    public String queryTransaction(HttpServletRequest req, Model model) {

        String numero = req.getParameter("numero");
        MyPayment payment = paymentRepository.findMyPaymentByNumeroTransaction(numero);
        Product product = productRepository.findById(payment.getProductId()).get();

        model.addAttribute("payment", payment);
        model.addAttribute("product", product);

        return "pay/query";
    }

    /**
     * 支付成功逻辑
     *
     * @param myPayment
     * @param product
     */
    public void doSuccess(MyPayment myPayment, Product product) {
        emailUtils.sendTemplateMail("i@tar.tn", myPayment.getEmail(), "【NASI-PAY】您订购的 ->" + product.getName() + "<- 到账了！", "mail", myPayment, product);
    }

}
