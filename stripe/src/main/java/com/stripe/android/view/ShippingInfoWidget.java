package com.stripe.android.view;

import android.content.Context;
import android.os.Build;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringDef;

import com.google.android.material.textfield.TextInputLayout;
import com.stripe.android.R;
import com.stripe.android.model.Address;
import com.stripe.android.model.ShippingInformation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * A widget used to collect address data from a user.
 */
public class ShippingInfoWidget extends LinearLayout {

    /**
     * Constants that can be used to mark fields in this widget as optional or hidden.
     * Some fields cannot be hidden.
     */
    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            CustomizableShippingField.ADDRESS_LINE_ONE_FIELD,
            CustomizableShippingField.ADDRESS_LINE_TWO_FIELD,
            CustomizableShippingField.CITY_FIELD,
            CustomizableShippingField.POSTAL_CODE_FIELD,
            CustomizableShippingField.STATE_FIELD,
            CustomizableShippingField.PHONE_FIELD
    })
    public @interface CustomizableShippingField {
        String ADDRESS_LINE_ONE_FIELD = "address_line_one";
        // address line two is optional by default
        String ADDRESS_LINE_TWO_FIELD = "address_line_two";
        String CITY_FIELD = "city";
        String POSTAL_CODE_FIELD = "postal_code";
        String STATE_FIELD = "state";
        String PHONE_FIELD = "phone";
    }

    @NonNull private final ShippingPostalCodeValidator mShippingPostalCodeValidator;
    private List<String> mOptionalShippingInfoFields = new ArrayList<>();
    private List<String> mHiddenShippingInfoFields = new ArrayList<>();

    @NonNull private final CountryAutoCompleteTextView mCountryAutoCompleteTextView;
    @NonNull private final TextInputLayout mAddressLine1TextInputLayout;
    @NonNull private final TextInputLayout mAddressLine2TextInputLayout;
    @NonNull private final TextInputLayout mCityTextInputLayout;
    @NonNull private final TextInputLayout mNameTextInputLayout;
    @NonNull private final TextInputLayout mPostalCodeTextInputLayout;
    @NonNull private final TextInputLayout mStateTextInputLayout;
    @NonNull private final TextInputLayout mPhoneNumberTextInputLayout;
    @NonNull private final StripeEditText mAddressEditText;
    @NonNull private final StripeEditText mAddressEditText2;
    @NonNull private final StripeEditText mCityEditText;
    @NonNull private final StripeEditText mNameEditText;
    @NonNull private final StripeEditText mPostalCodeEditText;
    @NonNull private final StripeEditText mStateEditText;
    @NonNull private final StripeEditText mPhoneNumberEditText;

    public ShippingInfoWidget(@NonNull Context context) {
        this(context, null);
    }

    public ShippingInfoWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ShippingInfoWidget(@NonNull Context context, @Nullable AttributeSet attrs,
                              int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(VERTICAL);
        inflate(context, R.layout.add_address_widget, this);

        mCountryAutoCompleteTextView = findViewById(R.id.country_autocomplete_aaw);
        mAddressLine1TextInputLayout = findViewById(R.id.tl_address_line1_aaw);
        mAddressLine2TextInputLayout = findViewById(R.id.tl_address_line2_aaw);
        mCityTextInputLayout = findViewById(R.id.tl_city_aaw);
        mNameTextInputLayout = findViewById(R.id.tl_name_aaw);
        mPostalCodeTextInputLayout = findViewById(R.id.tl_postal_code_aaw);
        mStateTextInputLayout = findViewById(R.id.tl_state_aaw);
        mAddressEditText = findViewById(R.id.et_address_line_one_aaw);
        mAddressEditText2 = findViewById(R.id.et_address_line_two_aaw);
        mCityEditText = findViewById(R.id.et_city_aaw);
        mNameEditText = findViewById(R.id.et_name_aaw);
        mPostalCodeEditText = findViewById(R.id.et_postal_code_aaw);
        mStateEditText = findViewById(R.id.et_state_aaw);
        mPhoneNumberEditText = findViewById(R.id.et_phone_number_aaw);
        mPhoneNumberTextInputLayout = findViewById(R.id.tl_phone_number_aaw);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mNameEditText.setAutofillHints(View.AUTOFILL_HINT_NAME);
            mAddressLine1TextInputLayout.setAutofillHints(View.AUTOFILL_HINT_POSTAL_ADDRESS);
            mPostalCodeEditText.setAutofillHints(View.AUTOFILL_HINT_POSTAL_CODE);
            mPhoneNumberEditText.setAutofillHints(View.AUTOFILL_HINT_PHONE);
        }

        mShippingPostalCodeValidator = new ShippingPostalCodeValidator();

        initView();
    }

    /**
     * @param optionalAddressFields address fields that should be optional.
     */
    public void setOptionalFields(@Nullable List<String> optionalAddressFields) {
        if (optionalAddressFields != null) {
            mOptionalShippingInfoFields = optionalAddressFields;
        } else {
            mOptionalShippingInfoFields = new ArrayList<>();
        }
        renderLabels();
        renderCountrySpecificLabels(mCountryAutoCompleteTextView.getSelectedCountryCode());
    }

    /**
     * @param hiddenAddressFields address fields that should be hidden. Hidden fields are
     *                            automatically optional.
     */
    public void setHiddenFields(@Nullable List<String> hiddenAddressFields) {
        if (hiddenAddressFields != null) {
            mHiddenShippingInfoFields = hiddenAddressFields;
        } else {
            mHiddenShippingInfoFields = new ArrayList<>();
        }
        renderLabels();
        renderCountrySpecificLabels(mCountryAutoCompleteTextView.getSelectedCountryCode());
    }

    @Nullable
    public ShippingInformation getShippingInformation() {
        if (!validateAllFields()) {
            return null;
        }

        final Address address = new Address.Builder()
                .setCity(mCityEditText.getText().toString())
                .setCountry(mCountryAutoCompleteTextView.getSelectedCountryCode())
                .setLine1(mAddressEditText.getText().toString())
                .setLine2(mAddressEditText2.getText().toString())
                .setPostalCode(mPostalCodeEditText.getText().toString())
                .setState(mStateEditText.getText().toString())
                .build();
        return new ShippingInformation(address, mNameEditText.getText().toString(),
                mPhoneNumberEditText.getText().toString());
    }

    /**
     * @param shippingInformation shippingInformation to populated into the widget input fields.
     */
    public void populateShippingInfo(@Nullable ShippingInformation shippingInformation) {
        if (shippingInformation == null) {
            return;
        }

        final Address address = shippingInformation.getAddress();
        if (address != null) {
            mCityEditText.setText(address.getCity());
            if (address.getCountry() != null && !address.getCountry().isEmpty()) {
                mCountryAutoCompleteTextView.setCountrySelected(address.getCountry());
            }
            mAddressEditText.setText(address.getLine1());
            mAddressEditText2.setText(address.getLine2());
            mPostalCodeEditText.setText(address.getPostalCode());
            mStateEditText.setText(address.getState());
        }
        mNameEditText.setText(shippingInformation.getName());
        mPhoneNumberEditText.setText(shippingInformation.getPhone());
    }

    /**
     * Validates all fields and shows error messages if appropriate.
     *
     * @return {@code true} if all shown fields are valid, {@code false} otherwise
     */
    public boolean validateAllFields() {
        final boolean isPostalCodeValid = mShippingPostalCodeValidator.isValid(
                Objects.requireNonNull(mPostalCodeEditText.getText()).toString(),
                mCountryAutoCompleteTextView.getSelectedCountryCode(),
                mOptionalShippingInfoFields, mHiddenShippingInfoFields
        );
        mPostalCodeEditText.setShouldShowError(!isPostalCodeValid);

        final boolean requiredAddressLine1Empty = mAddressEditText.getText().toString().isEmpty() &&
                !mOptionalShippingInfoFields
                        .contains(CustomizableShippingField.ADDRESS_LINE_ONE_FIELD) &&
                !mHiddenShippingInfoFields
                        .contains(CustomizableShippingField.ADDRESS_LINE_ONE_FIELD);
        mAddressEditText.setShouldShowError(requiredAddressLine1Empty);

        final boolean requiredCityEmpty = mCityEditText.getText().toString().isEmpty() &&
                !mOptionalShippingInfoFields.contains(CustomizableShippingField.CITY_FIELD) &&
                !mHiddenShippingInfoFields.contains(CustomizableShippingField.CITY_FIELD);
        mCityEditText.setShouldShowError(requiredCityEmpty);

        final boolean requiredNameEmpty = mNameEditText.getText().toString().isEmpty();
        mNameEditText.setShouldShowError(requiredNameEmpty);

        final boolean requiredStateEmpty = mStateEditText.getText().toString().isEmpty() &&
                !mOptionalShippingInfoFields.contains(CustomizableShippingField.STATE_FIELD) &&
                !mHiddenShippingInfoFields.contains(CustomizableShippingField.STATE_FIELD);
        mStateEditText.setShouldShowError(requiredStateEmpty);

        final boolean requiredPhoneNumberEmpty =
                mPhoneNumberEditText.getText().toString().isEmpty() &&
                        !mOptionalShippingInfoFields
                                .contains(CustomizableShippingField.PHONE_FIELD) &&
                        !mHiddenShippingInfoFields.contains(CustomizableShippingField.PHONE_FIELD);
        mPhoneNumberEditText.setShouldShowError(requiredPhoneNumberEmpty);

        return isPostalCodeValid && !requiredAddressLine1Empty && !requiredCityEmpty &&
                !requiredStateEmpty && !requiredNameEmpty && !requiredPhoneNumberEmpty;
    }

    private void initView() {
        mCountryAutoCompleteTextView.setCountryChangeListener(new CountryAutoCompleteTextView
                .CountryChangeListener() {
            @Override
            public void onCountryChanged(@NonNull String countryCode) {
                renderCountrySpecificLabels(countryCode);
            }
        });
        mPhoneNumberEditText.addTextChangedListener(new PhoneNumberFormattingTextWatcher());
        setupErrorHandling();
        renderLabels();
        renderCountrySpecificLabels(mCountryAutoCompleteTextView.getSelectedCountryCode());
    }

    private void setupErrorHandling() {
        mAddressEditText.setErrorMessageListener(new ErrorListener(mAddressLine1TextInputLayout));
        mCityEditText.setErrorMessageListener(new ErrorListener(mCityTextInputLayout));
        mNameEditText.setErrorMessageListener(new ErrorListener(mNameTextInputLayout));
        mPostalCodeEditText.setErrorMessageListener(new ErrorListener(mPostalCodeTextInputLayout));
        mStateEditText.setErrorMessageListener(new ErrorListener(mStateTextInputLayout));
        mPhoneNumberEditText.setErrorMessageListener(
                new ErrorListener(mPhoneNumberTextInputLayout));
        mAddressEditText.setErrorMessage(getResources().getString(R.string.address_required));
        mCityEditText.setErrorMessage(getResources().getString(R.string.address_city_required));
        mNameEditText.setErrorMessage(getResources().getString(R.string.address_name_required));
        mPhoneNumberEditText.setErrorMessage(getResources().getString(R.string
                .address_phone_number_required));
    }

    private void renderLabels() {
        mNameTextInputLayout.setHint(getResources().getString(R.string.address_label_name));
        if (mOptionalShippingInfoFields.contains(CustomizableShippingField.CITY_FIELD)) {
            mCityTextInputLayout.setHint(getResources().getString(R.string
                    .address_label_city_optional));
        } else {
            mCityTextInputLayout.setHint(getResources().getString(R.string.address_label_city));
        }
        if (mOptionalShippingInfoFields.contains(CustomizableShippingField.PHONE_FIELD)) {
            mPhoneNumberTextInputLayout.setHint(getResources().getString(R.string
                    .address_label_phone_number_optional));
        } else {
            mPhoneNumberTextInputLayout.setHint(getResources().getString(R.string
                    .address_label_phone_number));
        }
        hideHiddenFields();
    }

    private void hideHiddenFields() {
        if (mHiddenShippingInfoFields.contains(CustomizableShippingField.ADDRESS_LINE_ONE_FIELD)) {
            mAddressLine1TextInputLayout.setVisibility(GONE);
        }
        if (mHiddenShippingInfoFields.contains(CustomizableShippingField.ADDRESS_LINE_TWO_FIELD)) {
            mAddressLine2TextInputLayout.setVisibility(GONE);
        }
        if (mHiddenShippingInfoFields.contains(CustomizableShippingField.STATE_FIELD)) {
            mStateTextInputLayout.setVisibility(GONE);
        }
        if (mHiddenShippingInfoFields.contains(CustomizableShippingField.CITY_FIELD)) {
            mCityTextInputLayout.setVisibility(GONE);
        }
        if (mHiddenShippingInfoFields.contains(CustomizableShippingField.POSTAL_CODE_FIELD)) {
            mPostalCodeTextInputLayout.setVisibility(GONE);
        }
        if (mHiddenShippingInfoFields.contains(CustomizableShippingField.PHONE_FIELD)) {
            mPhoneNumberTextInputLayout.setVisibility(GONE);
        }
    }

    private void renderCountrySpecificLabels(@NonNull String countrySelected) {
        if (countrySelected.equals(Locale.US.getCountry())) {
            renderUSForm();
        } else if (countrySelected.equals(Locale.UK.getCountry())) {
            renderGreatBritainForm();
        } else if (countrySelected.equals(Locale.CANADA.getCountry())) {
            renderCanadianForm();
        } else {
            renderInternationalForm();
        }

        if (CountryUtils.doesCountryUsePostalCode(countrySelected) &&
                !mHiddenShippingInfoFields.contains(CustomizableShippingField.POSTAL_CODE_FIELD)) {
            mPostalCodeTextInputLayout.setVisibility(VISIBLE);
        } else {
            mPostalCodeTextInputLayout.setVisibility(GONE);
        }
    }

    private void renderUSForm() {
        if (mOptionalShippingInfoFields
                .contains(CustomizableShippingField.ADDRESS_LINE_ONE_FIELD)) {
            mAddressLine1TextInputLayout.setHint(getResources().getString(R.string
                    .address_label_address_optional));
        } else {
            mAddressLine1TextInputLayout.setHint(getResources().getString(R.string
                    .address_label_address));
        }
        mAddressLine2TextInputLayout.setHint(getResources().getString(R.string
                .address_label_apt_optional));
        if (mOptionalShippingInfoFields.contains(CustomizableShippingField.POSTAL_CODE_FIELD)) {
            mPostalCodeTextInputLayout.setHint(getResources().getString(R.string
                    .address_label_zip_code_optional));
        } else {
            mPostalCodeTextInputLayout.setHint(getResources().getString(R.string
                    .address_label_zip_code));
        }
        if (mOptionalShippingInfoFields.contains(CustomizableShippingField.STATE_FIELD)) {
            mStateTextInputLayout.setHint(getResources().getString(R.string
                    .address_label_state_optional));
        } else {
            mStateTextInputLayout.setHint(getResources().getString(R.string.address_label_state));
        }
        mPostalCodeEditText.setErrorMessage(getResources().getString(R.string.address_zip_invalid));
        mStateEditText.setErrorMessage(getResources().getString(R.string.address_state_required));
    }

    private void renderGreatBritainForm() {
        if (mOptionalShippingInfoFields
                .contains(CustomizableShippingField.ADDRESS_LINE_ONE_FIELD)) {
            mAddressLine1TextInputLayout.setHint(getResources().getString(R.string
                    .address_label_address_line1_optional));
        } else {
            mAddressLine1TextInputLayout.setHint(getResources().getString(R.string
                    .address_label_address_line1));
        }
        mAddressLine2TextInputLayout.setHint(getResources().getString(R.string
                .address_label_address_line2_optional));
        if (mOptionalShippingInfoFields.contains(CustomizableShippingField.POSTAL_CODE_FIELD)) {
            mPostalCodeTextInputLayout.setHint(getResources().getString(R.string
                    .address_label_postcode_optional));
        } else {
            mPostalCodeTextInputLayout.setHint(getResources().getString(R.string
                    .address_label_postcode));
        }
        if (mOptionalShippingInfoFields.contains(CustomizableShippingField.STATE_FIELD)) {
            mStateTextInputLayout.setHint(getResources().getString(R.string
                    .address_label_county_optional));
        } else {
            mStateTextInputLayout.setHint(getResources().getString(R.string.address_label_county));
        }
        mPostalCodeEditText.setErrorMessage(getResources().getString(R.string
                .address_postcode_invalid));
        mStateEditText.setErrorMessage(getResources().getString(R.string.address_county_required));
    }

    private void renderCanadianForm() {
        if (mOptionalShippingInfoFields
                .contains(CustomizableShippingField.ADDRESS_LINE_ONE_FIELD)) {
            mAddressLine1TextInputLayout.setHint(getResources().getString(R.string
                    .address_label_address_optional));
        } else {
            mAddressLine1TextInputLayout.setHint(getResources().getString(R.string
                    .address_label_address));
        }
        mAddressLine2TextInputLayout.setHint(getResources().getString(R.string
                .address_label_apt_optional));
        if (mOptionalShippingInfoFields.contains(CustomizableShippingField.POSTAL_CODE_FIELD)) {
            mPostalCodeTextInputLayout.setHint(getResources().getString(R.string
                    .address_label_postal_code_optional));
        } else {
            mPostalCodeTextInputLayout.setHint(getResources().getString(R.string
                    .address_label_postal_code));
        }
        if (mOptionalShippingInfoFields.contains(CustomizableShippingField.STATE_FIELD)) {
            mStateTextInputLayout.setHint(getResources().getString(R.string
                    .address_label_province_optional));
        } else {
            mStateTextInputLayout.setHint(getResources().getString(R.string
                    .address_label_province));
        }

        mPostalCodeEditText.setErrorMessage(getResources().getString(R.string
                .address_postal_code_invalid));
        mStateEditText.setErrorMessage(getResources().getString(R.string
                .address_province_required));
    }

    private void renderInternationalForm() {
        if (mOptionalShippingInfoFields
                .contains(CustomizableShippingField.ADDRESS_LINE_ONE_FIELD)) {
            mAddressLine1TextInputLayout.setHint(getResources().getString(R.string
                    .address_label_address_line1_optional));
        } else {
            mAddressLine1TextInputLayout.setHint(getResources().getString(R.string
                    .address_label_address_line1));
        }
        mAddressLine2TextInputLayout.setHint(getResources().getString(R.string
                .address_label_address_line2_optional));
        if (mOptionalShippingInfoFields.contains(CustomizableShippingField.POSTAL_CODE_FIELD)) {
            mPostalCodeTextInputLayout.setHint(getResources().getString(R.string
                    .address_label_zip_postal_code_optional));
        } else {
            mPostalCodeTextInputLayout.setHint(getResources().getString(R.string
                    .address_label_zip_postal_code));
        }
        if (mOptionalShippingInfoFields.contains(CustomizableShippingField.STATE_FIELD)) {
            mStateTextInputLayout.setHint(getResources().getString(R.string
                    .address_label_region_generic_optional));
        } else {
            mStateTextInputLayout.setHint(getResources().getString(R.string
                    .address_label_region_generic));
        }

        mPostalCodeEditText.setErrorMessage(getResources().getString(R.string
                .address_zip_postal_invalid));
        mStateEditText.setErrorMessage(getResources().getString(R.string
                .address_region_generic_required));
    }
}
