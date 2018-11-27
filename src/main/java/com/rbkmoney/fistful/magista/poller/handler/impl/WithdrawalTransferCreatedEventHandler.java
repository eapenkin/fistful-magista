package com.rbkmoney.fistful.magista.poller.handler.impl;

import com.rbkmoney.fistful.cashflow.FinalCashFlowPosting;
import com.rbkmoney.fistful.magista.dao.WithdrawalDao;
import com.rbkmoney.fistful.magista.domain.enums.WithdrawalEventType;
import com.rbkmoney.fistful.magista.domain.enums.WithdrawalStatus;
import com.rbkmoney.fistful.magista.domain.tables.pojos.WithdrawalEvent;
import com.rbkmoney.fistful.magista.exception.DaoException;
import com.rbkmoney.fistful.magista.exception.NotFoundException;
import com.rbkmoney.fistful.magista.exception.StorageException;
import com.rbkmoney.fistful.magista.poller.handler.WithdrawalEventHandler;
import com.rbkmoney.fistful.magista.util.CashFlowUtil;
import com.rbkmoney.fistful.withdrawal.Change;
import com.rbkmoney.fistful.withdrawal.SinkEvent;
import com.rbkmoney.geck.common.util.TBaseUtil;
import com.rbkmoney.geck.common.util.TypeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WithdrawalTransferCreatedEventHandler implements WithdrawalEventHandler {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final WithdrawalDao withdrawalDao;

    @Autowired
    public WithdrawalTransferCreatedEventHandler(WithdrawalDao withdrawalDao) {
        this.withdrawalDao = withdrawalDao;
    }

    @Override
    public boolean accept(Change change) {
        return change.isSetTransfer() && change.getTransfer().isSetCreated();
    }

    @Override
    public void handle(Change change, SinkEvent event) {
        try {
            log.info("Trying to handle WithdrawalTransferCreated, eventId={}, withdrawalId={}", event.getId(), event.getSource());
            WithdrawalEvent withdrawalEvent = withdrawalDao.getLastWithdrawalEvent(event.getSource());
            if (withdrawalEvent == null) {
                throw new NotFoundException(String.format("WithdrawalEvent with withdrawalId='%s' not found", event.getSource()));
            }
            List<FinalCashFlowPosting> postings = change.getTransfer().getCreated().getCashflow().getPostings();
            withdrawalEvent.setFee(CashFlowUtil.getFistfulFee(postings));
            withdrawalDao.saveWithdrawalEvent(withdrawalEvent);
            log.info("WithdrawalTransferCreated has been saved, eventId={}, withdrawalId={}", event.getId(), event.getSource());
        } catch (DaoException ex) {
            throw new StorageException(ex);
        }
    }

}