package nl.mijnummah.common.error;

/**
 * A resource that does not exist <em>or is not visible to the caller</em>. Used deliberately for both, so that
 * content of another mosque, unpublished content and non-existing IDs are indistinguishable from the outside.
 */
public class NotFoundException extends ApplicationException {

    public NotFoundException(String code, String message) {
        super(ErrorCategory.NOT_FOUND, code, message);
    }
}
