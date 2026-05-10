package org.sxk.store.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDelayMessageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long orderId;

    private String orderNo;
}