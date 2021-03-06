package io.wexchain.dcc.service.frontend.service.dcc.loan;

import com.wexmarket.topia.commons.basic.exception.ErrorCodeValidate;
import com.wexmarket.topia.commons.basic.rpc.utils.PaginationUtils;
import com.wexmarket.topia.commons.pagination.PageParam;
import com.wexmarket.topia.commons.pagination.Pagination;
import com.wexmarket.topia.commons.pagination.SortPageParam;
import io.wexchain.cryptoasset.loan.api.ApplyRequest;
import io.wexchain.cryptoasset.loan.api.ConfirmRepaymentRequest;
import io.wexchain.cryptoasset.loan.api.QueryLoanOrderPageRequest;
import io.wexchain.cryptoasset.loan.api.constant.LoanOrderStatus;
import io.wexchain.cryptoasset.loan.api.model.RepaymentBill;
import io.wexchain.dcc.loan.sdk.contract.LoanOrder;
import io.wexchain.dcc.service.frontend.common.constants.FrontendWebConstants;
import io.wexchain.dcc.service.frontend.common.constants.LoanExtParamConstants;
import io.wexchain.dcc.service.frontend.common.enums.FrontendErrorCode;
import io.wexchain.dcc.service.frontend.ctrlr.security.MemberDetails;
import io.wexchain.dcc.service.frontend.integration.back.CryptoAssetLoanOperationClient;
import io.wexchain.dcc.service.frontend.model.request.LoanCreditApplyRequest;
import io.wexchain.dcc.service.frontend.model.request.LoanInterestRequest;
import io.wexchain.dcc.service.frontend.model.vo.LoanOrderDetailVo;
import io.wexchain.dcc.service.frontend.model.vo.LoanOrderVo;
import io.wexchain.dcc.service.frontend.model.vo.LoanProductVo;
import io.wexchain.dcc.service.frontend.model.vo.RepaymentBillVo;
import io.wexchain.dcc.service.frontend.service.dcc.loanProduct.LoanProductService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;


/**
 * LoanCreditServiceImpl
 *
 */
@Service(value = "dccLoanService")
public class LoanServiceImpl implements LoanService,FrontendWebConstants,LoanExtParamConstants{

    private static final Logger logger = LoggerFactory.getLogger(LoanServiceImpl.class);

    @Autowired
    private io.wexchain.dcc.loan.sdk.service.LoanService loanService;

    @Autowired
    private LoanProductService loanProductService;

    @Value(value = "${app.identity}")
    private String appIdentity;

    @Autowired
    private CryptoAssetLoanOperationClient cryptoAssetLoanOperationClient;

