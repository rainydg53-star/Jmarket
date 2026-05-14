package com.jmarket.mileage.service;

import com.jmarket.auth.domain.User;
import com.jmarket.auth.repository.UserRepository;
import com.jmarket.common.exception.ErrorCode;
import com.jmarket.common.exception.JmarketException;
import com.jmarket.mileage.domain.MileageAccount;
import com.jmarket.mileage.domain.MileageLedger;
import com.jmarket.mileage.domain.MileageLedgerType;
import com.jmarket.mileage.domain.MileageWithdrawal;
import com.jmarket.mileage.domain.MileageWithdrawalStatus;
import com.jmarket.mileage.dto.MileageAccountResponse;
import com.jmarket.mileage.dto.MileageLedgerResponse;
import com.jmarket.mileage.dto.MileageWithdrawalCreateRequest;
import com.jmarket.mileage.dto.MileageWithdrawalResponse;
import com.jmarket.mileage.repository.MileageAccountRepository;
import com.jmarket.mileage.repository.MileageLedgerRepository;
import com.jmarket.mileage.repository.MileageWithdrawalRepository;
import com.jmarket.notification.service.NotificationEventService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MileageService {

    private static final String REF_TYPE_TRADE = "TRADE";
    private static final String REF_TYPE_AUCTION = "AUCTION";
    private static final String REF_TYPE_WITHDRAWAL = "WITHDRAWAL";

    private final MileageAccountRepository mileageAccountRepository;
    private final MileageLedgerRepository mileageLedgerRepository;
    private final MileageWithdrawalRepository mileageWithdrawalRepository;
    private final UserRepository userRepository;
    private final NotificationEventService notificationEventService;

    public MileageService(
            MileageAccountRepository mileageAccountRepository,
            MileageLedgerRepository mileageLedgerRepository,
            MileageWithdrawalRepository mileageWithdrawalRepository,
            UserRepository userRepository,
            NotificationEventService notificationEventService
    ) {
        this.mileageAccountRepository = mileageAccountRepository;
        this.mileageLedgerRepository = mileageLedgerRepository;
        this.mileageWithdrawalRepository = mileageWithdrawalRepository;
        this.userRepository = userRepository;
        this.notificationEventService = notificationEventService;
    }

    @Transactional
    public MileageAccountResponse getMyAccount(String currentUserEmail) {
        User user = findUserByEmail(currentUserEmail);
        MileageAccount account = findOrCreateAccount(user);
        return MileageAccountResponse.from(account);
    }

    @Transactional(readOnly = true)
    public List<MileageLedgerResponse> getMyLedger(String currentUserEmail) {
        User user = findUserByEmail(currentUserEmail);
        return mileageLedgerRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(MileageLedgerResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MileageWithdrawalResponse> getMyWithdrawals(String currentUserEmail) {
        User user = findUserByEmail(currentUserEmail);
        return mileageWithdrawalRepository.findAllByUserIdOrderByRequestedAtDesc(user.getId()).stream()
                .map(MileageWithdrawalResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MileageWithdrawalResponse> getAllWithdrawalsForAdmin() {
        return mileageWithdrawalRepository.findAllByOrderByRequestedAtDesc().stream()
                .map(MileageWithdrawalResponse::from)
                .toList();
    }

    @Transactional
    public MileageAccountResponse chargeMyMileage(String currentUserEmail, Long amount) {
        validatePositiveAmount(amount);
        User user = findUserByEmail(currentUserEmail);
        MileageAccount account = chargeByUserIdInternal(user.getId(), amount, "MANUAL_CHARGE", 0L);
        return MileageAccountResponse.from(account);
    }

    @Transactional
    public void chargeBySystem(Long userId, Long amount, String refType, Long refId) {
        chargeByUserIdInternal(userId, amount, refType, refId);
    }

    @Transactional
    public MileageAccountResponse useMyMileage(String currentUserEmail, Long amount) {
        validatePositiveAmount(amount);
        User user = findUserByEmail(currentUserEmail);
        MileageAccount account = findOrCreateAccountForUpdate(user.getId());
        if (account.getAvailableBalance() < amount) {
            throw new JmarketException(ErrorCode.MILEAGE_INSUFFICIENT_BALANCE);
        }

        safeAmountMutation(() -> account.debitBalance(amount));
        saveLedger(account, MileageLedgerType.USE, amount, "MANUAL_USE", 0L);
        return MileageAccountResponse.from(account);
    }

    @Transactional
    public MileageWithdrawalResponse requestWithdrawal(String currentUserEmail, MileageWithdrawalCreateRequest request) {
        validatePositiveAmount(request.amount());
        User user = findUserByEmail(currentUserEmail);
        MileageAccount account = findOrCreateAccountForUpdate(user.getId());
        if (account.getAvailableBalance() < request.amount()) {
            throw new JmarketException(ErrorCode.MILEAGE_INSUFFICIENT_BALANCE);
        }

        MileageWithdrawal withdrawal = mileageWithdrawalRepository.save(new MileageWithdrawal(
                user,
                request.amount(),
                request.bankName().trim(),
                maskAccountNumber(request.accountNumber().trim()),
                request.accountHolder().trim()
        ));
        safeAmountMutation(() -> account.requestWithdrawal(request.amount()));
        saveLedger(account, MileageLedgerType.WITHDRAW_REQUEST, request.amount(), REF_TYPE_WITHDRAWAL, withdrawal.getId());
        notificationEventService.notifyMileageWithdrawalRequested(withdrawal);
        return MileageWithdrawalResponse.from(withdrawal);
    }

    @Transactional
    public MileageWithdrawalResponse completeWithdrawal(Long withdrawalId) {
        MileageWithdrawal withdrawal = findWithdrawalById(withdrawalId);
        if (withdrawal.getStatus() != MileageWithdrawalStatus.REQUESTED) {
            throw new JmarketException(ErrorCode.MILEAGE_WITHDRAWAL_INVALID_STATUS);
        }
        MileageAccount account = findOrCreateAccountForUpdate(withdrawal.getUser().getId());
        if (account.getWithdrawPendingBalance() < withdrawal.getAmount()) {
            throw new JmarketException(ErrorCode.MILEAGE_INSUFFICIENT_RESERVED);
        }

        safeAmountMutation(() -> account.completeWithdrawal(withdrawal.getAmount()));
        withdrawal.complete();
        saveLedger(account, MileageLedgerType.WITHDRAW_COMPLETE, withdrawal.getAmount(), REF_TYPE_WITHDRAWAL, withdrawal.getId());
        return MileageWithdrawalResponse.from(withdrawal);
    }

    @Transactional
    public MileageWithdrawalResponse rejectWithdrawal(Long withdrawalId, String reason) {
        MileageWithdrawal withdrawal = findWithdrawalById(withdrawalId);
        if (withdrawal.getStatus() != MileageWithdrawalStatus.REQUESTED) {
            throw new JmarketException(ErrorCode.MILEAGE_WITHDRAWAL_INVALID_STATUS);
        }
        MileageAccount account = findOrCreateAccountForUpdate(withdrawal.getUser().getId());
        if (account.getWithdrawPendingBalance() < withdrawal.getAmount()) {
            throw new JmarketException(ErrorCode.MILEAGE_INSUFFICIENT_RESERVED);
        }

        safeAmountMutation(() -> account.releaseWithdrawal(withdrawal.getAmount()));
        withdrawal.reject(reason.trim());
        saveLedger(account, MileageLedgerType.WITHDRAW_REJECT, withdrawal.getAmount(), REF_TYPE_WITHDRAWAL, withdrawal.getId());
        return MileageWithdrawalResponse.from(withdrawal);
    }

    @Transactional
    public void reserveForTrade(Long userId, Long amount, Long tradeId) {
        validatePositiveAmount(amount);
        MileageAccount account = findOrCreateAccountForUpdate(userId);
        if (account.getAvailableBalance() < amount) {
            throw new JmarketException(ErrorCode.MILEAGE_INSUFFICIENT_BALANCE);
        }

        safeAmountMutation(() -> account.reserve(amount));
        saveLedger(account, MileageLedgerType.RESERVE, amount, REF_TYPE_TRADE, tradeId);
    }

    @Transactional
    public void releaseTradeReservation(Long userId, Long amount, Long tradeId) {
        validatePositiveAmount(amount);
        MileageAccount account = findOrCreateAccountForUpdate(userId);
        if (account.getReservedBalance() < amount) {
            throw new JmarketException(ErrorCode.MILEAGE_INSUFFICIENT_RESERVED);
        }

        safeAmountMutation(() -> account.releaseReservation(amount));
        saveLedger(account, MileageLedgerType.RELEASE, amount, REF_TYPE_TRADE, tradeId);
    }

    @Transactional
    public void settleTradeReservedTransfer(Long buyerId, Long sellerId, Long amount, Long tradeId) {
        validatePositiveAmount(amount);
        MileageAccount buyerAccount = findOrCreateAccountForUpdate(buyerId);
        if (buyerAccount.getReservedBalance() < amount) {
            throw new JmarketException(ErrorCode.MILEAGE_INSUFFICIENT_RESERVED);
        }
        MileageAccount sellerAccount = findOrCreateAccountForUpdate(sellerId);

        safeAmountMutation(() -> buyerAccount.debitReserved(amount));
        saveLedger(buyerAccount, MileageLedgerType.TRANSFER_OUT, amount, REF_TYPE_TRADE, tradeId);

        safeAmountMutation(() -> sellerAccount.addBalance(amount));
        saveLedger(sellerAccount, MileageLedgerType.TRANSFER_IN, amount, REF_TYPE_TRADE, tradeId);
    }

    @Transactional
    public void reserveForAuction(Long userId, Long amount, Long auctionId) {
        validatePositiveAmount(amount);
        MileageAccount account = findOrCreateAccountForUpdate(userId);
        if (account.getAvailableBalance() < amount) {
            throw new JmarketException(ErrorCode.MILEAGE_INSUFFICIENT_BALANCE);
        }

        safeAmountMutation(() -> account.reserve(amount));
        saveLedger(account, MileageLedgerType.RESERVE, amount, REF_TYPE_AUCTION, auctionId);
    }

    @Transactional
    public void releaseAuctionReservation(Long userId, Long amount, Long auctionId) {
        validatePositiveAmount(amount);
        MileageAccount account = findOrCreateAccountForUpdate(userId);
        if (account.getReservedBalance() < amount) {
            throw new JmarketException(ErrorCode.MILEAGE_INSUFFICIENT_RESERVED);
        }

        safeAmountMutation(() -> account.releaseReservation(amount));
        saveLedger(account, MileageLedgerType.RELEASE, amount, REF_TYPE_AUCTION, auctionId);
    }

    @Transactional
    public void settleAuctionReservedTransfer(Long winnerId, Long sellerId, Long amount, Long auctionId) {
        validatePositiveAmount(amount);
        MileageAccount winnerAccount = findOrCreateAccountForUpdate(winnerId);
        if (winnerAccount.getReservedBalance() < amount) {
            throw new JmarketException(ErrorCode.MILEAGE_INSUFFICIENT_RESERVED);
        }
        MileageAccount sellerAccount = findOrCreateAccountForUpdate(sellerId);

        safeAmountMutation(() -> winnerAccount.debitReserved(amount));
        saveLedger(winnerAccount, MileageLedgerType.TRANSFER_OUT, amount, REF_TYPE_AUCTION, auctionId);

        safeAmountMutation(() -> sellerAccount.addBalance(amount));
        saveLedger(sellerAccount, MileageLedgerType.TRANSFER_IN, amount, REF_TYPE_AUCTION, auctionId);
    }

    private void validatePositiveAmount(Long amount) {
        if (amount == null || amount <= 0) {
            throw new JmarketException(ErrorCode.MILEAGE_INVALID_AMOUNT);
        }
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new JmarketException(ErrorCode.USER_NOT_FOUND));
    }

    private MileageWithdrawal findWithdrawalById(Long withdrawalId) {
        return mileageWithdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new JmarketException(ErrorCode.MILEAGE_WITHDRAWAL_NOT_FOUND));
    }

    private MileageAccount findOrCreateAccount(User user) {
        return mileageAccountRepository.findByUserId(user.getId())
                .orElseGet(() -> mileageAccountRepository.save(new MileageAccount(user)));
    }

    private MileageAccount findOrCreateAccountForUpdate(Long userId) {
        return mileageAccountRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new JmarketException(ErrorCode.USER_NOT_FOUND));
                    return mileageAccountRepository.save(new MileageAccount(user));
                });
    }

    private void saveLedger(
            MileageAccount account,
            MileageLedgerType type,
            Long amount,
            String refType,
            Long refId
    ) {
        mileageLedgerRepository.save(new MileageLedger(
                account.getUser(),
                type,
                amount,
                account.getBalance(),
                account.getReservedBalance(),
                refType,
                refId
        ));
    }

    private MileageAccount chargeByUserIdInternal(Long userId, Long amount, String refType, Long refId) {
        validatePositiveAmount(amount);
        MileageAccount account = findOrCreateAccountForUpdate(userId);
        safeAmountMutation(() -> account.addBalance(amount));
        saveLedger(account, MileageLedgerType.CHARGE, amount, refType, refId);
        return account;
    }

    private void safeAmountMutation(Runnable mutation) {
        try {
            mutation.run();
        } catch (ArithmeticException ex) {
            throw new JmarketException(ErrorCode.MILEAGE_INVALID_AMOUNT);
        }
    }

    private String maskAccountNumber(String accountNumber) {
        String digits = accountNumber.replaceAll("[^0-9]", "");
        if (digits.length() <= 4) {
            return "****";
        }
        return "*".repeat(Math.max(0, digits.length() - 4)) + digits.substring(digits.length() - 4);
    }
}
