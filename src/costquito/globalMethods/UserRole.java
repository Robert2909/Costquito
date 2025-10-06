package costquito.globalMethods;

import com.google.gson.annotations.SerializedName;

public enum UserRole {
    ADMIN,
    @SerializedName(value = "VENDOR", alternate = { "VENDEDOR" })
    VENDOR
}