    @Override
    public LoanOrder getLastOrder(MemberDetails memberDetails) {
        try {
            return loanService.getLastOrder(memberDetails.getUsername());
        } catch (IOException e) {
            logger.error("查询最后借款订单失败：", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void apply(LoanCreditApplyRequest loanCreditApplyRequest,MemberDetails memberDetails) {
        LoanProductVo loanProductVo = loanProductService.getLoanProductVo(loanCreditApplyRequest.getLoanProductId());

        ApplyRequest applyRequest = new ApplyRequest();
        applyRequest.setChainOrderId(loanCreditApplyRequest.getOrderId());

        applyRequest.setMemberId(memberDetails.getId());
        applyRequest.setLoanProductId(loanCreditApplyRequest.getLoanProductId().toString());
        applyRequest.setAmount(loanCreditApplyRequest.getBorrowAmount());
        applyRequest.setBorrowDuration(loanCreditApplyRequest.getBorrowDuration());
        applyRequest.setBorrowName(loanCreditApplyRequest.getBorrowName());
        applyRequest.setDurationUnit(loanCreditApplyRequest.getDurationUnit());
        applyRequest.setCertNo(loanCreditApplyRequest.getCertNo());
        applyRequest.setMobile(loanCreditApplyRequest.getMobile());
        applyRequest.setBankCardNo(loanCreditApplyRequest.getBankCard());
        applyRequest.setBankMobile(loanCreditApplyRequest.getBankMobile());
        applyRequest.setApplyDate(loanCreditApplyRequest.getApplyDate());
        applyRequest.setLoanType(loanProductVo.getLoanType());
        applyRequest.setExpectAnnualRate(loanProductVo.getLoanRate().toString());
        applyRequest.setAssetCode(loanProductVo.getCurrency().getSymbol());
        applyRequest.setRepayMode("TOGETHER");
        applyRequest.setAppIdentity(appIdentity);

        Map<String, String> stringStringHashMap = new HashMap<>();
        applyRequest.setApplicationDigestClearingText(stringStringHashMap);

        io.wexchain.cryptoasset.loan.api.model.LoanOrder apply = cryptoAssetLoanOperationClient.apply(applyRequest);
        ErrorCodeValidate.isTrue(StringUtils.isBlank(apply.getFailCode()), FrontendErrorCode.LOAN_APPLY_FAIL, apply.getFailMessage());

    }

    @Override
    public Pagination<LoanOrderVo> queryLoanOrderPage(PageParam pageParam, Long memberId) {
        QueryLoanOrderPageRequest queryLoanOrderPageRequest = new QueryLoanOrderPageRequest();
        queryLoanOrderPageRequest.setMemberId(memberId.toString());
        queryLoanOrderPageRequest.setSortPageParam(new SortPageParam(pageParam.getNumber(),pageParam.getSize()));
        List<LoanOrderStatus> excludeStatusList = new ArrayList<>();
        excludeStatusList.add(LoanOrderStatus.CANCELLED);
        queryLoanOrderPageRequest.setExcludeStatusList(excludeStatusList);
        Pagination<io.wexchain.cryptoasset.loan.api.model.LoanOrder> loanOrderPagination = cryptoAssetLoanOperationClient.queryLoanOrderPage(queryLoanOrderPageRequest);

        List<LoanOrderVo> loanOrderVoList = new ArrayList<>();
        if(CollectionUtils.isNotEmpty(loanOrderPagination.getItems())){
            for (io.wexchain.cryptoasset.loan.api.model.LoanOrder loanOrder : loanOrderPagination.getItems()) {
                LoanProductVo loanProductVo = loanProductService.getLoanProductVo(Long.parseLong(loanOrder.getExtParam().get(LOAN_PRODUCT_ID)));
                LoanOrderVo loanOrderVo = new LoanOrderVo();
                setBaseLoanOrderInfo(loanOrderVo,loanOrder,loanProductVo);
                loanOrderVoList.add(loanOrderVo);
            }
        }
        Pagination<LoanOrderVo> loanOrderVoPagination = PaginationUtils.list2Page(loanOrderVoList, loanOrderPagination.getSortPageParam());
        return loanOrderVoPagination;
    }

    @Override
    public LoanOrderDetailVo getLoanOrderByChainOrderId(Long applyId) {
        io.wexchain.cryptoasset.loan.api.model.LoanOrder loanOrder = cryptoAssetLoanOperationClient.getLoanOrderByChainOrderId(applyId);
        LoanProductVo loanProductVo = loanProductService.getLoanProductVo(Long.parseLong(loanOrder.getExtParam().get(LOAN_PRODUCT_ID)));
        LoanOrderDetailVo vo = new LoanOrderDetailVo();
        vo.setReceiverAddress(loanOrder.getReceiverAddress());
        vo.setAllowRepayPermit(loanProductVo.isRepayPermit());
        Map<String, String> extParam = loanOrder.getExtParam();
        setBaseLoanOrderInfo(vo,loanOrder,loanProductVo);
        setLoanOrderDetailInfo(vo,extParam);

        return vo;
    }

    @Override
    public void confirmRepayment(ConfirmRepaymentRequest request) {
        cryptoAssetLoanOperationClient.confirmRepayment(request);
    }

    @Override
    public RepaymentBillVo queryRepaymentBill(Long chainOrderId) {
        RepaymentBill repaymentBill = cryptoAssetLoanOperationClient.queryRepaymentBill(chainOrderId);
        return new RepaymentBillVo(repaymentBill);
    }

    @Override
    public BigDecimal getLoanInterest(LoanInterestRequest loanInterestRequest) {
        LoanProductVo loanProductVo = loanProductService.getLoanProductVo(loanInterestRequest.getProductId());
        BigDecimal loanInterest = loanInterestRequest.getAmount().multiply(loanInterestRequest.getLoanPeriodValue()).multiply(loanProductVo.getLoanRate()).divide(new BigDecimal("365"),4,BigDecimal.ROUND_HALF_DOWN);
        return loanInterest;
    }

    private static void setBaseLoanOrderInfo(LoanOrderVo loanOrderVo , io.wexchain.cryptoasset.loan.api.model.LoanOrder loanOrder,LoanProductVo loanProductVo ){
        loanOrderVo.setOrderId(loanOrder.getChainOrderId());
        loanOrderVo.setApplyDate(new Date(Long.parseLong(loanOrder.getExtParam().get(APPLY_DATE))));
        loanOrderVo.setStatus(loanOrder.getStatus());
        loanOrderVo.setCurrency(loanProductVo.getCurrency());
        loanOrderVo.setLender(loanProductVo.getLender());
        loanOrderVo.setAmount(loanOrder.getAmount());
    }
    private static void setLoanOrderDetailInfo(LoanOrderDetailVo vo , Map<String, String> extParam){

        if(StringUtils.isNotBlank(extParam.getOrDefault(BORROW_DURATION_UNIT,null))){
            vo.setDurationUnit(extParam.get(BORROW_DURATION_UNIT));
        }
        if(StringUtils.isNotBlank(extParam.getOrDefault(LOAN_FEE,null))){
            vo.setFee(new BigDecimal(extParam.get(LOAN_FEE)).setScale(4));
        }
        if(StringUtils.isNotBlank(extParam.getOrDefault(DELIVER_DATE,null))){
            vo.setDeliverDate(new Date(Long.parseLong(extParam.get(DELIVER_DATE))));
        }
        if(StringUtils.isNotBlank(extParam.getOrDefault(REPAY_DATE,null))){
            vo.setRepayDate(new Date(Long.parseLong(extParam.get(REPAY_DATE))));
        }
        if(StringUtils.isNotBlank(extParam.getOrDefault(EXPECT_REPAY_DATE,null))){
            vo.setExpectRepayDate(new Date(Long.parseLong(extParam.get(EXPECT_REPAY_DATE))));
        }
        if(StringUtils.isNotBlank(extParam.getOrDefault(LOAN_INTEREST,null))){
            vo.setFee(new BigDecimal(extParam.get(LOAN_INTEREST)).setScale(4));
        }
        if(StringUtils.isNotBlank(extParam.getOrDefault(BORROW_DURATION,null))){
            vo.setBorrowDuration(new Integer(extParam.get(BORROW_DURATION)));
        }
        if(vo.getStatus() == LoanOrderStatus.DELIVERED){
            DateTime dateTime = new DateTime(vo.getRepayDate().getTime());
            DateTime startOfDay = dateTime.withTimeAtStartOfDay();
            if(startOfDay.isBeforeNow()){
                vo.setEarlyRepayAvailable(true);
            }
        }

    }
}
