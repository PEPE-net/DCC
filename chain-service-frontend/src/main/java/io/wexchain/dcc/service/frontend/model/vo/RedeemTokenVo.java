package io.wexchain.dcc.service.frontend.model.vo;

import io.wexchain.dcc.marketing.api.constant.RedeemTokenStatus;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigInteger;

/**
 * 发币订单
 *
 * @author zhengpeng
 */
public class RedeemTokenVo {

    /**
     * 订单ID
     */
    private Long id;

    /**
     * 场景code
     */
    private String scenarioCode;

    /**
     * 发币数量
     */
    private BigInteger amount;

    /**
     * 接收人钱包地址
     */
    private String receiverAddress;

    /**
     * 状态
     */
    private RedeemTokenStatus status;

    public RedeemTokenVo() {
    }

    public RedeemTokenVo(Long id, String scenarioCode, BigInteger amount, RedeemTokenStatus status) {
        this.id = id;
        this.scenarioCode = scenarioCode;
        this.amount = amount;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getScenarioCode() {
        return scenarioCode;
    }

    public void setScenarioCode(String scenarioCode) {
        this.scenarioCode = scenarioCode;
    }

    public BigInteger getAmount() {
        return amount;
    }

    public void setAmount(BigInteger amount) {
        this.amount = amount;
    }

    public String getReceiverAddress() {
        return receiverAddress;
    }

    public void setReceiverAddress(String receiverAddress) {
        this.receiverAddress = receiverAddress;
    }

    public RedeemTokenStatus getStatus() {
        return status;
    }

    public void setStatus(RedeemTokenStatus status) {
        this.status = status;
    }
}
