package nl.mijnummah.identity.application;

/** Published inside the transaction that stored a reset code; the email goes out after commit. */
record PasswordResetCodeIssued(String email, String displayName, String code) {

    @Override
    public String toString() {
        return "PasswordResetCodeIssued[***]";
    }
}
