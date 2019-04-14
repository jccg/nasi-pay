package me.qfdk.payment.config.pay;

import com.paypal.api.payments.*;
import com.paypal.base.rest.APIContext;
import lombok.Data;
import me.qfdk.payment.entity.Product;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Data
@Configuration
public class PayPalConfig {

    @Value("${paypal_clientId}")
    private String clientId;

    @Value("${paypal_clientSecret}")
    private String clientSecret;

    @Value("${paypal_mode}")
    private String mode;

    @Value("${paypal_returnUrl}")
    private String returnUrl;

    @Value("${payapl_cancelUrl}")
    private String cancelUrl;

    @Value("${paypal_currency}")
    private String currency;

    private APIContext apiContext;

    @PostConstruct
    public void init() {
        this.apiContext = new APIContext(clientId, clientSecret, mode);
    }


    public Payment getPayment(String transactionNumber, Product product) {
        // Set payer details
        Payer payer = new Payer();
        payer.setPaymentMethod("paypal");

        // Set redirect URLs
        RedirectUrls redirectUrls = new RedirectUrls();
        redirectUrls.setCancelUrl(cancelUrl);
        redirectUrls.setReturnUrl(returnUrl + "?out_trade_no=" + transactionNumber);


        // MyPayment amount
        Amount amount = new Amount();
        amount.setCurrency(currency);
        amount.setTotal(product.getPricePaypal().toString());

        ItemList itemList = new ItemList();

        List<Item> list = new ArrayList<>();

        Item item = new Item();
        item.setCurrency(currency);
        item.setName(product.getName());
        item.setPrice(product.getPricePaypal().toString());
        item.setQuantity("1");
        list.add(item);

        itemList.setItems(list);
        // Transaction information
        Transaction transaction = new Transaction();
        transaction.setAmount(amount);
        transaction.setDescription("您购买的服务如下:");
        transaction.setInvoiceNumber(transactionNumber);
        transaction.setItemList(itemList);

        // Add transaction to a list
        List<Transaction> transactions = new ArrayList<Transaction>();
        transactions.add(transaction);

        // Add Payment details
        Payment payment = new Payment();
        payment.setIntent("sale");
        payment.setPayer(payer);
        payment.setRedirectUrls(redirectUrls);
        payment.setTransactions(transactions);
        return payment;
    }

}
