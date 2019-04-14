package me.qfdk.payment.repository;

import me.qfdk.payment.entity.MyPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<MyPayment, Integer> {

    MyPayment findMyPaymentByNumeroTransactionAndStatus(String numero, String status);

    MyPayment findMyPaymentByNumeroTransactionAndStatusAndPayType(String numero, String status, String payType);

    MyPayment findMyPaymentByNumeroTransaction(String numero);

    List<MyPayment> findMyPaymentByNickNameAndProductIdAndStatusAndPayType(String nickName, Integer productId, String status, String payType);

}
