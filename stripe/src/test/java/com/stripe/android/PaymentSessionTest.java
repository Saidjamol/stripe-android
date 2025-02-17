package com.stripe.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import com.stripe.android.model.Customer;
import com.stripe.android.model.CustomerFixtures;
import com.stripe.android.model.PaymentMethod;
import com.stripe.android.model.PaymentMethodCreateParams;
import com.stripe.android.model.PaymentMethodFixtures;
import com.stripe.android.model.PaymentMethodTest;
import com.stripe.android.testharness.TestEphemeralKeyProvider;
import com.stripe.android.view.ActivityStarter;
import com.stripe.android.view.PaymentFlowActivity;
import com.stripe.android.view.PaymentFlowActivityStarter;
import com.stripe.android.view.PaymentMethodsActivity;
import com.stripe.android.view.PaymentMethodsActivityStarter;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link PaymentSession}
 */
@RunWith(RobolectricTestRunner.class)
public class PaymentSessionTest {

    @NonNull private static final Customer FIRST_CUSTOMER = CustomerFixtures.CUSTOMER;
    @NonNull private static final Customer SECOND_CUSTOMER = CustomerFixtures.OTHER_CUSTOMER;

    @NonNull private final TestEphemeralKeyProvider mEphemeralKeyProvider =
            new TestEphemeralKeyProvider();

    @NonNull private final PaymentSessionData mPaymentSessionData = new PaymentSessionData();

    @Mock private Activity mActivity;
    @Mock private ThreadPoolExecutor mThreadPoolExecutor;
    @Mock private PaymentSession.PaymentSessionListener mPaymentSessionListener;
    @Mock private CustomerSession mCustomerSession;
    @Mock private ActivityStarter<PaymentMethodsActivity, PaymentMethodsActivityStarter.Args>
            mPaymentMethodsActivityStarter;
    @Mock private ActivityStarter<PaymentFlowActivity, PaymentFlowActivityStarter.Args>
            mPaymentFlowActivityStarter;
    @Mock private PaymentSessionPrefs mPaymentSessionPrefs;

