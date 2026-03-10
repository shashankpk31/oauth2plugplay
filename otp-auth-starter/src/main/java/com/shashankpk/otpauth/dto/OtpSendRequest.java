package com.shashankpk.otpauth.dto;

import com.shashankpk.otpauth.model.OtpRecord;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtpSendRequest {

    private String phoneNumber;

    private String email;

    @NotNull(message = "OTP type is required")
    private OtpRecord.OtpType type;

    public String getIdentifier() {
        return phoneNumber != null ? phoneNumber : email;
    }
}
