package com.stripe.android.view;

import androidx.recyclerview.widget.RecyclerView;

import com.stripe.android.model.PaymentMethod;
import com.stripe.android.model.PaymentMethodFixtures;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test class for {@link PaymentMethodsAdapter}
 */
@RunWith(RobolectricTestRunner.class)
public class PaymentMethodsAdapterTest {
    @Mock private RecyclerView.AdapterDataObserver mAdapterDataObserver;
    private PaymentMethodsAdapter mPaymentMethodsAdapter;

    @Before
    public void setup() {
        PaymentMethodFixtures.createCard();
        MockitoAnnotations.initMocks(this);
        mPaymentMethodsAdapter = new PaymentMethodsAdapter(null,
                new PaymentMethodsActivityStarter.Args.Builder()
                        .build());
        mPaymentMethodsAdapter.registerAdapterDataObserver(mAdapterDataObserver);
    }

    @Test
    public void setSelection_changesSelection() {
        mPaymentMethodsAdapter.setPaymentMethods(PaymentMethodFixtures.CARD_PAYMENT_METHODS);
        assertEquals(4, mPaymentMethodsAdapter.getItemCount());
        verify(mAdapterDataObserver).onChanged();

        assertEquals(PaymentMethodFixtures.CARD_PAYMENT_METHODS.get(2).id,
                Objects.requireNonNull(mPaymentMethodsAdapter.getSelectedPaymentMethod()).id);

        mPaymentMethodsAdapter.setSelectedPaymentMethodId(
                PaymentMethodFixtures.CARD_PAYMENT_METHODS.get(1).id);
        assertEquals(PaymentMethodFixtures.CARD_PAYMENT_METHODS.get(1).id,
                mPaymentMethodsAdapter.getSelectedPaymentMethod().id);
    }

    @Test
    public void updatePaymentMethods_removesExistingPaymentMethodsAndAddsAllPaymentMethods() {
        final List<PaymentMethod> singlePaymentMethod =
                Collections.singletonList(PaymentMethodFixtures.CARD_PAYMENT_METHODS.get(0));

        mPaymentMethodsAdapter.setPaymentMethods(singlePaymentMethod);
        assertEquals(2, mPaymentMethodsAdapter.getItemCount());
        assertNotNull(mPaymentMethodsAdapter.getSelectedPaymentMethod());

        assertEquals(PaymentMethodFixtures.CARD_PAYMENT_METHODS.get(0).id,
                mPaymentMethodsAdapter.getSelectedPaymentMethod().id);

        mPaymentMethodsAdapter.setPaymentMethods(PaymentMethodFixtures.CARD_PAYMENT_METHODS);
        assertEquals(4, mPaymentMethodsAdapter.getItemCount());
        assertEquals(PaymentMethodFixtures.CARD_PAYMENT_METHODS.get(2).id,
                mPaymentMethodsAdapter.getSelectedPaymentMethod().id);
        verify(mAdapterDataObserver, times(2)).onChanged();
    }

    @Test
    public void updatePaymentMethods_withSelection_updatesPaymentMethodsAndSelectionMaintained() {
        mPaymentMethodsAdapter.setPaymentMethods(PaymentMethodFixtures.CARD_PAYMENT_METHODS);
        assertEquals(4, mPaymentMethodsAdapter.getItemCount());
        mPaymentMethodsAdapter.setSelectedPaymentMethodId(
                PaymentMethodFixtures.CARD_PAYMENT_METHODS.get(2).id);
        assertNotNull(mPaymentMethodsAdapter.getSelectedPaymentMethod());

        mPaymentMethodsAdapter.setPaymentMethods(PaymentMethodFixtures.CARD_PAYMENT_METHODS);
        assertEquals(4, mPaymentMethodsAdapter.getItemCount());
        assertEquals(PaymentMethodFixtures.CARD_PAYMENT_METHODS.get(2).id,
                mPaymentMethodsAdapter.getSelectedPaymentMethod().id);
    }

    @Test
    public void setPaymentMethods_whenNoInitialSpecified_selectsMostRecentlyCreated() {
        final PaymentMethodsAdapter adapter = new PaymentMethodsAdapter(null,
                new PaymentMethodsActivityStarter.Args.Builder()
                        .build());
        adapter.setPaymentMethods(PaymentMethodFixtures.CARD_PAYMENT_METHODS);
        assertEquals("pm_3000", adapter.getSelectedPaymentMethod().id);
    }

    @Test
    public void setPaymentMethods_whenInitialSpecified_selectsIt() {
        final PaymentMethodsAdapter adapter = new PaymentMethodsAdapter("pm_1000",
                new PaymentMethodsActivityStarter.Args.Builder()
                        .build());
        adapter.setPaymentMethods(PaymentMethodFixtures.CARD_PAYMENT_METHODS);
        assertEquals("pm_1000", adapter.getSelectedPaymentMethod().id);
    }
}
