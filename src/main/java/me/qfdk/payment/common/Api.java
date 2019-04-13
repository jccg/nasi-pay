package me.qfdk.payment.common;

import com.google.zxing.EncodeHintType;
import net.glxn.qrgen.QRCode;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController

public class Api {
    @RequestMapping(value = "/api/qrCode", method = RequestMethod.GET)
    public HttpEntity<byte[]> qr(@RequestParam("str") String qrCode) {
        byte[] bytes = QRCode.from(qrCode).withSize(250, 250).withHint(EncodeHintType.MARGIN,1).stream().toByteArray();
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.setContentLength(bytes.length);
        return new ResponseEntity<byte[]>(bytes, headers, HttpStatus.OK);
    }
}
