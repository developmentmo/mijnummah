package nl.mijnummah.identity.application;

/** Published inside the transaction that stored a reset code; the email goes out after commit. */
record PasswordResetCodeIssued(String email, String displayName, String code, Kind kind) {

    enum Kind {
        /** Asked for by the user ("wachtwoord vergeten"); valid {@link PasswordResetService#CODE_TTL}. */
        SELF_SERVICE,
        /** A new account made by a platform administrator; the user chooses their own password. */
        INVITATION,
        /** Sent by a platform administrator for an existing account. */
        BY_ADMINISTRATOR
    }

    @Override
    public String toString() {
        return "PasswordResetCodeIssued[" + kind + ", ***]";
    }
}