    @Captor private ArgumentCaptor<PaymentSessionData> mPaymentSessionDataArgumentCaptor;
    @Captor private ArgumentCaptor<Intent> mIntentArgumentCaptor;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        PaymentConfiguration.init(ApplicationProvider.getApplicationContext(),
                ApiKeyFixtures.FAKE_PUBLISHABLE_KEY);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                invocation.<Runnable>getArgument(0).run();
                return null;
            }
        }).when(mThreadPoolExecutor).execute(any(Runnable.class));
    }

    @Test
    public void init_addsPaymentSessionToken_andFetchesCustomer() {
        final CustomerSession customerSession = createCustomerSession();
        CustomerSession.setInstance(customerSession);

        final PaymentSession paymentSession = new PaymentSession(mActivity);
        paymentSession.init(mPaymentSessionListener, new PaymentSessionConfig.Builder().build());

        assertTrue(customerSession.getProductUsageTokens()
                .contains(PaymentSession.TOKEN_PAYMENT_SESSION));

        verify(mPaymentSessionListener).onCommunicatingStateChanged(eq(true));
    }

    @Test
    public void init_whenEphemeralKeyProviderContinues_fetchesCustomerAndNotifiesListener() {
        mEphemeralKeyProvider
                .setNextRawEphemeralKey(CustomerFixtures.EPHEMERAL_KEY_FIRST.toString());
        CustomerSession.setInstance(createCustomerSession());

        final PaymentSession paymentSession = new PaymentSession(mActivity);
        paymentSession.init(mPaymentSessionListener, new PaymentSessionConfig.Builder().build());
        verify(mPaymentSessionListener).onCommunicatingStateChanged(eq(true));
        verify(mPaymentSessionListener).onPaymentSessionDataChanged(any(PaymentSessionData.class));
        verify(mPaymentSessionListener).onCommunicatingStateChanged(eq(false));
    }

    @Test
    public void setCartTotal_setsExpectedValueAndNotifiesListener() {
        mEphemeralKeyProvider
                .setNextRawEphemeralKey(CustomerFixtures.EPHEMERAL_KEY_FIRST.toString());
        CustomerSession.setInstance(createCustomerSession());

        final PaymentSession paymentSession = new PaymentSession(mActivity);
        paymentSession.init(mPaymentSessionListener, new PaymentSessionConfig.Builder().build());
        paymentSession.setCartTotal(500L);

        verify(mPaymentSessionListener)
                .onPaymentSessionDataChanged(mPaymentSessionDataArgumentCaptor.capture());
        final PaymentSessionData data = mPaymentSessionDataArgumentCaptor.getValue();
        assertNotNull(data);
        assertEquals(500L, data.getCartTotal());
    }

    @Test
    public void handlePaymentData_whenPaymentMethodRequest_notifiesListenerAndFetchesCustomer() {
        CustomerSession.setInstance(createCustomerSession());

        PaymentSession paymentSession = new PaymentSession(mActivity);
        paymentSession.init(mPaymentSessionListener, new PaymentSessionConfig.Builder().build());

        // We have already tested the functionality up to here.
        reset(mPaymentSessionListener);

        final PaymentMethodsActivityStarter.Result result =
                new PaymentMethodsActivityStarter.Result(PaymentMethodFixtures.CARD_PAYMENT_METHOD);
        final boolean handled = paymentSession.handlePaymentData(
                PaymentMethodsActivityStarter.REQUEST_CODE, RESULT_OK,
                new Intent().putExtras(result.toBundle()));
        assertTrue(handled);

        verify(mPaymentSessionListener)
                .onPaymentSessionDataChanged(mPaymentSessionDataArgumentCaptor.capture());
        final PaymentSessionData data = mPaymentSessionDataArgumentCaptor.getValue();
        assertNotNull(data);
        assertEquals(PaymentMethodFixtures.CARD_PAYMENT_METHOD, data.getPaymentMethod());
    }

    @Test
    public void selectPaymentMethod_launchesPaymentMethodsActivityWithLog() {
        CustomerSession.setInstance(createCustomerSession());

        final PaymentSession paymentSession = new PaymentSession(mActivity);
        paymentSession.init(mPaymentSessionListener, new PaymentSessionConfig.Builder().build());

        paymentSession.presentPaymentMethodSelection();

        verify(mActivity).startActivityForResult(mIntentArgumentCaptor.capture(),
                eq(PaymentMethodsActivityStarter.REQUEST_CODE));

        final Intent intent = mIntentArgumentCaptor.getValue();
        assertNotNull(intent.getComponent());
        assertEquals(PaymentMethodsActivity.class.getName(),
                intent.getComponent().getClassName());

        final PaymentMethodsActivityStarter.Args args =
                PaymentMethodsActivityStarter.Args.create(intent);
        assertFalse(args.shouldRequirePostalCode);
    }

    @Test
    public void presentPaymentMethodSelection_withShouldRequirePostalCode_shouldPassInIntent() {
        CustomerSession.setInstance(createCustomerSession());

        final PaymentSession paymentSession = new PaymentSession(mActivity);
        paymentSession.init(mPaymentSessionListener, new PaymentSessionConfig.Builder().build());
        paymentSession.presentPaymentMethodSelection(true);

        verify(mActivity).startActivityForResult(mIntentArgumentCaptor.capture(),
                eq(PaymentMethodsActivityStarter.REQUEST_CODE));
        assertTrue(PaymentMethodsActivityStarter.Args.create(mIntentArgumentCaptor.getValue())
                .shouldRequirePostalCode);
    }

    @Test
    public void getSelectedPaymentMethodId_whenPrefsNotSet_returnsNull() {
        when(mCustomerSession.getCachedCustomer()).thenReturn(FIRST_CUSTOMER);
        CustomerSession.setInstance(mCustomerSession);
        assertNull(createPaymentSession().getSelectedPaymentMethodId(null));
    }

    @Test
    public void getSelectedPaymentMethodId_whenHasPaymentSessionData_returnsExpectedId() {
        mPaymentSessionData.setPaymentMethod(PaymentMethodFixtures.CARD_PAYMENT_METHOD);
        assertEquals("pm_123456789",
                createPaymentSession().getSelectedPaymentMethodId(null));
    }

    @Test
    public void getSelectedPaymentMethodId_whenHasPrefsSet_returnsExpectedId() {
        final String customerId = Objects.requireNonNull(FIRST_CUSTOMER.getId());
        when(mPaymentSessionPrefs.getSelectedPaymentMethodId(customerId)).thenReturn("pm_12345");

        when(mCustomerSession.getCachedCustomer()).thenReturn(FIRST_CUSTOMER);
        CustomerSession.setInstance(mCustomerSession);

        assertEquals("pm_12345",
                createPaymentSession().getSelectedPaymentMethodId(null));
    }

    @Test
    public void getSelectedPaymentMethodId_whenHasUserSpecifiedPaymentMethod_returnsExpectedId() {
        mPaymentSessionData.setPaymentMethod(PaymentMethodFixtures.CARD_PAYMENT_METHOD);
        assertEquals("pm_987",
                createPaymentSession().getSelectedPaymentMethodId("pm_987"));
    }

    @Test
    public void init_withoutSavedState_clearsLoggingTokensAndStartsWithPaymentSession() {
        final CustomerSession customerSession = createCustomerSession();
        CustomerSession.setInstance(customerSession);
        customerSession
                .addProductUsageTokenIfValid(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY);
        assertEquals(1, customerSession.getProductUsageTokens().size());

        PaymentSession paymentSession = new PaymentSession(mActivity);
        paymentSession.init(mPaymentSessionListener, new PaymentSessionConfig.Builder().build());

        // The init removes PaymentMethodsActivity, but then adds PaymentSession
        final Set<String> loggingTokens = customerSession.getProductUsageTokens();
        assertEquals(1, loggingTokens.size());
        assertFalse(loggingTokens.contains(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY));
        assertTrue(loggingTokens.contains(PaymentSession.TOKEN_PAYMENT_SESSION));
    }

    @Test
    public void init_withSavedStateBundle_doesNotClearLoggingTokens() {
        final CustomerSession customerSession = createCustomerSession();
        CustomerSession.setInstance(customerSession);
        customerSession
                .addProductUsageTokenIfValid(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY);
        assertEquals(1, customerSession.getProductUsageTokens().size());

        PaymentSession paymentSession = new PaymentSession(mActivity);
        // If it is given any saved state at all, the tokens are not cleared out.
        paymentSession.init(mPaymentSessionListener,
                new PaymentSessionConfig.Builder().build(), new Bundle());

        final Set<String> loggingTokens = customerSession.getProductUsageTokens();
        assertEquals(2, loggingTokens.size());
        assertTrue(loggingTokens.contains(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY));
        assertTrue(loggingTokens.contains(PaymentSession.TOKEN_PAYMENT_SESSION));
    }

    @Test
    public void completePayment_withLoggedActions_clearsLoggingTokensAndSetsResult() {
        final CustomerSession customerSession = createCustomerSession();
        CustomerSession.setInstance(customerSession);
        customerSession
                .addProductUsageTokenIfValid(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY);
        assertEquals(1, customerSession.getProductUsageTokens().size());

        PaymentSession paymentSession = new PaymentSession(mActivity);
        // If it is given any saved state at all, the tokens are not cleared out.
        paymentSession.init(mPaymentSessionListener,
                new PaymentSessionConfig.Builder().build(), new Bundle());

        final Set<String> loggingTokens = customerSession.getProductUsageTokens();
        assertEquals(2, loggingTokens.size());

        reset(mPaymentSessionListener);

        paymentSession.onCompleted();
        assertTrue(customerSession.getProductUsageTokens().isEmpty());
    }

    @Test
    public void init_withSavedState_setsPaymentSessionData() {
        mEphemeralKeyProvider
                .setNextRawEphemeralKey(CustomerFixtures.EPHEMERAL_KEY_FIRST.toString());
        CustomerSession.setInstance(createCustomerSession());

        PaymentSession paymentSession = new PaymentSession(mActivity);
        paymentSession.init(mPaymentSessionListener, new PaymentSessionConfig.Builder().build());

        paymentSession.setCartTotal(300L);

        verify(mPaymentSessionListener)
                .onPaymentSessionDataChanged(mPaymentSessionDataArgumentCaptor.capture());
        final Bundle bundle = new Bundle();
        paymentSession.savePaymentSessionInstanceState(bundle);
        PaymentSessionData firstPaymentSessionData = mPaymentSessionDataArgumentCaptor.getValue();

        PaymentSession.PaymentSessionListener secondListener =
                mock(PaymentSession.PaymentSessionListener.class);

        paymentSession.init(secondListener, new PaymentSessionConfig.Builder().build(), bundle);
        verify(secondListener)
                .onPaymentSessionDataChanged(mPaymentSessionDataArgumentCaptor.capture());

        final PaymentSessionData secondPaymentSessionData =
                mPaymentSessionDataArgumentCaptor.getValue();
        assertEquals(firstPaymentSessionData.getCartTotal(),
                secondPaymentSessionData.getCartTotal());
        assertEquals(firstPaymentSessionData.getPaymentMethod(),
                secondPaymentSessionData.getPaymentMethod());
    }

    @Test
    public void handlePaymentData_withInvalidRequestCode_aborts() {
        final PaymentSession paymentSession = createPaymentSession();
        assertFalse(paymentSession.handlePaymentData(-1, RESULT_CANCELED, new Intent()));
        verify(mCustomerSession, never()).retrieveCurrentCustomer(
                ArgumentMatchers.<CustomerSession.CustomerRetrievalListener>any());
    }

    @Test
    public void handlePaymentData_withValidRequestCodeAndCanceledResult_retrievesCustomer() {
        final PaymentSession paymentSession = createPaymentSession();
        assertFalse(paymentSession.handlePaymentData(PaymentMethodsActivityStarter.REQUEST_CODE,
                RESULT_CANCELED, new Intent()));
        verify(mCustomerSession).retrieveCurrentCustomer(
                ArgumentMatchers.<CustomerSession.CustomerRetrievalListener>any());
    }

    @NonNull
    private PaymentSession createPaymentSession() {
        return new PaymentSession(ApplicationProvider.getApplicationContext(), mCustomerSession,
                mPaymentMethodsActivityStarter, mPaymentFlowActivityStarter, mPaymentSessionData,
                mPaymentSessionPrefs);
    }

    @NonNull
    private CustomerSession createCustomerSession() {
        return new CustomerSession(ApplicationProvider.getApplicationContext(),
                mEphemeralKeyProvider, null, mThreadPoolExecutor,
                new FakeStripeRepository(),
                ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
                "acct_abc123", true);
    }

    private static final class FakeStripeRepository extends AbsFakeStripeRepository {
        @NonNull
        @Override
        public PaymentMethod createPaymentMethod(
                @NonNull PaymentMethodCreateParams paymentMethodCreateParams,
                @NonNull ApiRequest.Options options) {
            return Objects.requireNonNull(PaymentMethod.fromString(PaymentMethodTest.PM_CARD_JSON));
        }

        @NonNull
        @Override
        public Customer setDefaultCustomerSource(
                @NonNull String customerId,
                @NonNull String publishableKey,
                @NonNull Set<String> productUsageTokens,
                @NonNull String sourceId,
                @NonNull String sourceType,
                @NonNull ApiRequest.Options requestOptions) {
            return SECOND_CUSTOMER;
        }

        @NonNull
        @Override
        public Customer retrieveCustomer(
                @NonNull String customerId,
                @NonNull ApiRequest.Options requestOptions) {
            return FIRST_CUSTOMER;
        }
    }
}
