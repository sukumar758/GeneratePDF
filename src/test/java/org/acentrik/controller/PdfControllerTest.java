package org.acentrik.controller;

import org.acentrik.service.PasswordValidator;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link PdfController} password generation.
 */
public class PdfControllerTest {

    /**
     * Verify that the controller generates passwords that satisfy the
     * complexity requirements enforced by {@link PasswordValidator}.
     */
    @Test
    public void testGenerateRandomPasswordMeetsComplexity() throws Exception {
        PdfController controller = new PdfController();
        PasswordValidator validator = new PasswordValidator();

        // Inject PasswordValidator into the controller
        Field field = PdfController.class.getDeclaredField("passwordValidator");
        field.setAccessible(true);
        field.set(controller, validator);

        // Invoke the private generateRandomPassword method
        Method method = PdfController.class.getDeclaredMethod("generateRandomPassword");
        method.setAccessible(true);
        String password = (String) method.invoke(controller);

        List<String> errors = validator.validatePasswordComplexity(password);
        assertTrue(errors.isEmpty(), "Generated password should meet complexity requirements");
    }
}

