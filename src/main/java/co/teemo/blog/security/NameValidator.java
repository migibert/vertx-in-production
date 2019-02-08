package co.teemo.blog.security;

import io.vertx.ext.web.api.RequestParameter;
import io.vertx.ext.web.api.validation.ParameterTypeValidator;
import io.vertx.ext.web.api.validation.ValidationException;

public class NameValidator implements ParameterTypeValidator {
    @Override
    public RequestParameter isValid(String value) throws ValidationException {
        if(!isFirstCharUpperCase(value)) {
            throw new ValidationException("Name must start with an uppercase char");
        }

        if(!isStringAlphabetical(value)) {
            throw new ValidationException("Name must be composed with alphabetical chars");
        }

        return RequestParameter.create(value);
    }

    private static boolean isFirstCharUpperCase(String value) {
        return Character.isUpperCase(value.charAt(0));
    }

    private static boolean isStringAlphabetical(String value) {
        return value.chars().allMatch(Character::isAlphabetic);
    }
}
