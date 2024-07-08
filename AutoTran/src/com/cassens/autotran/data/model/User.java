package com.cassens.autotran.data.model;

import com.google.gson.annotations.SerializedName;
import com.sdgsystems.app_config.AppSetting;
import com.sdgsystems.util.HelperFuncs;

import java.util.Date;

public class User {
    public int user_id;

    @SerializedName("id")
    public String user_remote_id;
    @SerializedName("first_name")
    public String firstName;
    @SerializedName("last_name")
    public String lastName;
    public String email;
    public String driverNumber;
    public String deviceToken;
    public String deviceID;
    public String password;
    public String role;
    @SerializedName("user_type")
    public String userType;
    public String activationLink;
    public String status;
    public String created;
    public String modified;
    public String fullName;
    public int highClaims;
    public int requiresAudit;       // Should utilize in program logic only via requiresAnAudit()
    public int inspectionAccess;
    public String supervisorCardCode;
    public int supervisorPreloadChk;
    public Date driverLicenseExpiration;
    public Date medicalCertificateExpiration;
    public int helpTerm;
    public boolean autoInspectLastDelivery;

    public static final int LICENSE_EXPIRE_OK = 0;
    public static final int LICENSE_EXPIRE_WARNING = 1;
    public static final int LICENSE_EXPIRE_LOCK = 2;

    public enum ExpirationStatus {
        Okay(LICENSE_EXPIRE_OK),
        InWarningPeriod(LICENSE_EXPIRE_WARNING),
        InRestrictedPeriod(LICENSE_EXPIRE_LOCK),
        Expired(LICENSE_EXPIRE_LOCK);

        private int restrictionLevel = LICENSE_EXPIRE_OK;

        public int getRestrictionLevel() {
            return this.restrictionLevel;
        }
        private ExpirationStatus (int restrictionLevel) {
            this.restrictionLevel = restrictionLevel;
        }
    }

    public boolean requiresAnAudit() {
        return (highClaims != 0 || requiresAudit !=  0);
    }

    private ExpirationStatus getExpirationStatus(Date expirationDate) {

        if (expirationDate == null || HelperFuncs.isNullOrEmpty(String.valueOf(expirationDate))) {
            return ExpirationStatus.Okay;
        }

        Date today = new Date();
        Date warningDate = HelperFuncs.addSubtractDays(expirationDate, -AppSetting.LICENSE_WARNING_DAYS.getInt());
        Date restrictedDate = HelperFuncs.addSubtractDays(expirationDate, -AppSetting.LICENSE_LOCK_DAYS.getInt());

        if (today.after(expirationDate)) {
            return ExpirationStatus.Expired;
        }
        else if (!today.before(restrictedDate)) {
            return ExpirationStatus.InRestrictedPeriod;
        }
        else if (!today.before(warningDate)) {
            return ExpirationStatus.InWarningPeriod;
        }
        else {
            return ExpirationStatus.Okay;
        }
    }

    public ExpirationStatus getDriversLicenseExpirationStatus() {
        return getExpirationStatus(this.driverLicenseExpiration);
    }

    public ExpirationStatus getMedicalCertificateExpirationStatus() {
        return getExpirationStatus(this.medicalCertificateExpiration);
    }

    public int getLicenseExpirationRestrictionLevel() {
        return Math.max(getDriversLicenseExpirationStatus().getRestrictionLevel(), getMedicalCertificateExpirationStatus().getRestrictionLevel());
    }
}
