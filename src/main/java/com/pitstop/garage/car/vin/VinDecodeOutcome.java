package com.pitstop.garage.car.vin;

public record VinDecodeOutcome(String flashAttribute, String messageKey) {

    public static VinDecodeOutcome success() {
        return new VinDecodeOutcome("successMessage", "flash.car.vinDecoded");
    }

    public static VinDecodeOutcome failed() {
        return new VinDecodeOutcome("errorMessage", "flash.car.vinDecodeFailed");
    }

    public static VinDecodeOutcome unavailable() {
        return new VinDecodeOutcome("errorMessage", "flash.car.vinDecodeUnavailable");
    }
}
