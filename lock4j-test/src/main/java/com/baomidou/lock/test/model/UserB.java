package com.baomidou.lock.test.model;

import lombok.Data;
import org.springframework.stereotype.Component;

@Component("userB")
@Data
public class UserB {
    private Long id= Long.valueOf(1);

    private String name="苞米豆";

    private String address="苞米豆";

}
