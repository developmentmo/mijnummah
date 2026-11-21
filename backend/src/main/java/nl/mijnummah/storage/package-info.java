/**
 * Image storage: uploaded images (logos, cover photos) and serving them publicly.
 * <p>
 * Every image belongs to one mosque. This module does not check who may upload: the module that offers the upload
 * checks the caller's rights for that mosque first, and a module that links an image to its data checks with
 * {@link ImageStorage#requireOwnedBy} that the image belongs to the same mosque. Public API: {@link ImageStorage}.
 * Videos are never stored here; they stay at an external provider.
 */
@ApplicationModule(displayName = "Storage")
package nl.mijnummah.storage;

import org.springframework.modulith.ApplicationModule;
