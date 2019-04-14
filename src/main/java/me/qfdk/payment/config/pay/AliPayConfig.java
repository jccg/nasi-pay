package me.qfdk.payment.config.pay;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import me.qfdk.payment.entity.Product;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AliPayConfig {

    @Value("${appid}")
    private String APPID;

    @Value("${pid}")
    private String PID;

    @Value("${private_key}")
    private String RSA2_PRIVATE;

    @Value("${public_key}")
    private String RSA2_PUBLIC;

    @Value("${alipay_public_key}")
    private String ALIPAY_PUBLIC_KEY;

    @Value("${open_api_domain}")
    private String SERVERURL;

    @Value("${notify_url}")
    private String NOTIFY_URL;

    public AliPayConfig() {

    }

    public AlipayTradePrecreateResponse getReponse(String outTradeNo, Product product) throws AlipayApiException {
        AlipayClient alipayClient = new DefaultAlipayClient(SERVERURL, APPID, RSA2_PRIVATE, "json",
                "UTF-8", ALIPAY_PUBLIC_KEY, "RSA2"); //获得初始化的AlipayClient
        AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();//创建API对应的request类

        String body = "";

        request.setBizContent("{" +
                "    \"out_trade_no\":\"" + outTradeNo + "\"," +
                "    \"total_amount\":\"" + product.getPriceAliPay() + "\"," +
                "    \"body\":\"" + body + "\"," +
                "    \"subject\":\"" + product.getName() + "\"," +
                "    \"timeout_express\":\"90m\"}");//设置业务参数
        request.setNotifyUrl(NOTIFY_URL);
        AlipayTradePrecreateResponse response = alipayClient.execute(request);//通过alipayClient调用API，获得对应的response类
        return response;
    }
}
