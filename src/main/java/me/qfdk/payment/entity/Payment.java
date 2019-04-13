package me.qfdk.payment.entity;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.math.BigDecimal;
import java.util.Date;

@Entity(name = "payment")
@Data
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String numeroTransaction;

    private String status;

    private Date createDate;

    private String nickName;

    private BigDecimal money;

    private String info;

    private String payType;

    private String qrCode;

    private String productName;

}
