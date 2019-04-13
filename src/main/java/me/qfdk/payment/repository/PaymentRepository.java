package me.qfdk.payment.repository;

import me.qfdk.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    Payment findPaymentByNumeroTransactionAndStatus(String numero, String status);

    Payment findByNickNameAndStatusAndProductName(String nickname, String status, String productName);
}
